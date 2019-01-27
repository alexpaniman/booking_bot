package org.telegram.script;

public class Button {
    private String button;
    private String response;

    public Button(String button, String response) {
        this.button = button;
        this.response = response;
    }

    public String getButton() {
        return button;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "Button{" +
                "button='" + button + '\'' +
                ", response='" + response + '\'' +
                '}';
    }
}
