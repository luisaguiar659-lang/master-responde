package com.masterresponde.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

public class StvTriggerSettingsActivity extends Activity {
    public static final String KEY_TRIGGERS = "cfg_stv_test_triggers";
    public static final String DEFAULT_TRIGGERS =
            "teste stv\\n" +
            "teste stv+\\n" +
            "teste stv plus\\n" +
            "quero teste stv\\n" +
            "quero teste stv+\\n" +
            "quero um teste stv\\n" +
            "quero um teste stv+\\n" +
            "gerar teste stv\\n" +
            "gerar teste stv+\\n" +
            "gerar um teste stv\\n" +
            "gerar um teste stv+";

    private EditText triggers;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("master_responde_stv", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(7, 9, 11));

        TextView title = new TextView(this);
        title.setText("GATILHOS STV+");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Um gatilho por linha. O STV+ começa com frases equivalentes às do MasterFlix, mas identificadas com STV+ para não haver conflito entre os módulos.");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(14);
        hint.setPadding(0, 12, 0, 16);
        root.addView(hint);

        triggers = new EditText(this);
        triggers.setTextColor(Color.WHITE);
        triggers.setHintTextColor(Color.GRAY);
        triggers.setHint("Digite um gatilho por linha");
        triggers.setGravity(Gravity.TOP | Gravity.START);
        triggers.setMinLines(7);
        triggers.setPadding(16, 16, 16, 16);
        triggers.setBackgroundColor(Color.rgb(28, 32, 38));
        triggers.setText(prefs.getString(KEY_TRIGGERS, DEFAULT_TRIGGERS));
        root.addView(triggers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button save = new Button(this);
        save.setText("SALVAR GATILHOS");
        save.setOnClickListener(v -> {
            String value = triggers.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "Cadastre pelo menos um gatilho.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(KEY_TRIGGERS, value).apply();
            Toast.makeText(this, "Gatilhos do STV+ salvos.", Toast.LENGTH_SHORT).show();
            finish();
        });
        root.addView(save);

        Button restore = new Button(this);
        restore.setText("RESTAURAR PADRÃO");
        restore.setOnClickListener(v -> triggers.setText(DEFAULT_TRIGGERS));
        root.addView(restore);

        setContentView(root);
    }
}
