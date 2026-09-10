package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

public final class MasterIboMotor {
    private static final String PREFIX="master_ibo_flow_";
    private static final long TIMEOUT=15L*60L*1000L;
    private static final String[] APPS={"FACILITA","Gerencia Max","GPC PRO ANDROID","IBO REVENDA","IBONEW","TV ROKU - GPC PRO","UNI REVENDA","VU REVENDA","ZONE X"};
    private MasterIboMotor(){}

    public static boolean handle(Context context, Notification notification, String contactKey, String incoming, SharedPreferences prefs){
        if(context==null||notification==null||prefs==null||contactKey==null||contactKey.trim().isEmpty()) return false;
        String n=normalize(incoming), key=PREFIX+contactKey;
        Session s=load(prefs,key);
        if(s!=null&&s.updatedAt>0&&System.currentTimeMillis()-s.updatedAt>TIMEOUT){ clear(prefs,key); s=null; }
        if(s==null){
            if(!isTrigger(n)) return false;
            s=new Session(); s.step="APP"; save(prefs,key,s); reply(context,notification,menu()); return true;
        }
        if(isCancel(n)){ clear(prefs,key); reply(context,notification,"❌ Ativação IBO cancelada."); return true; }
        if("EXECUTING".equals(s.step)) return true;

        if("APP".equals(s.step)){
            String app=parseApp(incoming);
            if(app.isEmpty()){ reply(context,notification,"Opção inválida.\n\n"+menu()); return true; }
            s.appName=app; s.step="MAC"; save(prefs,key,s);
            reply(context,notification,"📺 Aplicativo: "+app+"\n\nEnvie o MAC do dispositivo.\nExemplo: 00:11:22:33:44:55");
            return true;
        }
        if("MAC".equals(s.step)){
            String mac=normalizeMac(incoming);
            if(!mac.matches("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")){ reply(context,notification,"MAC inválido. Envie no formato:\n00:11:22:33:44:55"); return true; }
            s.mac=mac; s.step="M3U"; save(prefs,key,s);
            reply(context,notification,"✅ MAC recebido.\n\nAgora envie a URL M3U completa.");
            return true;
        }
        if("M3U".equals(s.step)){
            String m3u=incoming==null?"":incoming.trim();
            String low=m3u.toLowerCase(Locale.ROOT);
            if(!(low.startsWith("http://")||low.startsWith("https://"))){ reply(context,notification,"M3U inválida. Envie uma URL começando com http:// ou https://"); return true; }
            s.m3u=m3u; s.serverName=server(m3u); s.step="EXECUTING"; save(prefs,key,s);
            final String app=s.appName,mac=s.mac,server=s.serverName,playlist=s.m3u;
            reply(context,notification,"⚙️ Processando ativação IBO...\n"+app+"\n"+mac);
            MasterIboWebAutomation.activate(context,app,mac,server,playlist,new MasterIboWebAutomation.Callback(){
                @Override public void onSuccess(String message){
                    clear(prefs,key);
                    reply(context,notification,"✅ IBO ATIVADO COM SUCESSO!\n\nAplicativo: "+app+"\nMAC: "+mac);
                }
                @Override public void onError(String code,String message){
                    clear(prefs,key);
                    String detail=message==null?"":message.trim();
                    if("CREDENTIALS_REQUIRED".equals(code)) detail="Abra MASTER IBO no menu do app, salve o login do painel e envie \"ativar ibo\" novamente.";
                    reply(context,notification,"❌ FALHA NA ATIVAÇÃO IBO"+(detail.isEmpty()?".":"\n\n"+detail));
                }
            });
            return true;
        }
        clear(prefs,key); return false;
    }

    private static boolean isTrigger(String n){ return "ativar ibo".equals(n)||n.startsWith("ativar ibo "); }
    private static boolean isCancel(String n){ return "cancelar".equals(n)||"cancelar ibo".equals(n)||"sair".equals(n); }
    private static String menu(){
        StringBuilder b=new StringBuilder("📲 MASTER IBO\nEscolha o aplicativo:\n\n");
        for(int i=0;i<APPS.length;i++) b.append(i+1).append(". ").append(APPS[i]).append(i+1<APPS.length?"\n":"");
        return b.append("\n\nEnvie somente o número da opção.\nPara sair: cancelar").toString();
    }
    private static String parseApp(String input){
        if(input==null) return ""; String v=input.trim();
        try{ int x=Integer.parseInt(v); if(x>=1&&x<=APPS.length) return APPS[x-1]; }catch(Exception ignored){}
        String n=normalize(v); for(String app:APPS) if(normalize(app).equals(n)) return app; return "";
    }
    private static String normalizeMac(String value){
        String clean=value==null?"":value.trim().toUpperCase(Locale.ROOT), hex=clean.replaceAll("[^0-9A-F]","");
        if(hex.length()!=12) return clean; StringBuilder b=new StringBuilder();
        for(int i=0;i<12;i+=2){ if(b.length()>0)b.append(':'); b.append(hex,i,i+2); } return b.toString();
    }
    private static String server(String m3u){ try{ String h=Uri.parse(m3u).getHost(); if(h!=null&&!h.trim().isEmpty())return h.trim(); }catch(Exception ignored){} return "Meu aplicativo"; }
    private static void reply(Context c,Notification n,String m){ WhatsAppReply.send(c,n,m); }

    private static void save(SharedPreferences prefs,String key,Session s){
        try{
            s.updatedAt=System.currentTimeMillis(); JSONObject o=new JSONObject();
            o.put("step",s.step); o.put("app",s.appName); o.put("mac",s.mac); o.put("m3u",s.m3u); o.put("server",s.serverName); o.put("updatedAt",s.updatedAt);
            prefs.edit().putString(key,o.toString()).apply();
        }catch(Exception ignored){}
    }
    private static Session load(SharedPreferences prefs,String key){
        String raw=prefs.getString(key,""); if(raw==null||raw.trim().isEmpty()) return null;
        try{
            JSONObject o=new JSONObject(raw); Session s=new Session();
            s.step=o.optString("step",""); s.appName=o.optString("app",""); s.mac=o.optString("mac",""); s.m3u=o.optString("m3u",""); s.serverName=o.optString("server",""); s.updatedAt=o.optLong("updatedAt",0L);
            return s.step.isEmpty()?null:s;
        }catch(Exception ignored){ return null; }
    }
    private static void clear(SharedPreferences prefs,String key){ prefs.edit().remove(key).apply(); }
    private static String normalize(String text){
        if(text==null)return "";
        return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
    }
    private static final class Session{ String step="",appName="",mac="",m3u="",serverName=""; long updatedAt=0L; }
}
