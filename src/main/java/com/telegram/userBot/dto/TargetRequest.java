package com.telegram.userBot.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class TargetRequest {
    private UUID targetUUID;
    private String productId;
    private String userId;
}
