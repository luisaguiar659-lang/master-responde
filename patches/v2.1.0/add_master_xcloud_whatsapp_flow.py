from pathlib import Path
import re

app = Path('projeto/app')
java = app / 'src/main/java/com/masterresponde/app'
svc = java / 'WhatsAppBusinessCaptureService.java'
dash = java / 'NeonDashboardActivity.java'
manifest = app / 'src/main/AndroidManifest.xml'
gradle = app / 'build.gradle'

(java / 'MasterXCloudApi.java').write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MasterXCloudApi {
    public interface Callback { void onSuccess(String message); void onError(String message); }
    private static final String PREF = "master_responde";
    private static final String DEFAULT_BASE = "https://api.masterxcloud.shop";
    private MasterXCloudApi(){}

    public static void execute(Context context, String operation, String email, String password, String device, String playlist, Callback callback) {
        new Thread(() -> {
            String token = "";
            String base = DEFAULT_BASE;
            try {
                SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                base = p.getString("master_xcloud_api_base", DEFAULT_BASE);
                if (base == null || base.trim().isEmpty()) base = DEFAULT_BASE;
                base = base.trim();
                while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                if (!base.startsWith("https://")) throw new ApiException("A API Master XCloud precisa usar HTTPS.");

                JSONObject loginBody = new JSONObject();
                loginBody.put("email", email == null ? "" : email.trim());
                loginBody.put("password", password == null ? "" : password);
                HttpResult login = post(base + "/auth/login", loginBody, null, 45000);
                if (login.code == 401) throw new ApiException("E-mail ou senha inválidos.");
                if (login.code < 200 || login.code >= 300) throw new ApiException(errorText(login, "Não foi possível entrar no painel XCloud."));

                JSONObject loginJson = new JSONObject(login.body);
                token = loginJson.optString("token", "").trim();
                if (token.isEmpty()) throw new ApiException("O Master XCloud não retornou uma sessão válida.");

                JSONObject opBody = new JSONObject();
                opBody.put("device", device == null ? "" : device.trim());
                opBody.put("playlist", playlist == null ? "" : playlist.trim());
                String path = "RESET".equals(operation) ? "/operations/reset" : "/operations/activate";
                HttpResult op = post(base + path, opBody, token, 120000);
                if (op.code == 401) throw new ApiException("A sessão do painel XCloud expirou. Tente novamente.");
                if (op.code < 200 || op.code >= 300) throw new ApiException(errorText(op, "A operação Master XCloud não foi concluída."));

                JSONObject out = new JSONObject(op.body);
                if (!out.optBoolean("ok", false)) throw new ApiException(out.optString("message", "A operação não foi confirmada pelo Master XCloud."));
                callback.onSuccess(out.optString("message", "Operação concluída."));
            } catch (ApiException e) {
                callback.onError(safe(e.getMessage()));
            } catch (Throwable e) {
                callback.onError("Falha de comunicação com o Master XCloud. Tente novamente em instantes.");
            } finally {
                if (!token.isEmpty()) {
                    try { post(base + "/auth/logout", new JSONObject(), token, 15000); } catch (Throwable ignored) {}
                }
            }
        }, "MasterXCloudApi").start();
    }

    private static HttpResult post(String url, JSONObject body, String token, int readTimeout) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(readTimeout);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "MASTER-RESPONDE/2.1.38");
        if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = c.getOutputStream()) { out.write(data); }
        int code = c.getResponseCode();
        InputStream raw = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
        String text = readAll(raw);
        c.disconnect();
        return new HttpResult(code, text);
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) { if (b.length() > 0) b.append('\n'); b.append(line); }
            return b.toString();
        }
    }

    private static String errorText(HttpResult r, String fallback) {
        try {
            JSONObject j = new JSONObject(r.body == null ? "" : r.body);
            String d = j.optString("detail", "").trim(); if (!d.isEmpty()) return safe(d);
            String m = j.optString("message", "").trim(); if (!m.isEmpty()) return safe(m);
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static String safe(String s) {
        if (s == null || s.trim().isEmpty()) return "Falha na operação Master XCloud.";
        String x = s.replace('\r', ' ').replace('\n', ' ').trim();
        if (x.length() > 240) x = x.substring(0, 240);
        return x;
    }

    private static final class HttpResult { final int code; final String body; HttpResult(int code, String body){ this.code=code; this.body=body==null?"":body; } }
    private static final class ApiException extends Exception { ApiException(String message){ super(message); } }
}
''', encoding='utf-8')

(java / 'MasterXCloudFlow.java').write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public final class MasterXCloudFlow {
    public interface Responder { void send(String text); }
    private static final String PREF = "master_responde";
    private static final long FLOW_TTL = 15L * 60L * 1000L;
    private MasterXCloudFlow(){}

    private static String norm(String s){ return s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    private static String id(String conversation){ return Integer.toHexString(norm(conversation).hashCode()); }
    private static String k(String conversation,String field){ return "master_xcloud_"+field+"_"+id(conversation); }
    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREF,Context.MODE_PRIVATE); }
    private static boolean triggerActivate(String m){ return "ativar xcloud".equals(m)||"ativar master xcloud".equals(m); }
    private static boolean triggerReset(String m){ return "reset xcloud".equals(m)||"reset master xcloud".equals(m); }

    public static boolean handle(Context c,String conversation,String message,Responder responder){
        if(c==null||responder==null)return false;
        String m=norm(message); if(m.isEmpty())return false;
        SharedPreferences sp=p(c); String stateKey=k(conversation,"state"); String state=sp.getString(stateKey,"");
        long updated=sp.getLong(k(conversation,"updated"),0L);
        if(!state.isEmpty()&&updated>0L&&System.currentTimeMillis()-updated>FLOW_TTL){ clear(c,conversation); state=""; }

        if(!state.isEmpty()&&"cancelar".equals(m)){ clear(c,conversation); responder.send("❌ Operação Master XCloud cancelada."); return true; }

        if(triggerActivate(m)||triggerReset(m)){
            clear(c,conversation);
            String op=triggerReset(m)?"RESET":"ACTIVATE";
            sp.edit().putString(k(conversation,"operation"),op).putString(stateKey,"EMAIL").putLong(k(conversation,"updated"),System.currentTimeMillis()).apply();
            responder.send("📧 MASTER XCLOUD\n\nInforme o e-mail do seu painel XCloud."); return true;
        }
        if(state.isEmpty())return false;
        if("PROCESSING".equals(state)){ responder.send("⏳ Sua operação Master XCloud ainda está sendo processada. Aguarde a conclusão."); return true; }

        if("EMAIL".equals(state)){
            if(!looksLikeEmail(message)){ responder.send("📧 E-mail inválido. Envie o e-mail usado no painel XCloud."); return true; }
            sp.edit().putString(k(conversation,"email"),message.trim()).putString(stateKey,"PASSWORD").putLong(k(conversation,"updated"),System.currentTimeMillis()).apply();
            responder.send("🔐 Agora envie a senha do seu painel XCloud."); return true;
        }

        if("PASSWORD".equals(state)){
            String password=message==null?"":message.trim();
            if(password.isEmpty()){ responder.send("🔐 A senha não pode ficar vazia. Envie sua senha do painel XCloud."); return true; }
            try{ new SecureStore(c).put(k(conversation,"password"),password); }
            catch(Throwable e){ clear(c,conversation); responder.send("❌ Não consegui proteger sua senha no aparelho. Operação cancelada."); return true; }
            sp.edit().putString(stateKey,"DEVICE").putLong(k(conversation,"updated"),System.currentTimeMillis()).apply();
            responder.send("📺 Informe o Device Key / MAC do aparelho."); return true;
        }

        if("DEVICE".equals(state)){
            String device=message==null?"":message.trim().toUpperCase(Locale.ROOT);
            if(device.isEmpty()){ responder.send("📺 Informe um Device Key / MAC válido."); return true; }
            sp.edit().putString(k(conversation,"device"),device).putString(stateKey,"M3U").putLong(k(conversation,"updated"),System.currentTimeMillis()).apply();
            responder.send("🔗 Agora envie a M3U / DNS."); return true;
        }

        if("M3U".equals(state)){
            final String playlist=message==null?"":message.trim();
            if(playlist.isEmpty()){ responder.send("🔗 A M3U / DNS não pode ficar vazia."); return true; }
            final String email=sp.getString(k(conversation,"email"),"");
            final String device=sp.getString(k(conversation,"device"),"");
            final String operation=sp.getString(k(conversation,"operation"),"ACTIVATE");
            final String password;
            try{ password=new SecureStore(c).get(k(conversation,"password")); }
            catch(Throwable e){ clear(c,conversation); responder.send("❌ Não consegui recuperar sua senha protegida. Inicie novamente."); return true; }
            if(email==null||email.trim().isEmpty()||password==null||password.isEmpty()||device==null||device.trim().isEmpty()){
                clear(c,conversation); responder.send("❌ Os dados da operação ficaram incompletos. Inicie novamente."); return true;
            }
            sp.edit().putString(stateKey,"PROCESSING").putLong(k(conversation,"updated"),System.currentTimeMillis()).apply();
            responder.send("⏳ MASTER XCLOUD\n\nDados recebidos. Processando sua operação, aguarde...");
            MasterXCloudApi.execute(c,operation,email,password,device,playlist,new MasterXCloudApi.Callback(){
                @Override public void onSuccess(String apiMessage){
                    clear(c,conversation);
                    responder.send("RESET".equals(operation)?"✅ MASTER XCLOUD\n\nReset concluído com sucesso.":"✅ MASTER XCLOUD\n\nAtivação concluída com sucesso.");
                }
                @Override public void onError(String error){ clear(c,conversation); responder.send("❌ MASTER XCLOUD\n\n"+(error==null?"Não foi possível concluir a operação.":error)); }
            });
            return true;
        }
        clear(c,conversation); return false;
    }

    private static boolean looksLikeEmail(String s){
        if(s==null)return false; String x=s.trim(); int at=x.indexOf('@'),dot=x.lastIndexOf('.');
        return at>0&&dot>at+1&&dot<x.length()-1&&x.length()<=200;
    }

    private static void clear(Context c,String conversation){
        SharedPreferences sp=p(c);
        sp.edit().remove(k(conversation,"state")).remove(k(conversation,"operation")).remove(k(conversation,"email")).remove(k(conversation,"device")).remove(k(conversation,"updated")).apply();
        try{ new SecureStore(c).put(k(conversation,"password"),""); }catch(Throwable ignored){}
    }
}
''', encoding='utf-8')

