package com.telegram.userBot.bot.event;

import com.telegram.userBot.bot.BotFunctionality;
import com.telegram.userBot.bot.BotState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.telegram.userBot.constant.MessageConstant.*;

public class EventsMessage {

    private final static SendMessage message = new SendMessage();
    private static Map<Long, BotState> cacheBotState = new HashMap<>();
    private static BotFunctionality botFunctionality = new BotFunctionality();

    public void startMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(START_MESSAGE);
        botFunctionality.executeMessage(message);
    }

    public void errorFormatChatIdMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(ERROR_CHAT_ID_FORMAT);
        botFunctionality.executeMessage(message);
    }

    public void errorFormatRuleMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(ERROR_RULE_FORMAT);
        botFunctionality.executeMessage(message);
    }

    public void errorNotUserInfo(long chatId) {
        message.setChatId(chatId);
        message.setText(ERROR_NOT_USER_INFO);
        cacheBotState.put(chatId, BotState.SET_NAME);
        botFunctionality.executeMessage(message);
    }

    public void errorFormatPIDMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(ERROR_PID_FORMAT);
        botFunctionality.executeMessage(message);
    }

    public void defaultMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(DEFAULT_MESSAGE);
        botFunctionality.executeMessage(message);
    }

    public void enterChatIdMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(ENTER_CHAT_ID);
        botFunctionality.executeMessage(message);
        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    public void enterNameMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(ENTER_CHAT_ID);
        botFunctionality.executeMessage(message);
        cacheBotState.put(chatId, BotState.SET_NAME);
    }

    public void helpMessage(long chatId) {
        message.setChatId(chatId);
        message.setText(HELP_MESSAGE);
        /* cacheBotState.put(chatId, BotState.HELP);*/
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

        botFunctionality.executeMessage(message);
    }
}
