from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
activity=java/'MasterXCloudActivity.java'
flow=java/'MasterXCloudWhatsAppFlow.java'
bg=java/'MasterXCloudBackgroundAutomation.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# -----------------------------------------------------------------------------
# Núcleo profissional isolado: fila, histórico, controle de acesso, limite,
# métricas, status do motor, alertas administrativos e backup/importação.
# Não altera os scripts funcionais de ATIVAR / RESET / EXCLUIR.
# -----------------------------------------------------------------------------
(java/'MasterXCloudOps.java').write_text(r'''package com.masterresponde.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public final class MasterXCloudOps {
    private static final String PREF="master_responde";
    private static final Handler H=new Handler(Looper.getMainLooper());
    private static final ArrayDeque<Job> Q=new ArrayDeque<>();
    private static boolean running=false;
    private static Context app;

    private static final class Job{
        final Context c; final Notification n; final String conversation,op,key,m3u;
        Job(Context c,Notification n,String conversation,String op,String key,String m3u){
            this.c=c.getApplicationContext();this.n=n;this.conversation=conversation;this.op=op;this.key=key;this.m3u=m3u;
        }
    }
    private MasterXCloudOps(){}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    private static String norm(String s){if(s==null)return "";return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
    public static String msg(Context c,String key,String def){String x=p(c).getString(key,def);return x==null||x.trim().isEmpty()?def:x;}

    public static boolean allowed(Context c,String conversation){
        String raw=p(c).getString("xcloud_authorized_contacts","").trim();
        if(raw.isEmpty())return true;
        String conv=norm(conversation);
        for(String x:raw.split("[\\r\\n,;]+")){String t=norm(x);if(!t.isEmpty()&&conv.contains(t))return true;}
        return false;
    }

    public static boolean rateAllowed(Context c,String conversation){
        SharedPreferences sp=p(c);int max=10;try{max=Integer.parseInt(sp.getString("xcloud_max_per_hour","10").trim());}catch(Throwable ignored){}
        if(max<=0)return true;
        String id=Integer.toHexString(norm(conversation).hashCode());String base="xcloud_rate_"+id+"_";long now=System.currentTimeMillis();long start=sp.getLong(base+"start",0L);int count=sp.getInt(base+"count",0);
        if(start==0L||now-start>=3600000L){start=now;count=0;}
        if(count>=max)return false;
        sp.edit().putLong(base+"start",start).putInt(base+"count",count+1).apply();return true;
    }

    public static synchronized int enqueue(Context c,Notification n,String conversation,String op,String key,String m3u){
        app=c.getApplicationContext();
        int pos=(running?1:0)+Q.size()+1;
        Q.addLast(new Job(c,n,conversation,op,key,m3u));
        history(c,conversation,op,key,"NA_FILA","");
        p(c).edit().putString("xcloud_engine_status",running?"ocupado":"iniciando").putInt("xcloud_queue_size",Q.size()+(running?1:0)).apply();
        if(!running)H.post(MasterXCloudOps::drain);
        return pos;
    }

    private static synchronized void drain(){
        if(running)return;
        Job j=Q.pollFirst();
        if(j==null){if(app!=null)p(app).edit().putInt("xcloud_queue_size",0).putString("xcloud_engine_status","online").apply();return;}
        running=true;p(j.c).edit().putString("xcloud_engine_status","processando "+j.op.toLowerCase(Locale.ROOT)).putInt("xcloud_queue_size",Q.size()+1).apply();
        history(j.c,j.conversation,j.op,j.key,"PROCESSANDO","");
        boolean started=MasterXCloudBackgroundAutomation.start(j.c,j.op,j.key,j.m3u,new MasterXCloudBackgroundAutomation.Callback(){
            @Override public void success(String message){complete(j,true,message);}
            @Override public void error(String message){complete(j,false,message);}
        });
        if(!started){
            running=false;Q.addFirst(j);H.postDelayed(MasterXCloudOps::drain,1200L);
        }
    }

    private static void complete(Job j,boolean ok,String detail){
        if(ok){
            String key="ACTIVATE".equals(j.op)?"xcloud_msg_success_activate":"RESET".equals(j.op)?"xcloud_msg_success_reset":"xcloud_msg_success_delete";
            String def="ACTIVATE".equals(j.op)?"✅ Dispositivo ativado com sucesso.":"RESET".equals(j.op)?"✅ Reset concluído com sucesso.":"✅ Dispositivo excluído com sucesso.";
            MasterflixDirectReply.send(j.c,j.n,"✅ MASTER XCLOUD\n\n"+msg(j.c,key,def));
            metric(j.c,j.op,true);history(j.c,j.conversation,j.op,j.key,"SUCESSO",detail);p(j.c).edit().putInt("xcloud_consecutive_failures",0).putString("xcloud_last_result","SUCESSO • "+j.op+" • "+j.key).apply();
        }else{
            String prefix=msg(j.c,"xcloud_msg_error_prefix","❌ Não foi possível concluir a operação.");
            MasterflixDirectReply.send(j.c,j.n,"❌ MASTER XCLOUD\n\n"+prefix+"\n"+(detail==null?"":detail));
            metric(j.c,j.op,false);history(j.c,j.conversation,j.op,j.key,"ERRO",detail);int fails=p(j.c).getInt("xcloud_consecutive_failures",0)+1;p(j.c).edit().putInt("xcloud_consecutive_failures",fails).putString("xcloud_last_result","ERRO • "+j.op+" • "+j.key).apply();if(fails>=3)adminAlert(j.c,detail);
        }
        MasterXCloudWhatsAppFlow.complete(j.c,j.conversation);
        synchronized(MasterXCloudOps.class){running=false;p(j.c).edit().putInt("xcloud_queue_size",Q.size()).putString("xcloud_engine_status",Q.isEmpty()?"online":"fila: "+Q.size()).apply();}
        H.post(MasterXCloudOps::drain);
    }

    public static void engineStatus(Context c,String status){if(c!=null)p(c).edit().putString("xcloud_engine_status",status).putLong("xcloud_status_at",System.currentTimeMillis()).apply();}
    public static synchronized int queueSize(){return Q.size()+(running?1:0);}
    public static String shortStatus(Context c){String s=p(c).getString("xcloud_engine_status","aguardando login");if(s==null||s.isEmpty())s="aguardando";return s;}
    public static String statusText(Context c){SharedPreferences sp=p(c);String d=day();return "Motor: "+shortStatus(c)+"\nFila: "+queueSize()+"\nHoje: "+sp.getInt("xcloud_ok_"+d,0)+" sucesso(s) • "+sp.getInt("xcloud_fail_"+d,0)+" falha(s)\nAtivar: "+sp.getInt("xcloud_activate_"+d,0)+" • Reset: "+sp.getInt("xcloud_reset_"+d,0)+" • Excluir: "+sp.getInt("xcloud_delete_"+d,0)+"\nÚltimo: "+sp.getString("xcloud_last_result","nenhuma operação");}
    private static String day(){return new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());}
    private static void metric(Context c,String op,boolean ok){SharedPreferences sp=p(c);String d=day();SharedPreferences.Editor e=sp.edit();String all=(ok?"xcloud_ok_":"xcloud_fail_")+d;e.putInt(all,sp.getInt(all,0)+1);String k="xcloud_"+("ACTIVATE".equals(op)?"activate_":"RESET".equals(op)?"reset_":"delete_")+d;e.putInt(k,sp.getInt(k,0)+1).apply();}

    public static synchronized void history(Context c,String conversation,String op,String key,String status,String detail){
        try{SharedPreferences sp=p(c);JSONArray a=new JSONArray(sp.getString("xcloud_history","[]"));JSONObject o=new JSONObject();o.put("time",System.currentTimeMillis());o.put("conversation",conversation==null?"":conversation);o.put("op",op);o.put("key",key);o.put("status",status);o.put("detail",detail==null?"":detail);JSONArray n=new JSONArray();n.put(o);for(int i=0;i<a.length()&&i<99;i++)n.put(a.get(i));sp.edit().putString("xcloud_history",n.toString()).apply();}catch(Throwable ignored){}
    }
    public static String historyText(Context c){
        try{JSONArray a=new JSONArray(p(c).getString("xcloud_history","[]"));StringBuilder b=new StringBuilder();SimpleDateFormat f=new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault());for(int i=0;i<a.length()&&i<30;i++){JSONObject o=a.getJSONObject(i);b.append(f.format(new Date(o.optLong("time")))).append(" • ").append(o.optString("op")).append(" • ").append(o.optString("key")).append(" • ").append(o.optString("status")).append('\n');}return b.length()==0?"Nenhuma operação registrada.":b.toString();}catch(Throwable e){return "Histórico indisponível.";}
    }

    private static void adminAlert(Context c,String detail){
        try{NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);String ch="xcloud_admin_alerts";if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"Alertas Master XCloud",NotificationManager.IMPORTANCE_HIGH));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,ch):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("MASTER XCLOUD: falhas consecutivas").setContentText(detail==null?"Verifique o motor XCloud.":detail).setAutoCancel(true);nm.notify(2145,b.build());}catch(Throwable ignored){}
    }

    private static final String[] BACKUP_KEYS={"xcloud_cmd_activate","xcloud_cmd_reset","xcloud_cmd_delete","xcloud_authorized_contacts","xcloud_max_per_hour","xcloud_msg_ask_key","xcloud_msg_ask_m3u","xcloud_msg_processing","xcloud_msg_success_activate","xcloud_msg_success_reset","xcloud_msg_success_delete","xcloud_msg_error_prefix"};
    public static String exportConfig(Context c){try{JSONObject o=new JSONObject();SharedPreferences sp=p(c);for(String k:BACKUP_KEYS)o.put(k,sp.getString(k,""));return o.toString(2);}catch(Throwable e){return "{}";}}
    public static boolean importConfig(Context c,String raw){try{JSONObject o=new JSONObject(raw);SharedPreferences.Editor e=p(c).edit();for(String k:BACKUP_KEYS)if(o.has(k))e.putString(k,o.optString(k,""));e.apply();return true;}catch(Throwable x){return false;}}
}
''',encoding='utf-8')

