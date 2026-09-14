package com.masterresponde.app.integration;

import android.content.Context;

public class EngineRegistry {

    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }

        IntegrationBootstrap.start(context);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
