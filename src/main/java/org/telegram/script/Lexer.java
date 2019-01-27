package org.telegram.script;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    public Lexer(){}

    private boolean isDigit(char c) {
        return  c == '0' ||
                c == '1' ||
                c == '2' ||
                c == '3' ||
                c == '4' ||
                c == '5' ||
                c == '6' ||
                c == '7' ||
                c == '8' ||
                c == '9';
    }

    private boolean sequenceContains(char[] chars, int start, String value) {
        if (chars.length - start < value.length())
            return false;
        char[] charsValue = value.toCharArray();
        for (int i = start, j = 0; j < value.length(); j++, i++)
            if (chars[i] != charsValue[j])
                return false;
        return true;
    }
    
    public List<Token> tokenize(String code){
        List<Token> tokens = new ArrayList<>();
        char[] chars = code.toCharArray();
        StringBuilder string = null;
        StringBuilder literal = null;
        StringBuilder variable = null;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (string != null) {
                if (c == '"') {
                    tokens.add(new TokenWithValue(TokenType.String, string.toString()));
                    string = null;
                    continue;
                }
                string.append(c);
                continue;
            }
            if (literal != null) {
                if (isDigit(c)) {
                    literal.append(c);
                    continue;
                }
                tokens.add(new TokenWithValue(TokenType.Literal, literal.toString()));
                literal = null;
                continue;
            }
            if (variable != null) {
                if (c == '>') {
                    tokens.add(new TokenWithValue(TokenType.Variable, variable.toString()));
                    variable = null;
                    continue;
                }
                variable.append(c);
                continue;
            }
            if (c == '<') {
                variable = new StringBuilder();
                continue;
            }
            if (c == '"') {
                string = new StringBuilder();
                continue;
            }
            if (c == ':') {
                tokens.add(new Token(TokenType.Colon));
                continue;
            }
            if (isDigit(c))
                literal = new StringBuilder(c + "");
            if (c == '(') {
                tokens.add(new Token(TokenType.LRB));
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(TokenType.RRB));
                continue;
            }
            if (c == ',') {
                tokens.add(new Token(TokenType.Coma));
                continue;
            }
            if (sequenceContains(chars, i, "Script")) {
                tokens.add(new Token(TokenType.Script));
                i += 5;
                continue;
            }
            if (sequenceContains(chars, i, "->")) {
                tokens.add(new Token(TokenType.Lambda));
                i ++;
                continue;
            }
            if (sequenceContains(chars, i, "Inline")) {
                tokens.add(new TokenWithValue(TokenType.MarkupType, "Inline"));
                i += 5;
                continue;
            }
            if (sequenceContains(chars, i, "Reply")) {
                tokens.add(new TokenWithValue(TokenType.MarkupType, "Reply"));
                i += 4;
            }
        }
        return tokens;
    }
}
