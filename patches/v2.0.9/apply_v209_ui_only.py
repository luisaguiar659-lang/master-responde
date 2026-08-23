from pathlib import Path
import re
import shutil

root = Path('projeto')
app = root / 'app'
java_dir = app / 'src/main/java/com/masterresponde/app'
res_drawable = app / 'src/main/res/drawable'

# Este script parte DIRETAMENTE do MASTER-RESPONDE-RAIZ.zip original.
# Não aplica nem modifica o motor NotificationAccessService/WhatsAppReply.
core_listener = java_dir / 'NotificationAccessService.java'
core_reply = java_dir / 'WhatsAppReply.java'
if not core_listener.exists() or not core_reply.exists():
    raise SystemExit('ERRO: núcleo original NotificationAccessService/WhatsAppReply não encontrado')
core_listener_before = core_listener.read_bytes()
core_reply_before = core_reply.read_bytes()

# Copia somente as telas Neon.
for name in ['NeonDashboardActivity.java', 'NeonSettingsActivity.java']:
    src = Path('patches/v2.0.9') / name
    dst = java_dir / name
    shutil.copyfile(src, dst)

# Instala os seis assets 3D.
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

# Remove somente conflito antigo de recurso, caso exista.
old = res_drawable / 'mr_notifications.xml'
if old.exists():
    old.unlink()

# Integra os PNGs no Dashboard sem tocar no núcleo de respostas.
dash = java_dir / 'NeonDashboardActivity.java'
d = dash.read_text(encoding='utf-8')

def replace_once(old, new, label):
    global d
    if old not in d:
        raise SystemExit('ERRO UI Neon: ponto não encontrado: ' + label)
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
    'robo cabecalho'
)
replace_once(
    'power=new PowerView();power.setOnClickListener(v->{prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).apply();refresh();});center.addView(power,new FrameLayout.LayoutParams(dp(100),dp(100),Gravity.CENTER));',
    'power=asset(R.drawable.mr_power);power.setOnClickListener(v->{prefs.edit().putBoolean("bot_enabled",!prefs.getBoolean("bot_enabled",false)).apply();refresh();});center.addView(power,new FrameLayout.LayoutParams(dp(100),dp(100),Gravity.CENTER));',
    'power central'
)
replace_once(
    'ShieldView shield=new ShieldView();sec.addView(shield,new LinearLayout.LayoutParams(dp(82),dp(100)));',
    'ImageView shield=asset(R.drawable.mr_security);sec.addView(shield,new LinearLayout.LayoutParams(dp(88),dp(100)));',
    'escudo'
)
replace_once(
    'if(power!=null){power.active=on;power.invalidate();}',
    'if(power!=null){power.setAlpha(on?1.0f:0.55f);}',
    'estado power'
)

old_service = '''private TextView service(LinearLayout p,int kind,String label,int accent){LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(2),dp(7),dp(2),dp(7));c.setBackground(bg(CARD,BORDER,7,1));ServiceIconView icon=new ServiceIconView(kind,accent);c.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));TextView l=text(label,9,Color.WHITE,true);l.setGravity(Gravity.CENTER);c.addView(l);TextView state=text("...",9,GREEN,true);state.setGravity(Gravity.CENTER);c.addView(state);p.addView(c,new LinearLayout.LayoutParams(0,dp(112),1f));p.addView(spaceH(8));return state;}'''
new_service = '''private TextView service(LinearLayout p,int kind,String label,int accent){LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(2),dp(5),dp(2),dp(6));c.setBackground(bg(CARD,BORDER,7,1));int res=kind==0?R.drawable.mr_whatsapp_business:kind==1?R.drawable.mr_accessibility:kind==2?R.drawable.mr_notifications:R.drawable.mr_robot;ImageView icon=asset(res);c.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView l=text(label,9,Color.WHITE,true);l.setGravity(Gravity.CENTER);c.addView(l);TextView state=text("...",9,GREEN,true);state.setGravity(Gravity.CENTER);c.addView(state);p.addView(c,new LinearLayout.LayoutParams(0,dp(116),1f));p.addView(spaceH(8));return state;}'''
replace_once(old_service, new_service, 'cards de servico')

