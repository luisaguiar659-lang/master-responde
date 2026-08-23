from pathlib import Path
import re

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'; gradle=app/'build.gradle'

(java/'MasterflixTestCooldown.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.text.Normalizer;import java.util.*;
public final class MasterflixTestCooldown{
 private static final String PREF="master_responde"; private MasterflixTestCooldown(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 private static String key(String conversation){String x=conversation==null?"":Normalizer.normalize(conversation,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();return "masterflix_test_last_"+Integer.toHexString(x.hashCode());}
 public static int hours(Context c){int h=p(c).getInt("masterflix_test_cooldown_hours",24);return Math.max(0,h);}
 public static long remainingMillis(Context c,String conversation){int h=hours(c);if(h<=0)return 0L;long last=p(c).getLong(key(conversation),0L);long rem=h*3600000L-(System.currentTimeMillis()-last);return Math.max(0L,rem);}
 public static void mark(Context c,String conversation){p(c).edit().putLong(key(conversation),System.currentTimeMillis()).apply();}
 public static String remainingText(long ms){long total=(ms+59999L)/60000L;long h=total/60L,m=total%60L;if(h>0&&m>0)return h+"h "+m+"min";if(h>0)return h+"h";return Math.max(1,m)+"min";}
}''',encoding='utf-8')

(java/'MasterflixAutomationSettingsActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;
public class MasterflixAutomationSettingsActivity extends Activity{
 SharedPreferences p;EditText test,reseller,hours;
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 EditText field(String hint,int lines){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);e.setMinLines(lines);e.setGravity(lines>1?Gravity.TOP:Gravity.CENTER_VERTICAL);return e;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));r.addView(tv("AUTOMAÇÕES MASTERFLIX",23,true));TextView sub=tv("Gatilhos e controle de solicitação de testes",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);
 r.addView(tv("GATILHOS DE TESTE",14,true));r.addView(tv("Um gatilho por linha. Ex.: teste, quero um teste, gerar teste",11,false));test=field("teste\nquero um teste\ngerar teste",5);test.setText(p.getString("masterflix_test_triggers","teste\nquero um teste\nquero teste\ngerar teste\ngerar um teste"));r.addView(test);
 r.addView(tv("INTERVALO PARA NOVO TESTE",14,true));r.addView(tv("Tempo, em horas, que a mesma pessoa precisa aguardar para solicitar outro teste. Use 0 para desativar o limite.",11,false));hours=field("24",1);hours.setInputType(InputType.TYPE_CLASS_NUMBER);hours.setText(String.valueOf(p.getInt("masterflix_test_cooldown_hours",24)));r.addView(hours);
 r.addView(tv("GATILHOS DE REVENDA",14,true));r.addView(tv("Um gatilho por linha. Ex.: quero ser revenda",11,false));reseller=field("quero ser revenda\nquero revenda",5);reseller.setText(p.getString("masterflix_reseller_triggers","quero ser revenda\nquero ser uma revenda\nquero revenda\nser revenda"));r.addView(reseller);
 Button save=new Button(this);save.setText("SALVAR AUTOMAÇÕES MASTERFLIX");save.setOnClickListener(v->save());r.addView(save);return sc;}
 void save(){String t=test.getText().toString().trim(),rv=reseller.getText().toString().trim();int h=24;try{h=Integer.parseInt(hours.getText().toString().trim());}catch(Exception ignored){}if(h<0)h=0;if(h>8760)h=8760;if(t.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de teste",Toast.LENGTH_SHORT).show();return;}if(rv.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de revenda",Toast.LENGTH_SHORT).show();return;}p.edit().putString("masterflix_test_triggers",t).putString("masterflix_reseller_triggers",rv).putInt("masterflix_test_cooldown_hours",h).apply();hours.setText(String.valueOf(h));Toast.makeText(this,"Configurações do MasterFlix salvas",Toast.LENGTH_SHORT).show();}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
old='''        if (MasterflixTestTrigger.matches(this, message)) {\n            String requestKey = conversation + "|" + message + "|" + System.currentTimeMillis();\n            if (TestTaskBridge.begin(n, requestKey)) {\n                boolean started = MasterflixBackgroundAutomation.start(this, n, prefs());\n                if (started) {\n                    prefs().edit().putString("last_test_status", "Teste: geração automática iniciada").apply();\n                    sendDirectReply(n, conversation, "Gerando seu teste, aguarde...");\n                } else {\n                    TestTaskBridge.complete();\n                    sendDirectReply(n, conversation, "Já existe uma geração de teste em andamento. Aguarde alguns instantes.");\n                }\n            }\n            return;\n        }'''
new='''        if (MasterflixTestTrigger.matches(this, message)) {\n            long cooldownRemaining = MasterflixTestCooldown.remainingMillis(this, conversation);\n            if (cooldownRemaining > 0L) {\n                sendDirectReply(n, conversation, "Você já solicitou um teste recentemente. Poderá solicitar outro em " + MasterflixTestCooldown.remainingText(cooldownRemaining) + ".");\n                prefs().edit().putString("last_test_status", "Teste: bloqueado pelo intervalo individual").apply();\n                return;\n            }\n            String requestKey = conversation + "|" + message + "|" + System.currentTimeMillis();\n            if (TestTaskBridge.begin(n, requestKey)) {\n                boolean started = MasterflixBackgroundAutomation.start(this, n, prefs());\n                if (started) {\n                    MasterflixTestCooldown.mark(this, conversation);\n                    prefs().edit().putString("last_test_status", "Teste: geração automática iniciada").apply();\n                    sendDirectReply(n, conversation, "Gerando seu teste, aguarde...");\n                } else {\n                    TestTaskBridge.complete();\n                    sendDirectReply(n, conversation, "Já existe uma geração de teste em andamento. Aguarde alguns instantes.");\n                }\n            }\n            return;\n        }'''
if old not in s: raise SystemExit('ERRO v2.1.25: bloco de gatilho de teste não encontrado')
s=s.replace(old,new,1);svc.write_text(s,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
needle='''  panel.addView(sideItem("▶","MasterFlix",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,MasterflixActivity.class));}));'''
repl='''  panel.addView(sideItem("▶","MasterFlix",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,MasterflixActivity.class));}));\n  panel.addView(sideItem("⚡","Automações MasterFlix",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,MasterflixAutomationSettingsActivity.class));}));'''
if needle not in d: raise SystemExit('ERRO v2.1.25: item MasterFlix no menu não encontrado')
d=d.replace(needle,repl,1)
for v in ['v2.1.22','v2.1.23','v2.1.24']:
 d=d.replace('brand.addView(text("'+v+'",10,MUTED,false));','brand.addView(text("v2.1.25",10,MUTED,false));')
dash.write_text(d,encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if '.MasterflixAutomationSettingsActivity' not in m:m=m.replace('</application>','<activity android:name=".MasterflixAutomationSettingsActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 96',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.25'",g,count=1);gradle.write_text(g,encoding='utf-8')
for p,mk in [(java/'MasterflixAutomationSettingsActivity.java','GATILHOS DE TESTE'),(java/'MasterflixAutomationSettingsActivity.java','GATILHOS DE REVENDA'),(java/'MasterflixAutomationSettingsActivity.java','INTERVALO PARA NOVO TESTE'),(java/'MasterflixTestCooldown.java','remainingMillis'),(svc,'MasterflixTestCooldown.mark'),(dash,'Automações MasterFlix'),(manifest,'.MasterflixAutomationSettingsActivity'),(gradle,"versionName '2.1.25'")]:
 if mk not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.25: requisito ausente '+mk)
print('v2.1.25: gatilhos teste/revenda configuráveis + intervalo individual de novo teste em horas')