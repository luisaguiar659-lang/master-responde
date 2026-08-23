from pathlib import Path
import re

path = Path('projeto/app/src/main/java/com/masterresponde/app/NotificationAccessService.java')
text = path.read_text(encoding='utf-8')

# Cacheia a ação REAL de resposta do WhatsApp Business para permitir envio em
# background sem exigir abrir a conversa/teclado. O fallback continua sendo a
# resposta via AccessibilityService quando não houver ação real disponível.
field_anchor = '    private static volatile NotificationAccessService activeInstance;\n'
fields = '''    private static final java.util.concurrent.ConcurrentHashMap<String, android.app.Notification.Action> MR_BUSINESS_REPLY_ACTIONS = new java.util.concurrent.ConcurrentHashMap<>();\n    private static final java.util.concurrent.ConcurrentHashMap<String, Long> MR_BUSINESS_REPLY_ACTION_TIMES = new java.util.concurrent.ConcurrentHashMap<>();\n    private static final long MR_REPLY_ACTION_TTL_MS = 10L * 60L * 1000L;\n'''
if 'MR_BUSINESS_REPLY_ACTIONS' not in text:
    if field_anchor not in text:
        raise SystemExit('ERRO background_private_reply: activeInstance não encontrado')
    text = text.replace(field_anchor, field_anchor + '\n' + fields, 1)

# Em toda notificação do WhatsApp Business, guarda a primeira ação com RemoteInput.
method = re.search(r'(public\s+void\s+onNotificationPosted\s*\(\s*StatusBarNotification\s+sbn\s*\)\s*\{)', text)
if not method:
    raise SystemExit('ERRO background_private_reply: onNotificationPosted não encontrado')
cache_call = 'mrCacheBusinessReplyAction(sbn);'
if cache_call not in text:
    # Insere após a guarda Business-only, se houver; caso contrário logo no início.
    guard = re.search(r'if\s*\(\s*sbn\s*==\s*null\s*\|\|\s*!WHATSAPP_BUSINESS\.equals\(sbn\.getPackageName\(\)\)\s*\)\s*\{\s*return;\s*\}', text[method.end():], re.S)
    if guard:
        pos = method.end() + guard.end()
        text = text[:pos] + '\n\n        ' + cache_call + text[pos:]
    else:
        text = text[:method.end()] + '\n        ' + cache_call + text[method.end():]

# Quando a mensagem veio da acessibilidade, ActiveChatNotification cria uma ação
# sintética que depende de chat aberto. Para privado, substituímos por uma ação
# real cacheada do WhatsApp Business quando disponível.
needle = re.compile(r'(Notification\s+notification\s*=\s*ActiveChatNotification\.create\s*\([^;]+;)', re.S)
m = needle.search(text)
if not m:
    raise SystemExit('ERRO background_private_reply: criação de ActiveChatNotification não encontrada')
inject = m.group(1) + '''\n\n        if (!isGroup) {\n            android.app.Notification.Action mrRealReplyAction =\n                    mrFindBusinessReplyAction(conversation);\n            if (mrRealReplyAction != null) {\n                notification.actions = new android.app.Notification.Action[]{mrRealReplyAction};\n                getSharedPreferences("master_responde", MODE_PRIVATE)\n                        .edit()\n                        .putString("accessibility_last_status", "Privado: resposta em background disponível")\n                        .apply();\n            }\n        }'''
if 'mrFindBusinessReplyAction(conversation)' not in text:
    text = text[:m.start()] + inject + text[m.end():]

helper = r'''
    private void mrCacheBusinessReplyAction(android.service.notification.StatusBarNotification sbn) {
        try {
            if (sbn == null || !WHATSAPP_BUSINESS.equals(sbn.getPackageName())) return;
            android.app.Notification n = sbn.getNotification();
            if (n == null || n.actions == null) return;

            android.os.Bundle extras = n.extras;
            String title = "";
            if (extras != null) {
                CharSequence c = extras.getCharSequence(android.app.Notification.EXTRA_CONVERSATION_TITLE);
                if (c == null || c.toString().trim().isEmpty()) {
                    c = extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                }
                if (c != null) title = normalize(c.toString());
            }
            if (title.isEmpty()) return;

            for (android.app.Notification.Action a : n.actions) {
                if (a == null || a.actionIntent == null) continue;
                android.app.RemoteInput[] inputs = a.getRemoteInputs();
                if (inputs == null || inputs.length == 0) continue;
                MR_BUSINESS_REPLY_ACTIONS.put(title, a);
                MR_BUSINESS_REPLY_ACTION_TIMES.put(title, System.currentTimeMillis());
                break;
            }
        } catch (Throwable ignored) {}
    }

    private android.app.Notification.Action mrFindBusinessReplyAction(String conversation) {
        try {
            String key = normalize(conversation == null ? "" : conversation);
            if (key.isEmpty()) return null;
            Long at = MR_BUSINESS_REPLY_ACTION_TIMES.get(key);
            if (at == null || System.currentTimeMillis() - at > MR_REPLY_ACTION_TTL_MS) {
                MR_BUSINESS_REPLY_ACTIONS.remove(key);
                MR_BUSINESS_REPLY_ACTION_TIMES.remove(key);
                return null;
            }
            return MR_BUSINESS_REPLY_ACTIONS.get(key);
        } catch (Throwable ignored) {
            return null;
        }
    }
'''
if 'private void mrCacheBusinessReplyAction(' not in text:
    pos = text.rfind('\n}')
    if pos < 0:
        raise SystemExit('ERRO background_private_reply: fim da classe não encontrado')
    text = text[:pos] + '\n' + helper + text[pos:]

path.write_text(text, encoding='utf-8')

final = path.read_text(encoding='utf-8')
for marker in ['MR_BUSINESS_REPLY_ACTIONS', 'mrCacheBusinessReplyAction(sbn)', 'mrFindBusinessReplyAction(conversation)', 'Privado: resposta em background disponível']:
    if marker not in final:
        raise SystemExit('ERRO background_private_reply: validação ausente ' + marker)

if 'WHATSAPP_BUSINESS.equals(sbn.getPackageName())' not in final:
    raise SystemExit('ERRO background_private_reply: trava Business-only ausente')

print('Resposta privada em background habilitada usando RemoteInput real do WhatsApp Business, com fallback por acessibilidade')
