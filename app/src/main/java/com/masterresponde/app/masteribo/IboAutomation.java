package com.masterresponde.app.masteribo;

public class IboAutomation {

    public boolean activate(String app, String mac, String m3u) {
        // Ponto de integração da automação real do Master IBO.
        // A lógica original de ativação será conectada aqui.
        return app != null && !app.isEmpty()
                && mac != null && !mac.isEmpty()
                && m3u != null && !m3u.isEmpty();
    }
}
