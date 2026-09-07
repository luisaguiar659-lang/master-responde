package com.masterresponde.app.masteribo;

public class IboActivation {

    private String application;
    private String mac;
    private String m3u;
    private String server;

    public IboActivation(String application, String mac, String m3u, String server) {
        this.application = application;
        this.mac = mac;
        this.m3u = m3u;
        this.server = server;
    }

    public String getApplication() {
        return application;
    }

    public String getMac() {
        return mac;
    }

    public String getM3u() {
        return m3u;
    }

    public String getServer() {
        return server;
    }
}
