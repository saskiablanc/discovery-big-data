package com.demo.consumer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;
import static org.apache.spark.sql.functions.upper;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

/**
 * Spark Job 2 — Consumer.
 *
 * Flux :
 *   Spark Structured Streaming lit le topic Kafka "persons"
 *   → parse JSON en colonnes structurées
 *   → TRANSFORMATION : lastName en majuscules
 *   → insertion bulk dans PostgreSQL via JDBC
 *   → notification échantillonnée du frontend via POST /api/persons/notify
 */
public class PersonConsumer {

    private static final String NOTIFY_URL    = "http://localhost:8082/api/persons/notify";
    private static final int    NOTIFY_SAMPLE = 100;

    public static void main(String[] args) throws Exception {

        SparkSession spark = SparkSession.builder()
                .appName("PersonConsumer")
                .master("local[*]")
                .config("spark.sql.shuffle.partitions", "4")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        System.out.println("=== Consumer démarré — en attente de messages Kafka ===");

        // 1. Lire le stream Kafka
        Dataset<Row> kafkaStream = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "localhost:9092")
                .option("subscribe", "persons")
                .option("startingOffsets", "earliest")
                .option("maxOffsetsPerTrigger", "100000")
                .load();

        // 2. Schéma JSON attendu
        StructType schema = new StructType()
                .add("id",          DataTypes.LongType)
                .add("firstName",   DataTypes.StringType)
                .add("lastName",    DataTypes.StringType)
                .add("nationality", DataTypes.StringType)
                .add("age",         DataTypes.IntegerType)
                .add("pictureUrl",  DataTypes.StringType);

        // 3. Parser le JSON
        Dataset<Row> parsed = kafkaStream
                .select(col("value").cast(DataTypes.StringType).alias("json"))
                .select(from_json(col("json"), schema).alias("data"))
                .select("data.*");

        // 4. TRANSFORMATION : lastName en majuscules
        Dataset<Row> transformed = parsed
                .withColumn("lastName", upper(col("lastName")));

        // 5. Pour chaque micro-batch : insertion bulk en BD + notification frontend
        StreamingQuery query = transformed.writeStream()
                .option("checkpointLocation", "/tmp/spark-checkpoints/person-consumer")
                .foreachBatch((batchDF, batchId) -> {
                    long count = batchDF.count();
                    System.out.println("Batch #" + batchId + " — lignes : " + count);

                    if (count == 0) return;

                    // INSERT bulk via JDBC
                    batchDF
                            .withColumnRenamed("firstName",   "first_name")
                            .withColumnRenamed("lastName",    "last_name")
                            .withColumnRenamed("pictureUrl",  "picture_url")
                            .write()
                            .mode("append")
                            .format("jdbc")
                            .option("url",           "jdbc:postgresql://localhost:5432/kafkaspark")
                            .option("dbtable",       "person")
                            .option("user",          "postgres")
                            .option("password",      "postgres")
                            .option("driver",        "org.postgresql.Driver")
                            .option("batchsize",     "50000")
                            .option("numPartitions", "4")
                            .save();

                    notifyCount(count);
                })
                .trigger(Trigger.ProcessingTime("2 seconds"))
                .outputMode("append")
                .start();

        query.awaitTermination();
    }

    private static void notifyCount(long count) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOTIFY_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"count\":" + count + "}"))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Notification échouée : " + e.getMessage());
        }
    }
}