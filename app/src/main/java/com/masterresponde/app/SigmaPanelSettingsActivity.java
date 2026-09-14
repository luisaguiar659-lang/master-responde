package com.masterresponde.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SigmaPanelSettingsActivity extends Activity {

    private SharedPreferences prefs;

    private EditText nameField;
    private EditText urlField;
    private EditText userField;
    private EditText passField;
    private EditText testField;
    private EditText signupField;
    private EditText supportField;

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
        LinearLayout root =
                new LinearLayout(
                        this
                );

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(
                        7,
                        9,
                        11
                )
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
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        Button back =
                new Button(
                        this
                );

        back.setText(
                "←"
        );

        back.setTextColor(
                Color.WHITE
        );

        back.setTextSize(
                24
        );

        back.setBackgroundColor(
                Color.TRANSPARENT
        );

        back.setOnClickListener(
                v -> finish()
        );

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(54)
                )
        );

        LinearLayout title =
                new LinearLayout(
                        this
                );

        title.setOrientation(
                LinearLayout.VERTICAL
        );

        title.addView(
                text(
                        "PAINEL SIGMA",
                        21,
                        Color.WHITE,
                        true
                )
        );

        title.addView(
                text(
                        "Perfil do painel usado pelas automações",
                        12,
                        Color.rgb(
                                150,
                                160,
                                170
                        ),
                        false
                )
        );

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1f
                )
        );

        root.addView(
                header
        );

        ScrollView scroll =
                new ScrollView(
                        this
                );

        LinearLayout content =
                new LinearLayout(
                        this
                );

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(40)
        );

        LinearLayout panel =
                card();

        panel.addView(
                sectionText(
                        "IDENTIFICAÇÃO"
                )
        );

        nameField =
                field(
                        "Nome do painel"
                );

        nameField.setText(
                SigmaPanelConfig.getPanelName(
                        this
                )
        );

        panel.addView(
                nameField
        );

        urlField =
                field(
                        "URL base do painel Sigma"
                );

        urlField.setText(
                SigmaPanelConfig.getBaseUrl(
                        this
                )
        );

        panel.addView(
                urlField
        );

        panel.addView(
                note(
                        "Exemplo: https://seudominio.sigmab.pro. As rotas de Login, Dashboard e Revendas são preenchidas automaticamente."
                )
        );

        content.addView(
                panel
        );

        gap(
                content,
                12
        );

        LinearLayout credentials =
                card();

        credentials.addView(
                sectionText(
                        "CREDENCIAIS"
                )
        );

        userField =
                field(
                        "Usuário ou e-mail"
                );

        userField.setText(
                SigmaPanelConfig.getUser(
                        this
                )
        );

        credentials.addView(
                userField
        );

        passField =
                field(
                        "Senha"
                );

        passField.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        passField.setText(
                SigmaPanelConfig.getPass(
                        this
                )
        );

        credentials.addView(
                passField
        );

        content.addView(
                credentials
        );

        gap(
                content,
                12
        );

        LinearLayout automation =
                card();

        automation.addView(
                sectionText(
                        "TESTE AUTOMÁTICO"
                )
        );

        testField =
                field(
                        "Texto preferido do teste (opcional)"
                );

        testField.setText(
                SigmaPanelConfig.getPreferredTest(
                        this
                )
        );

        automation.addView(
                testField
        );

        automation.addView(
                note(
                        "Se ficar vazio, o bot procura dinamicamente um teste disponível e prioriza opções com “COMPLETO”."
                )
        );

        content.addView(
                automation
        );

        gap(
                content,
                12
        );

        LinearLayout reseller =
                card();

        reseller.addView(
                sectionText(
                        "REVENDA"
                )
        );

        signupField =
                field(
                        "Link de cadastro da revenda"
                );

        signupField.setText(
                prefs.getString(
                        "reseller_signup_link",
                        ResellersActivity.DEFAULT_SIGNUP_LINK
                )
        );

        reseller.addView(
                signupField
        );

        supportField =
                field(
                        "Link do grupo de suporte"
                );

        supportField.setText(
                prefs.getString(
                        "reseller_support_group_link",
                        ResellersActivity.DEFAULT_SUPPORT_GROUP_LINK
                )
        );

        reseller.addView(
                supportField
        );

        content.addView(
                reseller
        );

        gap(
                content,
                16
        );

        Button save =
                actionButton(
                        "SALVAR PERFIL SIGMA"
                );

        save.setOnClickListener(
                v -> saveProfile()
        );

        content.addView(
                save
        );

        Button openPanel =
                darkButton(
                        "ABRIR MINI PAINEL"
                );

        openPanel.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                MasterflixActivity.class
                        )
                )
        );

        content.addView(
                openPanel
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
    }

    private void saveProfile() {
        String name =
                nameField.getText()
                        .toString()
                        .trim();

        String url =
                SigmaPanelConfig.normalizeBaseUrl(
                        urlField.getText()
                                .toString()
                );

        String user =
                userField.getText()
                        .toString()
                        .trim();

        String pass =
                passField.getText()
                        .toString();

        if (name.isEmpty()) {
            nameField.setError(
                    "Informe o nome do painel."
            );
            return;
        }

        if (url.isEmpty()) {
            urlField.setError(
                    "Informe a URL do painel."
            );
            return;
        }

        prefs.edit()
                .putString(
                        SigmaPanelConfig.KEY_PANEL_NAME,
                        name
                )
                .putString(
                        SigmaPanelConfig.KEY_BASE_URL,
                        url
                )
                .putString(
                        SigmaPanelConfig.KEY_PREFERRED_TEST,
                        testField.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "reseller_signup_link",
                        signupField.getText()
                                .toString()
                                .trim()
                )
                .putString(
                        "reseller_support_group_link",
                        supportField.getText()
                                .toString()
                                .trim()
                )
                .putBoolean(
                        "masterflix_session_active",
                        false
                )
                .apply();

        if (!user.isEmpty()
                && !pass.isEmpty()) {
            SigmaPanelConfig.saveCredentials(
                    this,
                    user,
                    pass
            );
        }

        Toast.makeText(
                this,
                "Perfil Sigma salvo. A sessão será aberta no painel configurado.",
                Toast.LENGTH_LONG
        ).show();

        buildScreen();
    }

    private EditText field(
            String hint
    ) {
        EditText field =
                new EditText(
                        this
                );

        field.setHint(
                hint
        );

        field.setTextColor(
                Color.WHITE
        );

        field.setHintTextColor(
                Color.rgb(
                        120,
                        135,
                        145
                )
        );

        field.setTextSize(
                13
        );

        field.setSingleLine(
                true
        );

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54)
                );

        lp.setMargins(
                0,
                dp(4),
                0,
                dp(4)
        );

        field.setLayoutParams(
                lp
        );

        return field;
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

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(
                        14,
                        19,
                        26
                )
        );

        background.setCornerRadius(
                dp(16)
        );

        background.setStroke(
                dp(1),
                Color.rgb(
                        52,
                        69,
                        84
                )
        );

        card.setBackground(
                background
        );

        return card;
    }

    private TextView sectionText(
            String value
    ) {
        TextView text =
                text(
                        value,
                        11,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                );

        text.setPadding(
                0,
                0,
                0,
                dp(8)
        );

        return text;
    }

    private TextView note(
            String value
    ) {
        TextView text =
                text(
                        value,
                        11,
                        Color.rgb(
                                145,
                                155,
                                165
                        ),
                        false
                );

        text.setPadding(
                0,
                dp(4),
                0,
                dp(6)
        );

        return text;
    }

    private Button actionButton(
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
                Color.WHITE
        );

        button.setTextSize(
                13
        );

        button.setTypeface(
                null,
                1
        );

        button.setBackgroundResource(
                getResources().getIdentifier(
                        "button_orange",
                        "drawable",
                        getPackageName()
                )
        );

        button.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                )
        );

        return button;
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
                Color.WHITE
        );

        button.setTextSize(
                13
        );

        button.setTypeface(
                null,
                1
        );

        button.setBackgroundResource(
                getResources().getIdentifier(
                        "button_dark",
                        "drawable",
                        getPackageName()
                )
        );

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );

        lp.setMargins(
                0,
                dp(10),
                0,
                0
        );

        button.setLayoutParams(
                lp
        );

        return button;
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

    private void gap(
            LinearLayout content,
            int height
    ) {
        android.view.View gap =
                new android.view.View(
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
