package org.telegram.shell;

import com.google.gson.Gson;
import org.telegram.database.DataBaseConnector;
import org.telegram.processor.Command;
import org.telegram.script.*;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.*;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.update.User;
import org.telegram.update.UsersList;

import java.io.File;
import java.util.*;

@SuppressWarnings({
        "unused",
        "WeakerAccess",
        "Duplicates",
        "deprecation",
        "UnusedReturnValue"
})
public class Polling extends TelegramLongPollingBot {
    private LinkedHashMap<String, String> initialMap = new LinkedHashMap<>();
    private UsersList usersList;
    private Scripts scripts;
    private String bot_username;
    private String bot_token;
    private Gson gson = new Gson();
    private Bot bot;

    public Polling(String bot_username, String bot_token, DataBaseConnector dbc, Bot bot) {
        this.usersList = new UsersList(dbc);
        this.bot_username = bot_username;
        this.bot_token = bot_token;
        this.bot = bot;
        dbc.connect();
    }

    public static Polling setUp(Polling polling) {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(polling);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return polling;
    }

    public static Polling build(String bot_username, String bot_token, DataBaseConnector dbc, Bot bot) {
        return new Polling(bot_username, bot_token, dbc, bot);
    }

    public Polling setInitMap(LinkedHashMap<String, String> linkedHashMap) {
        this.initialMap = linkedHashMap;
        return this;
    }

    public Polling setScripts(Scripts scripts) {
        this.scripts = scripts;
        return this;
    }

    public boolean activateScript(User user, long chat_id, String name) {
        if (scripts == null)
            return false;
        int index = -1;
        for (int i = 0; i < scripts.getScripts().size(); i++)
            if (scripts.getScripts().get(i).getTitle().equals(name)) {
                index = i;
                break;
            }
        if (index == -1)
            return false;
        Response response = new Response(index);
        stage(
                response,
                user,
                chat_id,
                scripts
                        .getScripts()
                        .get(index)
                        .getStages()
                        .get(0)
        );
        user.updateArgument("status", gson.toJson(response));
        return true;
    }

