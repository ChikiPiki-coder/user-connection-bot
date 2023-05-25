package com.telegram.userBot.mapper;

import com.telegram.userBot.dto.TargetRequest;
import com.telegram.userBot.entity.TargetEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring", imports = { UUID.class})
public interface RequestMapper {
    @Mapping(target = "targetUUID", source = "uuid")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "userId", source = "userId")
    TargetRequest toRequest(TargetEntity targetEntity);
}