# -----------------------------------------------------------------------------
# Fluxo WhatsApp: mantém o protocolo atual, adicionando autorização, limite,
# mensagens configuráveis e fila. A execução funcional continua no mesmo motor.
# -----------------------------------------------------------------------------
f=flow.read_text(encoding='utf-8')
old='''        if(!operation.isEmpty()){
            clear(sp,conversation);sp.edit().putString(k(conversation,"state"),"KEY").putString(k(conversation,"op"),operation).putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
            send(c,n,"📺 MASTER XCLOUD\\n\\nEnvie a Key do dispositivo.");return true;
        }'''
new='''        if(!operation.isEmpty()){
            if(!MasterXCloudOps.allowed(c,conversation)){send(c,n,"🔒 MASTER XCLOUD\\n\\nEste contato não está autorizado a usar este motor.");return true;}
            if(!MasterXCloudOps.rateAllowed(c,conversation)){send(c,n,"⚠️ MASTER XCLOUD\\n\\nLimite de operações por hora atingido. Tente novamente mais tarde.");return true;}
            clear(sp,conversation);sp.edit().putString(k(conversation,"state"),"KEY").putString(k(conversation,"op"),operation).putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
            send(c,n,MasterXCloudOps.msg(c,"xcloud_msg_ask_key","📺 MASTER XCLOUD\\n\\nEnvie a Key do dispositivo."));return true;
        }'''
