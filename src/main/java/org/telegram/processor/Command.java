package org.telegram.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings("unused")
public class Command {
    private String prefix;
    private String title;
    private List<String> parameters;
    private List<String> arguments;

    public static Command parseCommand(String command_prefix, String command) {
        if (command_prefix != null && !command.startsWith(command_prefix))
            return null;
        else
            command = command_prefix != null ? command.substring(command_prefix.length()) : command;
        if (command == null)
            return null;
        command = command.concat(" ");
        List<String> parameters = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        String title = "";
        StringBuilder temp = new StringBuilder();
        boolean parse_title = true;
        boolean skip = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            try {
                if (c == '\"') {
                    skip = !skip;
                    continue;
                }
                if (c == ' ' && !skip) {
                    if (temp.charAt(0) == '-') {
                        if (temp.length() == 1) {
                            temp = new StringBuilder();
                            continue;
                        }
                        if (temp.charAt(1) == '-') {
                            if (temp.length() == 2) {
                                temp = new StringBuilder();
                                continue;
                            }
                            String after_trim = temp.substring(2).trim();
                            if (after_trim.length() > 0)
                                parameters.add(after_trim);
                        } else {
                            String params = temp.substring(1);
                            for (int j = 1; j <= params.length(); j++) {
                                char chr = temp.charAt(j);
                                parameters.add(String.valueOf(chr));
                            }
                        }
                    } else {
                        if (parse_title) {
                            title = temp.toString();
                            parse_title = false;
                        } else {
                            String after_trim = temp.toString().trim();
                            if (after_trim.length() > 0)
                                arguments.add(after_trim);
                        }
                    }
                    temp = new StringBuilder();
                } else {
                    temp.append(c);
                }
            } catch (IndexOutOfBoundsException ignore) {
            }
        }
        return new Command(command_prefix == null ? "" : command_prefix, title, parameters, arguments);
    }

    public static Command parseCommand(String command) {
        return parseCommand(null, command);
    }

    public Command(String prefix, String title, List<String> parameters, List<String> arguments) {
        this.title = title;
        this.prefix = prefix;
        this.parameters = parameters == null ? new ArrayList<>() : parameters;
        this.arguments = arguments == null ? new ArrayList<>() : arguments;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters == null ? new ArrayList<>() : parameters;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments == null ? new ArrayList<>() : arguments;
    }

    public int numberOfParameters() {
        return parameters.size();
    }

    public boolean containsParameter(String o) {
        return parameters.contains(o);
    }

    public boolean addParameter(String s) {
        return parameters.add(s);
    }

    public boolean removeParameter(String o) {
        return parameters.remove(o);
    }

    public int numberOfArguments() {
        return arguments.size();
    }

    public boolean containsArgument(String o) {
        return arguments.contains(o);
    }

    public boolean addArgument(String s) {
        return arguments.add(s);
    }

    public boolean removeArgument(String o) {
        return arguments.remove(o);
    }

    public String getArgument(int index) {
        return arguments.get(index);
    }

    public String getParameter(int index) {
        return parameters.get(index);
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix.concat(title).concat(" "));
        StringBuilder short_params = new StringBuilder();
        StringBuilder long_params = new StringBuilder();
        boolean need_brackets = false;
        for (String parameter : parameters)
            if (parameter.length() > 1) {
                if (parameter.contains(" "))
                    long_params.append("--".concat("\"").concat(parameter).concat("\" "));
                else
                    long_params.append("--".concat(parameter).concat(" "));
            } else {
                if (parameter.equals(" ")) {
                    short_params.insert(0, " ");
                    need_brackets = true;
                } else
                    short_params.append(parameter);
            }
        if (short_params.length() > 0)
            builder.append("-".concat(need_brackets? "\"".concat(short_params.toString()).concat("\"") : short_params.toString()).concat(" "));
        builder.append(long_params);
        StringJoiner joiner = new StringJoiner(" ");
        for (String argument : arguments)
            joiner.add(argument.contains(" ") ? "\"".concat(argument).concat("\" ") : argument.concat(" "));
        builder.append(joiner.toString());
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Command{" +
                "prefix='" + prefix + '\'' +
                ", title='" + title + '\'' +
                ", parameters=" + parameters +
                ", arguments=" + arguments +
                '}';
    }
}
