package com.telegram.userBot;

import static com.telegram.userBot.dto.enums.BotState.SET_ARTICLE;
import static com.telegram.userBot.util.MessageConstant.ADD_PRODUCT;
import static com.telegram.userBot.util.MessageConstant.DEFAULT_MESSAGE;
import static com.telegram.userBot.util.MessageConstant.END_ASK;
import static com.telegram.userBot.util.MessageConstant.END_RULE_INFO_BAD;
import static com.telegram.userBot.util.MessageConstant.END_RULE_INFO_GOOD;
import static com.telegram.userBot.util.MessageConstant.ENTER_CHAT_ID;
import static com.telegram.userBot.util.MessageConstant.ERROR_CHAT_ID_FORMAT;
import static com.telegram.userBot.util.MessageConstant.ERROR_NOT_USER_INFO;
import static com.telegram.userBot.util.MessageConstant.ERROR_PID_FORMAT;
import static com.telegram.userBot.util.MessageConstant.ERROR_RULE_FORMAT;
import static com.telegram.userBot.util.MessageConstant.HELP_MESSAGE;
import static com.telegram.userBot.util.MessageConstant.SET_NAME;
import static com.telegram.userBot.util.MessageConstant.SET_RULE_VALUE;
import static com.telegram.userBot.util.MessageConstant.START_MESSAGE;
import static com.telegram.userBot.util.Validation.validateChatIdMessage;
import static com.telegram.userBot.util.Validation.validateRuleAndChatId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.telegram.userBot.command.CommandDictionary;
import com.telegram.userBot.dto.enums.BotState;
import com.telegram.userBot.dto.enums.TargetState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.telegram.userBot.client.LamodaClient;
import com.telegram.userBot.client.ScraperClient;
import com.telegram.userBot.config.property.TelegramProperty;
import com.telegram.userBot.dto.TargetDTO;
import com.telegram.userBot.dto.TargetRequest;
import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.TargetEntity;
import com.telegram.userBot.entity.UserInfoEntity;
import com.telegram.userBot.mapper.TargetMapper;
import com.telegram.userBot.mapper.UserInfoMapper;
import com.telegram.userBot.repository.TargetRepository;
import com.telegram.userBot.repository.UserInfoRepository;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {
    private final TelegramProperty config;
    private final UserInfoRepository userInfoRepository;
    private final TargetRepository targetRepository;
    private final UserInfoMapper userInfoMapper;
    private final TargetMapper targetMapper;
    private final LamodaClient lamodaClient;
    private final ScraperClient scraperClient;
    private final CommandDictionary commandDictionary;

    private Map<Long, BotState> cacheBotState = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            this.execute(new SetMyCommands(
                commandDictionary.getCommandList(),
                new BotCommandScopeDefault(),
                null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (!cacheBotState.containsKey(chatId)) {
                cacheBotState.put(chatId, BotState.DEFAULT);
            }
            BotState state = cacheBotState.get(chatId);
            switch (state) {
                case SET_NAME -> processingSetNameState(chatId, messageText);
                case SET_RULE -> processingSetRuleState(chatId, messageText);
                case SET_ARTICLE -> processingSetArticleState(chatId, messageText);
                case SET_CHAT_ID, END_ASK_USER_INFO -> processingEndAskUserInfoState(chatId, messageText);
                default -> processingDefaultState(chatId, messageText);
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);


        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            if (callbackData.equals("CHAT_ID")) {
                enterChatIdMessage(chatId);
            }

        }

    }

    private void processingSetRuleState(long chatId, String messageText) {
        if (validateRuleAndChatId(messageText)) {
            String[] words = messageText.split(",");
//            userInfoRepository.save(userInfoRepository.findByUserIdAndChatId())
            UserInfoEntity userEntity = userInfoRepository.findByUserIdAndChatId(chatId, Long.parseLong(words[1]));
            if (userEntity == null) {
                errorFormatRuleMessage(chatId);
            }
            TargetEntity targetEntity = targetMapper.updateEntity(
                    targetRepository.findByUserId(
                            userEntity.getUuid()),
                            TargetState.ACTIVE.name(),
                            Long.parseLong(words[0]));

            targetRepository.save(targetEntity);

            TargetRequest targetRequest = TargetRequest.builder()
                .targetUUID(targetEntity.getUuid())
                .productId(targetEntity.getProductId())
                .userId(targetEntity.getUserId().toString())
                .build();

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            try {
                if (scraperClient.addProduct(targetRequest).getStatusCode().is2xxSuccessful()) {
                    message.setText(END_RULE_INFO_GOOD);
                    cacheBotState.put(chatId, BotState.DEFAULT);
                } else {
                    message.setText(END_RULE_INFO_BAD);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                message.setText(END_RULE_INFO_BAD);
            }

            executeMessage(message);
        } else {
            errorFormatRuleMessage(chatId);
        }
    }

    private void errorFormatRuleMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ERROR_RULE_FORMAT);
        executeMessage(message);
    }

    private void processingSetArticleState(long chatId, String messageText) {
        if (validatePid(messageText)) {
            TargetDTO targetDTO = new TargetDTO();
            targetDTO.setProductId(messageText);
            UserInfoEntity userEntity = userInfoRepository.findByUserId(chatId);
            if (userEntity == null) {
                errorNotUserInfo(chatId);
                return;
            }
            targetDTO.setUserId(userEntity.getUuid());
            TargetEntity ruleInfoEntity = targetMapper.toNewEntity(targetDTO);
            targetRepository.save(ruleInfoEntity);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(SET_RULE_VALUE);

            cacheBotState.put(chatId, BotState.SET_RULE);

            executeMessage(message);
        } else {
            errorFormatPIDMessage(chatId);
        }
    }

    private void errorNotUserInfo(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ERROR_NOT_USER_INFO);
        cacheBotState.put(chatId, BotState.SET_NAME);
        executeMessage(message);
    }

    private void errorFormatPIDMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ERROR_PID_FORMAT);
        executeMessage(message);
    }

    private boolean validatePid(String messageText) {
        ResponseEntity<Void> productData;
        try {
            productData = lamodaClient.getProductData(messageText);
        } catch (FeignException e) {
            log.error(e.getMessage());
            return false;
        }
        return productData.getStatusCode().value() == 200;
    }


    private void errorFormatChatIdMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ERROR_CHAT_ID_FORMAT);
        executeMessage(message);
    }


    private void startMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(START_MESSAGE);
        executeMessage(message);
    }

    private void defaultMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(DEFAULT_MESSAGE);
        executeMessage(message);
    }

    private void enterChatIdMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ENTER_CHAT_ID);
        executeMessage(message);
        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    private void enterNameMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ENTER_CHAT_ID);
        executeMessage(message);
        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    public void helpMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(HELP_MESSAGE);
