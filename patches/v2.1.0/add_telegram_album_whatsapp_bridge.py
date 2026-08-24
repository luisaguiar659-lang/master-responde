from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'

# v2.1.36: último álbum do Motor Telegram API -> @infor banners no WhatsApp Business.
(java/'TelegramAlbumWhatsAppBridge.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.content.*;import android.net.Uri;import androidx.core.content.FileProvider;import org.json.JSONArray;import java.io.File;import java.util.*;
public final class TelegramAlbumWhatsAppBridge{
 private static final String P="master_responde";private TelegramAlbumWhatsAppBridge(){}
 public static boolean isBannerRequest(String s){if(s==null)return false;String x=s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");return x.equals("@infor banners")||x.equals("banners")||x.equals("banner");}
 public static boolean sendLatestAlbum(Context c,Notification n){
  if(c==null||n==null)return false;SharedPreferences p=c.getSharedPreferences(P,0);List<File> files=files(p);if(files.isEmpty()){status(p,"Nenhum álbum Telegram disponível para @infor banners");return false;}
  Notification.Action a=mediaAction(n);if(a==null||a.actionIntent==null){status(p,"WhatsApp Business não expôs envio de imagem pela notificação");return false;}RemoteInput[] inputs=a.getRemoteInputs();if(inputs==null){status(p,"WhatsApp Business sem RemoteInput de mídia");return false;}
  int sent=0;for(File f:files){if(!f.exists())continue;try{Uri u=FileProvider.getUriForFile(c,c.getPackageName()+".telegramfiles",f);Intent fill=new Intent();fill.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);boolean attached=false;for(RemoteInput r:inputs){if(r==null||r.getResultKey()==null)continue;if(r.isDataOnly()||allowsImage(r)){Map<String,Uri>d=new HashMap<>();d.put("image/jpeg",u);RemoteInput.addDataResultToIntent(r,fill,d);attached=true;break;}}if(!attached){status(p,"A ação Responder do WhatsApp Business não aceita imagem nesta versão");return false;}a.actionIntent.send(c,0,fill);sent++;try{Thread.sleep(650L);}catch(InterruptedException ignored){}}catch(Throwable e){status(p,"Falha ao enviar imagem do Telegram: "+(e.getMessage()==null?"":e.getMessage()));return false;}}
  if(sent>0){p.edit().putInt("telegram_whatsapp_last_sent_count",sent).putLong("telegram_whatsapp_last_sent_at",System.currentTimeMillis()).putString("telegram_whatsapp_last_status","Álbum Telegram enviado: "+sent+" imagens").apply();return true;}return false;
 }
 private static Notification.Action mediaAction(Notification n){if(n.actions==null)return null;Notification.Action fallback=null;for(Notification.Action a:n.actions){if(a==null||a.actionIntent==null)continue;RemoteInput[] rs=a.getRemoteInputs();if(rs==null||rs.length==0)continue;if(fallback==null)fallback=a;for(RemoteInput r:rs)if(r!=null&&(r.isDataOnly()||allowsImage(r)))return a;}return fallback;}
 private static boolean allowsImage(RemoteInput r){try{Set<String>t=r.getAllowedDataTypes();if(t!=null)for(String x:t)if(x!=null&&x.toLowerCase(Locale.ROOT).startsWith("image/"))return true;}catch(Throwable ignored){}return false;}
 private static List<File> files(SharedPreferences p){List<File>o=new ArrayList<>();try{JSONArray a=new JSONArray(p.getString("telegram_api_album_paths","[]"));for(int i=0;i<a.length();i++){String x=a.optString(i,"").trim();if(!x.isEmpty())o.add(new File(x));}}catch(Throwable ignored){}return o;}
 private static void status(SharedPreferences p,String s){p.edit().putString("telegram_whatsapp_last_status",s).apply();}
}''',encoding='utf-8')

xml=app/'src/main/res/xml';xml.mkdir(parents=True,exist_ok=True);(xml/'telegram_file_paths.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n <files-path name="telegram_album" path="telegram_api_album/" />\n</paths>\n''',encoding='utf-8')
m=manifest.read_text(encoding='utf-8');provider='''\n        <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.telegramfiles" android:exported="false" android:grantUriPermissions="true">\n            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/telegram_file_paths"/>\n        </provider>\n''';
if '.telegramfiles' not in m:m=m.replace('</application>',provider+'</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
if 'androidx.core:core:' not in g and 'androidx.core:core-' not in g:
 g=g.replace('dependencies {',"dependencies {\n    implementation 'androidx.core:core:1.15.0'",1) if 'dependencies {' in g else g+"\n\ndependencies {\n    implementation 'androidx.core:core:1.15.0'\n}\n"
g=re.sub(r'versionCode\s+\d+','versionCode 107',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.36'",g,count=1);gradle.write_text(g,encoding='utf-8')

s=svc.read_text(encoding='utf-8')
if 'TelegramAlbumWhatsAppBridge.sendLatestAlbum' not in s:
 needles=['''            if (message == null) message = "";\n            message = message.trim();''','''        if (message == null) message = "";\n        message = message.trim();'''];done=False
 for needle in needles:
  if needle in s:
   hook=needle+'''\n            try {\n                if (isGroupNotification(n) && TelegramAlbumWhatsAppBridge.isBannerRequest(message)) {\n                    if (TelegramAlbumWhatsAppBridge.sendLatestAlbum(this,n)) {\n                        prefs().edit().putString("accessibility_last_status","@infor banners: álbum Telegram enviado ao WhatsApp Business").apply();\n                        return;\n                    }\n                }\n            } catch (Throwable ignored) {}''';s=s.replace(needle,hook,1);done=True;break
 if not done:raise SystemExit('ERRO v2.1.36: ponto de leitura da mensagem WhatsApp não encontrado')
svc.write_text(s,encoding='utf-8')
D=dash.read_text(encoding='utf-8');D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.36",10,MUTED,false));',D,count=1);dash.write_text(D,encoding='utf-8')
for p,t in [(java/'TelegramAlbumWhatsAppBridge.java','sendLatestAlbum'),(java/'TelegramAlbumWhatsAppBridge.java','telegram_whatsapp_last_status'),(svc,'TelegramAlbumWhatsAppBridge.isBannerRequest'),(manifest,'.telegramfiles'),(xml/'telegram_file_paths.xml','telegram_api_album/'),(gradle,"versionName '2.1.36'")]:
 if t not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.36: requisito ausente '+t)
print('v2.1.36: @infor banners ligado ao último álbum capturado pelo Motor Telegram API, usando somente WhatsApp Business e fallback seguro quando a notificação não aceita mídia')