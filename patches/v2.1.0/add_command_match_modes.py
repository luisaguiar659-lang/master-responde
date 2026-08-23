from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
engine=java/'CommandEngine.java'
settings=java/'MessageSettingsActivity.java'
gradle=app/'build.gradle'

e=engine.read_text(encoding='utf-8')
old='''            String normalized = trigger.trim().toLowerCase(Locale.ROOT);\n            if (!normalized.isEmpty() && normalized.equals(incoming) && !reply.trim().isEmpty()) {\n                p.edit().putString("last_matched_command", trigger.trim()).apply();\n                return reply;\n            }\n'''
new='''            String normalized = trigger.trim().toLowerCase(Locale.ROOT);\n            String mode = p.getString("cmd_match_" + i, "EXATO");\n            boolean matched = false;\n            if (!normalized.isEmpty()) {\n                if ("CONTEM".equals(mode)) matched = incoming.contains(normalized);\n                else if ("COMECA".equals(mode)) matched = incoming.startsWith(normalized);\n                else matched = normalized.equals(incoming);\n            }\n            if (matched && !reply.trim().isEmpty()) {\n                p.edit().putString("last_matched_command", trigger.trim()).putString("last_match_mode", mode).apply();\n                return reply;\n            }\n'''
if old not in e: raise SystemExit('ERRO v2.1.11: comparação exata do CommandEngine não encontrada')
e=e.replace(old,new,1)
engine.write_text(e,encoding='utf-8')

s=settings.read_text(encoding='utf-8')
# imports
if 'import android.widget.Spinner;' not in s:
    s=s.replace('import android.widget.Switch;','import android.widget.Switch;\nimport android.widget.Spinner;\nimport android.widget.ArrayAdapter;')
# field
s=s.replace('private final Switch[] enabled=new Switch[4];','private final Switch[] enabled=new Switch[4];\n    private final Spinner[] matchMode=new Spinner[4];')
# insert spinner in each command card after trigger input
old_card='''        triggers[i]=input("Gatilho, ex.: @preco",prefs.getString("cmd_trigger_"+i,DT[i]));replies[i]=input("Resposta automática",prefs.getString("cmd_reply_"+i,DR[i]));c.addView(triggers[i]);space(c,8);c.addView(replies[i],new LinearLayout.LayoutParams(-1,dp(92)));space(c,12);return c;}'''
new_card='''        triggers[i]=input("Gatilho, ex.: @preco",prefs.getString("cmd_trigger_"+i,DT[i]));replies[i]=input("Resposta automática",prefs.getString("cmd_reply_"+i,DR[i]));c.addView(triggers[i]);space(c,8);TextView modeLabel=text("MODO DO GATILHO",11,MUTED,true);c.addView(modeLabel);Spinner sp=new Spinner(this);matchMode[i]=sp;String[] labels={"EXATO","CONTÉM","COMEÇA COM"};ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);sp.setAdapter(ad);String saved=prefs.getString("cmd_match_"+i,"EXATO");sp.setSelection("CONTEM".equals(saved)?1:("COMECA".equals(saved)?2:0));c.addView(sp);space(c,8);c.addView(replies[i],new LinearLayout.LayoutParams(-1,dp(92)));space(c,12);return c;}'''
if old_card not in s: raise SystemExit('ERRO v2.1.11: card de comando não encontrado')
s=s.replace(old_card,new_card,1)
# save match mode alongside other settings
old_save='''ed.putString("cmd_trigger_"+i,t).putString("cmd_reply_"+i,r).putBoolean("cmd_enabled_"+i,enabled[i].isChecked());}ed.apply();'''
new_save='''String mode=matchMode[i].getSelectedItemPosition()==1?"CONTEM":(matchMode[i].getSelectedItemPosition()==2?"COMECA":"EXATO");ed.putString("cmd_trigger_"+i,t).putString("cmd_reply_"+i,r).putBoolean("cmd_enabled_"+i,enabled[i].isChecked()).putString("cmd_match_"+i,mode);}ed.apply();'''
if old_save not in s: raise SystemExit('ERRO v2.1.11: salvamento dos comandos não encontrado')
s=s.replace(old_save,new_save,1)
settings.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 82',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.11'",g,count=1)
gradle.write_text(g,encoding='utf-8')

for pth,mark in [(engine,'cmd_match_'),(engine,'incoming.contains(normalized)'),(engine,'incoming.startsWith(normalized)'),(settings,'MODO DO GATILHO'),(settings,'matchMode')]:
    if mark not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.11: ausente '+mark)
print('v2.1.11: modos EXATO, CONTÉM e COMEÇA COM adicionados; fallback, métricas e motor preservados')