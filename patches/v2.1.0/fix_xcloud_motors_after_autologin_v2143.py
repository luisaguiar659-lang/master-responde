from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
bg=java/'MasterXCloudBackgroundAutomation.java'
gradle=app/'build.gradle'

b=bg.read_text(encoding='utf-8')

# A v2.1.42 passou a abrir diretamente a rota #/login. No painel SPA isso quebra o
# fluxo invisível em alguns aparelhos. Restauramos a estratégia que já funcionava:
# entrar primeiro na rota funcional de dispositivos; se a sessão expirou, o próprio
# painel redireciona para login e o script injeta automaticamente e-mail/senha.
old='''            // Sempre começa pela rota de login. Se a sessão ainda for válida, o painel
            // redireciona sozinho; se tiver expirado, o script injeta e-mail/senha automaticamente.
            web.loadUrl(LOGIN+"?t="+System.currentTimeMillis());'''
new='''            // Valida a sessão pela rota funcional. Se o painel pedir login, o WebView
            // injeta automaticamente as credenciais salvas e volta para dispositivos.
            web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());'''
if old not in b:
    raise SystemExit('ERRO v2.1.43: carga forçada da rota de login não encontrada')
b=b.replace(old,new,1)

# LOGIN não é mais usado pelo executor em background.
b=b.replace('    private static final String LOGIN="https://panel.xtream.cloud/#/login";\n','',1)
bg.write_text(b,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 114',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.43'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[
 (bg,'web.loadUrl(DEVICES+"?t="+System.currentTimeMillis())'),
 (bg,'MasterXCloudActivity.Scripts.login(email,password)'),
 (bg,'MasterXCloudActivity.Scripts.addDevice'),
 (bg,'MasterXCloudActivity.Scripts.deleteDevice'),
 (gradle,"versionName '2.1.43'")]
for p,m in checks:
    if m not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.43: requisito ausente '+m)
if 'web.loadUrl(LOGIN+' in bg.read_text(encoding='utf-8'):
    raise SystemExit('ERRO v2.1.43: rota direta de login ainda está ativa')
print('v2.1.43: motores XCloud restaurados; login automático ocorre somente quando o painel redireciona para autenticação')
