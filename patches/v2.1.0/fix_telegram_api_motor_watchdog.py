from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
tg=java/'TelegramApiAlbumService.java'
act=java/'TelegramApiSettingsActivity.java'
gradle=app/'build.gradle'
dash=java/'NeonDashboardActivity.java'

# v2.1.37
# Corrige o caso em que o botão do Motor Telegram API fica verde por causa da
# preferência persistida, mas o serviço real não está mais executando (muito
# comum depois de atualizar/reinstalar o APK ou após o Android matar o processo).
# Também adiciona heartbeat e auto-recuperação quando a consulta getUpdates
# fica sem vida por tempo demais.

s=tg.read_text(encoding='utf-8')

# marca heartbeat ao iniciar
needle='''        prefs().edit().putBoolean("telegram_api_motor_on",true).putString("telegram_api_status","Motor Telegram API ligado").apply();'''
repl='''        prefs().edit().putBoolean("telegram_api_motor_on",true)
                .putLong("telegram_api_heartbeat",System.currentTimeMillis())
                .putString("telegram_api_status","Motor Telegram API ligado").apply();'''
if needle not in s: raise SystemExit('ERRO v2.1.37: onStartCommand do Telegram API não encontrado')
s=s.replace(needle,repl,1)

# heartbeat em cada ciclo do polling, inclusive quando não há mensagens
needle2='''        while(running){
            try{'''
repl2='''        while(running){
            try{
                prefs().edit().putLong("telegram_api_heartbeat",System.currentTimeMillis()).apply();'''
if needle2 not in s: raise SystemExit('ERRO v2.1.37: loop principal Telegram API não encontrado')
s=s.replace(needle2,repl2,1)

# heartbeat logo após retorno do getUpdates
needle3='''                JSONObject root=getJson(u,30000);'''
repl3='''                JSONObject root=getJson(u,30000);
                prefs().edit().putLong("telegram_api_heartbeat",System.currentTimeMillis()).apply();'''
if needle3 not in s: raise SystemExit('ERRO v2.1.37: getUpdates não encontrado')
s=s.replace(needle3,repl3,1)

# em erro, mantém heartbeat e deixa status explícito
needle4='''            }catch(Throwable e){status("Erro Motor Telegram API: "+safe(e.getMessage()));sleep(2500);}'''
repl4='''            }catch(Throwable e){
                prefs().edit().putLong("telegram_api_heartbeat",System.currentTimeMillis()).apply();
                status("Erro Motor Telegram API: "+safe(e.getMessage()));sleep(2500);
            }'''
if needle4 not in s: raise SystemExit('ERRO v2.1.37: catch principal não encontrado')
s=s.replace(needle4,repl4,1)

tg.write_text(s,encoding='utf-8')

# A tela passa a tratar a preferência como "desejo do usuário", não como prova
# de que o serviço está vivo. Se deveria estar ligado e o heartbeat está velho,
# ela inicia novamente o ForegroundService automaticamente.
a=act.read_text(encoding='utf-8')

needle5=''' protected void onResume(){super.onResume();h.removeCallbacks(refresh);h.post(refresh);}protected void onPause(){h.removeCallbacks(refresh);super.onPause();}'''
repl5=''' protected void onResume(){super.onResume();ensureMotorAlive();h.removeCallbacks(refresh);h.post(refresh);}protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 void ensureMotorAlive(){
  boolean wanted=p.getBoolean("telegram_api_motor_on",false);
  if(!wanted)return;
  long hb=p.getLong("telegram_api_heartbeat",0L);
  long age=hb<=0?Long.MAX_VALUE:System.currentTimeMillis()-hb;
  if(age>45000L){
   try{Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);p.edit().putString("telegram_api_status","Reiniciando Motor Telegram API...").apply();}catch(Throwable e){p.edit().putString("telegram_api_status","Falha ao reiniciar Motor Telegram API").apply();}
  }
 }'''
if needle5 not in a: raise SystemExit('ERRO v2.1.37: onResume da tela Telegram API não encontrado')
a=a.replace(needle5,repl5,1)

# refresh também verifica travamento enquanto a tela está aberta
needle6=''' void update(){boolean on=p.getBoolean("telegram_api_motor_on",false);motor.setText(on?"MOTOR API LIGADO":"MOTOR API DESLIGADO");'''
repl6=''' void update(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(on){long hb=p.getLong("telegram_api_heartbeat",0L);if(hb<=0L||System.currentTimeMillis()-hb>45000L)ensureMotorAlive();}motor.setText(on?"MOTOR API LIGADO":"MOTOR API DESLIGADO");'''
if needle6 not in a: raise SystemExit('ERRO v2.1.37: update da tela Telegram API não encontrado')
a=a.replace(needle6,repl6,1)

act.write_text(a,encoding='utf-8')

# versão
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 108',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.37'",G,count=1)
gradle.write_text(G,encoding='utf-8')

D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.37",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

checks=[
 (tg,'telegram_api_heartbeat'),
 (act,'ensureMotorAlive()'),
 (act,'Reiniciando Motor Telegram API'),
 (gradle,"versionName '2.1.37'")
]
for pth,mark in checks:
 if mark not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.37: requisito ausente '+mark)

print('v2.1.37: Motor Telegram API ganha heartbeat e auto-reinício quando o botão fica verde mas o serviço não está vivo')
