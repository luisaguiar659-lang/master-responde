from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
settings=java/'MasterflixAutomationSettingsActivity.java'
flow=java/'ResellerFlow.java'
msg=java/'MessageSettings.java'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

# 1) Amplia a tela existente de Automações MasterFlix com campos visíveis de revenda.
s=settings.read_text(encoding='utf-8')
s=s.replace('SharedPreferences p;EditText test,reseller,hours;','SharedPreferences p;EditText test,reseller,hours,signupLink,supportLink,signupMessage,confirmedMessage;',1)

old=''' r.addView(tv("GATILHOS DE REVENDA",14,true));r.addView(tv("Um gatilho por linha. Ex.: quero ser revenda",11,false));reseller=field("quero ser revenda\\nquero revenda",5);reseller.setText(p.getString("masterflix_reseller_triggers","quero ser revenda\\nquero ser uma revenda\\nquero revenda\\nser revenda"));r.addView(reseller);\n Button save=new Button(this);save.setText("SALVAR AUTOMAÇÕES MASTERFLIX");save.setOnClickListener(v->save());r.addView(save);return sc;}'''
new=''' r.addView(tv("GATILHOS DE REVENDA",14,true));r.addView(tv("Um gatilho por linha. Ex.: quero ser revenda",11,false));reseller=field("quero ser revenda\\nquero revenda",5);reseller.setText(p.getString("masterflix_reseller_triggers","quero ser revenda\\nquero ser uma revenda\\nquero revenda\\nser revenda"));r.addView(reseller);\n r.addView(tv("LINK DE CADASTRO DA REVENDA",14,true));r.addView(tv("Link enviado ao cliente quando ele inicia o fluxo de revenda.",11,false));signupLink=field("https://...",2);signupLink.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);signupLink.setText(p.getString("reseller_signup_link",ResellerFlow.DEFAULT_SIGNUP_LINK));r.addView(signupLink);\n r.addView(tv("MENSAGEM DE CADASTRO",14,true));r.addView(tv("Use {LINK_CADASTRO} no ponto onde o link deve aparecer.",11,false));signupMessage=field("Mensagem enviada antes de pedir o e-mail",5);signupMessage.setText(p.getString("reseller_signup_message",ResellerFlow.DEFAULT_SIGNUP_MESSAGE));r.addView(signupMessage);\n r.addView(tv("LINK DO GRUPO DE SUPORTE",14,true));r.addView(tv("Link enviado após a confirmação da revenda.",11,false));supportLink=field("https://chat.whatsapp.com/...",2);supportLink.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);supportLink.setText(p.getString("reseller_support_group_link",ResellerFlow.DEFAULT_SUPPORT_GROUP_LINK));r.addView(supportLink);\n r.addView(tv("MENSAGEM DE REVENDA CONFIRMADA",14,true));r.addView(tv("Use {LINK_GRUPO} no ponto onde o grupo de suporte deve aparecer.",11,false));confirmedMessage=field("Mensagem de confirmação",6);confirmedMessage.setText(p.getString(MessageSettings.MSG_RESELLER_CONFIRMED,MessageSettings.DEFAULT_RESELLER_CONFIRMED));r.addView(confirmedMessage);\n Button save=new Button(this);save.setText("SALVAR AUTOMAÇÕES MASTERFLIX");save.setOnClickListener(v->save());r.addView(save);return sc;}'''
if old not in s: raise SystemExit('ERRO v2.1.26: bloco de revenda da tela não encontrado')
s=s.replace(old,new,1)

oldsave=''' void save(){String t=test.getText().toString().trim(),rv=reseller.getText().toString().trim();int h=24;try{h=Integer.parseInt(hours.getText().toString().trim());}catch(Exception ignored){}if(h<0)h=0;if(h>8760)h=8760;if(t.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de teste",Toast.LENGTH_SHORT).show();return;}if(rv.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de revenda",Toast.LENGTH_SHORT).show();return;}p.edit().putString("masterflix_test_triggers",t).putString("masterflix_reseller_triggers",rv).putInt("masterflix_test_cooldown_hours",h).apply();hours.setText(String.valueOf(h));Toast.makeText(this,"Configurações do MasterFlix salvas",Toast.LENGTH_SHORT).show();}'''
newsave=''' void save(){String t=test.getText().toString().trim(),rv=reseller.getText().toString().trim(),sl=signupLink.getText().toString().trim(),gl=supportLink.getText().toString().trim(),sm=signupMessage.getText().toString().trim(),cm=confirmedMessage.getText().toString().trim();int h=24;try{h=Integer.parseInt(hours.getText().toString().trim());}catch(Exception ignored){}if(h<0)h=0;if(h>8760)h=8760;if(t.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de teste",Toast.LENGTH_SHORT).show();return;}if(rv.isEmpty()){Toast.makeText(this,"Cadastre pelo menos um gatilho de revenda",Toast.LENGTH_SHORT).show();return;}if(sl.isEmpty()){Toast.makeText(this,"Informe o link de cadastro da revenda",Toast.LENGTH_SHORT).show();return;}if(gl.isEmpty()){Toast.makeText(this,"Informe o link do grupo de suporte",Toast.LENGTH_SHORT).show();return;}if(sm.isEmpty()){Toast.makeText(this,"Informe a mensagem de cadastro",Toast.LENGTH_SHORT).show();return;}if(cm.isEmpty()){Toast.makeText(this,"Informe a mensagem de confirmação",Toast.LENGTH_SHORT).show();return;}p.edit().putString("masterflix_test_triggers",t).putString("masterflix_reseller_triggers",rv).putInt("masterflix_test_cooldown_hours",h).putString("reseller_signup_link",sl).putString("reseller_support_group_link",gl).putString("reseller_signup_message",sm).putString(MessageSettings.MSG_RESELLER_CONFIRMED,cm).apply();hours.setText(String.valueOf(h));Toast.makeText(this,"Configurações do MasterFlix salvas",Toast.LENGTH_SHORT).show();}'''
if oldsave not in s: raise SystemExit('ERRO v2.1.26: método save não encontrado')
s=s.replace(oldsave,newsave,1)
settings.write_text(s,encoding='utf-8')

