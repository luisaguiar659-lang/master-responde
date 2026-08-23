from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; store=java/'ScheduledGroupBroadcast.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

# Corrige duas regressões da v2.1.31/32:
# 1) vários PendingIntents eram disparados no mesmo ciclo/tick;
# 2) um único grupo não localizado bloqueava para sempre o avanço do relógio.
# O método sendScheduledGroupReply da v2.1.27 permanece intacto.

s=store.read_text(encoding='utf-8')
anchor=''' public static void sent(Context c,long now){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb_next_at",n).putLong("sgb_last_sent_at",now).putString("sgb_last_status","Enviado automaticamente para todos os grupos de suporte").remove("sgb_cycle_sent").apply();}'''
if anchor not in s: raise SystemExit('ERRO v2.1.33: método sent da lista de suporte não encontrado')
replacement=''' public static void sent(Context c,long now){finishCycle(c,now,"Enviado automaticamente para todos os grupos de suporte");}\n public static void finishCycle(Context c,long now,String status){long step=intervalMinutes(c)*60000L,n=nextAt(c);if(n<=0L)n=now;do{n+=step;}while(n<=now);p(c).edit().putLong("sgb_next_at",n).putLong("sgb_last_sent_at",now).putString("sgb_last_status",status).remove("sgb_cycle_sent").remove("sgb_cycle_started_at").apply();}\n public static long retryWindowMillis(Context c){long half=intervalMinutes(c)*60000L/2L;return Math.max(20000L,Math.min(120000L,half));}'''
s=s.replace(anchor,replacement,1)
store.write_text(s,encoding='utf-8')

s=svc.read_text(encoding='utf-8')
# Aumenta apenas a frequência de verificação do mesmo motor. Não altera o envio.
s=s.replace('scheduledGroupHandler.postDelayed(this, 30000L);','scheduledGroupHandler.postDelayed(this, 3000L);',1)

start=s.find('    private void runScheduledGroupBroadcastIfDue() {')
end=s.find('    private boolean sendScheduledGroupReply(',start)
if start<0 or end<0: raise SystemExit('ERRO v2.1.33: motor de grupos de suporte não encontrado')
new=r'''    private void runScheduledGroupBroadcastIfDue() {
        long now = System.currentTimeMillis();
        if (!ScheduledGroupBroadcast.due(this, now)) return;

        java.util.List<String> targets = ScheduledGroupBroadcast.groups(this);
        if (targets.isEmpty()) return;
        String text = ScheduledGroupBroadcast.message(this);
        SharedPreferences p = prefs();

        long cycleStarted = p.getLong("sgb_cycle_started_at", 0L);
        if (cycleStarted <= 0L) {
            cycleStarted = now;
            p.edit().putLong("sgb_cycle_started_at", cycleStarted).remove("sgb_cycle_sent").apply();
        }

        java.util.HashSet<String> done = new java.util.HashSet<>();
        String rawDone = p.getString("sgb_cycle_sent", "");
        if (!rawDone.isEmpty()) {
            for (String x : rawDone.split("\\n")) {
                String k = x.trim().toLowerCase(java.util.Locale.ROOT);
                if (!k.isEmpty()) done.add(k);
            }
        }

        // Um destino por tick. Isso evita disputar vários PendingIntents do WhatsApp
        // praticamente no mesmo instante e dá tempo para cada envio ser processado.
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (String wanted : targets) {
                    String key = wanted.trim().toLowerCase(java.util.Locale.ROOT);
                    if (key.isEmpty() || done.contains(key)) continue;

                    for (StatusBarNotification item : active) {
                        if (item == null || !PACKAGE_NAME.equals(item.getPackageName())) continue;
                        Notification candidate = item.getNotification();
                        if (candidate == null || !isGroupNotification(candidate) || candidate.extras == null) continue;
                        String conv = firstNonEmpty(
                                candidate.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
                                candidate.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                                candidate.extras.getCharSequence(Notification.EXTRA_TITLE));
                        if (!wanted.equalsIgnoreCase(conv.trim())) continue;

                        if (sendScheduledGroupReply(candidate, wanted, text)) {
                            done.add(key);
                            saveScheduledSupportDone(done);
                            if (allScheduledSupportDone(targets, done)) {
                                ScheduledGroupBroadcast.sent(this, now);
                            } else {
                                ScheduledGroupBroadcast.waiting(this, "Enviados " + done.size() + " de " + targets.size() + " grupos de suporte");
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Um grupo que não puder ser alcançado não pode congelar o motor inteiro.
        // Depois da janela curta de novas tentativas, fecha o ciclo e o próximo
        // intervalo continua normalmente. No ciclo seguinte todos são tentados de novo.
        long age = now - cycleStarted;
        if (age >= ScheduledGroupBroadcast.retryWindowMillis(this)) {
            ScheduledGroupBroadcast.finishCycle(this, now,
                    "Ciclo concluído: " + done.size() + " de " + targets.size() + " grupos enviados");
        } else {
            ScheduledGroupBroadcast.waiting(this,
                    "Tentando grupos de suporte: " + done.size() + " de " + targets.size() + " enviados");
        }
    }

    private void saveScheduledSupportDone(java.util.Set<String> done) {
        StringBuilder b = new StringBuilder();
        for (String x : done) { if (b.length() > 0) b.append('\n'); b.append(x); }
        prefs().edit().putString("sgb_cycle_sent", b.toString()).apply();
    }

    private boolean allScheduledSupportDone(java.util.List<String> targets, java.util.Set<String> done) {
        for (String wanted : targets) {
            String k = wanted == null ? "" : wanted.trim().toLowerCase(java.util.Locale.ROOT);
            if (!k.isEmpty() && !done.contains(k)) return false;
        }
        return true;
    }

'''
s=s[:start]+new+s[end:]
svc.write_text(s,encoding='utf-8')

D=dash.read_text(encoding='utf-8');D=D.replace('brand.addView(text("v2.1.32",10,MUTED,false));','brand.addView(text("v2.1.33",10,MUTED,false));');dash.write_text(D,encoding='utf-8')
G=gradle.read_text(encoding='utf-8');G=re.sub(r'versionCode\s+\d+','versionCode 104',G,count=1);G=re.sub(r"versionName\s+'[^']+'","versionName '2.1.33'",G,count=1);gradle.write_text(G,encoding='utf-8')

checks=[(store,'retryWindowMillis'),(store,'finishCycle'),(svc,'scheduledGroupHandler.postDelayed(this, 3000L)'),(svc,'Um destino por tick'),(svc,'allScheduledSupportDone'),(svc,'sgb_cycle_started_at'),(gradle,"versionName '2.1.33'")]
for pth,m in checks:
 if m not in pth.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.33: requisito ausente '+m)
print('v2.1.33: envio serializado por grupo e ciclos recorrentes não ficam mais bloqueados por um destino')