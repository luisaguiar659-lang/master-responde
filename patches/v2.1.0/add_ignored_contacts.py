from pathlib import Path
import re

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'; gradle=app/'build.gradle'

(java/'IgnoredContacts.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import org.json.*;
public final class IgnoredContacts{
 private IgnoredContacts(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences("master_responde",Context.MODE_PRIVATE);}
 private static String norm(String s){return s==null?"":s.toLowerCase().replaceAll("[^\\p{L}\\p{N}+]","").trim();}
 public static boolean isIgnored(Context c,String conversation){
  String x=norm(conversation);if(x.isEmpty())return false;
  try{JSONArray a=new JSONArray(p(c).getString("ignored_contacts_json","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String n=norm(o.optString("name"));String ph=norm(o.optString("phone"));if((!n.isEmpty()&&n.equals(x))||(!ph.isEmpty()&&ph.equals(x))||(!ph.isEmpty()&&x.endsWith(ph))||(!x.isEmpty()&&ph.endsWith(x)))return true;}}catch(Exception ignored){}
  return false;
 }
}''',encoding='utf-8')

(java/'IgnoredContactsActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.graphics.Typeface;import android.provider.ContactsContract;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
public class IgnoredContactsActivity extends Activity{
 static final int REQ=620;SharedPreferences p;LinearLayout list;EditText search;ArrayList<Item> all=new ArrayList<>();HashSet<String> selected=new HashSet<>();
 static class Item{String name,phone;Item(String n,String p){name=n;phone=p;}String key(){return name+"|"+phone;}}
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);loadSelected();setContentView(build());if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},REQ);else loadContacts();}
 public void onRequestPermissionsResult(int r,String[] ps,int[] g){super.onRequestPermissionsResult(r,ps,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)loadContacts();else renderMessage("Permissão de contatos negada. Ative em Configurações do Android para escolher contatos.");}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,9,8,9);return t;}
 View build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,26,24,24);root.setBackgroundColor(Color.rgb(1,5,9));root.addView(tv("CONTATOS IGNORADOS",23,true));TextView sub=tv("Selecione diretamente da agenda do telefone",13,false);sub.setTextColor(Color.rgb(0,225,255));root.addView(sub);search=new EditText(this);search.setHint("Pesquisar contato...");search.setTextColor(Color.WHITE);search.setHintTextColor(Color.GRAY);search.setSingleLine(true);root.addView(search,new LinearLayout.LayoutParams(-1,-2));TextView count=tv("Toque no contato para marcar/desmarcar",11,false);count.setTextColor(Color.LTGRAY);root.addView(count);ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list,new ScrollView.LayoutParams(-1,-2));root.addView(sc,new LinearLayout.LayoutParams(-1,0,1f));Button save=new Button(this);save.setText("SALVAR CONTATOS IGNORADOS");save.setOnClickListener(v->save());root.addView(save);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render();}public void afterTextChanged(android.text.Editable e){}});return root;}
 void loadSelected(){try{JSONArray a=new JSONArray(p.getString("ignored_contacts_json","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);selected.add(o.optString("name")+"|"+o.optString("phone"));}}catch(Exception ignored){}}
 void loadContacts(){all.clear();HashSet<String> seen=new HashSet<>();Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC");if(c!=null)while(c.moveToNext()){String n=c.getString(0),ph=c.getString(1);if(n==null)n="Sem nome";if(ph==null)ph="";String k=n+"|"+ph;if(seen.add(k))all.add(new Item(n,ph));}}catch(Exception e){}finally{if(c!=null)c.close();}render();}
 void renderMessage(String msg){if(list==null)return;list.removeAllViews();list.addView(tv(msg,14,false));}
 void render(){if(list==null)return;list.removeAllViews();String q=search==null?"":search.getText().toString().toLowerCase().trim();int shown=0;for(Item it:all){if(!q.isEmpty()&&!it.name.toLowerCase().contains(q)&&!it.phone.toLowerCase().contains(q))continue;CheckBox cb=new CheckBox(this);cb.setText(it.name+"\n"+it.phone);cb.setTextColor(Color.WHITE);cb.setPadding(8,8,8,8);cb.setChecked(selected.contains(it.key()));cb.setOnCheckedChangeListener((b,on)->{if(on)selected.add(it.key());else selected.remove(it.key());});list.addView(cb,new LinearLayout.LayoutParams(-1,-2));shown++;}if(shown==0)list.addView(tv(all.isEmpty()?"Nenhum contato carregado.":"Nenhum contato encontrado.",14,false));}
 void save(){JSONArray a=new JSONArray();for(Item it:all)if(selected.contains(it.key()))try{JSONObject o=new JSONObject();o.put("name",it.name);o.put("phone",it.phone);a.put(o);}catch(Exception ignored){}p.edit().putString("ignored_contacts_json",a.toString()).remove("ignored_contacts").apply();Toast.makeText(this,selected.size()+" contato(s) ignorado(s)",Toast.LENGTH_SHORT).show();}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
needle='''        // PRIVADO: horário de atendimento vem antes de comandos/resposta padrão.'''
repl='''        // PRIVADO: contatos escolhidos da agenda têm prioridade máxima.\n        if (IgnoredContacts.isIgnored(this, conversation)) {\n            prefs().edit().putString("accessibility_last_status", "Contato ignorado • nenhuma automação enviada").apply();\n            return;\n        }\n\n        // PRIVADO: horário de atendimento vem antes de comandos/resposta padrão.'''
if 'IgnoredContacts.isIgnored(this, conversation)' not in s:
 if needle not in s: raise SystemExit('ERRO v2.1.21: ponto do fluxo privado não encontrado')
 s=s.replace(needle,repl,1)
svc.write_text(s,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
needle2='''  panel.addView(sideItem("⚙","Configurações",Color.WHITE,v->{d.dismiss();openSettings();}));'''
repl2='''  panel.addView(sideItem("⚙","Configurações",Color.WHITE,v->{d.dismiss();openSettings();}));\n  panel.addView(sideItem("⊘","Contatos Ignorados",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,IgnoredContactsActivity.class));}));'''
if 'IgnoredContactsActivity.class' not in d:
 if needle2 not in d: raise SystemExit('ERRO v2.1.21: menu configurações não encontrado')
 d=d.replace(needle2,repl2,1)
for oldv in ['v2.1.18','v2.1.19','v2.1.20']:
 d=d.replace('brand.addView(text("'+oldv+'",10,MUTED,false));','brand.addView(text("v2.1.21",10,MUTED,false));',1)
dash.write_text(d,encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if 'android.permission.READ_CONTACTS' not in m:m=m.replace('<application','<uses-permission android:name="android.permission.READ_CONTACTS"/>\n    <application',1)
if '.IgnoredContactsActivity' not in m:m=m.replace('</application>','<activity android:name=".IgnoredContactsActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 92',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.21'",g,count=1);gradle.write_text(g,encoding='utf-8')
for pth,mk in [(java/'IgnoredContacts.java','ignored_contacts_json'),(java/'IgnoredContactsActivity.java','ContactsContract.CommonDataKinds.Phone'),(svc,'IgnoredContacts.isIgnored'),(dash,'Contatos Ignorados'),(manifest,'android.permission.READ_CONTACTS'),(gradle,"versionName '2.1.21'")]:
 if mk not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.21: requisito ausente '+mk)
print('v2.1.21: contatos ignorados agora são escolhidos diretamente da agenda do telefone')