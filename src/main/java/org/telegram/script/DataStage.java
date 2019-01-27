package org.telegram.script;

import java.util.List;

public class DataStage extends Stage{
    private List<Button> data;

    public DataStage(int ind, String text, String variable, ButtonsType buttonsType, List<Button> data) {
        super(ind, text, variable, buttonsType);
        this.data = data;
    }

    public List<Button> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "DataStage{" +
                "data=" + data +
                "} " + super.toString();
    }
}
