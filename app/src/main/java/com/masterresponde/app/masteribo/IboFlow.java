package com.masterresponde.app.masteribo;

public class IboFlow {

    private final MasterIboMotor motor;

    public IboFlow(MasterIboMotor motor) {
        this.motor = motor;
    }

    public String start() {
        motor.start();
        return "Escolha o aplicativo IBO para ativação.";
    }

    public String receiveApp(String app) {
        motor.setApp(app);
        return "Envie o endereço MAC do aparelho.";
    }

    public String receiveMac(String mac) {
        motor.setMac(mac);
        return "Envie a lista M3U.";
    }

    public String receiveM3u(String m3u) {
        motor.setM3u(m3u);
        return "Processando ativação IBO.";
    }
}
