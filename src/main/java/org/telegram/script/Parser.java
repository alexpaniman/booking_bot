package org.telegram.script;

import java.util.ArrayList;
import java.util.List;

class Parser {
    //Script("Name"):
    private static final TokenType[] script = new TokenType[]{
            TokenType.Script,
            TokenType.LRB,
            TokenType.String,
            TokenType.RRB,
            TokenType.Colon
    };

    //Number -> "Text"(MarkupType, "Variable"):
    private static final TokenType[] stage = new TokenType[]{
            TokenType.Literal,
            TokenType.Lambda,
            TokenType.String,
            TokenType.LRB,
            TokenType.MarkupType,
            TokenType.Coma,
            TokenType.String,
            TokenType.RRB,
            TokenType.Colon
    };

    //Number -> "Text"(MarkupType, "Variable", $):
    private static final TokenType[] stage_read_all = new TokenType[]{
            TokenType.Literal,
            TokenType.Lambda,
            TokenType.String,
            TokenType.LRB,
            TokenType.MarkupType,
            TokenType.Coma,
            TokenType.String,
            TokenType.Coma,
            TokenType.Dollar,
            TokenType.RRB,
            TokenType.Colon
    };

    //Number -> "Text"(MarkupType, "Variable", $("Regex")):
    private static final TokenType[] stage_regex = new TokenType[]{
            TokenType.Literal,
            TokenType.Lambda,
            TokenType.String,
            TokenType.LRB,
            TokenType.MarkupType,
            TokenType.Coma,
            TokenType.String,
            TokenType.Coma,
            TokenType.Dollar,
            TokenType.LRB,
            TokenType.String,
            TokenType.RRB,
            TokenType.RRB,
            TokenType.Colon
    };

    //"Button" : "Response"
    private static final TokenType[] button = new TokenType[]{
            TokenType.String,
            TokenType.Colon,
            TokenType.String
    };

    private boolean sequenceContains(List<Token> tokens, int start, TokenType[] value) {
        if (tokens.size() - start < value.length)
            return false;
        for (int i = start, j = 0; j < value.length; j++, i++)
            if (!tokens.get(i).getType().equals(value[j]))
                return false;
        return true;
    }

    private void addStage(
            List<Stage> stages,
            List<Button> data,
            String dataVariable,
            String regex,
            Integer num,
            String text,
            String variable,
            ButtonsType buttonsType
    ) {
        Stage stage = null;
        if (dataVariable != null)
            stage = new VariableStage(num, text, variable, buttonsType, regex, dataVariable);
        else if (num != null)
            stage = new DataStage(num, text, variable, buttonsType, regex, data);
        if (stage != null)
            stages.add(stage);
    }

    Scripts parse(List<Token> tokens) throws ParseException {
        List<Script> scripts = new ArrayList<>();
        List<Stage> stages = new ArrayList<>();
        List<Button> data = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (sequenceContains(tokens, i, script)) {
                stages = new ArrayList<>();
                scripts.add(new Script(tokens.get(i + 2).getValue(), stages));
                i += 4;
                continue;
            }

            if (sequenceContains(tokens, i, stage)) {
                data = new ArrayList<>();

                String dataVar;
                if (i + 9 < tokens.size()) {
                    if (tokens.get(i + 9).getType().equals(TokenType.Variable))
                        dataVar = tokens.get(i + 9).getValue();
                    else
                        dataVar = null;
                } else
                      dataVar = null;

                addStage(
                        stages,
                        data,
                        dataVar,
                        null,
                        Integer.valueOf(tokens.get(i).getValue()),
                        tokens.get(i + 2).getValue(),
                        tokens.get(i + 6).getValue(),
                        tokens.get(i + 4).getValue().equals("Inline") ? ButtonsType.Inline : ButtonsType.Reply
                );

                i += 8;
                if (dataVar != null)
                    i++;
                continue;
            }

            if (sequenceContains(tokens, i, stage_read_all)) {
                data = new ArrayList<>();

                String dataVar;
                if (i + 11 < tokens.size()) {
                    if (tokens.get(i + 11).getType().equals(TokenType.Variable))
                        dataVar = tokens.get(i + 11).getValue();
                    else
                        dataVar = null;
                } else
                    dataVar = null;

                addStage(
                        stages,
                        data,
                        dataVar,
                        ".*",
                        Integer.valueOf(tokens.get(i).getValue()),
                        tokens.get(i + 2).getValue(),
                        tokens.get(i + 6).getValue(),
                        tokens.get(i + 4).getValue().equals("Inline") ? ButtonsType.Inline : ButtonsType.Reply
                );

                i += 10;
                if (dataVar != null)
                    i++;
                continue;
            }

            if (sequenceContains(tokens, i, stage_regex)) {
                data = new ArrayList<>();

                String dataVar;
                if (i + 14 < tokens.size()) {
                    if (tokens.get(i + 14).getType().equals(TokenType.Variable))
                        dataVar = tokens.get(i + 14).getValue();
                    else
                        dataVar = null;
                } else
                    dataVar = null;

                addStage(
                        stages,
                        data,
                        dataVar,
                        tokens.get(i + 10).getValue(),
                        Integer.valueOf(tokens.get(i).getValue()),
                        tokens.get(i + 2).getValue(),
                        tokens.get(i + 6).getValue(),
                        tokens.get(i + 4).getValue().equals("Inline") ? ButtonsType.Inline : ButtonsType.Reply
                );

                i += 13;
                if (dataVar != null)
                    i++;
                continue;
            }

            if (sequenceContains(tokens, i, button)) {
                String name = tokens.get(i).getValue();
                String response = tokens.get(i + 2).getValue();
                data.add(new Button(name, response));
                i += 2;
                continue;
            }

            if (tokens.get(i).getType().equals(TokenType.String)) {
                String name = tokens.get(i).getValue();
                data.add(new Button(name, name));
                continue;
            }

            throw new ParseException("Unexpected token: " + tokens.get(i).getType() + (tokens.get(i).getValue() == null? "" : " (" + tokens.get(i).getValue() + ")"));
        }
        return new Scripts(scripts);
    }
}
