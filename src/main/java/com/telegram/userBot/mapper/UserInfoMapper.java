package com.telegram.userBot.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.UserInfoEntity;

@Mapper(componentModel = "spring", imports = {UUID.class})
public interface UserInfoMapper {

    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    UserInfoEntity toNewEntity(UserInfoDTO userInfoDTO);


}
