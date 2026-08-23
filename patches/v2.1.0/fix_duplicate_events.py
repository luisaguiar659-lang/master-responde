from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
dash = java_dir / 'NeonDashboardActivity.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# 1) Injeta a trava antes do anti-eco e antes do motor.
anchor = '        // v2.1.4: eco é descartado antes de tocar nos dados da última conversa.\n'
if anchor not in s:
    raise SystemExit('ERRO v2.1.6: âncora do anti-eco não encontrada')

if 'if (isDuplicateIncoming(conversation, message))' not in s:
    block = '''        // v2.1.6: uma atualização repetida da mesma notificação não pode responder duas vezes.\n        if (isDuplicateIncoming(conversation, message)) {\n            prefs().edit()\n                    .putString("duplicate_last_status", "Evento duplicado ignorado")\n                    .putLong("duplicate_last_at", System.currentTimeMillis())\n                    .apply();\n            return;\n        }\n\n'''
    s = s.replace(anchor, block + anchor, 1)

# 2) Injeta o método de forma direta antes de isEcho.
method_anchor = '    private boolean isEcho(String conversation, String message) {'
if method_anchor not in s:
    raise SystemExit('ERRO v2.1.6: método isEcho não encontrado')

if 'private boolean isDuplicateIncoming(String conversation, String message)' not in s:
    method = '''    private boolean isDuplicateIncoming(String conversation, String message) {\n        String conv = conversation == null ? "" : conversation.trim().toLowerCase(java.util.Locale.ROOT);\n        String msg = message == null ? "" : message.trim();\n        String fingerprint = conv + "\\n" + msg;\n        SharedPreferences p = prefs();\n        String last = p.getString("last_incoming_fingerprint", "");\n        long lastAt = p.getLong("last_incoming_fingerprint_at", 0L);\n        long now = System.currentTimeMillis();\n        long age = now - lastAt;\n        if (fingerprint.equals(last) && age >= 0L && age < 2500L) {\n            return true;\n        }\n        p.edit()\n                .putString("last_incoming_fingerprint", fingerprint)\n                .putLong("last_incoming_fingerprint_at", now)\n                .commit();\n        return false;\n    }\n\n'''
    s = s.replace(method_anchor, method + method_anchor, 1)

service.write_text(s, encoding='utf-8')

# 3) Conecta métricas já produzidas pelo motor ao dashboard quando houver leituras antigas.
d = dash.read_text(encoding='utf-8')
metric_replacements = {
    'prefs.getInt("sent_total",0)': 'prefs.getInt("reply_sent_total",0)',
    'prefs.getInt("fail_total",0)': 'prefs.getInt("reply_fail_total",0)',
    'prefs.getInt("attempts_today",0)': 'prefs.getInt("attempt_total",0)',
    'prefs.getInt("attendances_today",0)': 'prefs.getInt("attendance_total",0)',
}
for old, new in metric_replacements.items():
    d = d.replace(old, new)

dash.write_text(d, encoding='utf-8')

# 4) Versiona.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 77', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.6'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# 5) Validação simples e objetiva.
final = service.read_text(encoding='utf-8')
checks = {
    'chamada dedup': 'if (isDuplicateIncoming(conversation, message))',
    'método dedup': 'private boolean isDuplicateIncoming(String conversation, String message)',
    'fingerprint': 'last_incoming_fingerprint',
    'janela': 'age < 2500L',
}
for name, token in checks.items():
    if token not in final:
        raise SystemExit('ERRO v2.1.6: ' + name + ' ausente')

print('v2.1.6: deduplicação aplicada com sucesso')
