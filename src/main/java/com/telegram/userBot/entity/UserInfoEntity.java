package com.telegram.userBot.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "user_info", schema = "public")
public class UserInfoEntity {

    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id")
    private Long userId;
//
//    @Column(name = "target_chat_id")
//    private Long targetChatId;

}
