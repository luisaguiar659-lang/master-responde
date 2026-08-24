from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
tg=java/'TelegramApiAlbumService.java'
act=java/'TelegramApiSettingsActivity.java'
svc=java/'WhatsAppBusinessCaptureService.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# v2.1.38
# Motor Telegram API independente:
# Telegram -> captura álbum -> fecha o álbum -> envia automaticamente aos grupos
# configurados NA PRÓPRIA tela do Motor Telegram API.
# Não usa gatilho, não usa horário e não depende de Avisos Automáticos.

(java/'TelegramAutoDeliveryStore.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.util.*;
public final class TelegramAutoDeliveryStore{
 private static final String P="master_responde";private TelegramAutoDeliveryStore(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static java.util.List<String> groups(Context c){
  java.util.ArrayList<String> out=new java.util.ArrayList<>();
  String raw=p(c).getString("telegram_api_destination_groups","");
  for(String x:raw.split("\\n")){String g=x.trim();if(!g.isEmpty()&&!out.contains(g))out.add(g);}return out;
 }
 public static String rawGroups(Context c){return p(c).getString("telegram_api_destination_groups","");}
}''',encoding='utf-8')

# 1) Tela do Motor Telegram API ganha destinos próprios.
a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p;EditText token,source;TextView state,lastChat,count,caption;LinearLayout previews;Button motor;',
'''SharedPreferences p;EditText token,source,destinations;TextView state,lastChat,count,caption,delivery;LinearLayout previews;Button motor;''',1)

anchor='''  r.addView(t("CHAT DE ORIGEM (ID)",13,true));source=f("Deixe vazio no primeiro teste");source.setText(p.getString("telegram_api_source_chat_id",""));r.addView(source);'''
insert=anchor+'''\n  r.addView(t("GRUPOS DE DESTINO - WHATSAPP BUSINESS",13,true));r.addView(t("Um nome exato de grupo por linha. Recebeu no Telegram, envia automaticamente para estes grupos.",11,false));destinations=f("Grupo 1\\nGrupo 2\\nGrupo 3");destinations.setMinLines(5);destinations.setGravity(Gravity.TOP);destinations.setText(p.getString("telegram_api_destination_groups",""));r.addView(destinations);'''
if anchor not in a: raise SystemExit('ERRO v2.1.38: campo CHAT DE ORIGEM não encontrado')
a=a.replace(anchor,insert,1)

oldsave='''Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->{p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();});r.addView(save);'''
newsave='''Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->{p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).putString("telegram_api_destination_groups",destinations.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();});r.addView(save);'''
if oldsave not in a: raise SystemExit('ERRO v2.1.38: botão salvar Telegram API não encontrado')
a=a.replace(oldsave,newsave,1)

oldstatus='''r.addView(t("STATUS",13,true));state=val();r.addView(state);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));'''
newstatus='''r.addView(t("STATUS",13,true));state=val();r.addView(state);r.addView(t("ENVIO AUTOMÁTICO",13,true));delivery=val();r.addView(delivery);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));'''
if oldstatus not in a: raise SystemExit('ERRO v2.1.38: bloco STATUS não encontrado')
a=a.replace(oldstatus,newstatus,1)

# Botão liga/desliga deixa de depender de onDestroy para destravar.
oldtoggle=''' void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}p.edit().putString("telegram_api_token",tk).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(new Intent(this,TelegramApiAlbumService.class));h.postDelayed(this::update,400);}'''
newtoggle=''' void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}p.edit().putBoolean("telegram_api_motor_on",true).putString("telegram_api_token",tk).putString("telegram_api_source_chat_id",source.getText().toString().trim()).putString("telegram_api_destination_groups",destinations.getText().toString().trim()).putString("telegram_api_status","Iniciando Motor Telegram API...").apply();Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else{p.edit().putBoolean("telegram_api_motor_on",false).putString("telegram_api_status","Motor Telegram API desligado").apply();stopService(new Intent(this,TelegramApiAlbumService.class));}update();h.postDelayed(this::update,400);}'''
if oldtoggle not in a: raise SystemExit('ERRO v2.1.38: toggle Telegram API não encontrado')
a=a.replace(oldtoggle,newtoggle,1)

# Mostra estado da entrega independente.
needle='''state.setText(p.getString("telegram_api_status","Aguardando configuração"));String id=p.getString("telegram_api_last_chat_id","");'''
repl='''state.setText(p.getString("telegram_api_status","Aguardando configuração"));delivery.setText(p.getString("telegram_auto_album_status","Aguardando novo conteúdo do Telegram"));String id=p.getString("telegram_api_last_chat_id","");'''
if needle not in a: raise SystemExit('ERRO v2.1.38: update status Telegram API não encontrado')
a=a.replace(needle,repl,1)
act.write_text(a,encoding='utf-8')

# 2) O recebimento continua marcando o álbum como pendente. Melhora o status.
s=tg.read_text(encoding='utf-8')
needle2='''            TelegramAlbumWhatsAppBridge.markChanged(this);'''
if needle2 not in s: raise SystemExit('ERRO v2.1.38: integração de álbum pendente não encontrada')
s=s.replace(needle2,'''            TelegramAlbumWhatsAppBridge.markChanged(this);\n            prefs().edit().putString("telegram_auto_album_status","Recebendo álbum do Telegram...").apply();''',1)
tg.write_text(s,encoding='utf-8')

# 3) Entrega automática ganha relógio próprio, separado de Avisos Automáticos.
s=svc.read_text(encoding='utf-8')
# remove chamada acoplada ao relógio dos avisos, se existir
s=s.replace('runScheduledGroupBroadcastIfDue(); runTelegramAlbumAutoDelivery(); scheduledGroupHandler.postDelayed(this, 3000L);',
            'runScheduledGroupBroadcastIfDue(); scheduledGroupHandler.postDelayed(this, 3000L);',1)

# destinos passam a vir exclusivamente do Motor Telegram API
s=s.replace('java.util.List<String> targets = ScheduledGroupBroadcast.groups(this);',
            'java.util.List<String> targets = TelegramAutoDeliveryStore.groups(this);',1)
s=s.replace('Álbum capturado, mas nenhum grupo está selecionado em Avisos Automáticos',
            'Álbum capturado, mas nenhum grupo de destino está configurado no Motor Telegram API',1)

# inicia/encerra relógio próprio junto do NotificationListener, sem depender do motor programado
if 'startTelegramAutoDeliveryLoop();' not in s:
    s=s.replace('startScheduledGroupBroadcastLoop();','startScheduledGroupBroadcastLoop();\n        startTelegramAutoDeliveryLoop();',1)
if 'stopTelegramAutoDeliveryLoop();' not in s:
    s=s.replace('stopScheduledGroupBroadcastLoop();','stopScheduledGroupBroadcastLoop();\n        stopTelegramAutoDeliveryLoop();',1)

if 'private final android.os.Handler telegramAutoDeliveryHandler' not in s:
    pos=s.rfind('}')
    helper=r'''
    private final android.os.Handler telegramAutoDeliveryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable telegramAutoDeliveryTick = new Runnable() {
        @Override public void run() {
            runTelegramAlbumAutoDelivery();
            telegramAutoDeliveryHandler.postDelayed(this, 1500L);
        }
    };

    private void startTelegramAutoDeliveryLoop() {
        telegramAutoDeliveryHandler.removeCallbacks(telegramAutoDeliveryTick);
        telegramAutoDeliveryHandler.postDelayed(telegramAutoDeliveryTick, 1000L);
    }

    private void stopTelegramAutoDeliveryLoop() {
        telegramAutoDeliveryHandler.removeCallbacks(telegramAutoDeliveryTick);
    }
