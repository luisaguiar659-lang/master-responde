package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class StvPanelConfig {
    public static final String DEFAULT_PANEL_NAME = "STV+";
    public static final String DEFAULT_BASE_URL = "https://stvmais.sigma.vin";
    private static final String PREFS = "master_responde_stv";
    private StvPanelConfig() {}
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static String getPanelName(Context c) { return DEFAULT_PANEL_NAME; }
    public static String getBaseUrl(Context c) { return normalizeBaseUrl(prefs(c).getString("base_url", DEFAULT_BASE_URL)); }
    public static String loginUrl(Context c) { return getBaseUrl(c) + "/#/sign-in?redirect=/dashboard"; }
    public static String dashboardUrl(Context c) { return getBaseUrl(c) + "/#/dashboard"; }
    public static String resellersUrl(Context c) { return getBaseUrl(c) + "/#/resellers"; }
    public static String getPreferredTest(Context c) { return "adult".equals(getMode(c)) ? "TESTE + ADULTO" : "TESTE SEM ADULTO"; }
    public static String getMode(Context c) { return "adult".equals(prefs(c).getString("preferred_mode", "standard")) ? "adult" : "standard"; }
    public static void saveMode(Context c, String mode) { prefs(c).edit().putString("preferred_mode", "adult".equals(mode) ? "adult" : "standard").apply(); }
    public static String normalizeBaseUrl(String value) {
        String base = value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value.trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "https://" + base;
        int hash = base.indexOf("#"); if (hash >= 0) base = base.substring(0, hash);
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }
    public static String getUser(Context c) { String value = new StvSecureStore(c).get("stv_user"); return value == null ? "" : value; }
    public static String getPass(Context c) { String value = new StvSecureStore(c).get("stv_pass"); return value == null ? "" : value; }
    public static void saveCredentials(Context c, String user, String pass) {
        boolean saved = false;
        try {
            StvSecureStore store = new StvSecureStore(c);
            store.put("stv_user", user == null ? "" : user.trim());
            store.put("stv_pass", pass == null ? "" : pass);
            saved = user != null && !user.trim().isEmpty() && pass != null && !pass.isEmpty();
        } catch (Exception ignored) {}
        prefs(c).edit()
                .putBoolean("stvplus_credentials_saved", saved)
                .putBoolean("credentials_saved", saved)
                .apply();
    }
}