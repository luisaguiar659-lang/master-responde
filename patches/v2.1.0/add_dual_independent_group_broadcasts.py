from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

# Estratégia nova: parte EXATAMENTE da v2.1.27 funcional.
# Motor 1 permanece com as mesmas chaves e o mesmo caminho de envio.
# Motor 2 é uma cópia independente, com preferências, relógio e estado próprios.

(java/'ScheduledGroupBroadcast2.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.util.*;
public final class ScheduledGroupBroadcast2{
 private static final String P="master_responde";private ScheduledGroupBroadcast2(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return p(c).getBoolean("sgb2_enabled",false);}
 public static String group(Context c){return p(c).getString("sgb2_group","").trim();}
 public static String message(Context c){return p(c).getString("sgb2_message","").trim();}
 public static int intervalMinutes(Context c){int v=Math.max(1,p(c).getInt("sgb2_interval_value",3));String u=p(c).getString("sgb2_interval_unit","HOURS");long m="MINUTES".equals(u)?v:(long)v*60L;return (int)Math.min(525600L,m);}
 public static long nextAt(Context c){long n=p(c).getLong("sgb2_next_at",0L);if(n<=0L){n=computeFirst(c,System.currentTimeMillis());p(c).edit().putLong("sgb2_next_at",n).apply();}return n;}
 public static long computeFirst(Context c,long now){Calendar x=Calendar.getInstance();x.setTimeInMillis(now);int wanted=p(c).getInt("sgb2_day",x.get(Calendar.DAY_OF_MONTH));int mins=p(c).getInt("sgb2_start_minutes",8*60);Calendar a=(Calendar)x.clone();a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);a.set(Calendar.DAY_OF_MONTH,Math.min(Math.max(1,wanted),a.getActualMaximum(Calendar.DAY_OF_MONTH)));a.set(Calendar.HOUR_OF_DAY,mins/60);a.set(Calendar.MINUTE,mins%60);if(a.getTimeInMillis()<=now){long step=intervalMinutes(c)*60000L;long base=a.getTimeInMillis();long k=(now-base)/step+1L;return base+k*step;}return a.getTimeInMillis();}
 public static boolean due(Context c,long now){return enabled(c)&&!group(c).isEmpty()&&!message(c).isEmpty()&&now>=nextAt(c);}
 public static void sent(Context c,long now){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb2_next_at",n).putLong("sgb2_last_sent_at",now).putString("sgb2_last_status","Enviado automaticamente").apply();}
 public static void waiting(Context c,String status){p(c).edit().putString("sgb2_last_status",status).apply();}
 public static void resetNext(Context c){p(c).edit().putLong("sgb2_next_at",computeFirst(c,System.currentTimeMillis())).apply();}
 public static String formatNext(Context c){long n=nextAt(c);return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(n));}
}''',encoding='utf-8')

(java/'ScheduledGroupBroadcastActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;import java.util.*;
public class ScheduledGroupBroadcastActivity extends Activity{
 SharedPreferences p;
 Switch e1,e2;EditText g1,m1,d1,i1,g2,m2,d2,i2;Spinner u1,u2;TextView t1,n1,s1,t2,n2,s2;int sm1,sm2;
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);sm1=p.getInt("sgb_start_minutes",8*60);sm2=p.getInt("sgb2_start_minutes",8*60);setContentView(build());}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 EditText f(String h,int l){EditText e=new EditText(this);e.setHint(h);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);e.setMinLines(l);e.setGravity(l>1?Gravity.TOP:Gravity.CENTER_VERTICAL);return e;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));r.addView(tv("AVISOS AUTOMÁTICOS",23,true));TextView sub=tv("Dois motores independentes, cada um com seu próprio grupo e relógio.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);addOne(r);addTwo(r);return sc;}
 void addOne(LinearLayout r){r.addView(tv("MOTOR 1",18,true));e1=new Switch(this);e1.setText("Ativar Motor 1");e1.setTextColor(Color.WHITE);e1.setChecked(p.getBoolean("sgb_enabled",false));r.addView(e1);g1=f("Nome exato do grupo",1);g1.setText(p.getString("sgb_group",""));r.addView(g1);m1=f("Mensagem automática",5);m1.setText(p.getString("sgb_message",""));r.addView(m1);d1=f("Dia de início: 1 a 31",1);d1.setInputType(InputType.TYPE_CLASS_NUMBER);d1.setText(String.valueOf(p.getInt("sgb_day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));r.addView(d1);t1=tv("",16,true);rt1();r.addView(t1);Button ph=new Button(this);ph.setText("HORÁRIO MOTOR 1");ph.setOnClickListener(v->new TimePickerDialog(this,(x,h,m)->{sm1=h*60+m;rt1();},sm1/60,sm1%60,true).show());r.addView(ph);LinearLayout row=new LinearLayout(this);i1=f("3",1);i1.setInputType(InputType.TYPE_CLASS_NUMBER);i1.setText(String.valueOf(p.getInt("sgb_interval_value",3)));u1=spinner(p.getString("sgb_interval_unit","HOURS"));row.addView(i1,new LinearLayout.LayoutParams(0,-2,1));row.addView(u1,new LinearLayout.LayoutParams(0,-2,1));r.addView(row);n1=tv("Próximo: "+safe1(),12,true);r.addView(n1);s1=tv("Status: "+p.getString("sgb_last_status","Aguardando configuração"),11,false);r.addView(s1);Button save=new Button(this);save.setText("SALVAR MOTOR 1");save.setOnClickListener(v->save1());r.addView(save);Button test=new Button(this);test.setText("TESTAR MOTOR 1 AGORA");test.setOnClickListener(v->{save1();p.edit().putLong("sgb_next_at",System.currentTimeMillis()-1000L).apply();n1.setText("Próximo: agora");});r.addView(test);}
 void addTwo(LinearLayout r){r.addView(tv("MOTOR 2",18,true));e2=new Switch(this);e2.setText("Ativar Motor 2");e2.setTextColor(Color.WHITE);e2.setChecked(p.getBoolean("sgb2_enabled",false));r.addView(e2);g2=f("Nome exato do segundo grupo",1);g2.setText(p.getString("sgb2_group",""));r.addView(g2);m2=f("Mensagem automática do segundo grupo",5);m2.setText(p.getString("sgb2_message",""));r.addView(m2);d2=f("Dia de início: 1 a 31",1);d2.setInputType(InputType.TYPE_CLASS_NUMBER);d2.setText(String.valueOf(p.getInt("sgb2_day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));r.addView(d2);t2=tv("",16,true);rt2();r.addView(t2);Button ph=new Button(this);ph.setText("HORÁRIO MOTOR 2");ph.setOnClickListener(v->new TimePickerDialog(this,(x,h,m)->{sm2=h*60+m;rt2();},sm2/60,sm2%60,true).show());r.addView(ph);LinearLayout row=new LinearLayout(this);i2=f("3",1);i2.setInputType(InputType.TYPE_CLASS_NUMBER);i2.setText(String.valueOf(p.getInt("sgb2_interval_value",3)));u2=spinner(p.getString("sgb2_interval_unit","HOURS"));row.addView(i2,new LinearLayout.LayoutParams(0,-2,1));row.addView(u2,new LinearLayout.LayoutParams(0,-2,1));r.addView(row);n2=tv("Próximo: "+safe2(),12,true);r.addView(n2);s2=tv("Status: "+p.getString("sgb2_last_status","Aguardando configuração"),11,false);r.addView(s2);Button save=new Button(this);save.setText("SALVAR MOTOR 2");save.setOnClickListener(v->save2());r.addView(save);Button test=new Button(this);test.setText("TESTAR MOTOR 2 AGORA");test.setOnClickListener(v->{save2();p.edit().putLong("sgb2_next_at",System.currentTimeMillis()-1000L).apply();n2.setText("Próximo: agora");});r.addView(test);}
 Spinner spinner(String unit){Spinner s=new Spinner(this);String[] x={"HORAS","MINUTOS"};s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,x));s.setSelection("MINUTES".equals(unit)?1:0);return s;}
 void rt1(){if(t1!=null)t1.setText(String.format(Locale.getDefault(),"Horário: %02d:%02d",sm1/60,sm1%60));}void rt2(){if(t2!=null)t2.setText(String.format(Locale.getDefault(),"Horário: %02d:%02d",sm2/60,sm2%60));}
 int num(EditText e,int def){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return def;}}
 void save1(){String g=g1.getText().toString().trim(),m=m1.getText().toString().trim();int d=Math.max(1,Math.min(31,num(d1,1))),iv=Math.max(1,num(i1,1));if(e1.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Preencha o Motor 1",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb_enabled",e1.isChecked()).putString("sgb_group",g).putString("sgb_message",m).putInt("sgb_day",d).putInt("sgb_start_minutes",sm1).putInt("sgb_interval_value",iv).putString("sgb_interval_unit",u1.getSelectedItemPosition()==1?"MINUTES":"HOURS").apply();ScheduledGroupBroadcast.resetNext(this);n1.setText("Próximo: "+safe1());Toast.makeText(this,"Motor 1 salvo",Toast.LENGTH_SHORT).show();}
 void save2(){String g=g2.getText().toString().trim(),m=m2.getText().toString().trim();int d=Math.max(1,Math.min(31,num(d2,1))),iv=Math.max(1,num(i2,1));if(e2.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Preencha o Motor 2",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb2_enabled",e2.isChecked()).putString("sgb2_group",g).putString("sgb2_message",m).putInt("sgb2_day",d).putInt("sgb2_start_minutes",sm2).putInt("sgb2_interval_value",iv).putString("sgb2_interval_unit",u2.getSelectedItemPosition()==1?"MINUTES":"HOURS").apply();ScheduledGroupBroadcast2.resetNext(this);n2.setText("Próximo: "+safe2());Toast.makeText(this,"Motor 2 salvo",Toast.LENGTH_SHORT).show();}
 String safe1(){try{return ScheduledGroupBroadcast.formatNext(this);}catch(Exception e){return "—";}}String safe2(){try{return ScheduledGroupBroadcast2.formatNext(this);}catch(Exception e){return "—";}}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
# Mantém o motor 1 da v2.1.27 intacto e adiciona um segundo relógio independente.
needle='''    private final android.os.Handler scheduledGroupHandler = new android.os.Handler(android.os.Looper.getMainLooper());'''
if needle not in s: raise SystemExit('ERRO v2.1.30: motor v2.1.27 não encontrado')
second=r'''    private final android.os.Handler scheduledGroupHandler2 = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable scheduledGroupTick2 = new Runnable() { @Override public void run() { runScheduledGroupBroadcast2IfDue(); scheduledGroupHandler2.postDelayed(this, 30000L); } };
'''
s=s.replace(needle,needle+'\n'+second,1)
s=s.replace('''        scheduledGroupHandler.postDelayed(scheduledGroupTick, 3000L);''','''        scheduledGroupHandler.postDelayed(scheduledGroupTick, 3000L);\n        scheduledGroupHandler2.removeCallbacks(scheduledGroupTick2);\n        scheduledGroupHandler2.postDelayed(scheduledGroupTick2, 5000L);''',1)
s=s.replace('''        scheduledGroupHandler.removeCallbacks(scheduledGroupTick);''','''        scheduledGroupHandler.removeCallbacks(scheduledGroupTick);\n        scheduledGroupHandler2.removeCallbacks(scheduledGroupTick2);''',1)
# Notificações novas acordam os dois motores, sem misturar estados.
s=s.replace('''        runScheduledGroupBroadcastIfDue();''','''        runScheduledGroupBroadcastIfDue();\n        runScheduledGroupBroadcast2IfDue();''',1)
anchor='''    private boolean sendScheduledGroupReply(Notification notification, String conversation, String replyText) {'''
if anchor not in s: raise SystemExit('ERRO v2.1.30: método de envio v2.1.27 ausente')
run2=r'''    private void runScheduledGroupBroadcast2IfDue() {
        long now = System.currentTimeMillis();
        if (!ScheduledGroupBroadcast2.due(this, now)) return;
        String wanted = ScheduledGroupBroadcast2.group(this);
        String text = ScheduledGroupBroadcast2.message(this);
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification item : active) {
                    if (item == null || !PACKAGE_NAME.equals(item.getPackageName())) continue;
                    Notification candidate = item.getNotification();
                    if (candidate == null || !isGroupNotification(candidate) || candidate.extras == null) continue;
                    String conv = firstNonEmpty(candidate.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE), candidate.extras.getCharSequence(Notification.EXTRA_SUB_TEXT), candidate.extras.getCharSequence(Notification.EXTRA_TITLE));
                    if (!wanted.equalsIgnoreCase(conv.trim())) continue;
                    if (sendScheduledGroupReply(candidate, wanted, text)) {
                        ScheduledGroupBroadcast2.sent(this, now);
                        return;
                    }
                }
            }
            ScheduledGroupBroadcast2.waiting(this, "Aguardando envio: " + wanted);
        } catch (Throwable e) {
            ScheduledGroupBroadcast2.waiting(this, "Falha temporária no Motor 2");
        }
    }

'''
s=s.replace(anchor,run2+anchor,1)
svc.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=D.replace('brand.addView(text("v2.1.27",10,MUTED,false));','brand.addView(text("v2.1.30",10,MUTED,false));');dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 101',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.30'",G,count=1);gradle.write_text(G,encoding='utf-8')

checks=[(java/'ScheduledGroupBroadcast2.java','sgb2_enabled'),(java/'ScheduledGroupBroadcastActivity.java','MOTOR 1'),(java/'ScheduledGroupBroadcastActivity.java','MOTOR 2'),(svc,'scheduledGroupTick2'),(svc,'runScheduledGroupBroadcast2IfDue'),(svc,'sendScheduledGroupReply(candidate, wanted, text)'),(gradle,"versionName '2.1.30'")]
for p,m in checks:
 if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.30: requisito ausente '+m)
print('v2.1.30: dois motores independentes sobre a base funcional v2.1.27; sem agregador multi-grupo')