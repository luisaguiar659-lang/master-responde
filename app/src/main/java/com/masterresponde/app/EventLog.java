package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** Registro isolado de auditoria. Não participa da decisão nem do envio de respostas. */
public final class EventLog {
    private static final String PREFS = "master_responde_audit";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_EVENTS = 400;
    private static final String KEY_STATS = "stats_v1";
    private static final int KEEP_DAYS = 45;

    /** Contadores permanentes (não são limitados pelas 400 últimas linhas do histórico). */
    public static final class Stats {
        /** [recebidas, respondidas, falhas, testes] desde o início. */
        public final int[] total = new int[4];
        /** Chave yyyyMMdd -> [recebidas, respondidas, falhas, testes]. */
        public final Map<String, int[]> days = new HashMap<>();

        public int[] day(long time) {
            int[] v = days.get(dayKey(time));
            return v == null ? new int[4] : v;
        }
    }

    private EventLog() {}

    public static synchronized void add(Context context, String type,
                                         String module, String detail,
                                         String result) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString(KEY_EVENTS, "[]"));
            ensureStats(prefs, old);
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
            JSONObject stats = new JSONObject(prefs.getString(KEY_STATS, "{}"));
            bump(stats, event.optLong("time"), event.optString("type"), event.optString("result"));
            prefs.edit()
                    .putString(KEY_EVENTS, next.toString())
                    .putString(KEY_STATS, stats.toString())
                    .apply();
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
                Context.MODE_PRIVATE).edit().remove(KEY_EVENTS).remove(KEY_STATS).apply();
    }

    /** Lê os contadores permanentes (cria a partir do histórico existente na primeira vez). */
    public static synchronized Stats stats(Context context) {
        Stats out = new Stats();
        if (context == null) return out;
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            ensureStats(prefs, new JSONArray(prefs.getString(KEY_EVENTS, "[]")));
            JSONObject stats = new JSONObject(prefs.getString(KEY_STATS, "{}"));
            JSONArray tot = stats.optJSONArray("tot");
            for (int i = 0; tot != null && i < 4; i++) out.total[i] = tot.optInt(i);
            JSONObject days = stats.optJSONObject("days");
            if (days != null) {
                Iterator<String> it = days.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    JSONArray a = days.optJSONArray(key);
                    if (a == null) continue;
                    int[] v = new int[4];
                    for (int i = 0; i < 4; i++) v[i] = a.optInt(i);
                    out.days.put(key, v);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static String dayKey(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        int key = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100
                + c.get(Calendar.DAY_OF_MONTH);
        return String.valueOf(key);
    }

    private static void ensureStats(SharedPreferences prefs, JSONArray events) {
        try {
            if (prefs.contains(KEY_STATS)) return;
            JSONObject stats = new JSONObject();
            for (int i = 0; i < events.length(); i++) {
                JSONObject e = events.optJSONObject(i);
                if (e == null) continue;
                bump(stats, e.optLong("time"), e.optString("type"), e.optString("result"));
            }
            prefs.edit().putString(KEY_STATS, stats.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static void bump(JSONObject stats, long time, String type, String result) throws Exception {
        int idx;
        if ("MENSAGEM RECEBIDA".equals(type)) idx = 0;
        else if ("RESPOSTA ENVIADA".equals(type)) idx = 1;
        else if ("FALHA".equals(type)) idx = 2;
        else return;
        boolean test = idx == 1 && "TESTE ENVIADO".equals(result);

        JSONArray tot = stats.optJSONArray("tot");
        if (tot == null) tot = new JSONArray();
        addTo(tot, idx, test);
        stats.put("tot", tot);

        JSONObject days = stats.optJSONObject("days");
        if (days == null) days = new JSONObject();
        String key = dayKey(time);
        JSONArray day = days.optJSONArray(key);
        if (day == null) day = new JSONArray();
        addTo(day, idx, test);
        days.put(key, day);

        // descarta dias muito antigos para o armazenamento não crescer
        String limit = dayKey(System.currentTimeMillis() - KEEP_DAYS * 86400000L);
        Iterator<String> it = days.keys();
        java.util.List<String> drop = new java.util.ArrayList<>();
        while (it.hasNext()) {
            String k = it.next();
            if (k.compareTo(limit) < 0) drop.add(k);
        }
        for (String k : drop) days.remove(k);
        stats.put("days", days);
    }

    private static void addTo(JSONArray arr, int idx, boolean test) throws Exception {
        while (arr.length() < 4) arr.put(0);
        arr.put(idx, arr.optInt(idx) + 1);
        if (test) arr.put(3, arr.optInt(3) + 1);
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
