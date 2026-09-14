package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RulesActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("master_responde", MODE_PRIVATE);
        buildScreen();
        renderRules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) renderRules();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 9, 11));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(Color.WHITE);
        back.setTextSize(24);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(58), dp(54)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("REGRAS", 21, Color.WHITE, true));
        titles.addView(text(
                "Resposta única ou categoria com sequência",
                12,
                Color.rgb(150, 160, 170),
                false
        ));
        header.addView(titles, new LinearLayout.LayoutParams(0, dp(58), 1f));

        root.addView(header);

        View line = new View(this);
        line.setBackgroundResource(getResources().getIdentifier(
                "line", "drawable", getPackageName()));
        root.addView(line, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(40));
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void renderRules() {
        content.removeAllViews();

        Button simple = actionButton("+ REGRA COM RESPOSTA ÚNICA", true);
        simple.setOnClickListener(v -> showSimpleDialog());
        content.addView(simple);

        gap(12);

        content.addView(text(
                "REGRAS são usadas para respostas privadas simples. As categorias de MÍDIAS são exclusivas do menu @infor nos grupos e não podem ser usadas aqui.",
                12,
                Color.rgb(145, 155, 165),
                false
        ));

        gap(16);

        JSONArray rules = loadRules();

        if (rules.length() == 0) {
            LinearLayout empty = card();
            empty.addView(text(
                    "Nenhuma regra cadastrada.",
                    15,
                    Color.rgb(170, 180, 190),
                    false
            ));
            content.addView(empty);
            return;
        }

        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.optJSONObject(i);
            if (rule == null) continue;

            final String id = rule.optString("id");
            String trigger = rule.optString("trigger");
            String reply = rule.optString("reply");
            String categoryId = rule.optString("category_id");
            String categoryName = rule.optString("category_name");
            boolean exact = rule.optBoolean("exact", false);

            LinearLayout c = card();

            c.addView(text(trigger, 17, Color.WHITE, true));

            c.addView(text(
                    exact ? "Correspondência: EXATA" : "Correspondência: CONTÉM",
                    11,
                    Color.rgb(255, 150, 0),
                    true
            ));

            String destination;
            if (!categoryId.isEmpty()) {
                destination =
                        "DESATIVADA — categoria \"" +
                                categoryName +
                                "\" agora é exclusiva do @infor em grupos.";
            } else {
                destination = "Resposta: " + reply;
            }

            TextView response = text(
                    destination,
                    13,
                    Color.rgb(205, 215, 220),
                    false
            );
            response.setPadding(0, dp(10), 0, dp(8));
            c.addView(response);

            Button delete = actionButton("EXCLUIR", false);
            delete.setOnClickListener(v -> confirmDelete(id));
            c.addView(delete);

            content.addView(c);
            gap(10);
        }
    }

    private void showSimpleDialog() {
        LinearLayout box = dialogBox();

        EditText trigger = field("Palavra ou frase");
        box.addView(trigger);

        EditText reply = field("Resposta automática");
        reply.setMinLines(3);
        reply.setGravity(Gravity.TOP);
        box.addView(reply);

        CheckBox exact = exactCheck();
        box.addView(exact);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Regra com resposta única")
                .setView(box)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("SALVAR", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String t = trigger.getText().toString().trim();
                String r = reply.getText().toString().trim();

                if (t.isEmpty()) {
                    trigger.setError("Informe a palavra ou frase.");
                    return;
                }

                if (r.isEmpty()) {
                    reply.setError("Informe a resposta.");
                    return;
                }

                addSimpleRule(t, r, exact.isChecked());
                dialog.dismiss();
                renderRules();
            });
        });

        dialog.show();
    }

    private void showCategoryRuleDialog() {
        JSONArray categories = loadCategories();

        List<JSONObject> available = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category == null) continue;

            JSONArray responses = category.optJSONArray("responses");
            if (responses == null || !hasSendableResponse(responses)) continue;

            available.add(category);
            names.add(category.optString("name"));
        }

        if (available.isEmpty()) {
            Toast.makeText(
                    this,
                    "Crie primeiro uma categoria com pelo menos um texto ou link no módulo Mídias.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        LinearLayout box = dialogBox();

        EditText trigger = field("Palavra ou frase");
        box.addView(trigger);

        TextView label = text(
                "Categoria que será enviada:",
                12,
                Color.rgb(60, 60, 60),
                true
        );
        box.addView(label);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
        );
        spinner.setAdapter(adapter);
        box.addView(spinner);

        CheckBox exact = exactCheck();
        box.addView(exact);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Regra com categoria")
                .setView(box)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("SALVAR", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String t = trigger.getText().toString().trim();

                if (t.isEmpty()) {
                    trigger.setError("Informe a palavra ou frase.");
                    return;
                }

                int position = spinner.getSelectedItemPosition();

                if (position < 0 || position >= available.size()) {
                    return;
                }

                JSONObject selected = available.get(position);

                addCategoryRule(
                        t,
                        selected.optString("id"),
                        selected.optString("name"),
                        exact.isChecked()
                );

                dialog.dismiss();
                renderRules();
            });
        });

        dialog.show();
    }

    private boolean hasSendableResponse(JSONArray responses) {
        if (responses == null) {
            return false;
        }

        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.optJSONObject(i);
            if (response == null) continue;

            String type =
                    response.optString(
                            "type",
                            "text"
                    );

            if ("link".equals(type)) {
                if (!response.optString(
                        "url",
                        ""
                ).trim().isEmpty()) {
                    return true;
                }

                continue;
            }

            if ("photo".equals(type)) {
                continue;
            }

            if (!response.optString(
                    "text",
                    ""
            ).trim().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void addSimpleRule(String trigger, String reply, boolean exact) {
        JSONArray rules = loadRules();

        JSONObject rule = new JSONObject();

        try {
            rule.put("id", UUID.randomUUID().toString());
            rule.put("trigger", trigger);
            rule.put("reply", reply);
            rule.put("exact", exact);
            rules.put(rule);
            saveRules(rules);
            Toast.makeText(this, "Regra salva.", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void addCategoryRule(
            String trigger,
            String categoryId,
            String categoryName,
            boolean exact
    ) {
        JSONArray rules = loadRules();

        JSONObject rule = new JSONObject();

        try {
            rule.put("id", UUID.randomUUID().toString());
            rule.put("trigger", trigger);
            rule.put("reply", "");
            rule.put("category_id", categoryId);
            rule.put("category_name", categoryName);
            rule.put("exact", exact);
            rules.put(rule);
            saveRules(rules);
            Toast.makeText(this, "Regra com categoria salva.", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void confirmDelete(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir regra?")
                .setMessage("Essa regra deixará de responder automaticamente.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("EXCLUIR", (d, w) -> {
                    deleteRule(id);
                    renderRules();
                })
                .show();
    }

    private void deleteRule(String id) {
        JSONArray oldRules = loadRules();
        JSONArray newRules = new JSONArray();

        for (int i = 0; i < oldRules.length(); i++) {
            JSONObject rule = oldRules.optJSONObject(i);
            if (rule == null) continue;

            if (!id.equals(rule.optString("id"))) {
                newRules.put(rule);
            }
        }

        saveRules(newRules);
    }

    private JSONArray loadRules() {
        try {
            return new JSONArray(
                    prefs.getString("text_rules", "[]")
            );
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveRules(JSONArray rules) {
        prefs.edit()
                .putString("text_rules", rules.toString())
                .apply();
    }

    private JSONArray loadCategories() {
        try {
            return new JSONArray(
                    prefs.getString("text_categories", "[]")
            );
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private CheckBox exactCheck() {
        CheckBox exact = new CheckBox(this);
        exact.setText("Responder somente se a mensagem for exatamente igual");
        exact.setTextColor(Color.rgb(40, 40, 40));
        return exact;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(8));
        return box;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(15), dp(15), dp(15));
        c.setBackgroundResource(getResources().getIdentifier(
                "card", "drawable", getPackageName()));
        return c;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.BLACK);
        e.setHintTextColor(Color.rgb(120, 120, 120));
        e.setTextSize(14);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(5), 0, dp(10));
        e.setLayoutParams(lp);

        return e;
    }

    private Button actionButton(String label, boolean orange) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(null, 1);
        b.setBackgroundResource(getResources().getIdentifier(
                orange ? "button_orange" : "button_dark",
                "drawable",
                getPackageName()
        ));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);

        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private void gap(int height) {
        View v = new View(this);
        content.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
