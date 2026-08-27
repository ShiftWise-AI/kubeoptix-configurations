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
@Table(name = "versions")
public class Version extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "version_number", nullable = false)
    public String versionNumber;

    // Markdown reports routinely exceed 255 chars, so the column must not use the varchar(255) default.
    @Column(name = "markdown_content", columnDefinition = "text")
    public String markdownContent;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "version")
    public List<DocumentVersion> documentVersions = new ArrayList<>();

    @PrePersist
    void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }
}