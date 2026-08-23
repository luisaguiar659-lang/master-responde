from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# Imports necessários para responder diretamente pela ação da notificação.
s = s.replace('import android.app.Notification;\n', '''import android.app.Notification;\nimport android.app.PendingIntent;\nimport android.app.RemoteInput;\nimport android.content.Intent;\n''', 1)

# Após registrar a captura, executa SOMENTE uma regra controlada de teste.
needle = '''                .putInt("capture_total", prefs().getInt("capture_total", 0) + 1)\n                .apply();\n    }\n'''
replacement = '''                .putInt("capture_total", prefs().getInt("capture_total", 0) + 1)\n                .apply();\n\n        // v2.1.2: primeira regra isolada do motor novo.\n        // Só @info dispara resposta; qualquer outra mensagem permanece apenas capturada.\n        if ("@info".equalsIgnoreCase(message.trim())) {\n            sendDirectReply(n, conversation, "MASTER RESPONDE: teste de resposta automática funcionando.");\n        }\n    }\n\n    private void sendDirectReply(Notification notification, String conversation, String replyText) {\n        Notification.Action[] actions = notification.actions;\n        if (actions == null) {\n            markReplyFailure("Falha: notificação sem ação de resposta");\n            return;\n        }\n\n        for (Notification.Action action : actions) {\n            if (action == null || action.actionIntent == null) continue;\n            RemoteInput[] inputs = action.getRemoteInputs();\n            if (inputs == null || inputs.length == 0) continue;\n\n            try {\n                Intent fillIn = new Intent();\n                Bundle results = new Bundle();\n                for (RemoteInput input : inputs) {\n                    results.putCharSequence(input.getResultKey(), replyText);\n                }\n                RemoteInput.addResultsToIntent(inputs, fillIn, results);\n                action.actionIntent.send(this, 0, fillIn);\n                prefs().edit()\n                        .putString("accessibility_last_conversation", conversation)\n                        .putString("accessibility_last_reply", replyText)\n                        .putString("accessibility_last_status", "Respondida com sucesso • RemoteInput")\n                        .putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)\n                        .apply();\n                return;\n            } catch (PendingIntent.CanceledException | RuntimeException e) {\n                markReplyFailure("Falha ao responder: " + e.getClass().getSimpleName());\n                return;\n            }\n        }\n        markReplyFailure("Falha: ação Responder não encontrada");\n    }\n\n    private void markReplyFailure(String status) {\n        prefs().edit()\n                .putString("accessibility_last_reply", "—")\n                .putString("accessibility_last_status", status)\n                .putInt("reply_fail_total", prefs().getInt("reply_fail_total", 0) + 1)\n                .apply();\n    }\n'''
if needle not in s:
    raise SystemExit('ERRO v2.1.2: ponto de integração da captura não encontrado')
s = s.replace(needle, replacement, 1)
service.write_text(s, encoding='utf-8')

g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 73', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.2'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final = service.read_text(encoding='utf-8')
for required in ['"@info".equalsIgnoreCase', 'RemoteInput.addResultsToIntent', 'action.actionIntent.send', 'com.whatsapp.w4b']:
    if required not in final:
        raise SystemExit('ERRO v2.1.2: integração ausente: ' + required)
print('v2.1.2: regra @info -> resposta direta via RemoteInput criada')
