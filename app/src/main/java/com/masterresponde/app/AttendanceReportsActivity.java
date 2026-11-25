package com.masterresponde.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Relatórios locais e exportação CSV dos atendimentos armazenados no SQLite. */
public class AttendanceReportsActivity extends Activity {
    private static final int REQUEST_EXPORT_CSV = 4107;
    private ConversationStore store;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConversationStore(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (store != null) render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(16));

        SQLiteDatabase db = store.getReadableDatabase();
        addMetric(root, "TOTAL DE ATENDIMENTOS", count(db,
                "SELECT COUNT(*) FROM conversations"), Color.rgb(0, 200, 255));
        addMetric(root, "EM ANDAMENTO", count(db,
                "SELECT COUNT(*) FROM conversations WHERE status='OPEN'"), Color.rgb(0, 210, 160));
        addMetric(root, "AGUARDANDO", count(db,
                "SELECT COUNT(*) FROM conversations WHERE status='WAITING'"), Color.rgb(255, 190, 70));
        addMetric(root, "RESOLVIDOS", count(db,
                "SELECT COUNT(*) FROM conversations WHERE status='RESOLVED'"), Color.rgb(100, 210, 140));
        addMetric(root, "PAUSADOS", count(db,
                "SELECT COUNT(*) FROM conversations WHERE status='PAUSED'"), Color.LTGRAY);
        addMetric(root, "PRIORIDADE ALTA", count(db,
                "SELECT COUNT(*) FROM conversations WHERE priority='HIGH'"), Color.rgb(255, 190, 70));
        addMetric(root, "URGENTES", count(db,
                "SELECT COUNT(*) FROM conversations WHERE priority='URGENT'"), Color.rgb(255, 90, 90));
        addMetric(root, "MENSAGENS REGISTRADAS", count(db,
                "SELECT COUNT(*) FROM messages"), Color.rgb(0, 200, 255));

        Button export = new Button(this);
        export.setText("EXPORTAR HISTÓRICO COMPLETO (CSV)");
        export.setOnClickListener(v -> exportCsv());
        root.addView(export);

        TextView privacy = new TextView(this);
        privacy.setText("Os relatórios são calculados no próprio aparelho. A exportação só acontece quando você escolhe onde salvar o arquivo.");
        privacy.setTextColor(Color.LTGRAY);
        privacy.setTextSize(12);
        privacy.setPadding(0, dp(10), 0, dp(8));
        root.addView(privacy);

        setContentView(NeonUi.wrap(this, "RELATÓRIOS",
                "Estatísticas locais da central de atendimentos", root));
    }

    private long count(SQLiteDatabase db, String sql) {
        Cursor c = db.rawQuery(sql, null);
        try { return c.moveToFirst() ? c.getLong(0) : 0L; }
        finally { c.close(); }
    }

    private void addMetric(LinearLayout root, String label, long value, int color) {
        TextView item = new TextView(this);
        item.setText(label + "\\n" + value);
        item.setTextColor(Color.WHITE);
        item.setTextSize(15);
        item.setTypeface(null, android.graphics.Typeface.BOLD);
        item.setPadding(dp(14), dp(10), dp(14), dp(10));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.rgb(5, 24, 43));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), color);
        item.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        root.addView(item, lp);
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "master-responde-atendimentos.csv");
        startActivityForResult(intent, REQUEST_EXPORT_CSV);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT_CSV || resultCode != RESULT_OK ||
                data == null || data.getData() == null) return;
        try {
            writeCsv(data.getData());
            Toast.makeText(this, "Histórico exportado com sucesso.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível exportar: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void writeCsv(Uri uri) throws Exception {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID;Tipo;Nome;Última mensagem;Atualizado em;Status;Prioridade;Não lidas;Etiquetas;Observações;Quantidade de mensagens\\n");
        Cursor c = store.getReadableDatabase().rawQuery(
                "SELECT c.id,c.kind,c.display_name,c.last_message,c.updated_at,c.status," +
                "c.priority,c.unread_count,c.labels,c.notes," +
                "(SELECT COUNT(*) FROM messages m WHERE m.conversation_id=c.id) " +
                "FROM conversations c ORDER BY c.updated_at DESC", null);
        try {
            while (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    if (i > 0) csv.append(';');
                    csv.append(csvCell(c.isNull(i) ? "" : c.getString(i)));
                }
                csv.append('\\n');
            }
        } finally {
            c.close();
        }
        try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
            if (out == null) throw new IllegalStateException("Destino de arquivo indisponível");
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value.replace("\\r", " ").replace("\\n", " ");
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (store != null) {
            store.close();
            store = null;
        }
        super.onDestroy();
    }
}
