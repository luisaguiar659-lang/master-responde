package com.masterresponde.app.integration;

import android.content.Context;

import com.masterresponde.app.engines.AutomationCloudEngine;
import com.masterresponde.app.engines.MasterIBOEngine;

public final class IntegrationBootstrap {

    private IntegrationBootstrap() {
    }

    public static void start(Context context) {
        try {
            AutomationCloudEngine.start(context);
        } catch (Exception ignored) {
        }

        try {
            MasterIBOEngine.start(context);
        } catch (Exception ignored) {
        }
    }
}
