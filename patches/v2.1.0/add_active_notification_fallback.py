from pathlib import Path
import re

app = Path('projeto/app')
java_dir = app / 'src/main/java/com/masterresponde/app'
service = java_dir / 'WhatsAppBusinessCaptureService.java'
gradle = app / 'build.gradle'

s = service.read_text(encoding='utf-8')

start = s.find('    private void sendDirectReply(Notification notification, String conversation, String replyText) {')
end = s.find('    private void markReplyFailure(String status) {', start)
if start < 0 or end < 0:
    raise SystemExit('ERRO v2.1.5: bloco de envio não encontrado')

new_methods = r'''    private void sendDirectReply(Notification notification, String conversation, String replyText) {
        // 1) Tenta a própria notificação recebida.
        if (tryReplyOnNotification(notification, conversation, replyText)) return;

        // 2) Algumas notificações do WhatsApp Business são resumos e não possuem
        // ação Responder. Procura então uma notificação ATIVA da MESMA conversa.
        if (conversation != null && !conversation.trim().isEmpty()) {
            try {
                StatusBarNotification[] active = getActiveNotifications();
                if (active != null) {
                    for (StatusBarNotification item : active) {
                        if (item == null || !PACKAGE_NAME.equals(item.getPackageName())) continue;
                        Notification candidate = item.getNotification();
                        if (candidate == null || candidate == notification) continue;
                        String candidateConversation = conversationOf(candidate);
                        if (!sameConversation(conversation, candidateConversation)) continue;
                        if (tryReplyOnNotification(candidate, conversation, replyText)) return;
                    }
                }
            } catch (Throwable ignored) {
                // A falha final abaixo será registrada no Dashboard.
            }
        }

        markReplyFailure("Falha: nenhuma ação Responder para esta conversa");
    }

    private boolean tryReplyOnNotification(Notification notification, String conversation, String replyText) {
        if (notification == null) return false;
        Notification.Action[] actions = notification.actions;
        if (actions == null || actions.length == 0) return false;

        for (Notification.Action action : actions) {
            if (action == null || action.actionIntent == null) continue;
            RemoteInput[] inputs = action.getRemoteInputs();
            if (inputs == null || inputs.length == 0) continue;

            try {
                Intent fillIn = new Intent();
                Bundle results = new Bundle();
                for (RemoteInput input : inputs) {
                    if (input != null && input.getResultKey() != null) {
                        results.putCharSequence(input.getResultKey(), replyText);
                    }
                }
                RemoteInput.addResultsToIntent(inputs, fillIn, results);
                action.actionIntent.send(this, 0, fillIn);
                prefs().edit()
                        .putString("accessibility_last_conversation", conversation)
                        .putString("accessibility_last_reply", replyText)
                        .putString("accessibility_last_status", "Respondida com sucesso • motor v2")
                        .putString("last_bot_reply_text", replyText)
                        .putLong("last_bot_reply_at", System.currentTimeMillis())
                        .putInt("reply_sent_total", prefs().getInt("reply_sent_total", 0) + 1)
                        .putInt("attempt_total", prefs().getInt("attempt_total", 0) + 1)
                        .putInt("attendance_total", prefs().getInt("attendance_total", 0) + 1)
                        .apply();
                return true;
            } catch (PendingIntent.CanceledException | RuntimeException ignored) {
                // Não marca falha ainda: pode existir outra ação/notificação válida.
            }
        }
        return false;
    }

    private String conversationOf(Notification notification) {
        if (notification == null || notification.extras == null) return "";
        Bundle e = notification.extras;
        return firstNonEmpty(
                e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
                e.getCharSequence(Notification.EXTRA_TITLE),
                e.getCharSequence(Notification.EXTRA_SUB_TEXT)
        );
    }

    private boolean sameConversation(String expected, String actual) {
        if (expected == null || actual == null) return false;
        String a = expected.trim();
        String b = actual.trim();
        return !a.isEmpty() && a.equalsIgnoreCase(b);
    }

'''

s = s[:start] + new_methods + s[end:]
service.write_text(s, encoding='utf-8')

g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 76', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName '2.1.5'", g, count=1)
gradle.write_text(g, encoding='utf-8')

final = service.read_text(encoding='utf-8')
for required in ['getActiveNotifications()', 'sameConversation(', 'tryReplyOnNotification(', 'nenhuma ação Responder para esta conversa']:
    if required not in final:
        raise SystemExit('ERRO v2.1.5: requisito ausente: ' + required)
print('v2.1.5: fallback seguro para notificação ativa da mesma conversa implementado')
