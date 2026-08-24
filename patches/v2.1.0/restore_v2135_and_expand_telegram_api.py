from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
tg=java/'TelegramApiAlbumService.java'
act=java/'TelegramApiSettingsActivity.java'
svc=java/'WhatsAppBusinessCaptureService.java'
dash=java/'NeonDashboardActivity.java'
manifest=app/'src/main/AndroidManifest.xml'
gradle=app/'build.gradle'

# v2.1.40
# Parte do Motor Telegram API que já funcionava no aparelho e NÃO altera
# getUpdates/getFile/media_group_id. Apenas acrescenta a segunda função:
# recebeu o álbum -> marca entrega pendente -> envia automaticamente aos grupos
# configurados na própria tela do Motor Telegram API.

# 1) Store independente dos grupos/delivery.
(java/'TelegramAutoDeliveryStore.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import org.json.*;import java.io.*;import java.util.*;
public final class TelegramAutoDeliveryStore{
 private static final String P="master_responde";private TelegramAutoDeliveryStore(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static java.util.List<String> groups(Context c){ArrayList<String>o=new ArrayList<>();String raw=p(c).getString("telegram_api_destination_groups","");for(String x:raw.split("\\n")){String g=x.trim();if(!g.isEmpty()&&!o.contains(g))o.add(g);}return o;}
 public static java.util.List<File> images(Context c){ArrayList<File>o=new ArrayList<>();try{JSONArray a=new JSONArray(p(c).getString("telegram_api_album_paths","[]"));for(int i=0;i<a.length();i++){File f=new File(a.optString(i,""));if(f.exists())o.add(f);}}catch(Throwable ignored){}return o;}
 public static void albumChanged(Context c){p(c).edit().putBoolean("telegram_auto_pending",true).putLong("telegram_auto_changed_at",System.currentTimeMillis()).remove("telegram_auto_done_groups").putString("telegram_auto_status","Recebendo conteúdo do Telegram...").apply();}
 public static boolean ready(Context c){SharedPreferences x=p(c);return x.getBoolean("telegram_auto_pending",false)&&System.currentTimeMillis()-x.getLong("telegram_auto_changed_at",0L)>=2500L;}
 public static Set<String> done(Context c){HashSet<String>o=new HashSet<>();for(String x:p(c).getString("telegram_auto_done_groups","").split("\\n")){x=x.trim().toLowerCase(Locale.ROOT);if(!x.isEmpty())o.add(x);}return o;}
 public static void saveDone(Context c,Set<String>s){StringBuilder b=new StringBuilder();for(String x:s){if(b.length()>0)b.append('\n');b.append(x);}p(c).edit().putString("telegram_auto_done_groups",b.toString()).apply();}
 public static void status(Context c,String s){p(c).edit().putString("telegram_auto_status",s).apply();}
 public static void finish(Context c,int groups,int images){p(c).edit().putBoolean("telegram_auto_pending",false).remove("telegram_auto_done_groups").putLong("telegram_auto_last_sent_at",System.currentTimeMillis()).putString("telegram_auto_status","Concluído: "+images+" imagens enviadas para "+groups+" grupos").apply();}
}''',encoding='utf-8')

# 2) Mantém polling original. Só avisa que o lote mudou depois de salvar a imagem.
s=tg.read_text(encoding='utf-8')
anchor='''.putString("telegram_api_last_caption",caption).putString("telegram_api_status",paths.length()>1?"Álbum capturado: "+paths.length()+" imagens":"Imagem capturada pela API")
                    .apply();'''
if anchor not in s: raise SystemExit('ERRO v2.1.40: saveAlbumItem do motor funcional não encontrado')
if 'TelegramAutoDeliveryStore.albumChanged(this)' not in s:
 s=s.replace(anchor,anchor+'\n            TelegramAutoDeliveryStore.albumChanged(this);',1)
tg.write_text(s,encoding='utf-8')

# 3) Tela do Motor API: destinos próprios + status de entrega + botão confiável.
a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p;EditText token,source;TextView state,lastChat,count,caption;LinearLayout previews;Button motor;',
'''SharedPreferences p;EditText token,source,destinations;TextView state,lastChat,count,caption,delivery;LinearLayout previews;Button motor;''',1)
anchor_ui='''  r.addView(t("CHAT DE ORIGEM (ID)",13,true));source=f("Deixe vazio no primeiro teste");source.setText(p.getString("telegram_api_source_chat_id",""));r.addView(source);'''
if anchor_ui not in a: raise SystemExit('ERRO v2.1.40: campo CHAT DE ORIGEM não encontrado')
a=a.replace(anchor_ui,anchor_ui+'''\n  r.addView(t("GRUPOS DE DESTINO - WHATSAPP BUSINESS",13,true));r.addView(t("Um grupo por linha. Recebeu no Telegram, envia automaticamente para estes grupos.",11,false));destinations=f("Grupo 1\\nGrupo 2");destinations.setMinLines(4);destinations.setGravity(Gravity.TOP);destinations.setText(p.getString("telegram_api_destination_groups",""));r.addView(destinations);''',1)
oldsave='''Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->{p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();});r.addView(save);'''
if oldsave not in a: raise SystemExit('ERRO v2.1.40: botão SALVAR não encontrado')
a=a.replace(oldsave,'''Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->{p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).putString("telegram_api_destination_groups",destinations.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();});r.addView(save);''',1)
oldstatus='''r.addView(t("STATUS",13,true));state=val();r.addView(state);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));'''
if oldstatus not in a: raise SystemExit('ERRO v2.1.40: bloco STATUS não encontrado')
a=a.replace(oldstatus,'''r.addView(t("STATUS DA API",13,true));state=val();r.addView(state);r.addView(t("ENVIO AUTOMÁTICO",13,true));delivery=val();r.addView(delivery);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));''',1)
oldtoggle=''' void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}p.edit().putString("telegram_api_token",tk).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(new Intent(this,TelegramApiAlbumService.class));h.postDelayed(this::update,400);}'''
if oldtoggle not in a: raise SystemExit('ERRO v2.1.40: toggle original não encontrado')
a=a.replace(oldtoggle,''' void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}p.edit().putBoolean("telegram_api_motor_on",true).putString("telegram_api_token",tk).putString("telegram_api_source_chat_id",source.getText().toString().trim()).putString("telegram_api_destination_groups",destinations.getText().toString().trim()).putString("telegram_api_status","Iniciando Motor Telegram API...").apply();Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else{p.edit().putBoolean("telegram_api_motor_on",false).putString("telegram_api_status","Motor Telegram API desligado").apply();stopService(new Intent(this,TelegramApiAlbumService.class));}update();}''',1)
needle='''state.setText(p.getString("telegram_api_status","Aguardando configuração"));String id=p.getString("telegram_api_last_chat_id","");'''
if needle not in a: raise SystemExit('ERRO v2.1.40: update da tela não encontrado')
a=a.replace(needle,'''state.setText(p.getString("telegram_api_status","Aguardando configuração"));delivery.setText(p.getString("telegram_auto_status","Aguardando conteúdo do Telegram"));String id=p.getString("telegram_api_last_chat_id","");''',1)
act.write_text(a,encoding='utf-8')

# 4) Bridge de mídia para WhatsApp Business via RemoteInput de dados.
(java/'TelegramWhatsAppAutoSender.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.content.*;import android.net.*;import androidx.core.content.FileProvider;import java.io.*;import java.util.*;
public final class TelegramWhatsAppAutoSender{
 private TelegramWhatsAppAutoSender(){}
 public static boolean send(Context c,Notification n,List<File>files){if(c==null||n==null||files==null||files.isEmpty())return false;Notification.Action action=find(n);if(action==null||action.actionIntent==null)return false;RemoteInput[] inputs=action.getRemoteInputs();if(inputs==null||inputs.length==0)return false;for(File f:files){try{Uri u=FileProvider.getUriForFile(c,c.getPackageName()+".telegramfiles",f);Intent fill=new Intent();fill.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);boolean attached=false;for(RemoteInput r:inputs){if(r==null||r.getResultKey()==null)continue;if(r.isDataOnly()||allowsImage(r)){HashMap<String,Uri>d=new HashMap<>();d.put("image/jpeg",u);RemoteInput.addDataResultToIntent(r,fill,d);attached=true;break;}}if(!attached)return false;action.actionIntent.send(c,0,fill);try{Thread.sleep(650L);}catch(InterruptedException ignored){}}catch(Throwable e){return false;}}return true;}
 private static Notification.Action find(Notification n){if(n.actions==null)return null;for(Notification.Action a:n.actions){if(a==null||a.actionIntent==null)continue;RemoteInput[]rs=a.getRemoteInputs();if(rs==null)continue;for(RemoteInput r:rs)if(r!=null&&(r.isDataOnly()||allowsImage(r)))return a;}return null;}
 private static boolean allowsImage(RemoteInput r){try{Set<String>s=r.getAllowedDataTypes();if(s!=null)for(String x:s)if(x!=null&&x.toLowerCase(Locale.ROOT).startsWith("image/"))return true;}catch(Throwable ignored){}return false;}
}''',encoding='utf-8')

