from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
manifest=app/'src/main/AndroidManifest.xml'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

(java/'TelegramApiAlbumService.java').write_text(r'''package com.masterresponde.app;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TelegramApiAlbumService extends Service {
    private static final String PREFS="master_responde";
    private static final String CHANNEL="telegram_api_motor";
    private volatile boolean running=false;
    private Thread worker;

    public void onCreate(){super.onCreate();createChannel();}
    public int onStartCommand(Intent i,int flags,int id){
        running=true;
        startForeground(381,new Notification.Builder(this,CHANNEL).setContentTitle("MASTER RESPONDE").setContentText("Motor Telegram API ativo").setSmallIcon(android.R.drawable.stat_notify_sync).build());
        if(worker==null||!worker.isAlive()){worker=new Thread(this::loop,"TelegramApiMotor");worker.start();}
        prefs().edit().putBoolean("telegram_api_motor_on",true).putString("telegram_api_status","Motor Telegram API ligado").apply();
        return START_STICKY;
    }
    public void onDestroy(){running=false;prefs().edit().putBoolean("telegram_api_motor_on",false).putString("telegram_api_status","Motor Telegram API desligado").apply();super.onDestroy();}
    public IBinder onBind(Intent i){return null;}

    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);n.createNotificationChannel(new NotificationChannel(CHANNEL,"Motor Telegram API",NotificationManager.IMPORTANCE_LOW));}}

    private void loop(){
        while(running){
            try{
                String token=prefs().getString("telegram_api_token","").trim();
                if(token.isEmpty()){status("Informe o token do bot");sleep(3000);continue;}
                long offset=prefs().getLong("telegram_api_offset",0L);
                String allowed=URLEncoder.encode("[\"message\",\"channel_post\"]","UTF-8");
                String u="https://api.telegram.org/bot"+token+"/getUpdates?timeout=20&limit=100&allowed_updates="+allowed+(offset>0?"&offset="+offset:"");
                JSONObject root=getJson(u,30000);
                if(root==null||!root.optBoolean("ok",false)){status(root==null?"Falha de conexão com Telegram":"Telegram recusou o token/getUpdates");sleep(3000);continue;}
                JSONArray arr=root.optJSONArray("result");
                if(arr==null){sleep(500);continue;}
                long max=offset;
                for(int x=0;x<arr.length();x++){
                    JSONObject up=arr.optJSONObject(x);if(up==null)continue;
                    long uid=up.optLong("update_id",0L);if(uid>=max)max=uid+1;
                    JSONObject m=up.optJSONObject("channel_post");if(m==null)m=up.optJSONObject("message");if(m==null)continue;
                    handleMessage(token,m);
                }
                if(max!=offset)prefs().edit().putLong("telegram_api_offset",max).apply();
            }catch(Throwable e){status("Erro Motor Telegram API: "+safe(e.getMessage()));sleep(2500);}
        }
    }

    private void handleMessage(String token,JSONObject m){
        try{
            JSONObject chat=m.optJSONObject("chat");if(chat==null)return;
            String chatId=String.valueOf(chat.optLong("id",0L));
            String title=first(chat.optString("title",""),chat.optString("username",""),chat.optString("first_name",""),chatId);
            SharedPreferences p=prefs();
            p.edit().putString("telegram_api_last_chat_id",chatId).putString("telegram_api_last_chat_title",title).apply();
            String wanted=p.getString("telegram_api_source_chat_id","").trim();
            if(!wanted.isEmpty()&&!wanted.equals(chatId))return;

            JSONArray photos=m.optJSONArray("photo");
            if(photos==null||photos.length()==0){
                String txt=first(m.optString("text",""),m.optString("caption",""),"Mensagem sem mídia");
                p.edit().putString("telegram_api_last_caption",txt).putString("telegram_api_status","Mensagem recebida de "+title).apply();
                return;
            }

            JSONObject ph=photos.optJSONObject(photos.length()-1);if(ph==null)return;
            String fileId=ph.optString("file_id","");if(fileId.isEmpty())return;
            String group=m.optString("media_group_id","");
            String messageId=String.valueOf(m.optLong("message_id",System.currentTimeMillis()));
            String caption=m.optString("caption","");
            String path=downloadPhoto(token,fileId,group,messageId);if(path.isEmpty())return;
            saveAlbumItem(group,path,chatId,title,caption);
        }catch(Throwable e){status("Falha ao processar mídia Telegram: "+safe(e.getMessage()));}
    }

    private String downloadPhoto(String token,String fileId,String group,String messageId){
        try{
            String q="https://api.telegram.org/bot"+token+"/getFile?file_id="+URLEncoder.encode(fileId,"UTF-8");
            JSONObject r=getJson(q,15000);if(r==null||!r.optBoolean("ok",false))return "";
            JSONObject f=r.optJSONObject("result");if(f==null)return "";String fp=f.optString("file_path","");if(fp.isEmpty())return "";
            URL url=new URL("https://api.telegram.org/file/bot"+token+"/"+fp);
            HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.connect();
            if(c.getResponseCode()/100!=2){c.disconnect();return "";}
            File dir=new File(getFilesDir(),"telegram_api_album");if(!dir.exists())dir.mkdirs();
            String safeGroup=group==null||group.isEmpty()?"single":group.replaceAll("[^A-Za-z0-9_-]","_");
            File out=new File(dir,"tg_"+safeGroup+"_"+messageId+".jpg");
            try(InputStream in=c.getInputStream();FileOutputStream os=new FileOutputStream(out,false)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)os.write(b,0,n);os.flush();}
            c.disconnect();return out.getAbsolutePath();
        }catch(Throwable e){return "";}
    }

    private void saveAlbumItem(String group,String path,String chatId,String title,String caption){
        try{
            SharedPreferences p=prefs();long now=System.currentTimeMillis();
            String oldGroup=p.getString("telegram_api_album_group","");long oldAt=p.getLong("telegram_api_album_last_at",0L);
            boolean same=!group.isEmpty()&&group.equals(oldGroup)&&now-oldAt<15000L;
            JSONArray paths;
            try{paths=same?new JSONArray(p.getString("telegram_api_album_paths","[]")):new JSONArray();}catch(Throwable e){paths=new JSONArray();}
            boolean exists=false;for(int i=0;i<paths.length();i++)if(path.equals(paths.optString(i))){exists=true;break;}
            if(!exists)paths.put(path);
            String effectiveGroup=group.isEmpty()?"single_"+now:group;
            p.edit().putString("telegram_api_album_group",effectiveGroup).putLong("telegram_api_album_last_at",now)
                    .putString("telegram_api_album_paths",paths.toString()).putInt("telegram_api_album_count",paths.length())
                    .putString("telegram_api_last_chat_id",chatId).putString("telegram_api_last_chat_title",title)
                    .putString("telegram_api_last_caption",caption).putString("telegram_api_status",paths.length()>1?"Álbum capturado: "+paths.length()+" imagens":"Imagem capturada pela API")
                    .apply();
        }catch(Throwable e){status("Falha ao montar álbum: "+safe(e.getMessage()));}
    }

    private JSONObject getJson(String u,int timeout)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(timeout);c.setRequestMethod("GET");
        int code=c.getResponseCode();InputStream in=code/100==2?c.getInputStream():c.getErrorStream();if(in==null){c.disconnect();return null;}
        StringBuilder sb=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(in))){String line;while((line=br.readLine())!=null)sb.append(line);}c.disconnect();
        try{return new JSONObject(sb.toString());}catch(Throwable e){return null;}
    }
    private void status(String s){prefs().edit().putString("telegram_api_status",s).apply();}
    private static String first(String...v){if(v!=null)for(String s:v)if(s!=null&&!s.trim().isEmpty())return s.trim();return "";}
    private static String safe(String s){return s==null?"":s;}
    private static void sleep(long ms){try{Thread.sleep(ms);}catch(InterruptedException ignored){}}
}
''',encoding='utf-8')

