package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Registro isolado de auditoria. Não participa da decisão nem do envio de respostas. */
public final class EventLog {
    private static final String PREFS = "master_responde_audit";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_EVENTS = 400;

    private EventLog() {}

    public static synchronized void add(Context context, String type,
                                         String module, String detail,
                                         String result) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString(KEY_EVENTS, "[]"));
            JSONArray next = new JSONArray();
            int start = Math.max(0, old.length() - MAX_EVENTS + 1);
            for (int i = start; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null) next.put(item);
            }
            JSONObject event = new JSONObject();
            event.put("time", System.currentTimeMillis());
            event.put("type", safe(type));
            event.put("module", safe(module));
            event.put("detail", safe(detail));
            event.put("result", safe(result));
            next.put(event);
            prefs.edit().putString(KEY_EVENTS, next.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static JSONArray read(Context context) {
        try {
            return new JSONArray(context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_EVENTS, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    public static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS,
                Context.MODE_PRIVATE).edit().remove(KEY_EVENTS).apply();
    }

    public static String date(long time) {
        return new SimpleDateFormat("dd/MM HH:mm:ss", new Locale("pt", "BR"))
                .format(new Date(time));
    }

    private static String safe(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
    }
}