helper_anchor = 'private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}'
helper = 'private ImageView asset(int res){ImageView v=new ImageView(this);v.setImageResource(res);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setAdjustViewBounds(true);return v;} '
if helper not in d:
    if helper_anchor not in d:
        raise SystemExit('ERRO UI Neon: helper anchor ausente')
    d = d.replace(helper_anchor, helper + helper_anchor, 1)

# O projeto original responde via NotificationListener + RemoteInput, sem AccessibilityService.
# Para manter o visual do card, usamos o acesso às notificações como estado operacional.
d = d.replace(
    'ac=WhatsAppAccessibilityService.isEnabled(this),no=isNotificationAccessEnabled();',
    'ac=isNotificationAccessEnabled(),no=isNotificationAccessEnabled();',
    1
)

# Mantém Configurações Neon, com acesso às telas antigas funcionais.
d = d.replace(
    'startActivity(new Intent(this,MessageSettingsActivity.class));}catch(Throwable e){Toast.makeText(this,"Configurações indisponíveis"',
    'startActivity(new Intent(this,NeonSettingsActivity.class));}catch(Throwable e){Toast.makeText(this,"Configurações indisponíveis"',
    1
)
dash.write_text(d, encoding='utf-8')

# Atualiza somente versão visual sobre a base original 2.0.2.
gradle = app / 'build.gradle'
text = gradle.read_text(encoding='utf-8')
text = re.sub(r'versionCode\s+63\b', 'versionCode 70', text, count=1)
text = re.sub(r"versionName\s+'2\.0\.2'", "versionName '2.0.9'", text, count=1)
if "versionName '2.0.9'" not in text:
    raise SystemExit('ERRO UI Neon: versão 2.0.9 não aplicada sobre a base original')
gradle.write_text(text, encoding='utf-8')

# Dashboard Neon vira launcher; serviços originais permanecem intactos no Manifest.
manifest = app / 'src/main/AndroidManifest.xml'
m = manifest.read_text(encoding='utf-8')
activity_pattern = re.compile(r'(<activity\b[^>]*android:name="\.MainActivity"[^>]*>)(.*?)(</activity>)', re.S)
match = activity_pattern.search(m)
if not match:
    raise SystemExit('ERRO UI Neon: MainActivity original não encontrada')
body = re.sub(r'\s*<intent-filter>\s*<action\s+android:name="android\.intent\.action\.MAIN"\s*/>\s*<category\s+android:name="android\.intent\.category\.LAUNCHER"\s*/>\s*</intent-filter>', '', match.group(2), flags=re.S)
m = m[:match.start()] + match.group(1) + body + match.group(3) + m[match.end():]
if 'android:name=".NeonDashboardActivity"' not in m:
    insert='''\n        <activity android:name=".NeonDashboardActivity" android:exported="true" android:screenOrientation="portrait"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity>\n'''
    idx=m.find('<activity')
    m=m[:idx]+insert+m[idx:]
if 'android:name=".NeonSettingsActivity"' not in m:
    insert='''\n        <activity android:name=".NeonSettingsActivity" android:exported="false" android:screenOrientation="portrait" />\n'''
    idx=m.find('<activity')
    m=m[:idx]+insert+m[idx:]
manifest.write_text(m, encoding='utf-8')

# Validações fortes: Business-only já existe na base original e o core deve ficar byte-a-byte igual.
listener_final = core_listener.read_bytes()
reply_final = core_reply.read_bytes()
if listener_final != core_listener_before:
    raise SystemExit('ERRO: NotificationAccessService original foi alterado')
if reply_final != core_reply_before:
    raise SystemExit('ERRO: WhatsAppReply original foi alterado')
listener_text = core_listener.read_text(encoding='utf-8')
if 'private static final String WHATSAPP_BUSINESS = "com.whatsapp.w4b";' not in listener_text:
    raise SystemExit('ERRO: trava Business-only original ausente')
if '!WHATSAPP_BUSINESS.equals(sbn.getPackageName())' not in listener_text:
    raise SystemExit('ERRO: filtro Business-only original ausente')
if 'WhatsAppReply.send(' not in listener_text:
    raise SystemExit('ERRO: envio original via WhatsAppReply ausente')
print('v2.0.9 UI Neon aplicada DIRETAMENTE sobre o projeto original; motor de respostas preservado byte-a-byte')
