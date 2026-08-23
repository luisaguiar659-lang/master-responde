from pathlib import Path
import re
import shutil

root = Path('projeto')
app = root / 'app'
java_dir = app / 'src/main/java/com/masterresponde/app'
res_xml = app / 'src/main/res/xml'

# 1) Copia o novo dashboard.
src = Path('patches/v2.0.9/NeonDashboardActivity.java')
dst = java_dir / 'NeonDashboardActivity.java'
dst.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(src, dst)

# 2) Atualiza versão.
gradle = app / 'build.gradle'
text = gradle.read_text(encoding='utf-8')
text = re.sub(r'versionCode\s+69\b', 'versionCode 70', text, count=1)
text = re.sub(r"versionName\s+'2\.0\.8'", "versionName '2.0.9'", text, count=1)
if "versionName '2.0.9'" not in text:
    raise SystemExit('ERRO: não foi possível atualizar versionName para 2.0.9')
gradle.write_text(text, encoding='utf-8')

# 3) Trava AccessibilityService exclusivamente no WhatsApp Business.
svc = java_dir / 'WhatsAppAccessibilityService.java'
text = svc.read_text(encoding='utf-8')
text = re.sub(r'\n\s*private static final String WHATSAPP = "com\.whatsapp";\s*', '\n', text, count=1)
text = text.replace(
    'return WHATSAPP_BUSINESS.equals(packageName)\n                || WHATSAPP.equals(packageName);',
    'return WHATSAPP_BUSINESS.equals(packageName);'
)
# Remove referências residuais a IDs do WhatsApp normal em arrays.
text = re.sub(r'\n\s*WHATSAPP \+ ":id/[^\"]+",?', '', text)

# Segunda trava na hora de enviar: a janela ativa também precisa ser Business.
needle = '        AccessibilityNodeInfo root =\n                getRootInActiveWindow();\n'
guard = '''        AccessibilityNodeInfo root =\n                getRootInActiveWindow();\n\n        if (root != null) {\n            CharSequence activePackage = root.getPackageName();\n            if (activePackage == null\n                    || !WHATSAPP_BUSINESS.contentEquals(activePackage)) {\n                updateReplyStatus(\n                        "Envio bloqueado: somente WhatsApp Business"\n                );\n                finishReplyAndContinue();\n                return;\n            }\n        }\n'''
if 'Envio bloqueado: somente WhatsApp Business' not in text:
    pos = text.find('    private void sendReplyNow(')
    if pos < 0:
        raise SystemExit('ERRO: sendReplyNow não encontrado')
    after = text.find(needle, pos)
    if after < 0:
        raise SystemExit('ERRO: raiz da janela em sendReplyNow não encontrada')
    text = text[:after] + text[after:].replace(needle, guard, 1)

if '|| WHATSAPP.equals(packageName)' in text or 'WHATSAPP + ":id/' in text:
    raise SystemExit('ERRO: referência ao WhatsApp normal permaneceu na acessibilidade')
svc.write_text(text, encoding='utf-8')

cfg = res_xml / 'accessibility_service_config.xml'
xml = cfg.read_text(encoding='utf-8')
xml = re.sub(r'android:packageNames="[^"]*"', 'android:packageNames="com.whatsapp.w4b"', xml)
if 'com.whatsapp,' in xml or ',com.whatsapp' in xml:
    raise SystemExit('ERRO: accessibility_service_config ainda permite WhatsApp normal')
cfg.write_text(xml, encoding='utf-8')

# 4) Novo dashboard vira launcher; MainActivity permanece para configurações.
manifest = app / 'src/main/AndroidManifest.xml'
m = manifest.read_text(encoding='utf-8')

# Remove MAIN/LAUNCHER da MainActivity sem remover a Activity.
activity_pattern = re.compile(
    r'(<activity\b[^>]*android:name="\.MainActivity"[^>]*>)(.*?)(</activity>)',
    re.S
)
match = activity_pattern.search(m)
if not match:
    raise SystemExit('ERRO: activity .MainActivity não encontrada no Manifest')
body = match.group(2)
body = re.sub(
    r'\s*<intent-filter>\s*<action\s+android:name="android\.intent\.action\.MAIN"\s*/>\s*<category\s+android:name="android\.intent\.category\.LAUNCHER"\s*/>\s*</intent-filter>',
    '',
    body,
    flags=re.S
)
m = m[:match.start()] + match.group(1) + body + match.group(3) + m[match.end():]

if 'android:name=".NeonDashboardActivity"' not in m:
    insert = '''\n        <activity\n            android:name=".NeonDashboardActivity"\n            android:exported="true"\n            android:screenOrientation="portrait">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN"/>\n                <category android:name="android.intent.category.LAUNCHER"/>\n            </intent-filter>\n        </activity>\n'''
    marker = '<activity'
    idx = m.find(marker)
    if idx < 0:
        raise SystemExit('ERRO: nenhum ponto de inserção de Activity encontrado')
    m = m[:idx] + insert + m[idx:]

manifest.write_text(m, encoding='utf-8')

# 5) Validações de segurança e launcher.
if 'android:packageNames="com.whatsapp.w4b"' not in cfg.read_text(encoding='utf-8'):
    raise SystemExit('ERRO: trava Business-only ausente no XML')
if 'NeonDashboardActivity' not in manifest.read_text(encoding='utf-8'):
    raise SystemExit('ERRO: dashboard neon ausente do Manifest')

print('v2.0.9 aplicada com sucesso')
