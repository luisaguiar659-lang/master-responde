from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
manifest = app / 'src/main/AndroidManifest.xml'
dash = java_dir / 'NeonDashboardActivity.java'

service = java_dir / 'WhatsAppBusinessCaptureService.java'
service.write_text(r'''package com.masterresponde.app;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class WhatsAppBusinessCaptureService extends NotificationListenerService {
    public static final String PACKAGE_NAME = "com.whatsapp.w4b";
    private static final String PREFS = "master_responde";
    private static final String KEY_CONNECTED = "capture_listener_connected";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        prefs().edit()
                .putBoolean(KEY_CONNECTED, true)
                .putString("accessibility_last_status", "Captura conectada ao WhatsApp Business")
                .apply();
    }

    @Override
    public void onListenerDisconnected() {
        prefs().edit()
                .putBoolean(KEY_CONNECTED, false)
                .putString("accessibility_last_status", "Captura desconectada")
                .apply();
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !PACKAGE_NAME.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null) return;
        Bundle extras = n.extras;
        if (extras == null) return;

        String conversation = firstNonEmpty(
                extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        );
        String message = firstNonEmpty(
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_TEXT)
        );

        if (message.isEmpty()) return;

        long now = System.currentTimeMillis();
        prefs().edit()
                .putBoolean(KEY_CONNECTED, true)
                .putString("accessibility_last_conversation", conversation)
                .putString("accessibility_last_message", message)
                .putString("accessibility_last_reply", "—")
                .putString("accessibility_last_status", "Mensagem capturada • sem resposta automática")
                .putLong("capture_last_timestamp", now)
                .putInt("capture_total", prefs().getInt("capture_total", 0) + 1)
                .apply();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private static String firstNonEmpty(CharSequence... values) {
        if (values == null) return "";
        for (CharSequence value : values) {
            if (value != null) {
                String s = value.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return "";
    }
}
''', encoding='utf-8')

# Registra apenas captura passiva. Nenhum RemoteInput, AccessibilityService ou envio.
m = manifest.read_text(encoding='utf-8')
if 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' not in m:
    service_xml = '''\n        <service\n            android:name=".WhatsAppBusinessCaptureService"\n            android:label="MASTER RESPONDE - Captura"\n            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"\n            android:exported="true">\n            <intent-filter>\n                <action android:name="android.service.notification.NotificationListenerService"/>\n            </intent-filter>\n        </service>\n'''
    pos = m.rfind('</application>')
    if pos < 0:
        raise SystemExit('ERRO captura: </application> não encontrado')
    m = m[:pos] + service_xml + m[pos:]
manifest.write_text(m, encoding='utf-8')

# Dashboard passa a mostrar o estado REAL do listener novo e continua sem motor de resposta.
d = dash.read_text(encoding='utf-8')
old = 'boolean on=prefs.getBoolean("bot_enabled",false),bi=isBusinessInstalled(),ac=false,no=isNotificationAccessEnabled();'
new = 'boolean on=prefs.getBoolean("bot_enabled",false),bi=isBusinessInstalled(),capture=prefs.getBoolean("capture_listener_connected",false),ac=capture,no=isNotificationAccessEnabled();'
if old in d:
    d = d.replace(old, new, 1)
elif 'capture_listener_connected' not in d:
    # fallback para pequenas variações do refresh
    d = re.sub(
        r'boolean on=prefs\.getBoolean\("bot_enabled",false\),bi=isBusinessInstalled\(\),ac=[^,;]+,no=isNotificationAccessEnabled\(\);',
        new,
        d,
        count=1,
    )

# Card 2 vira CAPTURA em vez de sugerir Accessibility antiga.
d = d.replace('acc=service(services,1,"Acessibilidade",PURPLE);', 'acc=service(services,1,"Captura",PURPLE);', 1)
d = d.replace('set(acc,ac?"ATIVA":"OFF",ac);', 'set(acc,ac?"ATIVA":"RECONECTAR",ac);', 1)
# Motor explicitamente não envia nesta etapa.
d = d.replace('set(engine,"EM CONSTRUÇÃO",false);', 'set(engine,"CAPTURA ONLY",false);', 1)
dash.write_text(d, encoding='utf-8')

# Versiona a etapa de captura.
gradle = app / 'build.gradle'
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 72', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.1'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# Valida arquitetura: captura sim, envio não.
final_manifest = manifest.read_text(encoding='utf-8')
if '.WhatsAppBusinessCaptureService' not in final_manifest:
    raise SystemExit('ERRO captura: serviço não registrado')
if 'NotificationListenerService' not in service.read_text(encoding='utf-8'):
    raise SystemExit('ERRO captura: listener ausente')
for forbidden in ['RemoteInput', 'AccessibilityService', 'WhatsAppReply.send', 'performAction', 'ACTION_SET_TEXT']:
    if forbidden in service.read_text(encoding='utf-8'):
        raise SystemExit('ERRO captura: mecanismo de envio proibido nesta etapa: ' + forbidden)
print('v2.1.1: captura passiva do WhatsApp Business criada; nenhum envio automático implementado')
