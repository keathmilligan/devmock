package net.keathmilligan.devmock;

import java.util.ArrayList;
import java.util.List;

abstract class MockInterface {
    MockDevice device;
    List<String> mappings = new ArrayList<>();
    String mappingsDir;

    MockInterface(MockDevice device, String mappingsDir) {
        this.device = device;
        this.mappingsDir = mappingsDir;
    }

    abstract void addMappings(List<String> mappings);
    abstract void start();
    abstract void stop();
    abstract void clearJournal();
    abstract void killSessions();
    abstract List<String> listSessions();
}