'''
    s=s[:pos]+helper+s[pos:]
svc.write_text(s,encoding='utf-8')

# 4) Desativa o Detector Telegram antigo: sem menu e sem captura por notificação.
D=dash.read_text(encoding='utf-8')
D=re.sub(r'\s*panel\.addView\(sideItem\("✈","Detector Telegram",Color\.WHITE,v->\{d\.dismiss\(\);startActivity\(new Intent\(this,TelegramDetectorActivity\.class\)\);\}\)\);','',D,count=1)
dash.write_text(D,encoding='utf-8')

s=svc.read_text(encoding='utf-8')
old='''        if (isTelegramPackage(sourcePackage)) {\n            captureTelegramNotification(sbn);\n            return;\n        }'''
new='''        if (isTelegramPackage(sourcePackage)) return;'''
if old in s:s=s.replace(old,new,1)
svc.write_text(s,encoding='utf-8')

# 5) versão
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 109',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.38'",G,count=1)
gradle.write_text(G,encoding='utf-8')
D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.38",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

checks=[
 (java/'TelegramAutoDeliveryStore.java','telegram_api_destination_groups'),
 (act,'GRUPOS DE DESTINO - WHATSAPP BUSINESS'),
 (act,'putBoolean("telegram_api_motor_on",false)'),
 (svc,'TelegramAutoDeliveryStore.groups(this)'),
 (svc,'telegramAutoDeliveryHandler'),
 (svc,'startTelegramAutoDeliveryLoop()'),
 (gradle,"versionName '2.1.38'")
]
for pth,mark in checks:
 if mark not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.38: requisito ausente '+mark)

print('v2.1.38: Motor Telegram API independente criado: recebeu álbum -> envia automaticamente, sem gatilho e sem Avisos Automáticos; destinos próprios e Detector Telegram antigo desativado')
