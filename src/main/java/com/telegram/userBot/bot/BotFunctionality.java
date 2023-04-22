package com.telegram.userBot.bot;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import com.telegram.userBot.constant.LoggerConst;
import com.telegram.userBot.exception.BadExecuteException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BotFunctionality extends TelegramLongPollingBot  {

    private final static SendMessage message = new SendMessage();
    private static Map<Long, BotState> cacheBotState = new HashMap<>();

    @SneakyThrows
    public void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(LoggerConst.NOT_EXECUTE_METHOD);
            throw new BadExecuteException(LoggerConst.NOT_EXECUTE_METHOD);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public String getBotUsername() {
        return null;
    }
}
