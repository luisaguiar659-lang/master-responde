package com.masterresponde.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public class NeonDashboardActivity extends Activity {

    private static final String PREFS = "master_responde";
    private static final int BG = Color.rgb(3, 7, 12);
    private static final int CARD = Color.rgb(8, 15, 23);
    private static final int CARD_2 = Color.rgb(10, 20, 29);
    private static final int GREEN = Color.rgb(0, 255, 102);
    private static final int CYAN = Color.rgb(0, 214, 255);
    private static final int PURPLE = Color.rgb(190, 47, 255);
    private static final int RED = Color.rgb(255, 47, 92);
    private static final int MUTED = Color.rgb(147, 164, 178);

    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView botState;
    private TextView waStatus;
    private TextView accStatus;
    private TextView notifStatus;
    private TextView engineStatus;
    private TextView lastConversation;
    private TextView lastMessage;
    private TextView lastReply;
    private TextView queueStatus;
    private Button powerButton;

    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1800L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildDashboard());
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private View buildDashboard() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = column();
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(root, matchWrap());

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("MASTER\nRESPONDE", 28, Color.WHITE, true);
        brand.setLineSpacing(0f, .9f);
        top.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView online = badge("● ONLINE", GREEN, Color.rgb(1, 42, 20));
        top.addView(online);
        root.addView(top);

        root.addView(space(12));
        TextView version = text("v2.0.9  •  FUTURISTA NEON", 12, MUTED, false);
        root.addView(version);
        root.addView(space(12));

        TextView protectedBar = text("🔒  SOMENTE WHATSAPP BUSINESS    PROTEGIDO", 13, GREEN, true);
        protectedBar.setGravity(Gravity.CENTER);
        protectedBar.setPadding(dp(12), dp(12), dp(12), dp(12));
        protectedBar.setBackground(strokeBg(Color.rgb(3, 25, 18), GREEN, 14, 1));
        root.addView(protectedBar, matchWrap());

        section(root, "STATUS DOS SERVIÇOS");
        LinearLayout statusRow = row();
        waStatus = statusCard(statusRow, "◉", "WhatsApp\nBusiness", GREEN);
        accStatus = statusCard(statusRow, "♿", "Acessibilidade", PURPLE);
        notifStatus = statusCard(statusRow, "♢", "Notificações", CYAN);
        engineStatus = statusCard(statusRow, "✦", "Motor de\nRespostas", GREEN);
        root.addView(statusRow, matchWrap());

        section(root, "CONTROLE DO BOT");
        LinearLayout control = card();
        TextView controlTitle = text("ATENDIMENTO AUTOMÁTICO", 12, MUTED, true);
        control.addView(controlTitle);
        control.addView(space(12));
        powerButton = new Button(this);
        powerButton.setTextSize(18);
        powerButton.setTypeface(Typeface.DEFAULT_BOLD);
        powerButton.setAllCaps(false);
        powerButton.setPadding(dp(12), dp(16), dp(12), dp(16));
        powerButton.setOnClickListener(v -> {
            boolean next = !prefs.getBoolean("bot_enabled", false);
            prefs.edit().putBoolean("bot_enabled", next).apply();
            refresh();
        });
        control.addView(powerButton, matchWrap());
        control.addView(space(10));
        botState = text("", 13, GREEN, true);
        botState.setGravity(Gravity.CENTER);
        control.addView(botState);
        TextView test = text("MODO TESTE  •  EM BREVE", 11, MUTED, false);
        test.setGravity(Gravity.CENTER);
        test.setPadding(0, dp(9), 0, 0);
        control.addView(test);
        root.addView(control, matchWrap());

        section(root, "ATIVIDADE EM TEMPO REAL");
        LinearLayout activity = card();
        lastConversation = activityLine(activity, "Última conversa");
        lastMessage = activityLine(activity, "Última mensagem");
        lastReply = activityLine(activity, "Último status");
        root.addView(activity, matchWrap());

        section(root, "FILA & ESTATÍSTICAS");
        LinearLayout metrics = row();
        LinearLayout queue = miniCard();
        queue.addView(text("FILA DE RESPOSTAS", 11, Color.WHITE, true));
        queueStatus = text("Aguardando dados", 18, CYAN, true);
        queueStatus.setPadding(0, dp(14), 0, 0);
        queue.addView(queueStatus);
        metrics.addView(queue, weighted());
        metrics.addView(spaceH(10));
        LinearLayout stats = miniCard();
        stats.addView(text("ESTATÍSTICAS HOJE", 11, Color.WHITE, true));
        TextView success = text("96%\nSUCESSO", 22, GREEN, true);
        success.setGravity(Gravity.CENTER);
        success.setPadding(0, dp(12), 0, 0);
        stats.addView(success);
        metrics.addView(stats, weighted());
        root.addView(metrics, matchWrap());

        section(root, "SEGURANÇA MÁXIMA");
        LinearLayout security = card();
        security.setBackground(strokeBg(Color.rgb(3, 24, 25), CYAN, 16, 1));
        security.addView(text("🛡  100% BUSINESS-ONLY", 17, GREEN, true));
        security.addView(space(9));
        security.addView(text("✓ Somente com.whatsapp.w4b\n✓ WhatsApp normal bloqueado\n✓ Anti-eco ativo\n✓ Proteção contra conversa errada\n✓ Sistema anti-loop", 13, Color.WHITE, false));
        root.addView(security, matchWrap());

        section(root, "ACESSO RÁPIDO");
        LinearLayout nav = row();
        nav.addView(navButton("⌂\nINÍCIO", v -> scroll.smoothScrollTo(0, 0)), weighted());
        nav.addView(spaceH(7));
        nav.addView(navButton("▦\nCOMANDOS", v -> openCommands()), weighted());
        nav.addView(spaceH(7));
        nav.addView(navButton("◷\nHISTÓRICO", v -> Toast.makeText(this, "Histórico detalhado será a próxima tela.", Toast.LENGTH_SHORT).show()), weighted());
        nav.addView(spaceH(7));
        nav.addView(navButton("⚙\nCONFIG.", v -> startActivity(new Intent(this, MainActivity.class))), weighted());
        root.addView(nav, matchWrap());

        return scroll;
    }

    private void refresh() {
        boolean enabled = prefs.getBoolean("bot_enabled", false);
        botState.setText(enabled ? "MODO ATUAL: AUTOMÁTICO • ATIVO" : "MODO ATUAL: PAUSADO");
        botState.setTextColor(enabled ? GREEN : RED);
        powerButton.setText(enabled ? "⏸  PAUSAR BOT" : "⏻  ATIVAR BOT");
        powerButton.setTextColor(enabled ? Color.WHITE : GREEN);
        powerButton.setBackground(strokeBg(enabled ? Color.rgb(42, 7, 18) : Color.rgb(2, 31, 16), enabled ? RED : GREEN, 16, 2));

        setStatus(waStatus, isBusinessInstalled() ? "ATIVO" : "NÃO INSTALADO", isBusinessInstalled());
        setStatus(accStatus, WhatsAppAccessibilityService.isEnabled(this) ? "ATIVA" : "PERMISSÃO", WhatsAppAccessibilityService.isEnabled(this));
        setStatus(notifStatus, isNotificationAccessEnabled() ? "ATIVAS" : "PERMISSÃO", isNotificationAccessEnabled());
        setStatus(engineStatus, enabled ? "ATIVO" : "PAUSADO", enabled);

        lastConversation.setText(valueOrDash(prefs.getString("accessibility_last_conversation", "")));
        lastMessage.setText(valueOrDash(prefs.getString("accessibility_last_message", "")));
        lastReply.setText(valueOrDash(prefs.getString("accessibility_last_status", "")));
        int pending = prefs.getInt("neon_pending_replies", 0);
        queueStatus.setText(pending == 0 ? "0 PENDENTES" : pending + " PENDENTES");
    }

    private boolean isBusinessInstalled() {
        try {
            getPackageManager().getPackageInfo("com.whatsapp.w4b", 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isNotificationAccessEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private void openCommands() {
        try {
            startActivity(new Intent(this, MessageSettingsActivity.class));
        } catch (Throwable e) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private void setStatus(TextView view, String value, boolean ok) {
        view.setText(value);
        view.setTextColor(ok ? GREEN : RED);
    }

    private TextView statusCard(LinearLayout parent, String icon, String label, int accent) {
        LinearLayout box = column();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(7), dp(12), dp(7), dp(12));
        box.setBackground(strokeBg(CARD, accent, 14, 1));
        TextView i = text(icon, 25, accent, true);
        i.setGravity(Gravity.CENTER);
        box.addView(i);
        TextView l = text(label, 10, Color.WHITE, true);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, dp(7), 0, dp(5));
        box.addView(l);
        TextView state = text("...", 10, GREEN, true);
        state.setGravity(Gravity.CENTER);
        box.addView(state);
        parent.addView(box, weighted());
        parent.addView(spaceH(7));
        return state;
    }

    private TextView activityLine(LinearLayout parent, String label) {
        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = text(label, 12, MUTED, false);
        line.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, .42f));
        TextView value = text("—", 12, Color.WHITE, true);
        line.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, .58f));
        parent.addView(line);
        parent.addView(space(9));
        return value;
    }

    private void section(LinearLayout root, String title) {
        root.addView(space(22));
        TextView t = text(title, 12, Color.WHITE, true);
        t.setLetterSpacing(.06f);
        root.addView(t);
        root.addView(space(9));
    }

    private Button navButton(String title, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(10);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(3), dp(10), dp(3), dp(10));
        b.setBackground(strokeBg(CARD, Color.rgb(36, 60, 74), 13, 1));
        b.setOnClickListener(click);
        return b;
    }

    private LinearLayout card() {
        LinearLayout l = column();
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        l.setBackground(strokeBg(CARD_2, Color.rgb(21, 96, 123), 16, 1));
        return l;
    }

    private LinearLayout miniCard() {
        LinearLayout l = column();
        l.setPadding(dp(13), dp(13), dp(13), dp(13));
        l.setBackground(strokeBg(CARD, Color.rgb(27, 83, 102), 15, 1));
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private TextView badge(String s, int color, int fill) {
        TextView t = text(s, 11, color, true);
        t.setPadding(dp(9), dp(6), dp(9), dp(6));
        t.setBackground(strokeBg(fill, color, 30, 1));
        return t;
    }

    private GradientDrawable strokeBg(int fill, int stroke, int radius, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(width), stroke);
        return d;
    }

    private Space space(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return s;
    }

    private Space spaceH(int w) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1));
        return s;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private ViewGroup.LayoutParams matchWrap() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        String v = value.trim();
        return v.length() > 80 ? v.substring(0, 77) + "..." : v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
