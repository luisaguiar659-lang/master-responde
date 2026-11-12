package com.masterresponde.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

public class TriggerSimulatorActivity extends Activity {
    private final int bg = Color.rgb(8, 11, 16);
    private final int panel = Color.rgb(20, 27, 36);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(160, 174, 190);
    private final int accent = Color.rgb(0, 190, 160);
    private EditText message;
    private CheckBox masterExact, stvExact;
    private LinearLayout result;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(16));
        root.setBackgroundColor(bg);
        root.addView(text("SIMULADOR DE GATILHOS", 21, white, true));
        root.addView(text("Teste uma mensagem sem enviá-la ao WhatsApp.", 12, muted, false));

        message = new EditText(this);
        message.setHint("Digite aqui a mensagem para simular");
        message.setTextColor(white);
        message.setHintTextColor(muted);
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        message.setMinLines(3);
        message.setGravity(48);
        message.setPadding(dp(12), dp(12), dp(12), dp(12));
        message.setBackground(round(panel));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.setMargins(0, dp(16), 0, dp(8));
        root.addView(message, mp);

        android.content.SharedPreferences master = getSharedPreferences("master_responde", MODE_PRIVATE);
        android.content.SharedPreferences stv = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
        masterExact = new CheckBox(this);
        masterExact.setText("MasterFlix: correspondência exata");
        masterExact.setTextColor(white);
        masterExact.setChecked(master.getBoolean(MessageSettings.KEY_TEST_MATCH_EXACT, true));
        stvExact = new CheckBox(this);
        stvExact.setText("STV+: correspondência exata");
        stvExact.setTextColor(white);
        stvExact.setChecked(stv.getBoolean(StvTriggerSettingsActivity.KEY_MATCH_EXACT, true));
        root.addView(masterExact);
        root.addView(stvExact);

        Button run = new Button(this);
        run.setText("SIMULAR MENSAGEM");
        run.setOnClickListener(v -> simulate());
        root.addView(run);

        ScrollView scroll = new ScrollView(this);
        result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(0, dp(12), 0, dp(20));
        scroll.addView(result);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void simulate() {
        result.removeAllViews();
        String incoming = normalize(message.getText().toString());
        if (incoming.isEmpty()) {
            addResult("Digite uma mensagem para iniciar a simulação.", muted);
            return;
        }

        android.content.SharedPreferences master = getSharedPreferences("master_responde", MODE_PRIVATE);
        android.content.SharedPreferences stv = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
        boolean found = false;

        String masterTriggers = master.getString(MessageSettings.KEY_TEST_TRIGGERS,
                MessageSettings.DEFAULT_TEST_TRIGGERS);
        if (matchesList(incoming, masterTriggers, masterExact.isChecked())) {
            addResult("MASTERFLIX — gatilho de teste reconhecido", accent);
            addResult("Ação prevista: iniciar geração do teste MasterFlix.", white);
            found = true;
        }

        String stvTriggers = stv.getString(StvTriggerSettingsActivity.KEY_TRIGGERS,
                StvTriggerSettingsActivity.DEFAULT_TRIGGERS);
        if (matchesList(incoming, stvTriggers, stvExact.isChecked())) {
            addResult("STV+ — gatilho de teste reconhecido", accent);
            addResult("Ação prevista: iniciar geração do teste STV+.", white);
            found = true;
        }

        JSONArray rules = readRules(master);
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.optJSONObject(i);
            if (rule == null) continue;
            String trigger = normalize(rule.optString("trigger", ""));
            if (trigger.isEmpty()) continue;
            boolean exact = rule.optBoolean("exact", false);
            boolean match = exact ? incoming.equals(trigger) : incoming.contains(trigger);
            if (!match) continue;
            String category = rule.optString("category_id", "");
            if (!category.isEmpty()) continue;
            addResult("REGRA: " + rule.optString("trigger", "Sem nome"), accent);
            addResult("Correspondência: " + (exact ? "EXATA" : "CONTÉM"), muted);
            addResult("Resposta prevista: " + rule.optString("reply", "(sem resposta cadastrada)"), white);
            found = true;
        }

        if (!found) addResult("Nenhum gatilho ou regra cadastrada seria acionado.", muted);
        addResult("Simulação concluída. Nenhuma mensagem foi enviada e nenhuma configuração foi alterada.", muted);
    }

    private JSONArray readRules(android.content.SharedPreferences prefs) {
        try { return new JSONArray(prefs.getString("text_rules", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private boolean matchesList(String incoming, String configured, boolean exact) {
        if (configured == null) return false;
        String value = configured.replace("\\n", "\n");
        for (String line : value.split("[\\n;]+")) {
            String trigger = normalize(line);
            if (trigger.isEmpty()) continue;
            if (exact ? incoming.equals(trigger) : incoming.contains(trigger)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private void addResult(String value, int color) {
        TextView t = text(value, 13, color, false);
        t.setPadding(dp(12), dp(9), dp(12), dp(9));
        result.addView(t);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private GradientDrawable round(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
