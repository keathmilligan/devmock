package net.keathmilligan.devmock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class DevMockServer {
    private static final Logger logger = LogManager.getLogger();
    private static final String DEFAULT_CONFIG = "devmock.json";
    private static final String DEFAULT_JOURNAL_DIR = "journal";
    private static DevMockServer instance;

    private String configFile;
    private List<MockDevice> deviceList = new ArrayList<>();
    private String journalDir = DEFAULT_JOURNAL_DIR;

    String getJournalDir() {
        return journalDir;
    }

    private DevMockServer(String configFile) {
        this.configFile = configFile;
    }

    static DevMockServer create(String configFile) {
        instance = new DevMockServer(configFile);
        instance.readConfig();
        return instance;
    }

    static DevMockServer create() {
        return create(DEFAULT_CONFIG);
    }

    static DevMockServer getInstance() {
        return instance;
    }

    private void readConfig() {
        logger.debug("parsing config file: {}", configFile);
        var parser = new JsonParser();
        JsonObject config;
        try {
            config = parser.parse(new FileReader(configFile)).getAsJsonObject();
        } catch (FileNotFoundException e) {
            logger.fatal("error reading config file: " + e.toString());
            throw new RuntimeException();
        }

        if (config.has("journalDir")) {
            journalDir = config.get("journalDir").getAsString();
        }

        StreamSupport.stream(config.get("devices").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsJsonObject).forEach(de -> {

            var device = new MockDevice(de.get("name").getAsString(),de.get("ip").getAsString());

            // get generic device info
            if (de.has("info")) {
                var deviceInfo = new HashMap<String, String>();
                de.get("info").getAsJsonObject().entrySet().forEach(entry -> {
                    deviceInfo.put(entry.getKey(), entry.getValue().getAsString());
                });
                device.addInfo(deviceInfo);
            }

            StreamSupport.stream(de.get("interfaces").getAsJsonArray().spliterator(), false)
                    .map(JsonElement::getAsJsonObject).forEach(ie -> {

                MockInterface ifx;
                var ifxType = ie.get("type").getAsString();
                var mappingsDir = ie.get("mappingsDir").getAsString();
                switch(ifxType) {
                    case "web":
                        String serverName = null;
                        if (ie.has("serverName")) {
                            serverName = ie.get("serverName").getAsString();
                        }
                        ifx = new WebInterface(device, mappingsDir, serverName, ie.get("http").getAsInt(), ie.get("https").getAsInt());
                        break;
                    case "tty":
                        ifx = new TTYInterface(device, mappingsDir, ie.get("port").getAsInt());
                        break;
                    case "ssh":
                        ifx = new SSHInterface(device, mappingsDir, ie.get("port").getAsInt());
                        break;
                    default:
                        logger.error("invalid interface type: " + ifxType);
                        throw new RuntimeException();
                }

                ifx.addMappings(StreamSupport.stream(ie.get("mappings").getAsJsonArray().spliterator(), false)
                        .map(JsonElement::getAsString).collect(Collectors.toList()));
                device.addInterface(ifx);
            });

            deviceList.add(device);
        });

        logger.info(deviceList.size() + " devices configured");
    }

    void start() {
        logger.info("starting DevMock server");
        deviceList.forEach(MockDevice::start);
    }

    void stop() {
        logger.info("stopping DevMock server");
        deviceList.forEach(MockDevice::stop);
    }

    void restart() {
        logger.info("restarting");
        stop();
        deviceList = new ArrayList<>();
        readConfig();
        start();
    }

    void clearJournals() {
        logger.info("clearing request/command journals");
        deviceList.forEach(MockDevice::clearJournals);
    }

    void killSessions() {
        logger.info("killing all sessions");
        deviceList.forEach(MockDevice::killSessions);
    }

    List<String> listSessions() {
        var sessions = new ArrayList<String>();
        deviceList.forEach(device -> sessions.addAll(device.listSessions()));
        return sessions;
    }
}