# FileProvider restrito ao diretório das imagens Telegram.
xml=app/'src/main/res/xml';xml.mkdir(parents=True,exist_ok=True)
(xml/'telegram_file_paths.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n <files-path name="telegram_album" path="telegram_api_album/"/>\n</paths>\n''',encoding='utf-8')
m=manifest.read_text(encoding='utf-8')
if '.telegramfiles' not in m:
 m=m.replace('</application>','''<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.telegramfiles" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/telegram_file_paths"/></provider>\n</application>''')
manifest.write_text(m,encoding='utf-8')

# 5) Entrega automática independente dentro do NotificationListener já existente.
s=svc.read_text(encoding='utf-8')
# desliga detector antigo: Telegram não é mais processado por notificação
s=re.sub(r'''if \(isTelegramPackage\(sourcePackage\)\) \{\s*captureTelegramNotification\(sbn\);\s*return;\s*\}''','if (isTelegramPackage(sourcePackage)) return;',s,count=1)
# liga loop próprio quando listener conecta/desconecta
if 'startTelegramApiAutoDeliveryLoop();' not in s:
 s=s.replace('super.onListenerConnected();','super.onListenerConnected();\n        startTelegramApiAutoDeliveryLoop();',1)
if 'stopTelegramApiAutoDeliveryLoop();' not in s:
 s=s.replace('super.onListenerDisconnected();','stopTelegramApiAutoDeliveryLoop();\n        super.onListenerDisconnected();',1)
