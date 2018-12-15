package org.telegram.shell;

import org.processor.Command;
import org.update.User;

import java.io.File;

public interface Bot {
    void onDocument(File file, User user, long chat_id);
    void onCommand(Command command, User user, long chat_id);
    void onImages(File[] file, User user, long chat_id);
    void onText(String text, User user, long chat_id);
}
