package com.example.telegrambot;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ReplyHandler extends TelegramLongPollingBot {
    private String botToken = "5402739096:AAEVPIgL_RlJerECELXc0Qb5S7HMtu1OeaA";
    private String botUsername = "Zalupa12345_bot";
    private Map<String, ArrayList<File>> photosMap = new HashMap<>();

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/readytoconvert");
        row.add("/clearhistory");
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        if (update.getMessage().hasPhoto()) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            if (photos.size() >= 1) {
                String fotoId = photos.stream().sorted(Comparator.comparing(PhotoSize::getFileSize)
                                .reversed())
                        .findFirst()
                        .orElse(null)
                        .getFileId();
                if (photosMap.containsKey(update.getMessage().getChatId().toString())) {
                    ArrayList<File> list = photosMap.get(update.getMessage().getChatId().toString());
                    list.add(getFile(botToken, fotoId));
                    photosMap.put(update.getMessage().getChatId().toString(), list);
                    System.out.println(update.getMessage().getChatId().toString() + "a");
                } else {
                    ArrayList<File> list = new ArrayList<>();
                    list.add(getFile(botToken, fotoId));
                    photosMap.put(update.getMessage().getChatId().toString(), list);
                    System.out.println(update.getMessage().getChatId().toString() + "b");
                }
            }
        } else if (update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {
            SendMessage sendMessage = new SendMessage(update.getMessage().getChatId().toString(), "Okay, let`s get started\nSend me photos and then press /readytoconvert button");
//            sendMessage.setReplyMarkup(markup);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.getMessage().hasText() && (update.getMessage().getText().equals("/readytoconvert") || update.getMessage().getText().equals("/readyToConvert"))) {
            sendPdfInChat(photosMap, update.getMessage().getChatId().toString());
            photosMap.remove(update.getMessage().getChatId().toString());
        } else if (update.getMessage().getText().equals("/clearhistory") || update.getMessage().getText().equals("/clearHistory")) {
            if (photosMap.containsKey(update.getMessage().getChatId().toString())) {
                photosMap.remove(update.getMessage().getChatId().toString());
                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId().toString());
                message.setText("History clearing done");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId().toString());
                message.setText("Your history is empty");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public File getFile(String botToken, String fileId) {
        BufferedReader reader;
        HttpURLConnection con = null;
        String line;
        StringBuilder responseContent = new StringBuilder();
        String requestPath = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
        try {
            URL url = new URL(requestPath);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            int status = con.getResponseCode();
            if (status >= 300) {
                reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            con.disconnect();
        }
        try {
            File file = downloadFile(parse(responseContent.toString()));
            return file;
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String parse(String responseBody) {
        JSONObject object = new JSONObject(responseBody);
        JSONObject internalObject = (JSONObject) object.get("result");
        String result = (String) internalObject.get("file_path");
        return result;
    }

    public void sendPdfInChat(Map<String, ArrayList<File>> photos, String chatId) {
        try {
            if (photos.isEmpty()) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You haven`t uploaded any photo\n Please do it");
                execute(message);
            } else {
                ArrayList<File> list = photos.get(chatId);
                File file = File.createTempFile("zalupa", ".pdf");
                PDDocument document = new PDDocument();

                for (int i = 0; i < list.size(); i++) {
                    PDImageXObject image = PDImageXObject.createFromFileByContent(list.get(i), document);
                    document.addPage(new PDPage(new PDRectangle(image.getWidth(), image.getHeight())));
                    PDPage page = document.getPage(i);
                    PDPageContentStream pdPageContentStream = new PDPageContentStream(document, page);
                    pdPageContentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
                    pdPageContentStream.close();
                }
                document.save(file);
                document.close();
                SendDocument sendDocument = new SendDocument(chatId, new InputFile(file));
                execute(sendDocument);
                file.delete();
            }
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
