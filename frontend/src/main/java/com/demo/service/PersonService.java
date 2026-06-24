package com.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.demo.model.Person;
import com.demo.repository.PersonRepository;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Page<Person> findPage(int page, int size) {
        return personRepository.findAll(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError((e)     -> emitters.remove(emitter));
        return emitter;
    }

    public void notifyBatchCount(long count) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data("{\"count\":" + count + "}"));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}