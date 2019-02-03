package org.telegram.script;

public class VariableStage extends Stage {
    private String dataVariable;

    VariableStage(int index, String text, String variable, ButtonsType buttonsType, String regex, String dataVariable) {
        super(index, text, variable, buttonsType, regex);
        this.dataVariable = dataVariable;
    }

    public String getDataVariable() {
        return dataVariable;
    }

    @Override
    public String toString() {
        return "VariableStage{" +
                "dataVariable='" + dataVariable + '\'' +
                "} " + super.toString();
    }
}