s = svc.read_text(encoding='utf-8')
anchor = '        // PRIVADO: fluxo de revenda MasterFlix restaurado do projeto original.'
insert = r'''        // PRIVADO: Master XCloud é isolado e só intercepta seus próprios gatilhos
        // ou uma conversa que já esteja dentro do fluxo XCloud.
        if (MasterXCloudFlow.handle(this, conversation, message, new MasterXCloudFlow.Responder() {
            @Override public void send(String text) {
                sendDirectReply(n, conversation, text);
            }
        })) {
            return;
        }

'''
if 'MasterXCloudFlow.handle(this, conversation, message' not in s:
    if anchor not in s: raise SystemExit('ERRO v2.1.38: ponto seguro antes do fluxo MasterFlix não encontrado')
    s = s.replace(anchor, insert + anchor, 1)
svc.write_text(s, encoding='utf-8')

m = manifest.read_text(encoding='utf-8')
if 'android.permission.INTERNET' not in m:
    m = m.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">','<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.INTERNET"/>',1)
manifest.write_text(m, encoding='utf-8')

d = dash.read_text(encoding='utf-8')
d = re.sub(r'brand\.addView\(text\("v2\.1\.\d+",10,MUTED,false\)\);','brand.addView(text("v2.1.38",10,MUTED,false));',d,count=1)
dash.write_text(d, encoding='utf-8')

