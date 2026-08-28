package com.shiftwise.ai.kubeoptix.settings;

import java.time.LocalDateTime;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_setings")
public class SystemSettings extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "language")
    public Language language = Language.EN;

    @Column(name = "cursor_api_key")
    public String cursorApiKey;

    @Column(name = "cursor_model")
    public String cursorModel;

    @Column(name = "llm_api_key")
    public String llmApiKey;

    @Column(name = "llm_model")
    public String llmModel;

    @Column(name = "status")
    public SettingsStatus status = SettingsStatus.ACTIVE;

    @Column(name = "default_extraction_method")
    public ExtractionMethod defaultExtractionMethod = ExtractionMethod.ML;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }
}