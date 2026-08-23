from pathlib import Path
import re

svc = Path('projeto/app/src/main/java/com/masterresponde/app/WhatsAppAccessibilityService.java')
text = svc.read_text(encoding='utf-8')

# Descobre a assinatura de sendReplyNow e usa o primeiro parâmetro String como texto da resposta.
m = re.search(r'(private\s+void\s+sendReplyNow\s*\(([^)]*)\)\s*\{)', text)
if not m:
    raise SystemExit('ERRO telemetry_dashboard: sendReplyNow não encontrado')
params = m.group(2)
sm = re.search(r'\bString\s+(\w+)', params)
if not sm:
    raise SystemExit('ERRO telemetry_dashboard: parâmetro String da resposta não encontrado')
reply_var = sm.group(1)

start_marker = 'mrTelemetryStart(' + reply_var + ');'
if start_marker not in text:
    text = text[:m.end()] + '\n        ' + start_marker + text[m.end():]

status_method = re.search(r'(private\s+void\s+updateReplyStatus\s*\(\s*String\s+(\w+)\s*\)\s*\{)', text)
if not status_method:
    status_method = re.search(r'(void\s+updateReplyStatus\s*\(\s*String\s+(\w+)\s*\)\s*\{)', text)
if not status_method:
    raise SystemExit('ERRO telemetry_dashboard: updateReplyStatus(String) não encontrado')
status_var = status_method.group(2)
status_marker = 'mrTelemetryStatus(' + status_var + ');'
if status_marker not in text:
    text = text[:status_method.end()] + '\n        ' + status_marker + text[status_method.end():]

helper = r'''
    private void mrTelemetryStart(String reply) {
        try {
            android.content.SharedPreferences p = getSharedPreferences("master_responde", MODE_PRIVATE);
            int attempts = p.getInt("stats_attempts_today", 0) + 1;
            int chats = p.getInt("stats_chats_today", 0) + 1;
            p.edit()
                    .putString("accessibility_last_reply", reply == null ? "" : reply)
                    .putString("accessibility_last_status", "Enviando resposta...")
                    .putInt("neon_pending_replies", 1)
                    .putInt("stats_attempts_today", attempts)
                    .putInt("stats_chats_today", chats)
                    .putBoolean("mr_telemetry_counted", false)
                    .putLong("mr_telemetry_started_at", System.currentTimeMillis())
                    .apply();
        } catch (Throwable ignored) {}
    }

    private void mrTelemetryStatus(String status) {
        try {
            android.content.SharedPreferences p = getSharedPreferences("master_responde", MODE_PRIVATE);
            String s = status == null ? "" : status.trim();
            android.content.SharedPreferences.Editor e = p.edit()
                    .putString("accessibility_last_status", s);

            String low = s.toLowerCase(java.util.Locale.ROOT);
            boolean success = low.contains("sucesso") || low.contains("enviad") || low.contains("respondid");
            boolean failure = low.contains("falha") || low.contains("erro") || low.contains("bloquead") || low.contains("não foi possível") || low.contains("nao foi possivel");
            boolean counted = p.getBoolean("mr_telemetry_counted", false);

            if ((success || failure) && !counted) {
                if (success) {
                    e.putInt("stats_sent_today", p.getInt("stats_sent_today", 0) + 1);
                } else {
                    e.putInt("stats_failed_today", p.getInt("stats_failed_today", 0) + 1);
                }
                e.putBoolean("mr_telemetry_counted", true);
                e.putInt("neon_pending_replies", 0);
                e.putLong("mr_telemetry_finished_at", System.currentTimeMillis());
            }
            e.apply();
        } catch (Throwable ignored) {}
    }
'''

if 'private void mrTelemetryStart(String reply)' not in text:
    pos = text.rfind('\n}')
    if pos < 0:
        raise SystemExit('ERRO telemetry_dashboard: fechamento da classe não encontrado')
    text = text[:pos] + '\n' + helper + text[pos:]

svc.write_text(text, encoding='utf-8')

final = svc.read_text(encoding='utf-8')
for marker in ['mrTelemetryStart(', 'mrTelemetryStatus(', 'accessibility_last_reply', 'accessibility_last_status', 'stats_sent_today', 'stats_failed_today']:
    if marker not in final:
        raise SystemExit('ERRO telemetry_dashboard: validação ausente ' + marker)
print('Telemetria do Dashboard conectada ao envio real de respostas')

# Também corrige o caminho de respostas privadas detectadas pela acessibilidade:
# usa a ação real de RemoteInput do WhatsApp Business quando ela estiver em cache,
# evitando depender de abrir a conversa e tocar no campo de texto.
background_fix = Path('patches/v2.0.9/background_private_reply.py')
if not background_fix.exists():
    raise SystemExit('ERRO telemetry_dashboard: background_private_reply.py ausente')
exec(compile(background_fix.read_text(encoding='utf-8'), str(background_fix), 'exec'))
