package com.example.lastname;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/")
    public Map<String, Object> status() {
        return Map.of(
                "app", "lastname-todo",
                "status", "running",
                "crudEndpoint", "/api/todos");
    }
}
