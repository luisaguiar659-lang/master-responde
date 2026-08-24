from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
tg=java/'TelegramApiAlbumService.java'
act=java/'TelegramApiSettingsActivity.java'
svc=java/'WhatsAppBusinessCaptureService.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'
manifest=app/'src/main/AndroidManifest.xml'

# v2.1.39 - correção consolidada do Motor Telegram API.
# Objetivos:
# 1. remover Detector Telegram antigo do menu/fluxo ativo;
# 2. botão do Motor API ligar/desligar de verdade;
# 3. polling Telegram com heartbeat e diagnóstico;
# 4. destinos próprios do Motor Telegram API;
# 5. recebeu álbum -> envia automaticamente, sem gatilho, sem horário e sem depender de Avisos Automáticos.

# -----------------------------------------------------------------------------
# Store independente dos destinos do Motor Telegram API
# -----------------------------------------------------------------------------
(java/'TelegramAutoDeliveryStore.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.util.*;
public final class TelegramAutoDeliveryStore{
 private static final String P="master_responde";private TelegramAutoDeliveryStore(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static List<String> groups(Context c){ArrayList<String> out=new ArrayList<>();String raw=p(c).getString("telegram_api_destination_groups","");for(String x:raw.split("\\n")){String g=x.trim();if(!g.isEmpty()&&!out.contains(g))out.add(g);}return out;}
 public static String raw(Context c){return p(c).getString("telegram_api_destination_groups","");}
}''',encoding='utf-8')

# -----------------------------------------------------------------------------
# Motor Telegram API: heartbeat, diagnóstico e fechamento do álbum
# -----------------------------------------------------------------------------
s=tg.read_text(encoding='utf-8')

# onStartCommand: estado real + heartbeat
s=re.sub(
 r'prefs\(\)\.edit\(\)\.putBoolean\("telegram_api_motor_on",true\)(?:\.putLong\("telegram_api_heartbeat",System\.currentTimeMillis\(\)\))?\.putString\("telegram_api_status","Motor Telegram API ligado"\)\.apply\(\);',
 'prefs().edit().putBoolean("telegram_api_motor_on",true).putLong("telegram_api_heartbeat",System.currentTimeMillis()).putString("telegram_api_status","Motor Telegram API ligado").apply();',
 s,count=1)

# onDestroy deve sempre refletir desligado real
s=re.sub(r'public void onDestroy\(\)\{[^}]*super\.onDestroy\(\);\}',
'''public void onDestroy(){running=false;prefs().edit().putBoolean("telegram_api_motor_on",false).putLong("telegram_api_heartbeat",0L).putString("telegram_api_status","Motor Telegram API desligado").apply();super.onDestroy();}''',s,count=1)

# heartbeat por ciclo
if 'putLong("telegram_api_heartbeat",System.currentTimeMillis())' not in s[s.find('private void loop()'):]:
 s=s.replace('while(running){\n            try{','while(running){\n            try{\n                prefs().edit().putLong("telegram_api_heartbeat",System.currentTimeMillis()).apply();',1)

# diagnóstico após getUpdates
needle='JSONObject root=getJson(u,30000);'
if needle in s and 'telegram_api_last_poll_at' not in s:
 s=s.replace(needle,needle+'\n                prefs().edit().putLong("telegram_api_last_poll_at",System.currentTimeMillis()).putLong("telegram_api_heartbeat",System.currentTimeMillis()).apply();',1)

# resposta não-ok: registra código/descrição do Telegram, inclusive 409
old='if(root==null||!root.optBoolean("ok",false)){status(root==null?"Falha de conexão com Telegram":"Telegram recusou o token/getUpdates");sleep(3000);continue;}'
new='''if(root==null||!root.optBoolean("ok",false)){String desc=root==null?"Falha de conexão com Telegram":root.optString("description","Telegram recusou getUpdates");int ec=root==null?0:root.optInt("error_code",0);status((ec>0?"Telegram "+ec+": ":"")+desc);prefs().edit().putInt("telegram_api_last_error_code",ec).putString("telegram_api_last_error",desc).apply();sleep(3000);continue;}'''
if old in s:s=s.replace(old,new,1)

# registra quantidade/update id
if 'telegram_api_last_batch_count' not in s:
 s=s.replace('JSONArray arr=root.optJSONArray("result");','JSONArray arr=root.optJSONArray("result");\n                prefs().edit().putInt("telegram_api_last_batch_count",arr==null?0:arr.length()).apply();',1)
 s=s.replace('if(max!=offset)prefs().edit().putLong("telegram_api_offset",max).apply();','if(max!=offset)prefs().edit().putLong("telegram_api_offset",max).putLong("telegram_api_last_update_id",max-1L).apply();',1)

# marca novo álbum para entrega independente
if 'telegram_auto_album_status' not in s:
 marker='.putString("telegram_api_last_caption",caption).putString("telegram_api_status",paths.length()>1?"Álbum capturado: "+paths.length()+" imagens":"Imagem capturada pela API")\n                    .apply();'
 if marker in s:
  s=s.replace(marker,marker+'\n            TelegramAlbumWhatsAppBridge.markChanged(this);\n            prefs().edit().putString("telegram_auto_album_status","Recebendo álbum do Telegram...").apply();',1)
elif 'TelegramAlbumWhatsAppBridge.markChanged(this);' not in s:
 # fallback menos frágil
 s=s.replace('                    .apply();\n        }catch(Throwable e){status("Falha ao montar álbum: "+safe(e.getMessage()));}',
'''                    .apply();\n            TelegramAlbumWhatsAppBridge.markChanged(this);\n            prefs().edit().putString("telegram_auto_album_status","Recebendo álbum do Telegram...").apply();\n        }catch(Throwable e){status("Falha ao montar álbum: "+safe(e.getMessage()));}''',1)

tg.write_text(s,encoding='utf-8')

# -----------------------------------------------------------------------------
# Tela do Motor Telegram API: sobrescreve de forma consolidada para evitar
# resíduos das versões anteriores e garantir todos os controles.
# -----------------------------------------------------------------------------
act.write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.text.*;import java.util.*;

public class TelegramApiSettingsActivity extends Activity{
 SharedPreferences p;EditText token,source,destinations;TextView state,lastChat,count,caption,delivery,diag;LinearLayout previews;Button motor;
 final Handler h=new Handler(Looper.getMainLooper());final Runnable refresh=new Runnable(){public void run(){update();h.postDelayed(this,1000);}};
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
 protected void onResume(){super.onResume();ensureMotorAlive();h.removeCallbacks(refresh);h.post(refresh);}protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 TextView t(String s,int z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.WHITE);if(b)v.setTypeface(null,Typeface.BOLD);v.setPadding(8,10,8,10);return v;}
 TextView val(){TextView v=t("—",14,false);v.setBackgroundColor(Color.rgb(5,18,25));v.setPadding(16,14,16,14);return v;}
 EditText f(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);return e;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));
  r.addView(t("MOTOR TELEGRAM API",24,true));TextView sub=t("Recebe imagens e álbuns diretamente pela API do Telegram e envia automaticamente para os grupos configurados, sem gatilho e sem depender de Avisos Automáticos.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);
  r.addView(t("TOKEN DO BOT",13,true));token=f("Token criado no BotFather");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setText(p.getString("telegram_api_token",""));r.addView(token);
  r.addView(t("CHAT DE ORIGEM (ID)",13,true));source=f("Deixe vazio no primeiro teste");source.setText(p.getString("telegram_api_source_chat_id",""));r.addView(source);
  r.addView(t("GRUPOS DE DESTINO - WHATSAPP BUSINESS",13,true));r.addView(t("Um nome EXATO por linha. Assim que o álbum chegar do Telegram, o envio começa automaticamente.",11,false));destinations=f("Grupo 1\nGrupo 2\nGrupo 3");destinations.setMinLines(5);destinations.setGravity(Gravity.TOP);destinations.setText(p.getString("telegram_api_destination_groups",""));r.addView(destinations);
  Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->save());r.addView(save);
  motor=new Button(this);motor.setOnClickListener(v->toggle());r.addView(motor);
  Button use=new Button(this);use.setText("USAR ÚLTIMO CHAT DETECTADO");use.setOnClickListener(v->{String id=p.getString("telegram_api_last_chat_id","");if(id.isEmpty()){Toast.makeText(this,"Nenhum chat detectado ainda",Toast.LENGTH_SHORT).show();return;}source.setText(id);p.edit().putString("telegram_api_source_chat_id",id).apply();Toast.makeText(this,"Chat de origem definido",Toast.LENGTH_SHORT).show();});r.addView(use);
  r.addView(t("STATUS DO MOTOR",13,true));state=val();r.addView(state);r.addView(t("ENVIO AUTOMÁTICO",13,true));delivery=val();r.addView(delivery);r.addView(t("DIAGNÓSTICO DA API",13,true));diag=val();r.addView(diag);
  r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));lastChat=val();r.addView(lastChat);r.addView(t("LEGENDA",13,true));caption=val();r.addView(caption);r.addView(t("IMAGENS NO ÁLBUM",13,true));count=val();r.addView(count);r.addView(t("PRÉVIAS DA API",13,true));previews=new LinearLayout(this);previews.setOrientation(LinearLayout.VERTICAL);r.addView(previews,new LinearLayout.LayoutParams(-1,-2));
  return sc;}
 void save(){p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).putString("telegram_api_destination_groups",destinations.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();}
 void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}save();p.edit().putBoolean("telegram_api_motor_on",true).putString("telegram_api_status","Iniciando Motor Telegram API...").apply();try{Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable e){p.edit().putBoolean("telegram_api_motor_on",false).putString("telegram_api_status","Falha ao iniciar Motor Telegram API").apply();}}else{p.edit().putBoolean("telegram_api_motor_on",false).putLong("telegram_api_heartbeat",0L).putString("telegram_api_status","Motor Telegram API desligado").apply();try{stopService(new Intent(this,TelegramApiAlbumService.class));}catch(Throwable ignored){}}update();}
 void ensureMotorAlive(){if(!p.getBoolean("telegram_api_motor_on",false))return;long hb=p.getLong("telegram_api_heartbeat",0L);if(hb>0&&System.currentTimeMillis()-hb<45000L)return;try{Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);p.edit().putString("telegram_api_status","Reiniciando Motor Telegram API...").apply();}catch(Throwable e){p.edit().putBoolean("telegram_api_motor_on",false).putString("telegram_api_status","Falha ao reiniciar Motor Telegram API").apply();}}
 void update(){boolean on=p.getBoolean("telegram_api_motor_on",false);motor.setText(on?"MOTOR API LIGADO":"MOTOR API DESLIGADO");motor.setTextColor(Color.WHITE);motor.setBackgroundColor(on?Color.rgb(20,150,70):Color.rgb(185,45,45));state.setText(p.getString("telegram_api_status","Aguardando configuração"));delivery.setText(p.getString("telegram_auto_album_status","Aguardando novo conteúdo do Telegram"));long poll=p.getLong("telegram_api_last_poll_at",0L);long uid=p.getLong("telegram_api_last_update_id",-1L);int batch=p.getInt("telegram_api_last_batch_count",0);int err=p.getInt("telegram_api_last_error_code",0);String er=p.getString("telegram_api_last_error","");String when=poll>0?new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date(poll)):"—";diag.setText("Última consulta: "+when+"\nUpdates: "+batch+"\nÚltimo update_id: "+(uid>=0?uid:"—")+(err>0?"\nErro "+err+": "+er:""));String id=p.getString("telegram_api_last_chat_id","");String title=p.getString("telegram_api_last_chat_title","");lastChat.setText((title.isEmpty()?"—":title)+(id.isEmpty()?"":"\nID: "+id));caption.setText(p.getString("telegram_api_last_caption","—"));int n=p.getInt("telegram_api_album_count",0);count.setText(String.valueOf(n));previews.removeAllViews();try{JSONArray a=new JSONArray(p.getString("telegram_api_album_paths","[]"));for(int i=0;i<a.length();i++){File f=new File(a.optString(i));if(!f.exists())continue;Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null)continue;ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);iv.setImageBitmap(b);iv.setPadding(0,6,0,12);previews.addView(iv,new LinearLayout.LayoutParams(-1,-2));}}catch(Throwable ignored){}if(on){long hb=p.getLong("telegram_api_heartbeat",0L);if(hb<=0||System.currentTimeMillis()-hb>45000L)ensureMotorAlive();}}
}''',encoding='utf-8')

