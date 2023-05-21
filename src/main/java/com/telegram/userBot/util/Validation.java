package com.telegram.userBot.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class Validation {
    public static boolean validateChatIdMessage(String messageText) {
        try {
            Long.parseLong(messageText);
            return true;
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public static boolean validateRule(String messageText) {
        try {
            Long.parseLong(messageText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validatePidAndPrice(String messageText) {
        String[] words = messageText.split(",");
        if (words.length != 2) {
            return false;
        }
        try {
            Long.parseLong(words[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
