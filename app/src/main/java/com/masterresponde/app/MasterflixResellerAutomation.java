package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

public class MasterflixResellerAutomation {

    private static final int CREDIT_AMOUNT = 50;

    private static final String MODE_FIND =
            "find";

    private static final String MODE_ADD =
            "add";

    private static final Object LOCK =
            new Object();

    private static MasterflixResellerAutomation current;

    private final Context context;
    private final Notification whatsappNotification;
    private final SharedPreferences prefs;
    private final String contactKey;
    private final String email;
    private final String mode;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private WebView webView;

    private boolean finished = false;
    private boolean pageReady = false;
    private boolean loginAssistAttempted = false;
    private boolean submitAttempted = false;

    private int searchAttempts = 0;
    private int dialogAttempts = 0;
    private int verifyAttempts = 0;
    private int completionAttempts = 0;
    private int submitSearchAttempts = 0;
    private int agreementAttempts = 0;

    private double beforeCredits = Double.NaN;

    private MasterflixResellerAutomation(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String contactKey,
            String email,
            String mode
    ) {
        this.context =
                context.getApplicationContext();

        this.whatsappNotification =
                notification;

        this.prefs =
                prefs;

        this.contactKey =
                contactKey;

        this.email =
                email == null
                        ? ""
                        : email.trim().toLowerCase(Locale.ROOT);

        this.mode =
                mode;
    }

    public static boolean findReseller(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String contactKey,
            String email
    ) {
        return start(
                context,
                notification,
                prefs,
                contactKey,
                email,
                MODE_FIND
        );
    }

    public static boolean addCredits(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String contactKey,
            String email
    ) {
        return start(
                context,
                notification,
                prefs,
                contactKey,
                email,
                MODE_ADD
        );
    }

    private static boolean start(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String contactKey,
            String email,
            String mode
    ) {
        synchronized (LOCK) {
            if (current != null
                    && !current.finished) {
                return false;
            }

            if (!MasterflixAutomationGate.tryAcquire(
                    "reseller"
            )) {
                return false;
            }

            try {
                current =
                        new MasterflixResellerAutomation(
                                context,
                                notification,
                                prefs,
                                contactKey,
                                email,
                                mode
                        );

                current.handler.post(
                        current::createAndStart
                );

                return true;

            } catch (Throwable e) {
                MasterflixAutomationGate.release(
                        "reseller"
                );

                current =
                        null;

                return false;
            }
        }
    }

    public static boolean isRunning() {
        synchronized (LOCK) {
            return current != null
                    && !current.finished;
        }
    }

    public static boolean cancelFind(
            String requestedContactKey
    ) {
        synchronized (LOCK) {
            if (current == null
                    || current.finished
                    || !MODE_FIND.equals(
                    current.mode
            )) {
                return false;
            }

            if (requestedContactKey != null
                    && !requestedContactKey.isEmpty()
                    && !requestedContactKey.equals(
                    current.contactKey
            )) {
                return false;
            }

            current.finished =
                    true;

            current.updateStatus(
                    "Revenda: verificação cancelada"
            );

            current.cleanup();

            return true;
        }
    }

    private void createAndStart() {
        try {
            updateStatus(
                    MODE_FIND.equals(mode)
                            ? "Revenda: procurando cadastro em "
                            + SigmaPanelConfig.getPanelName(context)
                            : "Revenda: preparando crédito inicial +50"
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

                            inspectLocation();
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

                            inspectLocation();
                        }
                    }
            );

            webView.loadUrl(
                    SigmaPanelConfig.resellersUrl(
                            context
                    )
            );

