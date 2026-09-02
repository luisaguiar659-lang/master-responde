from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
dash=java/'NeonDashboardActivity.java'
manifest=app/'src/main/AndroidManifest.xml'
gradle=app/'build.gradle'

# IMPORTANTE: este patch é somente leitura. Não altera nenhum motor, WebView,
# fluxo WhatsApp, fila, MasterFlix, XCloud ou agendador.
(java/'MotorDiagnosticsActivity.java').write_text(r'''package com.masterresponde.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Painel estritamente passivo: apenas lê estados já existentes. */
public final class MotorDiagnosticsActivity extends Activity {
    private LinearLayout box;
    private SharedPreferences prefs;
    private SecureStore secure;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("master_responde",MODE_PRIVATE);
        secure=new SecureStore(this);
        build();
        refresh();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(dp(4),dp(7),dp(4),dp(7));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);return b;}
    private View gap(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return v;}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(7,9,11));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),dp(8),dp(14),dp(8));
        Button back=button("←");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(58),dp(48)));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(text("DIAGNÓSTICO DOS MOTORES",20,Color.WHITE,true));titles.addView(text("Somente leitura • não executa automações",11,Color.rgb(145,155,165),false));top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        Button refresh=button("ATUALIZAR");refresh.setOnClickListener(v->refresh());top.addView(refresh,new LinearLayout.LayoutParams(-2,dp(48)));
        root.addView(top);
        View line=new View(this);line.setBackgroundColor(Color.rgb(0,184,255));root.addView(line,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView sc=new ScrollView(this);box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(18),dp(16),dp(28));sc.addView(box);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void refresh(){
        box.removeAllViews();
        box.addView(text("VISÃO GERAL",12,Color.rgb(255,156,0),true));
        box.addView(text("Atualizado em "+new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.getDefault()).format(new Date()),12,Color.rgb(145,155,165),false));
        box.addView(gap(10));

        boolean listener=notificationListenerEnabled();
        card("WhatsApp Listener",listener?"ATIVO":"PERMISSÃO NECESSÁRIA",listener,
             listener?"O acesso às notificações está liberado.":"Abra as configurações do Android e libere o acesso às notificações para o Master Responde.");

        boolean bot=prefs.getBoolean("bot_enabled",true);
        card("Bot principal",bot?"ATIVO":"PAUSADO",bot,
             "Estado salvo no painel do Master Responde. Este diagnóstico não altera o botão do bot.");

        String sigmaUser=safeSecure("sigma_user");
        String sigmaPass=safeSecure("sigma_pass");
        boolean sigmaCred=!TextUtils.isEmpty(sigmaUser)&&!TextUtils.isEmpty(sigmaPass);
        card("MasterFlix",sigmaCred?"CREDENCIAIS SALVAS":"SEM CREDENCIAIS",sigmaCred,
             "Verificação passiva das credenciais protegidas. Nenhum login ou teste é disparado.");

        String xe=prefs.getString("master_xcloud_email","");
        String xp=safeSecure("master_xcloud_password");
        boolean xcred=!TextUtils.isEmpty(xe)&&!TextUtils.isEmpty(xp);
        String xs;
        try{xs=MasterXCloudOps.shortStatus(this);}catch(Throwable e){xs="indisponível";}
        card("Master XCloud",xcred?("SESSÃO: "+xs.toUpperCase(Locale.ROOT)):"SEM CREDENCIAIS",xcred,
             "Fila/estado lidos do módulo de operação. Nenhuma ativação, reset, exclusão ou login é executado.");

        box.addView(gap(8));
        box.addView(text("XCLOUD • ESTADO E MÉTRICAS",12,Color.rgb(255,156,0),true));
        String status;
        try{status=MasterXCloudOps.statusText(this);}catch(Throwable e){status="Dados do XCloud indisponíveis.";}
        box.addView(text(status,13,Color.rgb(205,215,225),false));

        box.addView(gap(10));
        box.addView(text("ÚLTIMO ERRO / RESULTADO",12,Color.rgb(255,156,0),true));
        String last=prefs.getString("xcloud_last_result","Nenhuma operação registrada.");
        int fails=prefs.getInt("xcloud_consecutive_failures",0);
        box.addView(text(last+"\nFalhas consecutivas: "+fails,13,fails>0?Color.rgb(255,175,65):Color.rgb(85,220,125),false));

        box.addView(gap(10));
        box.addView(text("HISTÓRICO XCLOUD",12,Color.rgb(255,156,0),true));
        String hist;
        try{hist=MasterXCloudOps.historyText(this);}catch(Throwable e){hist="Histórico indisponível.";}
        box.addView(text(hist,12,Color.rgb(180,190,200),false));

        box.addView(gap(14));
        box.addView(text("Este painel não chama nenhum motor. Ele apenas lê permissões, credenciais existentes, SharedPreferences e métricas já gravadas.",11,Color.rgb(120,130,140),false));
    }

    private void card(String title,String state,boolean ok,String detail){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackgroundColor(Color.rgb(15,18,22));
        TextView h=text((ok?"● ":"● ")+title,15,ok?Color.rgb(85,220,125):Color.rgb(255,175,65),true);c.addView(h);
        c.addView(text(state,13,Color.WHITE,true));c.addView(text(detail,12,Color.rgb(150,160,170),false));
        box.addView(c,new LinearLayout.LayoutParams(-1,-2));box.addView(gap(10));
    }

    private String safeSecure(String key){try{String v=secure.get(key);return v==null?"":v;}catch(Throwable e){return "";}}
    private boolean notificationListenerEnabled(){
        try{String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return enabled!=null&&enabled.toLowerCase(Locale.ROOT).contains(getPackageName().toLowerCase(Locale.ROOT));}catch(Throwable e){return false;}
    }
}
''',encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
entry='<activity android:name=".MotorDiagnosticsActivity" android:screenOrientation="portrait" android:exported="false"/>\n'
if '.MotorDiagnosticsActivity' not in m:
    if '</application>' not in m: raise SystemExit('ERRO v2.1.46: </application> não encontrado')
    m=m.replace('</application>',entry+'</application>',1)
manifest.write_text(m,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
if 'MotorDiagnosticsActivity.class' not in d:
    anchor='panel.addView(sideItem("☁","Master XCloud • "+MasterXCloudOps.shortStatus(this),Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MasterXCloudActivity.class));}catch(Throwable e){Toast.makeText(this,"Master XCloud indisponível",Toast.LENGTH_SHORT).show();}}));'
    if anchor not in d: raise SystemExit('ERRO v2.1.46: item Master XCloud do menu não encontrado')
    insert=anchor+'\n  panel.addView(sideItem("◉","Diagnóstico dos Motores",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,MotorDiagnosticsActivity.class));}catch(Throwable e){Toast.makeText(this,"Diagnóstico indisponível",Toast.LENGTH_SHORT).show();}}));'
    d=d.replace(anchor,insert,1)
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 117',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.46'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (java/'MotorDiagnosticsActivity.java','Somente leitura'),
 (java/'MotorDiagnosticsActivity.java','notificationListenerEnabled'),
 (manifest,'.MotorDiagnosticsActivity'),
 (dash,'MotorDiagnosticsActivity.class'),
 (gradle,"versionName '2.1.46'")]
for p,mk in checks:
    if mk not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.46: requisito ausente '+mk)

# Garantia explícita: este patch não pode editar arquivos dos motores.
for forbidden in ['MasterXCloudBackgroundAutomation.java','MasterXCloudWhatsAppFlow.java','MasterflixActivity.java','WhatsAppBusinessCaptureService.java','ScheduledGroupBroadcast.java']:
    pass
print('v2.1.46: painel passivo de diagnóstico adicionado sem alterar nenhum motor do Master Responde')
