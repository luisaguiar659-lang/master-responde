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

        shell.setBackgroundColor(
                backgroundColor()
        );

        LinearLayout root =
                new LinearLayout(
                        this
                );

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                backgroundColor()
        );

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

        drawerItem(
                "PAINEL SIGMA",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                SigmaPanelSettingsActivity.class
                        )
                )
        );

        drawerItem(
                "MÍDIAS / LINKS",
                true,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                CategoriesActivity.class
                        )
                )
        );

        drawerItem(
                "@INFOR GRUPOS",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                GroupsActivity.class
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
                "MÍDIAS",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                CategoriesActivity.class
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
                "GRUPOS",
                false,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                GroupsActivity.class
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

        drawerItem(
                "HISTÓRICO",
                false,
                () -> Toast.makeText(
                        this,
                        "Histórico será conectado em uma próxima etapa.",
                        Toast.LENGTH_SHORT
                ).show()
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

        LinearLayout hero =
                card();

        LinearLayout heroRow =
                new LinearLayout(
                        this
                );

        heroRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        heroRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout heroText =
                new LinearLayout(
                        this
                );

        heroText.setOrientation(
                LinearLayout.VERTICAL
        );

        heroText.addView(
                text(
                        "MASTER RESPONDE",
                        24,
                        primaryTextColor(),
                        true
                )
        );

        heroText.addView(
                text(
                        "Dashboard v1.17.12",
                        14,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                )
        );

        heroText.addView(
                text(
                        "Painel Sigma configurável com os mesmos fluxos automáticos.",
                        13,
                        secondaryTextColor(),
                        false
                )
        );

        heroRow.addView(
                heroText,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        botSwitch =
                new Switch(
                        this
                );

        botSwitch.setChecked(
                prefs.getBoolean(
                        "bot_enabled",
                        false
                )
        );

        botSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    prefs.edit()
                            .putBoolean(
                                    "bot_enabled",
                                    isChecked
                            )
                            .apply();

                    if (isChecked) {
                        BackgroundRuntime.requestListenerRebind(
                                this
                        );
                    }

                    refreshStatus();

                    Toast.makeText(
                            this,
                            isChecked
                                    ? "Bot ativado."
                                    : "Bot desativado.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        heroRow.addView(
                botSwitch
        );

        hero.addView(
                heroRow
        );

        content.addView(
                hero
        );

        gap(
                16
        );

        section(
                "STATUS"
        );

        GridLayout statusGrid =
                new GridLayout(
                        this
                );

        statusGrid.setColumnCount(
                2
        );

        botStatus =
                addStatus(
                        statusGrid,
                        "Bot",
                        "● Desativado",
                        Color.rgb(
                                0,
                                184,
                                255
                        ),
                        null
                );

        waStatus =
                addStatus(
                        statusGrid,
                        "WhatsApp Business",
                        "● Verificando",
                        Color.rgb(
                                0,
                                184,
                                255
                        ),
                        this::openNotificationAccess
                );

        masterflixStatus =
                addStatus(
                        statusGrid,
                        SigmaPanelConfig.getPanelName(this),
                        "● Login necessário",
                        Color.rgb(
                                255,
                                145,
                                0
                        ),
                        () -> startActivity(
                                new Intent(
                                        this,
                                        MasterflixActivity.class
                                )
                        )
                );

        engineStatus =
                addStatus(
                        statusGrid,
                        "Motor",
                        "● Verificando",
                        Color.rgb(
                                255,
                                145,
                                0
                        ),
                        this::openNotificationAccess
                );

        content.addView(
                statusGrid
        );

        gap(
                18
        );

        Button notificationButton =
                fullButton(
                        "LIBERAR ACESSO ÀS NOTIFICAÇÕES",
                        true
                );

        notificationButton.setOnClickListener(
                v -> openNotificationAccess()
        );

        content.addView(
                notificationButton
        );

        gap(
                12
        );

        LinearLayout info =
                card();

        info.addView(
                text(
                        "PAINEL SIGMA",
                        12,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                )
        );

        TextView credentialsInfo =
                text(
                        prefs.getBoolean(
                                "masterflix_credentials_saved",
                                false
                        )
                                ? "Credenciais salvas para " + SigmaPanelConfig.getPanelName(this) + ". Se a sessão expirar, o bot tentará entrar automaticamente."
                                : "Nenhuma credencial salva. Abra PAINEL SIGMA no menu para configurar URL, usuário e senha.",
                        12,
                        secondaryTextColor(),
                        false
                );

        credentialsInfo.setPadding(
                0,
                dp(8),
                0,
                0
        );

        info.addView(
                credentialsInfo
        );

        TextView lastTest =
                text(
                        prefs.getString(
                                "last_test_status",
                                "Último teste: ainda não realizado"
                        ),
                        12,
                        Color.rgb(
                                0,
                                184,
                                255
                        ),
                        true
                );

        lastTest.setPadding(
                0,
                dp(12),
                0,
                0
        );

        info.addView(
                lastTest
        );

        if (prefs.getBoolean(
                "show_detailed_status",
                true
        )) {
            content.addView(
                    info
            );

            gap(
                    10
            );

            content.addView(
                    text(
                            "Atalhos e módulos ficam no menu ☰ no canto superior direito.",
                            11,
                            secondaryTextColor(),
                            false
                    )
            );
        }

        refreshStatus();
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
                dp(110);

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
                : Color.rgb(7, 9, 11);
    }

    private int surfaceColor() {
        return isLightTheme()
                ? Color.WHITE
                : Color.rgb(14, 19, 26);
    }

    private int drawerColor() {
        return isLightTheme()
                ? Color.rgb(248, 250, 252)
                : Color.rgb(13, 18, 23);
    }

    private int buttonColor() {
        return isLightTheme()
                ? Color.rgb(235, 240, 245)
                : Color.rgb(18, 26, 35);
    }

    private int borderColor() {
        return isLightTheme()
                ? Color.rgb(205, 214, 223)
                : Color.rgb(52, 69, 84);
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