g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+','versionCode 109',g,count=1)
g = re.sub(r"versionName\s+'[^']+'","versionName '2.1.38'",g,count=1)
gradle.write_text(g, encoding='utf-8')

checks=[
 (java/'MasterXCloudApi.java','/auth/login'),(java/'MasterXCloudApi.java','/operations/activate'),(java/'MasterXCloudApi.java','/operations/reset'),
 (java/'MasterXCloudFlow.java','"EMAIL"'),(java/'MasterXCloudFlow.java','"PASSWORD"'),(java/'MasterXCloudFlow.java','"DEVICE"'),(java/'MasterXCloudFlow.java','"M3U"'),
 (svc,'MasterXCloudFlow.handle(this, conversation, message'),(gradle,"versionName '2.1.38'")]
for path,marker in checks:
    if marker not in path.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.38: requisito ausente '+marker)

for required in ['ResellerFlow.handle(this, n, prefs(), conversation, message)','MasterflixTestTrigger.matches(this, message)','BusinessHours.enabled(this)','IgnoredContacts.isIgnored(this, conversation)','GroupMenuStore.trigger(this)','runScheduledGroupBroadcastIfDue()']:
    if required not in svc.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.38: função existente desapareceu: '+required)

print('v2.1.38: Master XCloud isolado: email -> senha -> device -> M3U -> API; funções existentes preservadas')
