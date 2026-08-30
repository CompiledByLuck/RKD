package com.example.savinov;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TodoRepository extends MongoRepository<Todo, String> {
}
