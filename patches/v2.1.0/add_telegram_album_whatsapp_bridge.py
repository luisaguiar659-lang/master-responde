from pathlib import Path
import re

app = Path('projeto/app')
java = app / 'src/main/java/com/masterresponde/app'
svc = java / 'WhatsAppBusinessCaptureService.java'
gradle = app / 'build.gradle'
dash = java / 'NeonDashboardActivity.java'

# v2.1.36
# Integra o último álbum capturado pelo Motor Telegram API com o comando
# @infor banners do WhatsApp Business. O envio tenta usar RemoteInput de dados
# (image/jpeg) da própria notificação do WhatsApp. Quando a versão do WhatsApp
# não expõe RemoteInput de mídia, o motor não usa o WhatsApp comum e registra
# o motivo no status, mantendo o fluxo seguro.

(java / 'TelegramAlbumWhatsAppBridge.java').write_text(r'''package com.masterresponde.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TelegramAlbumWhatsAppBridge {
    private static final String PREFS = "master_responde";
    private TelegramAlbumWhatsAppBridge() {}

    public static boolean isBannerRequest(String message) {
        if (message == null) return false;
        String x = normalize(message);
        return x.equals("@infor banners") || x.equals("banners") || x.equals("banner");
    }

    public static int albumCount(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("telegram_api_album_count", 0);
    }

    public static boolean sendLatestAlbum(Context c, Notification n) {
        if (c == null || n == null) return false;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<File> files = loadAlbumFiles(p);
        if (files.isEmpty()) {
            status(p, "Nenhum álbum Telegram disponível para @infor banners");
            return false;
        }

        Notification.Action mediaAction = findMediaReplyAction(n);
        if (mediaAction == null || mediaAction.actionIntent == null) {
            status(p, "WhatsApp Business não expôs envio de imagem pela notificação");
            return false;
        }

        RemoteInput[] inputs = mediaAction.getRemoteInputs();
        if (inputs == null || inputs.length == 0) {
            status(p, "WhatsApp Business sem RemoteInput de mídia disponível");
            return false;
        }

        int sent = 0;
        for (File f : files) {
            if (!f.exists()) continue;
            try {
                Uri uri = FileProvider.getUriForFile(
                        c,
                        c.getPackageName() + ".telegramfiles",
                        f
                );

                Intent fill = new Intent();
                fill.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                boolean attached = false;

                for (RemoteInput input : inputs) {
                    if (input == null || input.getResultKey() == null) continue;
                    try {
                        if (input.isDataOnly() || allowsImage(input)) {
                            java.util.Map<String, Uri> data = new java.util.HashMap<>();
                            data.put("image/jpeg", uri);
                            RemoteInput.addDataResultToIntent(input, fill, data);
                            attached = true;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }

                if (!attached) {
                    status(p, "A ação Responder do WhatsApp não aceita imagem nesta versão");
                    return false;
                }

                mediaAction.actionIntent.send(c, 0, fill);
                sent++;
                try { Thread.sleep(650L); } catch (InterruptedException ignored) {}
            } catch (PendingIntent.CanceledException e) {
                status(p, "A notificação do WhatsApp expirou durante o envio do álbum");
                return false;
            } catch (Throwable e) {
                status(p, "Falha ao enviar imagem do Telegram: " + safe(e.getMessage()));
                return false;
            }
        }

        if (sent > 0) {
            p.edit()
                    .putInt("telegram_whatsapp_last_sent_count", sent)
                    .putLong("telegram_whatsapp_last_sent_at", System.currentTimeMillis())
                    .putString("telegram_whatsapp_last_status", "Álbum Telegram enviado: " + sent + " imagens")
                    .apply();
            return true;
        }
        return false;
    }

    private static Notification.Action findMediaReplyAction(Notification n) {
        if (n.actions == null) return null;
        Notification.Action fallback = null;
        for (Notification.Action a : n.actions) {
            if (a == null || a.actionIntent == null) continue;
            RemoteInput[] rs = a.getRemoteInputs();
            if (rs == null || rs.length == 0) continue;
            if (fallback == null) fallback = a;
            for (RemoteInput r : rs) {
                if (r == null) continue;
                try {
                    if (r.isDataOnly() || allowsImage(r)) return a;
                } catch (Throwable ignored) {}
            }
        }
        return fallback;
    }

    private static boolean allowsImage(RemoteInput r) {
        try {
            java.util.Set<String> types = r.getAllowedDataTypes();
            if (types == null) return false;
            for (String t : types) {
                if (t != null && t.toLowerCase(Locale.ROOT).startsWith("image/")) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static List<File> loadAlbumFiles(SharedPreferences p) {
        List<File> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString("telegram_api_album_paths", "[]"));
            for (int i = 0; i < a.length(); i++) {
                String path = a.optString(i, "").trim();
                if (!path.isEmpty()) out.add(new File(path));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static void status(SharedPreferences p, String s) {
        p.edit().putString("telegram_whatsapp_last_status", s).apply();
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
''', encoding='utf-8')

