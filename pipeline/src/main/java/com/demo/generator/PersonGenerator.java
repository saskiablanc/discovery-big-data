package com.demo.generator;

import com.demo.model.PersonEvent;

/**
 * Spark Job 1 — Generator.
 *
 * Flux :
 *   Appel à l'API randomuser.me
 *   → parse le JSON en PersonEvent
 *   → envoie chaque personne dans le topic Kafka "persons"
 */
public class PersonGenerator {

    public static void main(String[] args) throws Exception {
        // TODO
    }
}