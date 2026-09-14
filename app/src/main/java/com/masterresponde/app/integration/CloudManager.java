package com.masterresponde.app.integration;

public class CloudManager {

    private static CloudManager instance;

    public static CloudManager getInstance() {
        if (instance == null) {
            instance = new CloudManager();
        }
        return instance;
    }

    public void initialize() {
        // Inicializacao do servico Cloud
    }

    public void sync() {
        // Sincronizacao com Automation Cloud
    }
}
