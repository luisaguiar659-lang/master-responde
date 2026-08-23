from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

# Parte diretamente da v2.1.27. O relógio e sendScheduledGroupReply continuam os mesmos.
store=java/'ScheduledGroupBroadcast.java'
s=store.read_text(encoding='utf-8')
s=s.replace('public static String group(Context c){return p(c).getString("sgb_group","").trim();}', '''public static String group(Context c){String all=p(c).getString("sgb_groups","").trim();if(!all.isEmpty()){for(String x:all.split("\\n")){String g=x.trim();if(!g.isEmpty())return g;}}return p(c).getString("sgb_group","").trim();}\n public static java.util.List<String> groups(Context c){java.util.ArrayList<String> out=new java.util.ArrayList<>();String all=p(c).getString("sgb_groups","").trim();if(all.isEmpty())all=p(c).getString("sgb_group","").trim();for(String x:all.split("\\n")){String g=x.trim();if(!g.isEmpty()&&!out.contains(g))out.add(g);}return out;}''',1)
s=s.replace('public static boolean due(Context c,long now){return enabled(c)&&!group(c).isEmpty()&&!message(c).isEmpty()&&now>=nextAt(c);}', 'public static boolean due(Context c,long now){return enabled(c)&&!groups(c).isEmpty()&&!message(c).isEmpty()&&now>=nextAt(c);}',1)
s=s.replace('public static void sent(Context c,long now){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb_next_at",n).putLong("sgb_last_sent_at",now).putString("sgb_last_status","Enviado automaticamente").apply();}', 'public static void sent(Context c,long now){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb_next_at",n).putLong("sgb_last_sent_at",now).putString("sgb_last_status","Enviado automaticamente para todos os grupos de suporte").remove("sgb_cycle_sent").apply();}',1)
store.write_text(s,encoding='utf-8')

# Tela: troca nome único por lista, um grupo de suporte por linha.
act=java/'ScheduledGroupBroadcastActivity.java'; a=act.read_text(encoding='utf-8')
a=a.replace('r.addView(tv("NOME EXATO DO GRUPO",14,true));group=field("Ex.: Clientes MASTERFLIX",1);group.setText(p.getString("sgb_group",""));r.addView(group);', 'r.addView(tv("GRUPOS DE SUPORTE",14,true));r.addView(tv("Um nome de grupo por linha. A mesma mensagem será enviada para todos no mesmo ciclo.",11,false));group=field("Suporte 1\\nSuporte 2\\nSuporte 3",5);String savedGroups=p.getString("sgb_groups","");if(savedGroups.trim().isEmpty())savedGroups=p.getString("sgb_group","");group.setText(savedGroups);r.addView(group);',1)
a=a.replace('if(enabled.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Informe o grupo e a mensagem",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb_enabled",enabled.isChecked()).putString("sgb_group",g).putString("sgb_message",m)', 'if(enabled.isChecked()&&(g.isEmpty()||m.isEmpty())){Toast.makeText(this,"Informe os grupos de suporte e a mensagem",Toast.LENGTH_SHORT).show();return;}p.edit().putBoolean("sgb_enabled",enabled.isChecked()).putString("sgb_groups",g).putString("sgb_group",g.contains("\\n")?g.substring(0,g.indexOf("\\n")).trim():g).putString("sgb_message",m)',1)
a=a.replace('AVISOS AUTOMÁTICOS DO GRUPO','AVISOS AUTOMÁTICOS - GRUPOS DE SUPORTE')
act.write_text(a,encoding='utf-8')

# Motor 1: um relógio, uma mensagem, vários destinos. Entregas concluídas no ciclo ficam registradas
# para não duplicar caso um dos grupos ainda precise ser localizado.
s=svc.read_text(encoding='utf-8')
start=s.find('    private void runScheduledGroupBroadcastIfDue() {')
end=s.find('    private boolean sendScheduledGroupReply(',start)
if start<0 or end<0: raise SystemExit('ERRO v2.1.31: motor 1 da v2.1.27 não encontrado')
new=r'''    private void runScheduledGroupBroadcastIfDue() {
        long now = System.currentTimeMillis();
        if (!ScheduledGroupBroadcast.due(this, now)) return;
        java.util.List<String> targets = ScheduledGroupBroadcast.groups(this);
        String text = ScheduledGroupBroadcast.message(this);
        java.util.HashSet<String> done = new java.util.HashSet<>();
        String rawDone = prefs().getString("sgb_cycle_sent", "");
        if (!rawDone.isEmpty()) for (String x : rawDone.split("\\n")) if (!x.trim().isEmpty()) done.add(x.trim().toLowerCase(java.util.Locale.ROOT));
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (String wanted : targets) {
                    String key = wanted.toLowerCase(java.util.Locale.ROOT);
                    if (done.contains(key)) continue;
                    for (StatusBarNotification item : active) {
                        if (item == null || !PACKAGE_NAME.equals(item.getPackageName())) continue;
                        Notification candidate = item.getNotification();
                        if (candidate == null || !isGroupNotification(candidate) || candidate.extras == null) continue;
                        String conv = firstNonEmpty(candidate.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE), candidate.extras.getCharSequence(Notification.EXTRA_SUB_TEXT), candidate.extras.getCharSequence(Notification.EXTRA_TITLE));
                        if (!wanted.equalsIgnoreCase(conv.trim())) continue;
                        if (sendScheduledGroupReply(candidate, wanted, text)) { done.add(key); break; }
                    }
                }
            }
            StringBuilder saved = new StringBuilder(); for(String x:done){if(saved.length()>0)saved.append('\n');saved.append(x);} prefs().edit().putString("sgb_cycle_sent",saved.toString()).apply();
            boolean all = true; for(String wanted:targets) if(!done.contains(wanted.toLowerCase(java.util.Locale.ROOT))){all=false;break;}
            if (all) ScheduledGroupBroadcast.sent(this, now);
            else ScheduledGroupBroadcast.waiting(this, "Enviados " + done.size() + " de " + targets.size() + " grupos de suporte");
        } catch (Throwable e) {
            ScheduledGroupBroadcast.waiting(this, "Falha temporária no envio aos grupos de suporte");
        }
    }

'''
s=s[:start]+new+s[end:]
svc.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=D.replace('brand.addView(text("v2.1.27",10,MUTED,false));','brand.addView(text("v2.1.31",10,MUTED,false));');dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 102',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.31'",G,count=1);gradle.write_text(G,encoding='utf-8')
for p,m in [(store,'sgb_groups'),(store,'groups(Context c)'),(act,'GRUPOS DE SUPORTE'),(svc,'sgb_cycle_sent'),(svc,'ScheduledGroupBroadcast.groups(this)'),(gradle,"versionName '2.1.31'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.31: requisito ausente '+m)
print('v2.1.31: motor 1 da v2.1.27 preservado com lista de grupos de suporte e um único temporizador')