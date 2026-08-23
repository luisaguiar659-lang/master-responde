from pathlib import Path
import re

app=Path('projeto/app')
java=app/'src/main/java/com/masterresponde/app'
service=java/'WhatsAppBusinessCaptureService.java'
settings=java/'MessageSettingsActivity.java'
gradle=app/'build.gradle'

s=service.read_text(encoding='utf-8')
old='''        String resolvedReply = CommandEngine.resolve(this, message);
        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {
            long wait = ConversationCooldown.remainingSeconds(this, conversation);
            if (wait > 0) {
                prefs().edit().putString("accessibility_last_status", "Temporizador ativo para esta pessoa • aguarde " + wait + "s").apply();
                return;
            }
            sendDirectReply(n, conversation, resolvedReply);
        } else {
'''
new='''        String resolvedReply = CommandEngine.resolve(this, message);
        if (resolvedReply != null && !resolvedReply.trim().isEmpty()) {
            String matchedType = prefs().getString("last_matched_command", "");
            boolean isFallbackReply = "RESPOSTA_PADRAO".equals(matchedType);
            if (isFallbackReply) {
                long wait = ConversationCooldown.remainingSeconds(this, conversation);
                if (wait > 0) {
                    prefs().edit().putString("accessibility_last_status", "Resposta padrão em temporizador para esta pessoa • aguarde " + wait + "s").apply();
                    return;
                }
            }
            sendDirectReply(n, conversation, resolvedReply);
        } else {
'''
if old not in s:
    raise SystemExit('ERRO v2.1.13: bloco de cooldown v2.1.12 não encontrado')
s=s.replace(old,new,1)
# Não marcar cooldown para comandos. Marcar somente após envio bem sucedido quando a resposta resolvida era fallback.
oldmark='''                action.actionIntent.send(this, 0, fillIn);
                ConversationCooldown.markSent(this, conversation);'''
newmark='''                action.actionIntent.send(this, 0, fillIn);
                if ("RESPOSTA_PADRAO".equals(prefs().getString("last_matched_command", ""))) {
                    ConversationCooldown.markSent(this, conversation);
                }'''
if oldmark not in s:
    raise SystemExit('ERRO v2.1.13: marcação de cooldown v2.1.12 não encontrada')
s=s.replace(oldmark,newmark,1)
service.write_text(s,encoding='utf-8')

# Ajustar texto da tela para deixar inequívoco que o timer pertence à resposta padrão.
t=settings.read_text(encoding='utf-8')
t=t.replace('TEMPORIZADOR POR PESSOA','TEMPORIZADOR DA RESPOSTA PADRÃO')
t=t.replace('Depois de responder uma pessoa, aguarda este tempo antes de responder a mesma pessoa novamente. Outras pessoas continuam sendo respondidas normalmente.','Depois de enviar a RESPOSTA PADRÃO para uma pessoa, aguarda este tempo antes de enviar outra resposta padrão para a mesma pessoa. Comandos continuam respondendo normalmente e cada pessoa possui seu próprio temporizador.')
t=t.replace('Comandos e temporizador salvos','Comandos e temporizador da resposta padrão salvos')
settings.write_text(t,encoding='utf-8')

g=gradle.read_text(encoding='utf-8')
g=re.sub(r'versionCode\s+\d+','versionCode 84',g,count=1)
g=re.sub(r"versionName\s+'[^']+'","versionName '2.1.13'",g,count=1)
gradle.write_text(g,encoding='utf-8')

checks=[(service,'boolean isFallbackReply'),(service,'"RESPOSTA_PADRAO".equals(matchedType)'),(service,'ConversationCooldown.markSent'),(settings,'TEMPORIZADOR DA RESPOSTA PADRÃO'),(settings,'Comandos continuam respondendo normalmente')]
for p,m in checks:
    if m not in p.read_text(encoding='utf-8'):
        raise SystemExit('ERRO v2.1.13: requisito ausente: '+m)
print('v2.1.13: cooldown restrito à RESPOSTA PADRAO; comandos ilimitados não sofrem bloqueio')