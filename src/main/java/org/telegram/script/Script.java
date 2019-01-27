package org.telegram.script;

import java.util.List;

public class Script {
    private String title;
    private List<Stage> stages;

    public Script(String title, List<Stage> stages){
        this.title = title;
        this.stages = stages;
    }

    public String getTitle() {
        return title;
    }

    public List<Stage> getStages() {
        return stages;
    }

    @Override
    public String toString() {
        return "Script{" +
                "title='" + title + '\'' +
                ", stages=" + stages +
                '}';
    }
}
