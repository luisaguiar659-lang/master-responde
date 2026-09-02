from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
bg=java/'MasterXCloudBackgroundAutomation.java'
gradle=app/'build.gradle'

b=bg.read_text(encoding='utf-8')
old='''            // Sempre começa pela rota de login. Se a sessão ainda for válida, o painel
            // redireciona sozinho; se tiver expirado, o script injeta e-mail/senha automaticamente.
            web.loadUrl(LOGIN+"?t="+System.currentTimeMillis());'''
new='''            // Abre a rota funcional validada. Se a sessão tiver expirado, o painel
            // redireciona ao login e page() injeta automaticamente as credenciais salvas.
            web.loadUrl(DEVICES+"?t="+System.currentTimeMillis());'''
if old not in b:
    raise SystemExit('ERRO v2.1.43: carga forçada de login da v2.1.42 não encontrada')
b=b.replace(old,new,1)
for marker in ['MasterXCloudActivity.Scripts.login(email,password)','low.contains("/login")||low.contains("#/login")','MasterXCloudActivity.Scripts.addDevice','MasterXCloudActivity.Scripts.deleteDevice']:
    if marker not in b:
        raise SystemExit('ERRO v2.1.43: requisito do motor ausente: '+marker)
bg.write_text(b,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 114',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.43'",g,count=1)
gradle.write_text(g,encoding='utf-8')

if 'web.loadUrl(DEVICES+"?t="+System.currentTimeMillis())' not in b:
    raise SystemExit('ERRO v2.1.43: executor não voltou a iniciar pela rota DEVICES')
if "versionName '2.1.43'" not in g:
    raise SystemExit('ERRO v2.1.43: versão não aplicada')
print('v2.1.43: motor XCloud background restaurado; login automático ocorre somente quando o painel redireciona para login')
