package com.telegram.userBot.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telegram.userBot.entity.RuleInfoEntity;

@Repository
public interface RuleInfoRepository  extends JpaRepository<RuleInfoEntity, UUID> {

}
