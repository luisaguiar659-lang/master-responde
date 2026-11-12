package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BackupActivity extends Activity {
    private static final int CREATE_BACKUP = 701;
    private static final int RESTORE_BACKUP = 702;
    private final int bg = Color.rgb(5, 17, 11);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(160, 174, 190);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundResource(R.drawable.app_background);
        TextView title = text("BACKUP E RESTAURAÇÃO", 21, white, true);
        root.addView(title);
        root.addView(text("Salve e recupere suas regras e configurações sem alterar os módulos durante a operação.", 13, muted, false));

        Button export = new Button(this);
        export.setText("EXPORTAR BACKUP");
        export.setOnClickListener(v -> chooseExport());
        root.addView(export);

        Button restore = new Button(this);
        restore.setText("RESTAURAR BACKUP");
        restore.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Restaurar configurações?")
                .setMessage("As configurações presentes no arquivo serão mescladas às atuais. Não apagaremos dados que não estejam no backup.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("CONTINUAR", (d, w) -> chooseImport())
                .show());
        root.addView(restore);

        root.addView(text("Por segurança, senhas, tokens, credenciais e tarefas temporárias não são incluídos no backup.", 12, muted, false));
        setContentView(root);
    }

    private void chooseExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "master-responde-backup.json");
        startActivityForResult(intent, CREATE_BACKUP);
    }

    private void chooseImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, RESTORE_BACKUP);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == CREATE_BACKUP) exportTo(data.getData());
            if (requestCode == RESTORE_BACKUP) importFrom(data.getData());
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível concluir a operação do backup.", Toast.LENGTH_LONG).show();
        }
    }

    private void exportTo(Uri uri) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "MASTER_RESPONDE_BACKUP");
        root.put("version", 1);
        root.put("created_at", System.currentTimeMillis());
        root.put("master_responde", exportPrefs("master_responde"));
        root.put("master_responde_stv", exportPrefs("master_responde_stv"));
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("Arquivo indisponível");
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
        Toast.makeText(this, "Backup exportado com sucesso.", Toast.LENGTH_LONG).show();
    }

    private JSONObject exportPrefs(String name) throws Exception {
        JSONObject result = new JSONObject();
        Map<String, ?> values = getSharedPreferences(name, MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (excluded(key) || value == null || value instanceof java.util.Set) continue;
            if (value instanceof String || value instanceof Boolean || value instanceof Number) {
                result.put(key, value);
            }
        }
        return result;
    }

    private void importFrom(Uri uri) throws Exception {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Arquivo indisponível");
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] part = new byte[4096];
            int read;
            while ((read = in.read(part)) != -1) buffer.write(part, 0, read);
            bytes = buffer.toByteArray();
        }
        JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!"MASTER_RESPONDE_BACKUP".equals(root.optString("format"))
                || root.optInt("version") != 1) {
            throw new IllegalArgumentException("Formato de backup não reconhecido");
        }
        int restored = restorePrefs("master_responde", root.optJSONObject("master_responde"));
        restored += restorePrefs("master_responde_stv", root.optJSONObject("master_responde_stv"));
        Toast.makeText(this, restored + " configurações restauradas. Reinicie o motor para aplicar tudo.", Toast.LENGTH_LONG).show();
    }

    private int restorePrefs(String name, JSONObject values) {
        if (values == null) return 0;
        android.content.SharedPreferences.Editor editor = getSharedPreferences(name, MODE_PRIVATE).edit();
        int count = 0;
        JSONArray names = values.names();
        if (names == null) return 0;
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            if (excluded(key)) continue;
            Object value = values.opt(key);
            if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Double) editor.putFloat(key, ((Double) value).floatValue());
            else continue;
            count++;
        }
        editor.apply();
        return count;
    }

    private boolean excluded(String key) {
        String k = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
        return k.contains("password") || k.contains("senha") || k.contains("token")
                || k.contains("secret") || k.contains("credential") || k.contains("api_key")
                || k.contains("pending") || k.contains("locator") || k.contains("request_key")
                || k.contains("last_") || k.contains("email");
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
