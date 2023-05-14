package com.telegram.userBot;

import static com.telegram.userBot.BotState.SET_ARTICLE;
import static com.telegram.userBot.constant.MessageConstant.ADD_PRODUCT;
import static com.telegram.userBot.constant.MessageConstant.DEFAULT_MESSAGE;
import static com.telegram.userBot.constant.MessageConstant.END_ASK;
import static com.telegram.userBot.constant.MessageConstant.END_RULE_INFO_BAD;
import static com.telegram.userBot.constant.MessageConstant.END_RULE_INFO_GOOD;
import static com.telegram.userBot.constant.MessageConstant.ENTER_CHAT_ID;
import static com.telegram.userBot.constant.MessageConstant.ERROR_CHAT_ID_FORMAT;
import static com.telegram.userBot.constant.MessageConstant.ERROR_NOT_USER_INFO;
import static com.telegram.userBot.constant.MessageConstant.ERROR_PID_FORMAT;
import static com.telegram.userBot.constant.MessageConstant.ERROR_RULE_FORMAT;
import static com.telegram.userBot.constant.MessageConstant.HELP_MESSAGE;
import static com.telegram.userBot.constant.MessageConstant.SET_NAME;
import static com.telegram.userBot.constant.MessageConstant.SET_RULE_VALUE;
import static com.telegram.userBot.constant.MessageConstant.START_MESSAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
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
public class Bot extends TelegramLongPollingBot {
    private final TelegramProperty config;
    private final UserInfoRepository userInfoRepository;
    private final TargetRepository targetRepository;
    private final UserInfoMapper userInfoMapper;
    private final TargetMapper targetMapper;
    private final LamodaClient lamodaClient;
    private final ScraperClient scraperClient;


    private Map<Long, BotState> cacheBotState = new HashMap<>();

    public Bot(TelegramProperty config,
               UserInfoRepository userInfoRepository,
               TargetRepository targetRepository,
               UserInfoMapper userInfoMapper,
               LamodaClient lamodaClient,
               ScraperClient scraperClient,
               TargetMapper targetMapper) {
        this.userInfoRepository = userInfoRepository;
        this.targetRepository = targetRepository;
        this.userInfoMapper = userInfoMapper;
        this.lamodaClient = lamodaClient;
        this.scraperClient = scraperClient;
        this.targetMapper = targetMapper;
        this.config = config;

        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "Информация о боте"));
        botCommandList.add(new BotCommand("/help", "Инструкция как пользоваться"));
        botCommandList.add(new BotCommand("/addproduct", "Добавить новый товар для отслеживания"));
        botCommandList.add(new BotCommand("/deleteproduct", "Удалить товар из списка отслеживаемых"));
        botCommandList.add(new BotCommand("/getall", "Получить список всех зарегистрированных заявок"));
        botCommandList.add(new BotCommand("/stoptrack", "Остановить отслеживаемый товар"));
        botCommandList.add(new BotCommand("/startrack", "Остановить отслеживаемый товар"));
        botCommandList.add(new BotCommand("/changeprice", "Поменять цену"));

        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
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
                case DEFAULT:
                    processingDefaultState(chatId, messageText);
                    break;
                case SET_NAME:
                    processingSetNameState(chatId, messageText);
                    break;
                case SET_RULE:
                    processingSetRuleState(chatId, messageText);
                    break;
                case SET_ARTICLE:
                    processingSetArticleState(chatId, messageText);
                    break;
                case SET_CHAT_ID:
                case END_ASK_USER_INFO:
                    processingEndAskUserInfoState(chatId, messageText);
                    break;
                default:
                    processingDefaultState(chatId, messageText);
                    break;
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);


        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            switch (callbackData) {
                case "CHAT_ID":
                    enterChatIdMessage(chatId);
                    break;
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
                    "ACTIVE",
                    Long.parseLong(words[0])
            );

            targetRepository.save(targetEntity);

            TargetRequest targetRequest = new TargetRequest();
            targetRequest.setTargetUUID(targetEntity.getUuid());
            targetRequest.setProductId(targetEntity.getProductId());
            targetRequest.setUserId(targetEntity.getUserId().toString());

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

    private boolean validateRuleAndChatId(String messageText) {
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
            case "/start":
                startMessage(chatId);
                break;
            case "/help":
                helpMessage(chatId);
                break;
            case "/addproduct":
                addProductMessage(chatId);
                break;
            case "/getall":
                getAllProducts(chatId);
                break;
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
            default:
                defaultMessage(chatId);
                break;
        }
    }

    private void getAllProducts(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        StringBuilder result = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        targetRepository.findAll().forEach(
                target -> {
                    result.append(
                            index +
                            " SKU: " +
                            target.getProductId() +
                            " PRICE: " +
                            target.getRuleValue() +
                            " STATE: " +
                            target.getState() + "\n");
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

    private boolean validateChatIdMessage(String messageText) {
        try {
            Long.parseLong(messageText);
            return true;
        } catch (NumberFormatException e) {
            return false;
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

