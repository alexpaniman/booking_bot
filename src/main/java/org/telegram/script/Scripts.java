package org.telegram.script;

import java.util.List;

public class Scripts {
    private List<Script> scripts;

    public Scripts(List<Script> scripts) {
        this.scripts = scripts;
    }

    public List<Script> getScripts() {
        return scripts;
    }

    @Override
    public String toString() {
        return "Scripts{" +
                "scripts=" + scripts +
                '}';
    }
}
