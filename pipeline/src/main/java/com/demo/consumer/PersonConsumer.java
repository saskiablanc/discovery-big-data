package com.demo.consumer;

import com.demo.model.PersonEvent;

/**
 * Spark Job 2 — Consumer.
 *
 * Flux :
 *   Spark Structured Streaming lit le topic Kafka "persons"
 *   → chaque message JSON est parsé en PersonEvent
 *   → chaque personne est insérée dans la table person de PostgreSQL
 */
public class PersonConsumer {

    public static void main(String[] args) throws Exception {
        // TODO
    }
}