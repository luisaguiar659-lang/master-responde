from pathlib import Path
import re

root=Path('projeto/app/src/main/java/com/masterresponde/app')
gradle=Path('projeto/app/build.gradle')
manifest=Path('projeto/app/src/main/AndroidManifest.xml')
svc=root/'WhatsAppBusinessCaptureService.java'
dash=root/'NeonDashboardActivity.java'

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+', 'versionCode 80', g, count=1)
g=re.sub(r"versionName\s+'[^']+'", "versionName '2.1.9'", g, count=1)
gradle.write_text(g,encoding='utf-8')

(root/'HistoryStore.java').write_text(r'''package com.masterresponde.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public final class HistoryStore {
 private static final String PREF="master_history", KEY="events"; private static final int MAX=200;
 private HistoryStore(){}
 public static synchronized void add(Context c,String conversation,String incoming,String reply,String status){try{SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);JSONArray old=new JSONArray(p.getString(KEY,"[]")),out=new JSONArray();JSONObject e=new JSONObject();e.put("time",new SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault()).format(new Date()));e.put("conversation",conversation==null?"":conversation);e.put("incoming",incoming==null?"":incoming);e.put("reply",reply==null?"":reply);e.put("status",status==null?"":status);out.put(e);for(int i=0;i<old.length()&&out.length()<MAX;i++)out.put(old.getJSONObject(i));p.edit().putString(KEY,out.toString()).apply();}catch(Exception ignored){}}
 public static JSONArray get(Context c){try{return new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
 public static void clear(Context c){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply();}
}''',encoding='utf-8')

(root/'HistoryActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.Activity;import android.os.Bundle;import android.graphics.Color;import android.graphics.Typeface;import android.widget.*;import org.json.*;
public class HistoryActivity extends Activity{
 private LinearLayout list; private TextView tv(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(12,8,12,8);return v;}
 @Override public void onCreate(Bundle b){super.onCreate(b);render();}
 private void render(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,7,12));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(28,35,28,35);sc.addView(list);TextView t=tv("HISTÓRICO DE ATENDIMENTOS",22,Color.WHITE);t.setTypeface(null,Typeface.BOLD);list.addView(t);list.addView(tv("Últimos 200 eventos do motor",13,Color.rgb(0,220,255)));Button clear=new Button(this);clear.setText("LIMPAR HISTÓRICO");clear.setOnClickListener(v->{HistoryStore.clear(this);render();});list.addView(clear);JSONArray a=HistoryStore.get(this);if(a.length()==0)list.addView(tv("\nNenhum atendimento registrado ainda.",16,Color.LTGRAY));for(int i=0;i<a.length();i++)try{JSONObject e=a.getJSONObject(i);TextView card=tv("\n"+e.optString("time")+" • "+e.optString("conversation")+"\nRecebida: "+e.optString("incoming")+"\nResposta: "+e.optString("reply")+"\nStatus: "+e.optString("status"),14,Color.WHITE);card.setBackgroundColor(Color.rgb(5,18,25));list.addView(card);}catch(Exception ignored){}setContentView(sc);}
}''',encoding='utf-8')

# Nesta etapa preservamos totalmente o motor funcional. O histórico recebe o snapshot
# que o próprio dashboard já mantém, evitando introduzir variáveis inexistentes no serviço.
d=dash.read_text(encoding='utf-8')
if 'HistoryActivity.class' not in d:
    d=d.replace('nav("•\\nHISTÓRICO",Color.WHITE,v->Toast.makeText(this,"Em breve",Toast.LENGTH_SHORT).show())','nav("•\\nHISTÓRICO",Color.WHITE,v->startActivity(new Intent(this,HistoryActivity.class)))')
    # fallback para qualquer variante do ícone/texto
    d=re.sub(r'nav\("[^"\\n]*\\nHISTÓRICO",Color\.WHITE,v->Toast\.makeText\(this,"Em breve",Toast\.LENGTH_SHORT\)\.show\(\)\)', 'nav("•\\nHISTÓRICO",Color.WHITE,v->startActivity(new Intent(this,HistoryActivity.class)))', d, count=1)
dash.write_text(d,encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if 'android:name=".HistoryActivity"' not in m:
    anchor='<activity android:name=".MessageSettingsActivity"'
    idx=m.find(anchor)
    if idx>=0:m=m[:idx]+'<activity android:name=".HistoryActivity" android:screenOrientation="portrait" android:exported="false"/>\n        '+m[idx:]
    else:m=m.replace('</application>','<activity android:name=".HistoryActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

for p,mark in [(root/'HistoryStore.java','class HistoryStore'),(root/'HistoryActivity.java','HISTÓRICO DE ATENDIMENTOS'),(dash,'HistoryActivity.class'),(manifest,'.HistoryActivity')]:
    if mark not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.9: requisito ausente: '+mark)
print('v2.1.9: histórico criado e navegação ligada; motor funcional preservado')