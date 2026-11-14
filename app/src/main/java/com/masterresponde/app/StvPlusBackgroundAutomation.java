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
import android.webkit.JavascriptInterface;
import org.json.JSONObject;

/** Automação STV+ isolada; não utiliza estado nem classes de automação MasterFlix. */
public final class StvPlusBackgroundAutomation {
    private static final Object LOCK = new Object();
    private static StvPlusBackgroundAutomation active;
    private final Context context;
    private final Notification notification;
    private final SharedPreferences prefs;
    private final String mode;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private boolean finished;
    private boolean dashboard;
    private int attempts;
    private int copyAttempts;
    private int captureAttempts;
    private volatile String capturedCopyText = "";

    private StvPlusBackgroundAutomation(Context c, Notification n, SharedPreferences p, String m) {
        context=c.getApplicationContext(); notification=n; prefs=p; mode="adult".equals(m)?"adult":"standard";
    }
    public static boolean isRunning(){ synchronized(LOCK){return active!=null&&!active.finished;} }
    public static boolean start(Context c, Notification n, SharedPreferences p, String m){
        synchronized(LOCK){if(isRunning())return false;active=new StvPlusBackgroundAutomation(c,n,p,m);active.handler.post(active::begin);return true;}
    }
    private void begin(){
        try{
            status("STV+: abrindo painel");
            web=new WebView(context);
            WebSettings s=web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setLoadsImagesAutomatically(true);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            CookieManager cookies=CookieManager.getInstance();
            cookies.setAcceptCookie(true);
            if(android.os.Build.VERSION.SDK_INT>=21) cookies.setAcceptThirdPartyCookies(web,true);
            web.addJavascriptInterface(new Bridge(), "StvBridge");
            web.setWebViewClient(new WebViewClient(){
                @Override public void onPageFinished(WebView v,String u){super.onPageFinished(v,u);inspectPage();}
                @Override public void doUpdateVisitedHistory(WebView v,String u,boolean reload){super.doUpdateVisitedHistory(v,u,reload);inspectPage();}
            });
            web.loadUrl(StvPanelConfig.dashboardUrl(context));
            handler.postDelayed(()->{if(!finished&&!dashboard)fail("Não foi possível confirmar o acesso ao painel STV+. Verifique a conexão, o login e a verificação de segurança.");},30000L);
        }catch(Throwable e){fail("Não foi possível abrir o painel STV+.");}
    }
    private void inspectPage(){
        if(finished||web==null)return;
        web.evaluateJavascript("(function(){return location.href||'';})();",v->{
            String u=decode(v);
            if(u.contains("#/dashboard")){dashboard=true;status("STV+: painel aberto; selecionando modalidade");if(attempts==0)handler.postDelayed(this::selectMode,900L);}
            else if(u.contains("#/sign-in"))fillLogin();
        });
    }
    private void fillLogin(){
        String user=JSONObject.quote(StvPanelConfig.getUser(context)),pass=JSONObject.quote(StvPanelConfig.getPass(context));
        String js="(function(){var p=document.querySelector('input[type=password]'),u=document.querySelector('input[type=email],input[autocomplete=username],input[type=text]');if(!p)return 'no-form';var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;if(u&&!u.value){s.call(u,"+user+");u.dispatchEvent(new Event('input',{bubbles:true}));}if(!p.value){s.call(p,"+pass+");p.dispatchEvent(new Event('input',{bubbles:true}));}return 'filled';})();";
        web.evaluateJavascript(js,v->status("STV+: credenciais preenchidas quando o formulário foi identificado. Conclua manualmente qualquer desafio de segurança."));
    }
    private void selectMode(){
        if(finished||web==null)return;attempts++;
        String wanted=mode.equals("adult")?"TESTE + ADULTO":"TESTE SEM ADULTO";
        String q=JSONObject.quote(wanted);
        String js="(function(){function n(s){return(s||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').toUpperCase().replace(/\\s+/g,' ').trim();}var t=n("+q+"),a=Array.from(document.querySelectorAll('body *')).filter(e=>e.offsetWidth||e.offsetHeight||e.getClientRects().length).map(e=>({e,t:n(e.innerText||e.textContent||'')})).filter(x=>x.t===t||x.t.startsWith(t)).sort((x,y)=>x.t.length-y.t.length);if(!a.length){window.scrollBy(0,550);return 'missing';}var e=a[0].e.closest('button,a,[role=button],label,[tabindex]')||a[0].e;e.scrollIntoView({block:'center'});e.click();return 'clicked';})();";
        web.evaluateJavascript(js,v->{if("clicked".equals(decode(v)))handler.postDelayed(this::copyButton,2300L);else if(attempts<22)handler.postDelayed(this::selectMode,650L);else fail("A modalidade "+wanted+" não foi encontrada no painel.");});
    }
    private void copyButton(){
        if(finished||web==null)return;copyAttempts++;
        installCopyHook();
        String js="(function(){var a=Array.from(document.querySelectorAll('button,a,[role=button],*')).filter(e=>(e.offsetWidth||e.offsetHeight||e.getClientRects().length)&&(e.innerText||e.textContent||'').trim().toUpperCase()==='COPIAR');if(a.length){a.sort((x,y)=>(x.innerText||x.textContent||'').length-(y.innerText||y.textContent||'').length);var e=a[0].closest('button,a,[role=button]')||a[0];try{e.scrollIntoView({block:'center'});}catch(x){}e.click();return 'clicked';}window.scrollBy(0,600);return 'scroll';})();";
        web.evaluateJavascript(js,v->{if("clicked".equals(decode(v))){captureAttempts=0;handler.postDelayed(this::waitForCapturedText,350L);}else if(copyAttempts<25)handler.postDelayed(this::copyButton,500L);else fail("O botão COPIAR não foi encontrado.");});
    }
    private void installCopyHook(){
        if(finished||web==null)return;
        String js="(function(){try{if(window.__stvCopyHook)return 'ok';window.__stvCopyHook=true;"
                +"if(navigator.clipboard&&navigator.clipboard.writeText){var original=navigator.clipboard.writeText.bind(navigator.clipboard);"
                +"navigator.clipboard.writeText=function(t){try{window.StvBridge.copiedText(String(t||''));}catch(e){}return original(t);};}"
                +"document.addEventListener('copy',function(e){try{var t=e.clipboardData?e.clipboardData.getData('text/plain'):'';"
                +"if(!t&&window.getSelection)t=String(window.getSelection()||'');if(t)window.StvBridge.copiedText(t);}catch(x){}},true);"
                +"return 'ok';}catch(e){return 'error';}})();";
        web.evaluateJavascript(js,null);
    }
    private void waitForCapturedText(){
        if(finished)return;
        String value=capturedCopyText==null?"":capturedCopyText.trim();
        if(isValidGeneratedTest(value)){sendResult(value);return;}
        if(++captureAttempts<18){handler.postDelayed(this::waitForCapturedText,300L);}
        else extractResult();
    }
    private void sendResult(String result){
        if(finished)return;
        if(!isValidGeneratedTest(result)){
            extractResult();
            return;
        }
        finished=true;
        boolean sent=WhatsAppReply.send(context,notification,"✅ *TESTE STV+*\n⏳ Validade: *1 hora*\n\n"+result);
        EventLog.add(context, sent ? "RESPOSTA ENVIADA" : "FALHA", "STV+",
                "Geração de teste concluída", sent ? "TESTE ENVIADO" : "FALHA NO ENVIO");
        status(sent?"Teste STV+ gerado e enviado.":"Teste STV+ gerado, mas o WhatsApp não confirmou o envio.");
        cleanup();
    }

