from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; tg=java/'TelegramApiAlbumService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'

# v2.1.36: envio AUTOMÁTICO do álbum recebido pelo Telegram para os mesmos
# grupos selecionados em Avisos Automáticos (ScheduledGroupBroadcast.groups).
# Não depende de comando no WhatsApp. O álbum é considerado completo após
# uma pequena janela sem novas imagens e entra numa fila de entrega imediata.

(java/'TelegramAlbumWhatsAppBridge.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.content.*;import android.net.Uri;import androidx.core.content.FileProvider;import org.json.JSONArray;import java.io.File;import java.util.*;
public final class TelegramAlbumWhatsAppBridge{
 private static final String P="master_responde";private TelegramAlbumWhatsAppBridge(){}
 public static void markChanged(Context c){c.getSharedPreferences(P,0).edit().putBoolean("telegram_auto_album_pending",true).putLong("telegram_auto_album_changed_at",System.currentTimeMillis()).remove("telegram_auto_album_done_groups").apply();}
 public static boolean pending(Context c){SharedPreferences p=c.getSharedPreferences(P,0);return p.getBoolean("telegram_auto_album_pending",false)&&System.currentTimeMillis()-p.getLong("telegram_auto_album_changed_at",0L)>=2200L;}
 public static List<File> files(Context c){SharedPreferences p=c.getSharedPreferences(P,0);List<File>o=new ArrayList<>();try{JSONArray a=new JSONArray(p.getString("telegram_api_album_paths","[]"));for(int i=0;i<a.length();i++){String x=a.optString(i,"").trim();if(!x.isEmpty()){File f=new File(x);if(f.exists())o.add(f);}}}catch(Throwable ignored){}return o;}
 public static Set<String> doneGroups(Context c){Set<String>o=new HashSet<>();String s=c.getSharedPreferences(P,0).getString("telegram_auto_album_done_groups","");for(String x:s.split("\\n")){x=x.trim().toLowerCase(Locale.ROOT);if(!x.isEmpty())o.add(x);}return o;}
 public static void saveDone(Context c,Set<String>d){StringBuilder b=new StringBuilder();for(String x:d){if(b.length()>0)b.append('\n');b.append(x);}c.getSharedPreferences(P,0).edit().putString("telegram_auto_album_done_groups",b.toString()).apply();}
 public static void finish(Context c,int groups,int images){c.getSharedPreferences(P,0).edit().putBoolean("telegram_auto_album_pending",false).remove("telegram_auto_album_done_groups").putLong("telegram_auto_album_sent_at",System.currentTimeMillis()).putString("telegram_auto_album_status","Álbum enviado automaticamente: "+images+" imagens para "+groups+" grupos").apply();}
 public static void status(Context c,String s){c.getSharedPreferences(P,0).edit().putString("telegram_auto_album_status",s).apply();}
 public static boolean sendAlbum(Context c,Notification n,List<File>files){if(c==null||n==null||files==null||files.isEmpty())return false;Notification.Action a=mediaAction(n);if(a==null||a.actionIntent==null){status(c,"Aguardando ação de mídia do WhatsApp Business");return false;}RemoteInput[] inputs=a.getRemoteInputs();if(inputs==null||inputs.length==0){status(c,"WhatsApp Business sem entrada de mídia nesta notificação");return false;}for(File f:files){try{Uri u=FileProvider.getUriForFile(c,c.getPackageName()+".telegramfiles",f);Intent fill=new Intent();fill.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);boolean attached=false;for(RemoteInput r:inputs){if(r==null||r.getResultKey()==null)continue;if(r.isDataOnly()||allowsImage(r)){Map<String,Uri>d=new HashMap<>();d.put("image/jpeg",u);RemoteInput.addDataResultToIntent(r,fill,d);attached=true;break;}}if(!attached){status(c,"Aguardando suporte a imagem na notificação do WhatsApp Business");return false;}a.actionIntent.send(c,0,fill);try{Thread.sleep(700L);}catch(InterruptedException ignored){}}catch(Throwable e){status(c,"Falha temporária no envio automático do álbum");return false;}}return true;}
 private static Notification.Action mediaAction(Notification n){if(n.actions==null)return null;for(Notification.Action a:n.actions){if(a==null||a.actionIntent==null)continue;RemoteInput[] rs=a.getRemoteInputs();if(rs==null)continue;for(RemoteInput r:rs)if(r!=null&&(r.isDataOnly()||allowsImage(r)))return a;}return null;}
 private static boolean allowsImage(RemoteInput r){try{Set<String>t=r.getAllowedDataTypes();if(t!=null)for(String x:t)if(x!=null&&x.toLowerCase(Locale.ROOT).startsWith("image/"))return true;}catch(Throwable ignored){}return false;}
}''',encoding='utf-8')

# O recebimento de cada foto rearma o debounce. Depois de ~2,2s sem nova foto,
# o serviço do WhatsApp Business passa a tratar o álbum como completo.
s=tg.read_text(encoding='utf-8')
needle='''                    .putString("telegram_api_last_caption",caption).putString("telegram_api_status",paths.length()>1?"Álbum capturado: "+paths.length()+" imagens":"Imagem capturada pela API")
                    .apply();'''
if needle not in s: raise SystemExit('ERRO v2.1.36: persistência do álbum Telegram não encontrada')
s=s.replace(needle,needle+'\n            TelegramAlbumWhatsAppBridge.markChanged(this);',1)
tg.write_text(s,encoding='utf-8')

xml=app/'src/main/res/xml';xml.mkdir(parents=True,exist_ok=True);(xml/'telegram_file_paths.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n <files-path name="telegram_album" path="telegram_api_album/" />\n</paths>\n''',encoding='utf-8')
m=manifest.read_text(encoding='utf-8');provider='''\n        <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.telegramfiles" android:exported="false" android:grantUriPermissions="true">\n            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/telegram_file_paths"/>\n        </provider>\n'''
if '.telegramfiles' not in m:m=m.replace('</application>',provider+'</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
if 'androidx.core:core:' not in g and 'androidx.core:core-' not in g:
 g=g.replace('dependencies {',"dependencies {\n    implementation 'androidx.core:core:1.15.0'",1) if 'dependencies {' in g else g+"\n\ndependencies {\n    implementation 'androidx.core:core:1.15.0'\n}\n"
g=re.sub(r'versionCode\s+\d+','versionCode 107',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.36'",g,count=1);gradle.write_text(g,encoding='utf-8')

s=svc.read_text(encoding='utf-8')
old='''private final Runnable scheduledGroupTick = new Runnable() { @Override public void run() { runScheduledGroupBroadcastIfDue(); scheduledGroupHandler.postDelayed(this, 3000L); } };'''
new='''private final Runnable scheduledGroupTick = new Runnable() { @Override public void run() { runScheduledGroupBroadcastIfDue(); runTelegramAlbumAutoDelivery(); scheduledGroupHandler.postDelayed(this, 3000L); } };'''
if old in s:s=s.replace(old,new,1)
elif 'runTelegramAlbumAutoDelivery();' not in s:
 raise SystemExit('ERRO v2.1.36: relógio do motor de avisos não encontrado')

if 'private void runTelegramAlbumAutoDelivery()' not in s:
 pos=s.rfind('}')
 helper=r'''
    private void runTelegramAlbumAutoDelivery() {
        if (!TelegramAlbumWhatsAppBridge.pending(this)) return;
        java.util.List<String> targets = ScheduledGroupBroadcast.groups(this);
        java.util.List<java.io.File> images = TelegramAlbumWhatsAppBridge.files(this);
        if (targets.isEmpty()) { TelegramAlbumWhatsAppBridge.status(this,"Álbum capturado, mas nenhum grupo está selecionado em Avisos Automáticos"); return; }
        if (images.isEmpty()) { TelegramAlbumWhatsAppBridge.status(this,"Álbum sem imagens válidas para envio"); return; }
        java.util.Set<String> done = TelegramAlbumWhatsAppBridge.doneGroups(this);
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            for (String wanted : targets) {
                String key = wanted.trim().toLowerCase(java.util.Locale.ROOT);
                if (key.isEmpty() || done.contains(key)) continue;
                for (StatusBarNotification item : active) {
                    if (item == null || !PACKAGE_NAME.equals(item.getPackageName())) continue;
                    Notification candidate = item.getNotification();
                    if (candidate == null || !isGroupNotification(candidate) || candidate.extras == null) continue;
                    String conv = firstNonEmpty(candidate.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),candidate.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),candidate.extras.getCharSequence(Notification.EXTRA_TITLE));
                    if (!wanted.equalsIgnoreCase(conv.trim())) continue;
                    if (TelegramAlbumWhatsAppBridge.sendAlbum(this,candidate,images)) {
                        done.add(key); TelegramAlbumWhatsAppBridge.saveDone(this,done);
                        TelegramAlbumWhatsAppBridge.status(this,"Álbum automático: enviado para "+done.size()+" de "+targets.size()+" grupos");
                        if (done.size() >= targets.size()) TelegramAlbumWhatsAppBridge.finish(this,targets.size(),images.size());
                        return;
                    }
                }
            }
            TelegramAlbumWhatsAppBridge.status(this,"Álbum pronto: aguardando notificação ativa dos grupos selecionados");
        } catch (Throwable e) { TelegramAlbumWhatsAppBridge.status(this,"Falha temporária no motor automático de imagens"); }
    }
'''
 s=s[:pos]+helper+s[pos:]
svc.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.36",10,MUTED,false));',D,count=1);dash.write_text(D,encoding='utf-8')
for p,t in [(java/'TelegramAlbumWhatsAppBridge.java','telegram_auto_album_pending'),(tg,'TelegramAlbumWhatsAppBridge.markChanged(this)'),(svc,'runTelegramAlbumAutoDelivery()'),(svc,'ScheduledGroupBroadcast.groups(this)'),(manifest,'.telegramfiles'),(gradle,"versionName '2.1.36'")]:
 if t not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.36: requisito ausente '+t)
print('v2.1.36: álbum recebido pela API do Telegram entra automaticamente na fila e é enviado imediatamente aos grupos selecionados em Avisos Automáticos, somente pelo WhatsApp Business')