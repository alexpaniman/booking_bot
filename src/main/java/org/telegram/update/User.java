package org.telegram.update;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

public class User {
    long id;
    long[] chatId;
    String firstName;
    String lastName;
    String userName;
    String languageCode;
    boolean isBot;
    LinkedHashMap<String, String> arguments;
    UsersList list;

    public User(long id, long[] chatId, String firstName, String lastName, String userName, String languageCode, boolean isBot, LinkedHashMap<String, String> arguments, UsersList list) {
        this.id = id;
        this.chatId = chatId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.languageCode = languageCode;
        this.isBot = isBot;
        this.arguments = arguments;
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                isBot == user.isBot &&
                Arrays.equals(chatId, user.chatId) &&
                Objects.equals(firstName, user.firstName) &&
                Objects.equals(lastName, user.lastName) &&
                Objects.equals(userName, user.userName) &&
                Objects.equals(languageCode, user.languageCode) &&
                Objects.equals(arguments, user.arguments) &&
                Objects.equals(list, user.list);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, firstName, lastName, userName, languageCode, isBot, arguments, list);
        result = 31 * result + Arrays.hashCode(chatId);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", chatId=" + Arrays.toString(chatId) +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", isBot=" + isBot +
                ", arguments=" + arguments +
                ", list=" + list +
                '}';
    }

    public String getArgument(String name) {
        return arguments.get(name);
    }

    public String updateArgument(String name, String newValue) {
        list.updateArgument(id, name, newValue);
        return arguments.put(name, newValue);
    }

    public long getId() {
        return id;
    }

    public long[] getChatIdArray() {
        return chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public boolean isBot() {
        return isBot;
    }

    public void delete() {
        list.deleteUser(id);
    }
}
