package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

public class GroupsActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout content;

    private static final String[] MENU_CATEGORIES = new String[]{
            "Tutoriais",
            "Promoções e Planos",
            "Aplicativos Parceiros",
            "Banners"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("master_responde", MODE_PRIVATE);
        buildScreen();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) render();
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
        titles.addView(text("GRUPOS", 21, Color.WHITE, true));
        titles.addView(text(
                "Menu rápido por @infor",
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

    private void render() {
        content.removeAllViews();

        LinearLayout command = card();

        command.addView(text(
                "COMANDO DO GRUPO",
                11,
                Color.rgb(255, 150, 0),
                true
        ));

        command.addView(text(
                "@infor",
                24,
                Color.WHITE,
                true
        ));

        command.addView(text(
                "No grupo, o MASTER RESPONDE fica em silêncio. " +
                "Ele abre o menu somente quando alguém envia @infor.",
                12,
                Color.rgb(155, 165, 175),
                false
        ));

        content.addView(command);

        gap(14);

        LinearLayout menu = card();

        menu.addView(text(
                "🤖 MASTER RESPONDE — CENTRAL DE SUPORTE",
                16,
                Color.WHITE,
                true
        ));

        menu.addView(text(
                "\n1️⃣ Tutoriais\n" +
                "2️⃣ Promoções e Planos\n" +
                "3️⃣ Aplicativos Parceiros\n" +
                "4️⃣ Banners\n\n" +
                "Digite o número da opção desejada.\n\n" +
                "ATALHOS DIRETOS:\n" +
                "@infor 1\n" +
                "@infor planos\n" +
                "@infor aplicativos\n" +
                "@infor banners",
                14,
                Color.rgb(210, 220, 225),
                false
        ));

        content.addView(menu);

        gap(14);

        content.addView(text(
                "STATUS DAS CATEGORIAS 1 A 4",
                12,
                Color.rgb(255, 150, 0),
                true
        ));

        gap(6);

        for (int i = 0; i < MENU_CATEGORIES.length; i++) {
            String category = MENU_CATEGORIES[i];
            boolean configured = hasCategoryWithResponses(category);

            LinearLayout item = card();

            item.addView(text(
                    (i + 1) + " • " + category,
                    14,
                    Color.WHITE,
                    true
            ));

            item.addView(text(
                    configured
                            ? "● Configurada"
                            : "● Ainda não configurada em Mídias",
                    12,
                    configured
                            ? Color.rgb(0, 184, 255)
                            : Color.rgb(255, 145, 0),
                    true
            ));

            content.addView(item);
            gap(7);
        }

        content.addView(text(
                "Nesta v1.5.6, as opções 7, 8 e 0 aparecem no menu, " +
                "mas ainda não executam automações. Elas entram nas próximas etapas.",
                12,
                Color.rgb(140, 150, 160),
                false
        ));
    }

    private boolean hasCategoryWithResponses(String wantedName) {
        try {
            JSONArray categories = new JSONArray(
                    prefs.getString("text_categories", "[]")
            );

            String wanted = normalize(wantedName);

            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.optJSONObject(i);
                if (category == null) continue;

                if (!wanted.equals(
                        normalize(category.optString("name")))) {
                    continue;
                }

                JSONArray responses =
                        category.optJSONArray("responses");

                return responses != null
                        && responses.length() > 0;
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";

        return Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(15), dp(15), dp(15));
        c.setBackgroundResource(getResources().getIdentifier(
                "card", "drawable", getPackageName()));
        return c;
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
