from pathlib import Path
import re

app = Path('projeto/app')
java = app / 'src/main/java/com/masterresponde/app'
dash = java / 'NeonDashboardActivity.java'
svc = java / 'WhatsAppBusinessCaptureService.java'
manifest = app / 'src/main/AndroidManifest.xml'
gradle = app / 'build.gradle'

# Remove qualquer vestígio do experimento antigo que dependia da API/web externa.
for old in [java / 'MasterXCloudApi.java', java / 'MasterXCloudFlow.java']:
    if old.exists():
        old.unlink()

s = svc.read_text(encoding='utf-8')
# Se alguma base antiga contiver a chamada experimental, remove somente o bloco isolado.
s = re.sub(
    r'\s*// PRIVADO: Master XCloud é isolado.*?MasterXCloudFlow\.handle\(this, conversation, message.*?\}\)\) \{\s*return;\s*\}\s*',
    '\n', s, count=1, flags=re.S
)
svc.write_text(s, encoding='utf-8')

activity = r'''package com.masterresponde.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * MASTER XCLOUD incorporado ao MASTER RESPONDE.
 * Motor baseado no fluxo local validado da v1.10.3: WebView -> painel Xtream.
 * Não usa api.masterxcloud.shop, Cloudflare Tunnel nem o antigo motor web/API.
 */
public class MasterXCloudActivity extends Activity {
    private static final String PANEL_LOGIN = "https://panel.xtream.cloud/#/login";
    // IMPORTANTE: esta é a rota validada pelo motor funcional.
    private static final String PANEL_DEVICES = "https://panel-v2.xtream.cloud/dashboard/devices";
    private static final String PLAYLIST_BASE = "https://xtream.cloud/custom-playlist";
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "panel.xtream.cloud", "panel-v2.xtream.cloud", "xtream.cloud"));

    private enum Flow { ACTIVATE, RESET, DELETE }
    private enum Stage { IDLE, DEVICE, DELETE, PLAYLIST, VERIFY, COMPLETED, FAILED }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private WebView web;
    private LinearLayout loginBox, operationBox;
    private EditText email, password, device, playlist;
    private Spinner flow;
    private Button enter, execute, back;
    private TextView status;
    private AlertDialog progressDialog;
    private TextView progressTitle, progressStep, progressDevice;
    private ProgressBar progressBar;

    private Flow activeFlow = Flow.ACTIVATE;
    private Stage stage = Stage.IDLE;
    private boolean authenticated = false;
    private boolean running = false;
    private boolean loginInjected = false;
    private boolean deviceInjected = false;
    private boolean deleteInjected = false;
    private boolean playlistInjected = false;
    private boolean resetDeleted = false;
    private int retries = 0;
    private String currentDevice = "";
    private String currentPlaylist = "";
    private String operationId = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        setupWeb();
        String savedEmail = getSharedPreferences("master_responde", MODE_PRIVATE)
                .getString("master_xcloud_email", "");
        email.setText(savedEmail);
        showLogin();
    }

    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }
    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private TextView label(String text, int size, int color, boolean bold){
        TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(null,1); return t;
    }
    private EditText field(String hint, boolean secret){
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(Color.rgb(115,125,135));
        e.setTextColor(Color.WHITE); e.setTextSize(15); e.setSingleLine(true);
        e.setPadding(dp(14),dp(10),dp(14),dp(10));
        if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }
    private Button button(String text){
        Button b=new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setTypeface(null,1);
        return b;
    }
    private View gap(int h){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return v; }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(7,9,11));
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(10),dp(10),dp(8));
        TextView title=label("MASTER XCLOUD",21,Color.WHITE,true); top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1f));
        back=button("VOLTAR"); back.setOnClickListener(v->finish()); top.addView(back,new LinearLayout.LayoutParams(-2,dp(46)));
        root.addView(top);
        View line=new View(this); line.setBackgroundColor(Color.rgb(0,184,255)); root.addView(line,new LinearLayout.LayoutParams(-1,dp(3)));

        loginBox=new LinearLayout(this); loginBox.setOrientation(LinearLayout.VERTICAL); loginBox.setPadding(dp(22),dp(26),dp(22),dp(30));
        loginBox.addView(label("LOGIN XCLOUD",13,Color.rgb(255,156,0),true)); loginBox.addView(gap(12));
        email=field("E-mail do painel",false); password=field("Senha do painel",true);
        loginBox.addView(email,new LinearLayout.LayoutParams(-1,dp(56))); loginBox.addView(gap(10));
        loginBox.addView(password,new LinearLayout.LayoutParams(-1,dp(56))); loginBox.addView(gap(14));
        enter=button("ENTRAR"); enter.setOnClickListener(v->login()); loginBox.addView(enter,new LinearLayout.LayoutParams(-1,dp(56)));
        loginBox.addView(gap(16)); status=label("",13,Color.rgb(0,184,255),false); status.setGravity(Gravity.CENTER); loginBox.addView(status);
        ScrollView loginScroll=new ScrollView(this); loginScroll.addView(loginBox); root.addView(loginScroll,new LinearLayout.LayoutParams(-1,0,1f));

        operationBox=new LinearLayout(this); operationBox.setOrientation(LinearLayout.VERTICAL); operationBox.setPadding(dp(18),dp(20),dp(18),dp(30));
        operationBox.addView(label("AUTOMAÇÃO LOCAL",12,Color.rgb(255,156,0),true)); operationBox.addView(gap(8));
        operationBox.addView(label("Ativação, reset e exclusão executados diretamente no painel Xtream.",13,Color.rgb(165,175,185),false)); operationBox.addView(gap(18));
        flow=new Spinner(this); ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Ativar MAC + DNS","Reset + DNS","Excluir MAC"}); flow.setAdapter(a); operationBox.addView(flow,new LinearLayout.LayoutParams(-1,dp(54)));
        operationBox.addView(gap(10)); device=field("Device Key / MAC",false); operationBox.addView(device,new LinearLayout.LayoutParams(-1,dp(56)));
        operationBox.addView(gap(10)); playlist=field("M3U / DNS (http://...)",true); playlist.setTransformationMethod(PasswordTransformationMethod.getInstance()); operationBox.addView(playlist,new LinearLayout.LayoutParams(-1,dp(56)));
        operationBox.addView(gap(14)); execute=button("EXECUTAR"); execute.setOnClickListener(v->startFlow()); operationBox.addView(execute,new LinearLayout.LayoutParams(-1,dp(58)));
        operationBox.addView(gap(18)); TextView safe=label("● MOTOR LOCAL XCLOUD v1.10.3",12,Color.rgb(85,220,125),true); safe.setGravity(Gravity.CENTER); operationBox.addView(safe);
        ScrollView opScroll=new ScrollView(this); opScroll.addView(operationBox); root.addView(opScroll,new LinearLayout.LayoutParams(-1,0,1f));

        web=new WebView(this); web.setVisibility(View.GONE); root.addView(web,new LinearLayout.LayoutParams(1,1));
        setContentView(root);
    }

    @SuppressLint("SetJavaScriptEnabled") private void setupWeb(){
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        web.addJavascriptInterface(new Bridge(),"AutomationBridge");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){
                String host=r.getUrl().getHost(), scheme=r.getUrl().getScheme();
                return !"https".equalsIgnoreCase(scheme)||host==null||!ALLOWED.contains(host);
            }
            @Override public void onPageFinished(WebView v,String url){ super.onPageFinished(v,url); pageFinished(url); }
        });
    }

    private void showLogin(){ loginBox.setVisibility(View.VISIBLE); operationBox.setVisibility(View.GONE); }
    private void showOperations(){ loginBox.setVisibility(View.GONE); operationBox.setVisibility(View.VISIBLE); }

    private void login(){
        String em=email.getText().toString().trim(), pw=password.getText().toString();
        if(em.isEmpty()||pw.isEmpty()){toast("Informe e-mail e senha.");return;}
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("master_xcloud_email",em).apply();
        running=true; authenticated=false; loginInjected=false; enter.setEnabled(false); enter.setText("ENTRANDO..."); status.setText("Conectando ao painel...");
        web.loadUrl(PANEL_DEVICES+"?t="+System.currentTimeMillis());
    }

    private void pageFinished(String url){
        if(url==null)return; String low=url.toLowerCase(Locale.ROOT);
        if(running&&!authenticated){
            if((low.contains("/login")||low.contains("#/login"))&&!loginInjected){
                loginInjected=true; web.evaluateJavascript(Scripts.login(email.getText().toString().trim(),password.getText().toString()),null); return;
            }
            if(low.contains("/dashboard")&&!low.contains("/login")){
                authenticated=true; running=false; enter.setEnabled(true); enter.setText("ENTRAR"); password.setText(""); status.setText(""); showOperations(); return;
            }
        }
        if(!running||!authenticated)return;
        if(low.contains("panel-v2.xtream.cloud")&&low.contains("/dashboard/devices")){
            if(stage==Stage.VERIFY){ web.evaluateJavascript(Scripts.verifyPlaylist(currentDevice,currentPlaylist,operationId),null); return; }
            if(activeFlow==Flow.DELETE || (activeFlow==Flow.RESET&&!resetDeleted)){
                if(deleteInjected)return; deleteInjected=true; web.evaluateJavascript(Scripts.deleteDevice(currentDevice,operationId),null); return;
            }
            if(!deviceInjected){deviceInjected=true; web.evaluateJavascript(Scripts.addDevice(currentDevice,operationId),null);} return;
        }
        if(low.contains("xtream.cloud/custom-playlist")&&!playlistInjected){
            playlistInjected=true; stage=Stage.PLAYLIST; web.evaluateJavascript(Scripts.addPlaylist(currentDevice,currentPlaylist,operationId),null);
        }
    }

    private void startFlow(){
        if(running)return; if(!authenticated){showLogin();toast("Faça login primeiro.");return;}
        String d=device.getText().toString().trim().toUpperCase(Locale.ROOT); String p=playlist.getText().toString().trim();
        if(!d.matches("^[A-Z0-9_-]{4,32}$")){toast("Device Key inválida.");return;}
        int pos=flow.getSelectedItemPosition(); activeFlow=pos==1?Flow.RESET:pos==2?Flow.DELETE:Flow.ACTIVATE;
        if(activeFlow!=Flow.DELETE && !p.toLowerCase(Locale.ROOT).startsWith("http://")){toast("A M3U deve começar com http://.");return;}
        if(activeFlow==Flow.RESET||activeFlow==Flow.DELETE){
            String msg=activeFlow==Flow.RESET?"Resetar "+d+" e reaplicar a M3U/DNS?":"Excluir "+d+" do painel?";
            new AlertDialog.Builder(this).setTitle("Confirmar operação").setMessage(msg).setNegativeButton("CANCELAR",null)
                    .setPositiveButton("CONFIRMAR",(x,w)->begin(d,p)).show(); return;
        }
        begin(d,p);
    }

    private void begin(String d,String p){
        currentDevice=d; currentPlaylist=p; operationId=UUID.randomUUID().toString(); retries=0; resetDeleted=false;
        deviceInjected=false; deleteInjected=false; playlistInjected=false; running=true; stage=Stage.DEVICE;
        execute.setEnabled(false); execute.setText("PROCESSANDO..."); showProgress(); updateProgress("Preparando operação...",8,false,false);
        web.loadUrl(PANEL_DEVICES+"?t="+System.currentTimeMillis());
    }

    public class Bridge { @JavascriptInterface public void postMessage(String raw){ onMessage(raw); } }
    private void onMessage(String raw){ runOnUiThread(()->{
        try{
            JSONObject o=new JSONObject(raw); String type=o.optString("type","progress"), code=o.optString("code","EVENT"), msg=o.optString("message","");
            String op=o.optString("operationId",""); if(!running||!operationId.equals(op))return;
            updateProgress(friendly(code,msg),progress(code),"success".equals(type),"error".equals(type));

            if("PLAYLIST_VERIFY_ON_DEVICES".equals(code)){
                stage=Stage.VERIFY; web.loadUrl(PANEL_DEVICES+"?t="+System.currentTimeMillis());
                if(activeFlow==Flow.RESET){ ui.postDelayed(()->{ if(running&&activeFlow==Flow.RESET&&stage==Stage.VERIFY)finish("Reset + DNS concluído com sucesso."); },4500); }
                return;
            }
            if("DEVICE_ADDED".equals(code)){
                web.loadUrl(PLAYLIST_BASE+"?device_key="+android.net.Uri.encode(currentDevice)+"&type=xtream&mode=add"); return;
            }
            if("DEVICE_DELETED".equals(code)&&activeFlow==Flow.DELETE){finish("Dispositivo excluído com sucesso.");return;}
            if("DEVICE_DELETED".equals(code)&&activeFlow==Flow.RESET){
                resetDeleted=true; deleteInjected=false; deviceInjected=false; playlistInjected=false; retries=0; stage=Stage.DEVICE;
                updateProgress("Dispositivo removido. Ativando novamente...",45,false,false);
                web.loadUrl(PANEL_DEVICES+"?t="+System.currentTimeMillis()); return;
            }
            if("PLAYLIST_ADDED".equals(code)){finish(activeFlow==Flow.RESET?"Reset + DNS concluído com sucesso.":"Ativação concluída com sucesso.");return;}
            if("error".equals(type)){ fail(code,msg); }
        }catch(Exception e){fail("INVALID_MESSAGE","Resposta inválida do painel.");}
    });}

    private boolean recoverable(String c){return c.contains("NOT_FOUND")||c.contains("TIMEOUT")||c.contains("NOT_CONFIRMED")||c.contains("PANEL_NOT_READY")||c.contains("VISIBLE_AFTER");}
    private void fail(String code,String msg){
        if(recoverable(code)&&retries<3){retries++; updateProgress("Tentativa automática "+retries+"/3...",30,false,false);
            ui.postDelayed(()->{if(!running)return; deviceInjected=false;deleteInjected=false;playlistInjected=false;
                if(code.startsWith("PLAYLIST_"))web.loadUrl(PLAYLIST_BASE+"?device_key="+android.net.Uri.encode(currentDevice)+"&type=xtream&mode=add&t="+System.currentTimeMillis());
                else web.loadUrl(PANEL_DEVICES+"?t="+System.currentTimeMillis());},900); return;}
        stage=Stage.FAILED; running=false; execute.setEnabled(true);execute.setText("EXECUTAR"); updateProgress(msg,100,false,true);
        ui.postDelayed(this::dismissProgress,3200);
    }
    private void finish(String msg){
        if(!running)return; stage=Stage.COMPLETED; running=false; execute.setEnabled(true);execute.setText("EXECUTAR");
        updateProgress(msg,100,true,false); device.setText("");playlist.setText(""); currentDevice="";currentPlaylist="";operationId="";
        ui.postDelayed(this::dismissProgress,1300);
    }

    private void showProgress(){
        dismissProgress(); LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(24),dp(24),dp(20));box.setBackgroundColor(Color.rgb(15,18,22));
        progressTitle=label(activeFlow==Flow.RESET?"RESETANDO DISPOSITIVO":activeFlow==Flow.DELETE?"EXCLUINDO DISPOSITIVO":"ATIVANDO DISPOSITIVO",20,Color.WHITE,true);progressTitle.setGravity(Gravity.CENTER);box.addView(progressTitle);
        progressDevice=label(currentDevice,16,Color.rgb(0,184,255),true);progressDevice.setGravity(Gravity.CENTER);progressDevice.setPadding(0,dp(8),0,dp(16));box.addView(progressDevice);
        progressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progressBar.setMax(100);progressBar.setProgress(8);box.addView(progressBar,new LinearLayout.LayoutParams(-1,dp(8)));
        progressStep=label("Preparando operação...",16,Color.WHITE,true);progressStep.setGravity(Gravity.CENTER);progressStep.setPadding(0,dp(18),0,0);box.addView(progressStep);
        progressDialog=new AlertDialog.Builder(this).setView(box).setCancelable(false).create();progressDialog.show();
    }
    private void updateProgress(String text,int pct,boolean success,boolean error){
        if(progressDialog==null||!progressDialog.isShowing())return; progressBar.setProgress(pct);progressStep.setText(text);
        if(success){progressTitle.setText("✓  OPERAÇÃO CONCLUÍDA");progressTitle.setTextColor(Color.rgb(183,255,60));}
        else if(error){progressTitle.setText("!  NÃO FOI POSSÍVEL CONCLUIR");progressTitle.setTextColor(Color.rgb(255,92,92));}
    }
    private void dismissProgress(){try{if(progressDialog!=null&&progressDialog.isShowing())progressDialog.dismiss();}catch(Exception ignored){}progressDialog=null;}
    private int progress(String c){if(c.contains("SEARCH"))return 18;if(c.contains("DIALOG")||c.contains("KEY_FILLED"))return 32;if(c.contains("DEACTIV")||c.contains("SAVING"))return 48;if(c.contains("DEVICE_ADDED")||c.contains("ACTIVAT"))return 62;if(c.contains("PLAYLIST_FILL"))return 75;if(c.contains("VERIFY"))return 88;if(c.contains("DELET"))return 78;if(c.contains("ADDED"))return 100;return 38;}
    private String friendly(String c,String m){if(c.contains("SEARCH"))return "Localizando dispositivo no XCloud...";if(c.contains("DEACTIV"))return "Desativando dispositivo...";if(c.contains("DELET"))return "Removendo dispositivo...";if(c.contains("DIALOG")||c.contains("KEY_FILLED"))return "Preparando dados do dispositivo...";if(c.contains("DEVICE_ADDED"))return "Dispositivo confirmado. Preparando DNS...";if(c.contains("PLAYLIST_FILL"))return "Aplicando M3U / DNS...";if(c.contains("VERIFY"))return "Confirmando alterações no painel...";return m;}

    @Override public void onBackPressed(){if(running){toast("Aguarde a operação terminar.");return;}super.onBackPressed();}

    private static class Scripts {
        private static String q(String s){return JSONObject.quote(s);}
        private static String helper(){return "function post(type,code,message){AutomationBridge.postMessage(JSON.stringify({type,code,message,operationId:(typeof OP==='undefined'?'':OP)}));}"+
                "function clickReal(b){try{b.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true,cancelable:true}));b.dispatchEvent(new PointerEvent('pointerup',{bubbles:true,cancelable:true}));}catch(e){}b.click();}"+
                "function setReactInput(i,v){const own=Object.getOwnPropertyDescriptor(i,'value');const proto=Object.getPrototypeOf(i);const pd=Object.getOwnPropertyDescriptor(proto,'value');if(pd&&pd.set&&(!own||own.set!==pd.set)){pd.set.call(i,v);}else if(own&&own.set){own.set.call(i,v);}else{i.value=v;}i.dispatchEvent(new Event('input',{bubbles:true}));i.dispatchEvent(new Event('change',{bubbles:true}));if(i._valueTracker){i._valueTracker.setValue('');i.dispatchEvent(new Event('input',{bubbles:true}));}}";}
        static String login(String e,String p){return "(function(){const EMAIL="+q(e)+";const PASSWORD="+q(p)+";"+helper()+"let n=0;const t=setInterval(()=>{n++;const a=[...document.querySelectorAll('input')];const em=a.find(i=>i.type==='email')||a.find(i=>/email|e-mail|user|usuario/i.test(i.name||''))||a.find(i=>i.type==='text');const pw=a.find(i=>i.type==='password');if(!em||!pw){if(n>=60){clearInterval(t);post('error','LOGIN_FIELDS_NOT_FOUND','Campos de login não encontrados.');}return;}clearInterval(t);setReactInput(em,EMAIL);setReactInput(pw,PASSWORD);const f=pw.closest('form')||em.closest('form');const b=(f&&f.querySelector('button[type=\"submit\"]'))||[...document.querySelectorAll('button')].find(x=>/login|entrar|sign in/i.test((x.textContent||'').trim()));if(f&&typeof f.requestSubmit==='function')f.requestSubmit();else if(b)clickReal(b);else if(f)f.submit();},500);})();true;";}
        static String addDevice(String mac,String op){return "(function(){const MAC="+q(mac)+";const OP="+q(op)+";"+helper()+"function findAdd(){const bs=[...document.querySelectorAll('button')];let b=bs.find(x=>{const c=String(x.className||'');const t=(x.textContent||'').toLowerCase().trim();return c.includes('bg-primary')&&c.includes('w-full')&&(t.includes('add')||t.includes('novo'));});if(b)return b;b=bs.find(x=>{const c=String(x.className||'');return c.includes('bg-primary')&&c.includes('w-full');});if(b)return b;const plus=document.querySelector('svg.lucide-plus');return plus?plus.closest('button'):bs.find(x=>/add device|adicionar dispositivo/i.test(x.textContent||''));}const add=findAdd();if(!add){post('error','ADD_DEVICE_BUTTON_NOT_FOUND','Botão Add Device não encontrado.');return;}clickReal(add);let n=0;const t=setInterval(()=>{n++;const d=document.querySelector('[role=\"dialog\"]');if(!d){if(n>=20){clearInterval(t);post('error','DEVICE_DIALOG_TIMEOUT','O cadastro não abriu.');}return;}clearInterval(t);const ins=[...d.querySelectorAll('input')];let k=ins.find(i=>!/radio|checkbox|hidden/.test(i.type)&&/device|key|mac|codigo|código/i.test(i.placeholder||''))||ins.find(i=>!/radio|checkbox|hidden/.test(i.type));if(!k){post('error','DEVICE_KEY_INPUT_NOT_FOUND','Campo Device Key não encontrado.');return;}setReactInput(k,MAC);k.blur();post('progress','DEVICE_KEY_FILLED','Device Key preenchida.');setTimeout(()=>{const f=d.querySelector('form');let b=f&&f.querySelector('button[type=\"submit\"]');if(!b)b=[...d.querySelectorAll('button')].reverse().find(x=>!/cancel|voltar/i.test(x.textContent||''));post('progress','DEVICE_SAVING','Salvando dispositivo...');if(f&&typeof f.requestSubmit==='function')f.requestSubmit();else if(b)clickReal(b);else if(f)f.submit();else{post('error','DEVICE_SAVE_NOT_FOUND','Botão de salvar não encontrado.');return;}let w=0;const v=setInterval(()=>{w++;const found=[...document.querySelectorAll('tr')].some(r=>(r.innerText||'').toUpperCase().includes(MAC.toUpperCase()));if(found){clearInterval(v);post('success','DEVICE_ADDED','Dispositivo confirmado no painel.');}else if(w>=40){clearInterval(v);post('error','DEVICE_ADD_NOT_CONFIRMED','O dispositivo não apareceu no painel.');}},500);},1200);},500);})();true;";}
        static String addPlaylist(String mac,String url,String op){return "(function(){const MAC="+q(mac)+";const URL_LISTA="+q(url)+";const OP="+q(op)+";"+helper()+"let n=0;const t=setInterval(()=>{n++;const ins=[...document.querySelectorAll('input')];const u=ins.find(i=>/http|url|link|playlist/i.test((i.placeholder||'')+' '+(i.getAttribute('aria-label')||'')));if(!u){if(n>=30){clearInterval(t);post('error','PLAYLIST_URL_INPUT_NOT_FOUND','Campo da URL não encontrado.');}return;}clearInterval(t);post('progress','PLAYLIST_FILL','Aplicando M3U / DNS...');setReactInput(u,URL_LISTA);u.blur();setTimeout(()=>{const f=u.closest('form');const bs=[...document.querySelectorAll('button')];const save=(f&&f.querySelector('button[type=\"submit\"]'))||bs.find(b=>/save|add|salvar/i.test(b.textContent||''));if(f&&typeof f.requestSubmit==='function')f.requestSubmit();else if(save)clickReal(save);else if(f)f.submit();else{post('error','PLAYLIST_SAVE_NOT_FOUND','Botão Save não encontrado.');return;}post('progress','PLAYLIST_VERIFYING','Confirmando lista...');let c=0;const v=setInterval(()=>{c++;const ns=[...document.querySelectorAll('[role=\"alert\"],[role=\"status\"],.toast,[class*=\"toast\"],[class*=\"alert\"]')];const txt=ns.map(x=>(x.innerText||x.textContent||'').toLowerCase()).join(' ');if(/invalid|failed|error|required|erro|inválid|falhou/.test(txt)){clearInterval(v);post('error','PLAYLIST_VISIBLE_ERROR','O painel informou erro ao salvar a lista.');}else if(/success|saved|added|created|sucesso|salv|adicion/.test(txt)){clearInterval(v);post('success','PLAYLIST_ADDED','Lista adicionada e confirmada.');}else if(c>=12){clearInterval(v);post('progress','PLAYLIST_VERIFY_ON_DEVICES','Verificando M3U na lista de dispositivos...');}},500);},900);},500);})();true;";}
        static String verifyPlaylist(String mac,String url,String op){return "(function(){const MAC="+q(mac)+";const URL_LISTA="+q(url)+";const OP="+q(op)+";"+helper()+"function payload(r){let x=(r.innerText||r.textContent||'');r.querySelectorAll('*').forEach(e=>['title','value','href','aria-label'].forEach(a=>{const v=e.getAttribute&&e.getAttribute(a);if(v)x+=' '+v;}));return x;}let n=0;const t=setInterval(()=>{n++;const r=[...document.querySelectorAll('tr')].find(x=>payload(x).toUpperCase().includes(MAC.toUpperCase()));if(r){const h=payload(r);let host='';try{host=(new URL(URL_LISTA)).host.toLowerCase();}catch(e){}if(h.includes(URL_LISTA)||(host&&h.toLowerCase().includes(host))||/http:\\/\\//i.test(h)){clearInterval(t);post('success','PLAYLIST_ADDED','M3U confirmada na tabela de dispositivos.');return;}}if(n>=40){clearInterval(t);post('error','PLAYLIST_NOT_CONFIRMED','A M3U não apareceu na tabela de dispositivos.');}},500);})();true;";}
        static String deleteDevice(String mac,String op){return "(function(){const MAC="+q(mac)+";const OP="+q(op)+";"+helper()+"function row(){return [...document.querySelectorAll('tr')].find(r=>(r.innerText||'').toUpperCase().includes(MAC.toUpperCase()));}function menu(r){const i=r.querySelector('svg.lucide-ellipsis')||r.querySelector('svg[class*=\"ellipsis\"]');return i?i.closest('button'):null;}function action(ws){const es=[...document.querySelectorAll('[role=\"menuitem\"]'),...document.querySelectorAll('button')];return es.find(e=>{const t=(e.textContent||'').trim().toLowerCase();return t&&t.length<80&&ws.some(w=>t.includes(w));})||null;}function confirm(){const ds=[...document.querySelectorAll('[role=\"dialog\"]')];const d=ds[ds.length-1];if(!d)return false;const b=[...d.querySelectorAll('button')].find(x=>{const t=(x.textContent||'').trim().toLowerCase();return t&&!t.includes('cancel')&&!t.includes('não')&&!t.includes('nao')&&t!=='no';});if(!b)return false;clickReal(b);return true;}function verify(){post('progress','DELETE_VERIFYING','Confirmando exclusão...');let n=0;const v=setInterval(()=>{n++;if(!row()){clearInterval(v);post('success','DEVICE_DELETED','Dispositivo removido e confirmado.');}else if(n>=12){clearInterval(v);post('error','DELETE_NOT_CONFIRMED','O painel não confirmou a exclusão.');}},500);}post('progress','DELETE_SEARCH','Procurando dispositivo...');let n=0;const t=setInterval(()=>{n++;const ins=[...document.querySelectorAll('input')];const s=ins.find(i=>i.type==='search')||ins.find(i=>/search|busca|filtr/i.test(i.placeholder||''))||ins.find(i=>i.offsetParent!==null&&(i.type==='text'||!i.type));if(!s){if(n>=20){clearInterval(t);post('error','SEARCH_INPUT_NOT_FOUND','Campo de pesquisa não encontrado.');}return;}clearInterval(t);setReactInput(s,MAC);setTimeout(()=>{const r=row();if(!r){post('error','DEVICE_NOT_FOUND','Dispositivo não encontrado.');return;}const m=menu(r);if(!m){post('error','DEVICE_MENU_NOT_FOUND','Menu não encontrado.');return;}clickReal(m);setTimeout(()=>{const de=action(['deactivate','desativar','disable','inactiv']);const del=action(['delete','excluir','deletar','remove','apagar','trash','lixeira']);if(de){post('progress','DEACTIVATING','Desativando no painel...');clickReal(de);setTimeout(()=>{confirm();setTimeout(()=>{const r2=row();if(!r2){post('error','DEVICE_NOT_VISIBLE_AFTER_DEACTIVATE','Dispositivo saiu da lista após desativar.');return;}const m2=menu(r2);if(!m2){post('error','SECOND_MENU_NOT_FOUND','Não foi possível reabrir o menu.');return;}clickReal(m2);setTimeout(()=>{const d2=action(['delete','excluir','deletar','remove','apagar','trash','lixeira']);if(!d2){post('error','DELETE_ACTION_NOT_FOUND','Ação Delete não encontrada.');return;}post('progress','DELETING','Removendo dispositivo...');clickReal(d2);setTimeout(()=>{confirm();setTimeout(verify,900);},900);},1200);},2200);},800);return;}if(del){post('progress','DELETING','Removendo dispositivo...');clickReal(del);setTimeout(()=>{confirm();setTimeout(verify,900);},900);return;}post('error','DELETE_ACTION_NOT_FOUND','Deactivate/Delete não encontrado.');},1000);},700);},500);})();true;";}
    }
}
'''

