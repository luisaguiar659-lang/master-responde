from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

# v2.1.28: substitui apenas store/UI do agendador e adapta o loop para múltiplos itens.
# O método sendScheduledGroupReply, já validado na v2.1.27, é preservado sem alteração.
(java/'ScheduledGroupBroadcast.java').write_text(r'''package com.masterresponde.app;
import android.content.*;import org.json.*;import java.util.*;
public final class ScheduledGroupBroadcast{
 private static final String P="master_responde", KEY="sgb_items_v2";private ScheduledGroupBroadcast(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
 public static JSONArray items(Context c){
  String raw=p(c).getString(KEY,"");
  if(raw.isEmpty()){JSONArray a=migrate(c);save(c,a);return a;}
  try{return new JSONArray(raw);}catch(Exception e){return new JSONArray();}
 }
 private static JSONArray migrate(Context c){JSONArray a=new JSONArray();SharedPreferences x=p(c);String g=x.getString("sgb_group","").trim(),m=x.getString("sgb_message","").trim();if(g.isEmpty()&&m.isEmpty())return a;try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("enabled",x.getBoolean("sgb_enabled",false));o.put("group",g);o.put("message",m);o.put("day",x.getInt("sgb_day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));o.put("startMinutes",x.getInt("sgb_start_minutes",8*60));o.put("intervalValue",x.getInt("sgb_interval_value",3));o.put("intervalUnit",x.getString("sgb_interval_unit","HOURS"));o.put("nextAt",x.getLong("sgb_next_at",0L));o.put("lastSentAt",x.getLong("sgb_last_sent_at",0L));o.put("status",x.getString("sgb_last_status","Migrado da v2.1.27"));a.put(o);}catch(Exception ignored){}return a;}
 public static void save(Context c,JSONArray a){p(c).edit().putString(KEY,a==null?"[]":a.toString()).apply();}
 public static JSONObject fresh(){JSONObject o=new JSONObject();try{o.put("id",UUID.randomUUID().toString());o.put("enabled",true);o.put("group","");o.put("message","");o.put("day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH));o.put("startMinutes",8*60);o.put("intervalValue",3);o.put("intervalUnit","HOURS");o.put("nextAt",0L);o.put("lastSentAt",0L);o.put("status","Aguardando configuração");}catch(Exception ignored){}return o;}
 public static long intervalMillis(JSONObject o){int v=Math.max(1,o.optInt("intervalValue",3));String u=o.optString("intervalUnit","HOURS");long mins="MINUTES".equals(u)?v:(long)v*60L;return Math.min(525600L,mins)*60000L;}
 public static long computeFirst(JSONObject o,long now){Calendar x=Calendar.getInstance();x.setTimeInMillis(now);Calendar a=(Calendar)x.clone();a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);a.set(Calendar.DAY_OF_MONTH,Math.min(Math.max(1,o.optInt("day",x.get(Calendar.DAY_OF_MONTH))),a.getActualMaximum(Calendar.DAY_OF_MONTH)));int mins=o.optInt("startMinutes",8*60);a.set(Calendar.HOUR_OF_DAY,mins/60);a.set(Calendar.MINUTE,mins%60);long base=a.getTimeInMillis();if(base<=now){long step=intervalMillis(o),k=(now-base)/step+1L;return base+k*step;}return base;}
 public static long nextAt(JSONObject o,long now){long n=o.optLong("nextAt",0L);if(n<=0L){n=computeFirst(o,now);try{o.put("nextAt",n);}catch(Exception ignored){}}return n;}
 public static boolean due(JSONObject o,long now){return o.optBoolean("enabled",false)&&!o.optString("group","").trim().isEmpty()&&!o.optString("message","").trim().isEmpty()&&now>=nextAt(o,now);}
 public static void sent(JSONObject o,long now){long n=nextAt(o,now),step=intervalMillis(o);do{n+=step;}while(n<=now);try{o.put("nextAt",n);o.put("lastSentAt",now);o.put("status","Enviado automaticamente");}catch(Exception ignored){}}
 public static String format(long t){if(t<=0)return "—";return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(t));}
}''',encoding='utf-8')

