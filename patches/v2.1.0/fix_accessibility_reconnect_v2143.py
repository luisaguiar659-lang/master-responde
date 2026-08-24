from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
acc=java/'TelegramWhatsAppAccessibilityService.java'
act=java/'TelegramApiSettingsActivity.java'
gradle=app/'build.gradle'
dash=java/'NeonDashboardActivity.java'

# v2.1.43
# NÃO altera TelegramApiAlbumService.java.
# Corrige somente a segunda etapa: conexão/reconexão do AccessibilityService + diagnóstico.

s=acc.read_text(encoding='utf-8')

# Garante heartbeat imediato e status explícito quando o serviço realmente conecta.
s=s.replace('TelegramAutoDeliveryStore.status(this,"Envio automático pronto");',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit()
            .putBoolean("telegram_accessibility_enabled",true)
            .putLong("telegram_accessibility_heartbeat",System.currentTimeMillis())
            .putString("telegram_accessibility_diag","SERVIÇO CONECTADO")
            .apply();
        TelegramAutoDeliveryStore.status(this,"Serviço conectado • aguardando conteúdo do Telegram");''',1)

# Atualiza diagnóstico no tick e força tentativa de pendência.
s=s.replace('getSharedPreferences("master_responde",MODE_PRIVATE).edit().putLong("telegram_accessibility_heartbeat",System.currentTimeMillis()).apply();',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit()
                .putLong("telegram_accessibility_heartbeat",System.currentTimeMillis())
                .putString("telegram_accessibility_diag","HEARTBEAT OK")
                .apply();''',1)

# Registra cada transição importante para saber exatamente onde para.
s=s.replace('TelegramAutoDeliveryStore.status(this,"Álbum pronto • iniciando envio para "+groups.size()+" grupo(s)...");',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","PENDÊNCIA DETECTADA").apply();
        TelegramAutoDeliveryStore.status(this,"Álbum pronto • iniciando envio para "+groups.size()+" grupo(s)...");''',1)
s=s.replace('TelegramAutoDeliveryStore.status(this,"Abrindo WhatsApp Business • grupo "+(index+1)+"/"+groups.size());',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","ABRINDO WHATSAPP BUSINESS").apply();
            TelegramAutoDeliveryStore.status(this,"Abrindo WhatsApp Business • grupo "+(index+1)+"/"+groups.size());''',1)
s=s.replace('TelegramAutoDeliveryStore.status(this,"Grupo localizado: "+wanted);',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","GRUPO LOCALIZADO: "+wanted).apply();
            TelegramAutoDeliveryStore.status(this,"Grupo localizado: "+wanted);''',1)
s=s.replace('TelegramAutoDeliveryStore.status(this,"Enviando para "+sent+"...");',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","ENVIANDO: "+sent).apply();
            TelegramAutoDeliveryStore.status(this,"Enviando para "+sent+"...");''',1)
s=s.replace('TelegramAutoDeliveryStore.status(this,"Enviado para "+sent+" • "+(index+1)+"/"+groups.size());',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putString("telegram_accessibility_diag","ENVIADO: "+sent).apply();
        TelegramAutoDeliveryStore.status(this,"Enviado para "+sent+" • "+(index+1)+"/"+groups.size());''',1)

# Quando o serviço é interrompido/destruído, limpa heartbeat e deixa diagnóstico claro.
s=s.replace('@Override public void onInterrupt(){TelegramAutoDeliveryStore.status(this,"Envio automático interrompido");}',
'''@Override public void onInterrupt(){
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putLong("telegram_accessibility_heartbeat",0L).putString("telegram_accessibility_diag","SERVIÇO INTERROMPIDO").apply();
        TelegramAutoDeliveryStore.status(this,"Envio automático interrompido");
    }''',1)
s=s.replace('getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",false).apply();',
'''getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",false).putLong("telegram_accessibility_heartbeat",0L).putString("telegram_accessibility_diag","SERVIÇO DESCONECTADO").apply();''',1)
acc.write_text(s,encoding='utf-8')

# Tela: mostra heartbeat real + diagnóstico e botão de reconexão explícito.
a=act.read_text(encoding='utf-8')
a=a.replace('TextView state,lastChat,count,caption,delivery,autoAccess;', 'TextView state,lastChat,count,caption,delivery,autoAccess,autoDiag;',1)
anchor='''r.addView(t("ACESSO PARA ENVIO AUTOMÁTICO",13,true));autoAccess=val();r.addView(autoAccess);Button accessBtn=new Button(this);accessBtn.setText("ATIVAR ENVIO AUTOMÁTICO");accessBtn.setOnClickListener(v->{try{startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Throwable e){Toast.makeText(this,"Abra Acessibilidade nas configurações",Toast.LENGTH_LONG).show();}});r.addView(accessBtn);'''
if anchor not in a: raise SystemExit('ERRO v2.1.43: bloco de acesso não encontrado')
repl='''r.addView(t("ACESSO PARA ENVIO AUTOMÁTICO",13,true));autoAccess=val();r.addView(autoAccess);r.addView(t("DIAGNÓSTICO DO ENVIO",13,true));autoDiag=val();r.addView(autoDiag);Button accessBtn=new Button(this);accessBtn.setText("RECONECTAR ENVIO AUTOMÁTICO");accessBtn.setOnClickListener(v->{p.edit().putLong("telegram_accessibility_heartbeat",0L).putString("telegram_accessibility_diag","ABRINDO CONFIGURAÇÕES DE ACESSIBILIDADE").apply();try{startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Throwable e){Toast.makeText(this,"Abra Acessibilidade nas configurações",Toast.LENGTH_LONG).show();}});r.addView(accessBtn);'''
a=a.replace(anchor,repl,1)
old='''long hb=p.getLong("telegram_accessibility_heartbeat",0L);boolean live=System.currentTimeMillis()-hb<5000L;autoAccess.setText(isAutoSenderEnabled()?(live?"ATIVO • serviço conectado • WhatsApp Business":"ATIVO nas configurações • reconecte o serviço") : "DESATIVADO • toque em ATIVAR ENVIO AUTOMÁTICO");String id=p.getString("telegram_api_last_chat_id","");'''
new='''long hb=p.getLong("telegram_accessibility_heartbeat",0L);boolean live=System.currentTimeMillis()-hb<5000L;autoAccess.setText(isAutoSenderEnabled()?(live?"ATIVO • serviço conectado • WhatsApp Business":"ATIVO nas configurações • serviço sem heartbeat") : "DESATIVADO • reconecte o envio automático");autoDiag.setText(p.getString("telegram_accessibility_diag",live?"HEARTBEAT OK":"SEM HEARTBEAT DO SERVIÇO"));String id=p.getString("telegram_api_last_chat_id","");'''
if old not in a: raise SystemExit('ERRO v2.1.43: update de status não encontrado')
a=a.replace(old,new,1)
act.write_text(a,encoding='utf-8')

# versão
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 114',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.43'",G,count=1)
gradle.write_text(G,encoding='utf-8')
D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.43",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

for pth,mark in [(acc,'SERVIÇO CONECTADO'),(acc,'PENDÊNCIA DETECTADA'),(act,'DIAGNÓSTICO DO ENVIO'),(act,'RECONECTAR ENVIO AUTOMÁTICO'),(gradle,"versionName '2.1.43'")]:
    if mark not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.43: requisito ausente '+mark)

print('v2.1.43: reconexão/heartbeat/diagnóstico do envio corrigidos; captura Telegram API preservada')
