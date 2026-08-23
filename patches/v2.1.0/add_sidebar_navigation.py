from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
dash=java/'NeonDashboardActivity.java'
gradle=app/'build.gradle'

s=dash.read_text(encoding='utf-8')

# Hamburger passa a abrir o menu lateral.
old='TextView menu=text("☰",25,CYAN,false);menu.setGravity(Gravity.CENTER);'
new='TextView menu=text("☰",25,CYAN,false);menu.setGravity(Gravity.CENTER);menu.setClickable(true);menu.setOnClickListener(v->openSideMenu());'
if old not in s:
    raise SystemExit('ERRO v2.1.17: hamburger não encontrado')
s=s.replace(old,new,1)

# Remove a barra inferior inteira, sem alterar o restante do dashboard.
pattern=r'root\.addView\(space\(10\)\);View divider=new View\(this\);divider\.setBackgroundColor\(Color\.rgb\(24,33,40\)\);root\.addView\(divider,new LinearLayout\.LayoutParams\(-1,dp\(1\)\)\);LinearLayout nr=row\(\);.*?root\.addView\(nr\);return scroll;\}'
m=re.search(pattern,s,re.S)
if not m:
    raise SystemExit('ERRO v2.1.17: barra inferior não encontrada')
s=s[:m.start()]+'return scroll;}'+s[m.end():]

# Menu lateral funcional, usando somente Activities que já existem.
anchor=' private void openSettings(){'
if anchor not in s:
    raise SystemExit('ERRO v2.1.17: openSettings não encontrado')
menu_code=r''' private void openSideMenu(){
  final android.app.Dialog d=new android.app.Dialog(this);
  d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
  LinearLayout panel=col();panel.setPadding(dp(20),dp(24),dp(20),dp(24));panel.setBackgroundColor(Color.rgb(1,7,12));
  LinearLayout top=row();
  ImageView bot=asset(R.drawable.mr_robot);top.addView(bot,new LinearLayout.LayoutParams(dp(58),dp(58)));
  top.addView(spaceH(12));LinearLayout brand=col();brand.addView(text("MASTER RESPONDE",19,GREEN,true));brand.addView(text("v2.1.17",10,MUTED,false));brand.addView(text("● ONLINE",10,GREEN,true));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1f));panel.addView(top);
  panel.addView(space(18));
  sideSection(panel,"PRINCIPAL");
  panel.addView(sideItem("⌂","Início",GREEN,v->d.dismiss()));
  panel.addView(space(10));
  sideSection(panel,"GERENCIAMENTO");
  panel.addView(sideItem("▦","Comandos",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,MessageSettingsActivity.class));}));
  panel.addView(sideItem("◉","Histórico",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,HistoryActivity.class));}catch(Throwable e){Toast.makeText(this,"Histórico indisponível",Toast.LENGTH_SHORT).show();}}));
  panel.addView(sideItem("⚙","Configurações",Color.WHITE,v->{d.dismiss();openSettings();}));
  panel.addView(space(10));
  sideSection(panel,"AUTOMAÇÃO");
  panel.addView(sideItem("☷","Menu do Grupo",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,GroupMenuActivity.class));}catch(Throwable e){Toast.makeText(this,"Menu do grupo indisponível",Toast.LENGTH_SHORT).show();}}));
  TextView future=sideItem("◷","Horário de Atendimento",MUTED,v->Toast.makeText(this,"Será a próxima função",Toast.LENGTH_SHORT).show());panel.addView(future);
  panel.addView(space(16));
  LinearLayout secure=card(Color.rgb(18,110,50));secure.addView(text("🔒  SISTEMA PROTEGIDO",11,GREEN,true));secure.addView(text("Somente WhatsApp Business\nNúcleo de respostas preservado",9,Color.WHITE,false));panel.addView(secure);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.addView(panel,new ScrollView.LayoutParams(-1,-2));d.setContentView(sc);
  android.view.Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));w.setGravity(Gravity.LEFT|Gravity.TOP);android.view.WindowManager.LayoutParams lp=new android.view.WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(getResources().getDisplayMetrics().widthPixels*.88f);lp.height=android.view.WindowManager.LayoutParams.MATCH_PARENT;lp.dimAmount=.55f;w.setAttributes(lp);w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
  d.show();
 }
 private void sideSection(LinearLayout p,String label){TextView t=text(label,10,Color.WHITE,true);t.setPadding(dp(12),dp(10),dp(12),dp(8));t.setBackgroundColor(Color.rgb(7,17,24));p.addView(t,new LinearLayout.LayoutParams(-1,-2));}
 private TextView sideItem(String icon,String label,int color,View.OnClickListener click){TextView t=text(icon+"    "+label,15,color,false);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(16),dp(15),dp(12),dp(15));t.setBackground(bg(Color.rgb(3,10,16),Color.rgb(20,45,58),4,1));t.setOnClickListener(click);return t;}
'''
s=s.replace(anchor,menu_code+anchor,1)

dash.write_text(s,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 88',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.17'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[(dash,'openSideMenu()'),(dash,'Menu do Grupo'),(dash,'Horário de Atendimento'),(dash,'sideItem('),(gradle,"versionName '2.1.17'")]
for p,mk in checks:
    if mk not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.17: requisito ausente '+mk)
# Garante que a barra inferior visual antiga saiu.
final=dash.read_text(encoding='utf-8')
if 'nr.addView(nav(' in final:
    raise SystemExit('ERRO v2.1.17: navegação inferior antiga ainda presente')
print('v2.1.17: navegação movida para menu lateral; motor preservado')