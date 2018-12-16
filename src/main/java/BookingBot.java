import org.processor.Command;
import org.telegram.shell.Bot;
import org.telegram.shell.Polling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.update.User;

import java.io.File;
import java.sql.*;
import java.util.*;

public class BookingBot implements Bot {
    private Heroku heroku = new Heroku();
    private Polling bot = new Polling(
            ***REMOVED***,
            "***REMOVED***",
            heroku,
            this
    );

    public BookingBot() {
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        BookingBot bot = new BookingBot();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(
                    bot
                            .bot
                            .setInitMap(
                                    new LinkedHashMap<String, String>() {
                                        {
                                            put("RESERVE", "NULL");
                                        }
                                    }
                            )
            );
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    public void onDocument(File file, User user, long chat) {

    }

    public void onCommand(Command command, User user, long chat_id) {
        if (command.getTitle().equals("admin-sql")) {
            bot.sendMessage(chat_id, heroku.url);
        }
    }

    public void onImages(File[] files, User user, long chat_id) {

    }

    public void onText(String text, User user, long chat_id) {
        Connection connection = heroku.getConnection();
        try {
            connection.createStatement().execute(text);
            bot.sendMessage(chat_id, "Выполнено!");
        } catch (SQLException e) {
            bot.sendMessage(chat_id, "Что-то пошло не так!");
            e.printStackTrace();
        }
    }
}
