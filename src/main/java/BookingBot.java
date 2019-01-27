import org.telegram.database.DataBaseConnector;
import org.telegram.database.URLConnector;
import org.telegram.processor.Command;
import org.telegram.script.Button;
import org.telegram.script.ScriptsCompiler;
import org.telegram.shell.Polling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.update.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BookingBot implements org.telegram.shell.Bot {
    private DataBaseConnector heroku = new URLConnector("***REMOVED***");
    private Polling bot = new Polling(
            ***REMOVED***,
            "***REMOVED***",
            heroku,
            this
    );

    public static void main(String[] args) {
        ApiContextInitializer.init();
        BookingBot bot = new BookingBot();
        Polling.setUp(
                bot
                        .bot
                        .setScripts(new ScriptsCompiler().compile("src/main/resources/booking.script"))
                        .setInitMap(new LinkedHashMap<String, String>() {{
                            put("status", "null");
                            put("admin", "false");
                        }})
        );
    }

    private Statement statement() throws SQLException {
        return heroku.getConnection().createStatement();
    }

    private class Item{
        private String subject;
        private String date;
        private String item;
        private String reserved;

        public Item(String subject, String date, String item, String reserved) {
            this.subject = subject;
            this.date = date;
            this.item = item;
            this.reserved = reserved;
        }

        String getSubject() {
            return subject;
        }

        String getDate() {
            return date;
        }

        String getItem() {
            return item;
        }

        String getReserved() {
            return reserved;
        }
    }

    private List<Item> find() {
        try {
            List<Item> its = new ArrayList<>();
            ResultSet rs = statement().executeQuery("SELECT * FROM items");
            while (rs.next())
                its.add(
                        new Item(rs.getString("Subject"), rs.getString("Date"), rs.getString("Item"), rs.getString("Reserved"))
                );
            return its;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<String> find(String subject, String date, String item, int result, boolean reserved, List<Item> its) {
        if (its == null) {
            String sql = "SELECT * FROM ITEMS WHERE ";
            StringJoiner sj = new StringJoiner(" AND ");
            if (subject != null)
                sj.add("SUBJECT = '" + subject + "'");
            if (date != null)
                sj.add("DATE = '" + date + "'");
            if (item != null)
                sj.add("ITEM = '" + item + "'");
            if (reserved)
                sj.add("RESERVED IS NULL");
            else
                sj.add("NOT RESERVED IS NULL");
            sql += sj.toString();
            try {
                ResultSet resultSet = statement().executeQuery(sql);
                List<String> res = new ArrayList<>();
                while (resultSet.next())
                    res.add(resultSet.getString(result == 0 ? "ITEM" : (result == 1 ? "SUBJECT" : (result == 2 ? "DATE" : "RESERVED"))));
                return res;
            } catch (Exception exc) {
                return new ArrayList<>();
            }
        } else {
            return its
                    .stream()
                    .filter(el -> subject == null || el.getSubject().equals(subject))
                    .filter(el -> date == null || el.getDate().equals(date))
                    .filter(el -> item == null || el.getItem().equals(item))
                    .filter(el -> reserved? el.getReserved() == null : el.getReserved() != null)
                    .map(el -> result == 0? el.getItem() : (result == 1? el.getSubject() : (result == 2? el.getDate() : el.getReserved())))
                    .collect(Collectors.toList());
        }
    }

    private String repeat(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i ++)
            sb.append(' ');
        return sb.toString();
    }

    @Override
    public void onCommand(Command command, User user, long chat_id) {
        String title = command.getTitle();
        if (title.equals("start")) {
            bot.activateScript(user, chat_id, "Start");
            return;
        }
        if (title.equals("reserve")) {
            bot.activateScript(user, chat_id, "Reserve");
            return;
        }
        try {
            if (title.equals("list")) {
                StringBuilder stringBuilder = new StringBuilder();
                List<Item> its = find();
                for (String subject : new String[]{"WorldHistory", "UkrainianHistory"}) {
                    stringBuilder.append("` ` `***").append(subject.equals("WorldHistory")? "Всемирная история:" : "История Украины:").append("\n***` ` `");
                    for (int j = 0; j < 2; j++) {
                        for (String date : find(subject, null, null, 2, j == 0, its)
                                .stream()
                                .distinct()
                                .sorted(Comparator.comparing(date -> {
                                    try {
                                        return new SimpleDateFormat("yyyy-mm-dd").parse(date);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    return new Date();
                                }))
                                .collect(Collectors.toList())) {
                            stringBuilder.append("\t` ` `___").append(date).append(":___` ` `\n");
                            for (String item : find(subject, date, null, 0, j == 0, its)) {
                                stringBuilder.append("\t\t\t").append(item).append(repeat(7 - item.length()));
                                for (String reserved : find(subject, date, item, 3, j == 0, its)) {
                                    if (reserved == null) {
                                        stringBuilder.append(" - свободен\n");
                                        break;
                                    }
                                    User curr = bot.getUsersList().findUser(Long.parseLong(reserved));
                                    stringBuilder.append(" - ").append(curr.getFirstName()).append(" ").append(curr.getLastName().equalsIgnoreCase("null") ? "" : curr.getLastName()).append("\n");
                                }
                            }
                        }
                        if (j == 0)
                            stringBuilder.append("\n");
                    }
                    stringBuilder.append("\n\n");
                }
                bot.sendMessage(chat_id, "` ` `".concat(stringBuilder.toString()).concat("` ` `"));
                return;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (title.equals("add") && user.getArgument("admin").equals("t"))
            bot.activateScript(user, chat_id, "Add");
        if (title.equals("actions")) {
            if (user.getArgument("admin").equals("t"))
                bot.sendMessage(
                        chat_id,
                        "Действия:",
                        bot.inline(3, "/list", "/reserve", "/add")
                );
            else
                bot.sendMessage(
                        chat_id,
                        "Действия:",
                        bot.inline(2, "/list", "/reserve")
                );
        }
    }

    @Override
    public void onBuilderResponse(User user, long chat_id, String script, Map<String, String> varMap) {
        try {
            if (script.equals("Reserve")) {
                statement().execute("UPDATE ITEMS SET RESERVED = " + user.getId() + " WHERE RESERVED IS NULL AND DATE = '" + varMap.get("Date") + "' AND SUBJECT = '" + varMap.get("Subject") + "' AND ITEM = '" + varMap.get("Item") + "'");
                bot.sendMessage(chat_id, "Вы успешно забронировали " + varMap.get("Item") + " пункт по " + (varMap.get("Subject").equals("WorldHistory") ? "Всемирной истории" : "Истории Украины") + " " + varMap.get("Date") + " числа.");
                return;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (script.equals("Start")) {
            user.updateArgument("first_name", varMap.get("FirstName"));
            user.updateArgument("last_name", varMap.get("LastName"));
            return;
        }
        if (script.equals("Add")) {
            try {
                PreparedStatement ps = heroku.getConnection().prepareStatement("INSERT INTO ITEMS VALUES(?, '" + varMap.get("Subject") + "', '" + varMap.get("Date") + "', NULL)");
                List<String> items = subItems(varMap.get("Item"));
                if (items.size() == 0)
                    return;
                StringJoiner sj = new StringJoiner(", ");
                for (String item: items) {
                    ps.setString(1, item);
                    sj.add(item);
                    ps.addBatch();
                }
                ps.executeBatch();
                bot.sendMessage(chat_id, "Пункты: " + sj + " по " + (varMap.get("Subject").equals("WorldHistory")? "всемирной истории" : "истории Украины") + " " + varMap.get("Date") + " числа были успешно добавленны!");
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private List<String> forRegex(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        List<String> list = new ArrayList<>();
        while (matcher.find())
            list.add(str.substring(matcher.start(), matcher.end()));
        return list;
    }

    private List<String> subItems(String str) {
        List<String> items = new ArrayList<>();
        for (String paragraph: forRegex("§\\d+\\(.+?\\)", str)) {
            Pattern pattern = Pattern.compile("§\\d+");
            Matcher matcher = pattern.matcher(paragraph);
            if (!matcher.find())
                throw new IllegalArgumentException();
            String par = paragraph.substring(matcher.start(), matcher.end());
            String its = paragraph.substring(matcher.end() + 1, paragraph.length() - 1);
            String[] itemsArr = its.split(",");
            for (String itm: itemsArr) {
                String val = itm.trim();
                if (val.matches("\\d+ *- *\\d+")) {
                    List<String> list = forRegex("\\d+", val);
                    int first = Integer.parseInt(list.get(0));
                    int second = Integer.parseInt(list.get(1));
                    for (int i = Math.min(first, second); i <= Math.max(first, second); i ++)
                        items.add(par + "(" + i + ")");
                } else {
                    items.add(par + "(" + val + ")");
                }
            }
        }
        return items;
    }
    @Override
    public List<Button> setupDataVariable(String dataVar, User user, long chat_id, String script, Map<String, String> varMap) {
        List<Button> buttons = new ArrayList<>();
        try {
            if (script.equals("Reserve")) {
                if (    !varMap.get("Subject").equals("WorldHistory") &&
                        !varMap.get("Subject").equals("UkrainianHistory"))
                    return null;
                if (dataVar.equals("date")) {
                    ResultSet resultSet = statement().executeQuery("SELECT * FROM ITEMS WHERE CURRENT_DATE <= DATE AND SUBJECT = '" + varMap.get("Subject") + "' AND RESERVED IS NULL");
                    List<String> names = new ArrayList<>();
                    while (resultSet.next()) {
                        String date = resultSet.getString("DATE");
                        names.add(date);
                    }
                    buttons =
                            names
                                    .stream()
                                    .distinct()
                                    .map(name -> new Button(name, name))
                                    .collect(Collectors.toList());
                    if (buttons.size() == 0) {
                        bot.sendMessage(chat_id, "В ближайшее время не забронированных пунктов по этому предмету нет!");
                        return null;
                    }
                } else if (dataVar.equals("items")) {
                    try {
                        new SimpleDateFormat("yyyy-mm-dd").parse(varMap.get("Date"));
                    } catch (ParseException e) {
                        return null;
                    }
                    ResultSet resultSet = statement().executeQuery("SELECT * FROM ITEMS WHERE DATE = '" + varMap.get("Date") + "' AND SUBJECT = '" + varMap.get("Subject") + "' AND RESERVED IS NULL");
                    while (resultSet.next()) {
                        String item = resultSet.getString("ITEM");
                        buttons.add(new Button(item, item));
                    }
                    if (buttons.size() == 0) {
                        bot.sendMessage(chat_id, "В этот день не забронированных пунктов по этому предмету нет!");
                        return null;
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (buttons.size() == 0)
            return null;
        return buttons;
    }

    @Override
    public void onUpdate(Update update, User user, long chat_id) {
        if (update.hasCallbackQuery())
            bot.deleteMessage(chat_id, update.getCallbackQuery().getMessage().getMessageId());
    }
}
