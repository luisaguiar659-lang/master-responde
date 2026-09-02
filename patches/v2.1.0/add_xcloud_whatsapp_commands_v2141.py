from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
activity=java/'MasterXCloudActivity.java'
svc=java/'WhatsAppBusinessCaptureService.java'
gradle=app/'build.gradle'

# Expor apenas os scripts já validados do motor local para o executor em background.
a=activity.read_text(encoding='utf-8')
if 'private static class Scripts' in a:
    a=a.replace('private static class Scripts','static class Scripts',1)
elif 'static class Scripts' not in a:
    raise SystemExit('ERRO v2.1.41: classe Scripts do Master XCloud não encontrada')

# Campos para os três comandos configuráveis.
anchor='    private EditText email, password, device, playlist;'
if 'cmdActivate' not in a:
    a=a.replace(anchor,anchor+'\n    private EditText cmdActivate, cmdReset, cmdDelete;',1)

# Salvar credenciais do painel para que o motor do WhatsApp consiga operar sem pedir login ao cliente.
old='''                authenticated=true; running=false; enter.setEnabled(true); enter.setText("ENTRAR"); password.setText(""); status.setText(""); showOperations(); return;'''
new='''                authenticated=true; running=false; enter.setEnabled(true); enter.setText("ENTRAR");\n                try { new SecureStore(this).put("master_xcloud_password", password.getText().toString()); } catch (Throwable ignored) {}\n                getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("master_xcloud_email",email.getText().toString().trim()).apply();\n                password.setText(""); status.setText(""); showOperations(); return;'''
if old not in a:
    raise SystemExit('ERRO v2.1.41: ponto de login bem-sucedido não encontrado')
a=a.replace(old,new,1)

# Configuração visual dos três comandos dentro do próprio Master XCloud.
ui_anchor='''        operationBox.addView(gap(18)); TextView safe=label("● MOTOR LOCAL XCLOUD v1.10.3",12,Color.rgb(85,220,125),true); safe.setGravity(Gravity.CENTER); operationBox.addView(safe);'''
ui_insert=ui_anchor+r'''
        operationBox.addView(gap(24));
        operationBox.addView(label("COMANDOS DO WHATSAPP",12,Color.rgb(255,156,0),true));
        operationBox.addView(gap(8));
        SharedPreferences xp=getSharedPreferences("master_responde",MODE_PRIVATE);
        cmdActivate=field("Comando para ativar",false); cmdActivate.setText(xp.getString("xcloud_cmd_activate","ativar xcloud"));
        cmdReset=field("Comando para reset",false); cmdReset.setText(xp.getString("xcloud_cmd_reset","reset xcloud"));
        cmdDelete=field("Comando para excluir",false); cmdDelete.setText(xp.getString("xcloud_cmd_delete","excluir xcloud"));
        operationBox.addView(cmdActivate,new LinearLayout.LayoutParams(-1,dp(54))); operationBox.addView(gap(8));
        operationBox.addView(cmdReset,new LinearLayout.LayoutParams(-1,dp(54))); operationBox.addView(gap(8));
        operationBox.addView(cmdDelete,new LinearLayout.LayoutParams(-1,dp(54))); operationBox.addView(gap(10));
        Button saveCommands=button("SALVAR COMANDOS");
        saveCommands.setOnClickListener(v->{
            String ca=cmdActivate.getText().toString().trim(), cr=cmdReset.getText().toString().trim(), cd=cmdDelete.getText().toString().trim();
            if(ca.isEmpty()||cr.isEmpty()||cd.isEmpty()){toast("Os três comandos precisam ser preenchidos.");return;}
            if(ca.equalsIgnoreCase(cr)||ca.equalsIgnoreCase(cd)||cr.equalsIgnoreCase(cd)){toast("Use três comandos diferentes.");return;}
            xp.edit().putString("xcloud_cmd_activate",ca).putString("xcloud_cmd_reset",cr).putString("xcloud_cmd_delete",cd).apply();
            toast("Comandos do WhatsApp salvos.");
        });
        operationBox.addView(saveCommands,new LinearLayout.LayoutParams(-1,dp(54)));'''
if 'COMANDOS DO WHATSAPP' not in a:
    if ui_anchor not in a: raise SystemExit('ERRO v2.1.41: ponto visual do Master XCloud não encontrado')
    a=a.replace(ui_anchor,ui_insert,1)
activity.write_text(a,encoding='utf-8')

