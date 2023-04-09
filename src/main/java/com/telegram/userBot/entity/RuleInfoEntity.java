package com.telegram.userBot.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rule_info", schema = "public")
public class RuleInfoEntity {

    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "rule_value")
    private String ruleValue;


}
