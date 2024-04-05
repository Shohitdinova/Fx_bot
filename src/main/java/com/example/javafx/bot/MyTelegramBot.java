package com.example.javafx.bot;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
            throw new TelegramBotException("Failed to send message");
        }
    }

    private void saveFileToStaticFolder(Document document) {
        String fileName = document.getFileName();
        try {
            File downloadedFile = downloadFile(document.getFileId(), fileName);
            moveFileToStaticFolder(downloadedFile);
        } catch (IOException | TelegramApiException e) {
            throw new TelegramBotException("Failed to save file to static folder");
        }
    }

    private File downloadFile(String fileId, String fileName) throws TelegramApiException {
        // Faylni yuklab olish so'rovi
        GetFile getFileRequest = new GetFile();
        getFileRequest.setFileId(fileId);

        // Telegram API orqali faylni yuklab olish
        org.telegram.telegrambots.meta.api.objects.File file;
        try {
            file = execute(getFileRequest);
        } catch (TelegramApiException e) {
            throw new TelegramBotException("Failed to download file from Telegram API");
        }

        // Yuklangan faylni URL sini olish
        String filePath = file.getFilePath();

        // Faylni yuklab olish uchun URL yaratish
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

        String downloadDirectory = "downloads";
        String saveFilePath = downloadDirectory + "/" + fileName;

        try {
            // Faylni yuklab olish
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                connection.disconnect();
            } else {
                throw new TelegramBotException("Failed to download file. HTTP error code: ");
            }
        } catch (IOException e) {
            throw new TelegramBotException("Failed to download file");
        }

        return new File(saveFilePath);
    }
    private void moveFileToStaticFolder(java.io.File file) throws IOException {
        Path staticFolder = Path.of("src/main/resources/static");
        Path destination = staticFolder.resolve(file.getName());
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
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
        public TelegramBotException(String message) {
            super(message);
        }
    }
}