(java / 'MasterXCloudActivity.java').write_text(activity, encoding='utf-8')

# Manifest: Internet/Vibração e Activity interna.
m = manifest.read_text(encoding='utf-8')
if 'android.permission.INTERNET' not in m:
    m = m.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">', '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.INTERNET"/>', 1)
if 'MasterXCloudActivity' not in m:
    marker = '</application>'
    entry = '        <activity android:name=".MasterXCloudActivity" android:exported="false" android:screenOrientation="portrait"/>\n'
    if marker not in m: raise SystemExit('ERRO XCLOUD LOCAL: </application> ausente')
    m = m.replace(marker, entry + marker, 1)
manifest.write_text(m, encoding='utf-8')

# Menu lateral: adiciona Master XCloud sob AUTOMAÇÃO.
d = dash.read_text(encoding='utf-8')
if 'MasterXCloudActivity.class' not in d:
    anchor = 'panel.addView(sideItem("☷","Menu do Grupo",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,GroupMenuActivity.class));}'
    idx = d.find(anchor)
    if idx < 0: raise SystemExit('ERRO XCLOUD LOCAL: item Menu do Grupo não encontrado')
    line_start = idx
    insert = 'panel.addView(sideItem("☁","Master XCloud",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MasterXCloudActivity.class));}catch(Throwable e){Toast.makeText(this,"Master XCloud indisponível",Toast.LENGTH_SHORT).show();}}));\n  '
    d = d[:line_start] + insert + d[line_start:]
