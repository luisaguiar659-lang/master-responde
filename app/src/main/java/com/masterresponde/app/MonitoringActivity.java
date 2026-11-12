package com.masterresponde.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

public class MonitoringActivity extends Activity {
    private final int bg = Color.rgb(5, 17, 11);
    private final int card = Color.rgb(10, 27, 17);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(160, 174, 190);
    private final int green = Color.rgb(0, 214, 143);
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        root.setBackgroundColor(bg);
        root.addView(text("MONITORAMENTO", 22, white, true));
        root.addView(text("Visão geral do funcionamento", 13, muted, false));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        android.content.SharedPreferences prefs = getSharedPreferences("master_responde", MODE_PRIVATE);
        android.content.SharedPreferences stv = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
        boolean bot = prefs.getBoolean("bot_enabled", false);
        boolean listener = BackgroundRuntime.hasNotificationAccess(this);
        addCard("MOTOR DE RESPOSTAS", bot ? "ATIVADO" : "DESATIVADO",
                bot ? green : Color.rgb(255, 170, 40), "Controle principal do aplicativo");
        addCard("ACESSO AO WHATSAPP", listener ? "PERMISSÃO LIBERADA" : "VERIFICAR PERMISSÃO",
                listener ? green : Color.rgb(255, 170, 40), "Acesso às notificações do WhatsApp Business");
        addCard("MASTERFLIX", prefs.getString("last_test_status", "Nenhum teste registrado"),
                white, "Último estado informado pelo módulo");
        addCard("STV+ — TESTES", stv.getString("last_stv_status", "Nenhum teste registrado"),
                white, "Último estado de geração de teste");
        addCard("STV+ — REVENDAS", stv.getString("last_stv_reseller_status", "Nenhuma atividade registrada"),
                white, "Último estado do fluxo de revendas");

        JSONArray events = EventLog.read(this);
        int received = 0, sent = 0, errors = 0;
        for (int i = 0; i < events.length(); i++) {
            org.json.JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            String type = e.optString("type");
            if ("MENSAGEM RECEBIDA".equals(type)) received++;
            if ("RESPOSTA ENVIADA".equals(type)) sent++;
            if ("FALHA".equals(type)) errors++;
        }
        addCard("MENSAGENS RECEBIDAS", String.valueOf(received), white, "Eventos registrados nesta instalação");
        addCard("RESPOSTAS CONFIRMADAS", String.valueOf(sent), green, "Envios confirmados pelo serviço");
        addCard("FALHAS REGISTRADAS", String.valueOf(errors), errors > 0 ? Color.rgb(255,100,90) : green,
                "Ocorrências registradas no histórico");
        addCard("ÚLTIMA ATIVIDADE", events.length() == 0 ? "Sem registros" :
                EventLog.date(events.optJSONObject(events.length()-1).optLong("time")),
                white, "Abra HISTÓRICO para consultar os detalhes");
    }

    private void addCard(String title, String value, int valueColor, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(13), dp(15), dp(13));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(card);
        shape.setCornerRadius(dp(14));
        box.setBackground(shape);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        box.setLayoutParams(lp);
        box.addView(text(title, 11, muted, true));
        box.addView(text(value, 18, valueColor, true));
        box.addView(text(description, 11, muted, false));
        content.addView(box);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(3), 0, dp(3));
        return t;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
