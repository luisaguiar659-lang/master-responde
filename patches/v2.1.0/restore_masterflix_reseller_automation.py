from pathlib import Path
import re, zipfile

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'
zip_path=Path('MASTER-RESPONDE-RAIZ.zip')
with zipfile.ZipFile(zip_path) as z:
    reseller=z.read('app/src/main/java/com/masterresponde/app/MasterflixResellerAutomation.java').decode('utf-8')
    gate=z.read('app/src/main/java/com/masterresponde/app/MasterflixAutomationGate.java').decode('utf-8')
    login=z.read('app/src/main/java/com/masterresponde/app/MasterflixLoginAssist.java').decode('utf-8')

# Reaproveita o motor original do painel, adaptando somente as pontes removidas pelo núcleo novo.
reseller=reseller.replace('WhatsAppReply.send(', 'MasterflixDirectReply.send(')
reseller=reseller.replace('ResellersActivity.DEFAULT_SUPPORT_GROUP_LINK', 'ResellerFlow.DEFAULT_SUPPORT_GROUP_LINK')
reseller=re.sub(r'\n\s*BackgroundRuntime\.clearResellerPending\(\s*prefs\s*\);', '', reseller)
(java/'MasterflixResellerAutomation.java').write_text(reseller,encoding='utf-8')
(java/'MasterflixAutomationGate.java').write_text(gate,encoding='utf-8')
(java/'MasterflixLoginAssist.java').write_text(login,encoding='utf-8')

# Compatibilidade mínima com as mensagens que o motor original usa.
(java/'MessageSettings.java').write_text(r'''package com.masterresponde.app;
import android.app.Notification;import android.content.*;import java.util.*;
public final class MessageSettings{
 public static final String MSG_RESELLER_CONFIRMED="msg_reseller_confirmed",MSG_RESELLER_NOT_FOUND="msg_reseller_not_found";
 public static final String DEFAULT_RESELLER_CONFIRMED="✅ Cadastro de revenda confirmado!\n\nEntre no nosso grupo de suporte e solicite a ativação do seu painel.\n\n📲 *Grupo de suporte:*\n{LINK_GRUPO}";
 public static final String DEFAULT_RESELLER_NOT_FOUND="Não consegui confirmar seu cadastro de revenda no painel Sigma.\n\n{MOTIVO}";
 private MessageSettings(){}
 public static Map<String,String> variables(String... x){Map<String,String> m=new HashMap<>();for(int i=0;i+1<x.length;i+=2)m.put(x[i],x[i+1]);return m;}
 public static boolean send(Context c,Notification n,SharedPreferences p,String key,String def){return send(c,n,p,key,def,new HashMap<>());}
 public static boolean send(Context c,Notification n,SharedPreferences p,String key,String def,Map<String,String> vars){String text=p.getString(key,def);for(Map.Entry<String,String> e:vars.entrySet())text=text.replace("{"+e.getKey()+"}",e.getValue()==null?"":e.getValue());return MasterflixDirectReply.send(c,n,text);}
}''',encoding='utf-8')

