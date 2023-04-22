package com.telegram.userBot.bot.validate;

import com.telegram.userBot.bot.BotFunctionality;
import com.telegram.userBot.bot.BotState;
import com.telegram.userBot.bot.event.EventsWithMessage;
import com.telegram.userBot.client.LamodaClient;
import com.telegram.userBot.constant.LoggerConst;
import com.telegram.userBot.dto.TargetDTO;
import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.TargetEntity;
import com.telegram.userBot.entity.UserInfoEntity;
import com.telegram.userBot.mapper.TargetMapper;
import com.telegram.userBot.mapper.UserInfoMapper;
import com.telegram.userBot.repository.TargetRepository;
import com.telegram.userBot.repository.UserInfoRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static com.telegram.userBot.constant.MessageConstant.*;

@Slf4j
public class Validator {

    private LamodaClient lamodaClient;

    public boolean validatePid(String messageText) {
        ResponseEntity<Void> productData;
        try {
            productData = lamodaClient.getProductData(messageText);
        } catch (FeignException.FeignClientException e) {
            log.error(e.getMessage());
            return false;
        }
        log.info(LoggerConst.HTTP_200_OK);
        return productData.getStatusCode().value() == 200;
    }

    public boolean validateChatIdMessage(String messageText) {
        try {
            Long.parseLong(messageText);
            log.info(LoggerConst.LONG_MESSAGE_VALID);
            return true;
        } catch (NumberFormatException e) {
            log.error(LoggerConst.STRING_DOES_NOT_FORMAT);
            return false;
        }
    }

    public boolean validateRuleAndChatId(String messageText) {
        String[] words = messageText.split(",");
        if (words.length != 2) {
            return false;
        }

        try {
            Long.parseLong(words[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        try {
            Long.parseLong(words[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }



}
