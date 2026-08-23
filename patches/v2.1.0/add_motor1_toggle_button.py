from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; act=java/'ScheduledGroupBroadcastActivity.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

s=act.read_text(encoding='utf-8')

# Troca o Switch por um botão de estado visual. A configuração continua separada do liga/desliga.
s=s.replace('SharedPreferences p;Switch enabled;EditText group,msg,day,interval;Spinner unit;TextView start,next,status;int startMinutes;',
'''SharedPreferences p;Button motorButton;EditText group,msg,day,interval;Spinner unit;TextView start,next,status;int startMinutes;boolean motorOn;''',1)

s=s.replace('public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);startMinutes=p.getInt("sgb_start_minutes",8*60);setContentView(build());}',
'''public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);startMinutes=p.getInt("sgb_start_minutes",8*60);motorOn=p.getBoolean("sgb_enabled",false);setContentView(build());}''',1)

old='''enabled=new Switch(this);enabled.setText("Ativar motor de avisos");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean("sgb_enabled",false));r.addView(enabled);'''
new='''motorButton=new Button(this);refreshMotorButton();motorButton.setOnClickListener(v->toggleMotor());r.addView(motorButton);'''
if old not in s: raise SystemExit('ERRO v2.1.32: Switch do motor não encontrado')
s=s.replace(old,new,1)

# Salvar não altera mais o estado do motor.
s=s.replace('save.setText("SALVAR E ATIVAR PROGRAMAÇÃO")','save.setText("SALVAR CONFIGURAÇÃO")',1)
s=s.replace('if(enabled.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Informe os grupos de suporte e a mensagem",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb_enabled",enabled.isChecked()).putString("sgb_groups",g)',
'''if(g.isEmpty()||m.isEmpty()){Toast.makeText(this,"Informe os grupos de suporte e a mensagem",Toast.LENGTH_SHORT).show();return;}p.edit().putString("sgb_groups",g)''',1)

# Remove o antigo botão de teste da tela. O novo botão acima é o único controle liga/desliga.
oldtest='''Button test=new Button(this);test.setText("DEIXAR ENVIO VENCIDO PARA TESTE");test.setOnClickListener(v->{p.edit().putLong("sgb_next_at",System.currentTimeMillis()-1000L).apply();next.setText("Próximo envio: agora");Toast.makeText(this,"Teste armado. O motor tentará enviar ao grupo.",Toast.LENGTH_LONG).show();});r.addView(test);'''
if oldtest in s:s=s.replace(oldtest,'',1)

# Acrescenta métodos de controle visual e estado real.
anchor=''' void refreshStart(){if(start!=null)start.setText(String.format(Locale.getDefault(),"%02d:%02d",startMinutes/60,startMinutes%60));}'''
if anchor not in s: raise SystemExit('ERRO v2.1.32: refreshStart não encontrado')
methods=r''' void refreshMotorButton(){if(motorButton==null)return;if(motorOn){motorButton.setText("MOTOR LIGADO");motorButton.setTextColor(Color.WHITE);motorButton.setBackgroundColor(Color.rgb(20,150,70));}else{motorButton.setText("MOTOR DESLIGADO");motorButton.setTextColor(Color.WHITE);motorButton.setBackgroundColor(Color.rgb(185,45,45));}}
 void toggleMotor(){
  if(!motorOn){
   String groups=p.getString("sgb_groups","").trim(),message=p.getString("sgb_message","").trim();
   if(groups.isEmpty()||message.isEmpty()){Toast.makeText(this,"Salve os grupos e a mensagem antes de ligar o motor",Toast.LENGTH_LONG).show();return;}
   motorOn=true;p.edit().putBoolean("sgb_enabled",true).apply();
   if(p.getLong("sgb_next_at",0L)<=0L)ScheduledGroupBroadcast.resetNext(this);
   Toast.makeText(this,"Motor ligado",Toast.LENGTH_SHORT).show();
  }else{
   motorOn=false;p.edit().putBoolean("sgb_enabled",false).apply();
   Toast.makeText(this,"Motor desligado",Toast.LENGTH_SHORT).show();
  }
  refreshMotorButton();
  if(next!=null)next.setText("Próximo envio: "+safeNext());
 }
'''
s=s.replace(anchor,methods+anchor,1)

# Texto explicativo passa a refletir o controle novo.
s=s.replace('Envia uma mensagem automaticamente, sem gatilho, no intervalo configurado.', 'Configure, salve e use o botão para ligar ou desligar o motor de avisos.',1)

act.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=D.replace('brand.addView(text("v2.1.31",10,MUTED,false));','brand.addView(text("v2.1.32",10,MUTED,false));');dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 103',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.32'",G,count=1);gradle.write_text(G,encoding='utf-8')

for p,m in [(act,'MOTOR LIGADO'),(act,'MOTOR DESLIGADO'),(act,'SALVAR CONFIGURAÇÃO'),(act,'toggleMotor()'),(act,'Color.rgb(20,150,70)'),(act,'Color.rgb(185,45,45)'),(gradle,"versionName '2.1.32'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.32: requisito ausente '+m)
if 'Ativar motor de avisos' in act.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.32: Switch antigo ainda presente')
print('v2.1.32: Motor 1 com botão visual verde/vermelho para ligar/desligar; salvar configuração separado')