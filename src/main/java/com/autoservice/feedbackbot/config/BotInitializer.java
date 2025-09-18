package com.autoservice.feedbackbot.config;


import com.autoservice.feedbackbot.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramBotsApi telegramBotsApi;
    private final TelegramBotService telegramBotService;

    @PostConstruct
    public void init() throws TelegramApiException {
        telegramBotsApi.registerBot(telegramBotService);
    }
}