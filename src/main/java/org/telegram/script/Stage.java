package org.telegram.script;

public class Stage {
    private int index;
    private String text;
    private String variable;
    private ButtonsType buttonsType;

    public Stage(int index, String text, String variable, ButtonsType buttonsType) {
        this.index = index;
        this.text = text;
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
