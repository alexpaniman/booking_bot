package org.telegram.script;

public class Token {
    private TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }

    public TokenWithValue cast() throws ClassCastException{
        return ((TokenWithValue) this);
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                '}';
    }
}
