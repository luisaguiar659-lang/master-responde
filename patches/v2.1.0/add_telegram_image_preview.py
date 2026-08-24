from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
svc=java/'WhatsAppBusinessCaptureService.java'
act=java/'TelegramDetectorActivity.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

s=svc.read_text(encoding='utf-8')

# Salva uma cópia local da imagem exposta pela notificação para diagnóstico/preview.
needle='''            long now = System.currentTimeMillis();
            String fingerprint = (conversation + "\\n" + title + "\\n" + message).toLowerCase(java.util.Locale.ROOT);'''
repl='''            long now = System.currentTimeMillis();
            String localImagePath = "";
            if (hasImage) {
                try {
                    android.graphics.Bitmap bitmap = null;
                    Object picture = e.get(Notification.EXTRA_PICTURE);
                    if (picture instanceof android.graphics.Bitmap) bitmap = (android.graphics.Bitmap) picture;
                    if (bitmap == null && mediaUri != null && !mediaUri.trim().isEmpty()) {
                        java.io.InputStream in = null;
                        try {
                            in = getContentResolver().openInputStream(android.net.Uri.parse(mediaUri));
                            if (in != null) bitmap = android.graphics.BitmapFactory.decodeStream(in);
                        } finally { if (in != null) try { in.close(); } catch (Throwable ignored) {} }
                    }
                    if (bitmap != null) {
                        java.io.File dir = new java.io.File(getFilesDir(), "telegram_preview");
                        if (!dir.exists()) dir.mkdirs();
                        java.io.File outFile = new java.io.File(dir, "last_banner.jpg");
                        java.io.FileOutputStream out = new java.io.FileOutputStream(outFile, false);
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out);
                        out.flush(); out.close();
                        localImagePath = outFile.getAbsolutePath();
                    }
                } catch (Throwable ignored) {}
            }
            String fingerprint = (conversation + "\\n" + title + "\\n" + message).toLowerCase(java.util.Locale.ROOT);'''
if needle not in s: raise SystemExit('ERRO v2.1.36: ponto de persistência Telegram não encontrado')
s=s.replace(needle,repl,1)

needle2='''.putString("telegram_last_media_uri", mediaUri)
                    .putLong("telegram_last_timestamp", now)'''
repl2='''.putString("telegram_last_media_uri", mediaUri)
                    .putString("telegram_last_image_path", localImagePath)
                    .putBoolean("telegram_last_image_recovered", !localImagePath.isEmpty())
                    .putLong("telegram_last_timestamp", now)'''
if needle2 not in s: raise SystemExit('ERRO v2.1.36: persistência de mídia não encontrada')
s=s.replace(needle2,repl2,1)
svc.write_text(s,encoding='utf-8')

a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p; TextView status,conversation,sender,message,media,time,total;', 'SharedPreferences p; TextView status,conversation,sender,message,media,recovery,time,total; ImageView preview;',1)
needle3='''        r.addView(t("TIPO DE CONTEÚDO",13,true));media=value();r.addView(media);
        r.addView(t("HORÁRIO",13,true));time=value();r.addView(time);'''
repl3='''        r.addView(t("TIPO DE CONTEÚDO",13,true));media=value();r.addView(media);
        r.addView(t("RECUPERAÇÃO DA IMAGEM",13,true));recovery=value();r.addView(recovery);
        r.addView(t("PRÉVIA CAPTURADA",13,true));
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setMinimumHeight(220);preview.setBackgroundColor(Color.rgb(5,18,25));r.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        r.addView(t("HORÁRIO",13,true));time=value();r.addView(time);'''
if needle3 not in a: raise SystemExit('ERRO v2.1.36: campo tipo de conteúdo não encontrado')
a=a.replace(needle3,repl3,1)
needle4='''        media.setText(image ? "🖼 IMAGEM / BANNER DETECTADO" : mt.toUpperCase(Locale.ROOT));
        long ts=p.getLong("telegram_last_timestamp",0L);'''
repl4='''        media.setText(image ? "🖼 IMAGEM / BANNER DETECTADO" : mt.toUpperCase(Locale.ROOT));
        String path=p.getString("telegram_last_image_path","");
        boolean recovered=p.getBoolean("telegram_last_image_recovered",false);
        recovery.setText(recovered ? "IMAGEM RECUPERADA COM SUCESSO" : (image ? "IMAGEM DETECTADA, ARQUIVO AINDA NÃO ACESSÍVEL" : "—"));
        if(recovered && path!=null && !path.isEmpty()){
            try{android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(path);preview.setImageBitmap(b);preview.setVisibility(View.VISIBLE);}catch(Throwable e){preview.setImageDrawable(null);}
        }else{preview.setImageDrawable(null);preview.setVisibility(image?View.VISIBLE:View.GONE);}
        long ts=p.getLong("telegram_last_timestamp",0L);'''
if needle4 not in a: raise SystemExit('ERRO v2.1.36: atualização tipo de mídia não encontrada')
a=a.replace(needle4,repl4,1)
a=a.replace('Teste de mídia: detectar texto e imagem/banner recebidos no Telegram. Nada será enviado ao WhatsApp.', 'Teste de captura: detectar e tentar recuperar a imagem/banner do Telegram. Nada será enviado ao WhatsApp.',1)
a=a.replace('TESTE: envie uma foto ou banner em um grupo do Telegram. Se a notificação expuser a mídia, o campo TIPO DE CONTEÚDO mostrará IMAGEM / BANNER DETECTADO. Ainda não haverá envio ao WhatsApp.', 'TESTE: envie uma imagem no grupo do Telegram. Se o Android liberar os dados da mídia pela notificação, a própria imagem aparecerá em PRÉVIA CAPTURADA. Ainda não haverá envio ao WhatsApp.',1)
act.write_text(a,encoding='utf-8')

D=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.35",10,MUTED,false));','brand.addView(text("v2.1.36",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 107',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.36'",G,count=1)
gradle.write_text(G,encoding='utf-8')

checks=[(svc,'telegram_last_image_path'),(svc,'last_banner.jpg'),(act,'PRÉVIA CAPTURADA'),(act,'IMAGEM RECUPERADA COM SUCESSO'),(gradle,"versionName '2.1.36'")]
for p,t in checks:
    if t not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.36: requisito ausente '+t)
print('v2.1.36: diagnóstico de recuperação e prévia local da imagem Telegram criado')