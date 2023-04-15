package com.telegram.userBot.dto;

import lombok.Data;

@Data
public class UserInfoDTO {

    private String userName;

    private Long chatId;
    private Long userId;
}
