package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

public class MasterflixBackgroundAutomation {

    private static final String DASHBOARD_URL =
            "https://masterflix.sigmab.pro/#/dashboard";

    private static final String TEST_PRODUCT =
            "MASTERFLIX TESTE COMPLETO 1H";

    private static final Object LOCK =
            new Object();

    private static MasterflixBackgroundAutomation current;

    private final Context context;
    private final Notification whatsappNotification;
    private final SharedPreferences prefs;
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private WebView webView;

    private boolean finished = false;
    private boolean dashboardSeen = false;
    private int productAttempts = 0;
    private int copyAttempts = 0;
    private int captureAttempts = 0;

    private volatile String capturedCopyText = "";

    private MasterflixBackgroundAutomation(
            Context context,
            Notification notification,
            SharedPreferences prefs
    ) {
        this.context =
                context.getApplicationContext();

        this.whatsappNotification =
                notification;

        this.prefs =
                prefs;
    }

    public static boolean isRunning() {
        synchronized (LOCK) {
            return current != null && !current.finished;
        }
    }

    public static boolean start(
            Context context,
            Notification notification,
            SharedPreferences prefs
    ) {
        synchronized (LOCK) {
            if (current != null
                    && !current.finished) {
                return false;
            }

            current =
                    new MasterflixBackgroundAutomation(
                            context,
                            notification,
                            prefs
                    );

            current.handler.post(
                    current::createAndStart
            );

            return true;
        }
    }

