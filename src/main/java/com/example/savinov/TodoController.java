package com.example.savinov;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping
    public List<Todo> getAll() {
        return todoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getById(@PathVariable String id) {
        return todoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Todo> create(@RequestBody Todo todo) {
        todo.setId(null);
        Todo saved = todoRepository.save(todo);
        return ResponseEntity.created(URI.create("/api/todos/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> replace(@PathVariable String id, @RequestBody Todo todo) {
        if (!todoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        todo.setId(id);
        return ResponseEntity.ok(todoRepository.save(todo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!todoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        todoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
