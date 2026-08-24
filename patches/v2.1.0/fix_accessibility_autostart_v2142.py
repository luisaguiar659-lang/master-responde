from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
act=java/'TelegramApiSettingsActivity.java'
acc=java/'TelegramWhatsAppAccessibilityService.java'
gradle=app/'build.gradle'
dash=java/'NeonDashboardActivity.java'

# v2.1.42
# NÃO altera TelegramApiAlbumService.java.
# Corrige apenas a partida da SEGUNDA ETAPA (envio automático via Acessibilidade).

s=acc.read_text(encoding='utf-8')

# 1) Escuta em tempo real a flag telegram_auto_pending gravada quando o álbum termina de ser salvo.
# IMPORTANTE: beginPending precisa ser declarado ANTES do prefListener para evitar
# "illegal forward reference" no javac.
s=s.replace('private List<File> images=new ArrayList<>();', '''private List<File> images=new ArrayList<>();
    private android.content.SharedPreferences prefs;
    private final Runnable beginPending=new Runnable(){@Override public void run(){
        try{if(!busy && TelegramAutoDeliveryStore.ready(TelegramWhatsAppAccessibilityService.this))beginBatch();}
        catch(Throwable e){TelegramAutoDeliveryStore.status(TelegramWhatsAppAccessibilityService.this,"Falha ao iniciar envio automático");}
    }};
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener=(sp,key)->{
        if("telegram_auto_pending".equals(key) && sp.getBoolean("telegram_auto_pending",false)){
            TelegramAutoDeliveryStore.status(TelegramWhatsAppAccessibilityService.this,"Conteúdo recebido • preparando envio automático...");
            h.removeCallbacks(beginPending);
            h.postDelayed(beginPending,2800L);
        }
    };''',1)

old='''    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",true).apply();
        TelegramAutoDeliveryStore.status(this,"Envio automático pronto");
        h.removeCallbacks(tick);h.post(tick);
    }'''
new='''    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        prefs=getSharedPreferences("master_responde",MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        prefs.edit().putBoolean("telegram_accessibility_enabled",true).putLong("telegram_accessibility_heartbeat",System.currentTimeMillis()).apply();
        TelegramAutoDeliveryStore.status(this,"Envio automático pronto");
        h.removeCallbacks(tick);h.post(tick);
        if(prefs.getBoolean("telegram_auto_pending",false)){h.removeCallbacks(beginPending);h.postDelayed(beginPending,800L);}
    }'''
if old not in s: raise SystemExit('ERRO v2.1.42: onServiceConnected v2.1.41 não encontrado')
s=s.replace(old,new,1)

old2='''    @Override public void onDestroy(){
        h.removeCallbacksAndMessages(null);
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",false).apply();
        super.onDestroy();
    }'''
new2='''    @Override public void onDestroy(){
        try{if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(prefListener);}catch(Throwable ignored){}
        h.removeCallbacksAndMessages(null);
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",false).apply();
        super.onDestroy();
    }'''
if old2 not in s: raise SystemExit('ERRO v2.1.42: onDestroy v2.1.41 não encontrado')
s=s.replace(old2,new2,1)

# 2) Tick passa a registrar heartbeat e tentar pendências em qualquer estado de tela.
oldtick='''    private final Runnable tick=new Runnable(){@Override public void run(){
        try{if(!busy && TelegramAutoDeliveryStore.ready(TelegramWhatsAppAccessibilityService.this))beginBatch();}
        catch(Throwable e){TelegramAutoDeliveryStore.status(TelegramWhatsAppAccessibilityService.this,"Falha ao preparar envio automático");}
        h.postDelayed(this,1000L);
    }};'''
newtick='''    private final Runnable tick=new Runnable(){@Override public void run(){
        try{
            getSharedPreferences("master_responde",MODE_PRIVATE).edit().putLong("telegram_accessibility_heartbeat",System.currentTimeMillis()).apply();
            if(!busy && TelegramAutoDeliveryStore.ready(TelegramWhatsAppAccessibilityService.this))beginBatch();
        }catch(Throwable e){TelegramAutoDeliveryStore.status(TelegramWhatsAppAccessibilityService.this,"Falha ao preparar envio automático");}
        h.postDelayed(this,1000L);
    }};'''
if oldtick not in s: raise SystemExit('ERRO v2.1.42: tick v2.1.41 não encontrado')
s=s.replace(oldtick,newtick,1)

# 3) Ao iniciar lote, já deixa diagnóstico claro antes de abrir o WhatsApp Business.
s=s.replace('''        busy=true;index=0;launchCurrent();''','''        busy=true;index=0;TelegramAutoDeliveryStore.status(this,"Álbum pronto • iniciando envio para "+groups.size()+" grupo(s)...");h.postDelayed(this::launchCurrent,250L);''',1)

acc.write_text(s,encoding='utf-8')

# 4) Tela mostra se o serviço está realmente executando, não apenas habilitado nas Configurações.
a=act.read_text(encoding='utf-8')
old_ui='''autoAccess.setText(isAutoSenderEnabled()?"ATIVO • WhatsApp Business":"DESATIVADO • toque em ATIVAR ENVIO AUTOMÁTICO");String id=p.getString("telegram_api_last_chat_id","");'''
new_ui='''long hb=p.getLong("telegram_accessibility_heartbeat",0L);boolean live=System.currentTimeMillis()-hb<5000L;autoAccess.setText(isAutoSenderEnabled()?(live?"ATIVO • serviço conectado • WhatsApp Business":"ATIVO nas configurações • reconecte o serviço") : "DESATIVADO • toque em ATIVAR ENVIO AUTOMÁTICO");String id=p.getString("telegram_api_last_chat_id","");'''
if old_ui not in a: raise SystemExit('ERRO v2.1.42: status de acessibilidade v2.1.41 não encontrado')
a=a.replace(old_ui,new_ui,1)
act.write_text(a,encoding='utf-8')

# 5) Versão. Captura Telegram permanece byte-a-byte fora deste patch.
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 113',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.42'",G,count=1)
gradle.write_text(G,encoding='utf-8')
D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.42",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

for pth,mark in [(acc,'OnSharedPreferenceChangeListener'),(acc,'telegram_accessibility_heartbeat'),(act,'serviço conectado'),(gradle,"versionName '2.1.42'")]:
    if mark not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.42: requisito ausente '+mark)
print('v2.1.42: partida automática corrigida; beginPending declarado antes do listener; captura Telegram API intocada')