//        cacheBotState.put(chatId, BotState.HELP);
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var setChatId = new InlineKeyboardButton();

        setChatId.setText("Ввести chat id");
        setChatId.setCallbackData("CHAT_ID");


        rowInLine.add(setChatId);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void processingEndAskUserInfoState(long chatId, String messageText) {
        UserInfoEntity userInfoEntity = userInfoMapper.updateEntity(userInfoRepository.findByUserId(chatId), messageText);
        userInfoRepository.save(userInfoEntity);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(END_ASK);
        cacheBotState.put(chatId, BotState.DEFAULT);
        executeMessage(message);
    }

    private void processingDefaultState(long chatId, String messageText) {
        switch (messageText) {
            case "/start" -> startMessage(chatId);
            case "/help" -> helpMessage(chatId);
            case "/addproduct" -> addProductMessage(chatId);
            case "/getall" -> getAllProducts(chatId);

//                case "/deleteProduct" :
//                    message.setText();
//                    break;
//                case "stopTrack" :
//                    message.setText();
//                    break;
//                case "/starTrack" :
//                    message.setText();
//                case "/setName" :
//                    message.setText();
//                    break;
//                case "/setPrice":
//                    message.setText();
//                    break;
            default -> defaultMessage(chatId);
        }
    }

    private void getAllProducts(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        StringBuilder result = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        targetRepository.findAll().forEach(
                target -> {
                    result.append(index)
                        .append(" SKU: ")
                        .append(target.getProductId())
                        .append(" PRICE: ")
                        .append(target.getRuleValue())
                        .append(" STATE: ")
                        .append(target.getState())
                        .append("\n");
                    index.incrementAndGet();
                }
        );
        message.setText(result.toString());
        executeMessage(message);
    }

    private void addProductMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ADD_PRODUCT);
        cacheBotState.put(chatId, SET_ARTICLE);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException();
        }
    }

    private void processingSetNameState(long chatId, String messageText) {
        if (validateChatIdMessage(messageText)) {
            UserInfoDTO userDTO = new UserInfoDTO();
            userDTO.setChatId(Long.parseLong(messageText));
            userDTO.setUserId(chatId);

            UserInfoEntity userInfoEntity = userInfoMapper.toNewEntity(userDTO);
            userInfoRepository.save(userInfoEntity);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(SET_NAME);

            cacheBotState.put(chatId, BotState.END_ASK_USER_INFO);

            executeMessage(message);
        } else {
            errorFormatChatIdMessage(chatId);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}

