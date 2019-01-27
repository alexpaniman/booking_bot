package org.telegram.script;

import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private int script;
    private int stage;
    private Map<String, String> varMap;

    public Response(int script, int stage) {
        varMap = new LinkedHashMap<>();
        this.script = script;
        this.stage = stage;
    }

    public Response(int script) {
        this(script, 0);
    }

    public String addVarValue(String name, String value) {
        return varMap.put(name, value);
    }

    public int getScript() {
        return script;
    }

    public int getStage() {
        return stage;
    }

    public int incrementStage() {
        return ++stage;
    }

    public void setScript(int script) {
        this.script = script;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public Map<String, String> getVarMap() {
        return varMap;
    }
}
