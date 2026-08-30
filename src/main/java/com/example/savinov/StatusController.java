package com.example.savinov;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/")
    public Map<String, Object> status() {
        return Map.of(
                "app", "savinov-todo",
                "status", "running",
                "crudEndpoint", "/api/todos");
    }
}
