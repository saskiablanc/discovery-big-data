package com.demo.generator;

import com.demo.model.PersonEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Spark Job 1 — Generator.
 *
 * Flux :
 *   Génère 1 000 000 de PersonEvent via DataFaker
 *   → Spark distribue les IDs en RDD sur 20 partitions
 *   → Chaque partition crée un KafkaProducer et envoie les messages
 *   → Envoi dans le topic Kafka "persons"
 */
public class PersonGenerator {

    private static final String KAFKA_HOST = "localhost:9092";
    private static final String TOPIC      = "persons";
    private static final long   TOTAL      = 1_000_000L;
    private static final int    PARTITIONS = 20;
    private static final int    BATCH_SIZE = 500;

    public static void main(String[] args) throws Exception {

        SparkSession spark = SparkSession.builder()
                .appName("PersonGenerator")
                .master("local[*]")
                .config("spark.sql.shuffle.partitions", "20")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

        System.out.println("=== Generator démarré — " + TOTAL + " personnes ===");

        // IDs de 1 à 1_000_000 — chaque personne a un id unique
        List<Long> indices = LongStream.rangeClosed(1, TOTAL)
                .boxed()
                .collect(Collectors.toList());

        JavaRDD<Long> rdd = sc.parallelize(indices, PARTITIONS);

        String kafkaHost = KAFKA_HOST;
        String topic     = TOPIC;

        rdd.foreachPartition(partition -> {

            Faker faker         = new Faker(Locale.ENGLISH);
            ObjectMapper mapper = new ObjectMapper();

            Properties props = new Properties();
            props.put("bootstrap.servers",  kafkaHost);
            props.put("key.serializer",     "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer",   "org.apache.kafka.common.serialization.StringSerializer");
            props.put("acks",               "1");
            props.put("linger.ms",          "5");
            props.put("batch.size",         "65536");
            props.put("compression.type",   "lz4");

            try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

                int count = 0;

                while (partition.hasNext()) {
                    long id = partition.next();

                    boolean isMale   = (id % 2 == 0);
                    String  gender   = isMale ? "men" : "women";
                    int     photoIdx = (int)(id % 100);

                    PersonEvent person = new PersonEvent(
                            id,
                            faker.name().firstName(),
                            faker.name().lastName(),
                            faker.address().country(),
                            faker.number().numberBetween(18, 90),
                            "https://randomuser.me/api/portraits/" + gender + "/" + photoIdx + ".jpg"
                    );

                    String value = mapper.writeValueAsString(person);
                    producer.send(new ProducerRecord<>(topic, String.valueOf(id), value),
                            (metadata, ex) -> {
                                if (ex != null) System.err.println("Kafka error: " + ex.getMessage());
                            });

                    count++;
                    if (count % BATCH_SIZE == 0) producer.flush();
                }

                producer.flush();
            }
        });

        System.out.println("=== " + TOTAL + " messages envoyés dans Kafka ===");

        sc.close();
        spark.stop();
    }
}