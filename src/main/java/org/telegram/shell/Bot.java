package org.telegram.shell;

import org.telegram.processor.Command;
import org.telegram.script.Button;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.update.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Bot {
    default void onCommand(Command command, User user, long chat_id) {}
    default void onDocument(File file, User user, long chat_id) {}
    default void onImages(File[] file, User user, long chat_id) {}
    default void onText(String text, User user, long chat_id) {}
    default void onUpdate(Update update, User user, long chat_id) {}
    default void onBuilderResponse(User user, long chat_id, String script, Map<String, String> builderResponse) {}
    default List<Button> setupDataVariable(String dataVar, User user, long chat_id, String script, Map<String, String> varMap) {
        return new ArrayList<>();
    }
}
