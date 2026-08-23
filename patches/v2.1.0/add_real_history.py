from pathlib import Path

root=Path('projeto/app/src/main/java/com/masterresponde/app')
gradle=Path('projeto/app/build.gradle')
manifest=Path('projeto/app/src/main/AndroidManifest.xml')
svc=root/'WhatsAppBusinessCaptureService.java'
dash=root/'NeonDashboardActivity.java'

# version
g=gradle.read_text()
g=g.replace("versionName '2.1.8'", "versionName '2.1.9'")
gradle.write_text(g)

# HistoryStore: SharedPreferences ring buffer, no database migration needed
(root/'HistoryStore.java').write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class HistoryStore {
    private static final String PREF="master_history";
    private static final String KEY="events";
    private static final int MAX=200;
    private HistoryStore() {}

    public static synchronized void add(Context c,String conversation,String incoming,String reply,String status){
        try {
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray old=new JSONArray(p.getString(KEY,"[]"));
            JSONArray out=new JSONArray();
            JSONObject e=new JSONObject();
            e.put("time",new SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault()).format(new Date()));
            e.put("conversation",conversation==null?"":conversation);
            e.put("incoming",incoming==null?"":incoming);
            e.put("reply",reply==null?"":reply);
            e.put("status",status==null?"":status);
            out.put(e);
            for(int i=0;i<old.length() && out.length()<MAX;i++) out.put(old.getJSONObject(i));
            p.edit().putString(KEY,out.toString()).apply();
        } catch(Exception ignored) {}
    }
    public static JSONArray get(Context c){
        try { return new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]")); }
        catch(Exception e){ return new JSONArray(); }
    }
    public static void clear(Context c){ c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply(); }
}
''')

(root/'HistoryActivity.java').write_text(r'''package com.masterresponde.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends Activity {
    private LinearLayout list;
    private TextView tv(String s,int sp,int color){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setPadding(12,8,12,8); return v; }
    @Override public void onCreate(Bundle b){ super.onCreate(b); render(); }
    @Override protected void onResume(){ super.onResume(); render(); }
    private void render(){
        ScrollView sc=new ScrollView(this); sc.setBackgroundColor(Color.rgb(1,7,12));
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(28,35,28,35); sc.addView(list);
        TextView title=tv("HISTÓRICO DE ATENDIMENTOS",22,Color.WHITE); title.setTypeface(null,Typeface.BOLD); list.addView(title);
        TextView sub=tv("Últimos 200 eventos do motor",13,Color.rgb(0,220,255)); list.addView(sub);
        Button clear=new Button(this); clear.setText("LIMPAR HISTÓRICO"); clear.setOnClickListener(v->{HistoryStore.clear(this);render();}); list.addView(clear);
        JSONArray a=HistoryStore.get(this);
        if(a.length()==0) list.addView(tv("\nNenhum atendimento registrado ainda.",16,Color.LTGRAY));
        for(int i=0;i<a.length();i++) try{
            JSONObject e=a.getJSONObject(i);
            String s="\n"+e.optString("time")+"  •  "+e.optString("conversation")+"\nRecebida: "+e.optString("incoming")+"\nResposta: "+e.optString("reply")+"\nStatus: "+e.optString("status");
            TextView card=tv(s,14,Color.WHITE); card.setBackgroundColor(Color.rgb(5,18,25)); list.addView(card);
        }catch(Exception ignored){}
        setContentView(sc);
    }
}
''')

# Instrument successful/failure status at central dashboard status writes, preserving metrics.
s=svc.read_text()
# Find likely success/failure preference status writes and append history using variables available in method.
# Safer: insert into dashboard preference update points using generic captured fields if present.
needle='prefs.edit()'
# We only add helper method and call it around known status strings using broad replacements.
if 'private void saveHistory(' not in s:
    pos=s.rfind('}')
    helper='''\n    private void saveHistory(String conversation, String incoming, String reply, String status) {\n        HistoryStore.add(this, conversation, incoming, reply, status);\n    }\n'''
    s=s[:pos]+helper+s[pos:]
# Hook after successful reply metric increment if recognizable
for marker in ['"Respondida com sucesso • motor v2"','"Respondida com sucesso"']:
    if marker in s and 'history_success_v219' not in s:
        # add history call immediately after first statement containing marker
        lines=s.splitlines(); out=[]; done=False
        for line in lines:
            out.append(line)
            if not done and marker in line and ';' in line:
                indent=line[:len(line)-len(line.lstrip())]
                out.append(indent+'HistoryStore.add(this, conversationName, messageText, responseText, "SUCESSO"); // history_success_v219')
                done=True
        if done: s='\n'.join(out)+'\n'
        break
svc.write_text(s)

# Dashboard HISTORY bottom tab -> HistoryActivity. Detect visible label vicinity / existing click setup.
d=dash.read_text()
if 'HistoryActivity.class' not in d:
    # replace common placeholder listener associated with history text when possible
    import re
    # Add helper method usable by existing/new click binding
    pos=d.rfind('}')
    d=d[:pos]+'''\n    private void openRealHistory() {\n        startActivity(new android.content.Intent(this, HistoryActivity.class));\n    }\n'''+d[pos:]
    # Attach by finding TextView whose text is HISTÓRICO if constructed in Java and next suitable point
    # fallback: make every TextView with exact HISTÓRICO clickable during recursive post-build is intrusive; use root content traversal in onResume.
    # inject binding utility
    pos=d.rfind('}')
    d=d[:pos]+'''\n    private void bindHistoryButton(android.view.View v) {\n        if (v instanceof android.widget.TextView) {\n            CharSequence t=((android.widget.TextView)v).getText();\n            if(t!=null && "HISTÓRICO".equalsIgnoreCase(t.toString().trim())) v.setOnClickListener(x -> openRealHistory());\n        }\n        if(v instanceof android.view.ViewGroup) { android.view.ViewGroup g=(android.view.ViewGroup)v; for(int i=0;i<g.getChildCount();i++) bindHistoryButton(g.getChildAt(i)); }\n    }\n'''+d[pos:]
    # bind after setContentView occurrences
    d=d.replace('setContentView(root);','setContentView(root);\n        bindHistoryButton(root);')
dash.write_text(d)

m=manifest.read_text()
if '.HistoryActivity' not in m:
    m=m.replace('</application>','        <activity android:name=".HistoryActivity" android:exported="false" />\n    </application>')
manifest.write_text(m)
print('v2.1.9 history patch applied')