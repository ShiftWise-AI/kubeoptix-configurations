package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document extends PanacheEntityBase {

    @Id
    @Column(name = "document_name", nullable = false, unique = true)
    public String documentName;

    // Business identity of a document: only one row may exist per title.
    @Column(nullable = false, unique = true)
    public String title;

    @Column(name = "project_manager", nullable = false)
    public String projectManager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    public Author author;

    @Column(name = "costumer", nullable = false)
    public String costumer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costumers_list", nullable = false)
    public Customer customer;

    @OneToMany(mappedBy = "document")
    public List<Version> versions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }
}