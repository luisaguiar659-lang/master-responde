package com.masterresponde.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ContextThemeWrapper;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Locale;

public final class MasterIboWebAutomation {
    private static final String BASE="https://gerenciaapp.top/";
    private static final String DASHBOARD="https://gerenciaapp.top/dashboard";
    private static final String CREATE="https://gerenciaapp.top/users/create";
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ArrayDeque<Task> QUEUE=new ArrayDeque<>();
    private static boolean processing=false;
    private MasterIboWebAutomation(){}

    public interface Callback{ void onSuccess(String message); void onError(String code,String message); }

    public static void activate(Context context,String app,String mac,String server,String m3u,Callback callback){
        if(context==null||callback==null)return;
        synchronized(QUEUE){
            QUEUE.add(new Task(context.getApplicationContext(),app,mac,server,m3u,callback));
            if(processing)return; processing=true;
        }
        MAIN.post(MasterIboWebAutomation::pump);
    }
    private static void pump(){
        Task t; synchronized(QUEUE){ t=QUEUE.poll(); if(t==null){processing=false;return;} }
        new Runner(t,MasterIboWebAutomation::pump).start();
    }
    private static final class Task{
        final Context context; final String app,mac,server,m3u; final Callback callback;
        Task(Context c,String a,String m,String s,String u,Callback cb){context=c;app=safe(a);mac=safe(m);server=safe(s);m3u=safe(u);callback=cb;}
        private static String safe(String s){return s==null?"":s.trim();}
    }

    private static final class Runner{
        final Task task; final Runnable after; final Handler handler=new Handler(Looper.getMainLooper());
        WebView web; String email="",password=""; boolean loginInjected=false,activateInjected=false,finished=false; int retry=0;
        Runner(Task t,Runnable a){task=t;after=a;}

