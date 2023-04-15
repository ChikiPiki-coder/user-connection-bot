package com.telegram.userBot.entity;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "target", schema = "public")
public class TargetEntity {
    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "state")
    private String state;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "rule_value")
    private Long ruleValue;

    @Column(name = "user_id")
    private UUID userId;
}
