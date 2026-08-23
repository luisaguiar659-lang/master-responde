from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
gradle = app / 'build.gradle'

# Novo motor de comandos separado do listener.
(java_dir / 'CommandEngine.java').write_text(r'''package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public final class CommandEngine {
    private static final String PREFS = "master_responde";
    private CommandEngine() {}

    public static String resolve(Context context, String message) {
        if (message == null) return null;
        String m = message.trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty()) return null;

        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if ("@info".equals(m)) {
            return p.getString("cmd_info_response", "MASTER RESPONDE: sistema online e funcionando.");
        }
        if ("@status".equals(m)) {
            return "MASTER RESPONDE: ONLINE • WhatsApp Business protegido • captura ativa.";
        }
        if ("@ajuda".equals(m) || "@help".equals(m)) {
            return "Comandos disponíveis: @info, @status, @ajuda";
        }
        return null;
    }
}
''', encoding='utf-8')

s = service.read_text(encoding='utf-8')

# Troca a regra fixa por motor de comandos e adiciona anti-eco forte.
old_rule = '''        // v2.1.2: primeira regra isolada do motor novo.\n        // Só @info dispara resposta; qualquer outra mensagem permanece apenas capturada.\n        if ("@info".equalsIgnoreCase(message.trim())) {\n            sendDirectReply(n, conversation, "MASTER RESPONDE: teste de resposta automática funcionando.");\n        }\n'''
new_rule = '''        // v2.1.3: ignora notificações geradas pela própria resposta do bot.\n        if (isEcho(conversation, message)) {\n            prefs().edit().putString("accessibility_last_status", "Eco ignorado • anti-loop ativo").apply();\n            return;\n        }\n\n        String resolvedReply = CommandEngine.resolve(this, message);\n        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {\n            sendDirectReply(n, conversation, resolvedReply);\n        }\n'''
if old_rule not in s:
    raise SystemExit('ERRO v2.1.3: regra v2.1.2 não encontrada')
s = s.replace(old_rule, new_rule, 1)

# Antes do método sendDirectReply, adiciona detector de eco.
marker = '    private void sendDirectReply(Notification notification, String conversation, String replyText) {\n'
anti = r'''    private boolean isEcho(String conversation, String message) {
        String conv = conversation == null ? "" : conversation.trim();
        String msg = message == null ? "" : message.trim();
        if ("Você".equalsIgnoreCase(conv) || "You".equalsIgnoreCase(conv)) return true;

        SharedPreferences p = prefs();
        String lastReply = p.getString("last_bot_reply_text", "");
        long lastAt = p.getLong("last_bot_reply_at", 0L);
        long age = System.currentTimeMillis() - lastAt;
        return !lastReply.isEmpty() && lastReply.equals(msg) && age >= 0L && age < 30000L;
    }

'''
if marker not in s:
    raise SystemExit('ERRO v2.1.3: método de resposta não encontrado')
s = s.replace(marker, anti + marker, 1)

# Registra a última resposta enviada para o anti-eco e atualiza métricas reais.
needle = '''                prefs().edit()\n                        .putString("accessibility_last_conversation", conversation)\n                        .putString("accessibility_last_reply", replyText)\n                        .putString("accessibility_last_status", "Respondida com sucesso • RemoteInput")\n                        .putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)\n                        .apply();\n'''
replacement = '''                prefs().edit()\n                        .putString("accessibility_last_conversation", conversation)\n                        .putString("accessibility_last_reply", replyText)\n                        .putString("accessibility_last_status", "Respondida com sucesso • motor v2")\n                        .putString("last_bot_reply_text", replyText)\n                        .putLong("last_bot_reply_at", System.currentTimeMillis())\n                        .putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)\n                        .putInt("attempt_total", prefs().getInt("attempt_total", 0) + 1)\n                        .apply();\n'''
if needle not in s:
    raise SystemExit('ERRO v2.1.3: bloco de sucesso não encontrado')
s = s.replace(needle, replacement, 1)

# Falhas também contam tentativa.
s = s.replace('.putInt("reply_fail_total", prefs().getInt("reply_fail_total", 0) + 1)\n                .apply();',
              '.putInt("reply_fail_total", prefs().getInt("reply_fail_total", 0) + 1)\n                .putInt("attempt_total", prefs().getInt("attempt_total", 0) + 1)\n                .apply();', 1)
service.write_text(s, encoding='utf-8')

# Versiona.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 74', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.3'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final = service.read_text(encoding='utf-8')
for required in ['CommandEngine.resolve', 'isEcho(', 'last_bot_reply_text', 'RemoteInput.addResultsToIntent']:
    if required not in final:
        raise SystemExit('ERRO v2.1.3: requisito ausente: ' + required)
print('v2.1.3: anti-eco ativo e motor inicial de comandos criado (@info, @status, @ajuda)')
