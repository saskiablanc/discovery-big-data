package com.demo.controller;

import com.demo.model.Person;
import com.demo.service.PersonService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@CrossOrigin(origins = "*")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    // GET /api/persons — liste complète
    @GetMapping
    public List<Person> getAll() {
        return personService.findAll();
    }

    // GET /api/persons/stream — flux SSE temps réel
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return personService.subscribe();
    }

    // POST /api/persons/notify — appelé par le Consumer Kafka
    @PostMapping("/notify")
    public void notify(@RequestBody Person person) {
        personService.notifyNewPerson(person);
    }
}