package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class AppSettingsActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        buildScreen();
    }

    private void buildScreen() {
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
                dp(18),
                dp(14),
                dp(18),
                dp(14)
        );

        TextView back =
                text(
                        "←",
                        24,
                        primaryTextColor(),
                        true
                );

        back.setGravity(
                Gravity.CENTER
        );

        back.setOnClickListener(
                v -> finish()
        );

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        LinearLayout titles =
                new LinearLayout(
                        this
                );

        titles.setOrientation(
                LinearLayout.VERTICAL
        );

        titles.addView(
                text(
                        "CONFIGURAÇÕES",
                        22,
                        primaryTextColor(),
                        true
                )
        );

        titles.addView(
                text(
                        "Preferências do MASTER RESPONDE",
                        12,
                        secondaryTextColor(),
                        false
                )
        );

        header.addView(
                titles,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
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
                dp(36)
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

        setContentView(
                root
        );

        renderSettings();
    }

    private void renderSettings() {
        content.removeAllViews();

        section(
                "APARÊNCIA"
        );

        LinearLayout appearance =
                card();

        appearance.addView(
                text(
                        "Tema da interface principal",
                        15,
                        primaryTextColor(),
                        true
                )
        );

        TextView current =
                text(
                        isLightTheme()
                                ? "Atual: Claro"
                                : "Atual: Escuro",
                        12,
                        secondaryTextColor(),
                        false
                );

        current.setPadding(
                0,
                dp(4),
                0,
                dp(10)
        );

        appearance.addView(
                current
        );

        Button dark =
                darkButton(
                        isLightTheme()
                                ? "USAR TEMA ESCURO"
                                : "✓ TEMA ESCURO"
                );

        dark.setOnClickListener(
                v -> setThemeMode(
                        "dark"
                )
        );

        appearance.addView(
                dark
        );

        Button light =
                darkButton(
                        isLightTheme()
                                ? "✓ TEMA CLARO"
                                : "USAR TEMA CLARO"
                );

        light.setOnClickListener(
                v -> setThemeMode(
                        "light"
                )
        );

        appearance.addView(
                light
        );

        content.addView(
                appearance
        );

        gap(
                14
        );

        section(
                "NAVEGAÇÃO"
        );

        LinearLayout navigation =
                card();

        LinearLayout returnMenu =
                settingRow(
                        "Voltar para o menu lateral",
                        "Depois de abrir uma opção pelo ☰, a seta Voltar retorna ao mesmo menu.",
                        "return_to_menu_enabled",
                        true
                );

        navigation.addView(
                returnMenu
        );

        content.addView(
                navigation
        );

        gap(
                14
        );

        section(
                "MOTOR"
        );

        LinearLayout engine =
                card();

        LinearLayout reconnect =
                settingRow(
                        "Reconexão automática",
                        "Tenta reconectar o acesso às notificações ao abrir, reiniciar ou atualizar o app.",
                        "auto_reconnect_engine",
                        true
                );

        engine.addView(
                reconnect
        );

        content.addView(
                engine
        );

        gap(
                14
        );

        section(
                "DASHBOARD"
        );

        LinearLayout dashboard =
                card();

        LinearLayout details =
                settingRow(
                        "Mostrar detalhes",
                        "Exibe credenciais do painel Sigma e último status do teste na tela principal.",
                        "show_detailed_status",
                        true
                );

        dashboard.addView(
                details
        );

        content.addView(
                dashboard
        );

        gap(
                14
        );

        section(
                "MANUTENÇÃO"
        );

        LinearLayout maintenance =
                card();

        Button reset =
                darkButton(
                        "RESTAURAR PREFERÊNCIAS DE INTERFACE"
                );

        reset.setOnClickListener(
                v -> {
                    prefs.edit()
                            .putString(
                                    "app_theme",
                                    "dark"
                            )
                            .putBoolean(
                                    "return_to_menu_enabled",
                                    true
                            )
                            .putBoolean(
                                    "auto_reconnect_engine",
                                    true
                            )
                            .putBoolean(
                                    "show_detailed_status",
                                    true
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            "Preferências restauradas.",
                            Toast.LENGTH_SHORT
                    ).show();

                    buildScreen();
                }
        );

        maintenance.addView(
                reset
        );

        TextView note =
                text(
                        "Esta opção não apaga regras, categorias, mensagens, credenciais ou sessão do painel Sigma.",
                        11,
                        secondaryTextColor(),
                        false
                );

        note.setPadding(
                0,
                dp(8),
                0,
                0
        );

        maintenance.addView(
                note
        );

        content.addView(
                maintenance
        );

        gap(
                16
        );

        LinearLayout about =
                card();

        about.addView(
                text(
                        "MASTER RESPONDE v1.17.6",
                        14,
                        primaryTextColor(),
                        true
                )
        );

        TextView aboutText =
                text(
                        "Motor unificado • WhatsApp Business • Painel Sigma • @infor em grupos",
                        11,
                        secondaryTextColor(),
                        false
                );

        aboutText.setPadding(
                0,
                dp(5),
                0,
                0
        );

        about.addView(
                aboutText
        );

        content.addView(
                about
        );
    }

    private LinearLayout settingRow(
            String title,
            String description,
            String key,
            boolean defaultValue
    ) {
        LinearLayout row =
                new LinearLayout(
                        this
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                0,
                dp(8),
                0,
                dp(8)
        );

        LinearLayout copy =
                new LinearLayout(
                        this
                );

        copy.setOrientation(
                LinearLayout.VERTICAL
        );

        copy.addView(
                text(
                        title,
                        14,
                        primaryTextColor(),
                        true
                )
        );

        TextView desc =
                text(
                        description,
                        11,
                        secondaryTextColor(),
                        false
                );

        desc.setPadding(
                0,
                dp(3),
                dp(8),
                0
        );

        copy.addView(
                desc
        );

        row.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Switch toggle =
                new Switch(
                        this
                );

        toggle.setChecked(
                prefs.getBoolean(
                        key,
                        defaultValue
                )
        );

        toggle.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        prefs.edit()
                                .putBoolean(
                                        key,
                                        isChecked
                                )
                                .apply()
        );

        row.addView(
                toggle
        );

        return row;
    }

    private void setThemeMode(
            String mode
    ) {
        prefs.edit()
                .putString(
                        "app_theme",
                        mode
                )
                .apply();

        buildScreen();
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
                ? Color.rgb(
                244,
                247,
                250
        )
                : Color.rgb(
                7,
                9,
                11
        );
    }

    private int surfaceColor() {
        return isLightTheme()
                ? Color.WHITE
                : Color.rgb(
                14,
                19,
                26
        );
    }

    private int buttonColor() {
        return isLightTheme()
                ? Color.rgb(
                235,
                240,
                245
        )
                : Color.rgb(
                18,
                26,
                35
        );
    }

    private int borderColor() {
        return isLightTheme()
                ? Color.rgb(
                205,
                214,
                223
        )
                : Color.rgb(
                52,
                69,
                84
        );
    }

    private int primaryTextColor() {
        return isLightTheme()
                ? Color.rgb(
                20,
                27,
                34
        )
                : Color.WHITE;
    }

    private int secondaryTextColor() {
        return isLightTheme()
                ? Color.rgb(
                90,
                105,
                118
        )
                : Color.rgb(
                150,
                160,
                170
        );
    }

    private LinearLayout card() {
        LinearLayout card =
                new LinearLayout(
                        this
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );

        card.setBackground(
                roundedBackground(
                        surfaceColor(),
                        borderColor(),
                        16
                )
        );

        return card;
    }

    private Button darkButton(
            String label
    ) {
        Button button =
                new Button(
                        this
                );

        button.setText(
                label
        );

        button.setTextColor(
                primaryTextColor()
        );

        button.setTextSize(
                12
        );

        button.setTypeface(
                null,
                1
        );

        button.setBackground(
                roundedBackground(
                        buttonColor(),
                        borderColor(),
                        13
                )
        );

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)
                );

        lp.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        button.setLayoutParams(
                lp
        );

        return button;
    }

    private void section(
            String label
    ) {
        TextView section =
                text(
                        label,
                        11,
                        isLightTheme()
                                ? Color.rgb(
                                0,
                                120,
                                180
                        )
                                : Color.rgb(
                                0,
                                184,
                                255
                        ),
                        true
                );

        section.setLetterSpacing(
                0.08f
        );

        section.setPadding(
                0,
                0,
                0,
                dp(7)
        );

        content.addView(
                section
        );
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView text =
                new TextView(
                        this
                );

        text.setText(
                value
        );

        text.setTextSize(
                size
        );

        text.setTextColor(
                color
        );

        if (bold) {
            text.setTypeface(
                    null,
                    1
            );
        }

        return text;
    }

    private GradientDrawable roundedBackground(
            int fill,
            int stroke,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                fill
        );

        drawable.setCornerRadius(
                dp(radius)
        );

        drawable.setStroke(
                dp(1),
                stroke
        );

        return drawable;
    }

    private void gap(
            int height
    ) {
        View gap =
                new View(
                        this
                );

        content.addView(
                gap,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
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
