from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
svc=java/'WhatsAppBusinessCaptureService.java'
act=java/'MasterflixAutomationSettingsActivity.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# 1) Campo visível em Automações MasterFlix.
a=act.read_text(encoding='utf-8')
a=a.replace('SharedPreferences p;EditText test,reseller,hours;', 'SharedPreferences p;EditText test,reseller,hours,preTestMessage;', 1)

needle=''' r.addView(tv("INTERVALO PARA NOVO TESTE",14,true));r.addView(tv("Tempo, em horas, que a mesma pessoa precisa aguardar para solicitar outro teste. Use 0 para desativar o limite.",11,false));hours=field("24",1);hours.setInputType(InputType.TYPE_CLASS_NUMBER);hours.setText(String.valueOf(p.getInt("masterflix_test_cooldown_hours",24)));r.addView(hours);'''
insert=needle+'''\n r.addView(tv("MENSAGEM ANTES DO TESTE",14,true));r.addView(tv("Esta mensagem será enviada imediatamente antes de iniciar a geração automática do teste.",11,false));preTestMessage=field("🧪 TESTE MASTER PLAY PLUS\\n\\n🚀 Que bom que você quer conhecer nosso serviço!\\n\\n⏳ Aguarde alguns instantes enquanto seu acesso é gerado automaticamente.",5);preTestMessage.setText(p.getString("masterflix_pre_test_message","🧪 *TESTE MASTER PLAY PLUS*\\n\\n🚀 Que bom que você quer conhecer nosso serviço!\\n\\n⏳ Aguarde alguns instantes enquanto seu acesso é gerado automaticamente."));r.addView(preTestMessage);'''
if needle not in a:
    raise SystemExit('ERRO v2.1.34: intervalo de teste não encontrado na tela')
a=a.replace(needle,insert,1)

oldsave='''void save(){String t=test.getText().toString().trim(),rv=reseller.getText().toString().trim();int h=24;'''
newsave='''void save(){String t=test.getText().toString().trim(),rv=reseller.getText().toString().trim(),pre=preTestMessage.getText().toString().trim();int h=24;'''
if oldsave not in a:
    raise SystemExit('ERRO v2.1.34: método save não encontrado')
a=a.replace(oldsave,newsave,1)

oldprefs='''.putString("masterflix_test_triggers",t).putString("masterflix_reseller_triggers",rv).putInt("masterflix_test_cooldown_hours",h).apply();'''
newprefs='''.putString("masterflix_test_triggers",t).putString("masterflix_reseller_triggers",rv).putInt("masterflix_test_cooldown_hours",h).putString("masterflix_pre_test_message",pre).apply();'''
if oldprefs not in a:
    raise SystemExit('ERRO v2.1.34: persistência MasterFlix não encontrada')
a=a.replace(oldprefs,newprefs,1)
act.write_text(a,encoding='utf-8')

# 2) Usa o texto configurado exatamente antes de iniciar a geração.
s=svc.read_text(encoding='utf-8')
old='''                    prefs().edit().putString("last_test_status", "Teste: geração automática iniciada").apply();\n                    sendDirectReply(n, conversation, "Gerando seu teste, aguarde...");'''
new='''                    prefs().edit().putString("last_test_status", "Teste: geração automática iniciada").apply();\n                    String preTestMessage = prefs().getString("masterflix_pre_test_message", "🧪 *TESTE MASTER PLAY PLUS*\\n\\n🚀 Que bom que você quer conhecer nosso serviço!\\n\\n⏳ Aguarde alguns instantes enquanto seu acesso é gerado automaticamente.").trim();\n                    if (!preTestMessage.isEmpty()) sendDirectReply(n, conversation, preTestMessage);'''
if old not in s:
    raise SystemExit('ERRO v2.1.34: mensagem antiga antes do teste não encontrada')
s=s.replace(old,new,1)
svc.write_text(s,encoding='utf-8')

# 3) Versão.
D=dash.read_text(encoding='utf-8')
D=D.replace('brand.addView(text("v2.1.33",10,MUTED,false));','brand.addView(text("v2.1.34",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8')
G=re.sub(r'versionCode\s+\d+','versionCode 105',G,count=1)
G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.34'",G,count=1)
gradle.write_text(G,encoding='utf-8')

checks=[
 (act,'MENSAGEM ANTES DO TESTE'),
 (act,'masterflix_pre_test_message'),
 (svc,'masterflix_pre_test_message'),
 (svc,'if (!preTestMessage.isEmpty()) sendDirectReply'),
 (gradle,"versionName '2.1.34'")
]
for p,t in checks:
    if t not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.34: requisito ausente '+t)
print('v2.1.34: mensagem antes do teste configurável no painel MasterFlix')