(java/'ResellerFlow.java').write_text(r'''package com.masterresponde.app;
import android.app.Notification;import android.content.*;import java.text.Normalizer;import java.util.*;import java.util.regex.*;
public final class ResellerFlow{
 public static final String DEFAULT_SIGNUP_LINK="https://masterflix.sigmab.pro/#/rs/en12xeNDPE/ryJDz2KDge";
 public static final String DEFAULT_SUPPORT_GROUP_LINK="https://chat.whatsapp.com/IWHogXDJTem2VgJTJhsTA0?s=cl&p=a&mlu=0&ilr=1";
 private static final Pattern EMAIL=Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",Pattern.CASE_INSENSITIVE);
 private ResellerFlow(){}
 private static String norm(String s){if(s==null)return "";return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
 public static String key(String conversation){return norm(conversation).replaceAll("[^a-z0-9]+","_");}
 public static boolean isTrigger(Context c,String msg){String raw=c.getSharedPreferences("master_responde",0).getString("masterflix_reseller_triggers","quero ser revenda\nquero ser uma revenda\nquero revenda\nser revenda");String m=norm(msg);for(String x:raw.split("\\r?\\n"))if(!x.trim().isEmpty()&&m.equals(norm(x)))return true;return false;}
 public static boolean isCancel(String msg){return "cancelar".equals(norm(msg));}
 public static boolean validEmail(String msg){return msg!=null&&EMAIL.matcher(msg.trim()).matches();}
 public static boolean handle(Context c,Notification n,SharedPreferences p,String conversation,String message){
  String k=key(conversation),state=p.getString("reseller_state_"+k,"");
  if("WAITING_EMAIL".equals(state)){
   if(isCancel(message)){p.edit().remove("reseller_state_"+k).putString("last_reseller_status","Revenda: fluxo cancelado").apply();MasterflixDirectReply.send(c,n,"Fluxo de revenda cancelado.");return true;}
   if(isTrigger(c,message))return true;
   if(!validEmail(message)){MasterflixDirectReply.send(c,n,"Envie somente o mesmo e-mail usado no cadastro da revenda.");return true;}
   String email=message.trim().toLowerCase(Locale.ROOT);try{new SecureStore(c).put("pending_reseller_email_"+k,email);}catch(Exception e){MasterflixDirectReply.send(c,n,"Não consegui salvar o e-mail com segurança. Tente novamente.");return true;}
   p.edit().putString("reseller_state_"+k,"SEARCHING").putString("last_reseller_status","Revenda: verificando e-mail cadastrado no painel Sigma").apply();
   MasterflixDirectReply.send(c,n,"Recebi seu e-mail. Verificando seu cadastro de revenda, aguarde...");
   boolean started=MasterflixResellerAutomation.findReseller(c,n,p,k,email);
   if(!started){p.edit().putString("reseller_state_"+k,"WAITING_EMAIL").putString("last_reseller_status","Revenda: verificação ocupada").apply();MasterflixDirectReply.send(c,n,"O MASTER RESPONDE está concluindo outra verificação. Envie o e-mail novamente em alguns instantes.");}
   return true;
  }
  if(isTrigger(c,message)){
   if("COMPLETE".equals(state)){MasterflixDirectReply.send(c,n,"✅ Este WhatsApp já possui uma revenda vinculada no MASTER RESPONDE.");return true;}
   if("REVIEW_REQUIRED".equals(state)){MasterflixDirectReply.send(c,n,"⚠️ Existe uma operação anterior dessa revenda que precisa ser conferida manualmente no painel Sigma antes de iniciar outra.");return true;}
   String link=p.getString("reseller_signup_link",DEFAULT_SIGNUP_LINK).trim();
   p.edit().putString("reseller_state_"+k,"WAITING_EMAIL").putString("last_reseller_status","Revenda: aguardando e-mail do cadastro").apply();
   MasterflixDirectReply.send(c,n,"🚀 Para ser Revenda, faça seu cadastro pelo link:\n\n"+link+"\n\nApós concluir seu cadastro, envie o e-mail usado no cadastro para ativação do painel.");return true;
  }
  return false;
 }
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
needle='''        // PRIVADO: geração automática de teste MasterFlix, restaurada do projeto original.'''
insert='''        // PRIVADO: fluxo de revenda MasterFlix restaurado do projeto original.\n        // Mantém estado por conversa: convite -> espera e-mail -> verificação no painel Sigma.\n        if (ResellerFlow.handle(this, n, prefs(), conversation, message)) {\n            return;\n        }\n\n        // PRIVADO: geração automática de teste MasterFlix, restaurada do projeto original.'''
if needle not in s: raise SystemExit('ERRO v2.1.24: ponto do fluxo privado MasterFlix não encontrado')
s=s.replace(needle,insert,1);svc.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 95',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.24'",g,count=1);gradle.write_text(g,encoding='utf-8')

for p,m in [(java/'MasterflixResellerAutomation.java','findReseller'),(java/'MasterflixResellerAutomation.java','Revenda: 50 créditos adicionados'),(java/'MasterflixAutomationGate.java','tryAcquire'),(java/'ResellerFlow.java','WAITING_EMAIL'),(svc,'ResellerFlow.handle'),(gradle,"versionName '2.1.24'")]:
 if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.24: requisito ausente '+m)
if 'BackgroundRuntime.clearResellerPending' in (java/'MasterflixResellerAutomation.java').read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.24: dependência antiga BackgroundRuntime permaneceu')
print('v2.1.24: fluxo de revenda MasterFlix restaurado: gatilho, cadastro, e-mail e verificação automática')