import org.database.DataBaseConnector;
import org.database.URLConnector;
import org.processor.Command;
import org.telegram.shell.Bot;
import org.telegram.shell.Polling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.update.User;

import java.io.File;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class BookingBot implements Bot {
    private DataBaseConnector heroku = new URLConnector("***REMOVED***");
    private Polling bot = new Polling(
            ***REMOVED***,
            "***REMOVED***",
            heroku,
            this
    );

    public BookingBot() {
    }

    private void addItem(long id, String item, String date, String subject) {
        try {
            heroku.getConnection().createStatement().execute("INSERT INTO ITEMS VALUES('" + item + "', '" + subject + "', '" + date + "', " + id + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void reserve(String item, String date, String subject, long id) {
        try {
            heroku.getConnection().createStatement().execute("UPDATE ITEMS SET RESERVED = " + id + " WHERE ITEM = '" + item + "' AND DATE = '" + date + "' AND SUBJECT = '" + subject + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean reserved(String subject, String date, long id) {
        try {
            return heroku.getConnection().createStatement().executeQuery("SELECT * FROM ITEMS WHERE RESERVED = " + id + " AND SUBJECT = '" + subject + "' AND DATE = '" + date + "'").next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean reserved(String subject, String date, String item) {
        try {
            return heroku.getConnection().createStatement().executeQuery("SELECT * FROM ITEMS WHERE ITEM = " + item + " AND SUBJECT = '" + subject + "' AND DATE = '" + date + "' AND RESERVED = NULL").next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    private String[] dates(long id, String subject) {
        try {
            ResultSet rs = heroku.getConnection().createStatement().executeQuery("SELECT * FROM ITEMS WHERE SUBJECT = '" + subject + "'");
            List<String> dates = new ArrayList<>();
            while (rs.next())
                dates.add(rs.getString("DATE"));
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date currDate = new Date();
            dates =
                    dates
                            .stream()
                            .map(source -> {
                                try {
                                    return df.parse(source);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .filter(date -> date.after(currDate))
                            .map(df::format)
                            .filter(el -> !reserved(subject, el, id))
                            .collect(Collectors.toCollection(ArrayList::new));
            return dates.toArray(new String[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private String[] items(String subject, String date) {
        try {
            ResultSet rs = heroku.getConnection().createStatement().executeQuery("SELECT * FROM ITEMS WHERE SUBJECT = '" + subject + "' AND DATE = '" + date + "'");
            List<String> dates = new ArrayList<>();
            while (rs.next())
                dates.add(rs.getString("ITEM"));
            return dates.toArray(new String[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        BookingBot bot = new BookingBot();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(
                    bot
                            .bot
            );
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    public void onDocument(File file, User user, long chat) {

    }

    public void onCommand(Command command, User user, long chat_id) {
        switch (command.getTitle()) {
            case "actions":
                bot.sendMessage(
                        chat_id,
                        "Действия:",
                        bot.getIKM(
                                new String[]{
                                        "Забронировать пункт",
                                        "Посмотреть список пунктов"
                                },
                                new String[]{
                                        "/reserve",
                                        "/items"
                                },
                                1
                        )
                );
                break;
            case "reserve":
                if (command.numberOfParameters() == 0) {
                    bot.sendMessage(
                            chat_id,
                            "Выберите предмет:",
                            bot.getIKM(
                                    new String[]{
                                            "Всемирная история",
                                            "История Украины",
                                            "Юриспруденция"
                                    },
                                    new String[]{
                                            "/reserve --world_history",
                                            "/reserve --ukrainian_history",
                                            "/reserve --jurisprudence"
                                    },
                                    1
                            )
                    );
                } else {
                    if (command.numberOfArguments() == 0) {
                        String[] arr = dates(user.getId(), command.getParameter(0));
                        if (arr.length == 0) {
                            bot.sendMessage(chat_id, "В ближайшее время этого предмета не будет!");
                            return;
                        }
                        bot.sendMessage(
                                chat_id,
                                "Выберите дату:",
                                bot.getIKM(
                                        arr,
                                        Arrays
                                                .stream(arr)
                                                .map(el -> "/reserve --" + command.getParameter(0) + " \"" + el + "\"")
                                                .toArray(String[]::new),
                                        3
                                )
                        );
                    } else {
                        String[] arr = items(command.getParameter(0), command.getArgument(0));
                        if (arr.length == 0) {
                            bot.sendMessage(chat_id, "Пунктов по этому предмету в этот день нет!");
                            return;
                        }
                        String subject = command.getParameter(0);
                        String date = command.getArgument(0);
                        if (reserved(subject, date, user.getId())) {
                            bot.sendMessage(chat_id, "Вы уже бронировали пункт в этот день!");
                            return;
                        }
                        bot.sendMessage(
                                chat_id,
                                "Выберите пункт:",
                                bot.getIKM(
                                        arr,
                                        Arrays
                                                .stream(arr)
                                                .map(el -> "/book --" + command.getParameter(0) + " \"" + el + "\" " + "\"" + command.getArgument(0) + "\"")
                                                .toArray(String[]::new),
                                        3
                                )
                        );
                    }
                }
                break;
            case "book":
                String item = command.getArgument(0);
                String subject = command.getParameter(0);
                String date = command.getArgument(1);
                if (reserved(subject, date, user.getId())) {
                    bot.sendMessage(chat_id, "Вы уже бронировали пункт в этот день!");
                    return;
                }
                reserve(item, date, subject, user.getId());
                bot.sendMessage(chat_id, "Пункт успешно забронированн!");
                break;
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
            bot.sendMessage(chat_id, text);
            e.printStackTrace();
        }
    }
}
