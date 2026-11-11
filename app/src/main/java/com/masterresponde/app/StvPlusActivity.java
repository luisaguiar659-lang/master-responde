package com.masterresponde.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class StvPlusActivity extends Activity {
    private WebView web;
    private EditText url, user, pass;
    private TextView status;
    private RadioButton adult, standard;
    private boolean generationStarted;
    private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 9, 11));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button back = new Button(this); back.setText("←"); back.setOnClickListener(v -> finish());
        header.addView(back);
        TextView title = new TextView(this); title.setText("STV+ • PAINEL E TESTES"); title.setTextColor(Color.WHITE); title.setTextSize(19); title.setPadding(dp(10),0,0,0);
        header.addView(title); root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16),dp(12),dp(16),dp(20));
        content.addView(label("CONFIGURAÇÃO DO STV+"));
        url = field("URL base do painel"); url.setText(StvPanelConfig.getBaseUrl(this)); content.addView(url);
        user = field("Usuário do STV+"); user.setText(StvPanelConfig.getUser(this)); content.addView(user);
        pass = field("Senha do STV+"); pass.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD); pass.setText(StvPanelConfig.getPass(this)); content.addView(pass);
        Button save = button("SALVAR ACESSO STV+"); save.setOnClickListener(v -> {
            StvPanelConfig.saveBaseUrl(this, url.getText().toString());
            boolean ok = StvPanelConfig.saveCredentials(this, user.getText().toString(), pass.getText().toString());
            Toast.makeText(this, ok ? "Acesso STV+ salvo com proteção do Android." : "Não foi possível salvar as credenciais.", Toast.LENGTH_LONG).show();
        }); content.addView(save);

        content.addView(label("TIPO DE TESTE"));
        RadioGroup modes = new RadioGroup(this); modes.setOrientation(RadioGroup.VERTICAL);
        adult = new RadioButton(this); adult.setText("Com conteúdo adulto"); adult.setTextColor(Color.WHITE);
        standard = new RadioButton(this); standard.setText("Sem conteúdo adulto"); standard.setTextColor(Color.WHITE);
        modes.addView(adult); modes.addView(standard);
        String selected = StvPanelConfig.getMode(this);
        ( "adult".equals(selected) ? adult : standard).setChecked(true);
        modes.setOnCheckedChangeListener((g,id) -> StvPanelConfig.saveMode(this, id == adult.getId() ? "adult" : "standard"));
        content.addView(modes);
        content.addView(label("VALIDADE DO TESTE: 1 HORA"));

        Button open = button("ABRIR PAINEL STV+"); open.setOnClickListener(v -> {
            if (web == null) createPanel(content);
            web.setVisibility(android.view.View.VISIBLE);
            if (web.getUrl() == null) web.loadUrl(StvPanelConfig.getBaseUrl(this) + "/#/sign-in?redirect=/dashboard");
        }); content.addView(open);
        Button generate = button("GERAR TESTE STV+"); generate.setOnClickListener(v -> {
            if (web == null) { createPanel(content); web.setVisibility(android.view.View.VISIBLE); web.loadUrl(StvPanelConfig.getBaseUrl(this) + "/#/sign-in?redirect=/dashboard"); }
            else runGeneration();
        }); content.addView(generate);
        status = label("Aguardando ação."); content.addView(status);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void createPanel(LinearLayout parent) {
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String page) {
                String host = android.net.Uri.parse(page).getHost();
                if (host == null || !host.equals(android.net.Uri.parse(StvPanelConfig.getBaseUrl(StvPlusActivity.this)).getHost())) return;
                fillLogin(view);
            }
        });
        web.setVisibility(android.view.View.GONE);
        parent.addView(web, new LinearLayout.LayoutParams(-1, dp(360)));
    }

    private void fillLogin(WebView view) {
        String u = org.json.JSONObject.quote(StvPanelConfig.getUser(this));
        String p = org.json.JSONObject.quote(StvPanelConfig.getPass(this));
        String js = "(function(){const v=e=>!!(e.offsetWidth||e.offsetHeight||e.getClientRects().length);const p=document.querySelector('input[type=password]');const u=document.querySelector('input[type=email],input[autocomplete=username],input[type=text]');if(!p)return 'no-form';const s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;if(u&&!u.value){s.call(u,"+u+");u.dispatchEvent(new Event('input',{bubbles:true}));}if(!p.value){s.call(p,"+p+");p.dispatchEvent(new Event('input',{bubbles:true}));}return 'filled';})();";
        view.evaluateJavascript(js, result -> { if ("\"filled\"".equals(result)) status.setText("Credenciais preenchidas. Se houver verificação de segurança, conclua manualmente."); });
    }

    private void runGeneration() {
        if (web == null || generationStarted) return;
        String target = adult.isChecked() ? "TESTE + ADULTO" : "TESTE SEM ADULTO";
        String q = org.json.JSONObject.quote(target);
        String js = "(function(){const n=s=>(s||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').toUpperCase().replace(/[^A-Z0-9]+/g,'');const t=n("+q+");const e=Array.from(document.querySelectorAll('body *')).filter(x=>x.offsetWidth||x.offsetHeight||x.getClientRects().length).map(x=>({x,t:n(x.innerText||x.textContent||'')})).filter(x=>x.t===t||x.t.startsWith(t)).sort((a,b)=>a.t.length-b.t.length)[0];if(!e)return 'not-found';e.x.scrollIntoView({block:'center'});e.x.click();return 'clicked';})();";
        web.evaluateJavascript(js, result -> {
            if ("\"clicked\"".equals(result)) {
                generationStarted = true; status.setText("Teste acionado. Aguardando resultado para copiar.");
                handler.postDelayed(this::copyResult, 3500L);
            } else status.setText("Opção " + target + " não encontrada no painel.");
        });
    }

    private void copyResult() {
        if (web == null) return;
        String js = "(function(){window.scrollTo(0,document.documentElement.scrollHeight);const b=Array.from(document.querySelectorAll('button,a,[role=button]')).find(e=>(e.innerText||'').trim().toUpperCase()==='COPIAR');if(!b)return 'not-found';b.click();return 'clicked';})();";
        web.evaluateJavascript(js, result -> {
            if ("\"clicked\"".equals(result)) status.setText("Botão COPIAR acionado. Aguardando área de transferência.");
            else status.setText("Botão COPIAR não encontrado.");
        });
    }

    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); if (web != null) { web.stopLoading(); web.destroy(); } super.onDestroy(); }
    private TextView label(String s) { TextView t = new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(14); t.setPadding(0,dp(12),0,dp(8)); return t; }
    private EditText field(String hint) { EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.LTGRAY); return e; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density); }
}