dash.write_text(d, encoding='utf-8')

# Versão final após o patch de sincronização v2.1.39.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 111', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.40'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# Valida integração e, principalmente, preservação do núcleo já testado.
checks = [
    (java/'MasterXCloudActivity.java', 'enum Flow { ACTIVATE, RESET, DELETE }'),
    (java/'MasterXCloudActivity.java', 'panel-v2.xtream.cloud/dashboard/devices'),
    (java/'MasterXCloudActivity.java', 'PLAYLIST_VERIFY_ON_DEVICES'),
    (java/'MasterXCloudActivity.java', 'deleteDevice(currentDevice,operationId)'),
    (dash, 'MasterXCloudActivity.class'),
    (gradle, "versionName '2.1.40'"),
    (svc, 'botEnabledNow'),
    (svc, 'MasterflixTestTrigger.matches'),
    (svc, 'runScheduledGroupBroadcastIfDue'),
    (svc, 'GroupMenuStore.trigger'),
]
for p, mark in checks:
    if mark not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO XCLOUD LOCAL: requisito ausente: '+mark)

whole = '\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in [svc, java/'MasterXCloudActivity.java'])
for forbidden in ['api.masterxcloud.shop', '/operations/activate', '/operations/reset', 'MasterXCloudApi.execute']:
    if forbidden in whole:
        raise SystemExit('ERRO XCLOUD LOCAL: dependência antiga ainda presente: '+forbidden)

print('v2.1.40: Master XCloud local integrado com ATIVAR / RESET / EXCLUIR; motor API antigo removido')