if old not in f: raise SystemExit('ERRO v2.1.45: gatilho WhatsApp XCloud não encontrado')
f=f.replace(old,new,1)
f=f.replace('send(c,n,"🔗 Agora envie a M3U / DNS começando com http://");return true;', 'send(c,n,MasterXCloudOps.msg(c,"xcloud_msg_ask_m3u","🔗 Agora envie a M3U / DNS começando com http://"));return true;',1)
start_re=r'''    private static void start\(Context c,Notification n,String conversation,String op,String key,String m3u\)\{.*?\n    \}\n    private static void clear'''
start_new='''    private static void start(Context c,Notification n,String conversation,String op,String key,String m3u){
        SharedPreferences sp=p(c);sp.edit().putString(k(conversation,"state"),"PROCESSING").putLong(k(conversation,"at"),System.currentTimeMillis()).apply();
        send(c,n,MasterXCloudOps.msg(c,"xcloud_msg_processing","⏳ MASTER XCLOUD\\n\\nDados recebidos. Processando, aguarde..."));
        int pos=MasterXCloudOps.enqueue(c,n,conversation,op,key,m3u);
        if(pos>1)send(c,n,"🕒 MASTER XCLOUD\\n\\nSeu pedido entrou na fila. Posição: "+pos+".");
    }
    static void complete(Context c,String conversation){clear(p(c),conversation);}
    private static void clear'''
