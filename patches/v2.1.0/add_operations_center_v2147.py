from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
dash=java/'NeonDashboardActivity.java'
manifest=app/'src/main/AndroidManifest.xml'
gradle=app/'build.gradle'

# v2.1.47: Central Operacional estritamente separada dos motores.
# Este patch cria apenas UI/leitura de estados + backup manual de configurações.
# NÃO edita nenhum arquivo de motor, WebView, WhatsApp flow ou agendador.
(java/'OperationsCenterActivity.java').write_text(r'''package com.masterresponde.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class OperationsCenterActivity extends Activity {
    private SharedPreferences prefs;
    private SecureStore secure;
    private LinearLayout box;
    private final int GREEN=Color.rgb(70,220,120), AMBER=Color.rgb(255,175,65), RED=Color.rgb(245,80,80), MUTED=Color.rgb(145,155,165);

    @Override protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("master_responde",MODE_PRIVATE);secure=new SecureStore(this);build();refresh();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView tv(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(null,1);t.setPadding(dp(5),dp(6),dp(5),dp(6));return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);return b;}
    private View gap(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return v;}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(6,8,11));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(8),dp(12),dp(8));
        Button back=btn("←");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(56),dp(48)));
        LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);tt.addView(tv("CENTRAL OPERACIONAL",21,Color.WHITE,true));tt.addView(tv("Dashboard • histórico • backup • saúde do Android",11,MUTED,false));top.addView(tt,new LinearLayout.LayoutParams(0,-2,1));
        Button ref=btn("ATUALIZAR");ref.setOnClickListener(v->refresh());top.addView(ref,new LinearLayout.LayoutParams(-2,dp(48)));root.addView(top);
        View line=new View(this);line.setBackgroundColor(Color.rgb(0,184,255));root.addView(line,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView sc=new ScrollView(this);box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(16),dp(14),dp(30));sc.addView(box);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void refresh(){
        box.removeAllViews();
        section("DASHBOARD OPERACIONAL");
        boolean listener=notificationListenerEnabled();
        boolean bot=prefs.getBoolean("bot_enabled",true);
        boolean mf=!TextUtils.isEmpty(safeSecure("sigma_user"))&&!TextUtils.isEmpty(safeSecure("sigma_pass"));
        String xe=prefs.getString("master_xcloud_email",""); boolean xc=!TextUtils.isEmpty(xe)&&!TextUtils.isEmpty(safeSecure("master_xcloud_password"));
        boolean motor1=prefs.getBoolean("sgb_enabled",false);
        statusCard("WhatsApp Listener",listener?"ATIVO":"PERMISSÃO NECESSÁRIA",listener?GREEN:RED,listener?"Captura disponível.":"Acesso às notificações do Android não está liberado.");
        statusCard("Bot principal",bot?"ATIVO":"PAUSADO",bot?GREEN:AMBER,"Estado salvo no Master Responde.");
        statusCard("MasterFlix",mf?"CONFIGURADO":"SEM CREDENCIAIS",mf?GREEN:AMBER,"Somente leitura das credenciais protegidas; nenhum login é executado.");
        String xs="indisponível";int q=0;try{xs=MasterXCloudOps.shortStatus(this);q=MasterXCloudOps.queueSize();}catch(Throwable ignored){}
        statusCard("Master XCloud",xc?("CONFIGURADO • "+xs.toUpperCase(Locale.ROOT)):"SEM CREDENCIAIS",xc?GREEN:AMBER,"Fila atual: "+q+" • nenhuma operação é disparada por esta tela.");
        long next=prefs.getLong("sgb_next_at",0L);
        statusCard("Motor 1 • Avisos",motor1?"LIGADO":"DESLIGADO",motor1?GREEN:AMBER,motor1?("Próximo ciclo: "+fmt(next)):"A configuração permanece intacta.");

        String day=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());
        int ok=prefs.getInt("xcloud_ok_"+day,0), fail=prefs.getInt("xcloud_fail_"+day,0);
        box.addView(gap(4));section("RESUMO DE HOJE");
        info("XCloud",ok+" sucesso(s) • "+fail+" falha(s)");
        info("Ativar / Reset / Excluir",prefs.getInt("xcloud_activate_"+day,0)+" / "+prefs.getInt("xcloud_reset_"+day,0)+" / "+prefs.getInt("xcloud_delete_"+day,0));
        info("Último resultado XCloud",prefs.getString("xcloud_last_result","Nenhuma operação registrada."));

        section("HISTÓRICO CENTRALIZADO");
        renderGeneralHistory();
        box.addView(gap(6));
        TextView hx=tv("MASTER XCLOUD",12,Color.rgb(0,210,255),true);box.addView(hx);
        String hxt;try{hxt=MasterXCloudOps.historyText(this);}catch(Throwable e){hxt="Histórico XCloud indisponível.";}
        box.addView(tv(hxt,12,Color.rgb(185,195,205),false));

        section("BACKUP / RESTAURAÇÃO DAS CONFIGURAÇÕES");
        box.addView(tv("O backup inclui preferências e configurações do Master Responde. Senhas protegidas pelo AndroidKeyStore não são copiadas para a área de transferência.",11,MUTED,false));
        Button exp=btn("COPIAR BACKUP COMPLETO");exp.setOnClickListener(v->copyBackup());box.addView(exp,new LinearLayout.LayoutParams(-1,dp(50)));
        Button imp=btn("RESTAURAR BACKUP DA ÁREA DE TRANSFERÊNCIA");imp.setOnClickListener(v->confirmImport());box.addView(imp,new LinearLayout.LayoutParams(-1,dp(50)));

        section("SAÚDE DO ANDROID");
        boolean batt=batteryUnrestricted();
        boolean notif=notificationsAllowed();
        health("Acesso às notificações",listener,"Necessário para captar mensagens do WhatsApp Business.");
        health("Notificações do app",notif,"Importante para alertas e estado operacional.");
        health("Bateria sem restrição",batt,"Reduz o risco de o Android interromper tarefas em segundo plano.");
        Button nset=btn("ABRIR ACESSO ÀS NOTIFICAÇÕES");nset.setOnClickListener(v->open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));box.addView(nset,new LinearLayout.LayoutParams(-1,dp(50)));
        Button bset=btn("ABRIR CONFIGURAÇÃO DE BATERIA");bset.setOnClickListener(v->open(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));box.addView(bset,new LinearLayout.LayoutParams(-1,dp(50)));
        Button aset=btn("ABRIR INFORMAÇÕES DO APP");aset.setOnClickListener(v->{try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Throwable e){toast("Não foi possível abrir as configurações.");}});box.addView(aset,new LinearLayout.LayoutParams(-1,dp(50)));

        box.addView(gap(12));box.addView(tv("Central somente de supervisão. Nenhum botão desta tela executa Ativar, Reset, Excluir, teste MasterFlix, envio em grupo ou qualquer outro motor.",11,Color.rgb(110,125,140),false));
    }

    private void section(String s){box.addView(gap(10));box.addView(tv(s,12,Color.rgb(255,156,0),true));}
    private void statusCard(String title,String state,int color,String detail){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(13),dp(10),dp(13),dp(10));c.setBackgroundColor(Color.rgb(15,18,23));c.addView(tv("● "+title,15,color,true));c.addView(tv(state,13,Color.WHITE,true));c.addView(tv(detail,11,MUTED,false));box.addView(c,new LinearLayout.LayoutParams(-1,-2));box.addView(gap(8));}
    private void info(String k,String v){box.addView(tv(k+"\n"+v,13,Color.rgb(205,215,225),false));}
    private void health(String title,boolean ok,String detail){box.addView(tv((ok?"● OK • ":"● ATENÇÃO • ")+title+"\n"+detail,12,ok?GREEN:AMBER,false));}

    private void renderGeneralHistory(){
        box.addView(tv("ATENDIMENTOS / RESPOSTAS",12,Color.rgb(0,210,255),true));
        try{JSONArray a=HistoryStore.get(this);if(a.length()==0){box.addView(tv("Nenhum atendimento registrado.",12,MUTED,false));return;}StringBuilder b=new StringBuilder();for(int i=0;i<a.length()&&i<30;i++){JSONObject o=a.getJSONObject(i);b.append(o.optString("time")).append(" • ").append(o.optString("conversation")).append("\n").append(o.optString("incoming")).append(" → ").append(o.optString("reply")).append(" • ").append(o.optString("status")).append("\n\n");}box.addView(tv(b.toString(),12,Color.rgb(185,195,205),false));}catch(Throwable e){box.addView(tv("Histórico geral indisponível.",12,MUTED,false));}
    }

    private void copyBackup(){
        try{JSONObject root=new JSONObject();root.put("format","MASTER_RESPONDE_BACKUP_V1");root.put("created_at",System.currentTimeMillis());root.put("master_responde",encodePrefs(getSharedPreferences("master_responde",MODE_PRIVATE)));root.put("master_history",encodePrefs(getSharedPreferences("master_history",MODE_PRIVATE)));android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("MASTER RESPONDE BACKUP",root.toString()));toast("Backup copiado. Credenciais seguras não foram expostas.");}catch(Throwable e){toast("Falha ao gerar backup.");}
    }
    private JSONObject encodePrefs(SharedPreferences sp)throws Exception{JSONObject out=new JSONObject();for(Map.Entry<String,?> e:sp.getAll().entrySet()){Object v=e.getValue();JSONObject o=new JSONObject();if(v instanceof Boolean){o.put("t","b");o.put("v",v);}else if(v instanceof Integer){o.put("t","i");o.put("v",v);}else if(v instanceof Long){o.put("t","l");o.put("v",v);}else if(v instanceof Float){o.put("t","f");o.put("v",v);}else if(v instanceof Set){o.put("t","sset");JSONArray a=new JSONArray();for(Object x:(Set<?>)v)a.put(String.valueOf(x));o.put("v",a);}else{o.put("t","s");o.put("v",v==null?"":String.valueOf(v));}out.put(e.getKey(),o);}return out;}
    private void confirmImport(){new AlertDialog.Builder(this).setTitle("Restaurar configurações?").setMessage("As preferências do backup substituirão as atuais. Os motores não serão iniciados nem executados durante a restauração.").setNegativeButton("CANCELAR",null).setPositiveButton("RESTAURAR",(d,w)->importBackup()).show();}
    private void importBackup(){
        try{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(!cm.hasPrimaryClip()){toast("Área de transferência vazia.");return;}CharSequence cs=cm.getPrimaryClip().getItemAt(0).coerceToText(this);JSONObject root=new JSONObject(cs.toString());if(!"MASTER_RESPONDE_BACKUP_V1".equals(root.optString("format"))){toast("Backup inválido.");return;}decodePrefs(getSharedPreferences("master_responde",MODE_PRIVATE),root.getJSONObject("master_responde"));decodePrefs(getSharedPreferences("master_history",MODE_PRIVATE),root.getJSONObject("master_history"));toast("Configurações restauradas. Nenhum motor foi executado.");refresh();}catch(Throwable e){toast("Não foi possível restaurar o backup.");}
    }
    private void decodePrefs(SharedPreferences sp,JSONObject obj)throws Exception{SharedPreferences.Editor ed=sp.edit();Iterator<String> it=obj.keys();while(it.hasNext()){String k=it.next();JSONObject o=obj.getJSONObject(k);String t=o.optString("t","s");if("b".equals(t))ed.putBoolean(k,o.optBoolean("v"));else if("i".equals(t))ed.putInt(k,o.optInt("v"));else if("l".equals(t))ed.putLong(k,o.optLong("v"));else if("f".equals(t))ed.putFloat(k,(float)o.optDouble("v"));else if("sset".equals(t)){JSONArray a=o.optJSONArray("v");Set<String>s=new HashSet<>();if(a!=null)for(int i=0;i<a.length();i++)s.add(a.optString(i));ed.putStringSet(k,s);}else ed.putString(k,o.optString("v",""));}ed.apply();}

    private boolean notificationListenerEnabled(){try{String e=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return e!=null&&e.toLowerCase(Locale.ROOT).contains(getPackageName().toLowerCase(Locale.ROOT));}catch(Throwable x){return false;}}
    private boolean notificationsAllowed(){try{if(Build.VERSION.SDK_INT>=24)return ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled();return true;}catch(Throwable e){return true;}}
    private boolean batteryUnrestricted(){try{android.os.PowerManager p=(android.os.PowerManager)getSystemService(POWER_SERVICE);return Build.VERSION.SDK_INT<23||p.isIgnoringBatteryOptimizations(getPackageName());}catch(Throwable e){return false;}}
    private String safeSecure(String k){try{String v=secure.get(k);return v==null?"":v;}catch(Throwable e){return "";}}
    private String fmt(long t){if(t<=0)return "não programado";try{return new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(t));}catch(Throwable e){return "indisponível";}}
    private void open(String action){try{startActivity(new Intent(action));}catch(Throwable e){toast("Não foi possível abrir esta configuração.");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
''',encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
entry='<activity android:name=".OperationsCenterActivity" android:screenOrientation="portrait" android:exported="false"/>\n'
if '.OperationsCenterActivity' not in m:
    if '</application>' not in m: raise SystemExit('ERRO v2.1.47: </application> não encontrado')
    m=m.replace('</application>',entry+'</application>',1)
manifest.write_text(m,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
if 'OperationsCenterActivity.class' not in d:
    anchor='panel.addView(sideItem("◉","Diagnóstico dos Motores",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MotorDiagnosticsActivity.class));}catch(Throwable e){Toast.makeText(this,"Diagnóstico indisponível",Toast.LENGTH_SHORT).show();}}));'
    if anchor not in d: raise SystemExit('ERRO v2.1.47: item Diagnóstico não encontrado')
    insert=anchor+'\n  panel.addView(sideItem("▣","Central Operacional",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,OperationsCenterActivity.class));}catch(Throwable e){Toast.makeText(this,"Central Operacional indisponível",Toast.LENGTH_SHORT).show();}}));'
    d=d.replace(anchor,insert,1)
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 118',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.47'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (java/'OperationsCenterActivity.java','CENTRAL OPERACIONAL'),
 (java/'OperationsCenterActivity.java','HISTÓRICO CENTRALIZADO'),
 (java/'OperationsCenterActivity.java','COPIAR BACKUP COMPLETO'),
 (java/'OperationsCenterActivity.java','SAÚDE DO ANDROID'),
 (java/'OperationsCenterActivity.java','Nenhum motor foi executado'),
 (manifest,'.OperationsCenterActivity'),
 (dash,'OperationsCenterActivity.class'),
 (gradle,"versionName '2.1.47'")]
for p,mk in checks:
    if mk not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.47: requisito ausente '+mk)

# Garantia estrutural: o patch não abre nem grava arquivos de motor.
for forbidden in ['MasterXCloudBackgroundAutomation.java','MasterXCloudWhatsAppFlow.java','MasterflixActivity.java','WhatsAppBusinessCaptureService.java','ScheduledGroupBroadcast.java','MasterXCloudActivity.java']:
    if forbidden in '\n'.join(str(x) for x in [dash,manifest,gradle]): raise SystemExit('ERRO v2.1.47: referência indevida de motor')

print('v2.1.47: dashboard operacional, histórico central, backup e saúde Android adicionados sem alterar nenhum motor')
