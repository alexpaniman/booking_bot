package org.telegram.shell;

import org.database.DataBaseConnector;
import org.processor.Command;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.*;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.update.User;
import org.update.UsersList;

import java.io.File;
import java.util.*;

public class Polling extends TelegramLongPollingBot {

    private LinkedHashMap<String, String> initialMap = new LinkedHashMap<>();
    private UsersList usersList;
    private String bot_username;
    private String bot_token;
    private Bot bot;

    public Polling(String bot_username, String bot_token, DataBaseConnector dbc, Bot bot) {
        this.usersList = new UsersList(dbc);
        this.bot_username = bot_username;
        this.bot_token = bot_token;
        this.bot = bot;
        dbc.connect();
    }

    public Polling setUp(String bot_username, String bot_token, DataBaseConnector dbc, Bot bot) {
        Polling polling = new Polling(bot_username, bot_token, dbc, bot);
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(polling);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return polling;
    }

    public Polling setInitMap(LinkedHashMap linkedHashMap) {
        this.initialMap = linkedHashMap;
        return this;
    }

    private LinkedHashMap<String, String> getInitMapClone() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : initialMap.entrySet())
            map.put(entry.getKey(), entry.getValue());
        return map;
    }

    private User findUser(Message message) {
        org.telegram.telegrambots.api.objects.User telegram = message.getFrom();
        User user = usersList.findUser(telegram.getId());
        if (user == null) {
            user = new User(
                    telegram.getId(),
                    new long[]{message.getChatId()},
                    telegram.getFirstName(),
                    telegram.getLastName(),
                    telegram.getUserName(),
                    telegram.getLanguageCode(),
                    telegram.getBot(),
                    getInitMapClone(),
                    usersList
            );
            usersList.addUser(user);
        }
        return user;
    }

    private User findUser(CallbackQuery CQ) {
        org.telegram.telegrambots.api.objects.User telegram = CQ.getFrom();
        User user = usersList.findUser(telegram.getId());
        if (user == null) {
            user = new User(
                    telegram.getId(),
                    new long[]{CQ.getMessage().getChatId()},
                    telegram.getFirstName(),
                    telegram.getLastName(),
                    telegram.getUserName(),
                    telegram.getLanguageCode(),
                    telegram.getBot(),
                    getInitMapClone(),
                    usersList
            );
            usersList.addUser(user);
        }
        return user;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                Command command = Command.parseCommand("/", text);
                User user = findUser(message);
                if (command == null)
                    bot.onText(
                            text,
                            user,
                            message.getChatId()
                    );
                else
                    bot.onCommand(
                            command,
                            user,
                            message.getChatId()
                    );
            }
            if (message.hasPhoto()) {
                bot.onImages(
                        message
                                .getPhoto()
                                .stream()
                                .map(photoSize -> {
                                    try {
                                        return downloadFile(getFile(new GetFile().setFileId(photoSize.getFileId())));
                                    } catch (TelegramApiException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }).toArray(File[]::new),
                        findUser(message),
                        message.getChatId()
                );
            }
            if (message.hasDocument()) {
                try {
                    bot.onDocument(
                            downloadFile(
                                    getFile(
                                            new GetFile()
                                                    .setFileId(
                                                            message
                                                                    .getDocument()
                                                                    .getFileId()
                                                    )
                                    )
                            ),
                            findUser(message),
                            message.getChatId()
                    );
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            CallbackQuery CQ = update.getCallbackQuery();
            Command command = Command.parseCommand("/", data);
            User user = findUser(CQ);
            if (command == null)
                bot.onText(
                        data,
                        user,
                        CQ.getMessage().getChatId()
                );
            else
                bot.onCommand(
                        command,
                        user,
                        CQ.getMessage().getChatId()
                );
        }
    }

    @Override
    public String getBotUsername() {
        return bot_username;
    }

    @Override
    public String getBotToken() {
        return bot_token;
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);
    }

    public boolean sendMessage(long id, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);
        try {
            sendMessage(sendMessage);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMessage(long id, String text, InlineKeyboardMarkup IKM) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(IKM);
        sendMessage.setText(text);
        sendMessage.setChatId(id);
        sendMessage.enableMarkdown(true);
        try {
            sendMessage(sendMessage);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendDocument(long id, java.io.File document) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(id);
        sendDocument.setNewDocument(document);
        try {
            sendDocument(sendDocument);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendPhoto(long id, java.io.File photo) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(id);
        sendPhoto.setNewPhoto(photo);
        try {
            sendPhoto(sendPhoto);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendAudio(long id, java.io.File audio) {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setNewAudio(audio);
        sendAudio.setChatId(id);
        try {
            sendAudio(sendAudio);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendContact(long id, String first_name, String last_name, String phone_number) {
        SendContact sendContact = new SendContact();
        sendContact.setChatId(id);
        sendContact.setFirstName(first_name);
        sendContact.setLastName(last_name);
        sendContact.setPhoneNumber(phone_number);
        try {
            sendContact(sendContact);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendLocation(long id, float latitude, float longitude) {
        SendLocation sendLocation = new SendLocation();
        sendLocation.setLatitude(latitude);
        sendLocation.setLongitude(longitude);
        sendLocation.setChatId(id);
        try {
            sendLocation(sendLocation);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendSticker(long id, java.io.File file) {
        SendSticker sendSticker = new SendSticker();
        sendSticker.setChatId(id);
        sendSticker.setNewSticker(file);
        try {
            sendSticker(sendSticker);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendVoice(long id, java.io.File file) {
        SendVoice sendVoice = new SendVoice();
        sendVoice.setChatId(id);
        sendVoice.setNewVoice(file);
        try {
            sendVoice(sendVoice);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendVideo(long id, java.io.File file) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(id);
        sendVideo.setNewVideo(file);
        try {
            sendVideo(sendVideo);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public InlineKeyboardMarkup getIKM(int per_row, String[] buttons_name, Command[] buttons_command) {
        if (buttons_command.length != buttons_name.length)
            throw new IllegalArgumentException("IllegalArraysLength");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < buttons_name.length; i++) {
            String name = buttons_name[i];
            String command = buttons_command[i].build();
            row.add(new InlineKeyboardButton()
                    .setText(name)
                    .setCallbackData(command));
            if (row.size() >= per_row) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
            if (row.size() > 0 && i == buttons_name.length - 1) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getIKM(String[] buttons_name, String[] string_labels, int per_row) {
        Command[] commands = new Command[string_labels.length];
        for (int i = 0; i < commands.length; i++)
            commands[i] = new Command("label", string_labels[i], new ArrayList<>(), new ArrayList<>());
        return getIKM(per_row, buttons_name, commands);
    }
}