# -----------------------------------------------------------------------------
# Entrega automática independente do Motor de Avisos
# -----------------------------------------------------------------------------
s=svc.read_text(encoding='utf-8')

# Telegram por notificação deixa de ser processado pelo detector antigo.
s=re.sub(r'if\s*\(isTelegramPackage\(sourcePackage\)\)\s*\{\s*captureTelegramNotification\(sbn\);\s*return;\s*\}', 'if (isTelegramPackage(sourcePackage)) return;', s, count=1, flags=re.S)

# A função de entrega, se já existir, passa a usar exclusivamente destinos próprios.
s=s.replace('java.util.List<String> targets = ScheduledGroupBroadcast.groups(this);','java.util.List<String> targets = TelegramAutoDeliveryStore.groups(this);')
s=s.replace('Álbum capturado, mas nenhum grupo está selecionado em Avisos Automáticos','Álbum capturado, mas nenhum grupo de destino está configurado no Motor Telegram API')

# Se não existir ainda, cria a função completa.
if 'private void runTelegramAlbumAutoDelivery()' not in s:
 pos=s.rfind('}')
 helper=r'''
    private void runTelegramAlbumAutoDelivery() {
        if (!TelegramAlbumWhatsAppBridge.pending(this)) return;
        java.util.List<String> targets = TelegramAutoDeliveryStore.groups(this);
        java.util.List<java.io.File> images = TelegramAlbumWhatsAppBridge.files(this);
        if (targets.isEmpty()) { TelegramAlbumWhatsAppBridge.status(this,"Álbum capturado, mas nenhum grupo de destino está configurado no Motor Telegram API"); return; }
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
                        done.add(key);TelegramAlbumWhatsAppBridge.saveDone(this,done);
                        TelegramAlbumWhatsAppBridge.status(this,"Enviando álbum: "+done.size()+" de "+targets.size()+" grupos concluídos");
                        if(done.size()>=targets.size())TelegramAlbumWhatsAppBridge.finish(this,targets.size(),images.size());
                        return;
                    }
                }
            }
            TelegramAlbumWhatsAppBridge.status(this,"Álbum pronto: aguardando acesso aos grupos configurados");
        } catch(Throwable e){TelegramAlbumWhatsAppBridge.status(this,"Falha temporária no envio automático do álbum");}
    }
'''
 s=s[:pos]+helper+s[pos:]

