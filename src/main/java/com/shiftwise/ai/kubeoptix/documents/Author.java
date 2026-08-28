package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "authors")
public class Author extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String position;

    @Column(nullable = false)
    public String email;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "author")
    public List<Document> documents = new ArrayList<>();

    @PrePersist
    void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }
}