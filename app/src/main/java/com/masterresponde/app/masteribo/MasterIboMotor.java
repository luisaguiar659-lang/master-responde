package com.masterresponde.app.masteribo;

/**
 * Motor independente do Master IBO.
 * Não altera motores existentes.
 */
public class MasterIboMotor {

    public enum State {
        IDLE,
        WAITING_APP,
        WAITING_MAC,
        WAITING_M3U,
        PROCESSING,
        FINISHED,
        ERROR
    }

    private State state = State.IDLE;
    private String app;
    private String mac;
    private String m3u;

    public void start() {
        state = State.WAITING_APP;
    }

    public void setApp(String app) {
        this.app = app;
        state = State.WAITING_MAC;
    }

    public void setMac(String mac) {
        this.mac = mac;
        state = State.WAITING_M3U;
    }

    public void setM3u(String m3u) {
        this.m3u = m3u;
        state = State.PROCESSING;
    }

    public State getState() {
        return state;
    }

    public String getApp() {
        return app;
    }

    public String getMac() {
        return mac;
    }

    public String getM3u() {
        return m3u;
    }
}
