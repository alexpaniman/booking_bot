package org.telegram.script;

public class TokenWithValue extends Token {
    private String value;

    public TokenWithValue(TokenType type, String value) {
        super(type);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TokenWithValue{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}
