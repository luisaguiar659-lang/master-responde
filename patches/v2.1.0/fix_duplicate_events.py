from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
dash = java_dir / 'NeonDashboardActivity.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# Insere deduplicação ANTES da gravação da mensagem e do motor de comandos.
needle = '''        if (message.isEmpty()) return;\n\n        // v2.1.4: eco é descartado antes de tocar nos dados da última conversa.\n'''
replacement = '''        if (message.isEmpty()) return;\n\n        // v2.1.6: o WhatsApp pode atualizar a mesma notificação mais de uma vez.\n        // A mesma conversa+mensagem dentro de 2500 ms é um único evento lógico.\n        if (isDuplicateIncoming(conversation, message)) {\n            prefs().edit()\n                    .putString("duplicate_last_status", "Evento duplicado ignorado")\n                    .putLong("duplicate_last_at", System.currentTimeMillis())\n                    .apply();\n            return;\n        }\n\n        // v2.1.4: eco é descartado antes de tocar nos dados da última conversa.\n'''
if needle not in s:
    raise SystemExit('ERRO v2.1.6: ponto de deduplicação não encontrado')
s = s.replace(needle, replacement, 1)

# Adiciona método antes de isEcho.
marker = '    private boolean isEcho(String conversation, String message) {\n'
method = r'''    private boolean isDuplicateIncoming(String conversation, String message) {
        String conv = conversation == null ? "" : conversation.trim().toLowerCase(java.util.Locale.ROOT);
        String msg = message == null ? "" : message.trim();
        String fingerprint = conv + "\n" + msg;
        SharedPreferences p = prefs();
        String last = p.getString("last_incoming_fingerprint", "");
        long lastAt = p.getLong("last_incoming_fingerprint_at", 0L);
        long now = System.currentTimeMillis();
        long age = now - lastAt;
        if (fingerprint.equals(last) && age >= 0L && age < 2500L) {
            return true;
        }
        // commit() é intencional: fixa a trava antes de qualquer RemoteInput ser disparado.
        p.edit()
                .putString("last_incoming_fingerprint", fingerprint)
                .putLong("last_incoming_fingerprint_at", now)
                .commit();
        return false;
    }

'''
if marker not in s:
    raise SystemExit('ERRO v2.1.6: isEcho não encontrado')
s = s.replace(marker, method + marker, 1)

# Dashboard: tenta substituir placeholders/leituras antigas pelas métricas reais.
d = dash.read_text(encoding='utf-8')
for old,new in {
    'prefs.getInt("sent_total",0)': 'prefs.getInt("reply_sent_total",0)',
    'prefs.getInt("fail_total",0)': 'prefs.getInt("reply_fail_total",0)',
    'prefs.getInt("attempts_today",0)': 'prefs.getInt("attempt_total",0)',
    'prefs.getInt("attendances_today",0)': 'prefs.getInt("attendance_total",0)',
}.items():
    d = d.replace(old,new)

# Cobertura para construções do tipo row(...,"Enviadas",0) ou String.valueOf(0).
patterns = [
    (r'("Enviadas"\s*,\s*)(?:"0"|0|String\.valueOf\(0\))', r'\1String.valueOf(prefs.getInt("reply_sent_total",0))'),
    (r'("Falhas"\s*,\s*)(?:"0"|0|String\.valueOf\(0\))', r'\1String.valueOf(prefs.getInt("reply_fail_total",0))'),
    (r'("Tentativas(?: hoje)?"\s*,\s*)(?:"0"|0|String\.valueOf\(0\))', r'\1String.valueOf(prefs.getInt("attempt_total",0))'),
    (r'("Atendimentos"\s*,\s*)(?:"0"|0|String\.valueOf\(0\))', r'\1String.valueOf(prefs.getInt("attendance_total",0))'),
]
for pat,rep in patterns:
    d = re.sub(pat, rep, d)

dash.write_text(d, encoding='utf-8')

# Versão.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 77', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.6'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final = service.read_text(encoding='utf-8')
for required in ['isDuplicateIncoming(', 'last_incoming_fingerprint', 'age < 2500L', 'commit();']:
    if required not in final:
        raise SystemExit('ERRO v2.1.6: requisito ausente: ' + required)
print('v2.1.6: deduplicação 1 evento = 1 resposta e métricas consolidadas')
