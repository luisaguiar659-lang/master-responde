package com.masterresponde.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ServiceRecoveryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {
        if (context == null) {
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
                        "master_responde",
                        Context.MODE_PRIVATE
                );

        if (!prefs.getBoolean(
                "bot_enabled",
                false
        )) {
            return;
        }

        BackgroundRuntime.markListenerConnected(
                context,
                false
        );

        if (prefs.getBoolean(
                "auto_reconnect_engine",
                true
        )) {
            BackgroundRuntime.requestListenerRebind(
                    context
            );
        }
    }
}
