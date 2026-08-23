from pathlib import Path
import re
app=Path('projeto/app'); java=app/'src/main/java/com/masterresponde/app'; svc=java/'WhatsAppBusinessCaptureService.java'; gradle=app/'build.gradle'; dash=java/'NeonDashboardActivity.java'

s=svc.read_text(encoding='utf-8')
old='''        if (BusinessHours.enabled(this) && !BusinessHours.isOpen(this)) {
            String awayReply = BusinessHours.away(this);
            if (awayReply != null && !awayReply.trim().isEmpty()) {
                prefs().edit().putString("last_matched_command", "FORA_HORARIO").apply();
                sendDirectReply(n, conversation, awayReply.trim());
            }
            return;
        }
'''
new='''        if (BusinessHours.enabled(this) && !BusinessHours.isOpen(this)) {
            long wait = ConversationCooldown.remainingSeconds(this, conversation);
            if (wait > 0) {
                prefs().edit().putString("accessibility_last_status", "Mensagem fora do horário em temporizador para esta pessoa • aguarde " + wait + "s").apply();
                return;
            }
            String awayReply = BusinessHours.away(this);
            if (awayReply != null && !awayReply.trim().isEmpty()) {
                prefs().edit().putString("last_matched_command", "FORA_HORARIO").apply();
                sendDirectReply(n, conversation, awayReply.trim());
            }
            return;
        }
'''
if old not in s: raise SystemExit('ERRO v2.1.19: bloco de horário não encontrado')
s=s.replace(old,new,1)
# O ponto de envio já marca cooldown apenas para RESPOSTA_PADRAO; ampliar para FORA_HORARIO.
s=s.replace('''if ("RESPOSTA_PADRAO".equals(prefs().getString("last_matched_command", ""))) {
                    ConversationCooldown.markSent(this, conversation);
                }''','''String cooldownType = prefs().getString("last_matched_command", "");
                if ("RESPOSTA_PADRAO".equals(cooldownType) || "FORA_HORARIO".equals(cooldownType)) {
                    ConversationCooldown.markSent(this, conversation);
                }''',1)
svc.write_text(s,encoding='utf-8')

d=dash.read_text(encoding='utf-8').replace('brand.addView(text("v2.1.18",10,MUTED,false));','brand.addView(text("v2.1.19",10,MUTED,false));',1)
dash.write_text(d,encoding='utf-8')

g=gradle.read_text(encoding='utf-8');g=re.sub(r'versionCode\s+\d+','versionCode 90',g,count=1);g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.19'",g,count=1);gradle.write_text(g,encoding='utf-8')
for p,m in [(svc,'Mensagem fora do horário em temporizador'),(svc,'"FORA_HORARIO".equals(cooldownType)'),(gradle,"versionName '2.1.19'")]:
 if m not in p.read_text(encoding='utf-8'): raise SystemExit('ERRO v2.1.19: requisito ausente '+m)
print('v2.1.19: horário de atendimento compartilha o temporizador individual da resposta padrão')