package com.telegram.userBot.config;

import com.telegram.userBot.constant.LoggerConst;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.telegram.userBot.bot.Bot;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final Bot bot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(bot);
            log.info(LoggerConst.BOT_WAS_REGISTRATION);
        } catch (TelegramApiException telegramApiException){
            System.out.println(telegramApiException.getMessage());
            log.error(LoggerConst.BOT_NOT_REGISTERED);
        }
    }
}
