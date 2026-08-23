from pathlib import Path
import re
import shutil

root = Path('projeto')
app = root / 'app'
java_dir = app / 'src/main/java/com/masterresponde/app'
res_xml = app / 'src/main/res/xml'
res_drawable = app / 'src/main/res/drawable'

# 1) Copia as telas Neon.
for name in ['NeonDashboardActivity.java', 'NeonSettingsActivity.java']:
    src = Path('patches/v2.0.9') / name
    dst = java_dir / name
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)

# Instala os assets PNG 3D Neon locais.
res_drawable.mkdir(parents=True, exist_ok=True)
icon_src = Path('patches/v2.0.9/icon_assets')
png_icons = [
    'mr_robot.png', 'mr_security.png', 'mr_notifications.png',
    'mr_accessibility.png', 'mr_power.png', 'mr_whatsapp_business.png',
]
for name in png_icons:
    src = icon_src / name
    if not src.exists():
        raise SystemExit('ERRO: asset PNG ausente: ' + name)
    shutil.copyfile(src, res_drawable / name)

# Mantém Material Icons como fallback com nomes que não colidem com os PNGs.
material_src = Path('patches/v2.0.9/material_icons')
material_map = {
    'accessibility_new.xml': 'mr_material_accessibility_new.xml',
    'notifications.xml': 'mr_material_notifications.xml',
    'smart_toy.xml': 'mr_material_smart_toy.xml',
}
for src_name, dst_name in material_map.items():
    src = material_src / src_name
    if src.exists():
        shutil.copyfile(src, res_drawable / dst_name)
for old_name in ['mr_notifications.xml']:
    old = res_drawable / old_name
    if old.exists(): old.unlink()

# Integra os PNGs 3D no Dashboard.
dash = java_dir / 'NeonDashboardActivity.java'
d = dash.read_text(encoding='utf-8')

def replace_once(old, new, label):
    global d
    if old not in d:
        raise SystemExit('ERRO: não foi possível integrar asset no Dashboard: ' + label)
    d = d.replace(old, new, 1)

if 'import android.widget.ImageView;' not in d:
    d = d.replace('import android.widget.FrameLayout;\n', 'import android.widget.FrameLayout;\nimport android.widget.ImageView;\n', 1)

replace_once(
    'private TextView wa,acc,notif,engine,mode,lastChat,lastMsg,lastReply,lastStatus,pending,sent,fail,attempts,today,totalSent,rate,totalFail,online; private PowerView power; private RingView queueRing,successRing;',
    'private TextView wa,acc,notif,engine,mode,lastChat,lastMsg,lastReply,lastStatus,pending,sent,fail,attempts,today,totalSent,rate,totalFail,online; private ImageView power; private RingView queueRing,successRing;',
    'campo power'
)
replace_once(
    'RobotView robot=new RobotView();head.addView(robot,new LinearLayout.LayoutParams(dp(60),dp(60)));',
    'ImageView robot=asset(R.drawable.mr_robot);head.addView(robot,new LinearLayout.LayoutParams(dp(64),dp(64)));',
    'robô do cabeçalho'
)
replace_once(
    'power=new PowerView();power.setOnClickListener(v->{prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).apply();refresh();});center.addView(power,new FrameLayout.LayoutParams(dp(100),dp(100),Gravity.CENTER));',
    'power=asset(R.drawable.mr_power);power.setOnClickListener(v->{prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).apply();refresh();});center.addView(power,new FrameLayout.LayoutParams(dp(100),dp(100),Gravity.CENTER));',
    'Power central'
)
replace_once(
    'ShieldView shield=new ShieldView();sec.addView(shield,new LinearLayout.LayoutParams(dp(82),dp(100)));',
    'ImageView shield=asset(R.drawable.mr_security);sec.addView(shield,new LinearLayout.LayoutParams(dp(88),dp(100)));',
    'escudo de segurança'
)
replace_once(
    'if(power!=null){power.active=on;power.invalidate();}',
    'if(power!=null){power.setAlpha(on?1.0f:0.55f);}',
    'estado visual do Power'
)

# Troca os desenhos dos quatro cards por assets PNG 3D.
old_service = '''private TextView service(LinearLayout p,int kind,String label,int accent){LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(2),dp(7),dp(2),dp(7));c.setBackground(bg(CARD,BORDER,7,1));ServiceIconView icon=new ServiceIconView(kind,accent);c.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));TextView l=text(label,9,Color.WHITE,true);l.setGravity(Gravity.CENTER);c.addView(l);TextView state=text("...",9,GREEN,true);state.setGravity(Gravity.CENTER);c.addView(state);p.addView(c,new LinearLayout.LayoutParams(0,dp(112),1f));p.addView(spaceH(8));return state;}'''
new_service = '''private TextView service(LinearLayout p,int kind,String label,int accent){LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(2),dp(5),dp(2),dp(6));c.setBackground(bg(CARD,BORDER,7,1));int res=kind==0?R.drawable.mr_whatsapp_business:kind==1?R.drawable.mr_accessibility:kind==2?R.drawable.mr_notifications:R.drawable.mr_robot;ImageView icon=asset(res);c.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView l=text(label,9,Color.WHITE,true);l.setGravity(Gravity.CENTER);c.addView(l);TextView state=text("...",9,GREEN,true);state.setGravity(Gravity.CENTER);c.addView(state);p.addView(c,new LinearLayout.LayoutParams(0,dp(116),1f));p.addView(spaceH(8));return state;}'''
replace_once(old_service, new_service, 'ícones dos serviços')

