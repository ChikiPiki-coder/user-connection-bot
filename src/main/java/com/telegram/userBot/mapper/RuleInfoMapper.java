package com.telegram.userBot.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.telegram.userBot.dto.RuleInfoDTO;
import com.telegram.userBot.entity.RuleInfoEntity;

@Mapper(componentModel = "spring", imports = { UUID.class})
public interface RuleInfoMapper {
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    RuleInfoEntity toNewEntity(RuleInfoDTO ruleInfoDTO);


}
