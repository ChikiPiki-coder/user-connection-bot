package com.telegram.userBot.bot.process;

import com.telegram.userBot.bot.BotFunctionality;
import com.telegram.userBot.bot.BotState;
import com.telegram.userBot.bot.event.EventsMessage;
import com.telegram.userBot.bot.validate.Validator;
import com.telegram.userBot.client.ScraperClient;
import com.telegram.userBot.constant.LoggerConst;
import com.telegram.userBot.dto.TargetDTO;
import com.telegram.userBot.dto.TargetRequest;
import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.TargetEntity;
import com.telegram.userBot.entity.UserInfoEntity;
import com.telegram.userBot.mapper.TargetMapper;
import com.telegram.userBot.mapper.UserInfoMapper;
import com.telegram.userBot.repository.TargetRepository;
import com.telegram.userBot.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static com.telegram.userBot.constant.MessageConstant.*;

@Slf4j
public class Processing {

    private final Validator validator = new Validator();
    private EventsMessage eventsWithMessage;
    private BotFunctionality botFunctionality;
    private  Map<Long, BotState> cacheBotState;
    private SendMessage message;
    private UserInfoRepository userInfoRepository;
    private TargetRepository targetRepository;
    private UserInfoMapper userInfoMapper;
    private TargetMapper targetMapper;
    private ScraperClient scraperClient;

    public void processingSetArticleState(long chatId, String messageText) {
        if (validator.validatePid(messageText)) {
            TargetDTO targetDTO = new TargetDTO();
            targetDTO.setProductId(messageText);
            UserInfoEntity userEntity = userInfoRepository.findByUserId(chatId);
            if (userEntity == null) {
                eventsWithMessage.errorNotUserInfo(chatId);
                log.error(LoggerConst.NOT_USER_INFO_ERROR);
                return;
            }
            targetDTO.setUserId(userEntity.getUuid());
            TargetEntity ruleInfoEntity = targetMapper.toNewEntity(targetDTO);
            targetRepository.save(ruleInfoEntity);

            message.setChatId(chatId);
            message.setText(SET_RULE_VALUE);

            cacheBotState.put(chatId, BotState.SET_RULE);

            botFunctionality.executeMessage(message);
        } else {
            eventsWithMessage.errorFormatPIDMessage(chatId);
        }
    }

    public void processingSetNameState(long chatId, String messageText) {
        if (validator.validateChatIdMessage(messageText)) {
            UserInfoDTO userDTO = new UserInfoDTO();
            userDTO.setChatId(Long.parseLong(messageText));
            userDTO.setUserId(chatId);

            UserInfoEntity userInfoEntity = userInfoMapper.toNewEntity(userDTO);
            userInfoRepository.save(userInfoEntity);

            message.setChatId(chatId);
            message.setText(SET_NAME);

            cacheBotState.put(chatId, BotState.END_ASK_USER_INFO);

            botFunctionality.executeMessage(message);
            log.info(LoggerConst.SET_NAME_STATE_PROCESS);
        } else {
            log.error(ERROR_CHAT_ID_FORMAT);
            eventsWithMessage.errorFormatChatIdMessage(chatId);
        }
    }

    public void processingSetRuleState(long chatId, String messageText) {
        if (validator.validateRuleAndChatId(messageText)) {
            String[] words = messageText.split(",");
            TargetEntity targetEntity = targetMapper.updateEntity(
                targetRepository.findByUserId(
                    userInfoRepository.findByUserIdAndChatId(chatId, Long.parseLong(words[1])).getUuid()),
                "ACTIVE", Long.parseLong(words[0])
            );

            targetRepository.save(targetEntity);

            TargetRequest targetRequest = new TargetRequest();
            targetRequest.setTargetUUID(targetEntity.getUuid());
            targetRequest.setProductId(targetEntity.getProductId());
            targetRequest.setUserId(targetEntity.getUserId().toString());

            message.setChatId(chatId);
            try {
                if (scraperClient.addProduct(targetRequest).getStatusCode().is2xxSuccessful()) {
                    message.setText(END_RULE_INFO_GOOD);
                    cacheBotState.put(chatId, BotState.DEFAULT);
                } else {
                    message.setText(END_RULE_INFO_BAD);
                    log.error(LoggerConst.ERROR_SOMETHING);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                message.setText(END_RULE_INFO_BAD);
            }

            botFunctionality.executeMessage(message);
        } else {
            eventsWithMessage.errorFormatRuleMessage(chatId);
        }
    }

    public void processingEndAskUserInfoState(long chatId, String messageText) {
        UserInfoEntity userInfoEntity = userInfoMapper
            .updateEntity(userInfoRepository
                .findByUserId(chatId), messageText);
        userInfoRepository.save(userInfoEntity);

        message.setChatId(chatId);
        message.setText(END_ASK);
        cacheBotState.put(chatId, BotState.DEFAULT);
        botFunctionality.executeMessage(message);
    }

}