    public UsersList getUsersList() {
        return usersList;
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

    private void stage(Response response, User user, long chat_id, Stage next) {
        if (next instanceof VariableStage) {
            String dataVar = ((VariableStage) next).getDataVariable();
            List<Button> buttons = bot.setupDataVariable(
                    dataVar,
                    user,
                    chat_id,
                    scripts
                            .getScripts()
                            .get(response.getScript())
                            .getTitle(),
                    response.getVarMap()
            );
            if (buttons == null) {
                user.updateArgument("status", "null");
                return;
            }
            sendMessage(
                    chat_id,
                    next.getText(),
                    replyKeyboard(
                            next.getButtonsType(),
                            buttons
                                    .stream()
                                    .map(Button::getButton)
                                    .toArray(String[]::new),
                            buttons
                                    .stream()
                                    .map(Button::getResponse)
                                    .toArray(String[]::new),
                            next.getIndex()
                    )
            );
        } else if (next instanceof DataStage)
            sendMessage(
                    chat_id,
                    next.getText(),
                    replyKeyboard(
                            next.getButtonsType(),
                            ((DataStage) next)
                                    .getData()
                                    .stream()
                                    .map(Button::getButton)
                                    .toArray(String[]::new),
                            ((DataStage) next)
                                    .getData()
                                    .stream()
                                    .map(Button::getResponse)
                                    .toArray(String[]::new),
                            next.getIndex()
                    )
            );
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            User user = findUser(message);
            long chat_id = message.getChatId();
            if (!bot.onUpdate(update, user, chat_id)) {
                user.updateArgument("status", "null");
                return;
            }
            if (message.hasText()) {
                String text = message.getText();
                Command command = Command.parseCommand("/", text);
                String value = user.getArgument("status");
                if (value != null && !value.equalsIgnoreCase("NULL")) {
                    Response response = gson.fromJson(value, Response.class);
                    Stage curr = scripts
                            .getScripts()
                            .get(response.getScript())
                            .getStages()
                            .get(response.getStage());
                    Boolean test = curr.test(text);
                    if (test != null) {
                        if (!test) {
                            user.updateArgument("status", "null");
                            return;
                        }
                        response.getVarMap().put(curr.getVariable(), text);
                        if (scripts
                                .getScripts()
                                .get(response.getScript())
                                .getStages()
                                .size() <= response
                                .getStage() + 1) {
                            bot.onBuilderResponse(
                                    user,
                                    chat_id,
                                    scripts
                                            .getScripts()
                                            .get(response.getScript())
                                            .getTitle(),
                                    response.getVarMap()
                            );
                            user.updateArgument("status", "null");
                            return;
                        }
                        stage(
                                response,
                                user,
                                chat_id,
                                scripts
                                        .getScripts()
                                        .get(response.getScript())
                                        .getStages()
                                        .get(response.incrementStage())
                        );
                        user.updateArgument("status", gson.toJson(response));
                    }
                }
                if (command == null)
                    bot.onText(
                            text,
                            user,
                            chat_id
                    );
                else
                    bot.onCommand(
                            command,
                            user,
                            chat_id
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
                        user,
                        chat_id
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
                            user,
                            chat_id
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
            long chat_id = CQ.getMessage().getChatId();
            if (!bot.onUpdate(update, user, chat_id)) {
                user.updateArgument("status", "null");
                return;
            }
            String value = user.getArgument("status");
            if (value != null && !value.equalsIgnoreCase("NULL")) {
                Response response = gson.fromJson(value, Response.class);
                Script script = scripts
                        .getScripts()
                        .get(response.getScript());
                Stage curr = script
                        .getStages()
                        .get(response.getStage());
                Stage next = null;
                if (script.getStages().size() > response.getStage() + 1)
                    next = script
                            .getStages()
                            .get(response.incrementStage());
                if (curr instanceof DataStage)
                    response.addVarValue(
                            curr.getVariable(),
                            ((DataStage) curr)
                                    .getData()
                                    .stream()
                                    .map(Button::getResponse)
                                    .filter(resp -> resp.equals(data == null ? "null" : data))
                                    .findAny()
                                    .orElse(null)
                    );
                else if (curr instanceof VariableStage) {
                    String dataVar = ((VariableStage) curr).getDataVariable();
                    List<Button> buttons = bot.setupDataVariable(
                            dataVar,
                            user,
                            chat_id,
                            scripts
                                    .getScripts()
                                    .get(response.getScript())
                                    .getTitle(),
                            response.getVarMap()
                    );
                    if (buttons == null) {
                        user.updateArgument("status", "null");
                        return;
                    }
                    response.addVarValue(
                            curr.getVariable(),
                            buttons
                                    .stream()
                                    .map(Button::getResponse)
                                    .filter(resp -> resp.equals(data == null ? "null" : data))
                                    .findAny()
                                    .orElse(null)
                    );
                }
                if (response
                        .getVarMap()
                        .get(curr.getVariable())
                        .equals("null")) {
                    user.updateArgument("status", gson.toJson(response));
                    return;
                }
                if (next == null) {
                    bot.onBuilderResponse(
                            user,
                            chat_id,
                            script.getTitle(),
                            response.getVarMap()
                    );
                    user.updateArgument("status", "null");
                    return;
                }
                stage(
                        response,
                        user,
                        chat_id,
                        next
                );
                user.updateArgument("status", gson.toJson(response));
            }
            if (command == null)
                bot.onText(
                        data,
                        user,
                        chat_id
                );
            else
                bot.onCommand(
                        command,
                        user,
                        chat_id
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

    public boolean deleteMessage(long chat_id, long message_id) {
        DeleteMessage dm = new DeleteMessage()
                .setChatId(String.valueOf(chat_id))
                .setMessageId((int) message_id);
        try {
            return deleteMessage(dm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Integer sendMessage(long id, String text) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(id)
                .enableMarkdown(true)
                .setText(text);
        try {
            return sendMessage(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Integer sendMessage(long id, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage()
                .setReplyMarkup(replyKeyboard)
                .setText(text)
                .setChatId(id)
                .enableMarkdown(true);
        try {
            return sendMessage(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Integer sendMessage(long id, String text, int per_row, String... labels) {
        SendMessage sendMessage = new SendMessage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (String label : labels) {
            row.add(label);
            if (row.size() >= per_row) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        if (row.size() > 0)
            keyboard.add(row);
        ReplyKeyboardMarkup RKM = new ReplyKeyboardMarkup().setKeyboard(keyboard);
        sendMessage.setReplyMarkup(RKM)
                .setText(text)
                .setChatId(id)
                .enableMarkdown(true);
        try {
            return sendMessage(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean editMessage(long chat_id, int message_id, String text, InlineKeyboardMarkup rk) {
        EditMessageText emt = new EditMessageText()
                .enableMarkdown(true)
                .setText(text)
                .setChatId(chat_id)
                .setMessageId(message_id);
        if (rk != null)
            emt.setReplyMarkup(rk);
        try {
            editMessageText(emt);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean editMessage(long chat_id, int message_id, String text) {
        return editMessage(chat_id, message_id, text, null);
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

    public InlineKeyboardMarkup inlineKeyboardMarkup(String[] buttons_name, Command[] buttons_command, int per_row) {
        if (buttons_command.length != buttons_name.length)
            throw new IllegalArgumentException("IllegalArraysLength");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < buttons_name.length; i++) {
            String name = buttons_name[i];
            String command = buttons_command[i] == null ? "null" : buttons_command[i].build();
            row.add(
                    new InlineKeyboardButton()
                            .setText(name == null ? "null" : name)
                            .setCallbackData(command == null ? "null" : command)
            );
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

    public InlineKeyboardMarkup inlineKeyboardMarkup(String[] buttons_name, String[] string_labels, int per_row) {
        if (string_labels.length != buttons_name.length)
            throw new IllegalArgumentException("IllegalArraysLength");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < buttons_name.length; i++) {
            String name = buttons_name[i];
            String label = string_labels[i];
            row.add(
                    new InlineKeyboardButton()
                            .setText(name == null ? "null" : name)
                            .setCallbackData(label == null ? "null" : label)
            );
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

    public ReplyKeyboardMarkup replyKeyboardMarkup(String[] buttons_name, int per_row) {
        ReplyKeyboardMarkup RKM = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow KR = new KeyboardRow();
        for (int i = 0; i < buttons_name.length; i++) {
            String name = buttons_name[i];
            KR.add(
                    new KeyboardButton().setText(name)
            );
            if (KR.size() >= per_row) {
                keyboard.add(KR);
                KR = new KeyboardRow();
            }
            if (KR.size() > 0 && i == buttons_name.length - 1) {
                keyboard.add(KR);
                KR = new KeyboardRow();
            }
        }
        RKM.setKeyboard(keyboard);
        return RKM;
    }

    public ReplyKeyboard replyKeyboard(ButtonsType buttonsType, String[] buttons_name, String[] string_labels, int per_row) {
        if (buttonsType == null)
            return null;
        if (buttonsType.equals(ButtonsType.Inline))
            return inlineKeyboardMarkup(buttons_name, string_labels, per_row);
        if (buttonsType.equals(ButtonsType.Reply))
            return replyKeyboardMarkup(buttons_name, per_row);
        return null;
    }

    public InlineKeyboardMarkup inline(int perRow, String... buttons) {
        return inlineKeyboardMarkup(buttons, buttons, perRow);
    }

    public ReplyKeyboardMarkup reply(int perRow, String... buttons) {
        return replyKeyboardMarkup(buttons, perRow);
    }
}
