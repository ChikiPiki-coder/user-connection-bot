package com.telegram.userBot;

import static com.telegram.userBot.dto.enums.BotState.DEFAULT;
import static com.telegram.userBot.dto.enums.BotState.SET_ARTICLE;
import static com.telegram.userBot.dto.enums.BotState.SET_RULE;
import static com.telegram.userBot.dto.enums.TargetState.ACTIVE;
import static com.telegram.userBot.util.CommonConstant.CHAT_ID_FIELD;
import static com.telegram.userBot.util.CommonConstant.COMMA_SPLITERATOR;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.telegram.userBot.command.CommandDictionary;
import com.telegram.userBot.dto.enums.BotState;
import com.telegram.userBot.mapper.RequestMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
    private final RequestMapper requestMapper;
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

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            cacheBotState.putIfAbsent(chatId, DEFAULT);
            BotState state = cacheBotState.get(chatId);

            switch (state) {
                case SET_NAME -> processingSetNameState(chatId, messageText);
                case SET_RULE -> processingSetRuleState(chatId, messageText);
                case SET_ARTICLE -> processingSetArticleState(chatId, messageText);
                case SET_CHAT_ID, END_ASK_USER_INFO -> processingEndAskUserInfoState(chatId, messageText);
                default -> processingDefaultState(chatId, messageText);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (CHAT_ID_FIELD.equals(callbackData)) {
                enterChatIdMessage(chatId);
            }
        }

    }

    private void processingSetRuleState(long chatId, String text) {
        List<String> input = List.of(text.split(COMMA_SPLITERATOR));

        if (StringUtils.isNumeric(input.get(0)) && StringUtils.isNumeric(input.get(1))) {
            Long price = Long.valueOf(input.get(0));
            Long inputChatId = Long.valueOf(input.get(1));

            Optional<UserInfoEntity> possibleUser = userInfoRepository.
                findByUserIdAndChatId(chatId, inputChatId);

            possibleUser.ifPresentOrElse((entity) -> {
                    TargetEntity targetEntity = targetRepository.findByUserId(entity.getUuid());
                    targetMapper.updateEntity(targetEntity, ACTIVE.toString(), price);

                    targetRepository.save(targetMapper.updateEntity(
                        targetRepository.findByUserId(entity.getUuid()), ACTIVE.name(), price));

                    SendMessage.SendMessageBuilder message = SendMessage.builder().chatId(chatId);
                    try {
                        if (scraperClient.addProduct(requestMapper.toRequest(targetEntity))
                            .getStatusCode().is2xxSuccessful()) {
                            message.text(END_RULE_INFO_GOOD);

                            cacheBotState.put(chatId, BotState.DEFAULT);
                            executeMessage(message.build());

                            return;
                        }

                        message.text(END_RULE_INFO_BAD);
                        executeMessage(message.build());
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        message.text(END_RULE_INFO_BAD);

                        executeMessage(message.build());
                    }
                },
                () -> errorFormatRuleMessage(chatId)
            );

            return;
        }

        errorFormatRuleMessage(chatId);
    }

    private void processingSetArticleState(long chatId, String input) {
        if (validatePid(input)) {
            TargetDTO.TargetDTOBuilder targetDTO = TargetDTO.builder()
                .productId(input);

            Optional<UserInfoEntity> possibleUserEntity = userInfoRepository.findByUserId(chatId);

            possibleUserEntity.ifPresentOrElse((entity) -> {
                    targetDTO.userId(entity.getUuid());
                    targetRepository.save(targetMapper.toNewEntity(targetDTO.build()));

                    cacheBotState.put(chatId, SET_RULE);

                    executeMessage(SendMessage.builder()
                        .chatId(chatId)
                        .text(SET_RULE_VALUE)
                        .build());
                },
                () -> errorNotUserInfo(chatId)
            );

            return;
        }

        errorFormatPIDMessage(chatId);
    }

    public void helpMessage(long chatId) {
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var setChatId = new InlineKeyboardButton();

        setChatId.setText("Ввести chat id");
        setChatId.setCallbackData(CHAT_ID_FIELD);

        rowInLine.add(setChatId);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);

        executeMessage(SendMessage.builder()
            .chatId(chatId).text(HELP_MESSAGE)
            .replyMarkup(markupInLine)
            .build());
    }

    private void processingEndAskUserInfoState(long chatId, String messageText) {
        Optional<UserInfoEntity> possibleUserEntity = userInfoRepository.findByUserId(chatId);

        possibleUserEntity.ifPresent((entity) -> {
            userInfoRepository.save(userInfoMapper.updateEntity(entity, messageText));

            cacheBotState.put(chatId, DEFAULT);
            executeMessage(SendMessage.builder().chatId(chatId).text(END_ASK).build());
        });
    }

    private void processingDefaultState(long chatId, String messageText) {
        switch (messageText) {
            case "/start" -> startMessage(chatId);
            case "/help" -> helpMessage(chatId);
            case "/addproduct" -> addProductMessage(chatId);
            case "/getall" -> getAllProducts(chatId);

            default -> defaultMessage(chatId);
        }
    }

    private void getAllProducts(long chatId) {
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

        executeMessage(SendMessage.builder()
            .chatId(chatId).text(result.toString())
            .build());
    }

    private void addProductMessage(Long chatId) {
        cacheBotState.put(chatId, SET_ARTICLE);
        executeMessage(SendMessage.builder()
            .text(ADD_PRODUCT).chatId(chatId)
            .build());
    }

    private void processingSetNameState(long chatId, String input) {
        if (StringUtils.isNumeric(input)) {
            UserInfoEntity userInfoEntity = userInfoMapper.toNewEntity(UserInfoDTO.builder()
                .userId(chatId)
                .chatId(Long.parseLong(input))
                .build());
            userInfoRepository.save(userInfoEntity);

            cacheBotState.put(chatId, BotState.END_ASK_USER_INFO);

            executeMessage(SendMessage.builder()
                .chatId(chatId).text(SET_NAME)
                .build());

            return;
        }

        errorFormatChatIdMessage(chatId);
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

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    // Utility executor message methods

    private void errorFormatRuleMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(ERROR_RULE_FORMAT)
            .build());
    }


    private void errorFormatChatIdMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(ERROR_CHAT_ID_FORMAT)
            .build());
    }


    private void startMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(START_MESSAGE)
            .build());
    }

    private void defaultMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(DEFAULT_MESSAGE)
            .build());
    }

    private void enterChatIdMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(ENTER_CHAT_ID)
            .build());

        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    private void errorNotUserInfo(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(ERROR_NOT_USER_INFO)
            .build());

        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    private void errorFormatPIDMessage(long chatId) {
        executeMessage(SendMessage.builder()
            .chatId(chatId)
            .text(ERROR_PID_FORMAT)
            .build());
    }
}

