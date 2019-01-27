package org.telegram.script;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ScriptsCompiler {
    public Scripts compile(String path) {
        File file = new File(path);
        if (!file.exists())
            return null;
        StringBuilder language = new StringBuilder();
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException exc) {
            exc.printStackTrace();
            return null;
        }
        for (String line: lines)
            language.append(line).append("\n");
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(language.toString());
        Parser parser = new Parser();
        return parser.parse(tokens);
    }
}
