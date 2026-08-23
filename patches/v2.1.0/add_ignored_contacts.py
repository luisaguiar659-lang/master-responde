from pathlib import Path
import re

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'; gradle=app/'build.gradle'

(java/'IgnoredContacts.java').write_text(r'''package com.masterresponde.app;
import android.content.Context;import android.content.SharedPreferences;
public final class IgnoredContacts{
 private IgnoredContacts(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences("master_responde",Context.MODE_PRIVATE);}
 public static boolean isIgnored(Context c,String conversation){
  if(conversation==null)return false;String x=conversation.trim();if(x.isEmpty())return false;
  String raw=p(c).getString("ignored_contacts","");
  for(String line:raw.split("\\r?\\n")){String v=line.trim();if(!v.isEmpty()&&v.equalsIgnoreCase(x))return true;}
  return false;
 }
}''',encoding='utf-8')

(java/'IgnoredContactsActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.view.*;import android.widget.*;
public class IgnoredContactsActivity extends Activity{
 SharedPreferences p;EditText list;
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));r.addView(tv("CONTATOS IGNORADOS",23,true));TextView sub=tv("Esses contatos nunca recebem automações no privado.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);r.addView(tv("Digite exatamente o nome ou número que aparece na conversa do WhatsApp Business. Use um por linha.",12,false));list=new EditText(this);list.setText(p.getString("ignored_contacts",""));list.setHint("João da Silva\n+55 83 99999-9999\nCliente Teste");list.setTextColor(Color.WHITE);list.setHintTextColor(Color.GRAY);list.setGravity(Gravity.TOP);list.setMinLines(12);list.setBackgroundColor(Color.rgb(4,14,21));list.setPadding(18,18,18,18);r.addView(list,new LinearLayout.LayoutParams(-1,-2));Button save=new Button(this);save.setText("SALVAR CONTATOS IGNORADOS");save.setOnClickListener(v->{p.edit().putString("ignored_contacts",list.getText().toString()).apply();Toast.makeText(this,"Lista de ignorados salva",Toast.LENGTH_SHORT).show();});r.addView(save);Button clear=new Button(this);clear.setText("LIMPAR LISTA");clear.setOnClickListener(v->{list.setText("");p.edit().remove("ignored_contacts").apply();Toast.makeText(this,"Lista limpa",Toast.LENGTH_SHORT).show();});r.addView(clear);return sc;}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
# Insere somente após o roteamento de grupo ter retornado e antes do horário/comandos privados.
needle='''        // PRIVADO: horário de atendimento vem antes de comandos/resposta padrão.'''
repl='''        // PRIVADO: contatos ignorados têm prioridade máxima e nunca recebem automações.\n        if (IgnoredContacts.isIgnored(this, conversation)) {\n            prefs().edit().putString("accessibility_last_status", "Contato ignorado • nenhuma automação enviada").apply();\n            return;\n        }\n\n        // PRIVADO: horário de atendimento vem antes de comandos/resposta padrão.'''
if needle not in s: raise SystemExit('ERRO v2.1.20: ponto do fluxo privado não encontrado')
s=s.replace(needle,repl,1);svc.write_text(s,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
# Coloca item na seção Gerenciamento logo depois de Configurações.
needle2='''  panel.addView(sideItem("⚙","Configurações",Color.WHITE,v->{d.dismiss();openSettings();}));'''
repl2='''  panel.addView(sideItem("⚙","Configurações",Color.WHITE,v->{d.dismiss();openSettings();}));\n  panel.addView(sideItem("⊘","Contatos Ignorados",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,IgnoredContactsActivity.class));}));'''
if needle2 not in d: raise SystemExit('ERRO v2.1.20: item Configurações do menu lateral não encontrado')
d=d.replace(needle2,repl2,1)
d=d.replace('brand.addView(text("v2.1.18",10,MUTED,false));','brand.addView(text("v2.1.20",10,MUTED,false));',1)
d=d.replace('brand.addView(text("v2.1.19",10,MUTED,false));','brand.addView(text("v2.1.20",10,MUTED,false));',1)
dash.write_text(d,encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if '.IgnoredContactsActivity' not in m:m=m.replace('</application>','<activity android:name=".IgnoredContactsActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 91',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.20'",g,count=1);gradle.write_text(g,encoding='utf-8')

for p,mk in [(java/'IgnoredContacts.java','isIgnored'),(java/'IgnoredContactsActivity.java','SALVAR CONTATOS IGNORADOS'),(svc,'Contato ignorado • nenhuma automação enviada'),(dash,'Contatos Ignorados'),(manifest,'.IgnoredContactsActivity'),(gradle,"versionName '2.1.20'")]:
 if mk not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.20: requisito ausente '+mk)
print('v2.1.20: contatos ignorados criados; bloqueio somente no privado e antes de horário/comandos/resposta padrão')