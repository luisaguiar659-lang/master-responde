package com.masterresponde.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DiagnosticsActivity extends Activity {
    private final int bg = Color.rgb(8, 11, 16);
    private final int panel = Color.rgb(20, 27, 36);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(160, 174, 190);
    private final int green = Color.rgb(0, 200, 150);
    private final int warn = Color.rgb(255, 170, 40);
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(18));
        root.setBackgroundColor(bg);
        root.addView(text("CENTRAL DE DIAGNÓSTICO", 21, white, true));
        root.addView(text("Verificações do aparelho e do motor de atendimento", 12, muted, false));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(12), 0, dp(12));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        boolean bot = getSharedPreferences("master_responde", MODE_PRIVATE)
                .getBoolean("bot_enabled", false);
        boolean notification = BackgroundRuntime.hasNotificationAccess(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean battery = pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());

        item("Motor de respostas", bot ? "ATIVADO" : "DESATIVADO", bot);
        item("Acesso às notificações", notification ? "PERMISSÃO LIBERADA" : "PERMISSÃO NECESSÁRIA", notification);
        item("Otimização de bateria", battery ? "SEM RESTRIÇÃO" : "PODE LIMITAR O SEGUNDO PLANO", battery);
        item("WhatsApp Business", installed("com.whatsapp.w4b") ? "INSTALADO" : "NÃO LOCALIZADO", installed("com.whatsapp.w4b"));
        item("Histórico de eventos", EventLog.read(this).length() + " registros locais", true);

        Button access = new Button(this);
        access.setText("ABRIR ACESSO ÀS NOTIFICAÇÕES");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access);

        Button batteryButton = new Button(this);
        batteryButton.setText("CONFIGURAR BATERIA");
        batteryButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });
        root.addView(batteryButton);
        setContentView(root);
    }

    private boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception ignored) { return false; }
    }

    private void item(String label, String value, boolean ok) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(13), dp(11), dp(13), dp(11));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(panel);
        shape.setCornerRadius(dp(12));
        box.setBackground(shape);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(5), 0, dp(5));
        box.setLayoutParams(lp);
        box.addView(text(label, 12, muted, false));
        box.addView(text(value, 15, ok ? green : warn, true));
        list.addView(box);
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
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
