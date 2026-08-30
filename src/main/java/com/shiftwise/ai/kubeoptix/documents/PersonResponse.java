package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.UUID;

public record PersonResponse(UUID id, String name, String position, String email, LocalDateTime createdAt) {

    static PersonResponse from(Author author) {
        return new PersonResponse(author.id, author.name, author.position, author.email, author.createdAt);
    }

    static PersonResponse from(Customer customer) {
        return new PersonResponse(customer.id, customer.name, customer.position, customer.email, customer.createdAt);
    }
}