package com.example.telegrambot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class MenuHandler extends TelegramLongPollingBot {
    private String botToken = "5402739096:AAEVPIgL_RlJerECELXc0Qb5S7HMtu1OeaA";
    private String botUsername = "Zalupa12345_bot";
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

    }
}
