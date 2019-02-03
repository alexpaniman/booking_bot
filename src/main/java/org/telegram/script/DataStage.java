package org.telegram.script;

import java.util.List;

public class DataStage extends Stage{
    private List<Button> data;

    DataStage(int index, String text, String variable, ButtonsType buttonsType, String regex, List<Button> data) {
        super(index, text, variable, buttonsType, regex);
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