(java/'MasterXCloudBackgroundAutomation.java').write_text(r'''package com.masterresponde.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.util.Locale;
import java.util.UUID;

/** Executa o mesmo motor WebView/Xtream do Master XCloud sem abrir tela. */
public final class MasterXCloudBackgroundAutomation {
    public interface Callback { void success(String message); void error(String message); }
    private static final String DEVICES="https://panel-v2.xtream.cloud/dashboard/devices";
    private static final String PLAYLIST="https://xtream.cloud/custom-playlist";
    private static final Handler H=new Handler(Looper.getMainLooper());
    private static WebView web;
    private static Callback callback;
    private static String op="",device="",m3u="",operationId="",email="",password="";
    private static boolean busy=false,loginInjected=false,authenticated=false,deviceInjected=false,deleteInjected=false,playlistInjected=false,resetDeleted=false,verify=false;
    private static int retries=0;
    private static Runnable timeout;
    private MasterXCloudBackgroundAutomation(){}

    public static synchronized boolean start(Context context,String operation,String key,String playlist,Callback cb){
        if(busy)return false;
        busy=true; callback=cb; op=operation; device=key; m3u=playlist==null?"":playlist; operationId=UUID.randomUUID().toString();
        retries=0;loginInjected=false;authenticated=false;deviceInjected=false;deleteInjected=false;playlistInjected=false;resetDeleted=false;verify=false;
        email=context.getSharedPreferences("master_responde",Context.MODE_PRIVATE).getString("master_xcloud_email","").trim();
        try{password=new SecureStore(context).get("master_xcloud_password");}catch(Throwable e){password="";}
        if(email.isEmpty()||password==null||password.isEmpty()){busy=false;if(cb!=null)cb.error("O administrador precisa entrar no Master XCloud pelo aplicativo uma vez para salvar a sessão.");return true;}
        Context app=context.getApplicationContext();
        H.post(()->create(app));
        timeout=()->{if(busy)failFinal("Tempo limite excedido ao processar no painel XCloud.");};
        H.postDelayed(timeout,150000L);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled") private static void create(Context c){
        try{
            web=new WebView(c); WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);
            web.addJavascriptInterface(new Bridge(),"AutomationBridge");
            web.setWebViewClient(new WebViewClient(){
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String h=r.getUrl().getHost();return !"https".equalsIgnoreCase(r.getUrl().getScheme())||h==null||!(h.equals("panel.xtream.cloud")||h.equals("panel-v2.xtream.cloud")||h.equals("xtream.cloud"));}
                @Override public void onPageFinished(WebView v,String url){page(url);}
            });
            web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());
        }catch(Throwable e){failFinal("Não foi possível iniciar o motor local do XCloud.");}
    }

    private static void page(String url){
        if(!busy||web==null||url==null)return;String low=url.toLowerCase(Locale.ROOT);
        if((low.contains("/login")||low.contains("#/login"))&&!loginInjected){loginInjected=true;web.evaluateJavascript(MasterXCloudActivity.Scripts.login(email,password),null);return;}
        if(low.contains("/dashboard")&&!low.contains("/login"))authenticated=true;
        if(!authenticated)return;
        if(low.contains("panel-v2.xtream.cloud")&&low.contains("/dashboard/devices")){
            if(verify){web.evaluateJavascript(MasterXCloudActivity.Scripts.verifyPlaylist(device,m3u,operationId),null);return;}
            if("DELETE".equals(op)||("RESET".equals(op)&&!resetDeleted)){
                if(deleteInjected)return;deleteInjected=true;web.evaluateJavascript(MasterXCloudActivity.Scripts.deleteDevice(device,operationId),null);return;
            }
            if(!deviceInjected){deviceInjected=true;web.evaluateJavascript(MasterXCloudActivity.Scripts.addDevice(device,operationId),null);}return;
        }
        if(low.contains("xtream.cloud/custom-playlist")&&!playlistInjected){playlistInjected=true;web.evaluateJavascript(MasterXCloudActivity.Scripts.addPlaylist(device,m3u,operationId),null);}
    }

    public static final class Bridge{ @JavascriptInterface public void postMessage(String raw){H.post(()->message(raw));} }
    private static void message(String raw){
        if(!busy)return;
        try{
            JSONObject o=new JSONObject(raw);String type=o.optString("type","progress"),code=o.optString("code",""),msg=o.optString("message","");
            if(!operationId.equals(o.optString("operationId","")))return;
            if("PLAYLIST_VERIFY_ON_DEVICES".equals(code)){
                verify=true;playlistInjected=false;web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());
                if("RESET".equals(op))H.postDelayed(()->{if(busy&&"RESET".equals(op)&&verify)finish("Reset + DNS concluído com sucesso.");},4500L);
                return;
            }
            if("DEVICE_ADDED".equals(code)){web.loadUrl(PLAYLIST+"?device_key="+android.net.Uri.encode(device)+"&type=xtream&mode=add");return;}
            if("DEVICE_DELETED".equals(code)&&"DELETE".equals(op)){finish("Dispositivo excluído com sucesso.");return;}
            if("DEVICE_DELETED".equals(code)&&"RESET".equals(op)){
                resetDeleted=true;deleteInjected=false;deviceInjected=false;playlistInjected=false;verify=false;retries=0;web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());return;
            }
            if("PLAYLIST_ADDED".equals(code)){finish("RESET".equals(op)?"Reset + DNS concluído com sucesso.":"Ativação concluída com sucesso.");return;}
            if("error".equals(type))retryOrFail(code,msg);
        }catch(Throwable e){retryOrFail("INVALID_MESSAGE","Resposta inválida do painel XCloud.");}
    }

    private static void retryOrFail(String code,String msg){
        boolean recover=code.contains("NOT_FOUND")||code.contains("TIMEOUT")||code.contains("NOT_CONFIRMED")||code.contains("PANEL_NOT_READY")||code.contains("VISIBLE_AFTER");
        if(recover&&retries<3){retries++;deviceInjected=false;deleteInjected=false;playlistInjected=false;
            H.postDelayed(()->{if(!busy)return;if(code.startsWith("PLAYLIST_"))web.loadUrl(PLAYLIST+"?device_key="+android.net.Uri.encode(device)+"&type=xtream&mode=add&t="+System.currentTimeMillis());else web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());},900L);return;}
        failFinal(msg==null||msg.trim().isEmpty()?"Não foi possível concluir a operação no XCloud.":msg);
    }
    private static void finish(String msg){Callback cb=callback;cleanup();if(cb!=null)cb.success(msg);}
    private static void failFinal(String msg){Callback cb=callback;cleanup();if(cb!=null)cb.error(msg);}
    private static synchronized void cleanup(){
        busy=false;if(timeout!=null)H.removeCallbacks(timeout);timeout=null;
        WebView w=web;web=null;callback=null;operationId="";password="";
        if(w!=null)H.post(()->{try{w.stopLoading();w.removeJavascriptInterface("AutomationBridge");w.destroy();}catch(Throwable ignored){}});
    }
}
''',encoding='utf-8')

