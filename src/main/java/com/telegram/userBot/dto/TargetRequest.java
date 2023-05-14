package com.telegram.userBot.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetRequest {
    private UUID targetUUID;
    private String productId;
    private String userId;
}
