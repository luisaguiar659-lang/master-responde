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
                dp(16),
                dp(10),
                dp(12),
                dp(10)
        );

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

        titles.addView(
                text(
                        "MASTER RESPONDE",
                        21,
                        primaryTextColor(),
                        true
                )
        );

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

    private void buildDrawer(
            FrameLayout shell
    ) {
        menuOverlay =
                new FrameLayout(
                        this
                );

        menuOverlay.setBackgroundColor(
                Color.argb(
                        175,
                        0,
                        0,
                        0
                )
        );

        menuOverlay.setVisibility(
                View.GONE
        );

        menuOverlay.setOnClickListener(
                v -> closeMenu()
        );

        ScrollView drawerScroll =
                new ScrollView(
                        this
                );

        drawerPanel =
                new LinearLayout(
                        this
                );

        drawerPanel.setOrientation(
                LinearLayout.VERTICAL
        );

        drawerPanel.setPadding(
                dp(18),
                dp(24),
                dp(18),
                dp(30)
        );

        drawerPanel.setBackgroundColor(
                drawerColor()
        );

        drawerPanel.setClickable(
                true
        );

        drawerPanel.setOnClickListener(
                v -> {
                }
        );

        TextView close =
                text(
                        "‹  MENU",
                        18,
                        primaryTextColor(),
                        true
                );

        close.setPadding(
                0,
                0,
                0,
                dp(16)
        );

        close.setOnClickListener(
                v -> closeMenu()
        );

        drawerPanel.addView(
                close
        );

        drawerSection(
                "ATALHOS"
        );

        drawerItem(
                "STV+",
                true,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                StvPlusActivity.class
                        )
                )
        );

        drawerItem(
                "GERAR TESTE",
                true,
                () -> {
                    Intent intent =
                            new Intent(
                                    this,
                                    MasterflixActivity.class
                            );

                    intent.putExtra(
                            "auto_generate_test",
                            true
                    );

                    launchFromMenu(
                            intent
                    );
                }
        );

        drawerItem(
                "NOVA REVENDA",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                ResellersActivity.class
                        )
                )
        );

        drawerSection(
                "MÓDULOS"
        );

        drawerItem(
                "REGRAS",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                RulesActivity.class
                        )
                )
        );

        drawerItem(
                "REVENDAS",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                ResellersActivity.class
                        )
                )
        );

        drawerItem(
                "MENSAGENS E TEMPOS",
                true,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                MessageSettingsActivity.class
                        )
                )
        );

        drawerSection(
                "CONTROLE E DIAGNÓSTICO"
        );

        drawerItem(
                "MONITORAMENTO",
                true,
                () -> launchFromMenu(
                        new Intent(this, MonitoringActivity.class)
                )
        );

        drawerItem(
                "HISTÓRICO",
                false,
                () -> launchFromMenu(
                        new Intent(this, EventHistoryActivity.class)
                )
        );

        drawerItem(
                "SIMULADOR DE GATILHOS",
                false,
                () -> launchFromMenu(
                        new Intent(this, TriggerSimulatorActivity.class)
                )
        );

        drawerItem(
                "BACKUP / RESTAURAÇÃO",
                false,
                () -> launchFromMenu(
                        new Intent(this, BackupActivity.class)
                )
        );

        drawerItem(
                "CENTRAL DE DIAGNÓSTICO",
                false,
                () -> launchFromMenu(
                        new Intent(this, DiagnosticsActivity.class)
                )
        );

        drawerItem(
                "CONFIGURAÇÕES",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                AppSettingsActivity.class
                        )
                )
        );

        drawerScroll.addView(
                drawerPanel
        );

        FrameLayout.LayoutParams drawerLp =
                new FrameLayout.LayoutParams(
                        dp(292),
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.START
                );

        menuOverlay.addView(
                drawerScroll,
                drawerLp
        );

        shell.addView(
                menuOverlay,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
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

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(18), dp(18), dp(18));
        hero.addView(text("CENTRAL DE CONTROLE", 11, Color.rgb(0, 214, 143), true));
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("MASTER RESPONDE", 22, primaryTextColor(), true);
        titleBlock.addView(title);
        titleBlock.addView(text("Atendimento inteligente • Painel geral", 12, secondaryTextColor(), false));
        heroRow.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        botSwitch = new Switch(this);
        botSwitch.setChecked(prefs.getBoolean("bot_enabled", false));
        botSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("bot_enabled", isChecked).apply();
            if (isChecked) BackgroundRuntime.requestListenerRebind(this);
            refreshStatus();
            Toast.makeText(this, isChecked ? "Bot ativado." : "Bot desativado.", Toast.LENGTH_SHORT).show();
        });
        heroRow.addView(botSwitch);
        hero.addView(heroRow);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.bottomMargin = dp(18);
        content.addView(hero, heroLp);

        section("STATUS DO SISTEMA");
        GridLayout statusGrid = new GridLayout(this);
        statusGrid.setColumnCount(2);
        botStatus = addStatus(statusGrid, "BOT", "● Desativado",
                Color.rgb(0, 184, 255), null);
        waStatus = addStatus(statusGrid, "WHATSAPP BUSINESS", "● Verificando",
                Color.rgb(0, 184, 255), this::openNotificationAccess);
        masterflixStatus = addStatus(statusGrid, "MASTERFLIX", "● Login necessário",
                Color.rgb(255, 145, 0), () -> startActivity(new Intent(this, MasterflixActivity.class)));
        stvStatus = addStatus(statusGrid, "STV+", "● Configuração necessária",
                Color.rgb(255, 145, 0), () -> startActivity(new Intent(this, StvPlusActivity.class)));
        engineStatus = addStatus(statusGrid, "MOTOR EM 2º PLANO", "● Verificando",
                Color.rgb(255, 145, 0), this::openNotificationAccess);
        content.addView(statusGrid);

        gap(18);
        section("ATIVIDADE");
        GridLayout metrics = new GridLayout(this);
        metrics.setColumnCount(3);
        org.json.JSONArray events = EventLog.read(this);
        int received = 0, sent = 0, errors = 0;
        for (int i = 0; i < events.length(); i++) {
            org.json.JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String type = event.optString("type");
            if ("MENSAGEM RECEBIDA".equals(type)) received++;
            if ("RESPOSTA ENVIADA".equals(type)) sent++;
            if ("FALHA".equals(type)) errors++;
        }
        addMetric(metrics, "RECEBIDAS", String.valueOf(received), Color.rgb(0, 184, 255));
        addMetric(metrics, "RESPONDIDAS", String.valueOf(sent), Color.rgb(0, 214, 143));
        addMetric(metrics, "FALHAS", String.valueOf(errors), errors > 0 ? Color.rgb(255, 110, 90) : secondaryTextColor());
        content.addView(metrics);

        gap(18);
        section("MÓDULOS E AUTOMAÇÕES");
        GridLayout modules = new GridLayout(this);
        modules.setColumnCount(2);
        addModuleCard(modules, "MASTERFLIX", "Gerar teste e acessar painel",
                "↗", () -> startActivity(new Intent(this, MasterflixActivity.class)));
        addModuleCard(modules, "STV+", "Testes e gestão de revendas",
                "✦", () -> startActivity(new Intent(this, StvPlusActivity.class)));
        addModuleCard(modules, "REGRAS", "Gatilhos e respostas automáticas",
                "⌘", () -> startActivity(new Intent(this, RulesActivity.class)));
        addModuleCard(modules, "MONITORAMENTO", "Acompanhar o funcionamento",
                "◉", () -> startActivity(new Intent(this, MonitoringActivity.class)));
        content.addView(modules);

        gap(18);
        section("ACESSO RÁPIDO");
        LinearLayout access = card();
        access.setPadding(dp(14), dp(12), dp(14), dp(12));
        access.addView(text("WhatsApp Business", 15, primaryTextColor(), true));
        access.addView(text("Libere o acesso às notificações para manter o motor conectado.", 12, secondaryTextColor(), false));
        Button notificationButton = fullButton("CONFIGURAR ACESSO ÀS NOTIFICAÇÕES", true);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-1, dp(48));
        actionLp.topMargin = dp(10);
        access.addView(notificationButton, actionLp);
        notificationButton.setOnClickListener(v -> openNotificationAccess());
        content.addView(access);

        gap(14);
        LinearLayout last = card();
        last.setPadding(dp(15), dp(13), dp(15), dp(13));
        last.addView(text("ÚLTIMA ATIVIDADE", 11, Color.rgb(255, 150, 0), true));
        String lastStatus = prefs.getString("last_test_status", "Nenhum teste registrado até agora.");
        last.addView(text(lastStatus, 13, primaryTextColor(), true));
        content.addView(last);

        refreshStatus();
    }

    private void addMetric(GridLayout parent, String label, String value, int valueColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(13), dp(8), dp(13));
        box.setBackground(roundedBackground(surfaceColor(), borderColor(), 15));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(76);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        box.setLayoutParams(lp);
        box.addView(text(label, 9, secondaryTextColor(), true));
        box.addView(text(value, 22, valueColor, true));
        parent.addView(box);
    }

    private void addModuleCard(GridLayout parent, String label, String description, String symbol, Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(13), dp(12), dp(13));
        box.setBackground(roundedBackground(surfaceColor(), borderColor(), 16));
        box.setClickable(true);
        box.setFocusable(true);
        box.setOnClickListener(v -> action.run());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(112);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        box.setLayoutParams(lp);
        TextView icon = text(symbol, 19, Color.rgb(0, 214, 143), true);
        box.addView(icon);
        box.addView(text(label, 12, primaryTextColor(), true));
        box.addView(text(description, 10, secondaryTextColor(), false));
        parent.addView(box);
    }

    private void buildBottomNavigation(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(5), dp(8), dp(5), dp(8));
        nav.setBackground(roundedBackground(surfaceColor(), borderColor(), 22));

        addNavItem(nav, "▦", "Painel", null);
        addNavItem(nav, "☷", "Regras", () -> startActivity(new Intent(this, RulesActivity.class)));
        addNavItem(nav, "✦", "Testes", () -> startActivity(new Intent(this, MasterflixActivity.class)));
        addNavItem(nav, "◉", "Monitor", () -> startActivity(new Intent(this, MonitoringActivity.class)));
        addNavItem(nav, "☰", "Módulos", this::openMenu);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(66));
        lp.setMargins(dp(14), dp(5), dp(14), dp(8));
        root.addView(nav, lp);
    }

    private void addNavItem(LinearLayout parent, String icon, String label, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(3), dp(2), dp(3), dp(2));
        TextView symbol = text(icon, 19, Color.rgb(0, 214, 143), true);
        symbol.setGravity(Gravity.CENTER);
        item.addView(symbol);
        TextView caption = text(label, 9, secondaryTextColor(), false);
        caption.setGravity(Gravity.CENTER);
        item.addView(caption);
        item.setClickable(true);
        item.setFocusable(true);
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
                        secondaryTextColor(),
                        true
                );

        section.setLetterSpacing(
                0.08f
        );

        section.setPadding(
                0,
                dp(14),
                0,
                dp(7)
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
        LinearLayout c =
                new LinearLayout(
                        this
                );

        c.setOrientation(
                LinearLayout.VERTICAL
        );

        c.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        c.setBackground(
                roundedBackground(
                        surfaceColor(),
                        borderColor(),
                        18
                )
        );

        return c;
    }

    private TextView addStatus(
            GridLayout parent,
            String title,
            String value,
            int accent,
            Runnable click
    ) {
        LinearLayout c =
                card();

        GridLayout.LayoutParams lp =
                new GridLayout.LayoutParams();

        lp.width =
                0;

        lp.height =
                dp(88);

        lp.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        lp.setMargins(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
        );

        c.setLayoutParams(
                lp
        );

        c.addView(
                text(
                        title,
                        12,
                        secondaryTextColor(),
                        false
                )
        );

        TextView v =
                text(
                        value,
                        15,
                        accent,
                        true
                );

        v.setPadding(
                0,
                dp(10),
                0,
                0
        );

        c.addView(
                v
        );

        if (click != null) {
            c.setOnClickListener(
                    view -> click.run()
            );
        }

        parent.addView(
                c
        );

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

    private void section(
            String value
    ) {
        TextView t =
                text(
                        value,
                        12,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                );

        t.setLetterSpacing(
                0.08f
        );

        t.setPadding(
                0,
                0,
                0,
                dp(8)
        );

        content.addView(
                t
        );
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