(java/'MasterXCloudWhatsAppFlow.java').write_text(r'''package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.Normalizer;
import java.util.Locale;

public final class MasterXCloudWhatsAppFlow {
    private static final String P="master_responde";
    private static final long TTL=15L*60L*1000L;
    private MasterXCloudWhatsAppFlow(){}
    private static String norm(String s){if(s==null)return "";return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
    private static String id(String c){return Integer.toHexString(norm(c).hashCode());}
    private static String k(String c,String f){return "xcloud_flow_"+f+"_"+id(c);}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
    private static void send(Context c,Notification n,String text){MasterflixDirectReply.send(c,n,text);}
    private static String command(SharedPreferences p,String key,String def){return norm(p.getString(key,def));}

    public static boolean handle(Context c,Notification n,String conversation,String message){
        if(c==null||n==null)return false;SharedPreferences sp=p(c);String m=norm(message);if(m.isEmpty())return false;
        String state=sp.getString(k(conversation,"state"),"");long at=sp.getLong(k(conversation,"at"),0L);
        if(!state.isEmpty()&&at>0&&System.currentTimeMillis()-at>TTL){clear(sp,conversation);state="";}
        String ca=command(sp,"xcloud_cmd_activate","ativar xcloud"),cr=command(sp,"xcloud_cmd_reset","reset xcloud"),cd=command(sp,"xcloud_cmd_delete","excluir xcloud");
        String operation=m.equals(ca)?"ACTIVATE":m.equals(cr)?"RESET":m.equals(cd)?"DELETE":"";
        if(!operation.isEmpty()){
            clear(sp,conversation);sp.edit().putString(k(conversation,"state"),"KEY").putString(k(conversation,"op"),operation).putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
            send(c,n,"📺 MASTER XCLOUD\n\nEnvie a Key do dispositivo.");return true;
        }
        if(state.isEmpty())return false;
        if("cancelar".equals(m)){clear(sp,conversation);send(c,n,"❌ Operação Master XCloud cancelada.");return true;}
        if("PROCESSING".equals(state)){send(c,n,"⏳ Sua operação Master XCloud ainda está sendo processada. Aguarde a conclusão.");return true;}
        String op=sp.getString(k(conversation,"op"),"ACTIVATE");
        if("KEY".equals(state)){
            String key=message==null?"":message.trim().toUpperCase(Locale.ROOT);
            if(!key.matches("^[A-Z0-9_-]{4,32}$")){send(c,n,"⚠️ Key inválida. Envie somente a Key do dispositivo.");return true;}
            sp.edit().putString(k(conversation,"key"),key).putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
            if("DELETE".equals(op)){start(c,n,conversation,op,key,"");return true;}
            sp.edit().putString(k(conversation,"state"),"M3U").apply();send(c,n,"🔗 Agora envie a M3U / DNS começando com http://");return true;
        }
        if("M3U".equals(state)){
            String m3u=message==null?"":message.trim();if(!m3u.toLowerCase(Locale.ROOT).startsWith("http://")){send(c,n,"⚠️ A M3U / DNS precisa começar com http://");return true;}
            start(c,n,conversation,op,sp.getString(k(conversation,"key"),""),m3u);return true;
        }
        clear(sp,conversation);return false;
    }

    private static void start(Context c,Notification n,String conversation,String op,String key,String m3u){
        SharedPreferences sp=p(c);sp.edit().putString(k(conversation,"state"),"PROCESSING").putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
        send(c,n,"⏳ MASTER XCLOUD\n\nDados recebidos. Processando, aguarde...");
        boolean started=MasterXCloudBackgroundAutomation.start(c,op,key,m3u,new MasterXCloudBackgroundAutomation.Callback(){
            @Override public void success(String message){clear(sp,conversation);String out="DELETE".equals(op)?"✅ Dispositivo excluído com sucesso.":("RESET".equals(op)?"✅ Reset concluído com sucesso.":"✅ Dispositivo ativado com sucesso.");send(c,n,"✅ MASTER XCLOUD\n\n"+out);}
            @Override public void error(String message){clear(sp,conversation);send(c,n,"❌ MASTER XCLOUD\n\n"+message);}
        });
        if(!started){clear(sp,conversation);send(c,n,"⏳ O Master XCloud está concluindo outra operação. Tente novamente em alguns instantes.");}
    }
    private static void clear(SharedPreferences sp,String c){sp.edit().remove(k(c,"state")).remove(k(c,"op")).remove(k(c,"key")).remove(k(c,"at")).apply();}
}
''',encoding='utf-8')

