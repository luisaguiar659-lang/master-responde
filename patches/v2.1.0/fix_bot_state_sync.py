from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
dash = java_dir / 'NeonDashboardActivity.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

# O listener deve consultar o estado diretamente do mesmo arquivo de preferências
# usado pelo painel a cada notificação, evitando qualquer leitura obsoleta em memória.
old = 'if (!prefs().getBoolean("bot_enabled", false)) {'
new = '''boolean botEnabledNow = getSharedPreferences("master_responde", MODE_PRIVATE)\n                .getBoolean("bot_enabled", false);\n        prefs().edit()\n                .putString("debug_bot_state", "bot_enabled=" + botEnabledNow)\n                .putLong("debug_bot_state_at", System.currentTimeMillis())\n                .apply();\n        if (!botEnabledNow) {'''
if old not in s:
    raise SystemExit('ERRO sync bot: leitura antiga de bot_enabled não encontrada no serviço')
s = s.replace(old, new, 1)
service.write_text(s, encoding='utf-8')

# No painel, gravações do estado do bot passam a ser síncronas (commit), para que o
# NotificationListenerService enxergue o novo valor antes de receber a próxima mensagem.
d = dash.read_text(encoding='utf-8')
patterns = [
    ('prefs.edit().putBoolean("bot_enabled",true).apply();', 'prefs.edit().putBoolean("bot_enabled",true).commit();'),
    ('prefs.edit().putBoolean("bot_enabled",false).apply();', 'prefs.edit().putBoolean("bot_enabled",false).commit();'),
    ('prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).apply();', 'prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).commit();'),
]
changed = 0
for oldp, newp in patterns:
    if oldp in d:
        d = d.replace(oldp, newp)
        changed += 1
if changed < 2:
    raise SystemExit('ERRO sync bot: controles ATIVAR/PAUSAR do painel não encontrados')

# Mostra no status interno a última leitura real para diagnóstico, sem alterar o layout.
# O valor fica em SharedPreferences e pode ser consultado nos próximos testes.
dash.write_text(d, encoding='utf-8')

# Nova versão de teste para Xiaomi.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 110', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.39'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final_s = service.read_text(encoding='utf-8')
final_d = dash.read_text(encoding='utf-8')
for required in ['botEnabledNow', 'debug_bot_state', 'getSharedPreferences("master_responde", MODE_PRIVATE)', 'putBoolean("bot_enabled",true).commit()', 'putBoolean("bot_enabled",false).commit()']:
    if required not in final_s + final_d:
        raise SystemExit('ERRO sync bot: requisito ausente: ' + required)

print('v2.1.39: estado do bot sincronizado entre Dashboard e NotificationListenerService')
