from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
dash = java_dir / 'NeonDashboardActivity.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# Move a checagem de eco para ANTES de gravar conversa/mensagem no Dashboard.
old = '''        if (message.isEmpty()) return;\n\n        long now = System.currentTimeMillis();\n        prefs().edit()\n                .putBoolean(KEY_CONNECTED, true)\n                .putString("accessibility_last_conversation", conversation)\n                .putString("accessibility_last_message", message)\n                .putString("accessibility_last_reply", "—")\n                .putString("accessibility_last_status", "Mensagem capturada • sem resposta automática")\n                .putLong("capture_last_timestamp", now)\n                .putInt("capture_total", prefs().getInt("capture_total", 0) + 1)\n                .apply();\n\n        // v2.1.3: ignora notificações geradas pela própria resposta do bot.\n        if (isEcho(conversation, message)) {\n            prefs().edit().putString("accessibility_last_status", "Eco ignorado • anti-loop ativo").apply();\n            return;\n        }\n\n        String resolvedReply = CommandEngine.resolve(this, message);\n'''
new = '''        if (message.isEmpty()) return;\n\n        // v2.1.4: eco é descartado antes de tocar nos dados da última conversa.\n        if (isEcho(conversation, message)) {\n            prefs().edit()\n                    .putString("anti_loop_last_status", "Eco ignorado • anti-loop ativo")\n                    .putLong("anti_loop_last_at", System.currentTimeMillis())\n                    .apply();\n            return;\n        }\n\n        long now = System.currentTimeMillis();\n        prefs().edit()\n                .putBoolean(KEY_CONNECTED, true)\n                .putString("accessibility_last_conversation", conversation)\n                .putString("accessibility_last_message", message)\n                .putString("accessibility_last_reply", "—")\n                .putString("accessibility_last_status", "Mensagem capturada • aguardando motor")\n                .putLong("capture_last_timestamp", now)\n                .putInt("capture_total", prefs().getInt("capture_total", 0) + 1)\n                .apply();\n\n        String resolvedReply = CommandEngine.resolve(this, message);\n'''
if old not in s:
    raise SystemExit('ERRO v2.1.4: bloco de captura/eco não encontrado')
s = s.replace(old, new, 1)

# Em sucesso, mantém a conversa do remetente e contabiliza atendimento.
s = s.replace('.putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)\n                        .putInt("attempt_total", prefs().getInt("attempt_total", 0) + 1)',
              '.putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)\n                        .putInt("attempt_total", prefs().getInt("attempt_total", 0) + 1)\n                        .putInt("attendance_total", prefs().getInt("attendance_total", 0) + 1)', 1)
service.write_text(s, encoding='utf-8')

# Liga as métricas reais aos campos do Dashboard.
d = dash.read_text(encoding='utf-8')
# Substituições tolerantes para placeholders zerados/antigos.
d = d.replace('prefs.getInt("pending_total",0)', '0')
# Tenta substituir leituras/valores comuns se já existirem.
replacements = {
    'prefs.getInt("sent_total",0)': 'prefs.getInt("reply_sent_total",0)',
    'prefs.getInt("fail_total",0)': 'prefs.getInt("reply_fail_total",0)',
    'prefs.getInt("attempts_today",0)': 'prefs.getInt("attempt_total",0)',
    'prefs.getInt("attendances_today",0)': 'prefs.getInt("attendance_total",0)',
}
for a,b in replacements.items():
    d = d.replace(a,b)

# Se o Dashboard usa textos fixos, injeta contadores via regex nos labels já existentes.
d = re.sub(r'("Enviadas"\s*,\s*)"?0"?', r'\1String.valueOf(prefs.getInt("reply_sent_total",0))', d)
d = re.sub(r'("Falhas"\s*,\s*)"?0"?', r'\1String.valueOf(prefs.getInt("reply_fail_total",0))', d)
d = re.sub(r'("Tentativas"\s*,\s*)"?0"?', r'\1String.valueOf(prefs.getInt("attempt_total",0))', d)
d = re.sub(r'("Atendimentos"\s*,\s*)"?0"?', r'\1String.valueOf(prefs.getInt("attendance_total",0))', d)

dash.write_text(d, encoding='utf-8')

# Versão
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 75', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.4'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final = service.read_text(encoding='utf-8')
if final.find('if (isEcho(conversation, message))') > final.find('.putString("accessibility_last_conversation", conversation)'):
    raise SystemExit('ERRO v2.1.4: anti-eco ainda está depois da gravação')
print('v2.1.4: eco descartado antes do dashboard e métricas reais conectadas')
