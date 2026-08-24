from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
svc=java/'WhatsAppBusinessCaptureService.java'
dash=java/'NeonDashboardActivity.java'
manifest=app/'src/main/AndroidManifest.xml'
gradle=app/'build.gradle'

# 1) Detector passivo: intercepta Telegram ANTES do filtro do WhatsApp.
s=svc.read_text(encoding='utf-8')
old='''    @Override\n    public void onNotificationPosted(StatusBarNotification sbn) {\n        if (sbn == null || !PACKAGE_NAME.equals(sbn.getPackageName())) return;'''
if old not in s:
    raise SystemExit('ERRO v2.1.34: início de onNotificationPosted não encontrado')
new='''    @Override\n    public void onNotificationPosted(StatusBarNotification sbn) {\n        if (sbn == null) return;\n        String sourcePackage = sbn.getPackageName();\n        if (isTelegramPackage(sourcePackage)) {\n            captureTelegramNotification(sbn);\n            return;\n        }\n        if (!PACKAGE_NAME.equals(sourcePackage)) return;'''
s=s.replace(old,new,1)

# Métodos isolados: não respondem nem alteram o fluxo do WhatsApp.
pos=s.rfind('}')
if pos<0: raise SystemExit('ERRO v2.1.34: fim da classe não encontrado')
helper=r'''
    private boolean isTelegramPackage(String pkg) {
        return "org.telegram.messenger".equals(pkg)
                || "org.telegram.messenger.web".equals(pkg)
                || "org.telegram.messenger.beta".equals(pkg)
                || "org.thunderdog.challegram".equals(pkg);
    }

    private void captureTelegramNotification(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            Bundle e = n.extras;

            String title = firstNonEmpty(
                    e.getCharSequence(Notification.EXTRA_TITLE),
                    e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            );
            String conversation = firstNonEmpty(
                    e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
                    e.getCharSequence(Notification.EXTRA_SUB_TEXT),
                    e.getCharSequence(Notification.EXTRA_TITLE)
            );
            String message = firstNonEmpty(
                    e.getCharSequence(Notification.EXTRA_BIG_TEXT),
                    e.getCharSequence(Notification.EXTRA_TEXT)
            );

            // MessagingStyle costuma trazer a mensagem mais fiel, inclusive em grupos.
            try {
    java.util.List<android.app.Notification.MessagingStyle.Message> msgs =
            android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                    (android.os.Parcelable[]) e.getParcelableArray(Notification.EXTRA_MESSAGES));

    if (msgs != null && !msgs.isEmpty()) {
        android.app.Notification.MessagingStyle.Message last =
                msgs.get(msgs.size() - 1);

        if (last != null) {
            CharSequence txt = last.getText();

            if (txt != null && !txt.toString().trim().isEmpty()) {
                message = txt.toString().trim();
            }

            if (last.getSenderPerson() != null &&
                    last.getSenderPerson().getName() != null) {

                String sender =
                        last.getSenderPerson().getName().toString().trim();

                if (!sender.isEmpty()) {
                    title = sender;
                }
            }
        }
    }
} catch (Throwable ignored) {}

            if (message == null) message = "";
            message = message.trim();
            if (message.isEmpty()) return;

            long now = System.currentTimeMillis();
            String fingerprint = (conversation + "\n" + title + "\n" + message).toLowerCase(java.util.Locale.ROOT);
            SharedPreferences p = prefs();
            String lastFp = p.getString("telegram_last_fingerprint", "");
            long lastAt = p.getLong("telegram_last_fingerprint_at", 0L);
            if (fingerprint.equals(lastFp) && now - lastAt >= 0L && now - lastAt < 2500L) return;

            p.edit()
                    .putString("telegram_last_fingerprint", fingerprint)
                    .putLong("telegram_last_fingerprint_at", now)
                    .putString("telegram_last_conversation", conversation)
                    .putString("telegram_last_sender", title)
                    .putString("telegram_last_message", message)
                    .putLong("telegram_last_timestamp", now)
                    .putString("telegram_last_status", "Mensagem do Telegram detectada")
                    .putInt("telegram_capture_total", p.getInt("telegram_capture_total", 0) + 1)
                    .apply();
        } catch (Throwable ignored) {
            prefs().edit().putString("telegram_last_status", "Falha ao ler notificação do Telegram").apply();
        }
    }
'''
s=s[:pos]+helper+s[pos:]
svc.write_text(s,encoding='utf-8')

