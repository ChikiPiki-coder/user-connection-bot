package com.telegram.userBot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telegram.userBot.entity.TargetEntity;


@Repository
public interface TargetRepository extends JpaRepository<TargetEntity, UUID> {
    Optional<TargetEntity> findTargetEntitiesByProductIdAndUserId(String productId, UUID userId);

    Optional<TargetEntity> findByUuid(UUID uuid);

    TargetEntity findByUserId(UUID userId);

    List<TargetEntity> findAllByState(String state);


}

