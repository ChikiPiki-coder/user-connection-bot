package com.telegram.userBot.util;

public class MessageConstant {
    public static final String START_MESSAGE = "Добро пожаловать. Я Бот для отслеживания изменений цен товаров " +
            "на сайте lamoda.ru. Воспользуйтесь командой /help , чтобы получить подробную " +
            "инструкцию как мной пользоваться";
    public static final String HELP_MESSAGE = "Что бы бот работал корректно, необходимо выполнить ряд простых шагов \n" +
            "Не переживай, ничего сложного не будет, главное все делать по шагам которые написаны ниже \n " +
            "1. Перед нажатием на кнопку ниже, тебе надо создать телеграмм беседу. \n" +
            "2. В нее надо добавить двух ботов, \n   1 - @RawDataBot (https://t.me/RawDataBot) \n" +
            "   2 - https://t.me/MessServiceTest1Bot \n" +
            "3. Как только ты добавишь первого бота, он напишет большое сообщение " +
            "в котором будет раздел \" chat \", а в нем будет \" id \" \n" +
            "Надо скопировать это число, без пробелов и лишних символов \n" +
            "4. После всех манипуляций описанных выше, тыкай кнопку ниже. \n ";
    public static final String ADD_PRODUCT = "Для добавления продукта в список отслеживаемых," +
            " введите пожалуйста артикул товара. \n";
    public static final String DELETE_PRODUCT = "Для удаления продукта из списка отслеживаемых," +
            " введите пожалуйста артикул товара.";
    public static final String STOP_TRACK = "Для остановки отслеживания продукта," +
            " введите пожалуйста артикул товара.";
    public static final String START_TRACK = "Для возобновления отслеживания продукта," +
            " введите пожалуйста артикул товара.";
    public static final String CHANGE_PRICE = "Для изменения отслеживаемой цены продукта," +
            " введите пожалуйста артикул товара.";
    public static final String ENTER_CHAT_ID = "Введите chat_id пожалуйста \n" +
            "Где взять chat_id можно посмотреть в разделе Help";
    public static final String DEFAULT_MESSAGE = "Прости, я не знаю такой команды, воспользуйтесь командой /help";
    public static final String ERROR_CHAT_ID_FORMAT = "Неправильно введен chatId. Недопустимые символы. \n" +
            "ChatId - число от -9 223 372 036 854 775 808 до 9 223 372 036 854 775 807. Попробуйте еще раз)";
    public static final String SET_NAME = "Введите свое имя пожалуйста, чтобы бот общался с вами не обезличенно.";
    public static final String SET_RULE_VALUE = "Введите пожалуйста цену.";
    public static final String END_ASK = "Супер. Теперь ты можешь использовать весь функционал. Тыкай вкладку меню и вперед";
    public static final String END_DELETING = "Продукт удален из списка опрашиваемых";
    public static final String ERROR_PID_FORMAT = "Неправильно введен Артикул товара. Такого товара нет. Попробуйте еще раз)";
    public static final String ERROR_RULE_FORMAT = "Неправильно введен артикул или цена. Попробуйте еще раз)";
    public static final String END_RULE_INFO_GOOD = "Товар успешно добавлен в список отслеживаемых.\n" +
            "Как только цена изменится, бот вам сообщит";
    public static final String END_STOPPING_GOOD = "Отслеживания товара остановлено";
    public static final String END_CHANGE_PRICE = "Цена изменена.";
    public static final String END_STARTING_GOOD = "Отслеживания товара возобновлено";
    public static final String END_RULE_INFO_BAD = "Что то на сервере пошло не так. пык мык.\n Введите цену еще раз";
    public static final String ERROR_NOT_USER_INFO = "Вы не заполнили информацию о том в какой чат отправлять сообщения " +
            "и как к вам обращаться. Введите chatId)";
    public static final String SET_ARTICLE_AND_PRICE = "Введите пожалуйста артикул и новую цену через запятую в формате АРТИКУЛ,ЦЕНА.";

}