# 2) Tela de teste/diagnóstico. Sem envio ao WhatsApp.
(java/'TelegramDetectorActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.provider.Settings;import android.view.*;import android.widget.*;import java.text.*;import java.util.*;

public class TelegramDetectorActivity extends Activity {
    SharedPreferences p; TextView status,conversation,sender,message,time,total;
    final Handler h=new Handler(Looper.getMainLooper());
    final Runnable refresh=new Runnable(){public void run(){update();h.postDelayed(this,1000L);}};

    public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);setContentView(build());}
    protected void onResume(){super.onResume();h.removeCallbacks(refresh);h.post(refresh);}
    protected void onPause(){h.removeCallbacks(refresh);super.onPause();}

    TextView t(String s,int z,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.WHITE);if(bold)v.setTypeface(null,Typeface.BOLD);v.setPadding(8,10,8,10);return v;}
    TextView value(){TextView v=t("—",15,false);v.setBackgroundColor(Color.rgb(5,18,25));v.setPadding(18,16,18,16);return v;}
    View build(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));
        r.addView(t("DETECTOR TELEGRAM",24,true));
        TextView sub=t("Primeiro teste: apenas detectar mensagens recebidas no Telegram. Nada será enviado ao WhatsApp.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);
        status=value();r.addView(status);
        r.addView(t("CONVERSA / GRUPO",13,true));conversation=value();r.addView(conversation);
        r.addView(t("REMETENTE",13,true));sender=value();r.addView(sender);
        r.addView(t("MENSAGEM",13,true));message=value();message.setMinLines(3);r.addView(message);
        r.addView(t("HORÁRIO",13,true));time=value();r.addView(time);
        r.addView(t("TOTAL DETECTADO",13,true));total=value();r.addView(total);
        Button access=new Button(this);access.setText("ABRIR ACESSO ÀS NOTIFICAÇÕES");access.setOnClickListener(v->{try{startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}catch(Throwable e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}});r.addView(access);
        Button clear=new Button(this);clear.setText("LIMPAR ÚLTIMO TESTE");clear.setOnClickListener(v->{p.edit().remove("telegram_last_conversation").remove("telegram_last_sender").remove("telegram_last_message").remove("telegram_last_timestamp").putString("telegram_last_status","Aguardando mensagem do Telegram").apply();update();});r.addView(clear);
        r.addView(t("TESTE: peça para alguém mandar uma mensagem para você no Telegram ou em um grupo comum. Deixe o MASTER RESPONDE aberto nesta tela e veja os campos atualizarem.",12,false));
        return sc;
    }
    void update(){
        status.setText("Status: "+p.getString("telegram_last_status","Aguardando mensagem do Telegram"));
        conversation.setText(orDash(p.getString("telegram_last_conversation","")));
        sender.setText(orDash(p.getString("telegram_last_sender","")));
        message.setText(orDash(p.getString("telegram_last_message","")));
        long ts=p.getLong("telegram_last_timestamp",0L);time.setText(ts>0?new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.getDefault()).format(new Date(ts)):"—");
        total.setText(String.valueOf(p.getInt("telegram_capture_total",0)));
    }
    String orDash(String x){return x==null||x.trim().isEmpty()?"—":x.trim();}
}''',encoding='utf-8')

# 3) Manifest e menu lateral.
m=manifest.read_text(encoding='utf-8')
if '.TelegramDetectorActivity' not in m:
    m=m.replace('</application>','<activity android:name=".TelegramDetectorActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

D=dash.read_text(encoding='utf-8')
# Coloca perto de Avisos Automáticos se possível.
needle='''  panel.addView(sideItem("⏱","Avisos Automáticos",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,ScheduledGroupBroadcastActivity.class));}));'''
item='''\n  panel.addView(sideItem("✈","Detector Telegram",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,TelegramDetectorActivity.class));}));'''
if needle in D and 'TelegramDetectorActivity.class' not in D:
    D=D.replace(needle,needle+item,1)
elif 'TelegramDetectorActivity.class' not in D:
    raise SystemExit('ERRO v2.1.34: ponto do menu lateral não encontrado')
D=D.replace('brand.addView(text("v2.1.33",10,MUTED,false));','brand.addView(text("v2.1.34",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')

# 4) Versão.
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 105',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.34'",G,count=1)
gradle.write_text(G,encoding='utf-8')

checks=[
 (svc,'isTelegramPackage(sourcePackage)'),(svc,'captureTelegramNotification(sbn)'),(svc,'telegram_last_message'),
 (java/'TelegramDetectorActivity.java','DETECTOR TELEGRAM'),(java/'TelegramDetectorActivity.java','TOTAL DETECTADO'),
 (dash,'Detector Telegram'),(manifest,'.TelegramDetectorActivity'),(gradle,"versionName '2.1.34'")]
for pth,token in checks:
    if token not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.34: requisito ausente '+token)
print('v2.1.34: detector passivo de mensagens do Telegram criado sem qualquer encaminhamento ao WhatsApp')
