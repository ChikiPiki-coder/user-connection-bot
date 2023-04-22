package com.telegram.userBot.bot.product;

import com.telegram.userBot.bot.BotFunctionality;
import com.telegram.userBot.bot.BotState;
import com.telegram.userBot.constant.MessageConstant;
import com.telegram.userBot.repository.TargetRepository;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.telegram.userBot.bot.BotState.SET_ARTICLE;

public class ActionWithProduct {
    private static TargetRepository targetRepository;
    private static SendMessage message;
    private static BotFunctionality botFunctionality;
    private static Map<Long, BotState> cacheBotState;

    public static void addProductMessage(Long chatId) {
        message.setChatId(chatId);
        message.setText(MessageConstant.ADD_PRODUCT);
        cacheBotState.put(chatId, SET_ARTICLE);
        botFunctionality.executeMessage(message);
    }

    public static void getAllProducts(long chatId) {

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
        botFunctionality.executeMessage(message);
    }
}
