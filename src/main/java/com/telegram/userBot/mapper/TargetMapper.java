package com.telegram.userBot.mapper;

import java.sql.Timestamp;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.telegram.userBot.dto.TargetDTO;
import com.telegram.userBot.entity.TargetEntity;


@Mapper(componentModel = "spring", imports = { Timestamp.class, UUID.class})
public interface TargetMapper {

    @Mapping(target = "createdAt", expression = "java(new Timestamp(System.currentTimeMillis()))")
    @Mapping(target = "updatedAt", expression = "java(new Timestamp(System.currentTimeMillis()))")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "state", constant = "CREATING")
    TargetEntity toNewEntity(TargetDTO targetDTO);

    @Mapping(target = "updatedAt", expression = "java(new Timestamp(System.currentTimeMillis()))")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "ruleValue", source = "ruleValue")
    TargetEntity updateEntity(@MappingTarget TargetEntity targetEntity, String state, Long ruleValue);

    @Mapping(target = "updatedAt", expression = "java(new Timestamp(System.currentTimeMillis()))")
    @Mapping(target = "ruleValue", source = "ruleValue")
    TargetEntity changePrice(@MappingTarget TargetEntity targetEntity, Long ruleValue);
}