# Adiciona helper de ImageView local ao APK.
helper_anchor = 'private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}'
helper = 'private ImageView asset(int res){ImageView v=new ImageView(this);v.setImageResource(res);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setAdjustViewBounds(true);return v;} '
if helper not in d:
    if helper_anchor not in d:
        raise SystemExit('ERRO: ponto de inserção do helper de assets não encontrado')
    d = d.replace(helper_anchor, helper + helper_anchor, 1)

# Configurações Neon obrigatórias.
d = d.replace(
    'startActivity(new Intent(this,MessageSettingsActivity.class));}catch(Throwable e){Toast.makeText(this,"Configurações indisponíveis"',
    'startActivity(new Intent(this,NeonSettingsActivity.class));}catch(Throwable e){Toast.makeText(this,"Configurações indisponíveis"',
    1
)
dash.write_text(d, encoding='utf-8')

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
text = text.replace('return WHATSAPP_BUSINESS.equals(packageName)\n                || WHATSAPP.equals(packageName);','return WHATSAPP_BUSINESS.equals(packageName);')
text = re.sub(r'\n\s*WHATSAPP \+ ":id/[^\"]+",?', '', text)
needle = '        AccessibilityNodeInfo root =\n                getRootInActiveWindow();\n'
guard = '''        AccessibilityNodeInfo root =\n                getRootInActiveWindow();\n\n        if (root != null) {\n            CharSequence activePackage = root.getPackageName();\n            if (activePackage == null\n                    || !WHATSAPP_BUSINESS.contentEquals(activePackage)) {\n                updateReplyStatus("Envio bloqueado: somente WhatsApp Business");\n                finishReplyAndContinue();\n                return;\n            }\n        }\n'''
if 'Envio bloqueado: somente WhatsApp Business' not in text:
    pos = text.find('    private void sendReplyNow(')
    if pos < 0: raise SystemExit('ERRO: sendReplyNow não encontrado')
    after = text.find(needle, pos)
    if after < 0: raise SystemExit('ERRO: raiz da janela em sendReplyNow não encontrada')
    text = text[:after] + text[after:].replace(needle, guard, 1)
if '|| WHATSAPP.equals(packageName)' in text or 'WHATSAPP + ":id/' in text:
    raise SystemExit('ERRO: referência ao WhatsApp normal permaneceu na acessibilidade')
svc.write_text(text, encoding='utf-8')

cfg = res_xml / 'accessibility_service_config.xml'
xml = cfg.read_text(encoding='utf-8')
xml = re.sub(r'android:packageNames="[^"]*"', 'android:packageNames="com.whatsapp.w4b"', xml)
if 'com.whatsapp,' in xml or ',com.whatsapp' in xml: raise SystemExit('ERRO: accessibility_service_config ainda permite WhatsApp normal')
cfg.write_text(xml, encoding='utf-8')

# 4) Dashboard Neon vira launcher.
manifest = app / 'src/main/AndroidManifest.xml'
m = manifest.read_text(encoding='utf-8')
activity_pattern = re.compile(r'(<activity\b[^>]*android:name="\.MainActivity"[^>]*>)(.*?)(</activity>)', re.S)
match = activity_pattern.search(m)
if not match: raise SystemExit('ERRO: activity .MainActivity não encontrada no Manifest')
body = re.sub(r'\s*<intent-filter>\s*<action\s+android:name="android\.intent\.action\.MAIN"\s*/>\s*<category\s+android:name="android\.intent\.category\.LAUNCHER"\s*/>\s*</intent-filter>','',match.group(2),flags=re.S)
m = m[:match.start()] + match.group(1) + body + match.group(3) + m[match.end():]
if 'android:name=".NeonDashboardActivity"' not in m:
    insert='''\n        <activity android:name=".NeonDashboardActivity" android:exported="true" android:screenOrientation="portrait"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity>\n'''
    idx=m.find('<activity'); m=m[:idx]+insert+m[idx:]
if 'android:name=".NeonSettingsActivity"' not in m:
    insert='''\n        <activity android:name=".NeonSettingsActivity" android:exported="false" android:screenOrientation="portrait" />\n'''
    idx=m.find('<activity'); m=m[:idx]+insert+m[idx:]
manifest.write_text(m,encoding='utf-8')

# 5) Validações.
if 'android:packageNames="com.whatsapp.w4b"' not in cfg.read_text(encoding='utf-8'): raise SystemExit('ERRO: trava Business-only ausente no XML')
if 'NeonDashboardActivity' not in manifest.read_text(encoding='utf-8'): raise SystemExit('ERRO: dashboard neon ausente do Manifest')
if 'NeonSettingsActivity' not in manifest.read_text(encoding='utf-8'): raise SystemExit('ERRO: configurações neon ausentes do Manifest')
dash_final = dash.read_text(encoding='utf-8')
if 'new Intent(this,NeonSettingsActivity.class)' not in dash_final: raise SystemExit('ERRO: painel principal ainda não aponta para Configurações Neon')
for marker in ['R.drawable.mr_robot','R.drawable.mr_security','R.drawable.mr_notifications','R.drawable.mr_accessibility','R.drawable.mr_power','R.drawable.mr_whatsapp_business']:
    if marker not in dash_final: raise SystemExit('ERRO: Dashboard não usa asset: '+marker)
for name in png_icons:
    if not (res_drawable / name).exists(): raise SystemExit('ERRO: asset PNG não instalado: ' + name)
if (res_drawable / 'mr_notifications.xml').exists(): raise SystemExit('ERRO: conflito antigo mr_notifications.xml ainda existe')
print('v2.0.9 aplicada com Dashboard usando os 6 assets PNG 3D Neon')
