package com.telegram.userBot.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.UserInfoEntity;

@Mapper(componentModel = "spring", imports = {UUID.class})
public interface UserInfoMapper {

    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    UserInfoEntity toNewEntity(UserInfoDTO userInfoDTO);

    @Mapping(target = "userName", source = "userName")
    UserInfoEntity updateEntity(@MappingTarget UserInfoEntity userInfoEntity, String userName);

}