# Remove acoplamento do tick de avisos, se existir.
s=s.replace('runScheduledGroupBroadcastIfDue(); runTelegramAlbumAutoDelivery(); scheduledGroupHandler.postDelayed(this, 3000L);','runScheduledGroupBroadcastIfDue(); scheduledGroupHandler.postDelayed(this, 3000L);')

# Loop próprio de entrega automática.
if 'telegramAutoDeliveryHandler' not in s:
 pos=s.rfind('}')
 helper=r'''
    private final android.os.Handler telegramAutoDeliveryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable telegramAutoDeliveryTick = new Runnable(){@Override public void run(){runTelegramAlbumAutoDelivery();telegramAutoDeliveryHandler.postDelayed(this,1500L);}};
    private void startTelegramAutoDeliveryLoop(){telegramAutoDeliveryHandler.removeCallbacks(telegramAutoDeliveryTick);telegramAutoDeliveryHandler.postDelayed(telegramAutoDeliveryTick,1000L);}
    private void stopTelegramAutoDeliveryLoop(){telegramAutoDeliveryHandler.removeCallbacks(telegramAutoDeliveryTick);}
'''
 s=s[:pos]+helper+s[pos:]

# Liga/desliga o loop com o NotificationListener, independente do scheduler de avisos.
if 'startTelegramAutoDeliveryLoop();' not in s[s.find('onListenerConnected'):s.find('onListenerConnected')+500]:
 s=s.replace('super.onListenerConnected();','super.onListenerConnected();\n        startTelegramAutoDeliveryLoop();',1)
