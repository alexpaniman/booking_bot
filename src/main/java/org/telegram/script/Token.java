package org.telegram.script;

@SuppressWarnings("WeakerAccess")
public class Token {
    private TokenType type;
    private String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public Token(TokenType tokenType) {
        this.type = tokenType;
        this.value = null;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                '}';
    }
}
