package net.keathmilligan.devmock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MockDevice {
    private static final Logger logger = LogManager.getLogger();
    private List<MockInterface> interfaceList = new ArrayList<>();
    private String name;
    private String ipAddress;
    private HashMap<String, String> info = new HashMap<>();

    MockDevice(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        logger.debug("device created: " + name + " " + ipAddress);
    }

    String getName() {
        return name;
    }

    String getIpAddress() {
        return ipAddress;
    }

    Map<String, String> getInfo() { return info; }

    void addInfo(HashMap<String, String> info) {
        this.info.putAll(info);
    }

    void addInterface(MockInterface mockInterface) {
        this.interfaceList.add(mockInterface);
    }

    void start() {
        logger.trace("{}: starting all interfaces", name);
        interfaceList.forEach(MockInterface::start);
    }

    void stop() {
        logger.trace("{}: stopping all interfaces", name);
        interfaceList.forEach(MockInterface::stop);
    }

    void clearJournals() {
        logger.trace("{}: clearing journals", name);
        interfaceList.forEach(MockInterface::clearJournal);
    }

    void killSessions() {
        logger.trace("{}: starting all interfaces", name);
        interfaceList.forEach(MockInterface::killSessions);
    }

    List<String> listSessions() {
        var sessions = new ArrayList<String>();
        interfaceList.forEach(ifx -> {
            ifx.listSessions().forEach(session -> sessions.add(String.format("%s:%s:%s", name, ipAddress, session)));
        });
        return sessions;
    }

    HashMap<String, Object> getResponseProperties() {
        var props = new HashMap<String, Object>();

        props.put("name", name);
        props.put("address", ipAddress);
        props.put("info", info);

        return props;
    }
}