(java/'TelegramApiSettingsActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.util.*;

public class TelegramApiSettingsActivity extends Activity{
 SharedPreferences p;EditText token,source;TextView state,lastChat,count,caption;LinearLayout previews;Button motor;
 final Handler h=new Handler(Looper.getMainLooper());final Runnable refresh=new Runnable(){public void run(){update();h.postDelayed(this,1000);}};
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
 protected void onResume(){super.onResume();h.removeCallbacks(refresh);h.post(refresh);}protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 TextView t(String s,int z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.WHITE);if(b)v.setTypeface(null,Typeface.BOLD);v.setPadding(8,10,8,10);return v;}
 TextView val(){TextView v=t("—",14,false);v.setBackgroundColor(Color.rgb(5,18,25));v.setPadding(16,14,16,14);return v;}
 EditText f(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);return e;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));
  r.addView(t("MOTOR TELEGRAM API",24,true));TextView sub=t("Captura mensagens e álbuns diretamente pela API do Telegram, sem depender da notificação do Android.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);
  r.addView(t("TOKEN DO BOT",13,true));token=f("Token criado no BotFather");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setText(p.getString("telegram_api_token",""));r.addView(token);
  r.addView(t("CHAT DE ORIGEM (ID)",13,true));source=f("Deixe vazio no primeiro teste");source.setText(p.getString("telegram_api_source_chat_id",""));r.addView(source);
  Button save=new Button(this);save.setText("SALVAR CONFIGURAÇÃO");save.setOnClickListener(v->{p.edit().putString("telegram_api_token",token.getText().toString().trim()).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Toast.makeText(this,"Configuração salva",Toast.LENGTH_SHORT).show();});r.addView(save);
  motor=new Button(this);motor.setOnClickListener(v->toggle());r.addView(motor);
  Button use=new Button(this);use.setText("USAR ÚLTIMO CHAT DETECTADO");use.setOnClickListener(v->{String id=p.getString("telegram_api_last_chat_id","");if(id.isEmpty()){Toast.makeText(this,"Nenhum chat detectado ainda",Toast.LENGTH_SHORT).show();return;}source.setText(id);p.edit().putString("telegram_api_source_chat_id",id).apply();Toast.makeText(this,"Chat de origem definido",Toast.LENGTH_SHORT).show();});r.addView(use);
  r.addView(t("STATUS",13,true));state=val();r.addView(state);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));lastChat=val();r.addView(lastChat);r.addView(t("LEGENDA",13,true));caption=val();r.addView(caption);r.addView(t("IMAGENS NO ÁLBUM",13,true));count=val();r.addView(count);r.addView(t("PRÉVIAS DA API",13,true));previews=new LinearLayout(this);previews.setOrientation(LinearLayout.VERTICAL);r.addView(previews,new LinearLayout.LayoutParams(-1,-2));
  r.addView(t("Primeiro teste: crie um bot no BotFather, adicione-o ao grupo/canal de origem e ligue o motor. Com CHAT DE ORIGEM vazio, envie uma mensagem ou álbum. O ID e o nome aparecerão acima; depois use USAR ÚLTIMO CHAT DETECTADO.",12,false));return sc;}
 void toggle(){boolean on=p.getBoolean("telegram_api_motor_on",false);if(!on){String tk=token.getText().toString().trim();if(tk.isEmpty()){Toast.makeText(this,"Informe o token do bot",Toast.LENGTH_LONG).show();return;}p.edit().putString("telegram_api_token",tk).putString("telegram_api_source_chat_id",source.getText().toString().trim()).apply();Intent i=new Intent(this,TelegramApiAlbumService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(new Intent(this,TelegramApiAlbumService.class));h.postDelayed(this::update,400);}
 void update(){boolean on=p.getBoolean("telegram_api_motor_on",false);motor.setText(on?"MOTOR API LIGADO":"MOTOR API DESLIGADO");motor.setTextColor(Color.WHITE);motor.setBackgroundColor(on?Color.rgb(20,150,70):Color.rgb(185,45,45));state.setText(p.getString("telegram_api_status","Aguardando configuração"));String id=p.getString("telegram_api_last_chat_id","");String title=p.getString("telegram_api_last_chat_title","");lastChat.setText((title.isEmpty()?"—":title)+(id.isEmpty()?"":"\nID: "+id));caption.setText(p.getString("telegram_api_last_caption","—"));int n=p.getInt("telegram_api_album_count",0);count.setText(String.valueOf(n));previews.removeAllViews();try{JSONArray a=new JSONArray(p.getString("telegram_api_album_paths","[]"));for(int i=0;i<a.length();i++){File f=new File(a.optString(i));if(!f.exists())continue;android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null)continue;ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);iv.setImageBitmap(b);iv.setPadding(0,6,0,12);previews.addView(iv,new LinearLayout.LayoutParams(-1,-2));}}catch(Throwable ignored){}}
}''',encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if 'android.permission.INTERNET' not in m:m=m.replace('<manifest','<manifest',1).replace('>','>\n    <uses-permission android:name="android.permission.INTERNET"/>',1)
if 'android.permission.FOREGROUND_SERVICE' not in m:m=m.replace('>\n    <uses-permission android:name="android.permission.INTERNET"/>','>\n    <uses-permission android:name="android.permission.INTERNET"/>\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>',1)
if '.TelegramApiSettingsActivity' not in m:m=m.replace('</application>','<activity android:name=".TelegramApiSettingsActivity" android:screenOrientation="portrait" android:exported="false"/>\n        <service android:name=".TelegramApiAlbumService" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

D=dash.read_text(encoding='utf-8')
needle='''  panel.addView(sideItem("✈","Detector Telegram",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,TelegramDetectorActivity.class));}));'''
item='''\n  panel.addView(sideItem("☁","Motor Telegram API",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,TelegramApiSettingsActivity.class));}));'''
if needle in D and 'TelegramApiSettingsActivity.class' not in D:D=D.replace(needle,needle+item,1)
elif 'TelegramApiSettingsActivity.class' not in D:raise SystemExit('ERRO v2.1.38: item Detector Telegram não encontrado')
D=D.replace('brand.addView(text("v2.1.37",10,MUTED,false));','brand.addView(text("v2.1.38",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')

G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 109',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.38'",G,count=1);gradle.write_text(G,encoding='utf-8')

for pth,tok in [(java/'TelegramApiAlbumService.java','media_group_id'),(java/'TelegramApiAlbumService.java','getUpdates'),(java/'TelegramApiAlbumService.java','getFile'),(java/'TelegramApiSettingsActivity.java','USAR ÚLTIMO CHAT DETECTADO'),(manifest,'.TelegramApiAlbumService'),(dash,'Motor Telegram API'),(gradle,"versionName '2.1.38'")]:
 if tok not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.38: requisito ausente '+tok)
print('v2.1.38: motor Telegram Bot API criado para capturar álbum completo via media_group_id')