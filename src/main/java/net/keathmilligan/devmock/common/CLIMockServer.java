package net.keathmilligan.devmock.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public abstract class CLIMockServer {
    private static final Logger logger = LogManager.getLogger();

    private String eol = "\r\n";
    private String welcomeMessage = null;
    private String prompt = null;
    private List<CommandMapping> mappings = new ArrayList<>();
    private String notFound = null;
    private byte[] connectString = null;

    protected Configuration config;
    private Path journalPath;

    protected CLIMockServer(Configuration config) {
        this.config = config;

        // start command journal
        var journalDir = Paths.get(config.journalDir());
        if (!Files.exists(journalDir)) {
            try {
                Files.createDirectory(journalDir);
            } catch (IOException e) {
                logger.error("can't create journal dir: {}", journalDir);
                e.printStackTrace();
            }
        }
        journalPath = Paths.get(config.journalDir(),
                config.name() + '-' + config.port() + ".log");
    }

    public Configuration getConfig() {
        return config;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public String getPrompt() {
        return prompt;
    }

    String getEol() {
        return eol;
    }

    public List<CommandMapping> getMappings() {
        return mappings;
    }

    public String getNotFound() {
        return notFound;
    }

    public byte[] getConnectString() {
        return connectString;
    }

    public void addMapping(String jsonMapping) {
        var parser = new JsonParser();
        var jsCfg = parser.parse(jsonMapping).getAsJsonObject();
        if (jsCfg.has("connect")) {
            var s = jsCfg.get("connect").getAsString();

            // convert 2-digit hex sequence into bytes
            int len = s.length();
            connectString = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                connectString[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i+1), 16));
            }
        }
        if (jsCfg.has("eol")) {
            eol = jsCfg.get("eol").getAsString();
        }
        if (jsCfg.has("welcome")) {
            welcomeMessage = jsCfg.get("welcome").getAsString();
        }
        if (jsCfg.has("welcomeFile")) {
            var fileName = jsCfg.get("welcomeFile").getAsString();
            try {
                welcomeMessage = new String(Files.readAllBytes(Paths.get(config.mappingsDir(), fileName)));
            } catch (IOException e) {
                logger.error("error reading welcome message file: {}", e.toString());
            }
        }
        if (jsCfg.has("prompt")) {
            prompt = jsCfg.get("prompt").getAsString();
        }
        if (jsCfg.has("commands")) {
            StreamSupport.stream(jsCfg.get("commands").getAsJsonArray().spliterator(), false)
                    .map(JsonElement::getAsJsonObject).forEach(jsCmd -> {

                var command = jsCmd.get("command").getAsString();
                boolean includeEol = true;
                if (jsCmd.has("includeEol")) {
                    includeEol = jsCmd.get("includeEol").getAsBoolean();
                }
                String response;
                if (jsCmd.has("responseFile")) {
                    includeEol = false;
                    var responseFile = jsCmd.get("responseFile").getAsString();
                    try {
                        response = new String(Files.readAllBytes(Paths.get(config.mappingsDir(), responseFile)));
                    } catch (IOException e) {
                        logger.error("error reading response file {}: {}", responseFile, e.toString());
                        e.printStackTrace();
                        return;
                    }
                } else if (jsCmd.has("response")) {
                    response = jsCmd.get("response").getAsString();
                } else {
                    logger.error("missing command response");
                    return;
                }
                mappings.add(new CommandMapping(command, response, includeEol));
            });
        }

        if (jsCfg.has("notFound")) {
            notFound = jsCfg.get("notFound").getAsString();
        }
    }

    CommandMapping findMapping(String command) {
        // return the last mapping that matches or the default notFound response
        return mappings
                .stream()
                .filter(m -> command.matches(m.getCommand()))
                .reduce((first, second) -> second)
                .orElse(new CommandMapping("", notFound, true));
    }

    public void resetJournal() {
        logger.info("resetting command journal");
        try {
            Files.deleteIfExists(journalPath);
            Files.createFile(journalPath);
        } catch (IOException e) {
            logger.error("exception resetting journal: " + e.toString());
        }
    }

    synchronized void writeJournal(CLISession session, boolean sendRecv, String text) {
        String s = String.format("%s %s:%d: %s %d bytes:\n%s\n\n",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S").format(LocalDateTime.now()),
                session.remoteAddress(),
                session.remotePort(),
                sendRecv? "SEND" : "RECV",
                text.length(),
                text);
        try {
            Files.writeString(journalPath, s, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("can't write to journal: " + e.toString());
            e.printStackTrace();
        }
    }

    public abstract void start();

    public abstract void stop();

    public abstract void killAllSessions();
}
