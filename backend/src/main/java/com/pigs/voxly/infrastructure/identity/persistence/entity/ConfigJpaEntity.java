package com.pigs.voxly.infrastructure.identity.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class ConfigJpaEntity {

    @Id
    @Column(name = "config_key", length = 100, nullable = false)
    private String configKey;

    @Column(name = "boolean_value", nullable = false)
    private boolean booleanValue;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ConfigJpaEntity() {
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