if 'stopTelegramAutoDeliveryLoop();' not in s[s.find('onListenerDisconnected'):s.find('onListenerDisconnected')+500]:
 s=s.replace('super.onListenerDisconnected();','stopTelegramAutoDeliveryLoop();\n        super.onListenerDisconnected();',1)

svc.write_text(s,encoding='utf-8')

# -----------------------------------------------------------------------------
# Remove Detector Telegram antigo do menu, independentemente do ícone/spacing.
# -----------------------------------------------------------------------------
D=dash.read_text(encoding='utf-8')
D='\n'.join(line for line in D.splitlines() if not ('Detector Telegram' in line and 'sideItem' in line))+'\n'
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.39",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

# versão
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 110',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.39'",G,count=1)
gradle.write_text(G,encoding='utf-8')

# validações fortes: se falhar, não deve produzir APK fingindo que está tudo certo.
checks=[
 (java/'TelegramAutoDeliveryStore.java','telegram_api_destination_groups'),
 (act,'GRUPOS DE DESTINO - WHATSAPP BUSINESS'),
 (act,'DIAGNÓSTICO DA API'),
 (act,'putBoolean("telegram_api_motor_on",false)'),
 (tg,'telegram_api_last_poll_at'),
 (tg,'TelegramAlbumWhatsAppBridge.markChanged(this)'),
 (svc,'TelegramAutoDeliveryStore.groups(this)'),
 (svc,'telegramAutoDeliveryHandler'),
 (svc,'runTelegramAlbumAutoDelivery()'),
 (gradle,"versionName '2.1.39'")
]
for pth,mark in checks:
 if mark not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.39: requisito ausente '+str(pth)+' :: '+mark)
if 'Detector Telegram' in dash.read_text(encoding='utf-8') and 'sideItem' in '\n'.join(x for x in dash.read_text(encoding='utf-8').splitlines() if 'Detector Telegram' in x):
 raise SystemExit('ERRO v2.1.39: Detector Telegram ainda aparece no menu')
print('v2.1.39 OK: Motor Telegram API unificado, polling diagnosticável, botão corrigido, destinos próprios e envio automático independente; Detector Telegram removido do menu')
