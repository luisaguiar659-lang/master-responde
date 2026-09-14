package com.masterresponde.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MasterflixActivity extends Activity {

    private SharedPreferences prefs;
    private SecureStore secureStore;

    private EditText userField;
    private EditText passField;
    private TextView sessionStatus;
    private TextView generationStatus;
    private WebView webView;
    private boolean fullScreenPanelMode = false;

    private final Handler automationHandler =
            new Handler(Looper.getMainLooper());

    private boolean autoGenerateRequested = false;
    private boolean generationRunning = false;
    private int findProductAttempts = 0;
    private int findCopyAttempts = 0;
    private int clipboardAttempts = 0;
    private int copyScrollAttempts = 0;
    private boolean loginAssistAttempted = false;
    private volatile String capturedCopyText = "";

    private boolean autoResellerRequested = false;
    private boolean visibleResellerRunning = false;
    private boolean resellerRouteRequested = false;
    private boolean resellerSubmitAttempted = false;

    private String resellerEmail = "";
    private String resellerContactKey = "";

    private int resellerSearchAttempts = 0;
    private int resellerCardAttempts = 0;
    private int resellerCreditAttempts = 0;
    private int resellerAgreementAttempts = 0;
    private int resellerSubmitAttempts = 0;
    private int resellerCompletionAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "master_responde",
                MODE_PRIVATE
        );

        secureStore = new SecureStore(this);

        autoGenerateRequested =
                (getIntent() != null
                        && getIntent().getBooleanExtra(
                                "auto_generate_test",
                                false
                        ))
                        || TestTaskBridge.hasPending();

        autoResellerRequested =
                (getIntent() != null
                        && getIntent().getBooleanExtra(
                                "auto_reseller_credit",
                                false
                        ))
                        || ResellerTaskBridge.hasPending();

        if (autoResellerRequested) {
            prefs.edit()
                    .putBoolean(
                            "reseller_visible_activity_started",
                            true
                    )
                    .apply();

            resellerEmail =
                    getIntent() != null
                            ? getIntent().getStringExtra(
                                    "reseller_email"
                            )
                            : "";

            resellerContactKey =
                    getIntent() != null
                            ? getIntent().getStringExtra(
                                    "reseller_contact_key"
                            )
                            : "";

            if (resellerEmail == null
                    || resellerEmail.trim().isEmpty()) {
                resellerEmail =
                        ResellerTaskBridge.getEmail();
            }

            if (resellerContactKey == null
                    || resellerContactKey.trim().isEmpty()) {
                resellerContactKey =
                        ResellerTaskBridge.getContactKey();
            }

            if (resellerEmail == null) resellerEmail = "";
            if (resellerContactKey == null) resellerContactKey = "";

            resellerEmail =
                    resellerEmail.trim().toLowerCase(
                            java.util.Locale.ROOT
                    );
        }

        buildScreen();
        loadSavedCredentials();
        updateSessionStatus();

        if (autoGenerateRequested) {
            updateGenerationStatus(
                    "Preparando geração automática..."
            );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null
                && intent.getBooleanExtra(
                        "auto_generate_test",
                        false
                )) {

            autoGenerateRequested = true;
            generationRunning = false;
            findProductAttempts = 0;
            findCopyAttempts = 0;
            clipboardAttempts = 0;

            updateGenerationStatus(
                    "Preparando geração automática..."
            );

            openDashboard();
        }

        if (intent != null
                && intent.getBooleanExtra(
                        "auto_reseller_credit",
                        false
                )) {

            autoResellerRequested = true;
            visibleResellerRunning = false;
            resellerRouteRequested = false;
            resellerSubmitAttempted = false;

            resellerEmail =
                    intent.getStringExtra(
                            "reseller_email"
                    );

            resellerContactKey =
                    intent.getStringExtra(
                            "reseller_contact_key"
                    );

            if (resellerEmail == null
                    || resellerEmail.trim().isEmpty()) {
                resellerEmail =
                        ResellerTaskBridge.getEmail();
            }

            if (resellerContactKey == null
                    || resellerContactKey.trim().isEmpty()) {
                resellerContactKey =
                        ResellerTaskBridge.getContactKey();
            }

            if (resellerEmail == null) resellerEmail = "";
            if (resellerContactKey == null) resellerContactKey = "";

            resellerEmail =
                    resellerEmail.trim().toLowerCase(
                            java.util.Locale.ROOT
                    );

            openDashboard();
        }
    }

    @Override
    protected void onDestroy() {
        if (autoResellerRequested
                || visibleResellerRunning) {

            prefs.edit()
                    .putBoolean(
                            "reseller_visible_activity_started",
                            false
                    )
                    .apply();
        }

        if (webView != null) {
            webView.removeJavascriptInterface("MasterRespondeBridge");
            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
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

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(
                text(
                        SigmaPanelConfig.getPanelName(this),
                        21,
                        Color.WHITE,
                        true
                )
        );
        titleBox.addView(
                text(
                        "Login e sessão do painel Sigma",
                        12,
                        Color.rgb(150, 160, 170),
                        false
                )
        );

        header.addView(
                titleBox,
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
                dp(30)
        );

        LinearLayout statusCard = card();

        statusCard.addView(
                text(
                        "SESSÃO",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        sessionStatus = text(
                "● Login necessário",
                16,
                Color.rgb(255, 145, 0),
                true
        );

        sessionStatus.setPadding(
                0,
                dp(8),
                0,
                0
        );

        statusCard.addView(sessionStatus);
        content.addView(statusCard);

        gap(content, 12);

        LinearLayout loginCard = card();

        loginCard.addView(
                text(
                        "CREDENCIAIS",
                        11,
                        Color.rgb(255, 150, 0),
                        true
                )
        );

        userField = field(
                "Usuário ou E-mail"
        );

        loginCard.addView(userField);

        passField = field(
                "Senha"
        );

        passField.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        loginCard.addView(passField);

        Button save = actionButton(
                "SALVAR CREDENCIAIS",
                true
        );

        save.setOnClickListener(
                v -> saveCredentials()
        );

        loginCard.addView(save);

        Button fill = actionButton(
                "PREENCHER LOGIN SALVO",
                false
        );

        fill.setOnClickListener(
                v -> fillSavedLogin()
        );

        loginCard.addView(fill);

        Button clearSession = actionButton(
                "ENCERRAR SESSÃO",
                false
        );

        clearSession.setOnClickListener(
                v -> confirmClearSession()
        );

        loginCard.addView(clearSession);

        content.addView(loginCard);

        gap(content, 12);

        TextView notice = text(
                "A v1.17 salva usuário e senha criptografados para o painel Sigma ativo. " +
                "Se a sessão expirar, o MASTER RESPONDE tenta preencher o login salvo automaticamente. Ele NÃO marca " +
                "“Verificado”, não resolve CAPTCHA e não tenta contornar proteções do painel. " +
                "Quando essa verificação aparecer, conclua manualmente e toque em Continuar.",
                12,
                Color.rgb(150, 160, 170),
                false
        );

        content.addView(notice);

        gap(content, 12);

        LinearLayout browserCard = card();

        LinearLayout browserHeader = new LinearLayout(this);
        browserHeader.setOrientation(LinearLayout.HORIZONTAL);
        browserHeader.setGravity(Gravity.CENTER_VERTICAL);

        browserHeader.addView(
                text(
                        "PAINEL " + SigmaPanelConfig.getPanelName(this),
                        12,
                        Color.WHITE,
                        true
                ),
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Button loginButton = smallButton("LOGIN");
        loginButton.setOnClickListener(
                v -> openLogin()
        );

        browserHeader.addView(loginButton);

        Button dashboardButton = smallButton("PAINEL");
        dashboardButton.setOnClickListener(
                v -> openDashboard()
        );

        browserHeader.addView(dashboardButton);

        browserCard.addView(browserHeader);

        generationStatus = text(
                "Teste: aguardando comando",
                12,
                Color.rgb(0, 184, 255),
                true
        );

        generationStatus.setPadding(
                0,
                dp(10),
                0,
                dp(6)
        );

        browserCard.addView(generationStatus);

        Button generateButton = actionButton(
                "GERAR TESTE",
                true
        );

        generateButton.setOnClickListener(
                v -> requestGenerateTest()
        );

        browserCard.addView(generateButton);

        webView = new WebView(this);

        LinearLayout.LayoutParams webLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(500)
                );

        webLp.setMargins(
                0,
                dp(10),
                0,
                0
        );

        webView.setLayoutParams(webLp);

        // O MASTERFLIX fica dentro de um ScrollView do app.
        // Sem isso o ScrollView externo pode "roubar" o gesto e
        // impedir a rolagem do próprio painel.
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        webView.setNestedScrollingEnabled(true);

        webView.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();

            if (action == android.view.MotionEvent.ACTION_DOWN
                    || action == android.view.MotionEvent.ACTION_MOVE) {

                view.getParent()
                        .requestDisallowInterceptTouchEvent(true);

            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {

                view.getParent()
                        .requestDisallowInterceptTouchEvent(false);
            }

            return false;
        });

        configureWebView();

        browserCard.addView(webView);

        content.addView(browserCard);

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

        openDashboard();
    }

    private void configureWebView() {
        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        webView.addJavascriptInterface(
                new MasterRespondeBridge(),
                "MasterRespondeBridge"
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        super.onPageFinished(
                                view,
                                url
                        );

                        inspectCurrentLocation();
                        maybeStartAutomation();
                    }

                    @Override
                    public void doUpdateVisitedHistory(
                            WebView view,
                            String url,
                            boolean isReload
                    ) {
                        super.doUpdateVisitedHistory(
                                view,
                                url,
                                isReload
                        );

                        inspectCurrentLocation();
                        maybeStartAutomation();
                    }
                }
        );
    }

    private void inspectCurrentLocation() {
        if (webView == null) return;

        webView.evaluateJavascript(
                "(function(){try{" +
                        "return JSON.stringify({" +
                        "href:location.href," +
                        "path:location.hash||''" +
                        "});" +
                        "}catch(e){return ''}})();",
                value -> {
                    if (value == null) return;

                    String decoded = decodeJavascriptString(
                            value
                    );

                    if (decoded.isEmpty()) return;

                    try {
                        JSONObject object =
                                new JSONObject(decoded);

                        String href =
                                object.optString(
                                        "href",
                                        ""
                                );

                        updateSessionFromUrl(
                                href
                        );

                    } catch (Exception ignored) {
                    }
                }
        );
    }

    private void updateSessionFromUrl(
            String url
    ) {
        if (url == null) return;

        if (url.contains("#/dashboard")
                || url.contains("#/resellers")) {

            prefs.edit()
                    .putBoolean(
                            "masterflix_session_active",
                            true
                    )
                    .apply();

            runOnUiThread(
                    () -> {
                        updateSessionStatus();

                        // v1.16: mantém o painel MASTERFLIX dentro do card,
                        // como nas versões clássicas. Não troca a tela inteira
                        // pelo WebView quando a sessão fica ativa.
                        if (autoResellerRequested) {
                            if (url.contains("#/dashboard")
                                    && !resellerRouteRequested) {

                                resellerRouteRequested =
                                        true;

                                automationHandler.postDelayed(
                                        () -> {
                                            if (webView != null
                                                    && autoResellerRequested) {
                                                webView.loadUrl(
                                                        SigmaPanelConfig.resellersUrl(MasterflixActivity.this)
                                                );
                                            }
                                        },
                                        350L
                                );

                            } else if (url.contains("#/resellers")) {

                                automationHandler.postDelayed(
                                        this::startVisibleResellerAutomation,
                                        550L
                                );
                            }
                        }
                    }
            );

        } else if (url.contains("#/sign-in")) {

            prefs.edit()
                    .putBoolean(
                            "masterflix_session_active",
                            false
                    )
                    .apply();

            runOnUiThread(
                    () -> {
                        updateSessionStatus();

                        if (prefs.getBoolean(
                                "masterflix_credentials_saved",
                                false
                        )
                                && !loginAssistAttempted) {

                            loginAssistAttempted =
                                    true;

                            automationHandler.postDelayed(
                                    this::fillSavedLogin,
                                    450L
                            );
                        }
                    }
            );
        }
    }

    private void enterFullScreenPanel() {
        if (fullScreenPanelMode
                || webView == null) {
            return;
        }

        try {
            android.view.ViewParent parent =
                    webView.getParent();

            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(
                        webView
                );
            }

            webView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            webView.setBackgroundColor(
                    Color.WHITE
            );

            fullScreenPanelMode =
                    true;

            setContentView(
                    webView
            );

        } catch (Exception ignored) {
        }
    }

    private void openLogin() {
        webView.loadUrl(SigmaPanelConfig.loginUrl(this));
    }

    private void openDashboard() {
        webView.loadUrl(SigmaPanelConfig.dashboardUrl(this));
    }

    private void startVisibleResellerAutomation() {
        if (!autoResellerRequested
                || visibleResellerRunning
                || webView == null) {
            return;
        }

        if (resellerEmail == null
                || resellerEmail.trim().isEmpty()
                || resellerContactKey == null
                || resellerContactKey.trim().isEmpty()) {

            failVisibleReseller(
                    "Não encontrei os dados pendentes da revenda.",
                    false
            );

            return;
        }

        visibleResellerRunning =
                true;

        resellerSearchAttempts = 0;
        resellerCardAttempts = 0;
        resellerCreditAttempts = 0;
        resellerAgreementAttempts = 0;
        resellerSubmitAttempts = 0;
        resellerCompletionAttempts = 0;
        resellerSubmitAttempted = false;

        prefs.edit()
                .putString(
                        "last_reseller_status",
                        "Revenda: pesquisando e-mail na tela original"
                )
                .apply();

        automationHandler.postDelayed(
                this::searchResellerEmailVisible,
                400L
        );
    }

    private void searchResellerEmailVisible() {
        if (!visibleResellerRunning
                || webView == null) {
            return;
        }

        resellerSearchAttempts++;

        String wanted =
                JSONObject.quote(
                        resellerEmail
                );

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "function setValue(el,v){" +
                        "try{" +
                        "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,v);" +
                        "}catch(e){el.value=v;}" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true,key:'a'}));" +
                        "}" +
                        "var inputs=document.querySelectorAll('input');" +
                        "var search=null;" +
                        "for(var i=0;i<inputs.length;i++){" +
                        "var el=inputs[i];" +
                        "var ph=n((el.placeholder||'')+' '+(el.getAttribute('aria-label')||''));" +
                        "var type=(el.type||'').toLowerCase();" +
                        "if(type==='search'||ph.indexOf('pesquisar')!==-1||ph.indexOf('buscar')!==-1||ph.indexOf('search')!==-1){" +
                        "search=el;break;" +
                        "}" +
                        "}" +
                        "if(!search)return 'no_search';" +
                        "setValue(search," + wanted + ");" +
                        "try{search.focus();}catch(e){}" +
                        "return 'ok';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("ok".equals(result)) {
                        prefs.edit()
                                .putString(
                                        "last_reseller_status",
                                        "Revenda: e-mail pesquisado na tela original"
                                )
                                .apply();

                        automationHandler.postDelayed(
                                this::openAddCreditsFromVisibleCard,
                                650L
                        );

                        return;
                    }

                    if (resellerSearchAttempts < 25) {
                        automationHandler.postDelayed(
                                this::searchResellerEmailVisible,
                                350L
                        );
                    } else {
                        failVisibleReseller(
                                "Não encontrei o campo Pesquisar na página de Revendas.",
                                false
                        );
                    }
                }
        );
    }

    private void openAddCreditsFromVisibleCard() {
        if (!visibleResellerRunning
                || webView == null) {
            return;
        }

        resellerCardAttempts++;

        String wanted =
                JSONObject.quote(
                        resellerEmail
                );

        String script =
                "(function(){" +
                        "var wanted=" + wanted + ".toLowerCase();" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var emailEl=null;" +
                        "var best=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var t=n(all[i].innerText||all[i].textContent);" +
                        "if(t===wanted&&t.length<best){emailEl=all[i];best=t.length;}" +
                        "}" +
                        "if(!emailEl)return 'no_email';" +
                        "var card=null;" +
                        "var p=emailEl;" +
                        "for(var d=0;d<14&&p;d++,p=p.parentElement){" +
                        "var txt=n(p.innerText||p.textContent);" +
                        "if(txt.indexOf(wanted)!==-1" +
                        "&&(txt.indexOf('adicionar créditos')!==-1||txt.indexOf('adicionar creditos')!==-1)){" +
                        "card=p;break;" +
                        "}" +
                        "}" +
                        "if(!card)return 'no_card';" +
                        "var nodes=card.querySelectorAll('button,a,[role=button],*');" +
                        "var label=null;" +
                        "var labelBest=999999;" +
                        "for(var j=0;j<nodes.length;j++){" +
                        "var t=n(nodes[j].innerText||nodes[j].textContent);" +
                        "if((t==='adicionar créditos'||t==='adicionar creditos')&&t.length<labelBest){" +
                        "label=nodes[j];labelBest=t.length;" +
                        "}" +
                        "}" +
                        "if(!label)return 'no_add';" +
                        "var clickable=label.closest('button,a,[role=button],[tabindex]');" +
                        "if(!clickable){" +
                        "var q=label.parentElement;" +
                        "for(var x=0;x<6&&q&&!clickable;x++,q=q.parentElement){" +
                        "var bs=q.querySelectorAll('button,a,[role=button],[tabindex]');" +
                        "if(bs.length===1){clickable=bs[0];break;}" +
                        "}" +
                        "}" +
                        "if(!clickable)clickable=label.parentElement||label;" +
                        "try{clickable.scrollIntoView({behavior:'auto',block:'center'});}catch(e){}" +
                        "try{clickable.click();return 'clicked';}catch(e){" +
                        "try{clickable.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window}));return 'clicked';}" +
                        "catch(e2){return 'error';}" +
                        "}" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("clicked".equals(result)) {
                        prefs.edit()
                                .putString(
                                        "last_reseller_status",
                                        "Revenda: Adicionar Créditos aberto na tela original"
                                )
                                .apply();

                        automationHandler.postDelayed(
                                this::fillVisibleCreditField,
                                500L
                        );

                        return;
                    }

                    if (resellerCardAttempts < 30) {
                        automationHandler.postDelayed(
                                this::openAddCreditsFromVisibleCard,
                                400L
                        );
                    } else {
                        failVisibleReseller(
                                "Não encontrei Adicionar Créditos dentro do cartão dessa revenda.",
                                false
                        );
                    }
                }
        );
    }

    private void fillVisibleCreditField() {
        if (!visibleResellerRunning
                || webView == null) {
            return;
        }

        resellerCreditAttempts++;

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "function setValue(el,v){" +
                        "try{" +
                        "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,v);" +
                        "}catch(e){el.value=v;}" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true,key:'0'}));" +
                        "}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var anchor=null;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var t=n(all[i].innerText||all[i].textContent);" +
                        "if(t.indexOf('não remover')!==-1||t.indexOf('nao remover')!==-1){" +
                        "anchor=all[i];break;" +
                        "}" +
                        "}" +
                        "if(!anchor)return 'no_screen';" +
                        "var form=null;" +
                        "var p=anchor;" +
                        "for(var d=0;d<12&&p;d++,p=p.parentElement){" +
                        "var txt=n(p.innerText||p.textContent);" +
                        "if(txt.indexOf('use casas decimais')!==-1&&txt.indexOf('eu concordo em transferir')!==-1){" +
                        "form=p;break;" +
                        "}" +
                        "}" +
                        "if(!form)return 'no_form';" +
                        "var buttons=form.querySelectorAll('button,[role=button]');" +
                        "var minus=null,plus=null;" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var bt=n(buttons[j].innerText||buttons[j].textContent);" +
                        "if(!minus&&(bt==='-'||bt==='−'))minus=buttons[j];" +
                        "if(!plus&&(bt==='+'||bt==='＋'))plus=buttons[j];" +
                        "}" +
                        "if(!minus||!plus)return 'no_stepper';" +
                        "var stepper=null;" +
                        "var mp=minus.parentElement;" +
                        "for(var x=0;x<7&&mp&&!stepper;x++,mp=mp.parentElement){" +
                        "if(mp.contains(plus)){stepper=mp;break;}" +
                        "}" +
                        "if(!stepper)return 'no_stepper';" +
                        "var input=stepper.querySelector('input:not([type=checkbox]):not([type=radio]),textarea,[role=spinbutton]');" +
                        "if(!input)return 'no_input';" +
                        "setValue(input,'50');" +
                        "var val=('value' in input)?String(input.value||''):String(input.innerText||'');" +
                        "val=val.replace(/\\s+/g,'').replace(',','.');" +
                        "if(val!=='50'&&val!=='50.0'&&val!=='50.00')return 'not_50';" +
                        "return 'ok';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("ok".equals(result)) {
                        prefs.edit()
                                .putString(
                                        "last_reseller_status",
                                        "Revenda: 50 preenchido no controle − campo +"
                                )
                                .apply();

                        resellerAgreementAttempts = 0;

                        automationHandler.postDelayed(
                                this::markVisibleTransferAgreement,
                                350L
                        );

                        return;
                    }

                    if (resellerCreditAttempts < 30) {
                        automationHandler.postDelayed(
                                this::fillVisibleCreditField,
                                350L
                        );
                    } else {
                        failVisibleReseller(
                                "Não consegui preencher 50 no campo entre − e +.",
                                false
                        );
                    }
                }
        );
    }

    private void markVisibleTransferAgreement() {
        if (!visibleResellerRunning
                || webView == null) {
            return;
        }

        resellerAgreementAttempts++;

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var agreement=null;" +
                        "var best=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var t=n(all[i].innerText||all[i].textContent);" +
                        "if((t.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||t.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&t.length<best){agreement=all[i];best=t.length;}" +
                        "}" +
                        "if(!agreement)return 'waiting';" +
                        "var checkbox=null;" +
                        "try{" +
                        "var id=agreement.getAttribute('for');" +
                        "if(id)checkbox=document.getElementById(id);" +
                        "}catch(e){}" +
                        "if(!checkbox){" +
                        "var p=agreement;" +
                        "for(var d=0;d<6&&p&&!checkbox;d++,p=p.parentElement){" +
                        "var txt=n(p.innerText||p.textContent);" +
                        "if(txt.indexOf('eu concordo em transferir 50')===-1)continue;" +
                        "checkbox=p.querySelector('input[type=checkbox],[role=checkbox]');" +
                        "}" +
                        "}" +
                        "if(!checkbox)return 'no_checkbox';" +
                        "var type=(checkbox.type||'').toLowerCase();" +
                        "if(type==='checkbox'){" +
                        "if(!checkbox.checked)checkbox.click();" +
                        "return checkbox.checked?'ok':'failed';" +
                        "}" +
                        "if(checkbox.getAttribute('role')==='checkbox'){" +
                        "if(checkbox.getAttribute('aria-checked')!=='true')checkbox.click();" +
                        "return checkbox.getAttribute('aria-checked')==='true'?'ok':'failed';" +
                        "}" +
                        "return 'failed';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("ok".equals(result)) {
                        prefs.edit()
                                .putString(
                                        "last_reseller_status",
                                        "Revenda: autorização de 50 marcada"
                                )
                                .apply();

                        resellerSubmitAttempts = 0;

                        automationHandler.postDelayed(
                                this::clickVisibleFinalAdd,
                                350L
                        );

                        return;
                    }

                    if (resellerAgreementAttempts < 25) {
                        automationHandler.postDelayed(
                                this::markVisibleTransferAgreement,
                                300L
                        );
                    } else {
                        failVisibleReseller(
                                "Não consegui marcar “Eu concordo em transferir 50 créditos”.",
                                false
                        );
                    }
                }
        );
    }

    private void clickVisibleFinalAdd() {
        if (!visibleResellerRunning
                || webView == null
                || resellerSubmitAttempted) {
            return;
        }

        resellerSubmitAttempts++;

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var agreement=null;" +
                        "var best=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var t=n(all[i].innerText||all[i].textContent);" +
                        "if((t.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||t.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&t.length<best){agreement=all[i];best=t.length;}" +
                        "}" +
                        "if(!agreement)return 'not_ready';" +
                        "var p=agreement;" +
                        "var button=null;" +
                        "for(var d=0;d<8&&p&&!button;d++,p=p.parentElement){" +
                        "var buttons=p.querySelectorAll('button,a,[role=button]');" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var bt=n(buttons[j].innerText||buttons[j].textContent);" +
                        "if(bt.indexOf('adicionar')===0" +
                        "&&bt.indexOf('créditos')===-1" +
                        "&&bt.indexOf('creditos')===-1){" +
                        "button=buttons[j];break;" +
                        "}" +
                        "}" +
                        "}" +
                        "if(!button)return 'not_ready';" +
                        "if(button.disabled||button.getAttribute('aria-disabled')==='true'||button.hasAttribute('disabled'))return 'disabled';" +
                        "return 'ready';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("ready".equals(result)) {
                        armVisibleResellerSubmit();
                        return;
                    }

                    if (resellerSubmitAttempts < 24) {
                        automationHandler.postDelayed(
                                this::clickVisibleFinalAdd,
                                300L
                        );
                    } else {
                        failVisibleReseller(
                                "O botão final Adicionar não ficou disponível.",
                                false
                        );
                    }
                }
        );
    }

    private void armVisibleResellerSubmit() {
        if (!visibleResellerRunning
                || webView == null
                || resellerSubmitAttempted) {
            return;
        }

        resellerSubmitAttempted =
                true;

        prefs.edit()
                .putBoolean(
                        "reseller_submit_attempted_" + resellerContactKey,
                        true
                )
                .putLong(
                        "reseller_submit_time_" + resellerContactKey,
                        System.currentTimeMillis()
                )
                .putString(
                        "reseller_submit_email_" + resellerContactKey,
                        resellerEmail
                )
                .commit();

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var agreement=null;" +
                        "var best=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var t=n(all[i].innerText||all[i].textContent);" +
                        "if((t.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||t.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&t.length<best){agreement=all[i];best=t.length;}" +
                        "}" +
                        "if(!agreement)return 'missing';" +
                        "var p=agreement;" +
                        "var button=null;" +
                        "for(var d=0;d<8&&p&&!button;d++,p=p.parentElement){" +
                        "var buttons=p.querySelectorAll('button,a,[role=button]');" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var bt=n(buttons[j].innerText||buttons[j].textContent);" +
                        "if(bt.indexOf('adicionar')===0" +
                        "&&bt.indexOf('créditos')===-1" +
                        "&&bt.indexOf('creditos')===-1){" +
                        "button=buttons[j];break;" +
                        "}" +
                        "}" +
                        "}" +
                        "if(!button)return 'missing';" +
                        "try{button.click();return 'clicked';}catch(e){" +
                        "try{button.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window}));return 'clicked';}" +
                        "catch(e2){return 'error';}" +
                        "}" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if (!"clicked".equals(result)) {
                        failVisibleReseller(
                                "A operação foi reservada, mas não consegui confirmar o clique final.",
                                true
                        );

                        return;
                    }

                    prefs.edit()
                            .putString(
                                    "last_reseller_status",
                                    "Revenda: Adicionar acionado uma única vez"
                            )
                            .apply();

                    resellerCompletionAttempts = 0;

                    automationHandler.postDelayed(
                            this::waitVisibleResellerCompletion,
                            650L
                    );
                }
        );
    }

    private void waitVisibleResellerCompletion() {
        if (!visibleResellerRunning
                || webView == null) {
            return;
        }

        resellerCompletionAttempts++;

        String script =
                "(function(){" +
                        "function n(v){return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();}" +
                        "var text=n(document.body.innerText||document.body.textContent);" +
                        "if(text.indexOf('não remover')!==-1||text.indexOf('nao remover')!==-1){" +
                        "return 'open';" +
                        "}" +
                        "return 'closed';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("closed".equals(result)) {
                        finishVisibleResellerSuccess();
                        return;
                    }

                    if (resellerCompletionAttempts < 24) {
                        automationHandler.postDelayed(
                                this::waitVisibleResellerCompletion,
                                350L
                        );
                    } else {
                        failVisibleReseller(
                                "O botão foi acionado uma vez, mas a tela de transferência continuou aberta.",
                                true
                        );
                    }
                }
        );
    }

    private void finishVisibleResellerSuccess() {
        if (!visibleResellerRunning) {
            return;
        }

        visibleResellerRunning = false;
        autoResellerRequested = false;

        prefs.edit()
                .putString(
                        "reseller_state_" + resellerContactKey,
                        "COMPLETE"
                )
                .putBoolean(
                        "reseller_submit_attempted_" + resellerContactKey,
                        false
                )
                .putString(
                        "last_reseller_status",
                        "Revenda: painel ativado com 50 créditos"
                )
                .putBoolean(
                        "reseller_visible_activity_started",
                        false
                )
                .apply();

        try {
            secureStore.put(
                    "linked_reseller_email_" + resellerContactKey,
                    resellerEmail
            );
        } catch (Exception ignored) {
        }

        String supportGroupLink =
                prefs.getString(
                        "reseller_support_group_link",
                        ResellersActivity.DEFAULT_SUPPORT_GROUP_LINK
                ).trim();

        String successMessage =
                "✅ Painel de revenda ativado com sucesso!\n\n" +
                        "Foram adicionados *50 créditos*.";

        if (!supportGroupLink.isEmpty()) {
            successMessage +=
                    "\n\n📲 *Grupo de suporte:*\n" +
                            supportGroupLink;
        }

        Notification target =
                ResellerTaskBridge.getNotification();

        if (target != null) {
            WhatsAppReply.send(
                    this,
                    target,
                    successMessage
            );
        }

        ResellerTaskBridge.complete();

        Toast.makeText(
                this,
                "Revenda ativada com 50 créditos.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void failVisibleReseller(
            String reason,
            boolean afterSubmit
    ) {
        if (!visibleResellerRunning
                && !autoResellerRequested) {
            return;
        }

        visibleResellerRunning = false;
        autoResellerRequested = false;

        prefs.edit()
                .putString(
                        "reseller_state_" + resellerContactKey,
                        afterSubmit
                                ? "REVIEW_REQUIRED"
                                : "ERROR"
                )
                .putString(
                        "last_reseller_status",
                        "Revenda: " + reason
                )
                .putBoolean(
                        "reseller_visible_activity_started",
                        false
                )
                .apply();

        Notification target =
                ResellerTaskBridge.getNotification();

        if (target != null) {
            WhatsAppReply.send(
                    this,
                    target,
                    afterSubmit
                            ? "⚠️ Houve uma tentativa única de adicionar os 50 créditos. Por segurança, não vou repetir automaticamente. Confira essa revenda no painel Sigma."
                            : "Não consegui concluir a ativação da revenda automaticamente.\n\n" + reason
            );
        }

        if (!afterSubmit) {
            prefs.edit()
                    .putBoolean(
                            "reseller_submit_attempted_" + resellerContactKey,
                            false
                    )
                    .apply();
        }

        ResellerTaskBridge.complete();
    }

    private void requestGenerateTest() {
        autoGenerateRequested = true;
        generationRunning = false;
        findProductAttempts = 0;
        findCopyAttempts = 0;
        clipboardAttempts = 0;
        copyScrollAttempts = 0;
        loginAssistAttempted = false;
        capturedCopyText = "";

        updateGenerationStatus(
                "Abrindo teste no painel " + SigmaPanelConfig.getPanelName(this) + "..."
        );

        openDashboard();

        automationHandler.postDelayed(
                this::maybeStartAutomation,
                900L
        );
    }

    private void maybeStartAutomation() {
        if (!autoGenerateRequested
                || generationRunning
                || webView == null) {
            return;
        }

        String url =
                webView.getUrl();

        if (url == null
                || url.isEmpty()) {
            return;
        }

        if (url.contains("#/sign-in")) {
            updateGenerationStatus(
                    "Login/verificação necessária no painel Sigma."
            );

            if (!loginAssistAttempted) {
                loginAssistAttempted = true;

                automationHandler.postDelayed(
                        this::fillSavedLogin,
                        500L
                );
            }

            return;
        }

        if (!url.contains("#/dashboard")) {
            return;
        }

        generationRunning = true;
        findProductAttempts = 0;
        findCopyAttempts = 0;
        clipboardAttempts = 0;
        copyScrollAttempts = 0;
        capturedCopyText = "";

        String preferred =
                SigmaPanelConfig.getPreferredTest(
                        this
                );

        updateGenerationStatus(
                preferred.isEmpty()
                        ? "Procurando teste disponível no painel..."
                        : "Procurando " + preferred + "..."
        );

        automationHandler.postDelayed(
                this::findAndClickTestProduct,
                900L
        );
    }

    private void findAndClickTestProduct() {
        if (!generationRunning
                || webView == null) {
            return;
        }

        findProductAttempts++;

        String script =
                SigmaTestSelector.buildClickScript(
                        this
                );

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("clicked".equals(result)) {
                        updateGenerationStatus(
                                "Gerando teste..."
                        );

                        // Não escreva marcadores no clipboard: o Sigma usa o clipboard para o teste.
                        installCopyCaptureHook();

                        automationHandler.postDelayed(
                                this::findAndClickCopyButton,
                                900L
                        );

                        return;
                    }

                    if (findProductAttempts < 30) {
                        updateGenerationStatus(
                                "Aguardando o painel carregar a opção de teste..."
                        );

                        automationHandler.postDelayed(
                                this::findAndClickTestProduct,
                                650L
                        );
                    } else {
                        failGeneration(
                                "Não consegui acionar a rotina GERAR TESTE no painel Sigma."
                        );
                    }
                }
        );
    }

    private void findAndClickCopyButton() {
        if (!generationRunning
                || webView == null) {
            return;
        }

        findCopyAttempts++;
        copyScrollAttempts++;

        // 1) Procura COPIAR em todo o DOM.
        // 2) Se ainda não estiver disponível, força a rolagem
        //    de TODOS os contêineres roláveis do painel.
        // 3) Repete até o botão existir.
        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'')" +
                        ".replace(/\\\\s+/g,' ')" +
                        ".trim()" +
                        ".toUpperCase();" +
                        "}" +

                        "function findCopy(){" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best=null;" +
                        "var bestLen=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var el=all[i];" +
                        "var text=n(el.innerText||el.textContent);" +
                        "if(text==='COPIAR'||text==='COPIAR E FECHAR'){" +
                        "if(text.length<bestLen){" +
                        "best=el;" +
                        "bestLen=text.length;" +
                        "}" +
                        "}" +
                        "}" +
                        "return best;" +
                        "}" +

                        "var copy=findCopy();" +

                        "if(copy){" +
                        "try{" +
                        "copy.scrollIntoView({" +
                        "behavior:'auto'," +
                        "block:'center'," +
                        "inline:'nearest'" +
                        "});" +
                        "}catch(e){}" +
                        "return 'found';" +
                        "}" +

                        // Força a rolagem em elementos internos
                        // que usam overflow:auto/scroll.
                        "var scrollables=[];" +
                        "var nodes=document.querySelectorAll('body *');" +
                        "for(var j=0;j<nodes.length;j++){" +
                        "var node=nodes[j];" +
                        "try{" +
                        "var st=getComputedStyle(node);" +
                        "var oy=st.overflowY;" +
                        "if(node.scrollHeight>node.clientHeight+40" +
                        "&&(oy==='auto'||oy==='scroll'||oy==='overlay')){" +
                        "scrollables.push(node);" +
                        "}" +
                        "}catch(e){}" +
                        "}" +

                        "for(var k=0;k<scrollables.length;k++){" +
                        "var box=scrollables[k];" +
                        "var step=Math.max(350,Math.floor(box.clientHeight*0.80));" +
                        "box.scrollTop=Math.min(" +
                        "box.scrollHeight," +
                        "box.scrollTop+step" +
                        ");" +
                        "}" +

                        "try{" +
                        "window.scrollBy(0,Math.max(500,window.innerHeight*0.8));" +
                        "}catch(e){}" +

                        "return 'scrolled';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("found".equals(result)) {
                        updateGenerationStatus(
                                "Botão COPIAR localizado. Rolando até ele..."
                        );

                        automationHandler.postDelayed(
                                this::clickCopyButton,
                                350L
                        );

                        return;
                    }

                    if (findCopyAttempts < 65) {
                        updateGenerationStatus(
                                "Descendo no teste para localizar COPIAR..."
                        );

                        automationHandler.postDelayed(
                                this::findAndClickCopyButton,
                                450L
                        );

                    } else {
                        // Último fallback: tenta extrair o cartão completo
                        // de Detalhes do Cliente sem adivinhar credenciais.
                        extractGeneratedDetailsFromDom();
                    }
                }
        );
    }

    private void clickCopyButton() {
        if (!generationRunning
                || webView == null) {
            return;
        }

        // Ignora textos intermediários capturados durante a geração.
        // O clique em COPIAR/COPIAR E FECHAR deve fornecer o conteúdo final.
        capturedCopyText = "";
        installCopyCaptureHook();

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'')" +
                        ".replace(/\\\\s+/g,' ')" +
                        ".trim()" +
                        ".toUpperCase();" +
                        "}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best=null;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var el=all[i];" +
                        "if(n(el.innerText||el.textContent)==='COPIAR'||n(el.innerText||el.textContent)==='COPIAR E FECHAR'){" +
                        "best=el;" +
                        "break;" +
                        "}" +
                        "}" +
                        "if(!best){return 'not_found';}" +
                        "try{" +
                        "best.scrollIntoView({behavior:'auto',block:'center'});" +
                        "}catch(e){}" +
                        "var clickable=best.closest(" +
                        "'button,a,[role=button],[tabindex]'" +
                        ")||best;" +
                        "try{" +
                        "clickable.click();" +
                        "return 'clicked';" +
                        "}catch(e){" +
                        "try{" +
                        "best.dispatchEvent(new MouseEvent(" +
                        "'click'," +
                        "{bubbles:true,cancelable:true,view:window}" +
                        "));" +
                        "return 'clicked';" +
                        "}catch(e2){" +
                        "return 'error';" +
                        "}" +
                        "}" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("clicked".equals(result)) {
                        updateGenerationStatus(
                                "COPIAR acionado. Capturando o teste..."
                        );

                        clipboardAttempts = 0;

                        automationHandler.postDelayed(
                                this::readGeneratedTestFromClipboard,
                                450L
                        );

                    } else {
                        automationHandler.postDelayed(
                                this::findAndClickCopyButton,
                                350L
                        );
                    }
                }
        );
    }

    private void installCopyCaptureHook() {
        if (webView == null) return;

        String script =
                "(function(){" +
                        "try{" +
                        "if(window.__masterRespondeCopyHookInstalled){" +
                        "return 'already';" +
                        "}" +
                        "window.__masterRespondeCopyHookInstalled=true;" +

                        // Captura navigator.clipboard.writeText.
                        "if(navigator.clipboard&&navigator.clipboard.writeText){" +
                        "var originalWriteText=" +
                        "navigator.clipboard.writeText.bind(navigator.clipboard);" +
                        "navigator.clipboard.writeText=function(text){" +
                        "try{" +
                        "window.MasterRespondeBridge.copiedText(String(text||''));" +
                        "}catch(e){}" +
                        "return originalWriteText(text);" +
                        "};" +
                        "}" +

                        // Captura eventos copy como segunda camada.
                        "document.addEventListener('copy',function(ev){" +
                        "try{" +
                        "var txt='';" +
                        "if(ev.clipboardData){" +
                        "txt=ev.clipboardData.getData('text/plain')||'';" +
                        "}" +
                        "if(!txt&&window.getSelection){" +
                        "txt=String(window.getSelection()||'');" +
                        "}" +
                        "if(txt){" +
                        "window.MasterRespondeBridge.copiedText(txt);" +
                        "}" +
                        "}catch(e){}" +
                        "},true);" +

                        "return 'ok';" +
                        "}catch(e){" +
                        "return 'error';" +
                        "}" +
                        "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    private void readGeneratedTestFromClipboard() {
        if (!generationRunning) {
            return;
        }

        clipboardAttempts++;

        String value =
                capturedCopyText == null
                        ? ""
                        : capturedCopyText.trim();

        if (value.isEmpty()) {
            value = readClipboardText();
        }

        if (value.isEmpty()) {

            if (clipboardAttempts < 18) {
                automationHandler.postDelayed(
                        this::readGeneratedTestFromClipboard,
                        350L
                );
            } else {
                extractGeneratedDetailsFromDom();
            }

            return;
        }

        if (!isValidGeneratedTest(value)) {
            // Pode ser um texto intermediário do painel (toast/status).
            // Continua aguardando o conteúdo real antes de usar o fallback do DOM.
            capturedCopyText = "";
            if (clipboardAttempts < 18) {
                automationHandler.postDelayed(
                        this::readGeneratedTestFromClipboard,
                        350L
                );
            } else {
                extractGeneratedDetailsFromDom();
            }
            return;
        }

        finishGeneratedTest(
                value
        );
    }

    private void extractGeneratedDetailsFromDom() {
        if (!generationRunning
                || webView == null) {
            return;
        }

        updateGenerationStatus(
                "Lendo o bloco completo de Detalhes do Cliente..."
        );

        String script =
                "(function(){" +
                        "function norm(v){" +
                        "return (v||'').replace(/\\\\s+/g,' ').trim();" +
                        "}" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best='';" +
                        "for(var i=0;i<all.length;i++){" +
                        "var text=(all[i].innerText||all[i].textContent||'').trim();" +
                        "if(!text)continue;" +
                        "var low=text.toLowerCase();" +
                        "var hasM3u=low.indexOf('m3u')!==-1||low.indexOf('get.php')!==-1;" +
                        "var hasPass=low.indexOf('senha')!==-1||low.indexOf('password=')!==-1;" +
                        "var hasUser=low.indexOf('usuario')!==-1||low.indexOf('username=')!==-1;" +
                        "if(hasM3u&&hasPass&&hasUser){" +
                        "if(!best||text.length<best.length){" +
                        "best=text;" +
                        "}" +
                        "}" +
                        "}" +
                        "return best;" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String text =
                            decodeJavascriptString(
                                    value
                            ).trim();

                    if (!text.isEmpty()
                            && isValidGeneratedTest(text)) {

                        finishGeneratedTest(
                                text
                        );

                    } else {
                        failGeneration(
                                "Não consegui capturar o conteúdo completo do teste."
                        );
                    }
                }
        );
    }

    private String readClipboardText() {
        try {
            ClipboardManager manager =
                    (ClipboardManager) getSystemService(
                            Context.CLIPBOARD_SERVICE
                    );

            if (manager == null
                    || !manager.hasPrimaryClip()) {
                return "";
            }

            ClipData clip =
                    manager.getPrimaryClip();

            if (clip == null
                    || clip.getItemCount() == 0) {
                return "";
            }

            CharSequence value =
                    clip.getItemAt(0)
                            .coerceToText(this);

            return value == null
                    ? ""
                    : value.toString().trim();

        } catch (Exception e) {
            return "";
        }
    }

    private boolean isValidGeneratedTest(
            String value
    ) {
        if (value == null
                || value.trim().isEmpty()) {
            return false;
        }

        String normalized =
                normalizeText(
                        value
                );

        boolean hasUrl =
                normalized.contains("http://")
                        || normalized.contains("https://");

        boolean hasCredentials =
                (normalized.contains("usuario")
                        && normalized.contains("senha"))
                        || (normalized.contains("username=")
                        && normalized.contains("password="))
                        || (normalized.contains("user:")
                        && (normalized.contains("pass:")
                        || normalized.contains("senha:")));

        boolean hasM3u =
                normalized.contains("m3u")
                        || normalized.contains("get.php")
                        || (normalized.contains("username=")
                        && normalized.contains("password="));

        return hasUrl
                && hasCredentials
                && hasM3u;
    }

    private String normalizeText(
            String value
    ) {
        if (value == null) return "";

        return java.text.Normalizer
                .normalize(
                        value,
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }

    private void finishGeneratedTest(
            String fullContent
    ) {
        generationRunning = false;
        autoGenerateRequested = false;

        Notification target =
                TestTaskBridge.getNotification();

        boolean sent = false;

        if (target != null) {
            sent = WhatsAppReply.send(
                    this,
                    target,
                    fullContent
            );
        }

        prefs.edit()
                .putString(
                        "last_test_status",
                        target == null
                                ? "Teste gerado com sucesso"
                                : sent
                                ? "Teste gerado e enviado ao cliente"
                                : "Teste gerado, mas não foi possível enviar"
                )
                .putLong(
                        "last_test_success_time",
                        System.currentTimeMillis()
                )
                .apply();

        if (target != null) {
            TestTaskBridge.complete();
        }

        updateGenerationStatus(
                target == null
                        ? "✅ Teste gerado com sucesso. Conteúdo está no clipboard."
                        : sent
                        ? "✅ Teste gerado e enviado ao WhatsApp."
                        : "⚠️ Teste gerado, mas o envio ao WhatsApp falhou."
        );

        Toast.makeText(
                this,
                target == null
                        ? "Teste gerado com sucesso."
                        : sent
                        ? "Teste enviado ao cliente."
                        : "Teste gerado, mas não enviado.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void failGeneration(
            String reason
    ) {
        generationRunning = false;
        autoGenerateRequested = false;

        Notification target =
                TestTaskBridge.getNotification();

        if (target != null) {
            MessageSettings.send(
                    this,
                    target,
                    prefs,
                    MessageSettings.MSG_TEST_FAIL,
                    MessageSettings.DEFAULT_TEST_FAIL
            );

            TestTaskBridge.complete();
        }

        prefs.edit()
                .putString(
                        "last_test_status",
                        "Teste: falhou - " + reason
                )
                .apply();

        updateGenerationStatus(
                "⚠️ " + reason
        );
    }

    private void updateGenerationStatus(
            String value
    ) {
        runOnUiThread(
                () -> {
                    if (generationStatus != null) {
                        generationStatus.setText(
                                value
                        );
                    }
                }
        );
    }

    private void saveCredentials() {
        String user =
                userField.getText()
                        .toString()
                        .trim();

        String pass =
                passField.getText()
                        .toString();

        if (user.isEmpty()) {
            userField.setError(
                    "Informe o usuário ou e-mail."
            );
            return;
        }

        if (pass.isEmpty()) {
            passField.setError(
                    "Informe a senha."
            );
            return;
        }

        try {
            SigmaPanelConfig.saveCredentials(
                    this,
                    user,
                    pass
            );

            hideKeyboard();

            Toast.makeText(
                    this,
                    "Credenciais salvas com criptografia.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não foi possível salvar as credenciais.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void loadSavedCredentials() {
        String user =
                SigmaPanelConfig.getUser(
                        this
                );

        String pass =
                SigmaPanelConfig.getPass(
                        this
                );

        userField.setText(user);
        passField.setText(pass);
    }

    private void fillSavedLogin() {
        final String user =
                secureStore.get(
                        "masterflix_user"
                );

        final String pass =
                secureStore.get(
                        "masterflix_pass"
                );

        if (user.isEmpty()
                || pass.isEmpty()) {

            Toast.makeText(
                    this,
                    "Salve primeiro o usuário e a senha.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (webView.getUrl() == null
                || !webView.getUrl()
                .startsWith(
                        SigmaPanelConfig.getBaseUrl(
                                this
                        )
                )) {

            openLogin();
        }

        final String userJson =
                JSONObject.quote(user);

        final String passJson =
                JSONObject.quote(pass);

        String script =
                "(function(){" +
                        "var inputs=document.querySelectorAll('input');" +
                        "var user=null,pass=null;" +
                        "for(var i=0;i<inputs.length;i++){" +
                        "var t=(inputs[i].type||'').toLowerCase();" +
                        "if(t==='password'&&!pass){pass=inputs[i];}" +
                        "else if((t==='text'||t==='email'||!t)&&!user){user=inputs[i];}" +
                        "}" +
                        "function set(el,v){" +
                        "if(!el)return false;" +
                        "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,v);" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "return true;" +
                        "}" +
                        "return set(user," + userJson + ")&&set(pass," + passJson + ");" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> Toast.makeText(
                        this,
                        "Campos preenchidos. Conclua “Verificado” e toque em Continuar.",
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void confirmClearSession() {
        new AlertDialog.Builder(this)
                .setTitle("Encerrar sessão do painel Sigma?")
                .setMessage(
                        "Os cookies da sessão serão apagados. " +
                        "As credenciais criptografadas continuarão salvas."
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "ENCERRAR",
                        (dialog, which) ->
                                clearSession()
                )
                .show();
    }

    private void clearSession() {
        CookieManager.getInstance()
                .removeAllCookies(
                        value -> {
                            CookieManager.getInstance()
                                    .flush();

                            prefs.edit()
                                    .putBoolean(
                                            "masterflix_session_active",
                                            false
                                    )
                                    .apply();

                            updateSessionStatus();
                            openLogin();

                            Toast.makeText(
                                    this,
                                    "Sessão encerrada.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }

    private void updateSessionStatus() {
        boolean active =
                prefs.getBoolean(
                        "masterflix_session_active",
                        false
                );

        if (active) {
            sessionStatus.setText(
                    "● Sessão ativa"
            );

            sessionStatus.setTextColor(
                    Color.rgb(
                            0,
                            184,
                            255
                    )
            );

        } else {
            sessionStatus.setText(
                    "● Login necessário"
            );

            sessionStatus.setTextColor(
                    Color.rgb(
                            255,
                            145,
                            0
                    )
            );
        }
    }

    private String decodeJavascriptString(
            String value
    ) {
        if (value == null
                || "null".equals(value)) {
            return "";
        }

        try {
            return new org.json.JSONTokener(
                    value
            ).nextValue().toString();

        } catch (Exception e) {
            return "";
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE
                    );

            View current =
                    getCurrentFocus();

            if (manager != null
                    && current != null) {
                manager.hideSoftInputFromWindow(
                        current.getWindowToken(),
                        0
                );
            }

        } catch (Exception ignored) {
        }
    }

    private LinearLayout card() {
        LinearLayout c =
                new LinearLayout(this);

        c.setOrientation(
                LinearLayout.VERTICAL
        );

        c.setPadding(
                dp(15),
                dp(15),
                dp(15),
                dp(15)
        );

        c.setBackgroundResource(
                getResources().getIdentifier(
                        "card",
                        "drawable",
                        getPackageName()
                )
        );

        return c;
    }

    private EditText field(
            String hint
    ) {
        EditText e =
                new EditText(this);

        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(
                Color.rgb(
                        130,
                        140,
                        150
                )
        );

        e.setTextSize(14);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        lp.setMargins(
                0,
                dp(5),
                0,
                dp(8)
        );

        e.setLayoutParams(lp);

        return e;
    }

    private Button actionButton(
            String label,
            boolean orange
    ) {
        Button b =
                new Button(this);

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
                dp(4),
                0,
                dp(4)
        );

        b.setLayoutParams(lp);

        return b;
    }

    private Button smallButton(
            String label
    ) {
        Button b =
                new Button(this);

        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(10);
        b.setTypeface(null, 1);

        b.setBackgroundResource(
                getResources().getIdentifier(
                        "button_dark",
                        "drawable",
                        getPackageName()
                )
        );

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(46)
                );

        lp.setMargins(
                dp(4),
                0,
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
        TextView t =
                new TextView(this);

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

    public class MasterRespondeBridge {
        @JavascriptInterface
        public void sessionUrl(
                String url
        ) {
            updateSessionFromUrl(
                    url
            );
        }

        @JavascriptInterface
        public void copiedText(
                String value
        ) {
            if (value == null) return;

            String text =
                    value.trim();

            if (!text.isEmpty()
                    && !"__MASTER_RESPONDE_WAITING__".equals(text)) {
                capturedCopyText =
                        text;
            }
        }
    }
}
