package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class EventHistoryActivity extends Activity {
    private LinearLayout list;
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(150, 180, 205);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
        render();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(10));

        Button clear = new Button(this);
        clear.setText("LIMPAR HISTÓRICO");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Limpar histórico?")
                .setMessage("Os registros locais e os contadores serão apagados. Isso não altera regras nem configurações.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("LIMPAR", (d, w) -> {
                    EventLog.clear(this);
                    render();
                }).show());
        root.addView(clear);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(20));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "HISTÓRICO", "Registro local de eventos do MASTER RESPONDE", root));
    }

    private void render() {
        list.removeAllViews();
        JSONArray events = EventLog.read(this);
        if (events.length() == 0) {
            LinearLayout empty = NeonUi.statusCard(this, "clock", Color.rgb(0, 200, 255),
                    "Histórico", "Nenhuma atividade registrada ainda", "Os atendimentos aparecerão aqui.");
            list.addView(empty);
            return;
        }
        for (int i = events.length() - 1; i >= 0; i--) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            String type = e.optString("type");
            String icon = "bolt";
            int tone = Color.rgb(0, 200, 255);
            if ("MENSAGEM RECEBIDA".equals(type)) { icon = "chat"; tone = Color.rgb(0, 214, 143); }
            else if ("RESPOSTA ENVIADA".equals(type)) { icon = "reply"; tone = Color.rgb(0, 200, 255); }
            else if ("FALHA".equals(type)) { icon = "warning"; tone = Color.rgb(255, 100, 90); }

            String title = type + (e.optString("module").isEmpty() ? "" : " · " + e.optString("module"));
            LinearLayout card = NeonUi.statusCard(this, icon, tone, EventLog.date(e.optLong("time")),
                    title, e.optString("detail"));
            String result = e.optString("result");
            if (!result.isEmpty()) {
                TextView r = new TextView(this);
                r.setText("Resultado: " + result);
                r.setTextColor(muted);
                r.setTextSize(11);
                ((LinearLayout) card.getChildAt(1)).addView(r);
            }
            list.addView(card);
        }
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