    private void createAndStart() {
        try {
            updateStatus(
                    "Teste: iniciando MASTERFLIX em segundo plano"
            );

            webView =
                    new WebView(context);

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
                    new Bridge(),
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

                            handleLocation();
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

                            handleLocation();
                        }
                    }
            );

            webView.loadUrl(
                    DASHBOARD_URL
            );

            // Segurança contra fluxo preso para sempre.
            handler.postDelayed(
                    () -> {
                        if (!finished
                                && !dashboardSeen) {
                            inspectForSessionTimeout();
                        }
                    },
                    12000L
            );

        } catch (Throwable e) {
            fail(
                    "Não foi possível iniciar o MASTERFLIX em segundo plano."
            );
        }
    }

    private void handleLocation() {
        if (finished
                || webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "(function(){try{return location.href||'';}catch(e){return '';}})();",
                value -> {
                    String url =
                            decodeJavascriptString(
                                    value
                            );

                    if (finished) return;

                    if (url.contains("#/dashboard")) {
                        dashboardSeen =
                                true;

                        prefs.edit()
                                .putBoolean(
                                        "masterflix_session_active",
                                        true
                                )
                                .apply();

                        updateStatus(
                                "Teste: sessão MASTERFLIX ativa"
                        );

                        if (productAttempts == 0) {
                            handler.postDelayed(
                                    this::findAndClickProduct,
                                    1200L
                            );
                        }

                    } else if (url.contains("#/sign-in")) {

                        prefs.edit()
                                .putBoolean(
                                        "masterflix_session_active",
                                        false
                                )
                                .apply();

                        // Dá tempo para eventual redirecionamento automático.
                        handler.postDelayed(
                                () -> confirmStillOnLogin(),
                                1800L
                        );
                    }
                }
        );
    }

    private void confirmStillOnLogin() {
        if (finished
                || dashboardSeen
                || webView == null) {
            return;
        }

        webView.evaluateJavascript(
                "(function(){try{return location.href||'';}catch(e){return '';}})();",
                value -> {
                    String url =
                            decodeJavascriptString(
                                    value
                            );

                    if (!finished
                            && !dashboardSeen
                            && url.contains("#/sign-in")) {

                        fail(
                                "Sessão MASTERFLIX expirada. Abra o MASTER RESPONDE e faça o login/verificação."
                        );
                    }
                }
        );
    }

    private void inspectForSessionTimeout() {
        if (finished
                || dashboardSeen) {
            return;
        }

        if (webView == null) {
            fail(
                    "MASTERFLIX não iniciou em segundo plano."
            );
            return;
        }

        webView.evaluateJavascript(
                "(function(){try{return location.href||'';}catch(e){return '';}})();",
                value -> {
                    String url =
                            decodeJavascriptString(
                                    value
                            );

                    if (url.contains("#/sign-in")) {
                        fail(
                                "Sessão MASTERFLIX expirada. Abra o MASTER RESPONDE e faça o login/verificação."
                        );
                    } else if (!dashboardSeen) {
                        fail(
                                "MASTERFLIX não carregou o dashboard em segundo plano."
                        );
                    }
                }
        );
    }

    private void findAndClickProduct() {
        if (finished
                || webView == null) {
            return;
        }

        productAttempts++;

        String wanted =
                JSONObject.quote(
                        TEST_PRODUCT
                );

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toUpperCase();" +
                        "}" +
                        "var target=n(" + wanted + ");" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best=null;" +
                        "var bestLen=999999;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var el=all[i];" +
                        "var text=n(el.innerText||el.textContent);" +

                        "if(text.indexOf(target)!==-1" +
                        "&&text.length<bestLen){" +
                        "best=el;" +
                        "bestLen=text.length;" +
                        "}" +
                        "}" +

                        "if(!best){" +
                        "scrollAll();" +
                        "return 'not_found';" +
                        "}" +

                        "try{" +
                        "best.scrollIntoView({behavior:'auto',block:'center'});" +
                        "}catch(e){}" +

                        "var clickable=best.closest(" +
                        "'button,a,[role=button],[tabindex],li'" +
                        ")||best;" +

                        "try{" +
                        "clickable.click();" +
                        "return 'clicked';" +
                        "}catch(e){" +
                        "try{" +
                        "best.dispatchEvent(new MouseEvent(" +
                        "'click',{bubbles:true,cancelable:true,view:window}" +
                        "));" +
                        "return 'clicked';" +
                        "}catch(e2){" +
                        "return 'error';" +
                        "}" +
                        "}" +

                        "function scrollAll(){" +
                        "try{window.scrollBy(0,700);}catch(e){}" +
                        "var nodes=document.querySelectorAll('body *');" +
                        "for(var j=0;j<nodes.length;j++){" +
                        "var x=nodes[j];" +
                        "try{" +
                        "if(x.scrollHeight>x.clientHeight+40){" +
                        "x.scrollTop=Math.min(" +
                        "x.scrollHeight," +
                        "x.scrollTop+Math.max(400,x.clientHeight*0.8)" +
                        ");" +
                        "}" +
                        "}catch(e){}" +
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
                        updateStatus(
                                "Teste: produto localizado e acionado"
                        );

                        installCopyHook();

                        handler.postDelayed(
                                this::findAndClickCopy,
                                900L
                        );

                        return;
                    }

                    if (productAttempts < 35) {
                        updateStatus(
                                "Teste: procurando produto no MASTERFLIX"
                        );

                        handler.postDelayed(
                                this::findAndClickProduct,
                                550L
                        );

                    } else {
                        fail(
                                "Não encontrei MASTERFLIX TESTE COMPLETO 1H no painel."
                        );
                    }
                }
        );
    }

    private void installCopyHook() {
        if (finished
                || webView == null) {
            return;
        }

        String script =
                "(function(){" +
                        "try{" +
                        "if(window.__mrCopyHook){return 'ok';}" +
                        "window.__mrCopyHook=true;" +

                        "if(navigator.clipboard" +
                        "&&navigator.clipboard.writeText){" +

                        "var original=" +
                        "navigator.clipboard.writeText.bind(navigator.clipboard);" +

                        "navigator.clipboard.writeText=function(text){" +
                        "try{" +
                        "window.MasterRespondeBridge.copiedText(" +
                        "String(text||'')" +
                        ");" +
                        "}catch(e){}" +

                        "try{" +
                        "return original(text);" +
                        "}catch(e){" +
                        "return Promise.resolve();" +
                        "}" +
                        "};" +
                        "}" +

                        "document.addEventListener(" +
                        "'copy'," +
                        "function(ev){" +
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
                        "}," +
                        "true" +
                        ");" +

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

    private void findAndClickCopy() {
        if (finished
                || webView == null) {
            return;
        }

        copyAttempts++;

        installCopyHook();

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toUpperCase();" +
                        "}" +

                        "var all=document.querySelectorAll('body *');" +
                        "var copy=null;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var el=all[i];" +
                        "if(n(el.innerText||el.textContent)==='COPIAR'){" +
                        "copy=el;" +
                        "break;" +
                        "}" +
                        "}" +

                        "if(copy){" +
                        "try{" +
                        "copy.scrollIntoView({behavior:'auto',block:'center'});" +
                        "}catch(e){}" +

                        "var clickable=copy.closest(" +
                        "'button,a,[role=button],[tabindex]'" +
                        ")||copy;" +

                        "try{" +
                        "clickable.click();" +
                        "return 'clicked';" +
                        "}catch(e){" +
                        "try{" +
                        "copy.dispatchEvent(new MouseEvent(" +
                        "'click',{bubbles:true,cancelable:true,view:window}" +
                        "));" +
                        "return 'clicked';" +
                        "}catch(e2){" +
                        "return 'error';" +
                        "}" +
                        "}" +
                        "}" +

                        // Mesmo sem janela visível, força todos os
                        // contêineres roláveis para baixo.
                        "try{window.scrollBy(0,700);}catch(e){}" +
                        "var nodes=document.querySelectorAll('body *');" +

                        "for(var j=0;j<nodes.length;j++){" +
                        "var x=nodes[j];" +
                        "try{" +
                        "if(x.scrollHeight>x.clientHeight+40){" +
                        "var step=Math.max(400,Math.floor(x.clientHeight*0.85));" +
                        "x.scrollTop=Math.min(x.scrollHeight,x.scrollTop+step);" +
                        "}" +
                        "}catch(e){}" +
                        "}" +

                        "return 'scrolled';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("clicked".equals(result)) {
                        updateStatus(
                                "Teste: COPIAR acionado em segundo plano"
                        );

                        captureAttempts = 0;

                        handler.postDelayed(
                                this::waitForCapturedText,
                                350L
                        );

                        return;
                    }

                    if (copyAttempts < 70) {
                        updateStatus(
                                "Teste: descendo até o botão COPIAR"
                        );

                        handler.postDelayed(
                                this::findAndClickCopy,
                                420L
                        );

                    } else {
                        extractDetailsFromDom();
                    }
                }
        );
    }

    private void waitForCapturedText() {
        if (finished) return;

        captureAttempts++;

        String value =
                capturedCopyText == null
                        ? ""
                        : capturedCopyText.trim();

        if (!value.isEmpty()) {
            if (isValidGeneratedTest(value)) {
                success(value);
            } else {
                extractDetailsFromDom();
            }

            return;
        }

        if (captureAttempts < 18) {
            handler.postDelayed(
                    this::waitForCapturedText,
                    300L
            );
        } else {
            extractDetailsFromDom();
        }
    }

    private void extractDetailsFromDom() {
        if (finished
                || webView == null) {
            return;
        }

        updateStatus(
                "Teste: lendo Detalhes do Cliente"
        );

        String script =
                "(function(){" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best='';" +

                        "for(var i=0;i<all.length;i++){" +
                        "var text=(all[i].innerText||all[i].textContent||'').trim();" +
                        "if(!text)continue;" +

                        "var low=text.toLowerCase();" +

                        "var hasM3u=" +
                        "low.indexOf('m3u')!==-1" +
                        "||low.indexOf('get.php')!==-1;" +

                        "var hasUser=" +
                        "low.indexOf('usuario')!==-1" +
                        "||low.indexOf('usuário')!==-1" +
                        "||low.indexOf('username=')!==-1;" +

                        "var hasPass=" +
                        "low.indexOf('senha')!==-1" +
                        "||low.indexOf('password=')!==-1;" +

                        "if(hasM3u&&hasUser&&hasPass){" +
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

                        success(text);

                    } else {
                        fail(
                                "Não consegui capturar o conteúdo completo do teste."
                        );
                    }
                }
        );
    }

    private boolean isValidGeneratedTest(
            String value
    ) {
        if (value == null
                || value.trim().isEmpty()) {
            return false;
        }

        String normalized =
                normalizeText(value);

        boolean hasUrl =
                normalized.contains("http://")
                        || normalized.contains("https://");

        boolean hasUser =
                normalized.contains("usuario")
                        || normalized.contains("username=")
                        || normalized.contains("user:");

        boolean hasPass =
                normalized.contains("senha")
                        || normalized.contains("password=")
                        || normalized.contains("pass:");

        boolean hasM3u =
                normalized.contains("m3u")
                        || normalized.contains("get.php");

        return hasUrl
                && hasUser
                && hasPass
                && hasM3u;
    }

    private void success(
            String fullContent
    ) {
        if (finished) return;

        finished =
                true;

        boolean sent =
                WhatsAppReply.send(
                        context,
                        whatsappNotification,
                        fullContent
                );

        prefs.edit()
                .putString(
                        "last_test_status",
                        sent
                                ? "Teste gerado em segundo plano e enviado"
                                : "Teste gerado em segundo plano, mas envio falhou"
                )
                .putLong(
                        "last_test_success_time",
                        System.currentTimeMillis()
                )
                .apply();

        TestTaskBridge.complete();

        // Finaliza também o pending persistido. Sem isso, o
        // NotificationAccessService entende que o teste ainda está pendente
        // e reinicia o motor após o cleanup, gerando testes em sequência.
        BackgroundRuntime.clearTestPending(prefs);

        cleanup();
    }

    private void fail(
            String reason
    ) {
        if (finished) return;

        finished =
                true;

        prefs.edit()
                .putString(
                        "last_test_status",
                        "Teste: " + reason
                )
                .apply();

        WhatsAppReply.send(
                context,
                whatsappNotification,
                "Não consegui concluir o teste automaticamente. " +
                        "Confira o MASTERFLIX no MASTER RESPONDE."
        );

        TestTaskBridge.complete();

        // Finaliza também o pending persistido. Sem isso, o
        // NotificationAccessService entende que o teste ainda está pendente
        // e reinicia o motor após o cleanup, gerando testes em sequência.
        BackgroundRuntime.clearTestPending(prefs);

        cleanup();
    }

    private void cleanup() {
        handler.post(
                () -> {
                    try {
                        if (webView != null) {
                            webView.removeJavascriptInterface(
                                    "MasterRespondeBridge"
                            );

                            webView.stopLoading();
                            webView.loadUrl(
                                    "about:blank"
                            );

                            webView.destroy();
                            webView =
                                    null;
                        }

                    } catch (Throwable ignored) {
                    }

                    synchronized (LOCK) {
                        if (current == this) {
                            current =
                                    null;
                        }
                    }
                }
        );
    }

    private void updateStatus(
            String value
    ) {
        prefs.edit()
                .putString(
                        "last_test_status",
                        value
                )
                .apply();
    }

    private String decodeJavascriptString(
            String value
    ) {
        if (value == null
                || "null".equals(value)) {
            return "";
        }

        try {
            Object decoded =
                    new org.json.JSONTokener(
                            value
                    ).nextValue();

            return decoded == null
                    ? ""
                    : decoded.toString();

        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeText(
            String value
    ) {
        if (value == null) return "";

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(
                        Locale.ROOT
                );
    }

    public class Bridge {

        @JavascriptInterface
        public void copiedText(
                String value
        ) {
            if (value == null) return;

            String text =
                    value.trim();

            if (!text.isEmpty()) {
                capturedCopyText =
                        text;
            }
        }
    }
}
