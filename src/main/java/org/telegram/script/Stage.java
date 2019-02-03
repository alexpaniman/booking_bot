package org.telegram.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stage {
    private int index;
    private String text;
    private String regex;
    private String variable;
    private ButtonsType buttonsType;

    Stage(int index, String text, String variable, ButtonsType buttonsType, String regex) {
        this.index = index;
        this.text = text;
        this.regex = regex;
        this.variable = variable;
        this.buttonsType = buttonsType;
    }

    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public String getVariable() {
        return variable;
    }

    public ButtonsType getButtonsType() {
        return buttonsType;
    }

    public Boolean test(String text) {
        if (regex == null)
            return null;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    @Override
    public String toString() {
        return "Stage{" +
                "ind=" + index +
                ", text='" + text + '\'' +
                ", variable='" + variable + '\'' +
                ", buttonsType=" + buttonsType +
                '}';
    }
}
