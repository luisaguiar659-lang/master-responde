package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageSettingsActivity extends Activity {

    private SharedPreferences prefs;
    private SharedPreferences stvPrefs;
    private CheckBox masterflixExactCheck;
    private CheckBox stvExactCheck;

    private final Map<String, EditText> stvTriggerFields = new LinkedHashMap<>();
    private final Map<String, EditText> stvTextFields = new LinkedHashMap<>();
    private final Map<String, CheckBox> stvEnabledFields = new LinkedHashMap<>();

    private final Map<String, EditText> textFields =
            new LinkedHashMap<>();

    private final Map<String, CheckBox> enabledFields =
            new LinkedHashMap<>();

    private final Map<String, EditText> numberFields =
            new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        stvPrefs = getSharedPreferences("master_responde_stv", MODE_PRIVATE);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root =
                new LinearLayout(this);

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
                new LinearLayout(this);

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
                new Button(this);

        back.setText("←");
        back.setTextColor(Color.WHITE);
        back.setTextSize(24);
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

        LinearLayout titles =
                new LinearLayout(this);

        titles.setOrientation(
                LinearLayout.VERTICAL
        );

        titles.addView(
                text(
                        "MENSAGENS E TEMPOS",
                        20,
                        Color.WHITE,
                        true
                )
        );

        titles.addView(
                text(
                        "Textos, gatilhos e segundos",
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
                titles,
                new LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1f
                )
        );

        root.addView(header);

        View line =
                new View(this);

        line.setBackgroundResource(
                getResources()
                        .getIdentifier(
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
                new ScrollView(this);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(40)
        );

        addIntro(
                content
        );

        addSection(
                content,
                "GATILHOS RECEBIDOS"
        );

        addTextSetting(
                content,
                MessageSettings.KEY_TEST_TRIGGERS,
                "Gatilhos para gerar teste",
                "Um por linha.",
                MessageSettings.DEFAULT_TEST_TRIGGERS,
                false,
                false
        );

        masterflixExactCheck = addMatchModeSetting(
                content,
                "Correspondência do gatilho MasterFlix",
                prefs.getBoolean(MessageSettings.KEY_TEST_MATCH_EXACT, true),
                "Quando marcado, a mensagem precisa ser igual ao gatilho. Desmarcado: basta conter o texto."
        );

        addSection(content, "GATILHOS RECEBIDOS — STV+");
        addStvTriggerSetting(content);
        stvExactCheck = addMatchModeSetting(
                content,
                "Correspondência do gatilho STV+",
                stvPrefs.getBoolean(StvTriggerSettingsActivity.KEY_MATCH_EXACT, true),
                "Quando marcado, a mensagem precisa ser igual ao gatilho. Desmarcado: basta conter o texto."
        );

        addTextSetting(
                content,
                MessageSettings.KEY_RESELLER_TRIGGERS,
                "Gatilhos para nova revenda",
                "Um por linha.",
                MessageSettings.DEFAULT_RESELLER_TRIGGERS,
                false,
                false
        );

        addSection(content, "GATILHOS RECEBIDOS — REVENDA STV+");
        addStvTriggerTextSetting(content, MessageSettings.KEY_STV_RESELLER_TRIGGERS,
                "Gatilhos para nova revenda STV+", MessageSettings.DEFAULT_STV_RESELLER_TRIGGERS);
        addStvTriggerTextSetting(content, MessageSettings.KEY_STV_RESELLER_CANCEL_TRIGGER,
                "Comando para cancelar revenda STV+", MessageSettings.DEFAULT_STV_RESELLER_CANCEL_TRIGGER);

        addTextSetting(
                content,
                MessageSettings.KEY_RESELLER_CANCEL_TRIGGER,
                "Comando para cancelar revenda",
                "Exemplo: cancelar",
                MessageSettings.DEFAULT_RESELLER_CANCEL_TRIGGER,
                false,
                true
        );

        addTextSetting(
                content,
                MessageSettings.KEY_DIAGNOSTIC_TRIGGER,
                "Gatilho de teste do bot",
                "Exemplo: teste bot",
                MessageSettings.DEFAULT_DIAGNOSTIC_TRIGGER,
                false,
                true
        );

        addSection(
                content,
                "TEMPOS — SEGUNDOS"
        );

        addNumberSetting(
                content,
                MessageSettings.KEY_REPLY_DELAY_SECONDS,
                "Atraso antes de responder",
                "Tempo entre receber uma mensagem e começar a resposta.",
                MessageSettings.DEFAULT_REPLY_DELAY_SECONDS
        );

        addNumberSetting(
                content,
                MessageSettings.KEY_MIN_REPLY_INTERVAL_SECONDS,
                "Intervalo mínimo entre respostas",
                "Evita duas respostas saírem juntas para a mesma conversa.",
                MessageSettings.DEFAULT_MIN_REPLY_INTERVAL_SECONDS
        );

        addNumberSetting(
                content,
                MessageSettings.KEY_SEQUENCE_INTERVAL_SECONDS,
                "Intervalo em sequências",
                "Usado nas categorias com Texto 1, Texto 2, Texto 3...",
                MessageSettings.DEFAULT_SEQUENCE_INTERVAL_SECONDS
        );

        addNumberSetting(
                content,
                MessageSettings.KEY_PRIVATE_DUPLICATE_SECONDS,
                "Bloqueio de mensagem recebida repetida",
                "Durante esse tempo a mesma mensagem privada não é processada de novo.",
                MessageSettings.DEFAULT_PRIVATE_DUPLICATE_SECONDS
        );

        addNumberSetting(
                content,
                MessageSettings.KEY_RULE_DUPLICATE_SECONDS,
                "Bloqueio de regra repetida",
                "Evita executar a mesma regra novamente logo em seguida.",
                MessageSettings.DEFAULT_RULE_DUPLICATE_SECONDS
        );

        addNumberSetting(content, MessageSettings.KEY_GROUP_DUPLICATE_SECONDS,
                "Bloqueio de mensagem repetida em grupos",
                "Tempo para ignorar o mesmo comando repetido no mesmo grupo.",
                MessageSettings.DEFAULT_GROUP_DUPLICATE_SECONDS);

        addNumberSetting(content, MessageSettings.KEY_GROUP_MENU_SECONDS,
                "Tempo para manter o menu de grupos aberto",
                "Tempo máximo para aceitar opções depois de abrir o menu.",
                MessageSettings.DEFAULT_GROUP_MENU_SECONDS);

        addSection(content, "MENSAGENS ENVIADAS — GRUPOS");
        addTextSetting(content, MessageSettings.MSG_GROUP_MENU, "Menu principal dos grupos",
                "Variáveis: {CATEGORIAS} e {COMANDO_MENU}.", MessageSettings.DEFAULT_GROUP_MENU, true, false);
        addTextSetting(content, MessageSettings.MSG_GROUP_CATEGORY_MISSING, "Categoria não configurada",
                "Variável: {CATEGORIA}.", MessageSettings.DEFAULT_GROUP_CATEGORY_MISSING, true, false);
        addTextSetting(content, MessageSettings.MSG_GROUP_UNKNOWN, "Opção não reconhecida",
                "Variável: {COMANDO_MENU}.", MessageSettings.DEFAULT_GROUP_UNKNOWN, true, false);
        
        addSection(
                content,
                "MENSAGENS ENVIADAS"
        );

        addTextSetting(
                content,
                MessageSettings.MSG_DIAGNOSTIC,
                "Teste do bot",
                "Resposta ao gatilho de diagnóstico.",
                MessageSettings.DEFAULT_DIAGNOSTIC,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_TEST_WAIT,
                "Teste — aguardando",
                "Enviada antes de gerar o teste.",
                MessageSettings.DEFAULT_TEST_WAIT,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_TEST_FAIL,
                "Teste — falha",
                "Enviada se o teste não puder ser concluído.",
                MessageSettings.DEFAULT_TEST_FAIL,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_SIGNUP,
                "Revenda — link de cadastro",
                "Use {LINK_CADASTRO} para inserir automaticamente o link salvo.",
                MessageSettings.DEFAULT_RESELLER_SIGNUP,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_VERIFYING,
                "Revenda — verificando e-mail",
                "Enviada depois que o cliente manda o e-mail.",
                MessageSettings.DEFAULT_RESELLER_VERIFYING,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_CONFIRMED,
                "Revenda — cadastro confirmado",
                "Use {LINK_GRUPO} para inserir automaticamente o grupo de suporte.",
                MessageSettings.DEFAULT_RESELLER_CONFIRMED,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_NOT_FOUND,
                "Revenda — não confirmado",
                "Use {MOTIVO} para incluir o motivo retornado pela automação.",
                MessageSettings.DEFAULT_RESELLER_NOT_FOUND,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_CANCELLED,
                "Revenda — cancelado",
                "",
                MessageSettings.DEFAULT_RESELLER_CANCELLED,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_ALREADY_LINKED,
                "Revenda — já vinculada",
                "",
                MessageSettings.DEFAULT_RESELLER_ALREADY_LINKED,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_REVIEW,
                "Revenda — revisão manual",
                "",
                MessageSettings.DEFAULT_RESELLER_REVIEW,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_INVALID_EMAIL,
                "Revenda — e-mail inválido",
                "Desativada por padrão para preservar o comportamento estável. Ative se quiser orientar o cliente.",
                MessageSettings.DEFAULT_RESELLER_INVALID_EMAIL,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_SAVE_ERROR,
                "Revenda — erro ao salvar e-mail",
                "",
                MessageSettings.DEFAULT_RESELLER_SAVE_ERROR,
                true,
                false
        );

        addTextSetting(
                content,
                MessageSettings.MSG_RESELLER_BUSY,
                "Revenda — verificação ocupada",
                "",
                MessageSettings.DEFAULT_RESELLER_BUSY,
                true,
                false
        );

        addSection(content, "MENSAGENS ENVIADAS — REVENDA STV+");
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_SIGNUP, "Revenda STV+ — link de cadastro",
                "Use {LINK_CADASTRO} para inserir o link STV+ salvo.", MessageSettings.DEFAULT_STV_RESELLER_SIGNUP, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_VERIFYING, "Revenda STV+ — verificando e-mail",
                "Enviada depois que o cliente manda o e-mail.", MessageSettings.DEFAULT_STV_RESELLER_VERIFYING, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_CONFIRMED, "Revenda STV+ — cadastro confirmado",
                "Use {LINK_GRUPO} para inserir o grupo de suporte STV+.", MessageSettings.DEFAULT_STV_RESELLER_CONFIRMED, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_NOT_FOUND, "Revenda STV+ — não confirmado",
                "Use {MOTIVO} para incluir o motivo retornado pela automação.", MessageSettings.DEFAULT_STV_RESELLER_NOT_FOUND, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_CANCELLED, "Revenda STV+ — cancelado",
                "", MessageSettings.DEFAULT_STV_RESELLER_CANCELLED, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_ALREADY_LINKED, "Revenda STV+ — já vinculada",
                "", MessageSettings.DEFAULT_STV_RESELLER_ALREADY_LINKED, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_REVIEW, "Revenda STV+ — revisão manual",
                "", MessageSettings.DEFAULT_STV_RESELLER_REVIEW, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_INVALID_EMAIL, "Revenda STV+ — e-mail inválido",
                "", MessageSettings.DEFAULT_STV_RESELLER_INVALID_EMAIL, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_SAVE_ERROR, "Revenda STV+ — erro ao salvar e-mail",
                "", MessageSettings.DEFAULT_STV_RESELLER_SAVE_ERROR, true);
        addStvTextSetting(content, MessageSettings.MSG_STV_RESELLER_BUSY, "Revenda STV+ — verificação ocupada",
                "", MessageSettings.DEFAULT_STV_RESELLER_BUSY, true);

        gap(
                content,
                8
        );

        Button saveAll =
                actionButton(
                        "SALVAR TODAS AS CONFIGURAÇÕES",
                        true
                );

        saveAll.setOnClickListener(
                v -> saveAll()
        );

        content.addView(
                saveAll
        );

        gap(
                content,
                8
        );

        Button restore =
                actionButton(
                        "RESTAURAR PADRÕES DA v1.9",
                        false
                );

        restore.setOnClickListener(
                v -> restoreDefaults()
        );

        content.addView(
                restore
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

    private CheckBox addMatchModeSetting(
            LinearLayout content,
            String title,
            boolean checked,
            String description
    ) {
        LinearLayout c = card();
        c.addView(text(title, 14, Color.WHITE, true));
        c.addView(text(description, 11, Color.rgb(145, 155, 165), false));
        CheckBox box = new CheckBox(this);
        box.setText("Exigir mensagem exatamente igual ao gatilho");
        box.setTextColor(Color.rgb(205, 215, 220));
        box.setChecked(checked);
        c.addView(box);
        content.addView(c);
        gap(content, 10);
        return box;
    }

    private void addStvTriggerSetting(
            LinearLayout content
    ) {
        LinearLayout c = card();

        c.addView(
                text(
                        "Gatilhos para gerar teste STV+",
                        15,
                        Color.WHITE,
                        true
                )
        );

        TextView desc = text(
                "Um por linha. Estes comandos ativam somente o módulo STV+ e podem ser alterados independentemente dos gatilhos do MasterFlix.",
                11,
                Color.rgb(145, 155, 165),
                false
        );
        desc.setPadding(0, dp(3), 0, dp(6));
        c.addView(desc);

        EditText field = new EditText(this);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.rgb(120, 130, 140));
        field.setTextSize(13);
        field.setSingleLine(false);
        field.setMinLines(4);
        field.setGravity(Gravity.TOP);
        field.setText(
                stvPrefs.getString(
                        StvTriggerSettingsActivity.KEY_TRIGGERS,
                        StvTriggerSettingsActivity.DEFAULT_TRIGGERS
                )
        );

        stvTriggerFields.put(
                StvTriggerSettingsActivity.KEY_TRIGGERS,
                field
        );
        c.addView(field);
        content.addView(c);
        gap(content, 10);
    }


    private void addStvTriggerTextSetting(
            LinearLayout content,
            String key,
            String title,
            String defaultValue
    ) {
        LinearLayout c = card();
        c.addView(text(title, 15, Color.WHITE, true));
        TextView desc = text("Um por linha.", 11, Color.rgb(145, 155, 165), false);
        desc.setPadding(0, dp(3), 0, dp(6));
        c.addView(desc);
        EditText field = new EditText(this);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.rgb(120, 130, 140));
        field.setTextSize(13);
        field.setSingleLine(false);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP);
        field.setText(stvPrefs.getString(key, defaultValue));
        stvTriggerFields.put(key, field);
        c.addView(field);
        content.addView(c);
        gap(content, 10);
    }


    private void addStvTextSetting(
            LinearLayout content,
            String key,
            String title,
            String description,
            String defaultValue,
            boolean withEnabled
    ) {
        LinearLayout c = card();
        c.addView(text(title, 15, Color.WHITE, true));
        if (description != null && !description.trim().isEmpty()) {
            TextView desc = text(description, 11, Color.rgb(145, 155, 165), false);
            desc.setPadding(0, dp(3), 0, dp(6));
            c.addView(desc);
        }
        if (withEnabled) {
            CheckBox enabled = new CheckBox(this);
            enabled.setText("Enviar esta mensagem");
            enabled.setTextColor(Color.rgb(205, 215, 220));
            enabled.setChecked(stvPrefs.getBoolean(
                    MessageSettings.enabledKey(key),
                    MessageSettings.defaultEnabled(key)
            ));
            stvEnabledFields.put(key, enabled);
            c.addView(enabled);
        }
        EditText field = new EditText(this);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.rgb(120, 130, 140));
        field.setTextSize(13);
        field.setSingleLine(false);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP);
        field.setText(stvPrefs.getString(key, defaultValue));
        stvTextFields.put(key, field);
        c.addView(field);
        content.addView(c);
        gap(content, 10);
    }

    private void addIntro(
            LinearLayout content
    ) {
        LinearLayout card =
                card();

        card.addView(
                text(
                        "CONTROLE MANUAL",
                        11,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                )
        );

        card.addView(
                text(
                        "Aqui você controla os textos automáticos e os tempos do bot. " +
                        "As respostas criadas em REGRAS e MÍDIAS continuam sendo editadas nos módulos próprios, " +
                        "mas também obedecem aos tempos configurados nesta tela. " +
                        "Para respostas do WhatsApp, prefira atrasos curtos para não deixar a notificação expirar.",
                        13,
                        Color.rgb(
                                205,
                                215,
                                220
                        ),
                        false
                )
        );

        content.addView(
                card
        );

        gap(
                content,
                14
        );
    }

    private void addSection(
            LinearLayout content,
            String title
    ) {
        TextView section =
                text(
                        title,
                        12,
                        Color.rgb(
                                255,
                                150,
                                0
                        ),
                        true
                );

        section.setPadding(
                dp(2),
                dp(8),
                dp(2),
                dp(6)
        );

        content.addView(
                section
        );
    }

    private void addTextSetting(
            LinearLayout content,
            String key,
            String title,
            String description,
            String defaultValue,
            boolean withEnabled,
            boolean singleLine
    ) {
        LinearLayout c =
                card();

        c.addView(
                text(
                        title,
                        15,
                        Color.WHITE,
                        true
                )
        );

        if (description != null
                && !description.trim().isEmpty()) {

            TextView desc =
                    text(
                            description,
                            11,
                            Color.rgb(
                                    145,
                                    155,
                                    165
                            ),
                            false
                    );

            desc.setPadding(
                    0,
                    dp(3),
                    0,
                    dp(6)
            );

            c.addView(
                    desc
            );
        }

        if (withEnabled) {
            CheckBox enabled =
                    new CheckBox(this);

            enabled.setText(
                    "Enviar esta mensagem"
            );

            enabled.setTextColor(
                    Color.rgb(
                            205,
                            215,
                            220
                    )
            );

            enabled.setChecked(
                    prefs.getBoolean(
                            MessageSettings.enabledKey(
                                    key
                            ),
                            MessageSettings.defaultEnabled(
                                    key
                            )
                    )
            );

            enabledFields.put(
                    key,
                    enabled
            );

            c.addView(
                    enabled
            );
        }

        EditText field =
                new EditText(this);

        field.setTextColor(
                Color.WHITE
        );

        field.setHintTextColor(
                Color.rgb(
                        120,
                        130,
                        140
                )
        );

        field.setTextSize(
                13
        );

        field.setText(
                prefs.getString(
                        key,
                        defaultValue
                )
        );

        if (singleLine) {
            field.setSingleLine(
                    true
            );
        } else {
            field.setSingleLine(
                    false
            );

            field.setMinLines(
                    3
            );

            field.setGravity(
                    Gravity.TOP
            );
        }

        textFields.put(
                key,
                field
        );

        c.addView(
                field
        );

        content.addView(
                c
        );

        gap(
                content,
                10
        );
    }

    private void addNumberSetting(
            LinearLayout content,
            String key,
            String title,
            String description,
            int defaultValue
    ) {
        LinearLayout c =
                card();

        c.addView(
                text(
                        title,
                        15,
                        Color.WHITE,
                        true
                )
        );

        TextView desc =
                text(
                        description,
                        11,
                        Color.rgb(
                                145,
                                155,
                                165
                        ),
                        false
                );

        desc.setPadding(
                0,
                dp(3),
                0,
                dp(6)
        );

        c.addView(
                desc
        );

        EditText number =
                new EditText(this);

        number.setTextColor(
                Color.WHITE
        );

        number.setTextSize(
                16
        );

        number.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        number.setSingleLine(
                true
        );

        number.setText(
                String.valueOf(
                        prefs.getInt(
                                key,
                                defaultValue
                        )
                )
        );

        numberFields.put(
                key,
                number
        );

        c.addView(
                number
        );

        content.addView(
                c
        );

        gap(
                content,
                10
        );
    }

    private void saveAll() {
        SharedPreferences.Editor editor =
                prefs.edit();

        for (Map.Entry<String, EditText> entry
                : textFields.entrySet()) {

            editor.putString(
                    entry.getKey(),
                    entry.getValue()
                            .getText()
                            .toString()
                            .trim()
            );
        }

        for (Map.Entry<String, CheckBox> entry
                : enabledFields.entrySet()) {

            editor.putBoolean(
                    MessageSettings.enabledKey(
                            entry.getKey()
                    ),
                    entry.getValue()
                            .isChecked()
            );
        }

        for (Map.Entry<String, EditText> entry
                : numberFields.entrySet()) {

            String value =
                    entry.getValue()
                            .getText()
                            .toString()
                            .trim();

            int seconds;

            try {
                seconds =
                        Integer.parseInt(
                                value
                        );

            } catch (Exception e) {
                entry.getValue()
                        .setError(
                                "Informe segundos usando somente números."
                        );

                return;
            }

            if (seconds < 0
                    || seconds > 3600) {

                entry.getValue()
                        .setError(
                                "Use um valor entre 0 e 3600 segundos."
                        );

                return;
            }

            editor.putInt(
                    entry.getKey(),
                    seconds
            );
        }

        if (masterflixExactCheck != null) {
            editor.putBoolean(MessageSettings.KEY_TEST_MATCH_EXACT, masterflixExactCheck.isChecked());
        }
        editor.apply();

        SharedPreferences.Editor stvEditor = stvPrefs.edit();
        for (Map.Entry<String, EditText> entry : stvTriggerFields.entrySet()) {
            String value = entry.getValue().getText().toString().trim();
            if (value.isEmpty()) {
                entry.getValue().setError("Cadastre pelo menos um gatilho STV+.");
                return;
            }
            stvEditor.putString(entry.getKey(), value);
        }
        for (Map.Entry<String, EditText> entry : stvTextFields.entrySet()) {
            stvEditor.putString(entry.getKey(), entry.getValue().getText().toString().trim());
        }
        for (Map.Entry<String, CheckBox> entry : stvEnabledFields.entrySet()) {
            stvEditor.putBoolean(MessageSettings.enabledKey(entry.getKey()), entry.getValue().isChecked());
        }
        if (stvExactCheck != null) {
            stvEditor.putBoolean(StvTriggerSettingsActivity.KEY_MATCH_EXACT, stvExactCheck.isChecked());
        }
        stvEditor.apply();

        Toast.makeText(
                this,
                "Mensagens e tempos salvos.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void restoreDefaults() {
        SharedPreferences.Editor editor =
                prefs.edit();

        for (String key
                : textFields.keySet()) {
            editor.remove(
                    key
            );
        }

        for (String key
                : enabledFields.keySet()) {
            editor.remove(
                    MessageSettings.enabledKey(
                            key
                    )
            );
        }

        for (String key
                : numberFields.keySet()) {
            editor.remove(
                    key
            );
        }

        editor.remove(MessageSettings.KEY_TEST_MATCH_EXACT);
        editor.apply();

        SharedPreferences.Editor stvRestore = stvPrefs.edit();
        stvRestore.remove(StvTriggerSettingsActivity.KEY_TRIGGERS);
        stvRestore.remove(StvTriggerSettingsActivity.KEY_MATCH_EXACT);
        for (String key : stvTriggerFields.keySet()) stvRestore.remove(key);
        for (String key : stvTextFields.keySet()) {
            stvRestore.remove(key);
            stvRestore.remove(MessageSettings.enabledKey(key));
        }
        stvRestore.apply();

        Toast.makeText(
                this,
                "Padrões restaurados.",
                Toast.LENGTH_SHORT
        ).show();

        recreate();
    }

    private LinearLayout card() {
        LinearLayout c =
                new LinearLayout(this);

        c.setOrientation(
                LinearLayout.VERTICAL
        );

        c.setPadding(
                dp(15),
                dp(13),
                dp(15),
                dp(13)
        );

        c.setBackgroundResource(
                getResources()
                        .getIdentifier(
                                "card",
                                "drawable",
                                getPackageName()
                        )
        );

        return c;
    }

    private Button actionButton(
            String label,
            boolean orange
    ) {
        Button b =
                new Button(this);

        b.setText(
                label
        );

        b.setTextColor(
                Color.WHITE
        );

        b.setTextSize(
                12
        );

        b.setTypeface(
                null,
                1
        );

        b.setBackgroundResource(
                getResources()
                        .getIdentifier(
                                orange
                                        ? "button_orange"
                                        : "button_dark",
                                "drawable",
                                getPackageName()
                        )
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

        b.setLayoutParams(
                lp
        );

        return b;
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView t =
                new TextView(this);

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
            LinearLayout parent,
            int height
    ) {
        View v =
                new View(this);

        parent.addView(
                v,
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
