package com.masterresponde.app;

import android.content.SharedPreferences;

/** Configurações de tempos técnicos das automações, preservando os padrões atuais. */
public final class AutomationTiming {
    public static final String KEY_PENDING_TIMEOUT_MS = "timing_pending_timeout_ms";
    public static final String KEY_ECHO_WINDOW_MS = "timing_echo_window_ms";
    public static final String KEY_MASTERFLIX_QUEUE_RETRY_MS = "timing_masterflix_queue_retry_ms";
    public static final String KEY_STV_QUEUE_RETRY_MS = "timing_stv_queue_retry_ms";

    public static final int DEFAULT_PENDING_TIMEOUT_MS = 120000;
    public static final int DEFAULT_ECHO_WINDOW_MS = 30000;
    public static final int DEFAULT_MASTERFLIX_QUEUE_RETRY_MS = 2000;
    public static final int DEFAULT_STV_QUEUE_RETRY_MS = 1500;

    private AutomationTiming() {}

    public static long getMilliseconds(SharedPreferences prefs, String key, int defaultValue) {
        int value = prefs == null ? defaultValue : prefs.getInt(key, defaultValue);
        return Math.max(100L, Math.min(600000L, value));
    }
}
