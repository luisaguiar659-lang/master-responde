package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConversationStore(this);
        selectedId = getIntent().getStringExtra(EXTRA_ID);
        if (selectedId == null) buildInbox(); else buildDetails();
    }

    private void buildInbox() {
        LinearLayout root = column();
        Button history = new Button(this);
        history.setText("HISTÓRICO TÉCNICO DE EVENTOS");
        history.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, EventHistoryActivity.class)));
        root.addView(history);
        ScrollView scroll = new ScrollView(this);
        list = column();
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "ATENDIMENTOS",
                "Conversas recebidas pelo WhatsApp Business", root));
        renderInbox();
    }

    private void renderInbox() {
        list.removeAllViews();
        Cursor c = store.conversations(this);
        try {
            if (c.getCount() == 0) {
                list.addView(NeonUi.statusCard(this, "chat", Color.rgb(0, 200, 255),
                        "Nenhum atendimento", "As novas conversas aparecerão aqui",
                        "O acesso às notificações do WhatsApp Business precisa estar ativo."));
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
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setBackground(NeonUi.roundedBackground(Color.rgb(5, 24, 43),
                        Color.rgb(0, 160, 220), 12, 1));
                TextView title = text((kind.equals("group") ? "👥 " : "💬 ") + name +
                        (unread > 0 ? "  •  " + unread + " nova(s)" : ""), 16, Color.WHITE, true);
                TextView preview = text(last, 13, Color.rgb(185, 205, 220), false);
                preview.setMaxLines(2);
                TextView meta = text(ConversationStore.statusLabel(status) + "  •  " +
                        date(time), 11, Color.rgb(0, 205, 235), false);
                card.addView(title);
                card.addView(preview);
                card.addView(meta);
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
        ScrollView scroll = new ScrollView(this);
        list = column();
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(NeonUi.wrap(this, "CONVERSA",
                "Histórico local deste atendimento", root));
        renderMessages();
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
                card.setBackground(NeonUi.roundedBackground(
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
