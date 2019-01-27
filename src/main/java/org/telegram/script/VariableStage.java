package org.telegram.script;

public class VariableStage extends Stage {
    private String dataVariable;

    public VariableStage(int ind, String text, String variable, ButtonsType buttonsType, String dataVariable) {
        super(ind, text, variable, buttonsType);
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
