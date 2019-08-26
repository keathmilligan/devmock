package net.keathmilligan.devmock.common;

import java.util.regex.Pattern;

public class CommandMapping {
    private final String command;
    private final String response;
    private final boolean includeEol;
    private final Pattern pattern;

    CommandMapping(String command, String response, boolean includeEol) {
        this.command = command;
        this.response = response;
        this.includeEol = includeEol;
        this.pattern = Pattern.compile(command);
    }

    public String getCommand() {
        return command;
    }

    String getResponse() {
        return response;
    }

    boolean isIncludeEol() {
        return includeEol;
    }

    Pattern getPattern() {
        return pattern;
    }
}