# Intercepta somente no privado e antes dos fluxos MasterFlix/comandos gerais.
s=svc.read_text(encoding='utf-8')
anchor='        // PRIVADO: fluxo de revenda MasterFlix restaurado do projeto original.'
insert='''        // PRIVADO: comandos configuráveis do Master XCloud (ativar/reset/excluir).\n        if (MasterXCloudWhatsAppFlow.handle(this, n, conversation, message)) {\n            return;\n        }\n\n'''
if 'MasterXCloudWhatsAppFlow.handle' not in s:
    if anchor not in s: raise SystemExit('ERRO v2.1.41: ponto privado antes do MasterFlix não encontrado')
    s=s.replace(anchor,insert+anchor,1)
svc.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 112',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.41'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (activity,'COMANDOS DO WHATSAPP'),(activity,'master_xcloud_password'),
 (java/'MasterXCloudWhatsAppFlow.java','xcloud_cmd_activate'),(java/'MasterXCloudWhatsAppFlow.java','"DELETE".equals(op)'),
 (java/'MasterXCloudBackgroundAutomation.java','MasterXCloudActivity.Scripts.addDevice'),
 (java/'MasterXCloudBackgroundAutomation.java','MasterXCloudActivity.Scripts.deleteDevice'),
 (svc,'MasterXCloudWhatsAppFlow.handle(this, n, conversation, message)'),
 (gradle,"versionName '2.1.41'")]
for p,m in checks:
    if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.41: requisito ausente '+m)
print('v2.1.41: Master XCloud agora opera por três comandos configuráveis no WhatsApp: ativar, reset e excluir')
