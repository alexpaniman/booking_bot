package org.update;

import org.database.DataBaseConnector;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class UsersList {
    DataBaseConnector connector;

    public UsersList(DataBaseConnector connector) {
        this.connector = connector;
    }

    public static void main(String[] args) {
        new UsersList(
                null
        )
                .addUser(
                        new User(
                                123456789L,
                                new long[]{1111111111L, 2222222222L, 3333333333L, 4444444444L, 5555555555L},
                                "Alex",
                                "Paniman",
                                "AlexPaniman",
                                "RU",
                                false,
                                new LinkedHashMap<String, String>() {{
                                    put("ARG1", "111");
                                    put("ARG2", "222");
                                    put("ARG3", "333");
                                }},
                                null
                        )
                );
    }

    public void addUser(User user) {
        StringBuilder SQL = new StringBuilder("INSERT INTO USERS (ID, CHAT_ID, FIRST_NAME, LAST_NAME, USER_NAME, LANGUAGE_CODE, IS_BOT");
        for (Map.Entry<String, String> entry : user.arguments.entrySet())
            SQL.append(", ").append(entry.getKey());
        SQL.append(") VALUES (").append(user.id).append(", '");
        for (long chat_id : user.chatId)
            SQL.append(chat_id).append(" ");
        SQL.append("', ").append(user.firstName).append(", ").append(user.lastName).append(", ").append(user.userName).append(", ").append(user.languageCode).append(", ").append(user.isBot);
        for (Map.Entry<String, String> entry : user.arguments.entrySet())
            SQL.append(", ").append(entry.getValue());
        SQL.append(");");
        System.out.println(SQL);
        try {
            connector.getConnection().createStatement().execute(SQL.toString());
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
    }

    public void updateArgument(long id, String arg, String newValue) {
        try {
            connector.getConnection().createStatement().execute(
                    "UPDATE USERS SET " + arg + " = '" + newValue + "' WHERE ID = '" + id + "'"
            );
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
    }

    public User findUser(long id) {
        try {
            ResultSet rs = connector.getConnection().createStatement().executeQuery("SELECT * FROM USERS WHERE ID = " + id);
            if (!rs.next()) {
                return null;
            }
            long[] chat_id = Arrays.stream(rs.getString("CHAT_ID").split(" ")).mapToLong(Long::parseLong).toArray();
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 8; i <= rsmd.getColumnCount(); i++)
                map.put(rsmd.getColumnName(i), rs.getString(i));
            return new User(
                    id,
                    chat_id,
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("USER_NAME"),
                    rs.getString("LANGUAGE_CODE"),
                    rs.getBoolean("IS_BOT"),
                    map,
                    this
            );
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return null;
    }

    public void deleteUser(long id) {
        try {
            connector.getConnection().createStatement().execute("DELETE FROM USERS WHERE ID = " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