        @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"}) void start(){
            try{
                SharedPreferences secure=secure(task.context);
                email=secure.getString("email",""); password=secure.getString("password","");
                if(email==null)email=""; if(password==null)password=""; email=email.trim();
                if(email.isEmpty()||password.isEmpty()){fail("CREDENTIALS_REQUIRED","Credenciais do painel MASTER IBO não configuradas.");return;}
                ContextThemeWrapper themed=new ContextThemeWrapper(task.context,android.R.style.Theme_Material_Light_NoActionBar);
                web=new WebView(themed);
                WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
                s.setUserAgentString(s.getUserAgentString()+" MasterIBO/1.2.3");
                CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
                web.setWebChromeClient(new WebChromeClient()); web.addJavascriptInterface(new Bridge(),"AutomationBridge");
                web.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                        String h=r==null||r.getUrl()==null?null:r.getUrl().getHost();
                        if(h==null||!("gerenciaapp.top".equals(h)||"www.gerenciaapp.top".equals(h))){fail("EXTERNAL_NAVIGATION_BLOCKED","Navegação externa bloqueada.");return true;} return false;
                    }
                    @Override public void onPageFinished(WebView v,String url){super.onPageFinished(v,url); page(url);}
                    @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){super.onReceivedError(v,r,e); if(r!=null&&r.isForMainFrame()&&!finished)fail("NETWORK_ERROR","Falha ao carregar o painel MASTER IBO.");}
                });
                handler.postDelayed(()->{if(!finished)fail("TIMEOUT","O painel MASTER IBO demorou demais para concluir a ativação.");},70000L);
                web.loadUrl(DASHBOARD+"?t="+System.currentTimeMillis());
            }catch(Exception e){fail("INIT_ERROR","Falha ao iniciar o motor MASTER IBO.");}
        }

        private SharedPreferences secure(Context c)throws Exception{
            MasterKey k=new MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            return EncryptedSharedPreferences.create(c,"master_ibo_secure",k,EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        }
        private void page(String url){
            if(finished||web==null)return; String u=url==null?"":url.toLowerCase(Locale.ROOT);
            if(u.contains("/users/create")){ if(!activateInjected){activateInjected=true; web.evaluateJavascript(Js.activate(task.mac,task.app,task.server,task.m3u),null);} return; }
            if(u.contains("/dashboard")||u.contains("/menu")||(u.contains("/users")&&!u.contains("/create"))){ loginInjected=false; activateInjected=false; web.loadUrl(CREATE+"?t="+System.currentTimeMillis()); return; }
            if((u.equals(BASE)||u.startsWith(BASE+"?")||u.contains("/login")||u.contains("/signin"))&&!loginInjected){ loginInjected=true; web.evaluateJavascript(Js.login(email,password),null); return; }
            handler.postDelayed(()->{if(!finished&&web!=null&&!loginInjected){loginInjected=true;web.loadUrl(BASE+"?t="+System.currentTimeMillis());}},1500L);
        }
        private boolean recoverable(String c){return "CREATE_FORM_NOT_FOUND".equals(c)||"MAC_INPUT_NOT_FOUND".equals(c)||"APP_SELECT_NOT_FOUND".equals(c)||"PLAYLIST_FORM_NOT_READY".equals(c)||"PLAYLIST_INPUT_NOT_FOUND".equals(c);}
        private void event(String raw){
            if(finished)return;
            try{
                JSONObject o=new JSONObject(raw); String type=o.optString("type","progress"),code=o.optString("code","EVENT"),message=o.optString("message","");
                if("ACTIVATE_SUBMITTED".equals(code)){ handler.postDelayed(()->success("Ativação enviada com sucesso."),1200L); return; }
                if("error".equals(type)){
                    if(recoverable(code)&&retry<3){retry++;activateInjected=false;handler.postDelayed(()->{if(!finished&&web!=null)web.loadUrl(CREATE+"?t="+System.currentTimeMillis());},1000L);return;}
                    fail(code,message.isEmpty()?"Falha no painel MASTER IBO.":message);
                }
            }catch(Exception e){fail("INVALID_MESSAGE","Resposta inválida do painel MASTER IBO.");}
        }
        private final class Bridge{ @JavascriptInterface public void postMessage(String raw){ MAIN.post(()->event(raw)); } }
        private void success(String m){ if(finished)return;finished=true;try{task.callback.onSuccess(m);}finally{cleanup();} }
        private void fail(String c,String m){ if(finished)return;finished=true;try{task.callback.onError(c,m);}finally{cleanup();} }
        private void cleanup(){
            handler.removeCallbacksAndMessages(null);
            if(web!=null){try{web.stopLoading();web.loadUrl("about:blank");web.removeJavascriptInterface("AutomationBridge");web.destroy();}catch(Exception ignored){}web=null;}
            MAIN.post(after);
        }
    }

    private static final class Js{
        static String q(String s){return JSONObject.quote(s==null?"":s);}
        static String helpers(){return "function post(t,c,m){try{AutomationBridge.postMessage(JSON.stringify({type:t,code:c,message:m}));}catch(e){}}function norm(s){return(s||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').toLowerCase().trim();}function setVal(e,v){if(!e)return;let p=Object.getPrototypeOf(e),d=Object.getOwnPropertyDescriptor(p,'value');if(d&&d.set)d.set.call(e,v);else e.value=v;e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}function clickReal(e){if(!e)return;['pointerdown','mousedown','pointerup','mouseup','click'].forEach(t=>{try{e.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,view:window}));}catch(x){}});}function labelText(e){let p=e.parentElement,n=0,o='';while(p&&n<4){o+=' '+(p.innerText||'');p=p.parentElement;n++;}return norm(o);}function selectText(s,t){let x=norm(t),o=[...s.options].find(a=>norm(a.textContent)===x)||[...s.options].find(a=>norm(a.textContent).includes(x));if(!o)return false;setVal(s,o.value);return true;}";}
        static String login(String email,String password){
            return "(function(){const E="+q(email)+",P="+q(password)+";"+helpers()+"let n=0,t=setInterval(()=>{n++;let a=[...document.querySelectorAll('input')],e=a.find(i=>i.type==='email')||a.find(i=>/email|e-mail/i.test(norm(i.placeholder)))||a.find(i=>/email/i.test(norm(i.name))),p=a.find(i=>i.type==='password')||a.find(i=>/senha|password/i.test(norm(i.placeholder)));if(!e||!p){if(n>=50){clearInterval(t);post('error','LOGIN_FIELDS_NOT_FOUND','Campos de login não encontrados.');}return;}clearInterval(t);setVal(e,E);setVal(p,P);let f=p.closest('form')||e.closest('form'),b=(f&&f.querySelector('button[type=submit]'))||[...document.querySelectorAll('button')].find(x=>norm(x.textContent)==='entrar');if(f&&typeof f.requestSubmit==='function')f.requestSubmit();else if(b)clickReal(b);else if(f)f.submit();else{post('error','LOGIN_BUTTON_NOT_FOUND','Botão Entrar não encontrado.');return;}post('progress','LOGIN_OK','Login enviado.');},400);})();true;";
        }
        static String activate(String mac,String app,String server,String m3u){
            return "(function(){const MAC="+q(mac)+",APP="+q(app)+",SERVER="+q(server)+",M3U="+q(m3u)+";"+helpers()+"let n=0,t=setInterval(()=>{n++;let f=document.querySelector('form'),ins=[...document.querySelectorAll('input')],sels=[...document.querySelectorAll('select')];if(!f||!ins.length){if(n>=50){clearInterval(t);post('error','CREATE_FORM_NOT_FOUND','Formulário de cadastro não encontrado.');}return;}let mi=ins.find(i=>norm(i.placeholder).includes('00:00:00:00:00:00'))||ins.find(i=>labelText(i).includes('mac do dispositivo'));if(!mi){clearInterval(t);post('error','MAC_INPUT_NOT_FOUND','Campo MAC não encontrado.');return;}let as=sels.find(s=>[...s.options].some(o=>norm(o.textContent)===norm(APP)))||sels.find(s=>labelText(s).includes('app que o cliente'));if(!as){if(n>=50){clearInterval(t);post('error','APP_SELECT_NOT_FOUND','Lista de aplicativos não encontrada.');}return;}setVal(mi,MAC);if(!selectText(as,APP)){clearInterval(t);post('error','APP_OPTION_NOT_FOUND','Aplicativo não existe no painel.');return;}let title=ins.find(i=>norm(i.placeholder).includes('titulo geral'));if(title&&SERVER)setVal(title,SERVER);let ts=sels.find(s=>[...s.options].some(o=>norm(o.textContent)==='m3u8'));if(ts)selectText(ts,'M3U8');clearInterval(t);setTimeout(()=>{let all=[...document.querySelectorAll('input')],si=all.find(i=>norm(i.placeholder).includes('nome do servidor')),li=all.find(i=>norm(i.placeholder).includes('lista m3u8'))||all.find(i=>norm(i.placeholder).includes('m3u8')&&i!==si);if(!si||!li){post('error','PLAYLIST_FORM_NOT_READY','Campos da playlist M3U8 ainda não estão prontos.');return;}setVal(si,SERVER);setVal(li,M3U);setTimeout(()=>{let forms=[...document.querySelectorAll('form')],target=li.closest('form')||forms[forms.length-1]||f;if(target&&typeof target.reportValidity==='function'&&!target.reportValidity()){post('error','FORM_INVALID','O painel recusou algum campo obrigatório.');return;}let send=(target&&target.querySelector('button[type=submit]'))||[...document.querySelectorAll('button')].reverse().find(b=>norm(b.textContent)==='enviar');if(target&&typeof target.requestSubmit==='function')target.requestSubmit();else if(send)clickReal(send);else{post('error','SUBMIT_NOT_FOUND','Botão Enviar não encontrado.');return;}post('success','ACTIVATE_SUBMITTED','Ativação enviada.');},700);},700);},400);})();true;";
        }
    }
}
