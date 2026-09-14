package com.masterresponde.app.integration;

import android.content.Context;

public class CloudBridge {

    private final Context context;

    public CloudBridge(Context context) {
        this.context = context;
    }

    public void connect() {
        IntegrationBootstrap.start(context);
    }
}