if 'private final android.os.Handler telegramApiAutoHandler' not in s:
 pos=s.rfind('}')
 helper=r'''
    private final android.os.Handler telegramApiAutoHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable telegramApiAutoTick = new Runnable(){@Override public void run(){runTelegramApiAutoDelivery();telegramApiAutoHandler.postDelayed(this,1500L);}};
    private void startTelegramApiAutoDeliveryLoop(){telegramApiAutoHandler.removeCallbacks(telegramApiAutoTick);telegramApiAutoHandler.postDelayed(telegramApiAutoTick,1200L);}
    private void stopTelegramApiAutoDeliveryLoop(){telegramApiAutoHandler.removeCallbacks(telegramApiAutoTick);}
    private void runTelegramApiAutoDelivery(){
        if(!TelegramAutoDeliveryStore.ready(this))return;
        java.util.List<String>targets=TelegramAutoDeliveryStore.groups(this);
        java.util.List<java.io.File>images=TelegramAutoDeliveryStore.images(this);
        if(targets.isEmpty()){TelegramAutoDeliveryStore.status(this,"Configure pelo menos um grupo de destino");return;}
        if(images.isEmpty()){TelegramAutoDeliveryStore.status(this,"Nenhuma imagem válida no último lote");return;}
        java.util.Set<String>done=TelegramAutoDeliveryStore.done(this);
        try{
            StatusBarNotification[]active=getActiveNotifications();
            if(active==null){TelegramAutoDeliveryStore.status(this,"Aguardando notificações do WhatsApp Business");return;}
            for(String wanted:targets){
                String key=wanted.trim().toLowerCase(java.util.Locale.ROOT);if(key.isEmpty()||done.contains(key))continue;
                for(StatusBarNotification item:active){
                    if(item==null||!PACKAGE_NAME.equals(item.getPackageName()))continue;
                    Notification n=item.getNotification();if(n==null||!isGroupNotification(n)||n.extras==null)continue;
                    String conv=firstNonEmpty(n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),n.extras.getCharSequence(Notification.EXTRA_TITLE));
                    if(!wanted.equalsIgnoreCase(conv.trim()))continue;
                    TelegramAutoDeliveryStore.status(this,"Enviando para "+wanted+"...");
                    if(TelegramWhatsAppAutoSender.send(this,n,images)){
                        done.add(key);TelegramAutoDeliveryStore.saveDone(this,done);
                        TelegramAutoDeliveryStore.status(this,"Enviado para "+done.size()+" de "+targets.size()+" grupos");
                        if(done.size()>=targets.size())TelegramAutoDeliveryStore.finish(this,targets.size(),images.size());
                        return;
                    }else{TelegramAutoDeliveryStore.status(this,"Grupo localizado, mas o WhatsApp Business não expôs envio de imagem pela notificação");return;}
                }
            }
            TelegramAutoDeliveryStore.status(this,"Álbum pronto: aguardando notificação ativa dos grupos configurados");
        }catch(Throwable e){TelegramAutoDeliveryStore.status(this,"Falha temporária no envio automático");}
    }
'''
 s=s[:pos]+helper+s[pos:]
