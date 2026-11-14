package com.masterresponde.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.masterresponde.app.engines.AutomationCloudEngine;
import com.masterresponde.app.engines.MasterIBOEngine;

public class MainActivity extends Activity {

    private LinearLayout content;
    private SharedPreferences prefs;

    private TextView botStatus;
    private TextView dashboardBotBadge;
    private TextView waStatus;
    private TextView masterflixStatus;
    private TextView stvStatus;
    private TextView engineStatus;

    private Switch botSwitch;
    private String appliedTheme = "";

    private FrameLayout menuOverlay;
    private LinearLayout drawerPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    100
            );
        }

        buildScreen();

        // Inicia motores integrados automaticamente
        if (prefs.getBoolean("auto_start_engines", true)) {
            content.postDelayed(() -> {
                AutomationCloudEngine.start(this);
                MasterIBOEngine.start(this);
            }, 1200L);
        }

        // Inicia o MasterFlix automaticamente ao abrir o aplicativo
        if (prefs.getBoolean("auto_start_masterflix", true)) {
            content.postDelayed(() -> {
                try {
                    Intent intent = new Intent(this, MasterflixActivity.class);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }, 800L);
        }

        if (prefs.getBoolean(
                "auto_reconnect_engine",
                true
        )) {
            BackgroundRuntime.requestListenerRebind(
                    this
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentTheme =
                prefs.getString(
                        "app_theme",
                        "dark"
                );

        if (!currentTheme.equals(
                appliedTheme
        )
                && content != null) {
            buildScreen();
        }

        if (prefs.getBoolean(
                "auto_reconnect_engine",
                true
        )) {
            BackgroundRuntime.requestListenerRebind(
                    this
            );
        }

        refreshStatus();

        if (content != null) {
            content.postDelayed(
                    this::refreshStatus,
                    1200L
            );

            boolean returnToMenu =
                    prefs.getBoolean(
                            "return_to_menu_after_child",
                            false
                    );

            if (returnToMenu) {
                prefs.edit()
                        .putBoolean(
                                "return_to_menu_after_child",
                                false
                        )
                        .apply();

                if (prefs.getBoolean(
                        "return_to_menu_enabled",
                        true
                )) {
                    content.postDelayed(
                            this::openMenu,
                            180L
                    );
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (menuOverlay != null
                && menuOverlay.getVisibility() == View.VISIBLE) {
            closeMenu();
            return;
        }

        super.onBackPressed();
    }

    private void buildScreen() {
        appliedTheme =
                prefs.getString(
                        "app_theme",
                        "dark"
                );

        getWindow().setStatusBarColor(
                backgroundColor()
        );

        getWindow().setNavigationBarColor(
                backgroundColor()
        );

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(
                    isLightTheme()
                            ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            : 0
            );
        }

        FrameLayout shell =
                new FrameLayout(
                        this
                );

        if (isLightTheme()) {
            shell.setBackgroundColor(backgroundColor());
        } else {
            shell.setBackgroundResource(R.drawable.app_background);
        }

        LinearLayout root =
                new LinearLayout(
                        this
                );

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        if (isLightTheme()) {
            root.setBackgroundColor(backgroundColor());
        } else {
            root.setBackgroundResource(R.drawable.app_background);
        }

        LinearLayout header =
                new LinearLayout(
                        this
                );

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(14),
                dp(8),
                dp(10),
                dp(8)
        );
        header.setBackground(neonPanel(Color.rgb(2, 15, 31), Color.rgb(0, 150, 210), Color.rgb(255, 135, 20), 0));

        ImageView logo =
                new ImageView(
                        this
                );

        logo.setImageResource(
                getResources().getIdentifier(
                        "logo_master_responde",
                        "drawable",
                        getPackageName()
                )
        );

        header.addView(
                logo,
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(56)
                )
        );

        LinearLayout titles =
                new LinearLayout(
                        this
                );

        titles.setOrientation(
                LinearLayout.VERTICAL
        );

        titles.setPadding(
                dp(10),
                0,
                0,
                0
        );

        titles.addView(text("MASTER", 20, Color.WHITE, true));
        titles.addView(text("RESPONDE", 20, Color.rgb(255, 151, 35), true));

        titles.addView(
                text(
                        "Central inteligente de atendimento",
                        12,
                        secondaryTextColor(),
                        false
                )
        );

        header.addView(
                titles,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1f
                )
        );

        TextView menuButton =
                text(
                        "☰",
                        28,
                        primaryTextColor(),
                        true
                );

        menuButton.setGravity(
                Gravity.CENTER
        );

        menuButton.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );

        menuButton.setContentDescription(
                "Abrir menu"
        );

        menuButton.setOnClickListener(
                v -> openMenu()
        );

        header.addView(
                menuButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        root.addView(
                header
        );

        View line =
                new View(
                        this
                );

        line.setBackgroundResource(
                getResources().getIdentifier(
                        "line",
                        "drawable",
                        getPackageName()
                )
        );

        root.addView(
                line,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(3)
                )
        );

        ScrollView scroll =
                new ScrollView(
                        this
                );

        content =
                new LinearLayout(
                        this
                );

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dp(16),
                dp(18),
                dp(16),
                dp(40)
        );

        scroll.addView(
                content
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        buildBottomNavigation(root);

        shell.addView(
                root,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        buildDrawer(
                shell
        );

        setContentView(
                shell
        );

        buildDashboard();
    }

    private void buildDrawer(FrameLayout shell) {
        menuOverlay = new FrameLayout(this);
        menuOverlay.setBackgroundColor(Color.argb(210, 0, 2, 10));
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setOnClickListener(v -> closeMenu());

        ScrollView scroll = new ScrollView(this);
        drawerPanel = new LinearLayout(this);
        drawerPanel.setOrientation(LinearLayout.VERTICAL);
        drawerPanel.setPadding(dp(12), dp(12), dp(12), dp(24));
        drawerPanel.setBackground(neonPanel(Color.rgb(3, 13, 29), Color.rgb(0, 190, 245), Color.rgb(255, 143, 20), 20));
        drawerPanel.setClickable(true);
        drawerPanel.setOnClickListener(v -> { });

        FrameLayout banner = new FrameLayout(this);
        GradientDrawable bannerBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(2, 30, 54), Color.rgb(5, 13, 29), Color.rgb(49, 22, 7)});
        bannerBg.setCornerRadius(dp(18));
        bannerBg.setStroke(dp(1), Color.rgb(0, 185, 245));
        banner.setBackground(bannerBg);
        ImageView mascot = new ImageView(this);
        int logoId = getResources().getIdentifier("logo_master_responde", "drawable", getPackageName());
        if (logoId != 0) mascot.setImageResource(logoId);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        banner.addView(mascot, new FrameLayout.LayoutParams(dp(88), dp(92), Gravity.START | Gravity.CENTER_VERTICAL));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(88), dp(10), dp(34), dp(8));
        brand.addView(text("MASTER", 21, Color.WHITE, true));
        brand.addView(text("RESPONDE", 21, Color.rgb(255, 151, 35), true));
        brand.addView(text("Menu Principal", 12, Color.rgb(145, 190, 215), true));
        banner.addView(brand, new FrameLayout.LayoutParams(-1, -2));
        TextView close = text("×", 30, Color.WHITE, true);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> closeMenu());
        banner.addView(close, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.END | Gravity.TOP));
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(-1, dp(112));
        bannerLp.bottomMargin = dp(13);
        drawerPanel.addView(banner, bannerLp);

        drawerSection("ATALHOS RÁPIDOS");
        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        drawerShortcut(shortcuts, "STV+", "▶", Color.rgb(190, 67, 255), () ->
                launchFromMenu(new Intent(this, StvPlusActivity.class)));
        drawerShortcut(shortcuts, "GERAR TESTE", "▶", Color.rgb(255, 153, 24), () -> {
            Intent intent = new Intent(this, MasterflixActivity.class);
            intent.putExtra("auto_generate_test", true);
            launchFromMenu(intent);
        });
        drawerShortcut(shortcuts, "NOVA REVENDA", "+", Color.rgb(0, 220, 125), () ->
                launchFromMenu(new Intent(this, ResellersActivity.class)));
        LinearLayout.LayoutParams shortcutLp = new LinearLayout.LayoutParams(-1, dp(104));
        shortcutLp.bottomMargin = dp(13);
        drawerPanel.addView(shortcuts, shortcutLp);

        drawerSection("MÓDULOS");
        drawerLink("Regras", "▤", () -> launchFromMenu(new Intent(this, RulesActivity.class)));
        drawerLink("Revendas", "♙", () -> launchFromMenu(new Intent(this, ResellersActivity.class)));
        drawerLink("Mensagens e Tempos", "▣", () -> launchFromMenu(new Intent(this, MessageSettingsActivity.class)));

        drawerSection("CONTROLE E DIAGNÓSTICO");
        drawerLink("Monitoramento", "▱", () -> launchFromMenu(new Intent(this, MonitoringActivity.class)));
        drawerLink("Histórico", "◷", () -> launchFromMenu(new Intent(this, EventHistoryActivity.class)));
        drawerLink("Simulador de Gatilhos", "ϟ", () -> launchFromMenu(new Intent(this, TriggerSimulatorActivity.class)));
        drawerLink("Backup / Restauração", "▤", () -> launchFromMenu(new Intent(this, BackupActivity.class)));
        drawerLink("Central de Diagnóstico", "∿", () -> launchFromMenu(new Intent(this, DiagnosticsActivity.class)));
        drawerLink("Configurações", "⚙", () -> launchFromMenu(new Intent(this, AppSettingsActivity.class)));

        scroll.addView(drawerPanel);
        int panelWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(360));
        menuOverlay.addView(scroll, new FrameLayout.LayoutParams(panelWidth, -1, Gravity.START));
        shell.addView(menuOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void drawerShortcut(LinearLayout parent, String label, String symbol, int accent, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(7), dp(3), dp(5));
        tile.setBackground(neonPanel(Color.rgb(9, 22, 46), accent, accent, 14));
        TextView icon = text(symbol, 22, accent, true);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon);
        TextView title = text(label, 8, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(5), 0, 0);
        tile.addView(title);
        tile.setClickable(true);
        tile.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(tile, lp);
    }

    private void drawerLink(String label, String symbol, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(6), dp(11), dp(6));
        row.setBackground(neonPanel(Color.rgb(7, 27, 47), Color.rgb(0, 133, 185), Color.rgb(0, 133, 185), 13));
        TextView icon = text(symbol, 20, Color.rgb(0, 211, 245), true);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), -1));
        TextView title = text(label, 12, Color.WHITE, true);
        title.setPadding(dp(9), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text("›", 24, Color.WHITE, false));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> { closeMenu(); if (action != null) action.run(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(47));
        lp.setMargins(dp(2), dp(3), dp(2), dp(4));
        drawerPanel.addView(row, lp);
    }

    private void openMenu() {
        if (menuOverlay == null) {
            return;
        }

        menuOverlay.setVisibility(
                View.VISIBLE
        );

        menuOverlay.setAlpha(
                0f
        );

        menuOverlay.animate()
                .alpha(
                        1f
                )
                .setDuration(
                        160L
                )
                .start();
    }

    private void closeMenu() {
        if (menuOverlay == null
                || menuOverlay.getVisibility() != View.VISIBLE) {
            return;
        }

        menuOverlay.animate()
                .alpha(
                        0f
                )
                .setDuration(
                        130L
                )
                .withEndAction(
                        () -> {
                            menuOverlay.setVisibility(
                                    View.GONE
                            );

                            menuOverlay.setAlpha(
                                    1f
                            );
                        }
                )
                .start();
    }


    private void buildDashboard() {
        content.removeAllViews();
        final int cyan = Color.rgb(0, 220, 255);
        final int blue = Color.rgb(0, 132, 255);
        final int green = Color.rgb(0, 230, 125);
        final int orange = Color.rgb(255, 145, 20);
        final int purple = Color.rgb(181, 67, 255);
        final int red = Color.rgb(255, 65, 85);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(10), dp(10), dp(10), dp(10));
        hero.setBackground(neonPanel(Color.rgb(2, 13, 30), orange, cyan, 18));
        ImageView mascot = new ImageView(this);
        int logoId = getResources().getIdentifier("logo_master_responde", "drawable", getPackageName());
        if (logoId != 0) mascot.setImageResource(logoId);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(mascot, new LinearLayout.LayoutParams(dp(76), dp(92)));
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.setPadding(dp(8), 0, dp(4), 0);
        TextView eyebrow = text("CENTRAL DE AUTOMAÇÃO", 8, cyan, true);
        eyebrow.setLetterSpacing(0.1f);
        heroText.addView(eyebrow);
        TextView heroTitle = text("MASTER RESPONDE", 15, Color.WHITE, true);
        heroTitle.setPadding(0, dp(4), 0, dp(3));
        heroText.addView(heroTitle);
        heroText.addView(text("Atendimentos automáticos, testes e gestão de revendas em um só lugar.", 9, Color.rgb(185, 204, 224), false));
        hero.addView(heroText, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout toggleColumn = new LinearLayout(this);
        toggleColumn.setOrientation(LinearLayout.VERTICAL);
        toggleColumn.setGravity(Gravity.CENTER);
        dashboardBotBadge = text(prefs.getBoolean("bot_enabled", false) ? "● BOT ATIVO" : "● BOT PAUSADO", 8, prefs.getBoolean("bot_enabled", false) ? green : orange, true);
        dashboardBotBadge.setGravity(Gravity.CENTER);
        dashboardBotBadge.setPadding(dp(7), dp(6), dp(7), dp(6));
        dashboardBotBadge.setBackground(roundedBackground(Color.rgb(4, 34, 39), green, 18));
        toggleColumn.addView(dashboardBotBadge);
        botSwitch = new Switch(this);
        botSwitch.setChecked(prefs.getBoolean("bot_enabled", false));
        botSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("bot_enabled", checked).apply();
            if (checked) BackgroundRuntime.requestListenerRebind(this);
            refreshStatus();
            Toast.makeText(this, checked ? "Bot ativado." : "Bot desativado.", Toast.LENGTH_SHORT).show();
        });
        toggleColumn.addView(botSwitch);
        hero.addView(toggleColumn);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.bottomMargin = dp(17);
        content.addView(hero, heroLp);

        section("STATUS DO SISTEMA");
        GridLayout statusGrid = new GridLayout(this);
        statusGrid.setColumnCount(2);
        botStatus = addReferenceStatus(statusGrid, "BOT", "● Verificando", blue, "◎", null);
        waStatus = addReferenceStatus(statusGrid, "WHATSAPP BUSINESS", "● Verificando", green, "◉", this::openNotificationAccess);
        masterflixStatus = addReferenceStatus(statusGrid, "MASTERFLIX", "● Verificando", orange, "▶", () -> startActivity(new Intent(this, MasterflixActivity.class)));
        stvStatus = addReferenceStatus(statusGrid, "STV+", "● Verificando", purple, "STV", () -> startActivity(new Intent(this, StvPlusActivity.class)));
        engineStatus = addReferenceStatus(statusGrid, "2º PLANO", "● Verificando", Color.rgb(255, 195, 30), "▱", this::openNotificationAccess);
        content.addView(statusGrid);

        gap(17);
        section("ATIVIDADE DO ATENDIMENTO");
        org.json.JSONArray events = EventLog.read(this);
        int received = 0, sent = 0, errors = 0;
        long now = System.currentTimeMillis();
        int[] daily = new int[7];
        for (int i = 0; i < events.length(); i++) {
            org.json.JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String type = event.optString("type");
            if ("MENSAGEM RECEBIDA".equals(type)) received++;
            if ("RESPOSTA ENVIADA".equals(type)) sent++;
            if ("FALHA".equals(type)) errors++;
            long time = event.optLong("time", 0L);
            long age = now - time;
            if (time > 0 && age >= 0 && age < 7L * 86400000L) {
                int day = (int)(age / 86400000L);
                daily[6 - Math.min(6, day)]++;
            }
        }
        GridLayout activityGrid = new GridLayout(this);
        activityGrid.setColumnCount(3);
        addReferenceMetric(activityGrid, "RECEBIDAS", String.valueOf(received), cyan);
        addReferenceMetric(activityGrid, "RESPONDIDAS", String.valueOf(sent), green);
        addReferenceMetric(activityGrid, "FALHAS", String.valueOf(errors), errors > 0 ? red : Color.rgb(160, 175, 190));
        content.addView(activityGrid);

        gap(17);
        section("DESEMPENHO");
        LinearLayout performance = card();
        performance.setOrientation(LinearLayout.HORIZONTAL);
        performance.setGravity(Gravity.CENTER_VERTICAL);
        performance.setPadding(dp(15), dp(12), dp(15), dp(12));
        performance.addView(text("TAXA DE RESPOSTA", 10, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1f));
        int responseRate = received <= 0 ? 0 : Math.min(100, Math.round(sent * 100f / received));
        performance.addView(text(responseRate + "%", 25, green, true));
        content.addView(performance);

        gap(17);
        section("ACESSO RÁPIDO");
        GridLayout quickGrid = new GridLayout(this);
        quickGrid.setColumnCount(2);
        addReferenceQuick(quickGrid, "Gerar Teste MasterFlix", "▶", blue, () -> startActivity(new Intent(this, MasterflixActivity.class)));
        addReferenceQuick(quickGrid, "Gerar Teste STV+", "▶", purple, () -> startActivity(new Intent(this, StvPlusActivity.class)));
        addReferenceQuick(quickGrid, "Cadastrar Revenda", "+", orange, () -> startActivity(new Intent(this, ResellersActivity.class)));
        addReferenceQuick(quickGrid, "Regras do Bot", "≡", cyan, () -> startActivity(new Intent(this, RulesActivity.class)));
        addReferenceQuick(quickGrid, "Configurações", "⚙", blue, () -> startActivity(new Intent(this, AppSettingsActivity.class)));
        content.addView(quickGrid);

        gap(17);
        section("RESUMO GERAL");
        LinearLayout summary = card();
        summary.setPadding(dp(12), dp(11), dp(12), dp(12));
        addSummaryRow(summary, "Total de atendimentos recebidos", String.valueOf(received), cyan);
        addSummaryRow(summary, "Respostas enviadas", String.valueOf(sent), green);
        addSummaryRow(summary, "Falhas registradas", String.valueOf(errors), errors > 0 ? red : Color.rgb(170, 185, 200));
        addSummaryRow(summary, "Eventos armazenados", String.valueOf(events.length()), Color.WHITE);
        content.addView(summary);

        gap(12);
        LinearLayout chart = card();
        chart.setPadding(dp(12), dp(11), dp(12), dp(10));
        chart.addView(text("USO DIÁRIO — ÚLTIMOS 7 DIAS", 9, cyan, true));
        LinearLayout bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        bars.setGravity(Gravity.BOTTOM);
        int max = 1;
        for (int value : daily) max = Math.max(max, value);
        String[] days = {"6d", "5d", "4d", "3d", "2d", "Ontem", "Hoje"};
        for (int i = 0; i < daily.length; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.addView(text(String.valueOf(daily[i]), 7, secondaryTextColor(), false));
            View bar = new View(this);
            bar.setBackground(roundedBackground(blue, cyan, 5));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(17), dp(daily[i] == 0 ? 3 : Math.max(7, (int)(52f * daily[i] / max))));
            barLp.topMargin = dp(3);
            col.addView(bar, barLp);
            TextView day = text(days[i], 7, secondaryTextColor(), false);
            day.setPadding(0, dp(4), 0, 0);
            col.addView(day);
            bars.addView(col, new LinearLayout.LayoutParams(0, dp(82), 1f));
        }
        chart.addView(bars);
        content.addView(chart);
        refreshStatus();
    }

    private TextView addReferenceStatus(GridLayout parent, String label, String value, int accent, String symbol, Runnable click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(10), dp(8), dp(10));
        box.setBackground(neonPanel(Color.rgb(4, 18, 37), accent, accent, 15));
        TextView icon = text(symbol, 21, accent, true);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);
        TextView name = text(label, 9, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(5), 0, dp(7));
        box.addView(name);
        TextView status = text(value, 8, accent, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(5), dp(5), dp(5), dp(5));
        status.setBackground(roundedBackground(Color.rgb(4, 37, 40), Color.rgb(20, 115, 92), 16));
        box.addView(status);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(105);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        parent.addView(box, lp);
        if (click != null) {
            box.setClickable(true);
            box.setFocusable(true);
            box.setOnClickListener(v -> click.run());
        }
        return status;
    }

    private void addReferenceMetric(GridLayout parent, String label, String value, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(9), dp(10), dp(5), dp(10));
        box.setBackground(neonPanel(Color.rgb(4, 20, 40), accent, accent, 14));
        box.addView(text(label, 8, Color.rgb(180, 200, 218), true));
        box.addView(text(value, 22, accent, true));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(78);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        parent.addView(box, lp);
    }

    private void addReferenceQuick(GridLayout parent, String label, String symbol, int accent, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(10), dp(8), dp(10));
        tile.setBackground(neonPanel(Color.rgb(5, 19, 39), accent, accent, 15));
        TextView icon = text(symbol, 24, accent, true);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon);
        TextView caption = text(label, 10, Color.WHITE, true);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(2);
        caption.setPadding(0, dp(6), 0, 0);
        tile.addView(caption);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> action.run());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(94);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        parent.addView(tile, lp);
    }

    private void addSummaryRow(LinearLayout parent, String label, String value, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(9), dp(7), dp(9), dp(7));
        row.setBackground(roundedBackground(Color.rgb(7, 25, 43), Color.rgb(26, 76, 105), 10));
        row.addView(text(label, 10, Color.WHITE, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, 12, accent, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(5);
        parent.addView(row, lp);
    }

    private void buildBottomNavigation(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(3), dp(5), dp(3), dp(5));
        nav.setBackground(neonPanel(Color.rgb(3, 15, 30), Color.rgb(0, 165, 235), Color.rgb(255, 139, 20), 17));
        nav.setElevation(dp(5));
        addNavItem(nav, "⌂", "Dashboard", null);
        addNavItem(nav, "▣", "Atend.", () -> startActivity(new Intent(this, EventHistoryActivity.class)));
        addNavItem(nav, "▱", "Módulos", this::openMenu);
        addNavItem(nav, "♙", "Revendas", () -> startActivity(new Intent(this, ResellersActivity.class)));
        addNavItem(nav, "▥", "Relatórios", () -> startActivity(new Intent(this, MonitoringActivity.class)));
        addNavItem(nav, "⚙", "Config.", () -> startActivity(new Intent(this, AppSettingsActivity.class)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(60));
        lp.setMargins(dp(6), dp(4), dp(6), dp(7));
        root.addView(nav, lp);
    }

    private void addNavItem(LinearLayout parent, String icon, String label, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(1), dp(2), dp(1), dp(2));
        TextView symbol = text(icon, 16, Color.rgb(0, 211, 246), true);
        symbol.setGravity(Gravity.CENTER);
        item.addView(symbol);
        TextView caption = text(label, 7, Color.rgb(205, 218, 231), false);
        caption.setGravity(Gravity.CENTER);
        item.addView(caption);
        item.setClickable(true);
        item.setOnClickListener(v -> { if (action != null) action.run(); });
        parent.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    private void drawerSection(
            String value
    ) {
        TextView section =
                text(
                        value,
                        11,
                        Color.rgb(0, 211, 245),
                        true
                );

        section.setLetterSpacing(
                0.08f
        );

        section.setPadding(
                dp(5),
                dp(12),
                0,
                dp(8)
        );

        drawerPanel.addView(
                section
        );
    }

    private void drawerItem(
            String label,
            boolean orange,
            Runnable action
    ) {
        Button button =
                fullButton(
                        label,
                        false
                );

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54)
                );

        lp.setMargins(
                0,
                0,
                0,
                dp(8)
        );

        button.setLayoutParams(
                lp
        );

        button.setOnClickListener(
                v -> {
                    closeMenu();

                    if (action != null) {
                        action.run();
                    }
                }
        );

        drawerPanel.addView(
                button
        );
    }

    private void refreshStatus() {
        if (botStatus != null) {
            boolean enabled =
                    prefs.getBoolean(
                            "bot_enabled",
                            false
                    );

            botStatus.setText(
                    enabled
                            ? "● Ativo"
                            : "● Desativado"
            );

            botStatus.setTextColor(
                    enabled
                            ? Color.rgb(
                            0,
                            184,
                            255
                    )
                            : Color.rgb(
                            145,
                            155,
                            165
                    )
            );

            if (dashboardBotBadge != null) {
                dashboardBotBadge.setText(enabled ? "● BOT ATIVO" : "● BOT PAUSADO");
                dashboardBotBadge.setTextColor(enabled
                        ? Color.rgb(0, 245, 154)
                        : Color.rgb(255, 174, 66));
            }
        }

        if (waStatus != null) {
            boolean installed =
                    isPackageInstalled(
                            "com.whatsapp.w4b"
                    );

            boolean access =
                    hasNotificationAccess();

            boolean connected =
                    prefs.getBoolean(
                            BackgroundRuntime.KEY_LISTENER_CONNECTED,
                            false
                    );

            if (!installed) {
                waStatus.setText(
                        "● Não instalado"
                );

                waStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );

            } else if (!access) {
                waStatus.setText(
                        "● Permissão necessária"
                );

                waStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );

            } else if (connected) {
                waStatus.setText(
                        "● Motor conectado"
                );

                waStatus.setTextColor(
                        Color.rgb(
                                0,
                                184,
                                255
                        )
                );

            } else {
                waStatus.setText(
                        "● Reconectando"
                );

                waStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );

                BackgroundRuntime.requestListenerRebind(
                        this
                );
            }
        }

        if (stvStatus != null) {
            SharedPreferences stvPrefs =
                    getSharedPreferences("master_responde_stv", MODE_PRIVATE);
            boolean credentials =
                    stvPrefs.getBoolean(
                            "stvplus_credentials_saved",
                            stvPrefs.getBoolean("credentials_saved", false)
                    );
            boolean activeSession =
                    stvPrefs.getBoolean("stvplus_session_active", false);

            if (StvPlusBackgroundAutomation.isRunning()) {
                stvStatus.setText("● Gerando teste");
                stvStatus.setTextColor(Color.rgb(0, 184, 255));
            } else if (activeSession) {
                stvStatus.setText("● Sessão ativa");
                stvStatus.setTextColor(Color.rgb(0, 184, 255));
            } else if (credentials) {
                stvStatus.setText("● Login salvo");
                stvStatus.setTextColor(Color.rgb(255, 145, 0));
            } else {
                stvStatus.setText("● Login necessário");
                stvStatus.setTextColor(Color.rgb(255, 145, 0));
            }
        }

        if (engineStatus != null) {
            boolean access =
                    hasNotificationAccess();

            boolean connected =
                    prefs.getBoolean(
                            BackgroundRuntime.KEY_LISTENER_CONNECTED,
                            false
                    );

            boolean enabled =
                    prefs.getBoolean(
                            "bot_enabled",
                            false
                    );

            if (!enabled) {
                engineStatus.setText(
                        "● Bot desligado"
                );

                engineStatus.setTextColor(
                        Color.rgb(
                                145,
                                155,
                                165
                        )
                );

            } else if (!access) {
                engineStatus.setText(
                        "● Sem permissão"
                );

                engineStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );

            } else if (connected) {
                engineStatus.setText(
                        "● 2º plano ativo"
                );

                engineStatus.setTextColor(
                        Color.rgb(
                                0,
                                184,
                                255
                        )
                );

            } else {
                engineStatus.setText(
                        "● Reconectando"
                );

                engineStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );
            }
        }

        if (masterflixStatus != null) {
            boolean active =
                    prefs.getBoolean(
                            "masterflix_session_active",
                            false
                    );

            boolean credentials =
                    prefs.getBoolean(
                            "masterflix_credentials_saved",
                            false
                    );

            if (active) {
                masterflixStatus.setText(
                        "● Sessão ativa"
                );

                masterflixStatus.setTextColor(
                        Color.rgb(
                                0,
                                184,
                                255
                        )
                );

            } else if (credentials) {
                masterflixStatus.setText(
                        "● Login salvo"
                );

                masterflixStatus.setTextColor(
                        Color.rgb(
                                255,
                                150,
                                0
                        )
                );

            } else {
                masterflixStatus.setText(
                        "● Login necessário"
                );

                masterflixStatus.setTextColor(
                        Color.rgb(
                                255,
                                145,
                                0
                        )
                );
            }
        }
    }

    private boolean hasNotificationAccess() {
        return BackgroundRuntime.hasNotificationAccess(
                this
        );
    }

    private boolean isPackageInstalled(
            String packageName
    ) {
        try {
            getPackageManager()
                    .getPackageInfo(
                            packageName,
                            0
                    );

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void openNotificationAccess() {
        try {
            Intent intent =
                    new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                    );

            startActivity(
                    intent
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Abra Configurações > Acesso às notificações.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(futuristicCardBackground(surfaceColor(), borderColor(), 18));
        if (!isLightTheme()) c.setElevation(dp(3));
        return c;
    }

    private TextView addStatus(
            GridLayout parent,
            String title,
            String value,
            int accent,
            Runnable click
    ) {
        LinearLayout c = card();
        c.setPadding(dp(13), dp(12), dp(10), dp(12));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(91);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        c.setLayoutParams(lp);

        TextView heading = text("◉  " + title, 9, secondaryTextColor(), true);
        heading.setLetterSpacing(0.035f);
        c.addView(heading);

        TextView v = text(value, 12, accent, true);
        v.setPadding(0, dp(12), 0, 0);
        c.addView(v);

        if (click != null) {
            c.setClickable(true);
            c.setFocusable(true);
            c.setOnClickListener(view -> click.run());
        }
        parent.addView(c);
        return v;
    }

    private Button fullButton(
            String label,
            boolean orange
    ) {
        Button b =
                new Button(
                        this
                );

        b.setText(
                label
        );

        b.setTextColor(
                orange
                        ? Color.WHITE
                        : primaryTextColor()
        );

        b.setTextSize(
                13
        );

        b.setTypeface(
                null,
                1
        );

        if (orange) {
            b.setBackgroundResource(
                    getResources().getIdentifier(
                            "button_orange",
                            "drawable",
                            getPackageName()
                    )
            );
        } else {
            b.setBackground(
                    roundedBackground(
                            buttonColor(),
                            borderColor(),
                            14
                    )
            );
        }

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );

        b.setLayoutParams(
                lp
        );

        return b;
    }

    private void section(String value) {
        TextView t = text(value, 11, isLightTheme() ? Color.rgb(0, 120, 110) : Color.rgb(0, 220, 205), true);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, 0, 0, dp(8));
        content.addView(t);
    }

    private GradientDrawable futuristicCardBackground(int fill, int stroke, int radiusDp) {
        if (isLightTheme()) {
            return roundedBackground(fill, stroke, radiusDp);
        }
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(10, 25, 31), fill}
        );
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView t =
                new TextView(
                        this
                );

        t.setText(
                value
        );

        t.setTextSize(
                size
        );

        t.setTextColor(
                color
        );

        if (bold) {
            t.setTypeface(
                    null,
                    1
            );
        }

        return t;
    }

    private void gap(
            int height
    ) {
        View v =
                new View(
                        this
                );

        content.addView(
                v,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private void launchFromMenu(
            Intent intent
    ) {
        if (intent == null) {
            return;
        }

        prefs.edit()
                .putBoolean(
                        "return_to_menu_after_child",
                        true
                )
                .apply();

        startActivity(
                intent
        );
    }

    private boolean isLightTheme() {
        return "light".equals(
                prefs.getString(
                        "app_theme",
                        "dark"
                )
        );
    }

    private int backgroundColor() {
        return isLightTheme()
                ? Color.rgb(244, 247, 250)
                : Color.rgb(5, 17, 11);
    }

    private int surfaceColor() {
        return isLightTheme()
                ? Color.WHITE
                : Color.rgb(10, 27, 17);
    }

    private int drawerColor() {
        return isLightTheme()
                ? Color.rgb(248, 250, 252)
                : Color.rgb(8, 22, 14);
    }

    private int buttonColor() {
        return isLightTheme()
                ? Color.rgb(235, 240, 245)
                : Color.rgb(14, 35, 23);
    }

    private int borderColor() {
        return isLightTheme()
                ? Color.rgb(205, 214, 223)
                : Color.rgb(32, 73, 48);
    }

    private int primaryTextColor() {
        return isLightTheme()
                ? Color.rgb(20, 27, 34)
                : Color.WHITE;
    }

    private int secondaryTextColor() {
        return isLightTheme()
                ? Color.rgb(90, 105, 118)
                : Color.rgb(150, 160, 170);
    }

    private GradientDrawable roundedBackground(
            int fill,
            int stroke,
            int radiusDp
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                fill
        );

        drawable.setCornerRadius(
                dp(radiusDp)
        );

        drawable.setStroke(
                dp(1),
                stroke
        );

        return drawable;
    }

    private int dp(
            int value
    ) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
