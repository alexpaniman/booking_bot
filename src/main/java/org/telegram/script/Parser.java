package org.telegram.script;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private boolean sequenceContains(List<Token> tokens, int start, TokenType[] value) {
        if (tokens.size() - start < value.length)
            return false;
        for (int i = start, j = 0; j < value.length; j++, i++)
            if (!tokens.get(i).getType().equals(value[j]))
                return false;
        return true;
    }

    public Scripts parse(List<Token> tokens) {
        List<Script> scripts = new ArrayList<>();
        List<Stage> stages = new ArrayList<>();
        List<Button> data = new ArrayList<>();
        String scriptTitle = null;
        int num = -1;
        String text = null;
        String var = null;
        String variable = null;
        ButtonsType type = null;
        for (int i = 0; i < tokens.size(); i++) {
            if (sequenceContains(tokens, i, new TokenType[]{
                    TokenType.Script,
                    TokenType.LRB,
                    TokenType.String,
                    TokenType.RRB,
                    TokenType.Colon
            })) {
                Stage stage;
                if (var != null)
                    stage = new VariableStage(num, text, variable, type, var);
                else
                    stage = new DataStage(num, text, variable, type, data);
                stages.add(stage);
                if (scriptTitle != null)
                    scripts.add(new Script(scriptTitle, stages));
                scriptTitle = ((TokenWithValue) tokens.get(i + 2)).getValue();
                i += 4;
                stages = new ArrayList<>();
                data = new ArrayList<>();
                num = -1;
                text = null;
                var = null;
                variable = null;
                type = null;
                continue;
            }
            if (sequenceContains(tokens, i, new TokenType[]{
                    TokenType.Literal,
                    TokenType.Lambda,
                    TokenType.String,
                    TokenType.LRB,
                    TokenType.MarkupType,
                    TokenType.Coma,
                    TokenType.String,
                    TokenType.RRB,
                    TokenType.Colon
            })) {
                Stage stage = null;
                if (var != null)
                    stage = new VariableStage(num, text, variable, type, var);
                else if (num != -1)
                    stage = new DataStage(num, text, variable, type, data);
                if (stage != null)
                    stages.add(stage);
                num = Integer.parseInt(((TokenWithValue) tokens.get(i)).getValue());
                text = ((TokenWithValue) tokens.get(i + 2)).getValue();
                type = ((TokenWithValue) tokens.get(i + 4)).getValue().equals("Inline") ? ButtonsType.Inline : ButtonsType.Reply;
                variable = ((TokenWithValue) tokens.get(i + 6)).getValue();
                var = null;
                data = new ArrayList<>();
                i += 8;
                continue;
            }
            if (sequenceContains(tokens, i, new TokenType[]{
                    TokenType.String,
                    TokenType.Colon,
                    TokenType.String
            })) {
                String name = tokens.get(i).cast().getValue();
                String response = tokens.get(i + 2).cast().getValue();
                data.add(new Button(name, response));
                i += 2;
                continue;
            }
            if (tokens.get(i).getType().equals(TokenType.Variable)) {
                var = tokens.get(i).cast().getValue();
                continue;
            }
            if (tokens.get(i).getType().equals(TokenType.String)) {
                String name = tokens.get(i).cast().getValue();
                data.add(new Button(name, name));
            }
        }
        Stage stage = null;
        if (var != null)
            stage = new VariableStage(num, text, variable, type, var);
        else if (text != null)
            stage = new DataStage(num, text, variable, type, data);
        if (stage != null)
            stages.add(stage);
        if (stages.size() > 0)
            scripts.add(new Script(scriptTitle, stages));
        return new Scripts(scripts);
    }
}
