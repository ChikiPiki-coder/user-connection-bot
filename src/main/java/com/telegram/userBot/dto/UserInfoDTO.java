package com.telegram.userBot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDTO {

    private String userName;

    private Long chatId;
    private Long userId;
}
