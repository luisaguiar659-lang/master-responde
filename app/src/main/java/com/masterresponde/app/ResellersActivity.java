package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ResellersActivity extends Activity {

    public static final String DEFAULT_SIGNUP_LINK =
            "https://masterflix.sigmab.pro/#/rs/en12xeNDPE/ryJDz2KDge";

    public static final String DEFAULT_SUPPORT_GROUP_LINK =
            "https://chat.whatsapp.com/IWHogXDJTem2VgJTJhsTA0?s=cl&p=a&mlu=0&ilr=1";

    private SharedPreferences prefs;
    private EditText linkField;
    private EditText supportGroupField;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "master_responde",
                MODE_PRIVATE
        );

        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 9, 11));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(Color.WHITE);
        back.setTextSize(24);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(54)
                )
        );

        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);

        title.addView(
                text(
                        "REVENDAS",
                        21,
                        Color.WHITE,
                        true
                )
        );

        title.addView(
                text(
                        "Cadastro e confirmação da revenda",
                        12,
                        Color.rgb(150, 160, 170),
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

        root.addView(header);

        View line = new View(this);
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

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(40)
        );

        LinearLayout config = card();

        config.addView(
                text(
                        "LINK DE CADASTRO",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        linkField = new EditText(this);
        linkField.setTextColor(Color.WHITE);
        linkField.setHintTextColor(
                Color.rgb(130, 140, 150)
        );
        linkField.setTextSize(13);
        linkField.setSingleLine(false);
        linkField.setMinLines(2);

        linkField.setText(
                prefs.getString(
                        "reseller_signup_link",
                        DEFAULT_SIGNUP_LINK
                )
        );

        config.addView(linkField);

        Button save = actionButton(
                "SALVAR LINK",
                true
        );

        save.setOnClickListener(
                v -> saveLink()
        );

        config.addView(save);

        content.addView(config);

        gap(content, 12);

        LinearLayout supportCard = card();

        supportCard.addView(
                text(
                        "GRUPO DE SUPORTE",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        supportGroupField = new EditText(this);
        supportGroupField.setTextColor(Color.WHITE);
        supportGroupField.setHintTextColor(
                Color.rgb(130, 140, 150)
        );
        supportGroupField.setTextSize(13);
        supportGroupField.setSingleLine(false);
        supportGroupField.setMinLines(2);

        supportGroupField.setText(
                prefs.getString(
                        "reseller_support_group_link",
                        DEFAULT_SUPPORT_GROUP_LINK
                )
        );

        supportCard.addView(
                supportGroupField
        );

        Button saveSupportGroup = actionButton(
                "SALVAR GRUPO DE SUPORTE",
                true
        );

        saveSupportGroup.setOnClickListener(
                v -> saveSupportGroupLink()
        );

        supportCard.addView(
                saveSupportGroup
        );

        supportCard.addView(
                text(
                        "Esse link é enviado automaticamente depois que o e-mail for confirmado como revenda no painel Sigma ativo. O texto da mensagem pode ser alterado em MENSAGENS E TEMPOS.",
                        12,
                        Color.rgb(150, 160, 170),
                        false
                )
        );

        content.addView(
                supportCard
        );

        gap(content, 12);

        LinearLayout credit = card();

        credit.addView(
                text(
                        "ATIVAÇÃO PELO SUPORTE",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        credit.addView(
                text(
                        "Verificar cadastro e enviar grupo",
                        20,
                        Color.WHITE,
                        true
                )
        );

        credit.addView(
                text(
                        "O MASTER RESPONDE apenas confirma se o e-mail está cadastrado como revenda. Depois envia o grupo de suporte para o cliente solicitar a ativação do painel.",
                        12,
                        Color.rgb(150, 160, 170),
                        false
                )
        );

        content.addView(credit);

        gap(content, 12);

        LinearLayout flow = card();

        flow.addView(
                text(
                        "FLUXO AUTOMÁTICO",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        flow.addView(
                text(
                        "1. Cliente envia “Quero ser Revenda”.\n" +
                        "2. Bot envia o link de cadastro.\n" +
                        "3. Cliente conclui o cadastro e envia o e-mail usado.\n" +
                        "4. Bot verifica se o e-mail existe como revenda no painel Sigma ativo.\n" +
                        "5. Se estiver cadastrado, envia o link do grupo de suporte.\n" +
                        "6. Cliente entra no grupo e solicita a ativação do painel.",
                        13,
                        Color.rgb(205, 215, 220),
                        false
                )
        );

        content.addView(flow);

        gap(content, 12);

        LinearLayout last = card();

        last.addView(
                text(
                        "ÚLTIMO STATUS",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        status = text(
                "Nenhuma operação de revenda realizada.",
                13,
                Color.rgb(0, 184, 255),
                true
        );

        status.setPadding(
                0,
                dp(8),
                0,
                0
        );

        last.addView(status);
        content.addView(last);

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        setContentView(root);

        refreshStatus();
    }

    private void saveLink() {
        String value =
                linkField.getText()
                        .toString()
                        .trim();

        if (value.isEmpty()
                || !value.startsWith("https://")) {

            linkField.setError(
                    "Informe um link https:// válido."
            );

            return;
        }

        prefs.edit()
                .putString(
                        "reseller_signup_link",
                        value
                )
                .apply();

        Toast.makeText(
                this,
                "Link de cadastro salvo.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void saveSupportGroupLink() {
        String value =
                supportGroupField.getText()
                        .toString()
                        .trim();

        if (value.isEmpty()
                || !value.startsWith("https://")) {

            supportGroupField.setError(
                    "Informe um link https:// válido."
            );

            return;
        }

        prefs.edit()
                .putString(
                        "reseller_support_group_link",
                        value
                )
                .apply();

        Toast.makeText(
                this,
                "Grupo de suporte salvo.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void refreshStatus() {
        if (status == null) return;

        status.setText(
                prefs.getString(
                        "last_reseller_status",
                        "Nenhuma operação de revenda realizada."
                )
        );
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(15), dp(15), dp(15));
        c.setBackgroundResource(
                getResources().getIdentifier(
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
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(null, 1);
        b.setBackgroundResource(
                getResources().getIdentifier(
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
                        dp(52)
                );

        lp.setMargins(
                0,
                dp(6),
                0,
                0
        );

        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(null, 1);
        }

        return t;
    }

    private void gap(
            LinearLayout parent,
            int height
    ) {
        View v = new View(this);

        parent.addView(
                v,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
