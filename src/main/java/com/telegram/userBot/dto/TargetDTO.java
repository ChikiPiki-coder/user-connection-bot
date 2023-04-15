package com.telegram.userBot.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class TargetDTO {
    private String productId;
    private UUID userId;
    private Long ruleValue;
}
