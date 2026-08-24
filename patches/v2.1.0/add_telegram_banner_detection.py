from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
svc=java/'WhatsAppBusinessCaptureService.java'
act=java/'TelegramDetectorActivity.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

s=svc.read_text(encoding='utf-8')

needle='''            String message = firstNonEmpty(
                    e.getCharSequence(Notification.EXTRA_BIG_TEXT),
                    e.getCharSequence(Notification.EXTRA_TEXT)
            );'''
repl=needle+'''\n            boolean hasImage = false;\n            String mediaType = "texto";\n            String mediaUri = "";\n            try {\n                Object picture = e.get(Notification.EXTRA_PICTURE);\n                if (picture != null) { hasImage = true; mediaType = "imagem/banner"; }\n            } catch (Throwable ignored) {}'''
if needle not in s: raise SystemExit('ERRO v2.1.35: leitura da mensagem Telegram não encontrada')
s=s.replace(needle,repl,1)

# O patch v2.1.34 teve o bloco MessagingStyle corrigido depois do primeiro build.
# Em vez de depender da indentação exata, ancora na linha estável CharSequence txt.
needle2='''        if (last != null) {
            CharSequence txt = last.getText();'''
repl2='''        if (last != null) {
            try {
                String mime = last.getDataMimeType();
                android.net.Uri uri = last.getDataUri();
                if (mime != null && mime.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                    hasImage = true;
                    mediaType = "imagem/banner";
                    if (uri != null) mediaUri = uri.toString();
                }
            } catch (Throwable ignored) {}
            CharSequence txt = last.getText();'''
if needle2 not in s: raise SystemExit('ERRO v2.1.35: ponto da última mensagem MessagingStyle não encontrado')
s=s.replace(needle2,repl2,1)

needle3='''            if (message == null) message = "";
            message = message.trim();
            if (message.isEmpty()) return;'''
repl3='''            if (message == null) message = "";
            message = message.trim();
            String lowerTelegramMessage = message.toLowerCase(java.util.Locale.ROOT);
            if (!hasImage && (lowerTelegramMessage.equals("foto") || lowerTelegramMessage.equals("photo") || lowerTelegramMessage.startsWith("🖼") || lowerTelegramMessage.contains("imagem"))) {
                hasImage = true;
                mediaType = "imagem/banner";
            }
            if (message.isEmpty() && !hasImage) return;
            if (message.isEmpty() && hasImage) message = "[imagem sem legenda]";'''
if needle3 not in s: raise SystemExit('ERRO v2.1.35: normalização da mensagem não encontrada')
s=s.replace(needle3,repl3,1)

needle4='''.putString("telegram_last_message", message)
                    .putLong("telegram_last_timestamp", now)
                    .putString("telegram_last_status", "Mensagem do Telegram detectada")'''
repl4='''.putString("telegram_last_message", message)
                    .putBoolean("telegram_last_has_image", hasImage)
                    .putString("telegram_last_media_type", mediaType)
                    .putString("telegram_last_media_uri", mediaUri)
                    .putLong("telegram_last_timestamp", now)
                    .putString("telegram_last_status", hasImage ? "Imagem/banner do Telegram detectado" : "Mensagem do Telegram detectada")'''
if needle4 not in s: raise SystemExit('ERRO v2.1.35: persistência Telegram não encontrada')
s=s.replace(needle4,repl4,1)
svc.write_text(s,encoding='utf-8')

a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p; TextView status,conversation,sender,message,time,total;', 'SharedPreferences p; TextView status,conversation,sender,message,media,time,total;',1)
a=a.replace('''        r.addView(t("MENSAGEM",13,true));message=value();message.setMinLines(3);r.addView(message);
        r.addView(t("HORÁRIO",13,true));time=value();r.addView(time);''','''        r.addView(t("MENSAGEM / LEGENDA",13,true));message=value();message.setMinLines(3);r.addView(message);
        r.addView(t("TIPO DE CONTEÚDO",13,true));media=value();r.addView(media);
        r.addView(t("HORÁRIO",13,true));time=value();r.addView(time);''',1)
a=a.replace('''        message.setText(orDash(p.getString("telegram_last_message","")));
        long ts=p.getLong("telegram_last_timestamp",0L);''','''        message.setText(orDash(p.getString("telegram_last_message","")));
        boolean image=p.getBoolean("telegram_last_has_image",false);
        String mt=p.getString("telegram_last_media_type",image?"imagem/banner":"texto");
        media.setText(image ? "🖼 IMAGEM / BANNER DETECTADO" : mt.toUpperCase(Locale.ROOT));
        long ts=p.getLong("telegram_last_timestamp",0L);''',1)
a=a.replace('Primeiro teste: apenas detectar mensagens recebidas no Telegram. Nada será enviado ao WhatsApp.', 'Teste de mídia: detectar texto e imagem/banner recebidos no Telegram. Nada será enviado ao WhatsApp.',1)
a=a.replace('TESTE: peça para alguém mandar uma mensagem para você no Telegram ou em um grupo comum. Deixe o MASTER RESPONDE aberto nesta tela e veja os campos atualizarem.', 'TESTE: envie uma foto ou banner em um grupo do Telegram. Se a notificação expuser a mídia, o campo TIPO DE CONTEÚDO mostrará IMAGEM / BANNER DETECTADO. Ainda não haverá envio ao WhatsApp.',1)
act.write_text(a,encoding='utf-8')

D=dash.read_text(encoding='utf-8')
D=D.replace('brand.addView(text("v2.1.34",10,MUTED,false));','brand.addView(text("v2.1.35",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')

G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 106',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.35'",G,count=1)
gradle.write_text(G,encoding='utf-8')

checks=[
 (svc,'telegram_last_has_image'),(svc,'getDataMimeType()'),(svc,'Notification.EXTRA_PICTURE'),
 (act,'TIPO DE CONTEÚDO'),(act,'IMAGEM / BANNER DETECTADO'),(gradle,"versionName '2.1.35'")]
for p,t in checks:
    if t not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.35: requisito ausente '+t)
print('v2.1.35: detector Telegram agora identifica imagem/banner e legenda, sem encaminhar ao WhatsApp')