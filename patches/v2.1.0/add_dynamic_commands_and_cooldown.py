from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
engine=java/'CommandEngine.java'
settings=java/'MessageSettingsActivity.java'
service=java/'WhatsAppBusinessCaptureService.java'
gradle=app/'build.gradle'

engine.write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public final class CommandEngine {
    public static final String PREFS="master_responde";
    public static final String KEY_COMMANDS="commands_json";
    private static final String[] DT={"@info","@status","@ajuda",""};
    private static final String[] DR={"MASTER RESPONDE: sistema online e funcionando.","MASTER RESPONDE: ONLINE • WhatsApp Business protegido • captura ativa.","Comandos disponíveis: @info, @status, @ajuda",""};
    private CommandEngine(){}

    public static void ensureMigration(Context c){
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(p.contains(KEY_COMMANDS)) return;
        JSONArray a=new JSONArray();
        try{
            for(int i=0;i<4;i++){
                String t=p.getString("cmd_trigger_"+i,DT[i]);
                String r=p.getString("cmd_reply_"+i,DR[i]);
                boolean en=p.getBoolean("cmd_enabled_"+i,!DT[i].isEmpty());
                String mode=p.getString("cmd_match_"+i,"EXATO");
                if((t==null||t.trim().isEmpty()) && (r==null||r.trim().isEmpty())) continue;
                JSONObject o=new JSONObject();o.put("trigger",t==null?"":t);o.put("reply",r==null?"":r);o.put("enabled",en);o.put("mode",mode);a.put(o);
            }
        }catch(Exception ignored){}
        p.edit().putString(KEY_COMMANDS,a.toString()).apply();
    }

    public static JSONArray load(Context c){ensureMigration(c);try{return new JSONArray(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_COMMANDS,"[]"));}catch(Exception e){return new JSONArray();}}
    public static void save(Context c,JSONArray a){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_COMMANDS,a==null?"[]":a.toString()).apply();}

    public static String resolve(Context context,String message){
        if(message==null) return null;
        String incoming=message.trim().toLowerCase(Locale.ROOT);if(incoming.isEmpty()) return null;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        JSONArray a=load(context);
        for(int i=0;i<a.length();i++) try{
            JSONObject o=a.getJSONObject(i);if(!o.optBoolean("enabled",true))continue;
            String trigger=o.optString("trigger","").trim();String reply=o.optString("reply","").trim();String mode=o.optString("mode","EXATO");
            if(trigger.isEmpty()||reply.isEmpty())continue;
            String n=trigger.toLowerCase(Locale.ROOT);boolean matched;
            if("CONTEM".equals(mode))matched=incoming.contains(n);else if("COMECA".equals(mode))matched=incoming.startsWith(n);else matched=incoming.equals(n);
            if(matched){p.edit().putString("last_matched_command",trigger).putString("last_match_mode",mode).apply();return reply;}
        }catch(Exception ignored){}
        boolean fallbackEnabled=p.getBoolean("fallback_enabled",false);String fallback=p.getString("fallback_reply","Olá! Recebemos sua mensagem. Em breve retornaremos.");
        if(fallbackEnabled&&fallback!=null&&!fallback.trim().isEmpty()){p.edit().putString("last_matched_command","RESPOSTA_PADRAO").apply();return fallback.trim();}
        return null;
    }
}
''',encoding='utf-8')

(java/'ConversationCooldown.java').write_text(r'''package com.masterresponde.app;
import android.content.Context;import android.content.SharedPreferences;import java.util.Locale;
public final class ConversationCooldown{
 private static final String PREF="master_responde";private ConversationCooldown(){}
 private static String key(String c){String n=c==null?"":c.trim().toLowerCase(Locale.ROOT);return "cooldown_last_"+Integer.toHexString(n.hashCode());}
 public static long remainingSeconds(Context c,String conversation){SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);long sec=p.getLong("cooldown_seconds",60L);if(sec<=0)return 0;long last=p.getLong(key(conversation),0L);long left=sec*1000L-(System.currentTimeMillis()-last);return left>0?(left+999L)/1000L:0;}
 public static boolean shouldBlock(Context c,String conversation){return remainingSeconds(c,conversation)>0;}
 public static void markSent(Context c,String conversation){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putLong(key(conversation),System.currentTimeMillis()).apply();}
}
''',encoding='utf-8')

settings.write_text(r'''package com.masterresponde.app;
import android.app.Activity;import android.os.Bundle;import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
public class MessageSettingsActivity extends Activity{
 private static final int BG=Color.rgb(3,7,12),CARD=Color.rgb(8,15,23),GREEN=Color.rgb(0,255,102),CYAN=Color.rgb(0,214,255),MUTED=Color.rgb(147,164,178);private SharedPreferences prefs;private LinearLayout commandList;private EditText cooldown;private final ArrayList<Row> rows=new ArrayList<>();
 static class Row{LinearLayout card;EditText trigger,reply;Switch enabled;Spinner mode;}
 @Override protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(CommandEngine.PREFS,MODE_PRIVATE);CommandEngine.ensureMigration(this);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);setContentView(build());}
 private ScrollView build(){ScrollView s=new ScrollView(this);s.setBackgroundColor(BG);LinearLayout root=col();root.setPadding(dp(16),dp(18),dp(16),dp(32));s.addView(root,new ViewGroup.LayoutParams(-1,-2));LinearLayout h=row();TextView back=text("‹",34,CYAN,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());h.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));h.addView(text("COMANDOS",24,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1f));root.addView(h);root.addView(text("Adicione quantos comandos quiser",12,MUTED,false));space(root,12);
 LinearLayout timer=col();timer.setPadding(dp(14),dp(12),dp(14),dp(12));timer.setBackground(bg(CARD,GREEN,14,1));timer.addView(text("TEMPORIZADOR POR PESSOA",12,Color.WHITE,true));timer.addView(text("Depois de responder uma pessoa, aguarda este tempo antes de responder a mesma pessoa novamente. Outras pessoas continuam sendo respondidas normalmente.",11,MUTED,false));cooldown=input("Segundos (0 desativa)",String.valueOf(prefs.getLong("cooldown_seconds",60L)));cooldown.setInputType(InputType.TYPE_CLASS_NUMBER);timer.addView(cooldown);root.addView(timer);space(root,12);
 Button add=new Button(this);add.setText("+ ADICIONAR COMANDO");add.setOnClickListener(v->addRow(null));root.addView(add,new LinearLayout.LayoutParams(-1,dp(50)));space(root,12);commandList=col();root.addView(commandList);loadRows();Button save=new Button(this);save.setText("SALVAR TUDO");save.setOnClickListener(v->save());root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));space(root,10);Button fallback=new Button(this);fallback.setText("RESPOSTA PADRÃO");fallback.setOnClickListener(v->startActivity(new Intent(this,FallbackSettingsActivity.class)));root.addView(fallback,new LinearLayout.LayoutParams(-1,dp(52)));return s;}
 private void loadRows(){JSONArray a=CommandEngine.load(this);for(int i=0;i<a.length();i++)try{addRow(a.getJSONObject(i));}catch(Exception ignored){}if(rows.isEmpty())addRow(null);}
 private void addRow(JSONObject o){Row r=new Row();LinearLayout c=col();r.card=c;c.setPadding(dp(14),dp(12),dp(14),dp(14));c.setBackground(bg(CARD,CYAN,14,1));LinearLayout top=row();top.addView(text("COMANDO "+(rows.size()+1),12,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1f));r.enabled=new Switch(this);r.enabled.setChecked(o==null||o.optBoolean("enabled",true));top.addView(r.enabled);Button del=new Button(this);del.setText("REMOVER");del.setOnClickListener(v->{commandList.removeView(c);rows.remove(r);renumber();});top.addView(del);c.addView(top);r.trigger=input("Gatilho, ex.: preço",o==null?"":o.optString("trigger",""));c.addView(r.trigger);space(c,6);r.mode=new Spinner(this);String[] labels={"EXATO","CONTÉM","COMEÇA COM"};r.mode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));String m=o==null?"EXATO":o.optString("mode","EXATO");r.mode.setSelection("CONTEM".equals(m)?1:("COMECA".equals(m)?2:0));c.addView(r.mode);space(c,6);r.reply=input("Resposta automática",o==null?"":o.optString("reply",""));r.reply.setMinLines(3);c.addView(r.reply);space(c,10);rows.add(r);commandList.addView(c);}
 private void renumber(){for(int i=0;i<rows.size();i++){LinearLayout top=(LinearLayout)rows.get(i).card.getChildAt(0);((TextView)top.getChildAt(0)).setText("COMANDO "+(i+1));}}
 private void save(){long sec=60;try{sec=Long.parseLong(cooldown.getText().toString().trim());}catch(Exception e){Toast.makeText(this,"Temporizador inválido",Toast.LENGTH_SHORT).show();return;}if(sec<0)sec=0;JSONArray a=new JSONArray();try{for(Row r:rows){String t=r.trigger.getText().toString().trim(),rp=r.reply.getText().toString().trim();if(r.enabled.isChecked()&&(t.isEmpty()||rp.isEmpty())){Toast.makeText(this,"Preencha gatilho e resposta dos comandos ativos",Toast.LENGTH_SHORT).show();return;}if(t.isEmpty()&&rp.isEmpty())continue;JSONObject o=new JSONObject();o.put("trigger",t);o.put("reply",rp);o.put("enabled",r.enabled.isChecked());o.put("mode",r.mode.getSelectedItemPosition()==1?"CONTEM":(r.mode.getSelectedItemPosition()==2?"COMECA":"EXATO"));a.put(o);}}catch(Exception ignored){}CommandEngine.save(this,a);prefs.edit().putLong("cooldown_seconds",sec).apply();Toast.makeText(this,"Comandos e temporizador salvos",Toast.LENGTH_SHORT).show();}
 private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}private TextView text(String s,int sp,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);return t;}private EditText input(String h,String v){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(MUTED);e.setText(v);e.setTextColor(Color.WHITE);e.setTextSize(13);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(bg(Color.rgb(3,10,16),Color.rgb(27,78,96),10,1));return e;}private GradientDrawable bg(int f,int s,int r,int w){GradientDrawable d=new GradientDrawable();d.setColor(f);d.setCornerRadius(dp(r));d.setStroke(dp(w),s);return d;}private void space(LinearLayout l,int h){TextView v=new TextView(this);v.setHeight(dp(h));l.addView(v);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
''',encoding='utf-8')

s=service.read_text(encoding='utf-8')
old='''        String resolvedReply = CommandEngine.resolve(this, message);\n        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {\n            sendDirectReply(n, conversation, resolvedReply);\n        } else {\n            prefs().edit()\n                    .putString("accessibility_last_status", "Mensagem capturada • sem comando correspondente")\n                    .apply();\n        }\n'''
new='''        String resolvedReply = CommandEngine.resolve(this, message);\n        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {\n            long wait = ConversationCooldown.remainingSeconds(this, conversation);\n            if (wait > 0) {\n                prefs().edit().putString("accessibility_last_status", "Temporizador ativo para esta pessoa • aguarde " + wait + "s").apply();\n                return;\n            }\n            sendDirectReply(n, conversation, resolvedReply);\n        } else {\n            prefs().edit()\n                    .putString("accessibility_last_status", "Mensagem capturada • sem comando correspondente")\n                    .apply();\n        }\n'''
if old not in s: raise SystemExit('ERRO v2.1.12: bloco CommandEngine.resolve não encontrado')
s=s.replace(old,new,1)
needle='action.actionIntent.send(this, 0, fillIn);'
if needle not in s: raise SystemExit('ERRO v2.1.12: envio RemoteInput não encontrado')
s=s.replace(needle,needle+'\n                ConversationCooldown.markSent(this, conversation);',1)
service.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 83',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.12'",g,count=1);gradle.write_text(g,encoding='utf-8')
for p,m in [(engine,'commands_json'),(settings,'ADICIONAR COMANDO'),(settings,'TEMPORIZADOR POR PESSOA'),(java/'ConversationCooldown.java','remainingSeconds'),(service,'ConversationCooldown.markSent')]:
    if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.12: requisito ausente: '+m)
print('v2.1.12: comandos ilimitados + modos por comando + cooldown independente por pessoa')