from pathlib import Path
import re

app=Path('projeto/app');java=app/'src/main/java/com/masterresponde/app';engine=java/'CommandEngine.java';settings=java/'FallbackSettingsActivity.java';dash=java/'NeonDashboardActivity.java';gradle=app/'build.gradle'

e=engine.read_text(encoding='utf-8')
old='''if(matched){p.edit().putString("last_matched_command",trigger).putString("last_match_mode",mode).apply();AutoServiceSession.open(context,conversation);return reply;}'''
new='''if(matched){
                p.edit().putString("last_matched_command",trigger).putString("last_match_mode",mode).apply();
                String entryTrigger=p.getString("auto_service_entry_trigger","Olá! Posso ter mais informações sobre isso?");
                if(entryTrigger!=null&&!entryTrigger.trim().isEmpty()&&incoming.equals(entryTrigger.trim().toLowerCase(Locale.ROOT))){
                    AutoServiceSession.open(context,conversation);
                }else if(AutoServiceSession.active(context,conversation)){
                    AutoServiceSession.touch(context,conversation);
                }
                return reply;
            }'''
if old not in e:raise SystemExit('ERRO v2.1.36: abertura de sessão v2.1.35 não encontrada')
e=e.replace(old,new,1);engine.write_text(e,encoding='utf-8')

f=settings.read_text(encoding='utf-8')
f=f.replace('private SharedPreferences p; private Switch enabled; private EditText reply,sessionMinutes;','private SharedPreferences p; private Switch enabled; private EditText reply,sessionMinutes,entryTrigger;',1)
f=f.replace('sub.setText("Usada somente durante uma sessão de autoatendimento iniciada por um comando reconhecido")','sub.setText("Usada somente durante uma sessão iniciada pelo gatilho de entrada do anúncio")',1)
needle='''reply.setMinLines(4);r.addView(reply,new LinearLayout.LayoutParams(-1,-2));TextView st=new TextView(this);'''
insert='''reply.setMinLines(4);r.addView(reply,new LinearLayout.LayoutParams(-1,-2));TextView et=new TextView(this);et.setText("GATILHO DE ENTRADA DO ANÚNCIO");et.setTextColor(Color.WHITE);et.setTypeface(null,Typeface.BOLD);et.setPadding(0,18,0,6);r.addView(et);TextView eh=new TextView(this);eh.setText("Somente esta mensagem abre uma nova sessão de autoatendimento. Os comandos 1, 2, 3, 4 e comandos internos não abrem sessão sozinhos.");eh.setTextColor(Color.GRAY);r.addView(eh);entryTrigger=new EditText(this);entryTrigger.setText(p.getString("auto_service_entry_trigger","Olá! Posso ter mais informações sobre isso?"));entryTrigger.setTextColor(Color.WHITE);entryTrigger.setHintTextColor(Color.GRAY);r.addView(entryTrigger,new LinearLayout.LayoutParams(-1,-2));TextView st=new TextView(this);'''
if needle not in f:raise SystemExit('ERRO v2.1.36: posição do gatilho não encontrada')
f=f.replace(needle,insert,1)
old='''.putString("fallback_reply",x).putInt("auto_service_session_minutes",mins).apply();'''
new='''.putString("fallback_reply",x).putString("auto_service_entry_trigger",entryTrigger.getText().toString().trim()).putInt("auto_service_session_minutes",mins).apply();'''
if old not in f:raise SystemExit('ERRO v2.1.36: salvamento da sessão não encontrado')
f=f.replace(old,new,1);settings.write_text(f,encoding='utf-8')

d=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.35",10,MUTED,false));','brand.addView(text("v2.1.36",10,MUTED,false));');dash.write_text(d,encoding='utf-8')
g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 107',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.36'",g,count=1);gradle.write_text(g,encoding='utf-8')
for p,t in [(engine,'auto_service_entry_trigger'),(engine,'incoming.equals(entryTrigger'),(settings,'GATILHO DE ENTRADA DO ANÚNCIO'),(gradle,"versionName '2.1.36'")]:
 if t not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.36: requisito ausente '+t)
print('v2.1.36: somente o gatilho configurável do anúncio abre nova sessão; demais comandos apenas operam/renovam sessão já ativa')