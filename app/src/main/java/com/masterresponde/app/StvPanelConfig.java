package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class StvPanelConfig {
    private static final String PREFS = "master_responde_stv";
    private static final String DEFAULT_URL = "https://stvmais.sigma.vin";
    private StvPanelConfig() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context c) {
        String value = prefs(c).getString("base_url", DEFAULT_URL);
        if (value == null || value.trim().isEmpty()) value = DEFAULT_URL;
        value = value.trim();
        if (!value.startsWith("https://") && !value.startsWith("http://")) value = "https://" + value;
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    public static void saveBaseUrl(Context c, String url) {
        prefs(c).edit().putString("base_url", url == null ? DEFAULT_URL : url.trim()).apply();
    }

    public static String getMode(Context c) {
        return prefs(c).getString("preferred_mode", "standard");
    }

    public static void saveMode(Context c, String mode) {
        prefs(c).edit().putString("preferred_mode", "adult".equals(mode) ? "adult" : "standard").apply();
    }

    public static String getUser(Context c) {
        return new SecureStore(c).get("stv_user");
    }

    public static String getPass(Context c) {
        return new SecureStore(c).get("stv_pass");
    }

    public static boolean saveCredentials(Context c, String user, String pass) {
        try {
            SecureStore store = new SecureStore(c);
            store.put("stv_user", user == null ? "" : user.trim());
            store.put("stv_pass", pass == null ? "" : pass);
            prefs(c).edit().putBoolean("credentials_saved",
                    user != null && !user.trim().isEmpty() && pass != null && !pass.isEmpty()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
