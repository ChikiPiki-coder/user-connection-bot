package com.telegram.userBot.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telegram.userBot.entity.UserInfoEntity;

@Repository
public interface UserInfoRepository  extends JpaRepository<UserInfoEntity, UUID> {
}
