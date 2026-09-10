package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class MasterIboSettingsActivity extends Activity {
    private EditText emailInput, passwordInput;
    private SharedPreferences securePrefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try { securePrefs = securePreferences(); }
        catch (Exception e) {
            Toast.makeText(this, "Falha ao abrir armazenamento seguro do MASTER IBO.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        buildUi();
        emailInput.setText(securePrefs.getString("email", ""));
        passwordInput.setText(securePrefs.getString("password", ""));
    }

    private SharedPreferences securePreferences() throws Exception {
        MasterKey key = new MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(this, "master_ibo_secure", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(7, 9, 11));

        TextView title = label("MASTER IBO", 24); root.addView(title);
        TextView info = label("Credenciais do painel gerenciaapp.top usadas pelo Motor MASTER IBO.", 14);
        info.setTextColor(Color.rgb(170,180,190)); info.setPadding(0,dp(8),0,dp(22)); root.addView(info);

        emailInput = field("E-mail do painel");
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(emailInput);
        passwordInput = field("Senha");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput);

        Button save = new Button(this); save.setText("SALVAR CREDENCIAIS"); save.setAllCaps(false);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54)); bp.topMargin=dp(18); root.addView(save,bp);

        TextView hint = label("Depois de salvar, envie \"ativar ibo\" no WhatsApp Business. O fluxo coleta aplicativo, MAC e M3U e executa a ativação.", 13);
        hint.setTextColor(Color.rgb(140,150,160)); hint.setPadding(0,dp(18),0,0); root.addView(hint);
        scroll.addView(root); setContentView(scroll);
    }

    private EditText field(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(115,125,135)); e.setPadding(dp(14),0,dp(14),0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(54)); lp.bottomMargin=dp(12); e.setLayoutParams(lp); return e;
    }
    private TextView label(String text,int size) { TextView v=new TextView(this); v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(size); return v; }

    private void save() {
        String email=emailInput.getText().toString().trim(), password=passwordInput.getText().toString();
        if(email.isEmpty()){ emailInput.setError("Informe o e-mail."); return; }
        if(password.isEmpty()){ passwordInput.setError("Informe a senha."); return; }
        securePrefs.edit().putString("email",email).putString("password",password).apply();
        Toast.makeText(this,"MASTER IBO configurado.",Toast.LENGTH_SHORT).show();
    }
    private int dp(int value){ return Math.round(value*getResources().getDisplayMetrics().density); }
}