(java/'ScheduledGroupBroadcastActivity.java').write_text(r'''package com.masterresponde.app;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.Typeface;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
public class ScheduledGroupBroadcastActivity extends Activity{
 LinearLayout box;JSONArray items;
 public void onCreate(Bundle b){super.onCreate(b);load();}
 TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,10,8,10);return t;}
 EditText field(String hint,int lines){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setBackgroundColor(Color.rgb(5,18,25));e.setPadding(16,14,16,14);e.setMinLines(lines);e.setGravity(lines>1?Gravity.TOP:Gravity.CENTER_VERTICAL);return e;}
 void load(){items=ScheduledGroupBroadcast.items(this);ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(1,5,9));box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(28,30,28,40);sc.addView(box,new ViewGroup.LayoutParams(-1,-2));box.addView(tv("AVISOS AUTOMÁTICOS",23,true));TextView sub=tv("Grupos ilimitados, cada um com mensagem e temporizador independentes.",13,false);sub.setTextColor(Color.rgb(0,225,255));box.addView(sub);for(int i=0;i<items.length();i++)card(items.optJSONObject(i),i);Button add=new Button(this);add.setText("+ ADICIONAR GRUPO");add.setOnClickListener(v->{items.put(ScheduledGroupBroadcast.fresh());ScheduledGroupBroadcast.save(this,items);load();});box.addView(add);Button save=new Button(this);save.setText("SALVAR TODOS OS GRUPOS");save.setOnClickListener(v->{captureAll();ScheduledGroupBroadcast.save(this,items);Toast.makeText(this,"Avisos automáticos salvos",Toast.LENGTH_SHORT).show();load();});box.addView(save);setContentView(sc);}
 void card(JSONObject o,int idx){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(14,18,14,24);c.setBackgroundColor(Color.rgb(3,12,18));TextView title=tv("GRUPO "+(idx+1),17,true);title.setTextColor(Color.rgb(0,225,255));c.addView(title);Switch en=new Switch(this);en.setText("Ativo");en.setTextColor(Color.WHITE);en.setChecked(o.optBoolean("enabled",true));c.addView(en);EditText group=field("Nome exato do grupo",1);group.setText(o.optString("group"));c.addView(group);EditText msg=field("Mensagem automática",5);msg.setText(o.optString("message"));c.addView(msg);EditText day=field("Dia de início: 1 a 31",1);day.setInputType(InputType.TYPE_CLASS_NUMBER);day.setText(String.valueOf(o.optInt("day",Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));c.addView(day);int[] sm={o.optInt("startMinutes",8*60)};TextView time=tv(String.format(Locale.getDefault(),"Horário: %02d:%02d",sm[0]/60,sm[0]%60),15,true);c.addView(time);Button pick=new Button(this);pick.setText("ESCOLHER HORÁRIO");pick.setOnClickListener(v->new TimePickerDialog(this,(x,h,m)->{sm[0]=h*60+m;time.setText(String.format(Locale.getDefault(),"Horário: %02d:%02d",h,m));},sm[0]/60,sm[0]%60,true).show());c.addView(pick);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);EditText interval=field("3",1);interval.setInputType(InputType.TYPE_CLASS_NUMBER);interval.setText(String.valueOf(o.optInt("intervalValue",3)));Spinner unit=new Spinner(this);String[] units={"HORAS","MINUTOS"};unit.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,units));unit.setSelection("MINUTES".equals(o.optString("intervalUnit","HOURS"))?1:0);row.addView(interval,new LinearLayout.LayoutParams(0,-2,1));row.addView(unit,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);long n=ScheduledGroupBroadcast.nextAt(o,System.currentTimeMillis());c.addView(tv("Próximo envio: "+ScheduledGroupBroadcast.format(n),12,true));c.addView(tv("Status: "+o.optString("status","Aguardando configuração"),11,false));Button test=new Button(this);test.setText("TESTAR ESTE GRUPO AGORA");test.setOnClickListener(v->{capture(o,en,group,msg,day,sm[0],interval,unit,true);ScheduledGroupBroadcast.save(this,items);Toast.makeText(this,"Envio deste grupo marcado para agora",Toast.LENGTH_SHORT).show();load();});c.addView(test);Button del=new Button(this);del.setText("REMOVER GRUPO");del.setOnClickListener(v->{JSONArray nitems=new JSONArray();for(int j=0;j<items.length();j++)if(j!=idx)nitems.put(items.optJSONObject(j));items=nitems;ScheduledGroupBroadcast.save(this,items);load();});c.addView(del);o.remove("_views");try{o.put("_viewIndex",idx);}catch(Exception ignored){}c.setTag(new Object[]{o,en,group,msg,day,sm,interval,unit});box.addView(c);}
 void capture(JSONObject o,Switch en,EditText group,EditText msg,EditText day,int start,EditText interval,Spinner unit,boolean test){try{int d=1,v=1;try{d=Integer.parseInt(day.getText().toString().trim());}catch(Exception ignored){}try{v=Integer.parseInt(interval.getText().toString().trim());}catch(Exception ignored){}d=Math.max(1,Math.min(31,d));v=Math.max(1,v);String g=group.getText().toString().trim(),m=msg.getText().toString().trim();o.put("enabled",en.isChecked());o.put("group",g);o.put("message",m);o.put("day",d);o.put("startMinutes",start);o.put("intervalValue",v);o.put("intervalUnit",unit.getSelectedItemPosition()==1?"MINUTES":"HOURS");o.put("nextAt",test?System.currentTimeMillis()-1000L:ScheduledGroupBroadcast.computeFirst(o,System.currentTimeMillis()));o.put("status",test?"Teste armado":"Programação salva");}catch(Exception ignored){}}
 void captureAll(){for(int i=0;i<box.getChildCount();i++){View v=box.getChildAt(i);Object tag=v.getTag();if(!(tag instanceof Object[]))continue;Object[] a=(Object[])tag;if(a.length!=8||!(a[0] instanceof JSONObject))continue;int[] sm=(int[])a[5];capture((JSONObject)a[0],(Switch)a[1],(EditText)a[2],(EditText)a[3],(EditText)a[4],sm[0],(EditText)a[6],(Spinner)a[7],false);}}
}''',encoding='utf-8')

