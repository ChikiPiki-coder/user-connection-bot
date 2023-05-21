package com.telegram.userBot.command;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommandDictionary {
    private final List<BotCommand> commandList;

    @PostConstruct
    public void initCommands() {
        commandList.add(new BotCommand("/start", "Информация о боте"));
        commandList.add(new BotCommand("/help", "Инструкция как пользоваться"));
        commandList.add(new BotCommand("/addproduct", "Добавить новый товар для отслеживания"));
        commandList.add(new BotCommand("/deleteproduct", "Удалить товар из списка отслеживаемых"));
        commandList.add(new BotCommand("/getall", "Получить список всех зарегистрированных заявок"));
        commandList.add(new BotCommand("/stoptrack", "Остановить отслеживаемый товар"));
        commandList.add(new BotCommand("/starttrack", "Остановить отслеживаемый товар"));
        commandList.add(new BotCommand("/changeprice", "Поменять цену"));
    }

    public List<BotCommand> getCommandList() {
        return commandList;
    }
}
