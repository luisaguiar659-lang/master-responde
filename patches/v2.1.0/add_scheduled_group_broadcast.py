from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; dash=java/'NeonDashboardActivity.java'; manifest=app/'src/main/AndroidManifest.xml'; gradle=app/'build.gradle'

(java/'ScheduledGroupBroadcast.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import java.util.*;
public final class ScheduledGroupBroadcast{
 private static final String P="master_responde";private ScheduledGroupBroadcast(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return p(c).getBoolean("sgb_enabled",false);}
 public static String group(Context c){return p(c).getString("sgb_group","").trim();}
 public static String message(Context c){return p(c).getString("sgb_message","").trim();}
 public static int intervalMinutes(Context c){int v=Math.max(1,p(c).getInt("sgb_interval_value",3));String u=p(c).getString("sgb_interval_unit","HOURS");long m="MINUTES".equals(u)?v:(long)v*60L;return (int)Math.min(525600L,m);}
 public static long nextAt(Context c){long n=p(c).getLong("sgb_next_at",0L);if(n<=0L){n=computeFirst(c,System.currentTimeMillis());p(c).edit().putLong("sgb_next_at",n).apply();}return n;}
 public static long computeFirst(Context c,long now){Calendar x=Calendar.getInstance();x.setTimeInMillis(now);int wanted=p(c).getInt("sgb_day",x.get(Calendar.DAY_OF_MONTH));int mins=p(c).getInt("sgb_start_minutes",8*60);Calendar a=(Calendar)x.clone();a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);a.set(Calendar.DAY_OF_MONTH,Math.min(Math.max(1,wanted),a.getActualMaximum(Calendar.DAY_OF_MONTH)));a.set(Calendar.HOUR_OF_DAY,mins/60);a.set(Calendar.MINUTE,mins%60);if(a.getTimeInMillis()<=now){long step=intervalMinutes(c)*60000L;long base=a.getTimeInMillis();long k=(now-base)/step+1L;return base+k*step;}return a.getTimeInMillis();}
 public static boolean due(Context c,long now){return enabled(c)&&!group(c).isEmpty()&&!message(c).isEmpty()&&now>=nextAt(c);}
 public static void sent(Context c,long now){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb_next_at",n).putLong("sgb_last_sent_at",now).putString("sgb_last_status","Enviado automaticamente").apply();}
 public static void waiting(Context c,String status){p(c).edit().putString("sgb_last_status",status).apply();}
 public static void resetNext(Context c){p(c).edit().putLong("sgb_next_at",computeFirst(c,System.currentTimeMillis())).apply();}
 public static String formatNext(Context c){long n=nextAt(c);return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(n));}
}''',encoding='utf-8')

(java/'ScheduledGroupBroadcastActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;import java.util.*;
public class ScheduledGroupBroadcastActivity extends Activity{
 SharedPreferences p;Switch enabled;EditText group,msg,day,interval;Spinner unit;TextView start,next,status;int startMinutes;
 public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("master_responde",MODE_PRIVATE);startMinutes=p.getInt("sgb_start_minutes",8*60);setContentView(build());}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 EditText field(String hint,int lines){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);e.setMinLines(lines);e.setGravity(lines>1?Gravity.TOP:Gravity.CENTER_VERTICAL);return e;}
 View build(){ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,30,28,40);sc.addView(r,new ViewGroup.LayoutParams(-1,-2));r.addView(tv("AVISOS AUTOMÁTICOS DO GRUPO",23,true));TextView sub=tv("Envia uma mensagem automaticamente, sem gatilho, no intervalo configurado.",13,false);sub.setTextColor(Color.rgb(0,225,255));r.addView(sub);enabled=new Switch(this);enabled.setText("Ativar motor de avisos");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean("sgb_enabled",false));r.addView(enabled);r.addView(tv("NOME EXATO DO GRUPO",14,true));group=field("Ex.: Clientes MASTERFLIX",1);group.setText(p.getString("sgb_group",""));r.addView(group);r.addView(tv("MENSAGEM AUTOMÁTICA",14,true));msg=field("Ex.: Lembrete: seu vencimento é dia 10.",5);msg.setText(p.getString("sgb_message",""));r.addView(msg);r.addView(tv("DIA DE INÍCIO",14,true));r.addView(tv("Dia do mês em que o ciclo começa. Depois disso a mensagem segue o intervalo configurado.",11,false));day=field("1 a 31",1);day.setInputType(InputType.TYPE_CLASS_NUMBER);day.setText(String.valueOf(p.getInt("sgb_day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));r.addView(day);r.addView(tv("HORÁRIO DE INÍCIO",14,true));start=tv("",18,true);refreshStart();r.addView(start);Button pick=new Button(this);pick.setText("ESCOLHER HORÁRIO");pick.setOnClickListener(v->new TimePickerDialog(this,(x,h,m)->{startMinutes=h*60+m;refreshStart();},startMinutes/60,startMinutes%60,true).show());r.addView(pick);r.addView(tv("INTERVALO",14,true));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);interval=field("3",1);interval.setInputType(InputType.TYPE_CLASS_NUMBER);interval.setText(String.valueOf(p.getInt("sgb_interval_value",3)));unit=new Spinner(this);String[] units={"HORAS","MINUTOS"};ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,units);unit.setAdapter(a);unit.setSelection("MINUTES".equals(p.getString("sgb_interval_unit","HOURS"))?1:0);row.addView(interval,new LinearLayout.LayoutParams(0,-2,1));row.addView(unit,new LinearLayout.LayoutParams(0,-2,1));r.addView(row);next=tv("Próximo envio: "+safeNext(),13,true);r.addView(next);status=tv("Status: "+p.getString("sgb_last_status","Aguardando configuração"),12,false);r.addView(status);Button save=new Button(this);save.setText("SALVAR E ATIVAR PROGRAMAÇÃO");save.setOnClickListener(v->save());r.addView(save);Button test=new Button(this);test.setText("DEIXAR ENVIO VENCIDO PARA TESTE");test.setOnClickListener(v->{p.edit().putLong("sgb_next_at",System.currentTimeMillis()-1000L).apply();next.setText("Próximo envio: agora");Toast.makeText(this,"Teste armado. O motor tentará enviar ao grupo.",Toast.LENGTH_LONG).show();});r.addView(test);r.addView(tv("IMPORTANTE: para enviar sem abrir o WhatsApp, o Android precisa manter uma notificação ativa desse grupo com a ação Responder. Se não houver, o motor fica aguardando e tenta novamente quando o grupo voltar a ter uma notificação ativa.",11,false));return sc;}
 void refreshStart(){if(start!=null)start.setText(String.format(Locale.getDefault(),"%02d:%02d",startMinutes/60,startMinutes%60));}
 String safeNext(){try{return ScheduledGroupBroadcast.formatNext(this);}catch(Exception e){return "—";}}
 void save(){String g=group.getText().toString().trim(),m=msg.getText().toString().trim();int d=1,v=1;try{d=Integer.parseInt(day.getText().toString().trim());}catch(Exception ignored){}try{v=Integer.parseInt(interval.getText().toString().trim());}catch(Exception ignored){}d=Math.max(1,Math.min(31,d));v=Math.max(1,v);String u=unit.getSelectedItemPosition()==1?"MINUTES":"HOURS";if(enabled.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Informe o grupo e a mensagem",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb_enabled",enabled.isChecked()).putString("sgb_group",g).putString("sgb_message",m).putInt("sgb_day",d).putInt("sgb_start_minutes",startMinutes).putInt("sgb_interval_value",v).putString("sgb_interval_unit",u).apply();ScheduledGroupBroadcast.resetNext(this);next.setText("Próximo envio: "+safeNext());status.setText("Status: programação salva");Toast.makeText(this,"Programação salva",Toast.LENGTH_SHORT).show();}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
# Inicia relógio persistente enquanto o NotificationListener estiver conectado.
needle='''        super.onListenerConnected();'''
repl='''        super.onListenerConnected();\n        startScheduledGroupBroadcastLoop();'''
if needle not in s: raise SystemExit('ERRO v2.1.27: onListenerConnected não encontrado')
s=s.replace(needle,repl,1)
needle2='''        super.onListenerDisconnected();'''
repl2='''        stopScheduledGroupBroadcastLoop();\n        super.onListenerDisconnected();'''
s=s.replace(needle2,repl2,1)
# Também tenta imediatamente quando chega nova notificação do WhatsApp Business, útil quando estava aguardando o grupo.
anchor='''        Notification n = sbn.getNotification();'''
s=s.replace(anchor,'''        Notification n = sbn.getNotification();\n        runScheduledGroupBroadcastIfDue();''',1)
pos=s.rfind('}')
helper=r'''
    private final android.os.Handler scheduledGroupHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable scheduledGroupTick = new Runnable() { @Override public void run() { runScheduledGroupBroadcastIfDue(); scheduledGroupHandler.postDelayed(this, 30000L); } };

    private void startScheduledGroupBroadcastLoop() {
        scheduledGroupHandler.removeCallbacks(scheduledGroupTick);
        scheduledGroupHandler.postDelayed(scheduledGroupTick, 3000L);
    }

    private void stopScheduledGroupBroadcastLoop() {
        scheduledGroupHandler.removeCallbacks(scheduledGroupTick);
    }

    private void runScheduledGroupBroadcastIfDue() {
        long now = System.currentTimeMillis();
        if (!ScheduledGroupBroadcast.due(this, now)) return;
        String wanted = ScheduledGroupBroadcast.group(this);
        String text = ScheduledGroupBroadcast.message(this);
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
                        ScheduledGroupBroadcast.sent(this, now);
                        return;
                    }
                }
            }
            ScheduledGroupBroadcast.waiting(this, "Aguardando notificação ativa do grupo: " + wanted);
        } catch (Throwable e) {
            ScheduledGroupBroadcast.waiting(this, "Falha temporária no envio automático");
        }
    }

    private boolean sendScheduledGroupReply(Notification notification, String conversation, String replyText) {
        if (notification == null || notification.actions == null) return false;
        for (Notification.Action action : notification.actions) {
            if (action == null || action.actionIntent == null) continue;
            android.app.RemoteInput[] inputs = action.getRemoteInputs();
            if (inputs == null || inputs.length == 0) continue;
            try {
                android.content.Intent fill = new android.content.Intent();
                android.os.Bundle results = new android.os.Bundle();
                for (android.app.RemoteInput input : inputs) if (input != null && input.getResultKey() != null) results.putCharSequence(input.getResultKey(), replyText);
                android.app.RemoteInput.addResultsToIntent(inputs, fill, results);
                action.actionIntent.send(this, 0, fill);
                prefs().edit().putString("accessibility_last_conversation", conversation).putString("accessibility_last_reply", replyText).putString("accessibility_last_status", "Aviso automático enviado ao grupo").putString("last_bot_reply_text", replyText).putLong("last_bot_reply_at", System.currentTimeMillis()).putInt("reply_sent_total", prefs().getInt("reply_sent_total",0)+1).putInt("attempt_total", prefs().getInt("attempt_total",0)+1).apply();
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
'''
s=s[:pos]+helper+s[pos:]
svc.write_text(s,encoding='utf-8')

# Menu lateral
D=dash.read_text(encoding='utf-8')
needle3='''  panel.addView(sideItem("☷","Menu do Grupo",Color.WHITE,v->{d.dismiss();try{startActivity(new Intent(this,GroupMenuActivity.class));}catch(Throwable e){Toast.makeText(this,"Menu do grupo indisponível",Toast.LENGTH_SHORT).show();}}));'''
repl3=needle3+'''\n  panel.addView(sideItem("⏱","Avisos Automáticos",Color.WHITE,v->{d.dismiss();startActivity(new Intent(this,ScheduledGroupBroadcastActivity.class));}));'''
if needle3 not in D: raise SystemExit('ERRO v2.1.27: item Menu do Grupo não encontrado')
D=D.replace(needle3,repl3,1)
for v in ['v2.1.25','v2.1.26']:
 D=D.replace('brand.addView(text("'+v+'",10,MUTED,false));','brand.addView(text("v2.1.27",10,MUTED,false));')
dash.write_text(D,encoding='utf-8')

M=manifest.read_text(encoding='utf-8')
if '.ScheduledGroupBroadcastActivity' not in M:M=M.replace('</application>','<activity android:name=".ScheduledGroupBroadcastActivity" android:screenOrientation="portrait" android:exported="false"/>\n</application>')
manifest.write_text(M,encoding='utf-8')

G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 98',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.27'",G,count=1);gradle.write_text(G,encoding='utf-8')
for p,m in [(java/'ScheduledGroupBroadcast.java','intervalMinutes'),(java/'ScheduledGroupBroadcastActivity.java','AVISOS AUTOMÁTICOS DO GRUPO'),(svc,'runScheduledGroupBroadcastIfDue'),(svc,'sendScheduledGroupReply'),(dash,'Avisos Automáticos'),(manifest,'.ScheduledGroupBroadcastActivity'),(gradle,"versionName '2.1.27'")]:
 if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.27: requisito ausente '+m)
print('v2.1.27: motor de avisos automáticos em grupo criado, com dia/hora inicial e intervalo em minutos ou horas')