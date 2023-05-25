package com.telegram.userBot.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetDTO {
    private String productId;
    private UUID userId;
    private Long ruleValue;
}