s=svc.read_text(encoding='utf-8')
start=s.find('    private void runScheduledGroupBroadcastIfDue() {')
end=s.find('    private boolean sendScheduledGroupReply(',start)
if start<0 or end<0:raise SystemExit('ERRO v2.1.28: motor v2.1.27 não encontrado')
new=r'''    private void runScheduledGroupBroadcastIfDue() {
        long now=System.currentTimeMillis();
        org.json.JSONArray all=ScheduledGroupBroadcast.items(this);
        boolean changed=false;
        try {
            StatusBarNotification[] active=getActiveNotifications();
            for(int i=0;i<all.length();i++){
                org.json.JSONObject job=all.optJSONObject(i);
                if(job==null||!ScheduledGroupBroadcast.due(job,now))continue;
                String wanted=job.optString("group","").trim(),text=job.optString("message","").trim();
                boolean sent=false;
                if(active!=null)for(StatusBarNotification item:active){
                    if(item==null||!PACKAGE_NAME.equals(item.getPackageName()))continue;
                    Notification candidate=item.getNotification();
                    if(candidate==null||!isGroupNotification(candidate)||candidate.extras==null)continue;
                    String conv=firstNonEmpty(candidate.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),candidate.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),candidate.extras.getCharSequence(Notification.EXTRA_TITLE));
                    if(!wanted.equalsIgnoreCase(conv.trim()))continue;
                    if(sendScheduledGroupReply(candidate,wanted,text)){ScheduledGroupBroadcast.sent(job,now);sent=true;changed=true;break;}
                }
                if(!sent){try{job.put("status","Aguardando envio: "+wanted);}catch(Exception ignored){}changed=true;}
            }
        } catch(Throwable ignored) {}
        if(changed)ScheduledGroupBroadcast.save(this,all);
    }

'''
s=s[:start]+new+s[end:]
svc.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=D.replace('brand.addView(text("v2.1.27",10,MUTED,false));','brand.addView(text("v2.1.28",10,MUTED,false));');dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 99',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.28'",G,count=1);gradle.write_text(G,encoding='utf-8')
for p,m in [(java/'ScheduledGroupBroadcast.java','sgb_items_v2'),(java/'ScheduledGroupBroadcastActivity.java','+ ADICIONAR GRUPO'),(java/'ScheduledGroupBroadcastActivity.java','TESTAR ESTE GRUPO AGORA'),(svc,'ScheduledGroupBroadcast.items(this)'),(svc,'sendScheduledGroupReply(candidate,wanted,text)'),(gradle,"versionName '2.1.28'")]:
 if m not in p.read_text(encoding='utf-8'):raise SystemExit('ERRO v2.1.28: requisito ausente '+m)
print('v2.1.28: avisos automáticos com grupos ilimitados e temporizadores independentes; envio v2.1.27 preservado')