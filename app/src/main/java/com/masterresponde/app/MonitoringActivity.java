package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

public class MonitoringActivity extends Activity {
    private static final int GREEN = Color.rgb(0, 214, 143);
    private static final int AMBER = Color.rgb(255, 170, 40);
    private static final int RED = Color.rgb(255, 100, 90);
    private static final int CYAN = Color.rgb(0, 200, 255);
    private static final int PURPLE = Color.rgb(170, 110, 255);
    private static final int ORANGE = Color.rgb(255, 145, 20);

    private LinearLayout content;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (!isFinishing()) {
                render();
                refreshHandler.postDelayed(this, 20000L);
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(content);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "MONITORAMENTO", "Visão geral do funcionamento", body));
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) {
            render();
            refreshHandler.removeCallbacks(refreshTask);
            refreshHandler.postDelayed(refreshTask, 20000L);
        }
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(refreshTask);
        super.onPause();
    }

    private void render() {
        content.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("master_responde", MODE_PRIVATE);
        SharedPreferences stv = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
        boolean bot = prefs.getBoolean("bot_enabled", false);
        boolean listener = BackgroundRuntime.hasNotificationAccess(this);
        boolean serviceConnected = prefs.getBoolean(
                BackgroundRuntime.KEY_LISTENER_CONNECTED,
                false
        );
        long lastEventTime = prefs.getLong(
                BackgroundRuntime.KEY_LAST_EVENT_TIME,
                0L
        );

        EventLog.Stats st = EventLog.stats(this);
        int received = st.total[0];
        int sent = st.total[1];
        int errors = st.total[2];
        int rate = received <= 0 ? 0 : Math.min(100, Math.round(sent * 100f / received));

        // ---- resumo com anel ----
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(10), dp(10), dp(14), dp(10));
        hero.setBackgroundResource(R.drawable.card);
        NeonRingView ring = new NeonRingView(this);
        ring.setPercent(rate);
        hero.addView(ring, new LinearLayout.LayoutParams(dp(124), dp(124)));
        LinearLayout nums = new LinearLayout(this);
        nums.setOrientation(LinearLayout.VERTICAL);
        nums.setPadding(dp(8), 0, 0, 0);
        nums.addView(metric("Mensagens recebidas", String.valueOf(received), CYAN));
        nums.addView(metric("Respostas confirmadas", String.valueOf(sent), GREEN));
        nums.addView(metric("Falhas registradas", String.valueOf(errors), errors > 0 ? RED : GREEN));
        hero.addView(nums, new LinearLayout.LayoutParams(0, -2, 1f));
        content.addView(hero, lp(0, 0, 0, dp(6)));

        content.addView(section("ESTADO DO SISTEMA"));
        content.addView(NeonUi.statusCard(this, "robot", bot ? GREEN : AMBER, "Motor de respostas",
                bot ? "ATIVADO" : "DESATIVADO", "Controle principal do aplicativo"));
        content.addView(NeonUi.statusCard(this, "whatsapp", listener ? GREEN : AMBER, "Acesso ao WhatsApp",
                listener ? "PERMISSÃO LIBERADA" : "VERIFICAR PERMISSÃO", "Acesso às notificações do WhatsApp Business"));
        content.addView(NeonUi.statusCard(this, "activity", serviceConnected ? GREEN : AMBER,
                "Serviço de notificações", serviceConnected ? "CONECTADO" : "AGUARDANDO CONEXÃO",
                "Estado real informado pelo Android; atualização automática a cada 20 segundos"));
        content.addView(NeonUi.statusCard(this, "clock", CYAN, "Último evento recebido",
                lastEventTime <= 0L ? "Nenhum evento registrado" : EventLog.date(lastEventTime),
                "Horário do último evento de notificação processado"));

        content.addView(section("MÓDULOS"));
        content.addView(NeonUi.statusCard(this, "play", ORANGE, "MasterFlix",
                prefs.getString("last_test_status", "Nenhum teste registrado"), "Último estado informado pelo módulo"));
        content.addView(NeonUi.statusCard(this, "play", PURPLE, "STV+ — Testes",
                stv.getString("last_stv_status", "Nenhum teste registrado"), "Último estado de geração de teste"));
        content.addView(NeonUi.statusCard(this, "people", PURPLE, "STV+ — Revendas",
                stv.getString("last_stv_reseller_status", "Nenhuma atividade registrada"), "Último estado do fluxo de revendas"));

        JSONArray events = EventLog.read(this);
        content.addView(section("ATIVIDADE"));
        content.addView(NeonUi.statusCard(this, "clock", CYAN, "Última atividade",
                events.length() == 0 ? "Sem registros"
                        : EventLog.date(events.optJSONObject(events.length() - 1).optLong("time")),
                "Abra o Histórico para consultar os detalhes"));
    }

    private LinearLayout metric(String label, String value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(4), 0, dp(4));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(22);
        v.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        box.addView(v);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.rgb(150, 180, 205));
        l.setTextSize(11);
        box.addView(l);
        return box;
    }

    private TextView section(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.rgb(255, 145, 20));
        t.setTextSize(12);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLayoutParams(lp(0, dp(14), 0, dp(4)));
        return t;
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