f2,n=re.subn(start_re,start_new,f,count=1,flags=re.S)
if n!=1: raise SystemExit('ERRO v2.1.45: método start do WhatsApp XCloud não localizado')
flow.write_text(f2,encoding='utf-8')

# -----------------------------------------------------------------------------
# Recuperação de sessão e telemetria no executor em background.
# -----------------------------------------------------------------------------
b=bg.read_text(encoding='utf-8')
b=b.replace('private static int retries=0;','private static int retries=0,loginAttempts=0;\n    private static Context appContext;',1)
b=b.replace('retries=0;loginInjected=false;authenticated=false;deviceInjected=false;deleteInjected=false;playlistInjected=false;resetDeleted=false;verify=false;', 'retries=0;loginAttempts=0;loginInjected=false;authenticated=false;deviceInjected=false;deleteInjected=false;playlistInjected=false;resetDeleted=false;verify=false;\n        appContext=context.getApplicationContext(); MasterXCloudOps.engineStatus(appContext,"conectando");',1)
old_login='''        if((low.contains("/login")||low.contains("#/login"))&&!loginInjected){loginInjected=true;web.evaluateJavascript(MasterXCloudActivity.Scripts.login(email,password),null);return;}'''
new_login='''        if(low.contains("/login")||low.contains("#/login")){
            authenticated=false; MasterXCloudOps.engineStatus(appContext,"reautenticando");
            if(loginAttempts>=3){failFinal("Não foi possível renovar o login do painel XCloud.");return;}
            loginAttempts++;loginInjected=true;H.postDelayed(()->{if(busy&&web!=null)web.evaluateJavascript(MasterXCloudActivity.Scripts.login(email,password),null);},350L);return;
        }'''
if old_login not in b: raise SystemExit('ERRO v2.1.45: login do background não encontrado')
b=b.replace(old_login,new_login,1)
b=b.replace('if(low.contains("/dashboard")&&!low.contains("/login"))authenticated=true;', 'if(low.contains("/dashboard")&&!low.contains("/login")){authenticated=true;loginAttempts=0;MasterXCloudOps.engineStatus(appContext,"online");}',1)
b=b.replace('private static void finish(String msg){Callback cb=callback;cleanup();if(cb!=null)cb.success(msg);}', 'private static void finish(String msg){MasterXCloudOps.engineStatus(appContext,"online");Callback cb=callback;cleanup();if(cb!=null)cb.success(msg);}',1)
b=b.replace('private static void failFinal(String msg){Callback cb=callback;cleanup();if(cb!=null)cb.error(msg);}', 'private static void failFinal(String msg){MasterXCloudOps.engineStatus(appContext,"erro");Callback cb=callback;cleanup();if(cb!=null)cb.error(msg);}',1)
bg.write_text(b,encoding='utf-8')

