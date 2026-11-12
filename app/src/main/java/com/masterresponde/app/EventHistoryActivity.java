package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
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
    private final int bg = Color.rgb(5, 17, 11);
    private final int card = Color.rgb(10, 27, 17);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(160, 174, 190);
    private final int accent = Color.rgb(0, 214, 143);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
        render();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(16));
        root.setBackgroundColor(bg);

        TextView title = text("HISTÓRICO DE ATIVIDADES", 20, white, true);
        root.addView(title);
        root.addView(text("Registro local de eventos do MASTER RESPONDE", 12, muted, false));

        Button clear = new Button(this);
        clear.setText("LIMPAR HISTÓRICO");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Limpar histórico?")
                .setMessage("Os registros locais serão apagados. Isso não altera regras nem configurações.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("LIMPAR", (d, w) -> {
                    EventLog.clear(this);
                    render();
                }).show());
        root.addView(clear);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void render() {
        list.removeAllViews();
        JSONArray events = EventLog.read(this);
        if (events.length() == 0) {
            list.addView(text("Nenhuma atividade registrada ainda.", 14, muted, false));
            return;
        }
        for (int i = events.length() - 1; i >= 0; i--) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setColor(card);
            shape.setCornerRadius(dp(12));
            item.setBackground(shape);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(7), 0, dp(7));
            item.setLayoutParams(lp);
            item.addView(text(EventLog.date(e.optLong("time")), 11, muted, false));
            item.addView(text(e.optString("type") + " · " + e.optString("module"), 15, accent, true));
            item.addView(text(e.optString("detail"), 13, white, false));
            item.addView(text("Resultado: " + e.optString("result"), 11, muted, false));
            list.addView(item);
        }
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
