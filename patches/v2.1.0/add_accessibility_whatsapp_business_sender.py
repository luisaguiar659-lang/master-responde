from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
res=app/'src/main/res'
manifest=app/'src/main/AndroidManifest.xml'
act=java/'TelegramApiSettingsActivity.java'
svc=java/'WhatsAppBusinessCaptureService.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# v2.1.41
# IMPORTANTE: este patch NÃO lê nem altera TelegramApiAlbumService.java.
# A captura Telegram API da base funcional permanece intacta.
# Aqui trocamos somente a segunda etapa (entrega ao WhatsApp Business):
# álbum já capturado -> ACTION_SEND_MULTIPLE -> Acessibilidade seleciona grupo -> envia.

# 1) AccessibilityService exclusivo para WhatsApp Business.
(java/'TelegramWhatsAppAccessibilityService.java').write_text(r'''package com.masterresponde.app;

import android.accessibilityservice.AccessibilityService;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.*;

public class TelegramWhatsAppAccessibilityService extends AccessibilityService {
    private static final String WA="com.whatsapp.w4b";
    private final Handler h=new Handler(Looper.getMainLooper());
    private boolean busy=false;
    private int index=0,stage=0;
    private long lastAction=0L;
    private List<String> groups=new ArrayList<>();
    private List<File> images=new ArrayList<>();

    private final Runnable tick=new Runnable(){@Override public void run(){
        try{if(!busy && TelegramAutoDeliveryStore.ready(TelegramWhatsAppAccessibilityService.this))beginBatch();}
        catch(Throwable e){TelegramAutoDeliveryStore.status(TelegramWhatsAppAccessibilityService.this,"Falha ao preparar envio automático");}
        h.postDelayed(this,1000L);
    }};

    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",true).apply();
        TelegramAutoDeliveryStore.status(this,"Envio automático pronto");
        h.removeCallbacks(tick);h.post(tick);
    }

    @Override public void onDestroy(){
        h.removeCallbacksAndMessages(null);
        getSharedPreferences("master_responde",MODE_PRIVATE).edit().putBoolean("telegram_accessibility_enabled",false).apply();
        super.onDestroy();
    }

    @Override public void onInterrupt(){TelegramAutoDeliveryStore.status(this,"Envio automático interrompido");}

    private void beginBatch(){
        groups=TelegramAutoDeliveryStore.groups(this);
        images=TelegramAutoDeliveryStore.images(this);
        if(groups.isEmpty()){TelegramAutoDeliveryStore.status(this,"Configure pelo menos um grupo de destino");return;}
        if(images.isEmpty()){TelegramAutoDeliveryStore.status(this,"Nenhuma imagem válida no último lote");return;}
        busy=true;index=0;launchCurrent();
    }

    private void launchCurrent(){
        if(index>=groups.size()){TelegramAutoDeliveryStore.finish(this,groups.size(),images.size());busy=false;return;}
        try{
            ArrayList<Uri> uris=new ArrayList<>();
            ClipData clip=null;
            for(File f:images){
                Uri u=FileProvider.getUriForFile(this,getPackageName()+".telegramfiles",f);
                uris.add(u);
                if(clip==null)clip=ClipData.newUri(getContentResolver(),"telegram",u);else clip.addItem(new ClipData.Item(u));
            }
            Intent i=new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("image/*");i.setPackage(WA);
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(clip!=null)i.setClipData(clip);
            stage=0;lastAction=System.currentTimeMillis();
            TelegramAutoDeliveryStore.status(this,"Abrindo WhatsApp Business • grupo "+(index+1)+"/"+groups.size());
            startActivity(i);
            h.postDelayed(()->{if(busy&&stage==0)TelegramAutoDeliveryStore.status(this,"Procurando grupo: "+currentGroup());},1200L);
        }catch(Throwable e){
            TelegramAutoDeliveryStore.status(this,"Não foi possível abrir o WhatsApp Business");busy=false;
        }
    }

    private String currentGroup(){return index<groups.size()?groups.get(index):"";}

    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!busy||event==null)return;
        CharSequence pkg=event.getPackageName();if(pkg==null||!WA.contentEquals(pkg))return;
        if(System.currentTimeMillis()-lastAction<500L)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
        try{
            if(stage==0)selectGroup(root);
            else if(stage==1)clickFirstSend(root);
            else if(stage==2)clickFinalSend(root);
        }finally{try{root.recycle();}catch(Throwable ignored){}}
    }

    private void selectGroup(AccessibilityNodeInfo root){
        String wanted=currentGroup();
        AccessibilityNodeInfo exact=findText(root,wanted,true);
        if(exact!=null&&click(exact)){
            stage=1;lastAction=System.currentTimeMillis();
            TelegramAutoDeliveryStore.status(this,"Grupo localizado: "+wanted);
            return;
        }
        AccessibilityNodeInfo edit=findEditable(root);
        if(edit!=null){
            Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,wanted);
            if(edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b)){lastAction=System.currentTimeMillis();return;}
        }
        AccessibilityNodeInfo search=findAny(root,new String[]{"Pesquisar","Search","Buscar"});
        if(search!=null&&click(search)){lastAction=System.currentTimeMillis();}
    }

    private void clickFirstSend(AccessibilityNodeInfo root){
        if(System.currentTimeMillis()-lastAction<800L)return;
        AccessibilityNodeInfo send=findSend(root);
        if(send!=null&&click(send)){
            stage=2;lastAction=System.currentTimeMillis();
            TelegramAutoDeliveryStore.status(this,"Preparando "+images.size()+" imagens para "+currentGroup());
        }
    }

    private void clickFinalSend(AccessibilityNodeInfo root){
        if(System.currentTimeMillis()-lastAction<900L)return;
        AccessibilityNodeInfo send=findSend(root);
        if(send!=null&&click(send)){
            String sent=currentGroup();
            stage=3;lastAction=System.currentTimeMillis();
            TelegramAutoDeliveryStore.status(this,"Enviando para "+sent+"...");
            h.postDelayed(()->completeCurrent(sent),2800L);
        }
    }

    private void completeCurrent(String sent){
        if(!busy)return;
        TelegramAutoDeliveryStore.status(this,"Enviado para "+sent+" • "+(index+1)+"/"+groups.size());
        index++;
        h.postDelayed(this::launchCurrent,1200L);
    }

    private AccessibilityNodeInfo findSend(AccessibilityNodeInfo root){
        AccessibilityNodeInfo n=findAny(root,new String[]{"Enviar","Send"});if(n!=null)return n;
        return findByIdSuffix(root,"/send");
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n){
        if(n==null)return null;
        try{if(n.isEditable())return n;}catch(Throwable ignored){}
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findEditable(n.getChild(i));if(r!=null)return r;}return null;
    }

    private AccessibilityNodeInfo findText(AccessibilityNodeInfo n,String text,boolean exact){
        if(n==null||text==null)return null;
        CharSequence t=n.getText(),d=n.getContentDescription();
        if(matches(t,text,exact)||matches(d,text,exact))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findText(n.getChild(i),text,exact);if(r!=null)return r;}return null;
    }

    private AccessibilityNodeInfo findAny(AccessibilityNodeInfo n,String[] values){
        if(n==null)return null;
        for(String s:values){if(matches(n.getText(),s,false)||matches(n.getContentDescription(),s,false))return n;}
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findAny(n.getChild(i),values);if(r!=null)return r;}return null;
    }

    private AccessibilityNodeInfo findByIdSuffix(AccessibilityNodeInfo n,String suffix){
        if(n==null)return null;String id=n.getViewIdResourceName();if(id!=null&&id.endsWith(suffix))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findByIdSuffix(n.getChild(i),suffix);if(r!=null)return r;}return null;
    }

    private boolean matches(CharSequence c,String s,boolean exact){if(c==null)return false;String a=c.toString().trim();return exact?a.equalsIgnoreCase(s):a.toLowerCase(Locale.ROOT).contains(s.toLowerCase(Locale.ROOT));}

    private boolean click(AccessibilityNodeInfo n){
        AccessibilityNodeInfo x=n;
        for(int i=0;i<5&&x!=null;i++){
            try{if(x.isClickable()&&x.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;}catch(Throwable ignored){}
            x=x.getParent();
        }
        return false;
    }
}
''',encoding='utf-8')