# FileProvider para expor somente as imagens internas capturadas pelo Telegram.
resxml = app / 'src/main/res/xml'
resxml.mkdir(parents=True, exist_ok=True)
(resxml / 'telegram_file_paths.xml').write_text(r'''<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path name="telegram_album" path="telegram_api_album/" />
</paths>
''', encoding='utf-8')

manifest = app / 'src/main/AndroidManifest.xml'
m = manifest.read_text(encoding='utf-8')
provider = '''\n        <provider\n            android:name="androidx.core.content.FileProvider"\n            android:authorities="${applicationId}.telegramfiles"\n            android:exported="false"\n            android:grantUriPermissions="true">\n            <meta-data\n                android:name="android.support.FILE_PROVIDER_PATHS"\n                android:resource="@xml/telegram_file_paths"/>\n        </provider>\n'''
if '.telegramfiles' not in m:
    m = m.replace('</application>', provider + '</application>')
manifest.write_text(m, encoding='utf-8')

# Garante a dependência do FileProvider (AndroidX Core).
g = gradle.read_text(encoding='utf-8')
if 'androidx.core:core:' not in g and 'androidx.core:core-' not in g:
    if 'dependencies {' in g:
        g = g.replace('dependencies {', "dependencies {\n    implementation 'androidx.core:core:1.15.0'", 1)
    else:
        g += "\n\ndependencies {\n    implementation 'androidx.core:core:1.15.0'\n}\n"
g = re.sub(r'versionCode\s+\d+', 'versionCode 107', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.36'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# Hook no motor WhatsApp Business. O ponto escolhido é logo após a mensagem
# ser normalizada; só intercepta @infor banners em grupos, preservando todos
# os demais comandos e respostas.
s = svc.read_text(encoding='utf-8')
if 'TelegramAlbumWhatsAppBridge.sendLatestAlbum' not in s:
    candidates = [
        '''            if (message == null) message = "";\n            message = message.trim();''',
        '''        if (message == null) message = "";\n        message = message.trim();'''
    ]
    inserted = False
    for needle in candidates:
        if needle in s:
            hook = needle + '''\n            try {\n                if (isGroupNotification(n) && TelegramAlbumWhatsAppBridge.isBannerRequest(message)) {\n                    if (TelegramAlbumWhatsAppBridge.sendLatestAlbum(this, n)) {\n                        prefs().edit().putString("accessibility_last_status", "@infor banners: álbum Telegram enviado ao WhatsApp Business").apply();\n                        return;\n                    }\n                }\n            } catch (Throwable ignored) {}'''
            s = s.replace(needle, hook, 1)
            inserted = True
            break
    if not inserted:
        raise SystemExit('ERRO v2.1.36: ponto de leitura da mensagem WhatsApp não encontrado')
svc.write_text(s, encoding='utf-8')

# Mostra versão atual no painel quando houver o rótulo anterior.
D = dash.read_text(encoding='utf-8')
D = re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);',
           'brand.addView(text("v2.1.36",10,MUTED,false));', D, count=1)
dash.write_text(D, encoding='utf-8')

checks = [
    (java / 'TelegramAlbumWhatsAppBridge.java', 'sendLatestAlbum'),
    (java / 'TelegramAlbumWhatsAppBridge.java', 'com.whatsapp'),
    (svc, 'TelegramAlbumWhatsAppBridge.isBannerRequest'),
    (manifest, '.telegramfiles'),
    (resxml / 'telegram_file_paths.xml', 'telegram_api_album/'),
    (gradle, "versionName '2.1.36'")
]
for p, marker in checks:
    text = p.read_text(encoding='utf-8')
    if marker not in text:
        raise SystemExit('ERRO v2.1.36: requisito ausente ' + marker)

print('v2.1.36: @infor banners conectado ao último álbum capturado pelo Motor Telegram API, com envio exclusivo pelo WhatsApp Business e fallback seguro quando a notificação não aceita mídia')