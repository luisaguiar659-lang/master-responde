from pathlib import Path
import re

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'
svc=java/'WhatsAppBusinessCaptureService.java'; act=java/'TelegramDetectorActivity.java'; dash=java/'NeonDashboardActivity.java'; gradle=app/'build.gradle'
s=svc.read_text(encoding='utf-8')

# Em vez de sobrescrever sempre last_banner.jpg, acumula imagens distintas que chegam
# em notificações próximas da mesma conversa. Isso cobre o comportamento de álbum do Telegram.
old='''                        java.io.File outFile = new java.io.File(dir, "last_banner.jpg");
                        java.io.FileOutputStream out = new java.io.FileOutputStream(outFile, false);
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out);
                        out.flush(); out.close();
                        localImagePath = outFile.getAbsolutePath();'''
new='''                        long albumWindowMs = 8000L;
                        SharedPreferences albumPrefs = prefs();
                        long albumLastAt = albumPrefs.getLong("telegram_album_last_at", 0L);
                        String albumConversation = albumPrefs.getString("telegram_album_conversation", "");
                        int albumCount = albumPrefs.getInt("telegram_album_count", 0);
                        if (!conversation.equals(albumConversation) || now - albumLastAt > albumWindowMs) {
                            albumCount = 0;
                            java.io.File[] oldFiles = dir.listFiles();
                            if (oldFiles != null) for (java.io.File f : oldFiles) if (f.getName().startsWith("album_")) try { f.delete(); } catch (Throwable ignored) {}
                        }
                        albumCount++;
                        java.io.File outFile = new java.io.File(dir, "album_" + albumCount + ".jpg");
                        java.io.FileOutputStream out = new java.io.FileOutputStream(outFile, false);
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out);
                        out.flush(); out.close();
                        localImagePath = outFile.getAbsolutePath();
                        albumPrefs.edit().putLong("telegram_album_last_at", now).putString("telegram_album_conversation", conversation).putInt("telegram_album_count", albumCount).apply();'''
if old not in s: raise SystemExit('ERRO v2.1.37: gravação da prévia v2.1.36 não encontrada')
s=s.replace(old,new,1)

old2='''.putBoolean("telegram_last_image_recovered", !localImagePath.isEmpty())
                    .putLong("telegram_last_timestamp", now)'''
new2='''.putBoolean("telegram_last_image_recovered", !localImagePath.isEmpty())
                    .putInt("telegram_last_album_count", prefs().getInt("telegram_album_count", hasImage ? 1 : 0))
                    .putLong("telegram_last_timestamp", now)'''
if old2 not in s: raise SystemExit('ERRO v2.1.37: persistência preview não encontrada')
s=s.replace(old2,new2,1)
svc.write_text(s,encoding='utf-8')

a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p; TextView status,conversation,sender,message,media,recovery,time,total; ImageView preview;', 'SharedPreferences p; TextView status,conversation,sender,message,media,recovery,albumCount,time,total; LinearLayout previews;',1)
old3='''        r.addView(t("RECUPERAÇÃO DA IMAGEM",13,true));recovery=value();r.addView(recovery);
        r.addView(t("PRÉVIA CAPTURADA",13,true));
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setMinimumHeight(220);preview.setBackgroundColor(Color.rgb(5,18,25));r.addView(preview,new LinearLayout.LayoutParams(-1,-2));'''
new3='''        r.addView(t("RECUPERAÇÃO DA IMAGEM",13,true));recovery=value();r.addView(recovery);
        r.addView(t("IMAGENS NO ÚLTIMO LOTE",13,true));albumCount=value();r.addView(albumCount);
        r.addView(t("PRÉVIAS CAPTURADAS",13,true));
        previews=new LinearLayout(this);previews.setOrientation(LinearLayout.VERTICAL);previews.setPadding(0,4,0,4);r.addView(previews,new LinearLayout.LayoutParams(-1,-2));'''
if old3 not in a: raise SystemExit('ERRO v2.1.37: preview única não encontrada')
a=a.replace(old3,new3,1)
old4='''        String path=p.getString("telegram_last_image_path","");
        boolean recovered=p.getBoolean("telegram_last_image_recovered",false);
        recovery.setText(recovered ? "IMAGEM RECUPERADA COM SUCESSO" : (image ? "IMAGEM DETECTADA, ARQUIVO AINDA NÃO ACESSÍVEL" : "—"));
        if(recovered && path!=null && !path.isEmpty()){
            try{android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(path);preview.setImageBitmap(b);preview.setVisibility(View.VISIBLE);}catch(Throwable e){preview.setImageDrawable(null);}
        }else{preview.setImageDrawable(null);preview.setVisibility(image?View.VISIBLE:View.GONE);}'''
new4='''        boolean recovered=p.getBoolean("telegram_last_image_recovered",false);
        int count=p.getInt("telegram_last_album_count", image?1:0);
        recovery.setText(recovered ? (count>1 ? "ÁLBUM / LOTE RECUPERADO" : "IMAGEM RECUPERADA COM SUCESSO") : (image ? "IMAGEM DETECTADA, ARQUIVO AINDA NÃO ACESSÍVEL" : "—"));
        albumCount.setText(String.valueOf(count));
        previews.removeAllViews();
        if(recovered){
            java.io.File dir=new java.io.File(getFilesDir(),"telegram_preview");
            for(int i=1;i<=count;i++){
                java.io.File f=new java.io.File(dir,"album_"+i+".jpg");
                if(!f.exists()) continue;
                try{android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());if(b!=null){ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);iv.setImageBitmap(b);iv.setPadding(0,6,0,12);previews.addView(iv,new LinearLayout.LayoutParams(-1,-2));}}catch(Throwable ignored){}
            }
        }'''
if old4 not in a: raise SystemExit('ERRO v2.1.37: atualização preview única não encontrada')
a=a.replace(old4,new4,1)
a=a.replace('Teste de captura: detectar e tentar recuperar a imagem/banner do Telegram. Nada será enviado ao WhatsApp.', 'Teste de álbum: capturar uma ou várias imagens enviadas juntas pelo Telegram. Nada será enviado ao WhatsApp.',1)
a=a.replace('TESTE: envie uma imagem no grupo do Telegram. Se o Android liberar os dados da mídia pela notificação, a própria imagem aparecerá em PRÉVIA CAPTURADA. Ainda não haverá envio ao WhatsApp.', 'TESTE: selecione 3 imagens e envie juntas no Telegram. Aguarde alguns segundos. O contador e as prévias mostrarão quantas imagens o Android realmente entregou ao detector.',1)
act.write_text(a,encoding='utf-8')

D=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.36",10,MUTED,false));','brand.addView(text("v2.1.37",10,MUTED,false));'); dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8'); G=re.sub(r'versionCode\s+\d+','versionCode 108',G,count=1); G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.37'",G,count=1); gradle.write_text(G,encoding='utf-8')
for p,t in [(svc,'telegram_album_count'),(svc,'album_" + albumCount'),(act,'IMAGENS NO ÚLTIMO LOTE'),(act,'PRÉVIAS CAPTURADAS'),(gradle,"versionName '2.1.37'")]:
    if t not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.37: requisito ausente '+t)
print('v2.1.37: captura acumulada de álbum/lote Telegram criada')