            handler.postDelayed(
                    () -> {
                        if (!finished
                                && !pageReady) {
                            confirmLoadState();
                        }
                    },
                    12000L
            );

        } catch (Throwable e) {
            failBeforeSubmit(
                    "Não foi possível iniciar o painel Sigma em segundo plano."
            );
        }
    }

    private void inspectLocation() {
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

                    if (url.contains("#/resellers")) {
                        if (!pageReady) {
                            pageReady =
                                    true;

                            prefs.edit()
                                    .putBoolean(
                                            "masterflix_session_active",
                                            true
                                    )
                                    .apply();

                            handler.postDelayed(
                                    this::searchExactEmail,
                                    1000L
                            );
                        }

                    } else if (url.contains("#/sign-in")) {
                        prefs.edit()
                                .putBoolean(
                                        "masterflix_session_active",
                                        false
                                )
                                .apply();

                        if (!loginAssistAttempted) {
                            loginAssistAttempted = true;

                            updateStatus(
                                    "Revenda: sessão expirada; tentando login salvo"
                            );

                            handler.postDelayed(
                                    () -> MasterflixLoginAssist.fillAndSubmitIfSafe(
                                            context,
                                            webView,
                                            result -> {
                                                if ("verification_required".equals(result)) {
                                                    failBeforeSubmit(
                                                            "Login preenchido, mas o painel Sigma exige verificação manual."
                                                    );
                                                    return;
                                                }

                                                if ("no_credentials".equals(result)) {
                                                    failBeforeSubmit(
                                                            "Sessão expirada e não há credenciais salvas."
                                                    );
                                                    return;
                                                }

                                                handler.postDelayed(
                                                        this::confirmStillOnLogin,
                                                        2500L
                                                );
                                            }
                                    ),
                                    500L
                            );

                        } else {
                            handler.postDelayed(
                                    this::confirmStillOnLogin,
                                    1600L
                            );
                        }
                    }
                }
        );
    }

    private void confirmLoadState() {
        if (finished
                || pageReady
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

                    if (url.contains("#/sign-in")) {
                        failBeforeSubmit(
                                "Sessão do painel Sigma expirada. As credenciais salvas foram tentadas; verifique o login manualmente."
                        );
                    } else if (!pageReady) {
                        failBeforeSubmit(
                                "A página de revendas não carregou."
                        );
                    }
                }
        );
    }

    private void confirmStillOnLogin() {
        if (finished
                || pageReady
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

                    if (url.contains("#/sign-in")) {
                        failBeforeSubmit(
                                "Sessão do painel Sigma expirada. As credenciais salvas foram tentadas; verifique o login manualmente."
                        );
                    }
                }
        );
    }

    private void searchExactEmail() {
        if (finished
                || webView == null) {
            return;
        }

        searchAttempts++;

        String emailJson =
                JSONObject.quote(
                        email
                );

        String script =
                "(function(){" +
                        "var wanted=" + emailJson + ";" +

                        "function visible(el){" +
                        "if(!el)return false;" +
                        "var r=el.getBoundingClientRect();" +
                        "var st=getComputedStyle(el);" +
                        "return r.width>0&&r.height>0" +
                        "&&st.display!=='none'" +
                        "&&st.visibility!=='hidden';" +
                        "}" +

                        "function setValue(el,v){" +
                        "if(!el)return false;" +
                        "try{" +
                        "var proto=el.tagName==='TEXTAREA'" +
                        "?HTMLTextAreaElement.prototype" +
                        ":HTMLInputElement.prototype;" +
                        "var setter=Object.getOwnPropertyDescriptor(proto,'value').set;" +
                        "setter.call(el,v);" +
                        "}catch(e){el.value=v;}" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true,key:'a'}));" +
                        "return true;" +
                        "}" +

                        "var inputs=document.querySelectorAll('input,textarea');" +
                        "var search=null;" +

                        "for(var i=0;i<inputs.length;i++){" +
                        "var el=inputs[i];" +
                        "if(!visible(el))continue;" +
                        "var hint=((el.placeholder||'')+' '+(el.getAttribute('aria-label')||'')).toLowerCase();" +
                        "var type=(el.type||'').toLowerCase();" +
                        "if(type==='search'" +
                        "||hint.indexOf('buscar')!==-1" +
                        "||hint.indexOf('pesquisar')!==-1" +
                        "||hint.indexOf('search')!==-1" +
                        "||hint.indexOf('email')!==-1){" +
                        "search=el;" +
                        "break;" +
                        "}" +
                        "}" +

                        "if(search){" +
                        "setValue(search,wanted);" +
                        "try{search.focus();}catch(e){}" +
                        "}" +

                        "return search?'search_set':'no_search';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> handler.postDelayed(
                        this::inspectExactEmail,
                        700L
                )
        );
    }

    private void inspectExactEmail() {
        if (finished
                || webView == null) {
            return;
        }

        String emailJson =
                JSONObject.quote(
                        email
                );

        String script =
                "(function(){" +
                        "var wanted=" + emailJson + ".toLowerCase();" +
                        "var all=document.querySelectorAll('body *');" +
                        "var best=null;" +
                        "var bestLen=999999;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var el=all[i];" +
                        "var text=(el.innerText||el.textContent||'').trim();" +
                        "if(!text)continue;" +
                        "var low=text.toLowerCase();" +

                        "if(low===wanted" +
                        "||low.indexOf(wanted)!==-1){" +
                        "if(text.length<bestLen){" +
                        "best=el;" +
                        "bestLen=text.length;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!best){" +
                        "return JSON.stringify({found:false});" +
                        "}" +

                        "var row=best.closest('tr,[role=row]');" +

                        "if(!row){" +
                        "var p=best;" +
                        "for(var d=0;d<7&&p;d++,p=p.parentElement){" +
                        "var buttons=p.querySelectorAll('button,a,[role=button]');" +
                        "var text=(p.innerText||'');" +
                        "if(buttons.length>0&&text.toLowerCase().indexOf(wanted)!==-1){" +
                        "row=p;" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!row){row=best.parentElement||best;}" +

                        "var rowText=(row.innerText||row.textContent||'').trim();" +

                        "return JSON.stringify({" +
                        "found:true," +
                        "text:rowText" +
                        "});" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String decoded =
                            decodeJavascriptString(
                                    value
                            );

                    try {
                        JSONObject result =
                                new JSONObject(
                                        decoded
                                );

                        boolean found =
                                result.optBoolean(
                                        "found",
                                        false
                                );

                        if (found) {
                            if (MODE_FIND.equals(mode)) {
                                foundAndSendSupportGroup();
                            } else {
                                openAddCreditsForExactEmail();
                            }

                            return;
                        }

                    } catch (Exception ignored) {
                    }

                    if (searchAttempts < 20) {
                        handler.postDelayed(
                                this::searchExactEmail,
                                650L
                        );
                    } else {
                        failBeforeSubmit(
                                "Não encontrei o e-mail exato da revenda no painel Sigma."
                        );
                    }
                }
        );
    }

    private void foundAndSendSupportGroup() {
        if (finished) return;

        finished =
                true;

        setState(
                "COMPLETE"
        );

        updateStatus(
                "Revenda confirmada no painel Sigma. Grupo de suporte enviado."
        );

        try {
            SecureStore secureStore =
                    new SecureStore(
                            context
                    );

            secureStore.put(
                    "linked_reseller_email_" + contactKey,
                    email
            );

        } catch (Exception ignored) {
        }

        String supportGroupLink =
                prefs.getString(
                        "reseller_support_group_link",
                        ResellersActivity.DEFAULT_SUPPORT_GROUP_LINK
                ).trim();

        MessageSettings.send(
                context,
                whatsappNotification,
                prefs,
                MessageSettings.MSG_RESELLER_CONFIRMED,
                MessageSettings.DEFAULT_RESELLER_CONFIRMED,
                MessageSettings.variables(
                        "LINK_GRUPO",
                        supportGroupLink
                )
        );

        cleanup();
    }

    private void openAddCreditsForExactEmail() {
        if (finished
                || webView == null) {
            return;
        }

        String emailJson =
                JSONObject.quote(
                        email
                );

        String script =
                "(function(){" +
                        "var wanted=" + emailJson + ".toLowerCase();" +

                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim();" +
                        "}" +

                        "function norm(v){" +
                        "return n(v).toLowerCase();" +
                        "}" +

                        // 1. Localiza o elemento mais específico que contém o e-mail.
                        "var all=document.querySelectorAll('body *');" +
                        "var emailEl=null;" +
                        "var bestLen=999999;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var text=n(all[i].innerText||all[i].textContent);" +
                        "if(!text)continue;" +
                        "var low=text.toLowerCase();" +

                        "if(low===wanted||low.indexOf(wanted)!==-1){" +
                        "if(text.length<bestLen){" +
                        "emailEl=all[i];" +
                        "bestLen=text.length;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!emailEl){" +
                        "return JSON.stringify({ok:false,reason:'email'});" +
                        "}" +

                        // 2. Sobe pelos ancestrais até encontrar o CARTÃO da revenda:
                        // deve conter o e-mail e também o texto Adicionar Créditos.
                        "var card=null;" +
                        "var p=emailEl;" +

                        "for(var d=0;d<14&&p;d++,p=p.parentElement){" +
                        "var txt=norm(p.innerText||p.textContent);" +

                        "if(txt.indexOf(wanted)!==-1" +
                        "&&(txt.indexOf('adicionar créditos')!==-1" +
                        "||txt.indexOf('adicionar creditos')!==-1)){" +
                        "card=p;" +
                        "break;" +
                        "}" +
                        "}" +

                        "if(!card){" +
                        "return JSON.stringify({ok:false,reason:'card'});" +
                        "}" +

                        // 3. Dentro do MESMO cartão, procura exatamente o rótulo.
                        "var descendants=card.querySelectorAll('*');" +
                        "var label=null;" +
                        "var labelLen=999999;" +

                        "for(var j=0;j<descendants.length;j++){" +
                        "var t=n(descendants[j].innerText||descendants[j].textContent);" +
                        "var low=t.toLowerCase();" +

                        "if(low==='adicionar créditos'" +
                        "||low==='adicionar creditos'){" +
                        "if(t.length<labelLen){" +
                        "label=descendants[j];" +
                        "labelLen=t.length;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!label){" +
                        "return JSON.stringify({ok:false,reason:'label'});" +
                        "}" +

                        // 4. O texto pode ficar abaixo do botão roxo.
                        // Primeiro tenta um ancestral clicável do texto.
                        "var clickable=label.closest(" +
                        "'button,a,[role=button],[tabindex],[onclick]'" +
                        ");" +

                        // 5. Se o rótulo for irmão do botão, procura nos pais próximos.
                        "if(!clickable){" +
                        "var q=label.parentElement;" +

                        "for(var x=0;x<6&&q&&!clickable;x++,q=q.parentElement){" +
                        "var candidates=q.querySelectorAll(" +
                        "'button,a,[role=button],[tabindex],[onclick]'" +
                        ");" +

                        "if(candidates.length===1){" +
                        "clickable=candidates[0];" +
                        "break;" +
                        "}" +

                        "for(var c=0;c<candidates.length;c++){" +
                        "var meta=norm(" +
                        "(candidates[c].innerText||candidates[c].textContent||'')+' '+" +
                        "(candidates[c].getAttribute('aria-label')||'')+' '+" +
                        "(candidates[c].getAttribute('title')||'')" +
                        ");" +

                        "if(meta.indexOf('adicionar')!==-1" +
                        "||meta.indexOf('credit')!==-1){" +
                        "clickable=candidates[c];" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +
                        "}" +

                        // 6. Último fallback: o próprio bloco do rótulo/pai.
                        "if(!clickable){" +
                        "clickable=label.parentElement||label;" +
                        "}" +

                        "try{" +
                        "clickable.scrollIntoView({behavior:'auto',block:'center'});" +
                        "}catch(e){}" +

                        "try{" +
                        "clickable.click();" +
                        "}catch(e){" +
                        "try{" +
                        "clickable.dispatchEvent(new MouseEvent(" +
                        "'click'," +
                        "{bubbles:true,cancelable:true,view:window}" +
                        "));" +
                        "}catch(e2){" +
                        "return JSON.stringify({ok:false,reason:'click'});" +
                        "}" +
                        "}" +

                        "return JSON.stringify({ok:true});" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String decoded =
                            decodeJavascriptString(
                                    value
                            );

                    try {
                        JSONObject result =
                                new JSONObject(
                                        decoded
                                );

                        if (!result.optBoolean(
                                "ok",
                                false
                        )) {
                            String reason =
                                    result.optString(
                                            "reason",
                                            ""
                                    );

                            if ("email".equals(reason)) {
                                failBeforeSubmit(
                                        "Não consegui localizar exatamente a revenda pelo e-mail. Nenhum crédito foi enviado."
                                );

                            } else if ("card".equals(reason)
                                    || "label".equals(reason)) {

                                failBeforeSubmit(
                                        "Encontrei a revenda pelo e-mail, mas não localizei “Adicionar Créditos” dentro do cartão dela. Nenhum crédito foi enviado."
                                );

                            } else {
                                failBeforeSubmit(
                                        "Encontrei o botão de créditos, mas não consegui abri-lo. Nenhum crédito foi enviado."
                                );
                            }

                            return;
                        }

                        dialogAttempts = 0;

                        updateStatus(
                                "Revenda encontrada; abrindo Adicionar Créditos"
                        );

                        handler.postDelayed(
                                this::fillCreditDialog,
                                700L
                        );

                    } catch (Exception e) {
                        failBeforeSubmit(
                                "Não consegui abrir a janela de adicionar créditos. Nenhum crédito foi enviado."
                        );
                    }
                }
        );
    }

    private void fillCreditDialog() {
        if (finished
                || webView == null
                || submitAttempted) {
            return;
        }

        dialogAttempts++;

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "}" +

                        "function parentOf(el){" +
                        "if(!el)return null;" +
                        "if(el.parentElement)return el.parentElement;" +
                        "try{" +
                        "var root=el.getRootNode();" +
                        "if(root&&root.host)return root.host;" +
                        "}catch(e){}" +
                        "return null;" +
                        "}" +

                        "function deep(root){" +
                        "var result=[];" +
                        "var seen=[];" +
                        "function walk(node){" +
                        "if(!node)return;" +
                        "var children=[];" +
                        "try{children=node.querySelectorAll?node.querySelectorAll('*'):[];}catch(e){}" +
                        "for(var i=0;i<children.length;i++){" +
                        "var el=children[i];" +
                        "if(seen.indexOf(el)!==-1)continue;" +
                        "seen.push(el);" +
                        "result.push(el);" +
                        "try{if(el.shadowRoot)walk(el.shadowRoot);}catch(e){}" +
                        "}" +
                        "}" +
                        "walk(root);" +
                        "return result;" +
                        "}" +

                        "function meta(el){" +
                        "if(!el)return '';" +
                        "return n(" +
                        "(el.innerText||el.textContent||'')+' '+" +
                        "(el.getAttribute&&el.getAttribute('aria-label')||'')+' '+" +
                        "(el.getAttribute&&el.getAttribute('title')||'')" +
                        ");" +
                        "}" +

                        "function setValue(el,v){" +
                        "if(!el)return false;" +
                        "try{" +
                        "var tag=(el.tagName||'').toLowerCase();" +
                        "if(tag==='input'){" +
                        "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,v);" +
                        "}else if(tag==='textarea'){" +
                        "var setter2=Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set;" +
                        "setter2.call(el,v);" +
                        "}else if(el.isContentEditable){" +
                        "el.textContent=v;" +
                        "}else if('value' in el){" +
                        "el.value=v;" +
                        "}else{" +
                        "return false;" +
                        "}" +
                        "el.dispatchEvent(new Event('input',{bubbles:true,composed:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true,composed:true}));" +
                        "el.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true,composed:true,key:'0'}));" +
                        "return true;" +
                        "}catch(e){return false;}" +
                        "}" +

                        "var all=deep(document);" +

                        // Unique text that exists only inside the Add Credits screen.
                        "var anchor=null;" +
                        "var anchorLen=999999;" +
                        "for(var i=0;i<all.length;i++){" +
                        "var text=n(all[i].innerText||all[i].textContent);" +
                        "if(!text)continue;" +
                        "if(text.indexOf('não remover')!==-1" +
                        "||text.indexOf('nao remover')!==-1" +
                        "||text.indexOf('não digite um número negativo')!==-1" +
                        "||text.indexOf('nao digite um numero negativo')!==-1){" +
                        "if(text.length<anchorLen){" +
                        "anchor=all[i];" +
                        "anchorLen=text.length;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!anchor)return JSON.stringify({ok:false,reason:'credit_screen'});" +

                        // Smallest ancestor that is definitely the transfer form.
                        "var form=null;" +
                        "var p=anchor;" +
                        "for(var d=0;d<12&&p;d++,p=parentOf(p)){" +
                        "var text=n(p.innerText||p.textContent);" +
                        "if(text.indexOf('use casas decimais')!==-1" +
                        "&&text.indexOf('eu concordo em transferir')!==-1" +
                        "&&text.indexOf('adicionar')!==-1){" +
                        "form=p;" +
                        "break;" +
                        "}" +
                        "}" +

                        "if(!form)return JSON.stringify({ok:false,reason:'credit_form'});" +

                        "var nodes=deep(form);" +
                        "var minus=null;" +
                        "var plus=null;" +

                        // Find the - and + controls only inside this form.
                        "for(var j=0;j<nodes.length;j++){" +
                        "var m=meta(nodes[j]);" +
                        "if(!minus&&(m==='-'||m==='−'||m.indexOf('menos')!==-1||m.indexOf('decrement')!==-1))minus=nodes[j];" +
                        "if(!plus&&(m==='+'||m==='＋'||m.indexOf('mais')!==-1||m.indexOf('increment')!==-1))plus=nodes[j];" +
                        "}" +

                        "if(!minus||!plus)return JSON.stringify({ok:false,reason:'stepper'});" +

                        // Smallest common ancestor of - and +.
                        "var stepper=null;" +
                        "var mp=minus;" +
                        "for(var md=0;md<8&&mp&&!stepper;md++,mp=parentOf(mp)){" +
                        "var local=deep(mp);" +
                        "for(var q=0;q<local.length;q++){" +
                        "if(local[q]===plus){" +
                        "stepper=mp;" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!stepper)return JSON.stringify({ok:false,reason:'stepper_container'});" +

                        "var stepNodes=deep(stepper);" +
                        "var creditInput=null;" +

                        // Absolutely no fallback to another input on the page.
                        "for(var c=0;c<stepNodes.length;c++){" +
                        "var el=stepNodes[c];" +
                        "var tag=(el.tagName||'').toLowerCase();" +
                        "var type=(el.type||el.getAttribute&&el.getAttribute('type')||'').toLowerCase();" +
                        "if((tag==='input'||tag==='textarea'" +
                        "||el.getAttribute&&el.getAttribute('role')==='spinbutton'" +
                        "||el.isContentEditable)" +
                        "&&type!=='checkbox'" +
                        "&&type!=='radio'" +
                        "&&type!=='hidden'" +
                        "&&type!=='search'" +
                        "&&type!=='email'){" +
                        "creditInput=el;" +
                        "break;" +
                        "}" +
                        "}" +

                        "if(!creditInput)return JSON.stringify({ok:false,reason:'credit_input'});" +

                        "if(!setValue(creditInput,'50')){" +
                        "return JSON.stringify({ok:false,reason:'credit_set'});" +
                        "}" +

                        "var stored='';" +
                        "try{" +
                        "stored=('value' in creditInput)" +
                        "?String(creditInput.value||'')" +
                        ":String(creditInput.innerText||creditInput.textContent||'');" +
                        "}catch(e){}" +
                        "stored=stored.replace(/\\s+/g,'').replace(',','.');" +

                        "if(stored!=='50'&&stored!=='50.0'&&stored!=='50.00'){" +
                        "return JSON.stringify({ok:false,reason:'credit_verify',value:stored});" +
                        "}" +

                        "return JSON.stringify({ok:true});" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String decoded =
                            decodeJavascriptString(
                                    value
                            );

                    try {
                        JSONObject result =
                                new JSONObject(
                                        decoded
                                );

                        if (result.optBoolean(
                                "ok",
                                false
                        )) {
                            updateStatus(
                                    "Revenda: 50 preenchido no campo correto"
                            );

                            agreementAttempts = 0;

                            handler.postDelayed(
                                    this::markTransferAgreement,
                                    350L
                            );

                            return;
                        }

                        String reason =
                                result.optString(
                                        "reason",
                                        ""
                                );

                        if (dialogAttempts < 35) {
                            handler.postDelayed(
                                    this::fillCreditDialog,
                                    420L
                            );
                            return;
                        }

                        if ("credit_screen".equals(reason)
                                || "credit_form".equals(reason)) {
                            failBeforeSubmit(
                                    "O botão Adicionar Créditos foi aberto, mas a tela de transferência não ficou pronta. Nenhum crédito foi enviado."
                            );
                        } else if ("stepper".equals(reason)
                                || "stepper_container".equals(reason)
                                || "credit_input".equals(reason)) {
                            failBeforeSubmit(
                                    "Não encontrei o campo de créditos entre os botões − e +. Nenhum crédito foi enviado."
                            );
                        } else {
                            failBeforeSubmit(
                                    "Encontrei o campo correto de créditos, mas não consegui confirmar o valor 50. Nenhum crédito foi enviado."
                            );
                        }

                    } catch (Exception e) {
                        if (dialogAttempts < 35) {
                            handler.postDelayed(
                                    this::fillCreditDialog,
                                    420L
                            );
                        } else {
                            failBeforeSubmit(
                                    "Não consegui preparar a tela de adicionar créditos. Nenhum crédito foi enviado."
                            );
                        }
                    }
                }
        );
    }

    private void markTransferAgreement() {
        if (finished
                || webView == null
                || submitAttempted) {
            return;
        }

        agreementAttempts++;

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "}" +

                        "function parentOf(el){" +
                        "if(!el)return null;" +
                        "if(el.parentElement)return el.parentElement;" +
                        "try{" +
                        "var root=el.getRootNode();" +
                        "if(root&&root.host)return root.host;" +
                        "}catch(e){}" +
                        "return null;" +
                        "}" +

                        "function deep(root){" +
                        "var result=[];" +
                        "var seen=[];" +
                        "function walk(node){" +
                        "if(!node)return;" +
                        "var children=[];" +
                        "try{children=node.querySelectorAll?node.querySelectorAll('*'):[];}catch(e){}" +
                        "for(var i=0;i<children.length;i++){" +
                        "var el=children[i];" +
                        "if(seen.indexOf(el)!==-1)continue;" +
                        "seen.push(el);" +
                        "result.push(el);" +
                        "try{if(el.shadowRoot)walk(el.shadowRoot);}catch(e){}" +
                        "}" +
                        "}" +
                        "walk(root);" +
                        "return result;" +
                        "}" +

                        "var all=deep(document);" +
                        "var agreement=null;" +
                        "var best=999999;" +

                        // Wait until React updates 0 credits -> 50 credits.
                        "for(var i=0;i<all.length;i++){" +
                        "var txt=n(all[i].innerText||all[i].textContent);" +
                        "if(!txt)continue;" +
                        "if((txt.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||txt.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&txt.length<best){" +
                        "agreement=all[i];" +
                        "best=txt.length;" +
                        "}" +
                        "}" +

                        "if(!agreement)return JSON.stringify({ok:false,reason:'waiting_50'});" +

                        "var checkbox=null;" +

                        "try{" +
                        "var forId=agreement.getAttribute&&agreement.getAttribute('for');" +
                        "if(forId)checkbox=document.getElementById(forId);" +
                        "}catch(e){}" +

                        "if(!checkbox){" +
                        "var p=agreement;" +
                        "for(var d=0;d<7&&p&&!checkbox;d++,p=parentOf(p)){" +
                        "var ctx=n(p.innerText||p.textContent);" +
                        "if(ctx.indexOf('eu concordo em transferir 50 créditos')===-1" +
                        "&&ctx.indexOf('eu concordo em transferir 50 creditos')===-1){" +
                        "continue;" +
                        "}" +

                        "var local=deep(p);" +
                        "for(var j=0;j<local.length;j++){" +
                        "var el=local[j];" +
                        "var type=(el.type||el.getAttribute&&el.getAttribute('type')||'').toLowerCase();" +
                        "var role=el.getAttribute&&el.getAttribute('role');" +
                        "if(type==='checkbox'||role==='checkbox'){" +
                        "checkbox=el;" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!checkbox)return JSON.stringify({ok:false,reason:'checkbox'});" +

                        "var ctype=(checkbox.type||checkbox.getAttribute&&checkbox.getAttribute('type')||'').toLowerCase();" +
                        "var role=checkbox.getAttribute&&checkbox.getAttribute('role');" +

                        "if(ctype==='checkbox'){" +
                        "if(!checkbox.checked){" +
                        "try{checkbox.click();}catch(e){}" +
                        "}" +

                        "if(!checkbox.checked){" +
                        "try{" +
                        "checkbox.checked=true;" +
                        "checkbox.dispatchEvent(new Event('input',{bubbles:true,composed:true}));" +
                        "checkbox.dispatchEvent(new Event('change',{bubbles:true,composed:true}));" +
                        "}catch(e){}" +
                        "}" +

                        "if(!checkbox.checked)return JSON.stringify({ok:false,reason:'checkbox_failed'});" +
                        "}else if(role==='checkbox'){" +
                        "if(checkbox.getAttribute('aria-checked')!=='true'){" +
                        "try{checkbox.click();}catch(e){}" +
                        "}" +
                        "if(checkbox.getAttribute('aria-checked')!=='true'){" +
                        "return JSON.stringify({ok:false,reason:'checkbox_failed'});" +
                        "}" +
                        "}" +

                        "return JSON.stringify({ok:true});" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String decoded =
                            decodeJavascriptString(
                                    value
                            );

                    try {
                        JSONObject result =
                                new JSONObject(
                                        decoded
                                );

                        if (result.optBoolean(
                                "ok",
                                false
                        )) {
                            updateStatus(
                                    "Revenda: autorização de 50 créditos marcada"
                            );

                            submitSearchAttempts = 0;

                            handler.postDelayed(
                                    this::submitCreditsOnce,
                                    350L
                            );

                            return;
                        }

                        if (agreementAttempts < 25) {
                            handler.postDelayed(
                                    this::markTransferAgreement,
                                    300L
                            );
                            return;
                        }

                        failBeforeSubmit(
                                "Os 50 créditos foram preenchidos no campo correto, mas não consegui marcar a autorização da transferência. Nenhum crédito foi enviado."
                        );

                    } catch (Exception e) {
                        if (agreementAttempts < 25) {
                            handler.postDelayed(
                                    this::markTransferAgreement,
                                    300L
                            );
                        } else {
                            failBeforeSubmit(
                                    "Os 50 créditos foram preenchidos no campo correto, mas não consegui marcar a autorização da transferência. Nenhum crédito foi enviado."
                            );
                        }
                    }
                }
        );
    }

    private void submitCreditsOnce() {
        if (finished
                || webView == null
                || submitAttempted) {
            return;
        }

        submitSearchAttempts++;

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "}" +

                        "var all=document.querySelectorAll('body *');" +
                        "var agreement=null;" +
                        "var best=999999;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var txt=n(all[i].innerText||all[i].textContent);" +
                        "if((txt.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||txt.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&txt.length<best){" +
                        "agreement=all[i];" +
                        "best=txt.length;" +
                        "}" +
                        "}" +

                        "if(!agreement)return 'not_ready';" +

                        "var container=agreement;" +
                        "var button=null;" +

                        "for(var d=0;d<8&&container&&!button;d++,container=container.parentElement){" +
                        "var buttons=container.querySelectorAll('button,a,[role=button]');" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var b=buttons[j];" +
                        "var bt=n(b.innerText||b.textContent);" +

                        // Final button is 'Adicionar →'. Never accept 'Adicionar Créditos'.
                        "if(bt.indexOf('adicionar')===0" +
                        "&&bt.indexOf('créditos')===-1" +
                        "&&bt.indexOf('creditos')===-1){" +
                        "button=b;" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!button)return 'not_ready';" +

                        "var disabled=false;" +
                        "try{" +
                        "disabled=!!button.disabled" +
                        "||button.getAttribute('aria-disabled')==='true'" +
                        "||button.hasAttribute('disabled');" +
                        "}catch(e){}" +

                        "return disabled?'disabled':'ready';" +
                        "})();";

        webView.evaluateJavascript(
                script,
                value -> {
                    String result =
                            decodeJavascriptString(
                                    value
                            );

                    if ("ready".equals(result)) {
                        armAndClickFinalSubmit();
                        return;
                    }

                    if (submitSearchAttempts < 24) {
                        handler.postDelayed(
                                this::submitCreditsOnce,
                                350L
                        );
                    } else {
                        failBeforeSubmit(
                                "Os 50 créditos foram preenchidos e a autorização marcada, mas o botão final Adicionar não ficou disponível. Nenhum clique final foi feito."
                        );
                    }
                }
        );
    }

    private void armAndClickFinalSubmit() {
        if (finished
                || webView == null
                || submitAttempted) {
            return;
        }

        submitAttempted =
                true;

        prefs.edit()
                .putBoolean(
                        "reseller_submit_attempted_" + contactKey,
                        true
                )
                .putLong(
                        "reseller_submit_time_" + contactKey,
                        System.currentTimeMillis()
                )
                .putString(
                        "reseller_submit_email_" + contactKey,
                        email
                )
                .commit();

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "}" +

                        "var all=document.querySelectorAll('body *');" +
                        "var agreement=null;" +
                        "var best=999999;" +

                        "for(var i=0;i<all.length;i++){" +
                        "var txt=n(all[i].innerText||all[i].textContent);" +
                        "if((txt.indexOf('eu concordo em transferir 50 créditos')!==-1" +
                        "||txt.indexOf('eu concordo em transferir 50 creditos')!==-1)" +
                        "&&txt.length<best){" +
                        "agreement=all[i];" +
                        "best=txt.length;" +
                        "}" +
                        "}" +

                        "if(!agreement)return 'missing';" +

                        "var container=agreement;" +
                        "var button=null;" +

                        "for(var d=0;d<8&&container&&!button;d++,container=container.parentElement){" +
                        "var buttons=container.querySelectorAll('button,a,[role=button]');" +
                        "for(var j=0;j<buttons.length;j++){" +
                        "var b=buttons[j];" +
                        "var bt=n(b.innerText||b.textContent);" +
                        "if(bt.indexOf('adicionar')===0" +
                        "&&bt.indexOf('créditos')===-1" +
                        "&&bt.indexOf('creditos')===-1){" +
                        "button=b;" +
                        "break;" +
                        "}" +
                        "}" +
                        "}" +

                        "if(!button)return 'missing';" +

                        "try{" +
                        "button.click();" +
                        "return 'clicked';" +
                        "}catch(e){" +
                        "try{" +
                        "button.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window}));" +
                        "return 'clicked';" +
                        "}catch(e2){return 'error';}" +
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
                        uncertainAfterSubmit(
                                "A operação foi reservada para evitar duplicidade, mas não consegui confirmar o clique final."
                        );
                        return;
                    }

                    updateStatus(
                            "Revenda: Adicionar acionado uma única vez"
                    );

                    completionAttempts = 0;

                    handler.postDelayed(
                            this::waitForTransferCompletion,
                            700L
                    );
                }
        );
    }

    private void waitForTransferCompletion() {
        if (finished
                || webView == null) {
            return;
        }

        completionAttempts++;

        String script =
                "(function(){" +
                        "function n(v){" +
                        "return (v||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "}" +

                        "function cssShown(el){" +
                        "var p=el;" +
                        "while(p){" +
                        "try{" +
                        "var st=getComputedStyle(p);" +
                        "if(st.display==='none'||st.visibility==='hidden')return false;" +
                        "}catch(e){}" +
                        "p=p.parentElement;" +
                        "}" +
                        "return true;" +
                        "}" +

                        "var all=document.querySelectorAll('body *');" +
                        "for(var i=0;i<all.length;i++){" +
                        "var txt=n(all[i].innerText||all[i].textContent);" +
                        "if((txt.indexOf('não remover')!==-1||txt.indexOf('nao remover')!==-1)" +
                        "&&cssShown(all[i])){" +
                        "return 'open';" +
                        "}" +
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
                        successWithoutBalanceCheck();
                        return;
                    }

                    if (completionAttempts < 22) {
                        handler.postDelayed(
                                this::waitForTransferCompletion,
                                450L
                        );
                    } else {
                        uncertainAfterSubmit(
                                "O botão foi acionado uma única vez, mas a tela de transferência continuou aberta."
                        );
                    }
                }
        );
    }

    private void successWithoutBalanceCheck() {
        if (finished) return;

        finished =
                true;

        setState(
                "COMPLETE"
        );

        updateStatus(
                "Revenda: 50 créditos adicionados"
        );

        try {
            SecureStore secureStore =
                    new SecureStore(
                            context
                    );

            secureStore.put(
                    "linked_reseller_email_" + contactKey,
                    email
            );

        } catch (Exception ignored) {
        }

        prefs.edit()
                .putBoolean(
                        "reseller_submit_attempted_" + contactKey,
                        false
                )
                .apply();

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

        WhatsAppReply.send(
                context,
                whatsappNotification,
                successMessage
        );

        cleanup();
    }

    private void uncertainAfterSubmit(
            String reason
    ) {
        if (finished) return;

        finished =
                true;

        setState(
                "REVIEW_REQUIRED"
        );

        updateStatus(
                "Revenda: revisão necessária após tentativa única de +50"
        );

        WhatsAppReply.send(
                context,
                whatsappNotification,
                "⚠️ Houve uma tentativa única de adicionar os 50 créditos, " +
                        "mas não consegui confirmar o clique final.\n\n" +
                        "Por segurança, *não vou tentar novamente automaticamente*. " +
                        "Confira essa revenda no painel Sigma."
        );

        cleanup();
    }

    private void failBeforeSubmit(
            String reason
    ) {
        if (finished) return;

        finished =
                true;

        setState(
                "ERROR"
        );

        updateStatus(
                "Revenda: " + reason
        );

        if (MODE_FIND.equals(mode)) {
            MessageSettings.send(
                    context,
                    whatsappNotification,
                    prefs,
                    MessageSettings.MSG_RESELLER_NOT_FOUND,
                    MessageSettings.DEFAULT_RESELLER_NOT_FOUND,
                    MessageSettings.variables(
                            "MOTIVO",
                            reason
                    )
            );
        } else {
            WhatsAppReply.send(
                    context,
                    whatsappNotification,
                    "Não consegui concluir o cadastro da revenda automaticamente.\n\n" +
                            reason
            );
        }

        cleanup();
    }

    private void setState(
            String state
    ) {
        prefs.edit()
                .putString(
                        "reseller_state_" + contactKey,
                        state
                )
                .apply();
    }

    private void updateStatus(
            String value
    ) {
        prefs.edit()
                .putString(
                        "last_reseller_status",
                        value
                )
                .apply();
    }

    private boolean nearlyEqual(
            double a,
            double b
    ) {
        return Math.abs(a - b) < 0.001d;
    }

    private String formatCredits(
            double value
    ) {
        if (Math.abs(
                value - Math.rint(value)
        ) < 0.001d) {
            return String.valueOf(
                    (long) Math.rint(value)
            );
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
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

    private void cleanup() {
        handler.post(
                () -> {
                    try {
                        if (webView != null) {
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

                    BackgroundRuntime.clearResellerPending(
                            prefs
                    );

                    try {
                        new SecureStore(
                                context
                        ).remove(
                                "pending_reseller_email_" + contactKey
                        );
                    } catch (Exception ignored) {
                    }

                    MasterflixAutomationGate.release(
                            "reseller"
                    );

                    synchronized (LOCK) {
                        if (current == this) {
                            current =
                                    null;
                        }
                    }
                }
        );
    }
}
