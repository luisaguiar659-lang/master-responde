from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
engine = java_dir / 'CommandEngine.java'
dash = java_dir / 'NeonDashboardActivity.java'
settings = java_dir / 'NeonSettingsActivity.java'
manifest = app / 'src/main/AndroidManifest.xml'
gradle = app / 'build.gradle'

# Motor: quatro slots configuráveis persistidos em SharedPreferences.
engine.write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public final class CommandEngine {
    private static final String PREFS = "master_responde";
    private CommandEngine() {}

    private static final String[] DEFAULT_TRIGGERS = {"@info", "@status", "@ajuda", ""};
    private static final String[] DEFAULT_REPLIES = {
            "MASTER RESPONDE: sistema online e funcionando.",
            "MASTER RESPONDE: ONLINE • WhatsApp Business protegido • captura ativa.",
            "Comandos disponíveis: @info, @status, @ajuda",
            ""
    };

    public static String resolve(Context context, String message) {
        if (message == null) return null;
        String incoming = message.trim().toLowerCase(Locale.ROOT);
        if (incoming.isEmpty()) return null;

        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int i = 0; i < 4; i++) {
            String trigger = p.getString("cmd_trigger_" + i, DEFAULT_TRIGGERS[i]);
            String reply = p.getString("cmd_reply_" + i, DEFAULT_REPLIES[i]);
            boolean enabled = p.getBoolean("cmd_enabled_" + i, !DEFAULT_TRIGGERS[i].isEmpty());
            if (!enabled || trigger == null || reply == null) continue;
            String normalized = trigger.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && normalized.equals(incoming) && !reply.trim().isEmpty()) {
                p.edit().putString("last_matched_command", trigger.trim()).apply();
                return reply;
            }
        }
        return null;
    }
}
''', encoding='utf-8')

# Tela Neon simples para editar 4 comandos sem recompilar o APK.
(java_dir / 'MessageSettingsActivity.java').write_text(r'''package com.masterresponde.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MessageSettingsActivity extends Activity {
    private static final int BG=Color.rgb(3,7,12), CARD=Color.rgb(8,15,23), GREEN=Color.rgb(0,255,102), CYAN=Color.rgb(0,214,255), MUTED=Color.rgb(147,164,178);
    private static final String[] DT={"@info","@status","@ajuda",""};
    private static final String[] DR={"MASTER RESPONDE: sistema online e funcionando.","MASTER RESPONDE: ONLINE • WhatsApp Business protegido • captura ativa.","Comandos disponíveis: @info, @status, @ajuda",""};
    private SharedPreferences prefs;
    private final EditText[] triggers=new EditText[4], replies=new EditText[4];
    private final Switch[] enabled=new Switch[4];

    @Override protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("master_responde",MODE_PRIVATE);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);setContentView(build());}

    private ScrollView build(){ScrollView s=new ScrollView(this);s.setBackgroundColor(BG);LinearLayout root=col();root.setPadding(dp(16),dp(18),dp(16),dp(30));s.addView(root,new ViewGroup.LayoutParams(-1,-2));
        LinearLayout h=row();TextView back=text("‹",34,CYAN,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());h.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));h.addView(text("COMANDOS",24,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1f));root.addView(h);root.addView(text("Edite gatilho e resposta • sem recompilar",11,MUTED,false));space(root,16);
        for(int i=0;i<4;i++) root.addView(commandCard(i));
        Button save=new Button(this);save.setAllCaps(false);save.setText("SALVAR COMANDOS");save.setTextSize(15);save.setTypeface(Typeface.DEFAULT_BOLD);save.setTextColor(Color.BLACK);save.setBackground(bg(GREEN,GREEN,14,1));save.setOnClickListener(v->save());root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));return s;}

    private LinearLayout commandCard(int i){LinearLayout c=col();c.setPadding(dp(14),dp(12),dp(14),dp(14));c.setBackground(bg(CARD,CYAN,14,1));
        LinearLayout top=row();top.addView(text("COMANDO "+(i+1),12,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1f));Switch sw=new Switch(this);enabled[i]=sw;sw.setChecked(prefs.getBoolean("cmd_enabled_"+i,!DT[i].isEmpty()));top.addView(sw);c.addView(top);
        triggers[i]=input("Gatilho, ex.: @preco",prefs.getString("cmd_trigger_"+i,DT[i]));replies[i]=input("Resposta automática",prefs.getString("cmd_reply_"+i,DR[i]));c.addView(triggers[i]);space(c,8);c.addView(replies[i],new LinearLayout.LayoutParams(-1,dp(92)));space(c,12);return c;}

    private EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setText(value);e.setTextColor(Color.WHITE);e.setTextSize(13);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(bg(Color.rgb(3,10,16),Color.rgb(27,78,96),10,1));return e;}
    private void save(){SharedPreferences.Editor ed=prefs.edit();for(int i=0;i<4;i++){String t=triggers[i].getText().toString().trim();String r=replies[i].getText().toString().trim();if(enabled[i].isChecked()&&(t.isEmpty()||r.isEmpty())){Toast.makeText(this,"Comando "+(i+1)+": preencha gatilho e resposta",Toast.LENGTH_SHORT).show();return;}ed.putString("cmd_trigger_"+i,t).putString("cmd_reply_"+i,r).putBoolean("cmd_enabled_"+i,enabled[i].isChecked());}ed.apply();Toast.makeText(this,"Comandos salvos",Toast.LENGTH_SHORT).show();}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}private TextView text(String s,int sp,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);return t;}private GradientDrawable bg(int fill,int stroke,int radius,int width){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(width),stroke);return d;}private void space(LinearLayout l,int h){TextView v=new TextView(this);v.setHeight(dp(h));l.addView(v);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
''', encoding='utf-8')

# Configurações passa a abrir a nova tela.
s = settings.read_text(encoding='utf-8')
s = re.sub(r'private void openMessages\(\)\{.*?\}', 'private void openMessages(){startActivity(new Intent(this,MessageSettingsActivity.class));}', s, count=1, flags=re.S)
settings.write_text(s, encoding='utf-8')

# Botão COMANDOS do dashboard abre a tela configurável.
d = dash.read_text(encoding='utf-8')
d = d.replace('nav("▦\\nCOMANDOS",Color.WHITE,v->Toast.makeText(this,"Em breve",Toast.LENGTH_SHORT).show())', 'nav("▦\\nCOMANDOS",Color.WHITE,v->startActivity(new Intent(this,MessageSettingsActivity.class)))')
dash.write_text(d, encoding='utf-8')

# Registra Activity no Manifest.
m = manifest.read_text(encoding='utf-8')
if 'android:name=".MessageSettingsActivity"' not in m:
    anchor = '<activity android:name=".NeonSettingsActivity"'
    idx = m.find(anchor)
    if idx < 0: raise SystemExit('ERRO v2.1.8: NeonSettingsActivity não encontrada no Manifest')
    m = m[:idx] + '<activity android:name=".MessageSettingsActivity" android:screenOrientation="portrait" android:exported="false"/>\n        ' + m[idx:]
manifest.write_text(m, encoding='utf-8')

# Versiona.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 79', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.8'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# Validações.
checks = {
    engine: ['cmd_trigger_', 'cmd_reply_', 'last_matched_command'],
    java_dir / 'MessageSettingsActivity.java': ['SALVAR COMANDOS', 'cmd_enabled_', 'Comandos salvos'],
    dash: ['MessageSettingsActivity.class'],
    manifest: ['.MessageSettingsActivity']
}
for path, markers in checks.items():
    txt=path.read_text(encoding='utf-8')
    for marker in markers:
        if marker not in txt: raise SystemExit('ERRO v2.1.8: ausente '+marker+' em '+path.name)
print('v2.1.8: comandos configuráveis pelo painel criados; motor e métricas preservados')
