from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
dash = java_dir / 'NeonDashboardActivity.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# 1) Bot pausado: continua capturando e mostrando a mensagem, mas não responde.
needle = '''        String resolvedReply = CommandEngine.resolve(this, message);\n        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {\n            sendDirectReply(n, conversation, resolvedReply);\n        }\n'''
replacement = '''        if (!prefs().getBoolean("bot_enabled", false)) {\n            prefs().edit()\n                    .putString("accessibility_last_status", "Mensagem capturada • bot pausado")\n                    .apply();\n            return;\n        }\n\n        String resolvedReply = CommandEngine.resolve(this, message);\n        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {\n            sendDirectReply(n, conversation, resolvedReply);\n        } else {\n            prefs().edit()\n                    .putString("accessibility_last_status", "Mensagem capturada • sem comando correspondente")\n                    .apply();\n        }\n'''
if needle not in s:
    raise SystemExit('ERRO v2.1.7: ponto de controle do bot não encontrado')
s = s.replace(needle, replacement, 1)

service.write_text(s, encoding='utf-8')

# 2) Dashboard lê diretamente os contadores reais do motor novo.
d = dash.read_text(encoding='utf-8')
old_stats = '''int p=prefs.getInt("neon_pending_replies",0),s=prefs.getInt("stats_sent_today",0),f=prefs.getInt("stats_failed_today",0),a=prefs.getInt("stats_attempts_today",0),t=prefs.getInt("stats_chats_today",0);'''
new_stats = '''int p=0,s=prefs.getInt("reply_sent_total",0),f=prefs.getInt("reply_fail_total",0),a=prefs.getInt("attempt_total",0),t=prefs.getInt("attendance_total",0);'''
if old_stats in d:
    d = d.replace(old_stats, new_stats, 1)
else:
    # fallback tolerante caso patches anteriores já tenham mexido em alguns nomes
    d = re.sub(
        r'int p=[^;]+;',
        'int p=0,s=prefs.getInt("reply_sent_total",0),f=prefs.getInt("reply_fail_total",0),a=prefs.getInt("attempt_total",0),t=prefs.getInt("attendance_total",0);',
        d,
        count=1,
    )

# 3) Estado visual do botão permanece baseado em bot_enabled e ganha status claro.
d = d.replace('mode.setText(on?"MODO ATUAL:  AUTOMÁTICO":"MODO ATUAL:  PAUSADO")',
              'mode.setText(on?"MODO ATUAL:  AUTOMÁTICO • ATIVO":"MODO ATUAL:  PAUSADO")', 1)

dash.write_text(d, encoding='utf-8')

# 4) Versiona.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 78', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.7'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final_service = service.read_text(encoding='utf-8')
final_dash = dash.read_text(encoding='utf-8')
for required in ['prefs().getBoolean("bot_enabled", false)', 'bot pausado', 'reply_sent_total', 'reply_fail_total', 'attempt_total', 'attendance_total']:
    if required not in (final_service + final_dash):
        raise SystemExit('ERRO v2.1.7: requisito ausente: ' + required)
print('v2.1.7: ATIVAR/PAUSAR controla respostas reais e dashboard lê métricas do motor novo')
