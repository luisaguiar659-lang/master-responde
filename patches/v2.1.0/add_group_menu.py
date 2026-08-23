from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; gradle=app/'build.gradle'
# UI/engine standalone first: preserve stable notification core
(java/'GroupMenuStore.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import org.json.*;
public final class GroupMenuStore{
 private static final String P="master_prefs"; private GroupMenuStore(){}
 public static String trigger(Context c){return c.getSharedPreferences(P,0).getString("group_menu_trigger","@menu").trim();}
 public static JSONArray items(Context c){try{return new JSONArray(c.getSharedPreferences(P,0).getString("group_menu_items","[]"));}catch(Exception e){return new JSONArray();}}
 public static String menu(Context c){StringBuilder b=new StringBuilder();b.append(c.getSharedPreferences(P,0).getString("group_menu_title","📋 *MENU*"));JSONArray a=items(c);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.optBoolean("enabled",true))b.append("\n").append(o.optString("key")).append(" - ").append(o.optString("label"));}catch(Exception ignored){}return b.toString();}
 public static String resolveOption(Context c,String m){JSONArray a=items(c);String x=m==null?"":m.trim();for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.optBoolean("enabled",true)&&x.equalsIgnoreCase(o.optString("key").trim()))return o.optString("reply");}catch(Exception ignored){}return null;}
}''',encoding='utf-8')
(java/'GroupMenuActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;import android.content.*;import org.json.*;
public class GroupMenuActivity extends Activity{
 LinearLayout box; EditText trigger,title; JSONArray items;
 EditText e(String hint){EditText x=new EditText(this);x.setHint(hint);x.setTextColor(Color.WHITE);x.setHintTextColor(Color.GRAY);x.setBackgroundColor(Color.rgb(5,18,25));x.setPadding(18,14,18,14);return x;}
 TextView t(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.WHITE);v.setPadding(8,12,8,12);return v;}
 public void onCreate(Bundle b){super.onCreate(b);load();}
 void load(){SharedPreferences p=getSharedPreferences("master_prefs",0);try{items=new JSONArray(p.getString("group_menu_items","[]"));}catch(Exception e){items=new JSONArray();}ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,7,12));box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(28,32,28,40);sc.addView(box);box.addView(t("MENU DO GRUPO",24));box.addView(t("Gatilho que publica o menu no grupo",13));trigger=e("@menu");trigger.setText(p.getString("group_menu_trigger","@menu"));box.addView(trigger);title=e("Título do menu");title.setText(p.getString("group_menu_title","📋 *MENU*"));box.addView(title);box.addView(t("OPÇÕES ILIMITADAS",17));for(int i=0;i<items.length();i++)addCard(items.optJSONObject(i),i);Button add=new Button(this);add.setText("+ ADICIONAR OPÇÃO");add.setOnClickListener(v->{JSONObject o=new JSONObject();try{o.put("key",String.valueOf(items.length()+1));o.put("label","");o.put("reply","");o.put("enabled",true);items.put(o);}catch(Exception ignored){}load();});box.addView(add);Button save=new Button(this);save.setText("SALVAR MENU");save.setOnClickListener(v->save());box.addView(save);setContentView(sc);}
 void addCard(JSONObject o,int idx){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,12,12,18);EditText key=e("Opção: 1, 2, A...");key.setText(o.optString("key"));EditText label=e("Nome exibido: Planos");label.setText(o.optString("label"));EditText reply=e("Resposta desta opção");reply.setText(o.optString("reply"));CheckBox en=new CheckBox(this);en.setText("ATIVA");en.setTextColor(Color.WHITE);en.setChecked(o.optBoolean("enabled",true));Button del=new Button(this);del.setText("REMOVER");del.setOnClickListener(v->{JSONArray n=new JSONArray();for(int j=0;j<items.length();j++)if(j!=idx)n.put(items.optJSONObject(j));items=n;load();});c.addView(key);c.addView(label);c.addView(reply);c.addView(en);c.addView(del);box.addView(c);key.setTag(new Object[]{o,"key"});label.setTag(new Object[]{o,"label"});reply.setTag(new Object[]{o,"reply"});en.setTag(o);key.setOnFocusChangeListener((v,h)->{if(!h)put(o,"key",key.getText().toString());});label.setOnFocusChangeListener((v,h)->{if(!h)put(o,"label",label.getText().toString());});reply.setOnFocusChangeListener((v,h)->{if(!h)put(o,"reply",reply.getText().toString());});en.setOnCheckedChangeListener((b,x)->put(o,"enabled",x));}
 void put(JSONObject o,String k,Object v){try{o.put(k,v);}catch(Exception ignored){}}
 void save(){for(int i=0;i<box.getChildCount();i++){View v=box.getChildAt(i);v.clearFocus();}getSharedPreferences("master_prefs",0).edit().putString("group_menu_trigger",trigger.getText().toString().trim()).putString("group_menu_title",title.getText().toString()).putString("group_menu_items",items.toString()).apply();Toast.makeText(this,"Menu do grupo salvo",Toast.LENGTH_SHORT).show();}
}''',encoding='utf-8')
# add manifest activity
mf=app/'src/main/AndroidManifest.xml'; m=mf.read_text(encoding='utf-8');
if '.GroupMenuActivity' not in m:m=m.replace('</application>','<activity android:name=".GroupMenuActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
mf.write_text(m,encoding='utf-8')
# add access from command settings button, avoiding fragile layout rewrites
p=java/'MessageSettingsActivity.java'; s=p.read_text(encoding='utf-8')
needle='Button save=new Button(this);'
if needle in s and 'GroupMenuActivity.class' not in s:
 s=s.replace(needle,'Button groupMenu=new Button(this); groupMenu.setText("MENU DO GRUPO"); groupMenu.setOnClickListener(v->startActivity(new android.content.Intent(this,GroupMenuActivity.class))); root.addView(groupMenu);\n        '+needle,1)
p.write_text(s,encoding='utf-8')
# version
g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 85',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.14'",g,count=1);gradle.write_text(g,encoding='utf-8')
print('v2.1.14 group menu UI/store ready')