# -----------------------------------------------------------------------------
# Tela Master XCloud: controles avançados, histórico, métricas e backup.
# -----------------------------------------------------------------------------
a=activity.read_text(encoding='utf-8')
anchor='''        operationBox.addView(saveCommands,new LinearLayout.LayoutParams(-1,dp(54)));'''
extra=r'''
        operationBox.addView(gap(22));
        operationBox.addView(label("STATUS DO MOTOR",12,Color.rgb(255,156,0),true));
        TextView xHealth=label(MasterXCloudOps.statusText(this),12,Color.rgb(120,220,150),false);operationBox.addView(xHealth);
        Button refreshX=button("ATUALIZAR STATUS");refreshX.setOnClickListener(v->xHealth.setText(MasterXCloudOps.statusText(this)));operationBox.addView(refreshX,new LinearLayout.LayoutParams(-1,dp(50)));

        operationBox.addView(gap(20));operationBox.addView(label("SEGURANÇA E LIMITES",12,Color.rgb(255,156,0),true));
        EditText xAuth=field("Contatos autorizados (um por linha; vazio = todos)",false);xAuth.setSingleLine(false);xAuth.setText(xp.getString("xcloud_authorized_contacts",""));operationBox.addView(xAuth,new LinearLayout.LayoutParams(-1,dp(90)));
        EditText xLimit=field("Máximo de operações por contato/hora",false);xLimit.setText(xp.getString("xcloud_max_per_hour","10"));operationBox.addView(xLimit,new LinearLayout.LayoutParams(-1,dp(54)));

        operationBox.addView(gap(20));operationBox.addView(label("MENSAGENS DO XCLOUD",12,Color.rgb(255,156,0),true));
        EditText mKey=field("Mensagem pedindo Key",false);mKey.setText(xp.getString("xcloud_msg_ask_key","📺 MASTER XCLOUD\n\nEnvie a Key do dispositivo."));operationBox.addView(mKey,new LinearLayout.LayoutParams(-1,dp(70)));
        EditText mM3u=field("Mensagem pedindo M3U",false);mM3u.setText(xp.getString("xcloud_msg_ask_m3u","🔗 Agora envie a M3U / DNS começando com http://"));operationBox.addView(mM3u,new LinearLayout.LayoutParams(-1,dp(70)));
        EditText mProc=field("Mensagem de processamento",false);mProc.setText(xp.getString("xcloud_msg_processing","⏳ MASTER XCLOUD\n\nDados recebidos. Processando, aguarde..."));operationBox.addView(mProc,new LinearLayout.LayoutParams(-1,dp(70)));
        EditText mAct=field("Sucesso Ativar",false);mAct.setText(xp.getString("xcloud_msg_success_activate","✅ Dispositivo ativado com sucesso."));operationBox.addView(mAct,new LinearLayout.LayoutParams(-1,dp(60)));
        EditText mRes=field("Sucesso Reset",false);mRes.setText(xp.getString("xcloud_msg_success_reset","✅ Reset concluído com sucesso."));operationBox.addView(mRes,new LinearLayout.LayoutParams(-1,dp(60)));
        EditText mDel=field("Sucesso Excluir",false);mDel.setText(xp.getString("xcloud_msg_success_delete","✅ Dispositivo excluído com sucesso."));operationBox.addView(mDel,new LinearLayout.LayoutParams(-1,dp(60)));
        EditText mErr=field("Prefixo de erro",false);mErr.setText(xp.getString("xcloud_msg_error_prefix","❌ Não foi possível concluir a operação."));operationBox.addView(mErr,new LinearLayout.LayoutParams(-1,dp(60)));
        Button savePro=button("SALVAR CONFIGURAÇÕES AVANÇADAS");savePro.setOnClickListener(v->{String lim=xLimit.getText().toString().trim();try{Integer.parseInt(lim);}catch(Throwable e){toast("Informe um limite numérico.");return;}xp.edit().putString("xcloud_authorized_contacts",xAuth.getText().toString()).putString("xcloud_max_per_hour",lim).putString("xcloud_msg_ask_key",mKey.getText().toString()).putString("xcloud_msg_ask_m3u",mM3u.getText().toString()).putString("xcloud_msg_processing",mProc.getText().toString()).putString("xcloud_msg_success_activate",mAct.getText().toString()).putString("xcloud_msg_success_reset",mRes.getText().toString()).putString("xcloud_msg_success_delete",mDel.getText().toString()).putString("xcloud_msg_error_prefix",mErr.getText().toString()).apply();toast("Configurações XCloud salvas.");});operationBox.addView(savePro,new LinearLayout.LayoutParams(-1,dp(54)));

        operationBox.addView(gap(18));Button hist=button("HISTÓRICO DAS OPERAÇÕES");hist.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Histórico Master XCloud").setMessage(MasterXCloudOps.historyText(this)).setPositiveButton("FECHAR",null).show());operationBox.addView(hist,new LinearLayout.LayoutParams(-1,dp(52)));
        Button exp=button("COPIAR BACKUP DAS CONFIGURAÇÕES");exp.setOnClickListener(v->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("MASTER RESPONDE XCLOUD",MasterXCloudOps.exportConfig(this)));toast("Backup copiado.");});operationBox.addView(exp,new LinearLayout.LayoutParams(-1,dp(52)));
        Button imp=button("IMPORTAR BACKUP DA ÁREA DE TRANSFERÊNCIA");imp.setOnClickListener(v->{try{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);String raw=cm.hasPrimaryClip()?String.valueOf(cm.getPrimaryClip().getItemAt(0).coerceToText(this)):"";toast(MasterXCloudOps.importConfig(this,raw)?"Backup importado. Reabra a tela para atualizar os campos.":"Backup inválido.");}catch(Throwable e){toast("Não foi possível importar o backup.");}});operationBox.addView(imp,new LinearLayout.LayoutParams(-1,dp(52)));
'''
if anchor not in a: raise SystemExit('ERRO v2.1.45: ponto de configurações XCloud não encontrado')
a=a.replace(anchor,anchor+extra,1)
activity.write_text(a,encoding='utf-8')

