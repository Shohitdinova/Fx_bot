package com.example.javafx.bot;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
@Component
@RequiredArgsConstructor
public class MyTelegramBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}")
    private String USERNAME_BOT;

    @Value("${telegram.bot.token}")
    private String TOKEN_BOT;

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText() && message.getText().equals("/start")) {
                sendMessage(message.getChatId(), "Welcome to the BOT \uD83D\uDE0A!");
            }
            if (message.hasDocument()) {
                saveFileToStaticFolder(message.getDocument());
            }
        }
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramBotException("Failed to send message", e);
        }
    }

    private void saveFileToStaticFolder(Document document) {
        String fileName = document.getFileName();
        try {
            java.io.File downloadedFile = downloadFile(document.getFileId(), fileName);
            moveFileToStaticFolder(downloadedFile);
        } catch (IOException | TelegramApiException e) {
            throw new TelegramBotException("Failed to save file to static folder", e);
        }
    }

    private java.io.File downloadFile(String fileId, String fileName) throws TelegramApiException {
        throw new UnsupportedOperationException("File download not implemented");
    }

    private void moveFileToStaticFolder(java.io.File file) throws IOException {
        Path staticFolder = Path.of("src/main/resources/static");
        Path destination = staticFolder.resolve(file.getName());
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        // Properly close resources
        Files.deleteIfExists(file.toPath());
    }

    @Override
    public String getBotUsername() {
        return USERNAME_BOT;
    }

    @Override
    public String getBotToken() {
        return TOKEN_BOT;
    }

    static class TelegramBotException extends RuntimeException {
        public TelegramBotException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}