# 2) Configuração Android do serviço. Restringe eventos ao WhatsApp Business.
xml=res/'xml';xml.mkdir(parents=True,exist_ok=True)
(xml/'telegram_whatsapp_accessibility.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"\n android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewClicked"\n android:accessibilityFeedbackType="feedbackGeneric"\n android:notificationTimeout="100"\n android:canRetrieveWindowContent="true"\n android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"\n android:packageNames="com.whatsapp.w4b"\n android:description="@string/app_name"/>\n''',encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if '.TelegramWhatsAppAccessibilityService' not in m:
    block='''\n        <service android:name=".TelegramWhatsAppAccessibilityService" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" android:exported="false">\n            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter>\n            <meta-data android:name="android.accessibilityservice" android:resource="@xml/telegram_whatsapp_accessibility"/>\n        </service>\n'''
    m=m.replace('</application>',block+'</application>')
manifest.write_text(m,encoding='utf-8')

# 3) A tela do Motor Telegram API ganha somente o acesso necessário para o ENVIO.
# Não altera token, chat, polling, download ou montagem de álbum.
a=act.read_text(encoding='utf-8')
# Acrescenta campo de status do acesso.
a=a.replace('TextView state,lastChat,count,caption,delivery;','TextView state,lastChat,count,caption,delivery,autoAccess;',1)
anchor='''r.addView(t("ENVIO AUTOMÁTICO",13,true));delivery=val();r.addView(delivery);r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));'''
if anchor not in a: raise SystemExit('ERRO v2.1.41: bloco ENVIO AUTOMÁTICO v2.1.40 não encontrado')
insert='''r.addView(t("ENVIO AUTOMÁTICO",13,true));delivery=val();r.addView(delivery);r.addView(t("ACESSO PARA ENVIO AUTOMÁTICO",13,true));autoAccess=val();r.addView(autoAccess);Button accessBtn=new Button(this);accessBtn.setText("ATIVAR ENVIO AUTOMÁTICO");accessBtn.setOnClickListener(v->{try{startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Throwable e){Toast.makeText(this,"Abra Acessibilidade nas configurações",Toast.LENGTH_LONG).show();}});r.addView(accessBtn);r.addView(t("Ative uma única vez o serviço MASTER RESPONDE. Depois disso: recebeu no Telegram -> abre somente WhatsApp Business -> seleciona o grupo -> envia automaticamente.",11,false));r.addView(t("ÚLTIMO CHAT DETECTADO",13,true));'''
a=a.replace(anchor,insert,1)
needle='''delivery.setText(p.getString("telegram_auto_status","Aguardando conteúdo do Telegram"));String id=p.getString("telegram_api_last_chat_id","");'''
if needle not in a: raise SystemExit('ERRO v2.1.41: update da entrega não encontrado')
repl='''delivery.setText(p.getString("telegram_auto_status","Aguardando conteúdo do Telegram"));autoAccess.setText(isAutoSenderEnabled()?"ATIVO • WhatsApp Business":"DESATIVADO • toque em ATIVAR ENVIO AUTOMÁTICO");String id=p.getString("telegram_api_last_chat_id","");'''
a=a.replace(needle,repl,1)
# Helper não altera a lógica do motor.
pos=a.rfind('}')
helper=r'''
 boolean isAutoSenderEnabled(){
  try{
   String enabled=android.provider.Settings.Secure.getString(getContentResolver(),android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
   if(enabled==null)return false;
   return enabled.toLowerCase(java.util.Locale.ROOT).contains((getPackageName()+"/"+TelegramWhatsAppAccessibilityService.class.getName()).toLowerCase(java.util.Locale.ROOT))
       || enabled.toLowerCase(java.util.Locale.ROOT).contains(TelegramWhatsAppAccessibilityService.class.getName().toLowerCase(java.util.Locale.ROOT));
  }catch(Throwable e){return false;}
 }
'''
a=a[:pos]+helper+a[pos:]
act.write_text(a,encoding='utf-8')

# 4) Desliga SOMENTE o mecanismo antigo de envio por notificação da v2.1.40.
# A captura do WhatsApp Business e todas as demais funções do listener continuam.
s=svc.read_text(encoding='utf-8')
s=s.replace('startTelegramApiAutoDeliveryLoop();','/* v2.1.41: entrega Telegram agora é feita pelo AccessibilityService */',1)
svc.write_text(s,encoding='utf-8')

# 5) Versão. Não toca em TelegramApiAlbumService.java.
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 112',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.41'",G,count=1)
gradle.write_text(G,encoding='utf-8')
D=dash.read_text(encoding='utf-8')
D=re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.41",10,MUTED,false));',D,count=1)
dash.write_text(D,encoding='utf-8')

checks=[
 (java/'TelegramWhatsAppAccessibilityService.java','com.whatsapp.w4b'),
 (java/'TelegramWhatsAppAccessibilityService.java','ACTION_SEND_MULTIPLE'),
 (java/'TelegramWhatsAppAccessibilityService.java','TelegramAutoDeliveryStore.finish'),
 (xml/'telegram_whatsapp_accessibility.xml','com.whatsapp.w4b'),
 (act,'ATIVAR ENVIO AUTOMÁTICO'),
 (manifest,'.TelegramWhatsAppAccessibilityService'),
 (gradle,"versionName '2.1.41'")
]
for pth,mark in checks:
 if mark not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.41: requisito ausente '+mark)

print('v2.1.41: envio automático por Acessibilidade criado; captura Telegram API preservada sem alterações')
