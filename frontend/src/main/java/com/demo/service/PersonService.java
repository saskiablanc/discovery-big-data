package com.demo.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.demo.model.Person;
import com.demo.repository.PersonRepository;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    // Liste des clients SSE connectés
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<Person> findAll() {
        return personRepository.findAll();
    }

    // Abonner un nouveau client SSE
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError((e)     -> emitters.remove(emitter));
        return emitter;
    }

    // Appelé par le Consumer Kafka (ou manuellement) pour notifier tous les clients
    public void notifyNewPerson(Person person) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(person));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}