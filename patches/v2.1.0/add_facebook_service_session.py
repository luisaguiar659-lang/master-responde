from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
engine=java/'CommandEngine.java'
settings=java/'FallbackSettingsActivity.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# Sessão individual: o gatilho de entrada abre o autoatendimento para aquela conversa.
(java/'AutoServiceSession.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.util.Locale;
public final class AutoServiceSession{
 private static final String PREF="master_responde";private AutoServiceSession(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 private static String key(String conversation){String n=conversation==null?"":conversation.trim().toLowerCase(Locale.ROOT);return "auto_service_session_"+Integer.toHexString(n.hashCode());}
 public static int minutes(Context c){int m=p(c).getInt("auto_service_session_minutes",60);return Math.max(1,m);}
 public static void open(Context c,String conversation){p(c).edit().putLong(key(conversation),System.currentTimeMillis()).apply();}
 public static boolean active(Context c,String conversation){long at=p(c).getLong(key(conversation),0L);if(at<=0)return false;long ttl=minutes(c)*60000L;if(System.currentTimeMillis()-at>ttl){p(c).edit().remove(key(conversation)).apply();return false;}return true;}
 public static void touch(Context c,String conversation){if(active(c,conversation))open(c,conversation);}
}''',encoding='utf-8')

# CommandEngine passa a receber a conversa. Comando reconhecido abre/renova a sessão;
# fallback só existe dentro de sessão ativa.
e=engine.read_text(encoding='utf-8')
e=e.replace('public static String resolve(Context context,String message){','public static String resolve(Context context,String conversation,String message){',1)
old='''if(matched){p.edit().putString("last_matched_command",trigger).putString("last_match_mode",mode).apply();return reply;}'''
new='''if(matched){p.edit().putString("last_matched_command",trigger).putString("last_match_mode",mode).apply();AutoServiceSession.open(context,conversation);return reply;}'''
if old not in e: raise SystemExit('ERRO v2.1.35: retorno de comando não encontrado')
e=e.replace(old,new,1)
oldfb='''if(fallbackEnabled&&fallback!=null&&!fallback.trim().isEmpty()){p.edit().putString("last_matched_command","RESPOSTA_PADRAO").apply();return fallback.trim();}'''
newfb='''if(fallbackEnabled&&AutoServiceSession.active(context,conversation)&&fallback!=null&&!fallback.trim().isEmpty()){AutoServiceSession.touch(context,conversation);p.edit().putString("last_matched_command","RESPOSTA_PADRAO").apply();return fallback.trim();}'''
if oldfb not in e: raise SystemExit('ERRO v2.1.35: fallback não encontrado')
e=e.replace(oldfb,newfb,1)
engine.write_text(e,encoding='utf-8')

# Serviço envia a identificação da conversa ao motor.
svc=java/'WhatsAppBusinessCaptureService.java'
s=svc.read_text(encoding='utf-8')
oldcall='CommandEngine.resolve(this, message)'
if oldcall not in s: raise SystemExit('ERRO v2.1.35: chamada CommandEngine não encontrada')
s=s.replace(oldcall,'CommandEngine.resolve(this, conversation, message)')
svc.write_text(s,encoding='utf-8')

# Tela da Resposta Padrão deixa claro o escopo e permite configurar duração da sessão.
f=settings.read_text(encoding='utf-8')
f=f.replace('private SharedPreferences p; private Switch enabled; private EditText reply;','private SharedPreferences p; private Switch enabled; private EditText reply,sessionMinutes;',1)
f=f.replace('sub.setText("Usada quando nenhuma regra/comando corresponder")','sub.setText("Usada somente durante uma sessão de autoatendimento iniciada por um comando reconhecido")',1)
needle='''reply.setMinLines(4);r.addView(reply,new LinearLayout.LayoutParams(-1,-2));Button save=new Button(this);'''
insert='''reply.setMinLines(4);r.addView(reply,new LinearLayout.LayoutParams(-1,-2));TextView st=new TextView(this);st.setText("DURAÇÃO DA SESSÃO (MINUTOS)");st.setTextColor(Color.WHITE);st.setTypeface(null,Typeface.BOLD);st.setPadding(0,18,0,6);r.addView(st);TextView sh=new TextView(this);sh.setText("Após este tempo sem interação, a resposta padrão deixa de atuar para aquela pessoa. Um novo comando reconhecido inicia outra sessão.");sh.setTextColor(Color.GRAY);r.addView(sh);sessionMinutes=new EditText(this);sessionMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);sessionMinutes.setText(String.valueOf(p.getInt("auto_service_session_minutes",60)));sessionMinutes.setTextColor(Color.WHITE);sessionMinutes.setHintTextColor(Color.GRAY);r.addView(sessionMinutes,new LinearLayout.LayoutParams(-1,-2));Button save=new Button(this);'''
if needle not in f: raise SystemExit('ERRO v2.1.35: campo resposta padrão não encontrado')
f=f.replace(needle,insert,1)
oldsave='''save.setOnClickListener(v->{String x=reply.getText().toString().trim();if(enabled.isChecked()&&x.isEmpty()){Toast.makeText(this,"Digite uma resposta",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("fallback_enabled",enabled.isChecked()).putString("fallback_reply",x).apply();Toast.makeText(this,"Resposta padrão salva",Toast.LENGTH_SHORT).show();});'''
newsave='''save.setOnClickListener(v->{String x=reply.getText().toString().trim();if(enabled.isChecked()&&x.isEmpty()){Toast.makeText(this,"Digite uma resposta",Toast.LENGTH_SHORT).show();return;}int mins=60;try{mins=Integer.parseInt(sessionMinutes.getText().toString().trim());}catch(Exception ignored){}if(mins<1)mins=1;if(mins>10080)mins=10080;p.edit().putBoolean("fallback_enabled",enabled.isChecked()).putString("fallback_reply",x).putInt("auto_service_session_minutes",mins).apply();sessionMinutes.setText(String.valueOf(mins));Toast.makeText(this,"Resposta padrão e sessão salvas",Toast.LENGTH_SHORT).show();});'''
if oldsave not in f: raise SystemExit('ERRO v2.1.35: salvar fallback não encontrado')
f=f.replace(oldsave,newsave,1)
settings.write_text(f,encoding='utf-8')

# Versão.
d=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.34",10,MUTED,false));','brand.addView(text("v2.1.35",10,MUTED,false));')
dash.write_text(d,encoding='utf-8')
g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 106',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.35'",g,count=1);gradle.write_text(g,encoding='utf-8')

for pth,token in [(java/'AutoServiceSession.java','active(Context c,String conversation)'),(engine,'AutoServiceSession.active(context,conversation)'),(svc,'CommandEngine.resolve(this, conversation, message)'),(settings,'DURAÇÃO DA SESSÃO (MINUTOS)'),(gradle,"versionName '2.1.35'")]:
 if token not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.35: requisito ausente '+token)
print('v2.1.35: resposta padrão restrita a sessões individuais de autoatendimento')