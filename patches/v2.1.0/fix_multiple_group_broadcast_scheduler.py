from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; store=java/'ScheduledGroupBroadcast.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

# Corrige regressão da v2.1.28: quando nextAt era criado dentro de due(),
# o valor podia ficar apenas no JSONObject em memória e não ser persistido.
# No tick seguinte ele era recalculado a partir do novo "agora", empurrando o envio para frente.
s=store.read_text(encoding='utf-8')
old=''' public static boolean due(JSONObject o,long now){return o.optBoolean("enabled",false)&&!o.optString("group","").trim().isEmpty()&&!o.optString("message","").trim().isEmpty()&&now>=nextAt(o,now);}'''
new=''' public static boolean ensureNext(JSONObject o,long now){long before=o.optLong("nextAt",0L);if(before>0L)return false;nextAt(o,now);return true;}\n public static boolean due(JSONObject o,long now){return o.optBoolean("enabled",false)&&!o.optString("group","").trim().isEmpty()&&!o.optString("message","").trim().isEmpty()&&now>=nextAt(o,now);}'''
if old not in s: raise SystemExit('ERRO v2.1.29: due() não encontrado')
s=s.replace(old,new,1);store.write_text(s,encoding='utf-8')

# Mantém o mesmo método de envio da v2.1.27; altera apenas a gestão dos horários.
x=svc.read_text(encoding='utf-8')
old2='''            for(int i=0;i<all.length();i++){\n                org.json.JSONObject job=all.optJSONObject(i);\n                if(job==null||!ScheduledGroupBroadcast.due(job,now))continue;'''
new2='''            for(int i=0;i<all.length();i++){\n                org.json.JSONObject job=all.optJSONObject(i);\n                if(job==null)continue;\n                if(ScheduledGroupBroadcast.ensureNext(job,now))changed=true;\n                if(!ScheduledGroupBroadcast.due(job,now))continue;'''
if old2 not in x: raise SystemExit('ERRO v2.1.29: loop multi-grupo não encontrado')
x=x.replace(old2,new2,1)
svc.write_text(x,encoding='utf-8')

D=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.28",10,MUTED,false));','brand.addView(text("v2.1.29",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 100',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.29'",G,count=1);gradle.write_text(G,encoding='utf-8')
for p,m in [(store,'ensureNext'),(svc,'ScheduledGroupBroadcast.ensureNext(job,now)'),(svc,'sendScheduledGroupReply(candidate,wanted,text)'),(gradle,"versionName '2.1.29'")]:
 if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.29: requisito ausente '+m)
print('v2.1.29: persistência do próximo envio corrigida; motor de envio anterior preservado')