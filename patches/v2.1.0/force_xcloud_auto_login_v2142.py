from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
activity=java/'MasterXCloudActivity.java'
bg=java/'MasterXCloudBackgroundAutomation.java'
gradle=app/'build.gradle'

a=activity.read_text(encoding='utf-8')
old='''        email.setText(savedEmail);\n        showLogin();'''
new='''        email.setText(savedEmail);\n        showLogin();\n        // Se as credenciais já foram validadas uma vez, entra no painel automaticamente.\n        try {\n            String savedPassword = new SecureStore(this).get("master_xcloud_password");\n            if (savedEmail != null && !savedEmail.trim().isEmpty() && savedPassword != null && !savedPassword.isEmpty()) {\n                password.setText(savedPassword);\n                ui.postDelayed(this::login, 250L);\n            }\n        } catch (Throwable ignored) {}'''
if old not in a:
    raise SystemExit('ERRO v2.1.42: ponto de auto-login da tela não encontrado')
a=a.replace(old,new,1)
activity.write_text(a,encoding='utf-8')

b=bg.read_text(encoding='utf-8')
if 'private static final String LOGIN=' not in b:
    b=b.replace('private static final String DEVICES="https://panel-v2.xtream.cloud/dashboard/devices";',
                'private static final String LOGIN="https://panel.xtream.cloud/#/login";\n    private static final String DEVICES="https://panel-v2.xtream.cloud/dashboard/devices";',1)

old_load='''            web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());'''
new_load='''            // Sempre começa pela rota de login. Se a sessão ainda for válida, o painel\n            // redireciona sozinho; se tiver expirado, o script injeta e-mail/senha automaticamente.\n            web.loadUrl(LOGIN+"?t="+System.currentTimeMillis());'''
if old_load not in b:
    raise SystemExit('ERRO v2.1.42: carga inicial do executor XCloud não encontrada')
b=b.replace(old_load,new_load,1)

old_dash='''        if(low.contains("/dashboard")&&!low.contains("/login"))authenticated=true;\n        if(!authenticated)return;'''
new_dash='''        if(low.contains("/dashboard")&&!low.contains("/login")){\n            boolean firstAuth=!authenticated;\n            authenticated=true;\n            // Após confirmar o login, sempre entra na rota funcional validada do motor.\n            if(firstAuth && !(low.contains("panel-v2.xtream.cloud")&&low.contains("/dashboard/devices"))){\n                web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());\n                return;\n            }\n        }\n        if(!authenticated)return;'''
if old_dash not in b:
    raise SystemExit('ERRO v2.1.42: detecção de dashboard no executor XCloud não encontrada')
b=b.replace(old_dash,new_dash,1)
bg.write_text(b,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 113',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.42'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (activity,'ui.postDelayed(this::login, 250L)'),
 (bg,'private static final String LOGIN="https://panel.xtream.cloud/#/login"'),
 (bg,'web.loadUrl(LOGIN+"?t="+System.currentTimeMillis())'),
 (bg,'boolean firstAuth=!authenticated'),
 (gradle,"versionName '2.1.42'")]
for p,m in checks:
    if m not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.42: requisito ausente '+m)
print('v2.1.42: Master XCloud força validação/login automático antes de cada operação do WhatsApp')
