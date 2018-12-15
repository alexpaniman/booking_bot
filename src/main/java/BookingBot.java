import org.processor.Command;
import org.telegram.shell.Bot;
import org.telegram.shell.Polling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.update.User;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            botsApi.registerBot(bot.bot);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    public void onDocument(File file, User user, long chat) {

    }

    public void onCommand(Command command, User user, long chat_id) {
        if (command.getTitle().equals("/admin-sql")) {
            if (command.containsParameter("@")) {
                try {
                    Connection connection = heroku.getConnection();
                    DatabaseMetaData DBMD = connection.getMetaData();
                    String[] types = {"TABLE"};
                    ResultSet table = DBMD.getTables(null, null, "%", types);
                    List<String> tables = new ArrayList<>();
                    while (table.next())
                        tables.add(table.getString("TABLE_NAME"));
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String DB : tables) {
                        ResultSet RS = connection.createStatement().executeQuery("SELECT * FROM " + DB);
                        ResultSetMetaData RSMD = RS.getMetaData();
                        stringBuilder.append(DB).append(":\n");
                        while (RS.next()) {
                            stringBuilder.append("\t\tValue :\n");
                            for (int i = 1; i <= RSMD.getColumnCount(); i++)
                                stringBuilder.append("\t\t\t\t").append(RSMD.getColumnName(i)).append(" = '").append(RS.getString(i)).append("'\n");
                        }
                    }
                    bot.sendMessage(chat_id, stringBuilder.toString());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onImages(File[] files, User user, long chat_id) {

    }

    public void onText(String text, User user, long chat_id) {

    }
}
