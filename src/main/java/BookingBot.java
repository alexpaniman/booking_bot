import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.telegram.database.DataBaseConnector;
import org.telegram.database.URLConnector;
import org.telegram.processor.Command;
import org.telegram.script.*;
import org.telegram.shell.Bot;
import org.telegram.shell.Polling;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.update.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BookingBot implements Bot {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, String> alias;

    static {
        try {
            alias = gson.fromJson(
                    new FileReader("src/main/resources/alias.json"),
                    new TypeToken<Map<String, String>>() {}.getType()
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private DataBaseConnector heroku = new URLConnector("***REMOVED***");
    private Polling bot = new Polling(
            ***REMOVED***,
            "***REMOVED***",
            heroku,
            this
    );

    public static void main(String[] args) throws LexerException, ParseException {
        ApiContextInitializer.init();
        BookingBot bot = new BookingBot();
        Polling.setUp(
                bot
                        .bot
                        .setScripts(new ScriptsCompiler().compile("src/main/resources/booking.script"))
                        .setInitMap(new LinkedHashMap<String, String>() {{
                            put("status", "null");
                            put("admin", "false");
                            put("ban", "0");
                        }})
        );
    }

    private class Item {
        private String first_name;
        private String last_name;
        private String subject;
        private LocalDate date;
        private String item;
        private Long id;

        public Item(String first_name, String last_name, String subject, LocalDate date, String item, Long id) {
            this.first_name = first_name;
            this.last_name = last_name;
            this.subject = subject;
            this.date = date;
            this.item = item;
            this.id = id;
        }

        String getFirstName() {
            return first_name;
        }

        String getLastName() {
            return last_name;
        }

        String getSubject() {
            return subject;
        }

        String getItem() {
            return item;
        }

        LocalDate getDate() {
            return date;
        }

        Long getId() {
            return id;
        }
    }

    private Statement statement() throws SQLException {
        return heroku.getConnection().createStatement();
    }

    private List<Item> find() {
        try {
            List<Item> items = new ArrayList<>();
            ResultSet rs = statement().executeQuery("SELECT item, subject, date, id, first_name, last_name FROM items LEFT JOIN users ON users.id = items.reserved");
            while (rs.next())
                items.add(
                        new Item(
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("subject"),
                                LocalDate.parse(rs.getString("date"), dtf),
                                rs.getString("item"),
                                rs.getLong("id")
                        )
                );
            return items;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Item> find(String subject, LocalDate date, String item, boolean reserved, boolean actual, List<Item> items) {
        return items
                .stream()
                .filter(el -> !actual || el.getDate().compareTo(LocalDate.now()) >= 0)
                .filter(el -> subject == null || el.getSubject().equals(subject))
                .filter(el -> date == null || el.getDate().compareTo(date) == 0)
                .filter(el -> item == null || el.getItem().equals(item))
                .filter(el -> reserved ? el.getId() == 0 : el.getId() != 0)
                .collect(Collectors.toList());
    }

    private String repeat(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i++)
            sb.append(' ');
        return sb.toString();
    }

    private String list(boolean actual) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Item> its = find();
        for (String subject : new String[]{
                "WorldHistory",
                "UkrainianHistory"
        }) {
            stringBuilder
                    .append("`***")
                    .append(
                            subject
                                    .equals("WorldHistory") ?
                                    "Всемирная история:" : "История Украины:"
                    )
                    .append("***`\n");
            boolean contains = false;
            for (int j = 0; j < 2; j++) {
                List<Item> dates = find(subject, null, null, j == 0, actual, its);
                if (dates.size() != 0 && !contains)
                    contains = true;
                if (j == 1 && !contains)
                    stringBuilder.append("\t\tНичего!");
                for (LocalDate date : dates
                        .stream()
                        .sorted(Comparator.comparing(Item::getDate))
                        .map(Item::getDate)
                        .distinct()
                        .collect(Collectors.toList())) {
                    stringBuilder
                            .append("\t\t`___")
                            .append(dtf.format(date))
                            .append(":___`\n");
                    for (String item : find(subject, date, null, j == 0, actual, its)
                            .stream()
                            .map(Item::getItem)
                            .collect(Collectors.toList())) {
                        stringBuilder
                                .append("\t\t\t")
                                .append(item)
                                .append(repeat(7 - item.length()));
                        for (Item reserved : find(subject, date, item, j == 0, actual, its)) {
                            if (reserved.getId() == 0) {
                                if (reserved.getDate().compareTo(LocalDate.now()) >= 0)
                                    stringBuilder.append(" - свободен\n");
                                else
                                    stringBuilder.append(" - просрочен\n");
                                break;
                            }
                            stringBuilder
                                    .append(" - ")
                                    .append(
                                            reserved
                                                    .getFirstName()
                                                    ==
                                                    null ?
                                                    "" : reserved.getFirstName()
                                    )
                                    .append(" ")
                                    .append(
                                            reserved
                                                    .getLastName()
                                                    ==
                                                    null ?
                                                    "" : reserved.getLastName()
                                    )
                                    .append("\n");
                        }
                    }
                    if (dates.size() != 0)
                        stringBuilder.append("\n");
                }

            }
            stringBuilder.append("\n");
        }
        return "` ` `".concat(stringBuilder.toString()).concat("` ` `");
    }

    private String answers(User user) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            ResultSet rs = statement().executeQuery("SELECT date, item FROM items WHERE reserved = " + user.getId() + " AND subject = 'WorldHistory' ORDER BY date");
            stringBuilder.append("`***Всемирная история:***`\n");
            boolean contains = false;
            while (rs.next()) {
                contains = true;
                stringBuilder
                        .append("\t\t")
                        .append(rs.getString("date"))
                        .append(" - ")
                        .append(rs.getString("item"))
                        .append("\n");
            }
            if (!contains)
                stringBuilder.append("\t\tНичего!\n");
            rs = statement().executeQuery("SELECT date, item FROM items WHERE reserved = " + user.getId() + " AND subject = 'UkrainianHistory' ORDER BY date");
            stringBuilder.append("\n`***История Украины:***`\n");
            contains = false;
            while (rs.next()) {
                contains = true;
                stringBuilder
                        .append("\t\t")
                        .append(rs.getString("date"))
                        .append(" - ")
                        .append(rs.getString("item"))
                        .append("\n");
            }
            if (!contains)
                stringBuilder.append("\t\tНичего!\t");
            return "` ` `" + stringBuilder.toString() + "` ` `";
        } catch (Exception exc) {
            return null;
        }
    }

    @Override
    public void onText(String text, User user, long chat_id) {
        String command = alias.get(text);
        if (command != null)
            onCommand(
                    new Command(
                            "/",
                            command,
                            Collections.emptyList(),
                            Collections.emptyList()
                    ),
                    user,
                    chat_id
            );
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
        if (title.equals("banManagement") && user.getArgument("admin").equals("t")) {
            bot.activateScript(user, chat_id, "BanManagement");
            return;
        }
        if (title.equals("selfBan") && user.getArgument("admin").equals("f")) {
            user.updateArgument("ban", "Infinity");
            return;
        }
        if (title.equals("list")) {
            bot.sendMessage(
                    chat_id,
                    list(true),
                    bot.inlineKeyboardMarkup(
                            new String[]{
                                    "↻"
                            },
                            new String[]{
                                    "update_list"
                            },
                            1
                    )
            );
            return;
        }
        if (title.equals("fullList") && user.getArgument("admin").equals("t")) {
            bot.sendMessage(
                    chat_id,
                    list(false),
                    bot.inlineKeyboardMarkup(
                            new String[]{
                                    "↻"
                            },
                            new String[]{
                                    "update_full_list"
                            },
                            1
                    )
            );
            return;
        }
        if (title.equals("add") && user.getArgument("admin").equals("t")) {
            bot.activateScript(user, chat_id, "Add");
            return;
        }
        if (title.equals("actions")) {
            if (user.getArgument("admin").equals("t"))
                bot.sendMessage(
                        chat_id,
                        "Действия:",
                        bot.reply(
                                2,
                                "список пунктов",
                                "зарезервировать",
                                "мои ответы",
                                "добавить",
                                "помощь",
                                "отмена",
                                "управление банами",
                                "изменить имя",
                                "удалить пункты",
                                "разбронировать",
                                "список пользователей",
                                "список всех пунктов",
                                "перезагрузить кнопки"
                        ).setResizeKeyboard(true)
                );
            else
                bot.sendMessage(
                        chat_id,
                        "Действия:",
                        bot.reply(
                                2,
                                "список пунктов",
                                "зарезервировать",
                                "мои ответы",
                                "помощь",
                                "отмена"
                        ).setResizeKeyboard(true)
                );
            return;
        }
        if (title.equals("answers")) {
            bot.sendMessage(
                    chat_id,
                    answers(user),
                    bot.inlineKeyboardMarkup(
                            new String[]{
                                    "↻"
                            },
                            new String[]{
                                    "update_answers"
                            },
                            1
                    )
            );
            return;
        }
        if (title.equals("delete") && user.getArgument("admin").equals("t")) {
            bot.activateScript(user, chat_id, "Delete");
            return;
        }
        if (title.equals("unReserve") && user.getArgument("admin").equals("t")) {
            bot.activateScript(user, chat_id, "UnReserve");
            return;
        }
        if (title.equals("usersList") && user.getArgument("admin").equals("t")) {
            try {
                ResultSet set = statement().executeQuery("SELECT id, first_name, last_name, user_name, user_name, ban, admin FROM users");
                StringBuilder sb = new StringBuilder();
                int counter = 0;
                while (set.next()) {
                    sb
                            .append("Идентификатор: ")
                            .append(set.getString("id"))
                            .append("\nИмя: ")
                            .append(set.getString("first_name"))
                            .append("\nФамилия: ")
                            .append(set.getString("last_name"))
                            .append("\nЮзернейм: ")
                            .append(set.getString("user_name"))
                            .append("\nЗабанен: ")
                            .append(set.getString("ban").equals("Infinity") || Long.parseLong(set.getString("ban")) > System.currentTimeMillis() ? "да" : "нет")
                            .append("\nАдминистратор: ")
                            .append(set.getBoolean("admin") ? "да" : "нет")
                            .append("\n\n\n\n");
                    counter++;
                }
                sb.append("`***Итого ").append(counter).append(" пользователя(ей).***`");
                bot.sendMessage(chat_id, "` ` `" + sb.toString() + "` ` `");
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            return;
        }
        if (title.equals("help")) {
            if (user.getArgument("admin").equals("t")) {
                bot.sendMessage(
                        chat_id,
                        "` ` `Этот бот позволяет вести удобный учет ответов по историям." +
                                "Вы являетесь его администратором и можете выполнять следующие действия:\n" +
                                "\t`/list` - Посмотреть список актуальных пуктов. Алиас: 'список пунктов'\n\n" +
                                "\t`/reserve` - Забронировать пункт. Алиас: 'зарезервировать'\n\n" +
                                "\t`/answers` - Посмотреть свои брони. Алиас: 'мои ответы'\n\n" +
                                "\t`/help` - Помощь. Алиас: 'помощь'\n\n" +
                                "\t`/add` - Записать пункты. Алиас: 'добавить'\n\n" +
                                "\t`/start` - Изменить имя и фамилию. Алиас: 'изменить имя'\n\n" +
                                "\t`/banManagement` - Управление банами. Алиас: 'управление банами'\n\n" +
                                "\t`/cancellation` - Отмена любой запущенной операции. Алиас: 'отмена'\n\n" +
                                "\t`/delete` - Удалить пункты. Алиас: 'удалить пункты'\n\n" +
                                "\t`/unReserve` - Убрать бронь с пункта. Алиас: 'разбронировать'\n\n" +
                                "\t`/usersList` - Посмотреть список пользователей. Алиас: 'список пользователей'\n\n" +
                                "\t`/fullList` - Посмотреть список всех пунктов. Алиас: 'список всех пунктов'\n\n" +
                                "\t`/actions` - Прогружает кнопки действий. Алиас: 'перезагрузить кнопки'` ` `"
                );
            } else {
                bot.sendMessage(
                        chat_id,
                        "` ` `Этот бот позволяет вести удобный учет ответов по историям." +
                                "Вы являетесь его рядовым участником и можете выполнять следующие действия:\n" +
                                "\t`/list` - Посмотреть список актуальных пуктов. Алиас: 'список пунктов'\n\n" +
                                "\t`/reserve` - Забронировать пункт. Алиас: 'зарезервировать'\n\n" +
                                "\t`/answers` - Посмотреть свои брони. Алиас: 'мои ответы'\n\n" +
                                "\t`/help` - Помощь. Алиас: 'помощь'\n\n" +
                                "\t`/start` - Изменить имя и фамилию. Алиас: 'изменить имя'\n\n" +
                                "\t`/cancellation` - Отмена любой запущенной операции. Алиас: 'отмена'\n\n" +
                                "\t`/actions` - Прогружает кнопки действий. Алиас: 'перезагрузить кнопки'` ` `"
                );
            }
        }
    }

    private static String fromSubject(String subject) {
        return subject.equals("WorldHistory") ? "всемирной истории" : "истории Украины";
    }

    @Override
    public void onBuilderResponse(User user, long chat_id, String script, Map<String, String> varMap) {
        try {
            switch (script) {
                case "Reserve":
                    statement().execute("UPDATE ITEMS SET RESERVED = " + user.getId() + " WHERE RESERVED IS NULL AND DATE = '" + varMap.get("Date") + "' AND SUBJECT = '" + varMap.get("Subject") + "' AND ITEM = '" + varMap.get("Item") + "'");
                    bot.sendMessage(
                            chat_id,
                            "Вы успешно забронировали " +
                                    varMap.get("Item") +
                                    " пункт по " +
                                    (
                                            fromSubject(varMap.get("Subject"))
                                    ) +
                                    " " +
                                    varMap.get("Date") +
                                    " числа."
                    );
                    return;
                case "Start":
                    String first_name_old = user.getArgument("first_name");
                    String last_name_old = user.getArgument("last_name");
                    String first_name_new = varMap.get("FirstName");
                    String last_name_new = varMap.get("LastName");
                    user.updateArgument("first_name", first_name_new);
                    user.updateArgument("last_name", last_name_new);
                    bot.sendMessage(
                            chat_id,
                            "Ваше имя было успешно изменено с " +
                                    first_name_old +
                                    " " +
                                    last_name_old +
                                    " на " +
                                    first_name_new +
                                    " " +
                                    last_name_new
                    );
                    return;
                case "Add":
                    PreparedStatement ps = heroku.getConnection().prepareStatement("INSERT INTO ITEMS VALUES(?, '" + varMap.get("Subject") + "', '" + varMap.get("Date") + "', NULL)");
                    List<String> items = subItems(varMap.get("Item"));
                    if (items.size() == 0)
                        return;
                    StringJoiner sj = new StringJoiner(", ");
                    for (String item : items) {
                        ps.setString(1, item);
                        sj.add(item);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    bot.sendMessage(
                            chat_id,
                            "Пункты: " +
                                    sj +
                                    " по " +
                                    (
                                            fromSubject(varMap.get("Subject"))
                                    ) +
                                    " " +
                                    varMap.get("Date") +
                                    " числа были успешно добавленны!"
                    );
                    return;
                case "UnReserve":
                    String subject = varMap.get("Subject");
                    String date = varMap.get("Date");
                    String item = varMap.get("Item");
                    boolean success = statement().execute(
                            "UPDATE items SET reserved = NULL WHERE " +
                                    "reserved IS NOT NULL " +
                                    "AND subject = '" + subject + "' " +
                                    "AND date = '" + date + "' " +
                                    "AND item = '" + item + "'"
                    );
                    if (success)
                        bot.sendMessage(chat_id, "Вы успешно убрали бронь с " + item + " пункта по " + fromSubject(subject) + " " + date + " числа");
                    else
                        bot.sendMessage(chat_id, "Произвести эту операцию не удалось!");
                    return;
                case "Delete":
                    subject = varMap.get("Subject");
                    date = varMap.get("Date");
                    items = subItems(varMap.get("Items"));
                    StringJoiner joiner = new StringJoiner(", ");
                    ps = heroku.getConnection().prepareStatement("DELETE FROM items WHERE subject = '" + subject + "' AND date = '" + date + "' AND item = ?");
                    for (String current_item : items) {
                        ps.setString(1, current_item);
                        joiner.add(current_item);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    bot.sendMessage(chat_id, "Вы успешно удалили " + joiner + " пункты по " + fromSubject(subject) + " " + date + " числа");
                    return;
                case "BanManagement":
                    long id = Long.parseLong(varMap.get("Id"));
                    User banned = bot.getUsersList().findUser(id);
                    String time = varMap.get("Time");
                    switch (time) {
                        case "cancel":
                            statement().execute("UPDATE users SET ban = '0' WHERE id = " + id);
                            bot.sendMessage(chat_id, "Бан c '" + banned.getFirstName() + " " + banned.getLastName() + "' был успешно убран!");
                            break;
                        case "infinity":
                            statement().execute("UPDATE users SET ban = 'Infinity' WHERE id = " + id);
                            bot.sendMessage(chat_id, "Перманентный бан был успешно выдан '" + banned.getFirstName() + " " + banned.getLastName() + "'");
                            break;
                        default:
                            statement().execute("UPDATE users SET ban = '" + (long) (System.currentTimeMillis() + Double.parseDouble(time) * 3.6e+6) + "' WHERE id = " + id);
                            bot.sendMessage(chat_id, "Бан '" + banned.getFirstName() + " " + banned.getLastName() + "' был успешно продлён на " + time + " часов");
                            break;
                    }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
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
        for (String paragraph : forRegex("§\\d+\\(.+?\\)", str)) {
            Pattern pattern = Pattern.compile("§\\d+");
            Matcher matcher = pattern.matcher(paragraph);
            if (!matcher.find())
                throw new IllegalArgumentException();
            String par = paragraph.substring(matcher.start(), matcher.end());
            String its = paragraph.substring(matcher.end() + 1, paragraph.length() - 1);
            String[] itemsArr = its.split(",");
            for (String itm : itemsArr) {
                String val = itm.trim();
                if (val.matches("\\d+ *- *\\d+")) {
                    List<String> list = forRegex("\\d+", val);
                    int first = Integer.parseInt(list.get(0));
                    int second = Integer.parseInt(list.get(1));
                    for (int i = Math.min(first, second); i <= Math.max(first, second); i++)
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
            switch (script) {
                case "UnReserve":
                case "Reserve":
                    if (dataVar.equals("date")) {
                        ResultSet resultSet = statement().executeQuery("SELECT * FROM ITEMS WHERE CURRENT_DATE <= DATE AND SUBJECT = '" + varMap.get("Subject") + "'" + (script.equals("UnReserve") ? " AND RESERVED IS NOT NULL" : " AND RESERVED IS NULL"));
                        List<String> names = new ArrayList<>();
                        List<Item> items = find();
                        while (resultSet.next()) {
                            String date = resultSet.getString("DATE");
                            if (
                                    script.equals("UnReserve") ||
                                            find(varMap.get("Subject"), LocalDate.parse(date, dtf), null, false, true, items)
                                                    .stream()
                                                    .noneMatch(item -> item.getId() == user.getId())
                                    )
                                names.add(date);
                        }
                        buttons =
                                names
                                        .stream()
                                        .distinct()
                                        .map(name -> new Button(name, name))
                                        .collect(Collectors.toList());
                        if (buttons.size() == 0) {
                            if (script.equals("Reserve"))
                                bot.sendMessage(chat_id, "В ближайшее время не забронированных пунктов по этому предмету нет, либо вы уже забронировали какой-то пункт в этот день!");
                            else
                                bot.sendMessage(chat_id, "В ближайшее время забронированных пунктов по этому предмету нет!");
                            return null;
                        }
                    } else if (dataVar.equals("items")) {
                        ResultSet resultSet = statement().executeQuery("SELECT * FROM ITEMS WHERE DATE = '" + varMap.get("Date") + "' AND SUBJECT = '" + varMap.get("Subject") + "'" + (script.equals("UnReserve") ? " AND RESERVED IS NOT NULL" : " AND RESERVED IS NULL"));
                        while (resultSet.next()) {
                            String item = resultSet.getString("ITEM");
                            buttons.add(new Button(item, item));
                        }
                        if (buttons.size() == 0) {
                            if (script.equals("Reserve"))
                                bot.sendMessage(chat_id, "В этот день не забронированных пунктов по этому предмету нет!");
                            else
                                bot.sendMessage(chat_id, "В этот день забронированных пунктов по этому предмету нет!");
                            return null;
                        }
                    }
                    break;
                case "BanManagement":
                    ResultSet rs = statement().executeQuery("SELECT first_name, last_name, id FROM users");
                    while (rs.next())
                        buttons.add(new Button(rs.getString("first_name") + " " + rs.getString("last_name"), rs.getString("id")));
                    break;
                case "Delete":
                    rs = statement().executeQuery("SELECT DISTINCT date FROM items WHERE subject = '" + varMap.get("Subject") + "' AND CURRENT_DATE <= date");
                    while (rs.next()) {
                        String date = rs.getString("Date");
                        buttons.add(new Button(date, date));
                    }
                    if (buttons.size() == 0) {
                        bot.sendMessage(chat_id, "Нет пунктов, которые можно удалить!");
                        return null;
                    }
                    break;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        if (buttons.size() == 0)
            return null;
        return buttons;
    }

    @Override
    public boolean onUpdate(Update update, User user, long chat_id) {
        if (update.hasMessage() && update.getMessage().getText().equals("/cancellation")) {
            user.updateArgument("status", "null");
            return false;
        }
        String banStr = user.getArgument("ban");
        if (banStr.equals("Infinity")) {
            user.updateArgument("status", "null");
            bot.sendPhoto(chat_id, new File("src/main/resources/ban.jpg"));
            bot.sendMessage(chat_id, "Вы находитесь в перманентном бане! Не пытайтесь выбраться. Это... невозможно!");
            return false;
        }
        long ban = Long.parseLong(banStr);
        if (ban > System.currentTimeMillis()) {
            user.updateArgument("status", "null");
            bot.sendPhoto(chat_id, new File("src/main/resources/ban.jpg"));
            bot.sendMessage(chat_id, "Вы находитесь в временном бане. Он закончится через " + String.format("%.2f", Math.ceil((ban - System.currentTimeMillis()) * 100 / 3.6e+6) / 100) + " часов!");
            return false;
        }
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            int message_id = update.getCallbackQuery().getMessage().getMessageId();
            switch (data) {
                case "update_list":
                    bot.editMessage(
                            chat_id,
                            message_id,
                            list(true),
                            bot.inlineKeyboardMarkup(
                                    new String[]{
                                            "↻"
                                    },
                                    new String[]{
                                            "update_list"
                                    },
                                    1
                            )
                    );
                    break;
                case "update_full_list":
                    bot.editMessage(
                            chat_id,
                            message_id,
                            list(false),
                            bot.inlineKeyboardMarkup(
                                    new String[]{
                                            "↻"
                                    },
                                    new String[]{
                                            "update_full_list"
                                    },
                                    1
                            )
                    );
                    break;
                case "update_answers":
                    bot.editMessage(
                            chat_id,
                            message_id,
                            answers(user),
                            bot.inlineKeyboardMarkup(
                                    new String[]{
                                            "↻"
                                    },
                                    new String[]{
                                            "update_answers"
                                    },
                                    1
                            )
                    );
                    break;
                default:
                    bot.deleteMessage(chat_id, update.getCallbackQuery().getMessage().getMessageId());
                    break;
            }
        }
        return true;
    }
}