# 2) O convite de revenda passa a usar mensagem configurável com variável {LINK_CADASTRO}.
f=flow.read_text(encoding='utf-8')
f=f.replace('public static final String DEFAULT_SUPPORT_GROUP_LINK="https://chat.whatsapp.com/IWHogXDJTem2VgJTJhsTA0?s=cl&p=a&mlu=0&ilr=1";','public static final String DEFAULT_SUPPORT_GROUP_LINK="https://chat.whatsapp.com/IWHogXDJTem2VgJTJhsTA0?s=cl&p=a&mlu=0&ilr=1";\n public static final String DEFAULT_SIGNUP_MESSAGE="🚀 Para ser Revenda, faça seu cadastro pelo link:\\n\\n{LINK_CADASTRO}\\n\\nApós concluir seu cadastro, envie o e-mail usado no cadastro para ativação do painel.";',1)
oldsend='''   MasterflixDirectReply.send(c,n,"🚀 Para ser Revenda, faça seu cadastro pelo link:\\n\\n"+link+"\\n\\nApós concluir seu cadastro, envie o e-mail usado no cadastro para ativação do painel.");return true;'''
newsend='''   String invite=p.getString("reseller_signup_message",DEFAULT_SIGNUP_MESSAGE).replace("{LINK_CADASTRO}",link);MasterflixDirectReply.send(c,n,invite);return true;'''
if oldsend not in f: raise SystemExit('ERRO v2.1.26: envio do link de cadastro não encontrado')
f=f.replace(oldsend,newsend,1)
flow.write_text(f,encoding='utf-8')

# 3) A variável {LINK_GRUPO} sempre usa o valor configurado pelo usuário.
m=msg.read_text(encoding='utf-8')
oldloop='''for(Map.Entry<String,String> e:vars.entrySet())text=text.replace("{"+e.getKey()+"}",e.getValue()==null?"":e.getValue());return MasterflixDirectReply.send(c,n,text);'''
newloop='''for(Map.Entry<String,String> e:vars.entrySet()){String val=e.getValue()==null?"":e.getValue();if("LINK_GRUPO".equals(e.getKey()))val=p.getString("reseller_support_group_link",val);text=text.replace("{"+e.getKey()+"}",val);}if(text.contains("{LINK_GRUPO}"))text=text.replace("{LINK_GRUPO}",p.getString("reseller_support_group_link",ResellerFlow.DEFAULT_SUPPORT_GROUP_LINK));return MasterflixDirectReply.send(c,n,text);'''
if oldloop not in m: raise SystemExit('ERRO v2.1.26: substituição de variáveis MessageSettings não encontrada')
m=m.replace(oldloop,newloop,1)
msg.write_text(m,encoding='utf-8')

# 4) Versão visual/build.
d=dash.read_text(encoding='utf-8')
for v in ['v2.1.22','v2.1.23','v2.1.24','v2.1.25']:
 d=d.replace('brand.addView(text("'+v+'",10,MUTED,false));','brand.addView(text("v2.1.26",10,MUTED,false));')
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 97',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.26'",g,count=1);gradle.write_text(g,encoding='utf-8')

checks=[
(settings,'LINK DE CADASTRO DA REVENDA'),(settings,'LINK DO GRUPO DE SUPORTE'),(settings,'{LINK_CADASTRO}'),(settings,'{LINK_GRUPO}'),
(flow,'reseller_signup_message'),(flow,'DEFAULT_SIGNUP_MESSAGE'),(msg,'reseller_support_group_link'),(gradle,"versionName '2.1.26'")]
for pth,mk in checks:
 if mk not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.26: requisito ausente '+mk)
print('v2.1.26: links e mensagens de revenda visíveis e configuráveis na tela Automações MasterFlix')