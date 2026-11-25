package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConversationInboxActivity extends Activity {
    private static final String EXTRA_ID = "conversation_id";
    private ConversationStore store;
    private LinearLayout list;
    private String selectedId;
    private String filterStatus = "ALL";
    private String searchText = "";
    private LinearLayout detailsRoot;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConversationStore(this);
        selectedId = getIntent().getStringExtra(EXTRA_ID);
        if (selectedId == null) buildInbox(); else buildDetails();
    }

    @Override protected void onResume() {
        super.onResume();
        if (selectedId == null && list != null) renderInbox();
    }

    private void buildInbox() {
        LinearLayout root = column();
        Button history = new Button(this);
        history.setText("HISTÓRICO TÉCNICO DE EVENTOS");
        history.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, EventHistoryActivity.class)));
        root.addView(history);

        Button reports = new Button(this);
        reports.setText("RELATÓRIOS E EXPORTAR CSV");
        reports.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, AttendanceReportsActivity.class)));
        root.addView(reports);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Buscar nome, mensagem ou etiqueta");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.LTGRAY);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s == null ? "" : s.toString();
                renderInbox();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        root.addView(search);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(Gravity.CENTER);
        String[] labels = {"Todas", "Em andamento", "Aguardando", "Resolvido", "Pausado"};
        String[] values = {"ALL", "OPEN", "WAITING", "RESOLVED", "PAUSED"};
        for (int i = 0; i < labels.length; i++) {
            final String value = values[i];
            Button filter = new Button(this);
            filter.setText(labels[i]);
            filter.setTextSize(10);
            filter.setPadding(dp(4), 0, dp(4), 0);
            filter.setOnClickListener(v -> {
                filterStatus = value;
                renderInbox();
            });
            filters.addView(filter, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setFillViewport(true);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.addView(filters);
        root.addView(filterScroll, new LinearLayout.LayoutParams(-1, dp(54)));

        ScrollView scroll = new ScrollView(this);
        list = column();
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "ATENDIMENTOS",
                "Conversas recebidas pelo WhatsApp Business", root));
        renderInbox();
    }

    private void renderInbox() {
        if (list == null) return;
        list.removeAllViews();
        Cursor c = store.conversations(this, filterStatus, searchText);
        try {
            if (c.getCount() == 0) {
                list.addView(NeonUi.statusCard(this, "chat", Color.rgb(0, 200, 255),
                        "Nenhum atendimento encontrado",
                        "Altere a busca ou o filtro para ver outras conversas",
                        "As novas conversas aparecerão aqui quando chegarem."));
                return;
            }
            while (c.moveToNext()) {
                String id = c.getString(0);
                String kind = c.getString(1);
                String name = c.getString(2);
                String last = c.getString(3);
                long time = c.getLong(4);
                String status = c.getString(5);
                int unread = c.getInt(6);
                String labels = c.getString(7);
                String priority = c.getString(8);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setBackground(roundedBackground(Color.rgb(5, 24, 43),
                        Color.rgb(0, 160, 220), 12, 1));
                TextView title = text((kind.equals("group") ? "👥 " : "💬 ") + name +
                        (unread > 0 ? "  •  " + unread + " nova(s)" : ""), 16, Color.WHITE, true);
                TextView preview = text(last, 13, Color.rgb(185, 205, 220), false);
                preview.setMaxLines(2);
                TextView meta = text(ConversationStore.priorityLabel(priority).toUpperCase(Locale.ROOT) +
                        "  •  " + ConversationStore.statusLabel(status) + "  •  " + date(time),
                        11, "URGENT".equals(priority) ? Color.rgb(255, 90, 90) :
                        ("HIGH".equals(priority) ? Color.rgb(255, 190, 70) : Color.rgb(0, 205, 235)), false);
                card.addView(title);
                card.addView(preview);
                card.addView(meta);
                if (labels != null && !labels.trim().isEmpty()) {
                    card.addView(text("Etiquetas: " + labels, 11,
                            Color.rgb(255, 205, 90), true));
                }
                card.setClickable(true);
                card.setFocusable(true);
                card.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(this,
                            ConversationInboxActivity.class);
                    intent.putExtra(EXTRA_ID, id);
                    startActivity(intent);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, dp(9));
                list.addView(card, lp);
            }
        } finally {
            c.close();
        }
    }

    private void buildDetails() {
        store.markRead(selectedId);
        LinearLayout root = column();
        detailsRoot = root;

        Button status = new Button(this);
        status.setText("ALTERAR STATUS DO ATENDIMENTO");
        status.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Status do atendimento")
                .setItems(new String[]{"Em andamento", "Aguardando", "Resolvido", "Pausado"},
                        (d, which) -> {
                            String[] values = {"OPEN", "WAITING", "RESOLVED", "PAUSED"};
                            store.updateStatus(selectedId, values[which]);
                            renderMessages();
                        }).show());
        root.addView(status);

        Button priority = new Button(this);
        priority.setText("DEFINIR PRIORIDADE");
        priority.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Prioridade do atendimento")
                .setItems(new String[]{"Normal", "Alta", "Urgente"}, (d, which) -> {
                    String[] values = {"NORMAL", "HIGH", "URGENT"};
                    store.updatePriority(selectedId, values[which]);
                    refreshConversationInfo(detailsRoot);
                }).show());
        root.addView(priority);

        TextView currentPriority = text("", 12, Color.rgb(255, 190, 70), true);
        currentPriority.setTag("current_priority");
        root.addView(currentPriority);

        Button labels = new Button(this);
        labels.setText("EDITAR ETIQUETAS");
        labels.setOnClickListener(v -> editField("Etiquetas",
                "Separe as etiquetas por vírgula", store.labels(selectedId), false));
        root.addView(labels);

        Button notes = new Button(this);
        notes.setText("OBSERVAÇÕES INTERNAS");
        notes.setOnClickListener(v -> editField("Observações",
                "Informações internas deste atendimento", store.notes(selectedId), true));
        root.addView(notes);

        TextView currentLabels = text("", 12, Color.rgb(255, 205, 90), true);
        currentLabels.setTag("current_labels");
        root.addView(currentLabels);
        TextView currentNotes = text("", 12, Color.rgb(190, 210, 225), false);
        currentNotes.setTag("current_notes");
        root.addView(currentNotes);

        ScrollView scroll = new ScrollView(this);
        list = column();
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "CONVERSA",
                "Histórico local deste atendimento", root));
        refreshConversationInfo(root);
        renderMessages();
    }

    private void editField(String title, String hint, String current, boolean multiline) {
        EditText input = new EditText(this);
        input.setText(current == null ? "" : current);
        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setMinLines(multiline ? 4 : 2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(multiline
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    if (multiline) store.updateNotes(selectedId, input.getText().toString());
                    else store.updateLabels(selectedId, input.getText().toString());
                    if (detailsRoot != null) refreshConversationInfo(detailsRoot);
                }).show();
    }

    private void refreshConversationInfo(LinearLayout root) {
        TextView priority = root.findViewWithTag("current_priority");
        TextView labels = root.findViewWithTag("current_labels");
        TextView notes = root.findViewWithTag("current_notes");
        if (priority != null) {
            priority.setText("Prioridade: " + ConversationStore.priorityLabel(store.priority(selectedId)));
        }
        if (labels != null) {
            String value = store.labels(selectedId);
            labels.setText(value == null || value.trim().isEmpty()
                    ? "Etiquetas: nenhuma" : "Etiquetas: " + value);
        }
        if (notes != null) {
            String value = store.notes(selectedId);
            notes.setText(value == null || value.trim().isEmpty()
                    ? "Observações: nenhuma" : "Observações: " + value);
            notes.setMaxLines(3);
        }
    }

    private void renderMessages() {
        list.removeAllViews();
        Cursor c = store.messages(this, selectedId);
        try {
            if (c.getCount() == 0) {
                list.addView(NeonUi.statusCard(this, "chat", Color.CYAN,
                        "Sem mensagens", "Ainda não há mensagens nesta conversa", ""));
                return;
            }
            while (c.moveToNext()) {
                String body = c.getString(0);
                String direction = c.getString(1);
                long time = c.getLong(2);
                boolean outgoing = "OUT".equals(direction);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(12), dp(10), dp(12), dp(10));
                card.setBackground(roundedBackground(
                        outgoing ? Color.rgb(8, 48, 55) : Color.rgb(7, 28, 48),
                        outgoing ? Color.rgb(0, 190, 150) : Color.rgb(0, 145, 210), 10, 1));
                card.addView(text((outgoing ? "MASTER RESPONDE" : "CLIENTE") +
                        "  •  " + date(time), 10,
                        outgoing ? Color.rgb(0, 220, 170) : Color.CYAN, true));
                TextView message = text(body, 14, Color.WHITE, false);
                message.setTextIsSelectable(true);
                card.addView(message);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, dp(8));
                list.addView(card, lp);
            }
        } finally {
            c.close();
        }
    }

    @Override protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }

    private android.graphics.drawable.GradientDrawable roundedBackground(
            int fill, int stroke, int radius, int strokeWidth) {
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(strokeWidth), stroke);
        return d;
    }
    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(10), dp(8), dp(10), dp(12));
        return v;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, dp(3), 0, dp(3));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }
    private String date(long time) {
        return new SimpleDateFormat("dd/MM HH:mm", new Locale("pt", "BR"))
                .format(new Date(time));
    }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