# Status resumido no item do menu lateral, sem mudar a navegação.
d=dash.read_text(encoding='utf-8')
old_menu='panel.addView(sideItem("☁","Master XCloud",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MasterXCloudActivity.class));}catch(Throwable e){Toast.makeText(this,"Master XCloud indisponível",Toast.LENGTH_SHORT).show();}}));'
new_menu='panel.addView(sideItem("☁","Master XCloud • "+MasterXCloudOps.shortStatus(this),Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MasterXCloudActivity.class));}catch(Throwable e){Toast.makeText(this,"Master XCloud indisponível",Toast.LENGTH_SHORT).show();}}));'
if old_menu in d:d=d.replace(old_menu,new_menu,1)
dash.write_text(d,encoding='utf-8')

# v2.1.45
g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 116',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.45'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (java/'MasterXCloudOps.java','ArrayDeque<Job>'),
 (java/'MasterXCloudOps.java','xcloud_history'),
 (flow,'MasterXCloudOps.enqueue'),
 (flow,'MasterXCloudOps.allowed'),
 (bg,'loginAttempts>=3'),
 (activity,'SALVAR CONFIGURAÇÕES AVANÇADAS'),
 (activity,'COPIAR BACKUP DAS CONFIGURAÇÕES'),
 (gradle,"versionName '2.1.45'")]
for p,m in checks:
    if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.45: requisito ausente '+m)
print('v2.1.45: fila, histórico, mensagens, segurança, limite, status, recuperação de sessão, alertas, backup e métricas adicionados sem alterar os scripts funcionais XCloud')
