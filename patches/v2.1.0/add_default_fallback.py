from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
engine=java/'CommandEngine.java'
settings=java/'MessageSettingsActivity.java'
gradle=app/'build.gradle'

# Motor: se nenhum comando casar, usa resposta padrão opcional.
e=engine.read_text(encoding='utf-8')
old='''        return null;\n    }\n}\n'''
new='''        boolean fallbackEnabled = p.getBoolean("fallback_enabled", false);\n        String fallbackReply = p.getString("fallback_reply", "Olá! Recebemos sua mensagem. Em breve retornaremos.");\n        if (fallbackEnabled && fallbackReply != null && !fallbackReply.trim().isEmpty()) {\n            p.edit().putString("last_matched_command", "RESPOSTA_PADRAO").apply();\n            return fallbackReply.trim();\n        }\n        return null;\n    }\n}\n'''
if old not in e: raise SystemExit('ERRO v2.1.10: final do CommandEngine não encontrado')
e=e.replace(old,new,1)
engine.write_text(e,encoding='utf-8')

# Tela separada para manter a configuração simples e não arriscar os comandos já aprovados.
(java/'FallbackSettingsActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.Activity;import android.os.Bundle;import android.content.SharedPreferences;import android.graphics.Color;import android.graphics.Typeface;import android.widget.*;import android.view.ViewGroup;
public class FallbackSettingsActivity extends Activity{
 private SharedPreferences p; private Switch enabled; private EditText reply;
 @Override public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
 private ScrollView build(){ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(3,7,12));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,32,28,40);s.addView(r,new ViewGroup.LayoutParams(-1,-2));TextView t=new TextView(this);t.setText("RESPOSTA PADRÃO");t.setTextSize(24);t.setTextColor(Color.WHITE);t.setTypeface(null,Typeface.BOLD);r.addView(t);TextView sub=new TextView(this);sub.setText("Usada quando nenhuma regra/comando corresponder");sub.setTextColor(Color.rgb(0,214,255));sub.setTextSize(13);r.addView(sub);enabled=new Switch(this);enabled.setText("Ativar resposta padrão");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean("fallback_enabled",false));r.addView(enabled);reply=new EditText(this);reply.setText(p.getString("fallback_reply","Olá! Recebemos sua mensagem. Em breve retornaremos."));reply.setHint("Digite a resposta padrão");reply.setTextColor(Color.WHITE);reply.setHintTextColor(Color.GRAY);reply.setMinLines(4);r.addView(reply,new LinearLayout.LayoutParams(-1,-2));Button save=new Button(this);save.setText("SALVAR RESPOSTA PADRÃO");save.setOnClickListener(v->{String x=reply.getText().toString().trim();if(enabled.isChecked()&&x.isEmpty()){Toast.makeText(this,"Digite uma resposta",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("fallback_enabled",enabled.isChecked()).putString("fallback_reply",x).apply();Toast.makeText(this,"Resposta padrão salva",Toast.LENGTH_SHORT).show();});r.addView(save);return s;}
}''',encoding='utf-8')

# Adiciona botão na tela de comandos, sem alterar os 4 slots existentes.
s=settings.read_text(encoding='utf-8')
needle='root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));return s;}'
repl='root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));space(root,10);Button fallback=new Button(this);fallback.setAllCaps(false);fallback.setText("RESPOSTA PADRÃO");fallback.setOnClickListener(v->startActivity(new android.content.Intent(this,FallbackSettingsActivity.class)));root.addView(fallback,new LinearLayout.LayoutParams(-1,dp(52)));return s;}'
if needle not in s: raise SystemExit('ERRO v2.1.10: ponto da tela COMANDOS não encontrado')
s=s.replace(needle,repl,1)
settings.write_text(s,encoding='utf-8')

# Manifest
manifest=app/'src/main/AndroidManifest.xml'
m=manifest.read_text(encoding='utf-8')
if 'FallbackSettingsActivity' not in m:
    m=m.replace('</application>','<activity android:name=".FallbackSettingsActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

# Versão final desta etapa
g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 81',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.10'",g,count=1)
gradle.write_text(g,encoding='utf-8')

for pth,mark in [(engine,'fallback_enabled'),(settings,'FallbackSettingsActivity.class'),(java/'FallbackSettingsActivity.java','SALVAR RESPOSTA PADRÃO'),(manifest,'.FallbackSettingsActivity')]:
    if mark not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.10: ausente '+mark)
print('v2.1.10: resposta padrão configurável criada; comandos e métricas preservados')