    private void extractResult(){
        if(finished||web==null)return;
        String js="(function(){var a=Array.from(document.querySelectorAll('textarea,input,pre,code,[contenteditable=true]')).filter(e=>e.offsetWidth||e.offsetHeight||e.getClientRects().length).map(e=>e.value||e.innerText||e.textContent||'').map(x=>(x||'').trim()).filter(x=>x.length>10);return a.sort((x,y)=>y.length-x.length)[0]||'';})();";
        web.evaluateJavascript(js,v->{String result=decode(v).trim();if(isValidGeneratedTest(result))sendResult(result);else fail("O painel não disponibilizou um teste completo. Verifique a modalidade e tente novamente.");});
    }

    private boolean isValidGeneratedTest(String value){
        if(value==null||value.trim().isEmpty())return false;
        String text=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT);
        boolean hasUrl=text.contains("http://")||text.contains("https://");
        boolean hasUser=text.contains("usuario")||text.contains("username")||text.contains("user:")||text.contains("login:");
        boolean hasPass=text.contains("senha")||text.contains("password")||text.contains("pass:");
        return hasUrl&&hasUser&&hasPass;
    }
    private void fail(String reason){if(finished)return;finished=true;EventLog.add(context,"FALHA","STV+","Geração de teste interrompida",reason);status(reason);WhatsAppReply.send(context,notification,"⚠️ STV+: "+reason);cleanup();}
    private void status(String s){prefs.edit().putString("last_stv_status",s).apply();}
    private String decode(String v){if(v==null||"null".equals(v))return"";try{Object o=new org.json.JSONTokener(v).nextValue();return o==null?"":o.toString();}catch(Exception e){return v;}}
    private void cleanup(){handler.post(()->{try{if(web!=null){web.removeJavascriptInterface("StvBridge");web.stopLoading();web.loadUrl("about:blank");web.destroy();web=null;}}catch(Throwable ignored){}synchronized(LOCK){if(active==this)active=null;}TestAutomationQueue.onStvFinished();});}
    public class Bridge {
        @JavascriptInterface
        public void copiedText(String value) {
            if (value != null && !value.trim().isEmpty()) capturedCopyText = value.trim();
        }
    }
}
