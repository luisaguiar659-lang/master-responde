from pathlib import Path
import re, zipfile

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'
zip_path=Path('MASTER-RESPONDE-RAIZ.zip')
old_path='app/src/main/java/com/masterresponde/app/MasterflixBackgroundAutomation.java'
with zipfile.ZipFile(zip_path) as z:
    bg=z.read(old_path).decode('utf-8')

# Adapta o motor antigo para o núcleo novo: usa reply direto atual e remove dependência do runtime antigo.
bg=bg.replace('WhatsAppReply.send(', 'MasterflixDirectReply.send(')
bg=re.sub(r'\n\s*// Finaliza também o pending persistido\..*?BackgroundRuntime\.clearTestPending\(prefs\);', '', bg, flags=re.S)
(java/'MasterflixBackgroundAutomation.java').write_text(bg,encoding='utf-8')

(java/'TestTaskBridge.java').write_text(r'''package com.masterresponde.app;
import android.app.Notification;
public final class TestTaskBridge{
 private static final Object L=new Object(); private static Notification n; private static long at; private static String key="";
 public static boolean begin(Notification x,String k){synchronized(L){long now=System.currentTimeMillis();if(n!=null&&now-at<60000L)return false;n=x;at=now;key=k==null?"":k;return true;}}
 public static Notification getNotification(){synchronized(L){return n;}}
 public static boolean hasPending(){synchronized(L){return n!=null&&System.currentTimeMillis()-at<120000L;}}
 public static String getRequestKey(){synchronized(L){return key;}}
 public static void complete(){synchronized(L){n=null;at=0L;key="";}}
}''',encoding='utf-8')

(java/'MasterflixDirectReply.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.content.*;import android.os.*;
public final class MasterflixDirectReply{
 private MasterflixDirectReply(){}
 public static boolean send(Context c,Notification n,String text){if(c==null||n==null||text==null||text.trim().isEmpty()||n.actions==null)return false;for(Notification.Action a:n.actions){if(a==null||a.actionIntent==null)continue;RemoteInput[] ins=a.getRemoteInputs();if(ins==null||ins.length==0)continue;try{Bundle b=new Bundle();for(RemoteInput i:ins)if(i!=null&&i.getAllowFreeFormInput())b.putCharSequence(i.getResultKey(),text);Intent fill=new Intent();RemoteInput.addResultsToIntent(ins,fill,b);a.actionIntent.send(c,0,fill);SharedPreferences p=c.getSharedPreferences("master_responde",Context.MODE_PRIVATE);p.edit().putString("accessibility_last_reply",text).putString("accessibility_last_status","Teste MasterFlix enviado").putString("last_bot_reply_text",text).putLong("last_bot_reply_at",System.currentTimeMillis()).putInt("stats_sent_today",p.getInt("stats_sent_today",0)+1).putInt("stats_attempts_today",p.getInt("stats_attempts_today",0)+1).putInt("stats_chats_today",p.getInt("stats_chats_today",0)+1).apply();return true;}catch(Throwable ignored){}}return false;}
}''',encoding='utf-8')

(java/'MasterflixTestTrigger.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.text.Normalizer;import java.util.*;
public final class MasterflixTestTrigger{
 private MasterflixTestTrigger(){}
 private static String norm(String s){if(s==null)return "";String x=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();return x.replaceAll("\\s+"," ");}
 public static boolean matches(Context c,String msg){String raw=c.getSharedPreferences("master_responde",Context.MODE_PRIVATE).getString("masterflix_test_triggers","teste\nquero um teste\nquero teste\ngerar teste\ngerar um teste");String m=norm(msg);for(String line:raw.split("\\r?\\n"))if(!line.trim().isEmpty()&&m.equals(norm(line)))return true;return false;}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
needle='''        // PRIVADO: somente daqui em diante entram comandos/resposta padrão.\n        String resolvedReply = CommandEngine.resolve(this, message);'''
insert='''        // PRIVADO: geração automática de teste MasterFlix, restaurada do projeto original.\n        if (MasterflixTestTrigger.matches(this, message)) {\n            String requestKey = conversation + "|" + message + "|" + System.currentTimeMillis();\n            if (TestTaskBridge.begin(n, requestKey)) {\n                boolean started = MasterflixBackgroundAutomation.start(this, n, prefs());\n                if (started) {\n                    prefs().edit().putString("last_test_status", "Teste: geração automática iniciada").apply();\n                    sendDirectReply(n, conversation, "Gerando seu teste, aguarde...");\n                } else {\n                    TestTaskBridge.complete();\n                    sendDirectReply(n, conversation, "Já existe uma geração de teste em andamento. Aguarde alguns instantes.");\n                }\n            }\n            return;\n        }\n\n        // PRIVADO: somente daqui em diante entram comandos/resposta padrão.\n        String resolvedReply = CommandEngine.resolve(this, message);'''
if needle not in s: raise SystemExit('ERRO v2.1.23: ponto privado para teste não encontrado')
s=s.replace(needle,insert,1)
svc.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 94',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.23'",g,count=1);gradle.write_text(g,encoding='utf-8')

for p,m in [(java/'MasterflixBackgroundAutomation.java','MASTERFLIX TESTE COMPLETO 1H'),(java/'MasterflixDirectReply.java','RemoteInput.addResultsToIntent'),(java/'MasterflixTestTrigger.java','quero um teste'),(svc,'MasterflixBackgroundAutomation.start'),(gradle,"versionName '2.1.23'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.23: requisito ausente '+m)
if 'BackgroundRuntime.clearTestPending' in (java/'MasterflixBackgroundAutomation.java').read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.23: dependência antiga BackgroundRuntime permaneceu')
print('v2.1.23: geração automática de teste MasterFlix restaurada e isolada no privado')