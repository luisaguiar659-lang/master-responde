from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
manifest = app / 'src/main/AndroidManifest.xml'
gradle = app / 'build.gradle'

# Mantemos SOMENTE o Dashboard/Configurações Neon e apagamos todo o núcleo antigo.
keep = {'NeonDashboardActivity.java', 'NeonSettingsActivity.java'}
for f in java_dir.glob('*.java'):
    if f.name not in keep:
        f.unlink()

# Dashboard: remove qualquer dependência dos motores antigos.
dash = java_dir / 'NeonDashboardActivity.java'
d = dash.read_text(encoding='utf-8')
d = d.replace('ac=WhatsAppAccessibilityService.isEnabled(this),no=isNotificationAccessEnabled();',
              'ac=false,no=isNotificationAccessEnabled();')
d = d.replace('ac=isNotificationAccessEnabled(),no=isNotificationAccessEnabled();',
              'ac=false,no=isNotificationAccessEnabled();')
d = d.replace('BackgroundRuntime.requestListenerRebind(this);', '')
d = d.replace('prefs.getBoolean(BackgroundRuntime.KEY_LISTENER_CONNECTED,false)', 'false')
d = d.replace('prefs.getBoolean(BackgroundRuntime.KEY_LISTENER_CONNECTED, false)', 'false')
# Card do motor mostra NOVO NÚCLEO parado até implementarmos os módulos.
d = d.replace('set(engine,on?"ATIVO":"PAUSADO",on);', 'set(engine,"EM CONSTRUÇÃO",false);')
# Botões apenas controlam a intenção do usuário; nenhum envio é executado ainda.
d = d.replace('mode.setText(on?"MODO ATUAL:  AUTOMÁTICO":"MODO ATUAL:  PAUSADO");',
              'mode.setText(on?"MODO ATUAL:  PREPARADO":"MODO ATUAL:  PAUSADO");')
dash.write_text(d, encoding='utf-8')

# Configurações: remove atalhos para telas antigas que foram apagadas.
settings = java_dir / 'NeonSettingsActivity.java'
s = settings.read_text(encoding='utf-8')
s = re.sub(
    r'private void openMessages\(\)\{.*?\}\n\s*private void section',
    'private void openMessages(){Toast.makeText(this,"Novo módulo de comandos em construção",Toast.LENGTH_SHORT).show();}\n    private void section',
    s,
    flags=re.S,
)
settings.write_text(s, encoding='utf-8')

# Novo núcleo: criado do zero, deliberadamente sem automação ativa nesta etapa.
(java_dir / 'AutomationEngine.java').write_text('''package com.masterresponde.app;\n\nimport android.content.Context;\nimport android.content.SharedPreferences;\n\npublic final class AutomationEngine {\n    public enum State { STOPPED, READY, RUNNING, ERROR }\n    private static final String PREFS = "master_responde";\n    private static final String KEY_STATE = "new_engine_state";\n    private AutomationEngine() {}\n    public static State getState(Context c) {\n        String v = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATE, State.STOPPED.name());\n        try { return State.valueOf(v); } catch (Throwable ignored) { return State.STOPPED; }\n    }\n    public static void setState(Context c, State state) {\n        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_STATE, state.name()).apply();\n    }\n    public static void reset(Context c) { setState(c, State.STOPPED); }\n}\n''', encoding='utf-8')

(java_dir / 'AutomationEvent.java').write_text('''package com.masterresponde.app;\n\npublic final class AutomationEvent {\n    public final String sourcePackage;\n    public final String conversation;\n    public final String text;\n    public final long timestamp;\n    public AutomationEvent(String sourcePackage, String conversation, String text, long timestamp) {\n        this.sourcePackage = sourcePackage == null ? "" : sourcePackage;\n        this.conversation = conversation == null ? "" : conversation;\n        this.text = text == null ? "" : text;\n        this.timestamp = timestamp;\n    }\n}\n''', encoding='utf-8')

(java_dir / 'AutomationRule.java').write_text('''package com.masterresponde.app;\n\npublic final class AutomationRule {\n    public final String trigger;\n    public final String response;\n    public final boolean enabled;\n    public AutomationRule(String trigger, String response, boolean enabled) {\n        this.trigger = trigger == null ? "" : trigger;\n        this.response = response == null ? "" : response;\n        this.enabled = enabled;\n    }\n    public boolean matches(String text) {\n        return enabled && text != null && text.trim().equalsIgnoreCase(trigger.trim());\n    }\n}\n''', encoding='utf-8')

(java_dir / 'WhatsAppBusinessGateway.java').write_text('''package com.masterresponde.app;\n\npublic final class WhatsAppBusinessGateway {\n    public static final String PACKAGE_NAME = "com.whatsapp.w4b";\n    private WhatsAppBusinessGateway() {}\n    public static boolean acceptsPackage(String packageName) {\n        return PACKAGE_NAME.equals(packageName);\n    }\n}\n''', encoding='utf-8')

# Manifest mínimo: sem NotificationListener, AccessibilityService, receivers ou automações antigas.
manifest.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>\n    <queries><package android:name="com.whatsapp.w4b"/></queries>\n    <application\n        android:allowBackup="false"\n        android:label="MASTER RESPONDE"\n        android:theme="@style/AppTheme"\n        android:icon="@drawable/ic_launcher_master_responde"\n        android:roundIcon="@drawable/ic_launcher_master_responde">\n        <activity android:name=".NeonSettingsActivity" android:screenOrientation="portrait" android:exported="false"/>\n        <activity android:name=".NeonDashboardActivity" android:screenOrientation="portrait" android:exported="true">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN"/>\n                <category android:name="android.intent.category.LAUNCHER"/>\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n''', encoding='utf-8')

# Nova linha de versão para marcar o recomeço arquitetural.
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 71', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.0'", g, count=1)
gradle.write_text(g, encoding='utf-8')

# Validações: nenhum motor antigo pode sobreviver.
for forbidden in [
    'NotificationAccessService.java','WhatsAppReply.java','BackgroundRuntime.java',
    'ServiceRecoveryReceiver.java','MasterflixBackgroundAutomation.java',
    'MasterflixResellerAutomation.java','MasterflixAutomationGate.java',
    'ResellerTaskBridge.java','TestTaskBridge.java'
]:
    if (java_dir / forbidden).exists():
        raise SystemExit('ERRO reset: motor antigo ainda existe: ' + forbidden)

m = manifest.read_text(encoding='utf-8')
for forbidden in ['NotificationListenerService','BIND_NOTIFICATION_LISTENER_SERVICE','ServiceRecoveryReceiver']:
    if forbidden in m:
        raise SystemExit('ERRO reset: referência antiga no Manifest: ' + forbidden)

print('v2.1.0 CLEAN: Dashboard Neon preservado; motores antigos removidos; novo núcleo vazio criado')
