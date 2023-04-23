package com.telegram.userBot.bot;

import static com.telegram.userBot.bot.product.ActionWithProduct.addProductMessage;
import static com.telegram.userBot.bot.product.ActionWithProduct.getAllProducts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.telegram.userBot.bot.event.EventsMessage;
import com.telegram.userBot.bot.process.Processing;
import com.telegram.userBot.bot.product.ActionWithProduct;
import com.telegram.userBot.constant.LoggerConst;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.telegram.userBot.client.LamodaClient;
import com.telegram.userBot.client.ScraperClient;
import com.telegram.userBot.config.property.TelegramProperty;

import com.telegram.userBot.mapper.TargetMapper;
import com.telegram.userBot.mapper.UserInfoMapper;
import com.telegram.userBot.repository.TargetRepository;
import com.telegram.userBot.repository.UserInfoRepository;

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
    private final static SendMessage message = new SendMessage();
    private final EventsMessage eventsWithMessage = new EventsMessage();
    private final ActionWithProduct actionWithProduct = new ActionWithProduct();
    private final Processing processing = new Processing();

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
            log.info(LoggerConst.COMMAND_EXECUTED);
        } catch (TelegramApiException e) {
            log.error(LoggerConst.EXECUTED_FAILED);
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
                    processing.processingSetNameState(chatId, messageText);
                    break;
                case SET_RULE:
                    processing.processingSetRuleState(chatId, messageText);
                    break;
                case SET_ARTICLE:
                    processing.processingSetArticleState(chatId, messageText);
                    break;
                case SET_CHAT_ID:
                    break;
                case END_ASK_USER_INFO:
                    processing.processingEndAskUserInfoState(chatId, messageText);
                    break;
                default:
                    processingDefaultState(chatId, messageText);
                    break;
            }
            message.setChatId(chatId);

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            message.setChatId(chatId);
            switch (callbackData) {
                case "CHAT_ID":
                    eventsWithMessage.enterChatIdMessage(chatId);
                break;
            }

        }

    }

    private void processingDefaultState(long chatId, String messageText) {
        switch (messageText) {
            case "/start":
                eventsWithMessage.startMessage(chatId);
                break;
            case "/help":
                eventsWithMessage.helpMessage(chatId);
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
//                    break
            default:
                eventsWithMessage.defaultMessage(chatId);
                break;
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

