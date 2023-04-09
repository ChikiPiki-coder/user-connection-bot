package com.telegram.userBot;

import static com.telegram.userBot.constant.MessageConstant.DEFAULT_MESSAGE;
import static com.telegram.userBot.constant.MessageConstant.END_ASK;
import static com.telegram.userBot.constant.MessageConstant.ENTER_CHAT_ID;
import static com.telegram.userBot.constant.MessageConstant.ERROR_CHAT_ID_FORMAT;
import static com.telegram.userBot.constant.MessageConstant.HELP_MESSAGE;
import static com.telegram.userBot.constant.MessageConstant.SET_NAME;
import static com.telegram.userBot.constant.MessageConstant.START_MESSAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.telegram.userBot.config.property.TelegramProperty;
import com.telegram.userBot.dto.UserInfoDTO;
import com.telegram.userBot.entity.UserInfoEntity;
import com.telegram.userBot.mapper.RuleInfoMapper;
import com.telegram.userBot.mapper.UserInfoMapper;
import com.telegram.userBot.repository.RuleInfoRepository;
import com.telegram.userBot.repository.UserInfoRepository;

@Component
public class Bot extends TelegramLongPollingBot {
    private final TelegramProperty config;
    private final UserInfoRepository userInfoRepository;
    private final RuleInfoRepository ruleInfoRepository;

    private final UserInfoMapper userInfoMapper;

    private final RuleInfoMapper ruleInfoMapper;

    private Map<Long, BotState> cacheBotState = new HashMap<>();

    public Bot(TelegramProperty config,
               UserInfoRepository userInfoRepository,
               RuleInfoRepository ruleInfoRepository,
               RuleInfoMapper ruleInfoMapper,
               UserInfoMapper userInfoMapper) {
        this.userInfoRepository = userInfoRepository;
        this.ruleInfoRepository = ruleInfoRepository;
        this.ruleInfoMapper = ruleInfoMapper;
        this.userInfoMapper = userInfoMapper;

        this.config = config;

        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "Информация о боте"));
        botCommandList.add(new BotCommand("/help", "Инструкция как пользоваться"));
        botCommandList.add(new BotCommand("/addproduct", "Добавить новый товар для отслеживания"));
        botCommandList.add(new BotCommand("/deleteproduct", "Удалить товар из списка отслеживаемых"));
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
                case SET_ARTICLE:
                case SET_CHAT_ID:
                case END_ASK_USER_INFO:
                    processingEndAskUserInfoState(chatId, messageText);
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
        UserInfoDTO userDTO = new UserInfoDTO();
        userDTO.setUserName(messageText);

        UserInfoEntity userInfoEntity = userInfoMapper.toNewEntity(userDTO);
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
//                case "/addProduct" :
//                    message.setText();
//                    break;
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

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException();
        }
    }

    private boolean validateMessage(long chatId, String messageText) {
        try {
            Long.parseLong(messageText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private void processingSetNameState(long chatId, String messageText) {
        if (validateMessage(chatId, messageText)) {
            UserInfoDTO userDTO = new UserInfoDTO();
            userDTO.setChatId(Long.parseLong(messageText));

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