svc.write_text(s,encoding='utf-8')

# 6) Remove Detector Telegram do menu, mantendo só Motor Telegram API.
D=dash.read_text(encoding='utf-8')
D=re.sub(r'\s*panel\.addView\(sideItem\("✈","Detector Telegram",Color\.WHITE,v->\{d\.dismiss\(\);startActivity\(new Intent\(this,TelegramDetectorActivity\.class\)\);\}\)\);','',D,count=1)
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.40",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

# 7) AndroidX Core para FileProvider e versão final.
G=gradle.read_text(encoding='utf-8')
if 'androidx.core:core:' not in G:
 if 'dependencies {' in G:G=G.replace('dependencies {',"dependencies {\n    implementation 'androidx.core:core:1.15.0'",1)
 else:G+='\n\ndependencies {\n    implementation \'androidx.core:core:1.15.0\'\n}\n'
G=re.sub(r'versionCode\s+\d+','versionCode 111',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.40'",G,count=1)
gradle.write_text(G,encoding='utf-8')

# Checks fortes: falha build se a mudança não entrou.
checks=[
 (tg,'getUpdates'),(tg,'media_group_id'),(tg,'TelegramAutoDeliveryStore.albumChanged(this)'),
 (act,'GRUPOS DE DESTINO - WHATSAPP BUSINESS'),(act,'ENVIO AUTOMÁTICO'),
 (svc,'runTelegramApiAutoDelivery()'),(svc,'TelegramAutoDeliveryStore.groups(this)'),
 (dash,'Motor Telegram API'),(gradle,"versionName '2.1.40'")]
for pth,mark in checks:
 if mark not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.40: requisito ausente '+mark)
if 'Detector Telegram' in dash.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.40: Detector Telegram ainda está no menu')
print('v2.1.40: motor API funcional preservado; Detector removido; destinos próprios e envio automático independente adicionados')
