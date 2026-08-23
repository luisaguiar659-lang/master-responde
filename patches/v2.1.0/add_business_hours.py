from pathlib import Path
import re

app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; gradle=app/'build.gradle'; manifest=app/'src/main/AndroidManifest.xml'; svc=java/'WhatsAppBusinessCaptureService.java'; dash=java/'NeonDashboardActivity.java'

(java/'BusinessHours.java').write_text(r'''package com.masterresponde.app;
import android.content.Context;import android.content.SharedPreferences;import java.util.Calendar;
public final class BusinessHours{
 private BusinessHours(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences("master_responde",Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return p(c).getBoolean("hours_enabled",false);}
 public static String away(Context c){return p(c).getString("hours_away","Olá! No momento estamos fora do horário de atendimento. Retornaremos assim que possível.");}
 public static boolean isOpen(Context c){if(!enabled(c))return true;Calendar x=Calendar.getInstance();int dow=x.get(Calendar.DAY_OF_WEEK);String days=p(c).getString("hours_days","2,3,4,5,6");if(!(","+days+",").contains(","+dow+","))return false;int now=x.get(Calendar.HOUR_OF_DAY)*60+x.get(Calendar.MINUTE);int start=p(c).getInt("hours_start",8*60),end=p(c).getInt("hours_end",18*60);if(start==end)return true;if(start<end)return now>=start&&now<end;return now>=start||now<end;}
}''',encoding='utf-8')

(java/'BusinessHoursActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.Bundle;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.view.*;import android.widget.*;import java.util.*;
public class BusinessHoursActivity extends Activity{
 SharedPreferences p;Switch enabled;TextView time;EditText away;LinearLayout days;boolean[] checked=new boolean[8];int start,end;
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);start=p.getInt("hours_start",480);end=p.getInt("hours_end",1080);String ds=p.getString("hours_days","2,3,4,5,6");for(int i=1;i<=7;i++)checked[i]=(","+ds+",").contains(","+i+",");setContentView(build());}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));r.addView(tv("HORÁRIO DE ATENDIMENTO",23,true));TextView sub=tv("Controle automático para conversas privadas",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);enabled=new Switch(this);enabled.setText("Ativar controle de horário");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean("hours_enabled",false));r.addView(enabled);r.addView(tv("DIAS DE ATENDIMENTO",14,true));days=new LinearLayout(this);days.setOrientation(LinearLayout.VERTICAL);String[] names={"","Domingo","Segunda-feira","Terça-feira","Quarta-feira","Quinta-feira","Sexta-feira","Sábado"};for(int i=1;i<=7;i++){final int d=i;CheckBox c=new CheckBox(this);c.setText(names[i]);c.setTextColor(Color.WHITE);c.setChecked(checked[i]);c.setOnCheckedChangeListener((b,v)->checked[d]=v);days.addView(c);}r.addView(days);r.addView(tv("HORÁRIO",14,true));time=tv("",18,true);refreshTime();r.addView(time);LinearLayout br=new LinearLayout(this);Button a=new Button(this);a.setText("ABERTURA");a.setOnClickListener(v->pick(true));Button f=new Button(this);f.setText("FECHAMENTO");f.setOnClickListener(v->pick(false));br.addView(a,new LinearLayout.LayoutParams(0,-2,1));br.addView(f,new LinearLayout.LayoutParams(0,-2,1));r.addView(br);r.addView(tv("MENSAGEM FORA DO HORÁRIO",14,true));away=new EditText(this);away.setText(p.getString("hours_away","Olá! No momento estamos fora do horário de atendimento. Retornaremos assim que possível."));away.setTextColor(Color.WHITE);away.setHintTextColor(Color.GRAY);away.setMinLines(4);r.addView(away);Button save=new Button(this);save.setText("SALVAR HORÁRIO");save.setOnClickListener(v->save());r.addView(save);return sc;}
 void pick(boolean opening){int m=opening?start:end;new TimePickerDialog(this,(v,h,min)->{if(opening)start=h*60+min;else end=h*60+min;refreshTime();},m/60,m%60,true).show();}
 void refreshTime(){if(time!=null)time.setText(String.format(Locale.getDefault(),"%02d:%02d  →  %02d:%02d",start/60,start%60,end/60,end%60));}
 void save(){StringBuilder ds=new StringBuilder();for(int i=1;i<=7;i++)if(checked[i]){if(ds.length()>0)ds.append(',');ds.append(i);}if(enabled.isChecked()&&ds.length()==0){Toast.makeText(this,"Selecione pelo menos um dia",Toast.LENGTH_SHORT).show();return;}String msg=away.getText().toString().trim();if(enabled.isChecked()&&msg.isEmpty()){Toast.makeText(this,"Digite a mensagem fora do horário",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("hours_enabled",enabled.isChecked()).putString("hours_days",ds.toString()).putInt("hours_start",start).putInt("hours_end",end).putString("hours_away",msg).apply();Toast.makeText(this,"Horário salvo",Toast.LENGTH_SHORT).show();}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
needle='''        // PRIVADO: somente daqui em diante entram comandos/resposta padrão.\n        String resolvedReply = CommandEngine.resolve(this, message);'''
repl='''        // PRIVADO: horário de atendimento vem antes de comandos/resposta padrão.\n        // Fora do horário, envia somente a mensagem configurada e encerra o fluxo privado.\n        if (BusinessHours.enabled(this) && !BusinessHours.isOpen(this)) {\n            String awayReply = BusinessHours.away(this);\n            if (awayReply != null && !awayReply.trim().isEmpty()) {\n                prefs().edit().putString("last_matched_command", "FORA_HORARIO").apply();\n                sendDirectReply(n, conversation, awayReply.trim());\n            }\n            return;\n        }\n\n        // PRIVADO: somente daqui em diante entram comandos/resposta padrão.\n        String resolvedReply = CommandEngine.resolve(this, message);'''
if needle not in s: raise SystemExit('ERRO v2.1.18: roteamento privado não encontrado')
s=s.replace(needle,repl,1);svc.write_text(s,encoding='utf-8')

d=dash.read_text(encoding='utf-8')
old='TextView future=sideItem("◷","Horário de Atendimento",MUTED,v->Toast.makeText(this,"Será a próxima função",Toast.LENGTH_SHORT).show());panel.addView(future);'
new='TextView future=sideItem("◷","Horário de Atendimento",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,BusinessHoursActivity.class));});panel.addView(future);'
if old not in d: raise SystemExit('ERRO v2.1.18: item futuro do horário não encontrado')
d=d.replace(old,new,1).replace('brand.addView(text("v2.1.17",10,MUTED,false));','brand.addView(text("v2.1.18",10,MUTED,false));',1);dash.write_text(d,encoding='utf-8')

m=manifest.read_text(encoding='utf-8')
if '.BusinessHoursActivity' not in m:m=m.replace('</application>','<activity android:name=".BusinessHoursActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(m,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 89',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.18'",g,count=1);gradle.write_text(g,encoding='utf-8')
for pth,mk in [(java/'BusinessHours.java','isOpen'),(java/'BusinessHoursActivity.java','SALVAR HORÁRIO'),(svc,'FORA_HORARIO'),(dash,'BusinessHoursActivity.class'),(manifest,'.BusinessHoursActivity'),(gradle,"versionName '2.1.18'")]:
 if mk not in pth.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.18: requisito ausente '+mk)
print('v2.1.18: horário de atendimento configurável criado; grupo permanece isolado; comandos e resposta padrão preservados')