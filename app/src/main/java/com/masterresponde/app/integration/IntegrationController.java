package com.masterresponde.app.integration;

import android.content.Context;

public class IntegrationController {

    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }

        initialized = true;

        EngineRegistry.initialize(context);
        CloudBridge.connect(context);
    }
}
