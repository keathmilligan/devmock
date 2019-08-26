package net.keathmilligan.devmock.common;

import static java.net.InetAddress.getLoopbackAddress;

public class Configuration {
    private static final int DEFAULT_PORT = 1024;
    private static final String DEFAULT_BIND_ADDRESS = getLoopbackAddress().getHostAddress();

    private int portNumber = DEFAULT_PORT;
    private String bindAddress = DEFAULT_BIND_ADDRESS;
    private String mappingsDir = "";
    private ResponsePropertiesProvider responsePropertiesProvider = null;
    private String journalDir;
    private String name = "cli";

    public static Configuration options() {
        return new Configuration();
    }

    public Configuration port(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    public Configuration bindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    public Configuration mappingsDir(String mappingsDir) {
        this.mappingsDir = mappingsDir;
        return this;
    }

    public Configuration responsePropertiesProvider(ResponsePropertiesProvider responsePropertiesProvider) {
        this.responsePropertiesProvider = responsePropertiesProvider;
        return this;
    }

    public Configuration journalDir(String journalDir) {
        this.journalDir = journalDir;
        return this;
    }

    public Configuration name(String name) {
        this.name = name;
        return this;
    }

    public int port() {
        return portNumber;
    }

    public String bindAddress() {
        return bindAddress;
    }

    String mappingsDir() {
        return mappingsDir;
    }

    ResponsePropertiesProvider responsePropertiesProvider() {
        return responsePropertiesProvider;
    }

    String journalDir() {
        return journalDir;
    }

    String name() {
        return name;
    }
}
