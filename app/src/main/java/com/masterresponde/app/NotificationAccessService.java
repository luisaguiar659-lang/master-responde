package com.masterresponde.app;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationAccessService extends NotificationListenerService {

    private static final String WHATSAPP_BUSINESS = "com.whatsapp.w4b";

    private static final Map<String, Long> RECENT = new ConcurrentHashMap<>();
    private static final Map<String, Long> PRIVATE_RECENT = new ConcurrentHashMap<>();
    private static final Map<String, Long> GROUP_MENU_OPEN = new ConcurrentHashMap<>();
    private static final Map<String, Long> GROUP_RECENT = new ConcurrentHashMap<>();
    private static final Object SEND_LOCK = new Object();

    // Mensagens reais do MessagingStyle carregam timestamp estável. O WhatsApp
    // pode republicá-las quando a notificação é atualizada (por exemplo, após
    // o envio de um teste). Mantemos esses eventos processados por mais tempo,
    // sempre isolados por conversa, para não disparar regras antigas novamente.
    private static final long PRIVATE_STABLE_EVENT_TTL_MS = 10L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());



    @Override
    public void onCreate() {
        super.onCreate();

        ensurePromotionsCategory();

        BackgroundRuntime.markListenerConnected(
                this,
                false
        );
    }

    private void ensurePromotionsCategory() {
        SharedPreferences prefs = getSharedPreferences(
                "master_responde",
                MODE_PRIVATE
        );

        try {
            JSONArray categories = new JSONArray(
                    prefs.getString("text_categories", "[]")
            );

            JSONObject target = null;

            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.optJSONObject(i);
                if (category == null) continue;

                if ("promocoes e planos".equals(
                        normalize(category.optString("name", ""))
                )) {
                    target = category;
                    break;
                }
            }

            if (target != null) {
                JSONArray existing = cleanResponses(
                        target.optJSONArray("responses")
                );
                if (existing.length() > 0) {
                    return;
                }
            }

            if (target == null) {
                target = new JSONObject();
                target.put("id", "default-promotions-plans");
                target.put("name", "Promoções e Planos");
                target.put("responses", new JSONArray());
                categories.put(target);
            }

            JSONArray responses = new JSONArray();
            JSONObject response = new JSONObject();
            response.put("id", "default-promotions-plans-text");
            response.put("type", "text");
            response.put("text",
                    "🔥 *PLANOS & PROMOÇÕES MASTER* 🔥\n\n" +
                    "Tenha suas próprias ferramentas para revenda, com *personalização* e mensalidade fixa. 🚀\n\n" +
                    "☁️ *XCLOUD ILIMITADO — PERSONALIZÁVEL*\n" +
                    "🔥 *1º mês: R$ 20,00*\n" +
                    "🔄 Renovação: *R$ 25,00/mês*\n\n" +
                    "📺 *IBO REVENDA ILIMITADO — PERSONALIZÁVEL*\n" +
                    "🔥 *1º mês: R$ 20,00*\n" +
                    "🔄 Renovação: *R$ 25,00/mês*\n\n" +
                    "💎 *COMBO XCLOUD + IBO REVENDA*\n" +
                    "♾️ Ambos ilimitados e personalizáveis\n" +
                    "🔥 *1º mês: R$ 35,00*\n" +
                    "🔄 Renovação: *R$ 40,00/mês*\n\n" +
                    "✅ Sem surpresa nas próximas mensalidades.\n" +
                    "🔒 *O valor da renovação é fixo.*\n\n" +
                    "━━━━━━━━━━━━━━\n" +
                    "🤖 Digite *@infor* para voltar ao menu principal."
            );
            responses.put(response);
            target.put("responses", responses);

            prefs.edit()
                    .putString("text_categories", categories.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        BackgroundRuntime.markListenerConnected(
                this,
                true
        );

        handler.postDelayed(
                this::recoverPendingAutomations,
                900L
        );

        handler.postDelayed(
                this::recoverPendingAutomations,
                15000L
        );

        handler.postDelayed(
                this::recoverPendingAutomations,
                45000L
        );
    }

    @Override
    public void onListenerDisconnected() {
        BackgroundRuntime.markListenerConnected(
                this,
                false
        );

        SharedPreferences prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        if (prefs.getBoolean(
                "auto_reconnect_engine",
                true
        )) {
            handler.postDelayed(
                    () -> BackgroundRuntime.requestListenerRebind(
                            this
                    ),
                    1500L
            );
        }

        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        BackgroundRuntime.markListenerConnected(
                this,
                false
        );

        handler.removeCallbacksAndMessages(
                null
        );

        SharedPreferences prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        if (prefs.getBoolean(
                "auto_reconnect_engine",
                true
        )
                && BackgroundRuntime.hasNotificationAccess(
                this
        )) {
            BackgroundRuntime.requestListenerRebind(
                    this
            );
        }

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (!WHATSAPP_BUSINESS.equals(sbn.getPackageName())) return;

        BackgroundRuntime.markEvent(
                this
        );

        SharedPreferences prefs =
                getSharedPreferences("master_responde", MODE_PRIVATE);

        if (!prefs.getBoolean("bot_enabled", false)) return;

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        String notificationKey =
                sbn.getKey() == null ? "no-key" : sbn.getKey();

        boolean isGroup =
                isGroupNotification(notification);

        if (!isGroup
                && recoverPendingForCurrentNotification(
                notification,
                prefs
        )) {
            return;
        }

        if (isGroup) {
            handleGroupNotification(
                    sbn,
                    notification,
                    notificationKey,
                    prefs
            );
            return;
        }

        List<PrivateMessage> privateMessages =
                extractRecentPrivateMessages(
                        notification,
                        sbn.getPostTime()
                );

        if (!privateMessages.isEmpty()) {
            for (PrivateMessage message : privateMessages) {
                // No MessagingStyle, remetente vazio normalmente representa
                // mensagem enviada pelo próprio usuário/aparelho. Durante uma
                // janela curta após o bot responder, ignore essas saídas sem
                // bloquear mensagens novas do contato.
                if (message.senderKey.isEmpty()
                        && WhatsAppReply.hasRecentOutgoing(
                        this,
                        notification
                )) {
                    continue;
                }

                handlePrivate(
                        notification,
                        notificationKey,
                        message.text,
                        message.eventId,
                        prefs
                );
            }

            return;
        }

        CharSequence value =
                notification.extras.getCharSequence(
                        Notification.EXTRA_TEXT
                );

        if (value == null) return;

        String incoming =
                normalize(value.toString());

        if (incoming.isEmpty()) return;

        handlePrivate(
                notification,
                notificationKey,
                incoming,
                "fallback|" + sbn.getPostTime(),
                prefs
        );
    }

    private List<PrivateMessage> extractRecentPrivateMessages(
            Notification notification,
            long postTime
    ) {
        List<PrivateMessage> result =
                new ArrayList<>();

        if (notification == null
                || notification.extras == null) {
            return result;
        }

        Parcelable[] rawMessages =
                notification.extras.getParcelableArray(
                        Notification.EXTRA_MESSAGES
                );

        if (rawMessages == null
                || rawMessages.length == 0) {
            return result;
        }

        for (int i = 0;
             i < rawMessages.length;
             i++) {

            Parcelable raw =
                    rawMessages[i];

            if (!(raw instanceof Bundle)) {
                continue;
            }

            Bundle bundle =
                    (Bundle) raw;

            CharSequence textValue =
                    bundle.getCharSequence(
                            "text"
                    );

            if (textValue == null) {
                continue;
            }

            String text =
                    normalize(
                            textValue.toString()
                    );

            if (text.isEmpty()) {
                continue;
            }

            long messageTime =
                    bundle.getLong(
                            "time",
                            0L
                    );

            if (messageTime > 0L
                    && postTime > 0L
                    && postTime - messageTime > 30000L) {
                continue;
            }

            String eventId =
                    messageTime > 0L
                            ? "msg|" + messageTime
                            : "idx|" + i + "|" + postTime;

            String senderKey =
                    extractMessageSenderKey(
                            bundle
                    );

            result.add(
                    new PrivateMessage(
                            text,
                            eventId,
                            senderKey
                    )
            );
        }

        return result;
    }

    private static class PrivateMessage {
        final String text;
        final String eventId;
        final String senderKey;

        PrivateMessage(
                String text,
                String eventId,
                String senderKey
        ) {
            this.text =
                    text;

            this.eventId =
                    eventId;

            this.senderKey =
                    senderKey == null
                            ? ""
                            : senderKey;
        }
    }

    private void handleGroupNotification(
            StatusBarNotification sbn,
            Notification notification,
            String notificationKey,
            SharedPreferences prefs
    ) {
        List<GroupMessage> messages =
                extractRecentGroupMessages(
                        notification,
                        sbn.getPostTime()
                );

        if (!messages.isEmpty()) {
            for (GroupMessage message : messages) {
                handleGroup(
                        notification,
                        notificationKey,
                        message.text,
                        message.eventId,
                        message.senderKey,
                        prefs
                );
            }
            return;
        }

        // Fallback para aparelhos onde EXTRA_MESSAGES não é fornecido.
        CharSequence value =
                notification.extras.getCharSequence(
                        Notification.EXTRA_TEXT
                );

        if (value == null) return;

        String incoming =
                normalize(value.toString());

        if (incoming.isEmpty()) return;

        handleGroup(
                notification,
                notificationKey,
                incoming,
                "fallback|" + sbn.getPostTime(),
                "",
                prefs
        );
    }

    private List<GroupMessage> extractRecentGroupMessages(
            Notification notification,
            long postTime
    ) {
        List<GroupMessage> result =
                new ArrayList<>();

        if (notification == null
                || notification.extras == null) {
            return result;
        }

        Parcelable[] rawMessages =
                notification.extras.getParcelableArray(
                        Notification.EXTRA_MESSAGES
                );

        if (rawMessages == null
                || rawMessages.length == 0) {
            return result;
        }

        for (int i = 0; i < rawMessages.length; i++) {
            Parcelable raw =
                    rawMessages[i];

            if (!(raw instanceof Bundle)) {
                continue;
            }

            Bundle bundle =
                    (Bundle) raw;

            CharSequence textValue =
                    bundle.getCharSequence("text");

            if (textValue == null) {
                continue;
            }

            String text =
                    normalize(
                            textValue.toString()
                    );

            if (text.isEmpty()) {
                continue;
            }

            long messageTime =
                    bundle.getLong(
                            "time",
                            0L
                    );

            // O WhatsApp costuma devolver também mensagens antigas
            // no histórico da notificação. Só olhamos as recentes.
            if (messageTime > 0L
                    && postTime > 0L
                    && postTime - messageTime > 30000L) {
                continue;
            }

            String senderKey =
                    extractMessageSenderKey(
                            bundle
                    );

            String eventId;

            if (messageTime > 0L) {
                eventId =
                        "msg|"
                                + messageTime;
            } else {
                // Fallback: posição + postTime da atualização atual.
                eventId =
                        "idx|"
                                + i
                                + "|"
                                + postTime;
            }

            result.add(
                    new GroupMessage(
                            text,
                            eventId,
                            senderKey
                    )
            );
        }

        return result;
    }

    private static class GroupMessage {
        final String text;
        final String eventId;
        final String senderKey;

        GroupMessage(
                String text,
                String eventId,
                String senderKey
        ) {
            this.text = text;
            this.eventId = eventId;
            this.senderKey =
                    senderKey == null
                            ? ""
                            : senderKey;
        }
    }

    private String extractMessageSenderKey(
            Bundle bundle
    ) {
        if (bundle == null) {
            return "";
        }

        // MessagingStyle usa "sender" nas versões antigas e
        // "sender_person" nas versões novas. Não persistimos o nome:
        // devolvemos apenas um hash curto para separar participantes.
        String raw = "";

        CharSequence sender =
                bundle.getCharSequence(
                        "sender"
                );

        if (sender != null
                && sender.length() > 0) {
            raw =
                    normalize(
                            sender.toString()
                    );
        }

        if (raw.isEmpty()
                && android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                Parcelable senderPerson =
                        bundle.getParcelable(
                                "sender_person"
                        );

                android.app.Person person =
                        senderPerson instanceof android.app.Person
                                ? (android.app.Person) senderPerson
                                : null;

                if (person != null
                        && person.getKey() != null
                        && !person.getKey().trim().isEmpty()) {
                    raw =
                            "key|"
                                    + person.getKey().trim();

                } else if (person != null
                        && person.getName() != null
                        && person.getName().length() > 0) {
                    raw =
                            normalize(
                                    person.getName().toString()
                            );
                }
            } catch (Throwable ignored) {
            }
        }

        if (raw.isEmpty()) {
            // Em MessagingStyle, sender ausente normalmente representa
            // uma mensagem enviada pelo próprio usuário do aparelho.
            return "";
        }

        return Integer.toHexString(
                raw.hashCode()
        );
    }

    private boolean isGroupNotification(Notification notification) {
        if (notification == null || notification.extras == null) {
            return false;
        }

        // Indicador oficial quando o WhatsApp o fornece.
        if (notification.extras.getBoolean(
                Notification.EXTRA_IS_GROUP_CONVERSATION,
                false
        )) {
            return true;
        }

        // Em alguns aparelhos/versões do WhatsApp Business,
        // a PRIMEIRA mensagem do grupo ainda não vem com o boolean acima.
        // O título da conversa, porém, já vem preenchido.
        CharSequence conversationTitle =
                notification.extras.getCharSequence(
                        Notification.EXTRA_CONVERSATION_TITLE
                );

        return conversationTitle != null
                && conversationTitle.toString().trim().length() > 0;
    }

    private void handleGroup(
            Notification notification,
            String notificationKey,
            String incoming,
            String eventId,
            String senderKey,
            SharedPreferences prefs
    ) {
        // Em grupos, só aplica o anti-eco quando a mensagem não possui
        // remetente (padrão usado pelo MessagingStyle para saída própria).
        // Assim, outra pessoa do mesmo grupo pode enviar texto idêntico
        // à resposta do bot sem ser bloqueada.
        if ((senderKey == null || senderKey.isEmpty())
                && WhatsAppReply.isRecentlySent(
                this,
                notification,
                incoming
        )) {
            return;
        }

        String groupKey = groupConversationKey(notification);

        // Proteção lógica específica para grupos.
        // O WhatsApp pode disparar duas callbacks da mesma mensagem
        // com notificationKey diferente.
        // v1.5.5: deduplicação ATÔMICA.
        // Duas callbacks podem chegar praticamente juntas.
        // Agora a checagem e o registro acontecem dentro da mesma trava.
        if (isDuplicateGroupMessage(
                notification,
                incoming,
                eventId,
                senderKey,
                prefs
        )) {
            return;
        }

        String groupTrigger =
                MessageSettings.normalizedSingleTrigger(
                        prefs,
                        MessageSettings.KEY_GROUP_TRIGGER,
                        MessageSettings.DEFAULT_GROUP_TRIGGER
                );

        if (groupTrigger.equals(incoming)) {
            setGroupMenuOpen(
                    prefs,
                    groupKey,
                    System.currentTimeMillis()
            );

            sendConfiguredWithProtection(
                    notification,
                    notificationKey,
                    "group_menu|" + groupKey,
                    incoming,
                    prefs,
                    MessageSettings.MSG_GROUP_MENU,
                    MessageSettings.DEFAULT_GROUP_MENU,
                    MessageSettings.variables(
                            "COMANDO_MENU",
                            groupTrigger,
                            "CATEGORIAS",
                            buildGroupCategoryMenu(
                                    prefs
                            )
                    )
            );
            return;
        }

        // Permite executar direto no mesmo comando:
        // comando 1 / comando planos / comando tutoriais...
        String directPrefix =
                groupTrigger + " ";

        if (incoming.startsWith(directPrefix)) {
            String command =
                    incoming.substring(
                            directPrefix.length()
                    ).trim();

            executeGroupCommand(
                    notification,
                    notificationKey,
                    command,
                    prefs
            );

            setGroupMenuOpen(
                    prefs,
                    groupKey,
                    System.currentTimeMillis()
            );
            return;
        }

        // Mantém o menu aberto para várias escolhas seguidas sem novo @infor.
        Long openedAt =
                getGroupMenuOpen(
                        prefs,
                        groupKey
                );

        if (openedAt == null
                || openedAt <= 0L) {
            return;
        }

        long groupMenuTimeout =
                MessageSettings.getMilliseconds(
                        prefs,
                        MessageSettings.KEY_GROUP_MENU_SECONDS,
                        MessageSettings.DEFAULT_GROUP_MENU_SECONDS
                );

        if (System.currentTimeMillis() - openedAt > groupMenuTimeout) {
            clearGroupMenuOpen(
                    prefs,
                    groupKey
            );
            return;
        }

        if (isRecognizedGroupCommand(
                incoming,
                prefs
        )) {
            executeGroupCommand(
                    notification,
                    notificationKey,
                    incoming,
                    prefs
            );

            setGroupMenuOpen(
                    prefs,
                    groupKey,
                    System.currentTimeMillis()
            );
        }
    }

    private void setGroupMenuOpen(
            SharedPreferences prefs,
            String groupKey,
            long time
    ) {
        GROUP_MENU_OPEN.put(
                groupKey,
                time
        );

        prefs.edit()
                .putLong(
                        groupMenuPreferenceKey(
                                groupKey
                        ),
                        time
                )
                .apply();
    }

    private Long getGroupMenuOpen(
            SharedPreferences prefs,
            String groupKey
    ) {
        Long memory =
                GROUP_MENU_OPEN.get(
                        groupKey
                );

        long persisted =
                prefs.getLong(
                        groupMenuPreferenceKey(
                                groupKey
                        ),
                        0L
                );

        if (memory != null
                && memory > persisted) {
            return memory;
        }

        return persisted > 0L
                ? persisted
                : null;
    }

    private void clearGroupMenuOpen(
            SharedPreferences prefs,
            String groupKey
    ) {
        GROUP_MENU_OPEN.remove(
                groupKey
        );

        prefs.edit()
                .remove(
                        groupMenuPreferenceKey(
                                groupKey
                        )
                )
                .apply();
    }

    private String groupMenuPreferenceKey(
            String groupKey
    ) {
        return "group_menu_open_"
                + Integer.toHexString(
                groupKey == null
                        ? 0
                        : groupKey.hashCode()
        );
    }

    private boolean isRecognizedGroupCommand(
            String command,
            SharedPreferences prefs
    ) {
        String c =
                normalize(
                        command
                );

        return resolveDynamicGroupCategory(
                c,
                prefs
        ) != null;
    }

    private boolean isDuplicateGroupMessage(
            Notification notification,
            String incoming,
            String eventId,
            String senderKey,
            SharedPreferences prefs
    ) {
        String stableGroupKey =
                stableGroupKey(
                        notification
                );

        String participant =
                senderKey == null
                        ? ""
                        : senderKey;

        String fingerprint =
                stableGroupKey
                        + "|"
                        + participant
                        + "|"
                        + eventId
                        + "|"
                        + incoming;

        synchronized (SEND_LOCK) {
            long now =
                    System.currentTimeMillis();

            long duplicateWindow =
                    MessageSettings.getMilliseconds(
                            prefs,
                            MessageSettings.KEY_GROUP_DUPLICATE_SECONDS,
                            MessageSettings.DEFAULT_GROUP_DUPLICATE_SECONDS
                    );

            Long memoryTime =
                    GROUP_RECENT.get(
                            fingerprint
                    );

            if (memoryTime != null
                    && now - memoryTime < duplicateWindow) {
                return true;
            }

            // v1.16: o WhatsApp pode criar eventos diferentes para o mesmo
            // comando repetido rapidamente. Além do eventId, seguramos o
            // conteúdo por grupo durante a janela configurada.
            String contentKey =
                    "group_content_"
                            + Integer.toHexString(
                            (stableGroupKey + "|" + participant + "|" + incoming).hashCode()
                    );

            long contentTime =
                    prefs.getLong(
                            contentKey,
                            0L
                    );

            if (contentTime > 0L
                    && now - contentTime < duplicateWindow) {
                return true;
            }

            String last =
                    prefs.getString(
                            "last_group_fingerprint",
                            ""
                    );

            long lastTime =
                    prefs.getLong(
                            "last_group_fingerprint_time",
                            0L
                    );

            if (fingerprint.equals(last)
                    && now - lastTime < duplicateWindow) {

                GROUP_RECENT.put(
                        fingerprint,
                        now
                );

                return true;
            }

            // Reserva antes de responder.
            GROUP_RECENT.put(
                    fingerprint,
                    now
            );

            prefs.edit()
                    .putString(
                            "last_group_fingerprint",
                            fingerprint
                    )
                    .putLong(
                            "last_group_fingerprint_time",
                            now
                    )
                    .putLong(
                            "group_content_"
                                    + Integer.toHexString(
                                    (stableGroupKey + "|" + participant + "|" + incoming).hashCode()
                            ),
                            now
                    )
                    .apply();

            if (GROUP_RECENT.size() > 300) {
                GROUP_RECENT.clear();

                GROUP_RECENT.put(
                        fingerprint,
                        now
                );
            }

            return false;
        }
    }

    private String stableGroupKey(
            Notification notification
    ) {
        if (notification == null) {
            return "grupo";
        }

        String shortcutId =
                notification.getShortcutId();

        if (shortcutId != null
                && !shortcutId.trim().isEmpty()) {
            return "shortcut|"
                    + shortcutId.trim();
        }

        if (notification.extras != null) {
            CharSequence conversation =
                    notification.extras.getCharSequence(
                            Notification.EXTRA_CONVERSATION_TITLE
                    );

            if (conversation != null
                    && conversation.length() > 0) {
                return "title|"
                        + normalize(
                                conversation.toString()
                        );
            }
        }

        return "grupo";
    }

    private void executeGroupCommand(
            Notification notification,
            String notificationKey,
            String command,
            SharedPreferences prefs
    ) {
        String c =
                normalize(
                        command
                );

        JSONObject category =
                resolveDynamicGroupCategory(
                        c,
                        prefs
                );

        if (category != null) {
            String categoryId =
                    category.optString(
                            "id",
                            ""
                    );

            String categoryName =
                    category.optString(
                            "name",
                            "Categoria"
                    );

            JSONArray responses =
                    cleanResponses(
                            category.optJSONArray(
                                    "responses"
                            )
                    );

            if (responses.length() == 0) {
                sendConfiguredWithProtection(
                        notification,
                        notificationKey,
                        "group_missing|" + categoryId,
                        c,
                        prefs,
                        MessageSettings.MSG_GROUP_CATEGORY_MISSING,
                        MessageSettings.DEFAULT_GROUP_CATEGORY_MISSING,
                        MessageSettings.variables(
                                "CATEGORIA",
                                categoryName
                        )
                );
            } else {
                sendSequenceWithProtection(
                        notification,
                        notificationKey,
                        "group_category|" + categoryId,
                        c,
                        responses
                );
            }

            return;
        }

        String groupTrigger =
                MessageSettings.normalizedSingleTrigger(
                        prefs,
                        MessageSettings.KEY_GROUP_TRIGGER,
                        MessageSettings.DEFAULT_GROUP_TRIGGER
                );

        sendConfiguredWithProtection(
                notification,
                notificationKey,
                "group_unknown",
                c,
                prefs,
                MessageSettings.MSG_GROUP_UNKNOWN,
                MessageSettings.DEFAULT_GROUP_UNKNOWN,
                MessageSettings.variables(
                        "COMANDO_MENU",
                        groupTrigger
                )
        );
    }

    private JSONObject resolveDynamicGroupCategory(
            String command,
            SharedPreferences prefs
    ) {
        String c =
                normalize(
                        command
                );

        JSONArray categories =
                configuredGroupCategories(
                        prefs
                );

        if (categories.length() == 0) {
            return null;
        }

        int numeric =
                parsePositiveInt(
                        c
                );

        if (numeric >= 1
                && numeric <= categories.length()) {
            return categories.optJSONObject(
                    numeric - 1
            );
        }

        // Mantém compatibilidade com os gatilhos antigos 1..8:
        // eles passam a apontar para a categoria existente na mesma posição.
        for (int i = 1;
             i <= Math.min(
                     8,
                     categories.length()
             );
             i++) {

            String key =
                    groupOptionKey(
                            i
                    );

            String defaults =
                    groupOptionDefault(
                            i
                    );

            if (key != null
                    && defaults != null
                    && MessageSettings.matchesAnyTrigger(
                    prefs,
                    key,
                    defaults,
                    c
            )) {
                return categories.optJSONObject(
                        i - 1
                );
            }
        }

        // Também aceita o nome real da categoria:
        // @infor banners / tutoriais / materiais etc.
        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.optJSONObject(
                            i
                    );

            if (category == null) {
                continue;
            }

            String name =
                    normalize(
                            category.optString(
                                    "name",
                                    ""
                            )
                    );

            if (!name.isEmpty()
                    && name.equals(c)) {
                return category;
            }
        }

        return null;
    }

    private JSONArray configuredGroupCategories(
            SharedPreferences prefs
    ) {
        JSONArray result =
                new JSONArray();

        try {
            JSONArray categories =
                    new JSONArray(
                            prefs.getString(
                                    "text_categories",
                                    "[]"
                            )
                    );

            for (int i = 0;
                 i < categories.length();
                 i++) {

                JSONObject category =
                        categories.optJSONObject(
                                i
                        );

                if (category == null) {
                    continue;
                }

                String name =
                        category.optString(
                                "name",
                                ""
                        ).trim();

                if (name.isEmpty()) {
                    continue;
                }

                String normalizedName = normalize(name);
                boolean allowed =
                        "tutoriais".equals(normalizedName)
                        || "promocoes e planos".equals(normalizedName)
                        || "aplicativos parceiros".equals(normalizedName)
                        || "banners".equals(normalizedName);

                if (!allowed) {
                    continue;
                }

                // Só entra no menu se houver ao menos texto/link enviável.
                JSONArray responses =
                        cleanResponses(
                                category.optJSONArray(
                                        "responses"
                                )
                        );

                if (responses.length() == 0) {
                    continue;
                }

                result.put(
                        category
                );
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    private String buildGroupCategoryMenu(
            SharedPreferences prefs
    ) {
        JSONArray categories =
                configuredGroupCategories(
                        prefs
                );

        if (categories.length() == 0) {
            return "Nenhuma categoria com texto/link foi configurada.";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.optJSONObject(
                            i
                    );

            if (category == null) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(
                        "\n"
                );
            }

            builder.append(
                    i + 1
            );

            builder.append(
                    "️⃣ "
            );

            builder.append(
                    category.optString(
                            "name",
                            "Categoria"
                    )
            );
        }

        return builder.toString();
    }

    private int parsePositiveInt(
            String value
    ) {
        try {
            if (value == null
                    || !value.matches("\\d+")) {
                return -1;
            }

            return Integer.parseInt(
                    value
            );

        } catch (Exception e) {
            return -1;
        }
    }

    private String groupOptionKey(
            int option
    ) {
        switch (option) {
            case 1:
                return MessageSettings.KEY_GROUP_OPTION_1;
            case 2:
                return MessageSettings.KEY_GROUP_OPTION_2;
            case 3:
                return MessageSettings.KEY_GROUP_OPTION_3;
            case 4:
                return MessageSettings.KEY_GROUP_OPTION_4;
            case 5:
                return MessageSettings.KEY_GROUP_OPTION_5;
            case 6:
                return MessageSettings.KEY_GROUP_OPTION_6;
            case 7:
                return MessageSettings.KEY_GROUP_OPTION_7;
            case 8:
                return MessageSettings.KEY_GROUP_OPTION_8;
            default:
                return null;
        }
    }

    private String groupOptionDefault(
            int option
    ) {
        switch (option) {
            case 1:
                return MessageSettings.DEFAULT_GROUP_OPTION_1;
            case 2:
                return MessageSettings.DEFAULT_GROUP_OPTION_2;
            case 3:
                return MessageSettings.DEFAULT_GROUP_OPTION_3;
            case 4:
                return MessageSettings.DEFAULT_GROUP_OPTION_4;
            case 5:
                return MessageSettings.DEFAULT_GROUP_OPTION_5;
            case 6:
                return MessageSettings.DEFAULT_GROUP_OPTION_6;
            case 7:
                return MessageSettings.DEFAULT_GROUP_OPTION_7;
            case 8:
                return MessageSettings.DEFAULT_GROUP_OPTION_8;
            default:
                return null;
        }
    }

    private void handlePrivate(
            Notification notification,
            String notificationKey,
            String incoming,
            String eventId,
            SharedPreferences prefs
    ) {
        // O WhatsApp Business pode republicar, em EXTRA_MESSAGES, a resposta
        // enviada pelo próprio bot via RemoteInput. Sem este filtro, a saída
        // pode voltar como entrada e disparar menus/regras novamente.
        if (WhatsAppReply.isRecentlySent(
                this,
                notification,
                incoming
        )) {
            return;
        }

        if (isDuplicatePrivateMessage(
                notification,
                notificationKey,
                incoming,
                eventId,
                prefs
        )) {
            return;
        }

        if (MessageSettings.matchesAnyTrigger(
                prefs,
                MessageSettings.KEY_DIAGNOSTIC_TRIGGER,
                MessageSettings.DEFAULT_DIAGNOSTIC_TRIGGER,
                incoming
        )) {
            sendConfiguredWithProtection(
                    notification,
                    notificationKey,
                    "fixed_test",
                    incoming,
                    prefs,
                    MessageSettings.MSG_DIAGNOSTIC,
                    MessageSettings.DEFAULT_DIAGNOSTIC,
                    null
            );
            return;
        }

        String resellerContactKey =
                privateContactKey(
                        notification
                );

        if (handlePendingResellerFlow(
                notification,
                resellerContactKey,
                incoming,
                prefs
        )) {
            return;
        }

        if (isNewResellerRequest(incoming)) {
            beginNewResellerFlow(
                    notification,
                    resellerContactKey,
                    prefs
            );
            return;
        }

        if (isTestRequest(incoming)) {
            startMasterflixTest(
                    notification,
                    notificationKey,
                    incoming,
                    prefs
            );
            return;
        }

        try {
            JSONArray rules = new JSONArray(
                    prefs.getString("text_rules", "[]")
            );

            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.optJSONObject(i);
                if (rule == null) continue;

                String trigger = normalize(
                        rule.optString("trigger", "")
                );

                boolean exact = rule.optBoolean("exact", false);
                if (trigger.isEmpty()) continue;

                boolean match = exact
                        ? incoming.equals(trigger)
                        : incoming.contains(trigger);

                if (!match) continue;

                String ruleId = rule.optString("id", String.valueOf(i));
                String categoryId = rule.optString("category_id", "");

                // v1.13.1: categorias de MÍDIAS são exclusivas do menu @infor
                // em grupos. Regras privadas antigas com categoria ficam
                // ignoradas e não enviam nada.
                if (!categoryId.isEmpty()) {
                    continue;
                }

                String response = rule.optString("reply", "").trim();

                if (!response.isEmpty()) {
                    sendSingleWithProtection(
                            notification,
                            notificationKey,
                            "rule|" + ruleId,
                            incoming,
                            response
                    );
                }

                return;
            }

        } catch (Exception ignored) {
        }
    }

    private boolean isDuplicatePrivateMessage(
            Notification notification,
            String notificationKey,
            String incoming,
            String messageEventId,
            SharedPreferences prefs
    ) {
        if (incoming == null
                || incoming.trim().isEmpty()) {
            return true;
        }

        String contactKey =
                privateContactKey(
                        notification
                );

        String eventId =
                messageEventId == null
                        ? ""
                        : messageEventId.trim();

        if (eventId.isEmpty()) {
            long eventTime =
                    notification != null
                            ? notification.when
                            : 0L;

            if (eventTime > 0L) {
                eventId =
                        String.valueOf(
                                eventTime
                        );

            } else if (notificationKey != null
                    && !notificationKey.trim().isEmpty()) {
                eventId =
                        notificationKey.trim();

            } else {
                eventId =
                        "no-event";
            }
        }

        String eventFingerprint =
                contactKey
                        + "|"
                        + eventId
                        + "|"
                        + incoming;

        String contentFingerprint =
                contactKey
                        + "|"
                        + incoming;

        synchronized (SEND_LOCK) {
            long now =
                    System.currentTimeMillis();

            long duplicateWindow =
                    MessageSettings.getMilliseconds(
                            prefs,
                            MessageSettings.KEY_PRIVATE_DUPLICATE_SECONDS,
                            MessageSettings.DEFAULT_PRIVATE_DUPLICATE_SECONDS
                    );

            Long eventSeen =
                    PRIVATE_RECENT.get(
                            eventFingerprint
                    );

            long eventWindow =
                    eventId.startsWith("msg|")
                            ? Math.max(
                            duplicateWindow,
                            PRIVATE_STABLE_EVENT_TTL_MS
                    )
                            : duplicateWindow;

            if (eventSeen != null
                    && now - eventSeen < eventWindow) {
                return true;
            }

            // Segunda proteção para atualizações rápidas da mesma
            // notificação onde o WhatsApp troca key/when.
            String privateContentKey =
                    "last_private_content_fingerprint_"
                            + contactKey;

            String privateContentTimeKey =
                    "last_private_content_fingerprint_time_"
                            + contactKey;

            String lastContent =
                    prefs.getString(
                            privateContentKey,
                            ""
                    );

            long lastContentTime =
                    prefs.getLong(
                            privateContentTimeKey,
                            0L
                    );

            if (contentFingerprint.equals(
                    lastContent
            )
                    && now - lastContentTime < duplicateWindow) {

                PRIVATE_RECENT.put(
                        eventFingerprint,
                        now
                );

                return true;
            }

            // Reserva a mensagem antes de qualquer resposta.
            PRIVATE_RECENT.put(
                    eventFingerprint,
                    now
            );

            prefs.edit()
                    .putString(
                            privateContentKey,
                            contentFingerprint
                    )
                    .putLong(
                            privateContentTimeKey,
                            now
                    )
                    .apply();

            if (PRIVATE_RECENT.size() > 300) {
                PRIVATE_RECENT.clear();

                PRIVATE_RECENT.put(
                        eventFingerprint,
                        now
                );
            }

            return false;
        }
    }

    private boolean isNewResellerRequest(
            String incoming
    ) {
        SharedPreferences prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        return MessageSettings.matchesAnyTrigger(
                prefs,
                MessageSettings.KEY_RESELLER_TRIGGERS,
                MessageSettings.DEFAULT_RESELLER_TRIGGERS,
                incoming
        );
    }

    private void beginNewResellerFlow(
            Notification notification,
            String contactKey,
            SharedPreferences prefs
    ) {
        String state =
                prefs.getString(
                        "reseller_state_" + contactKey,
                        ""
                );

        if ("REVIEW_REQUIRED".equals(state)) {
            MessageSettings.send(
                    this,
                    notification,
                    prefs,
                    MessageSettings.MSG_RESELLER_REVIEW,
                    MessageSettings.DEFAULT_RESELLER_REVIEW
            );

            return;
        }

        if ("COMPLETE".equals(state)) {
            MessageSettings.send(
                    this,
                    notification,
                    prefs,
                    MessageSettings.MSG_RESELLER_ALREADY_LINKED,
                    MessageSettings.DEFAULT_RESELLER_ALREADY_LINKED
            );

            return;
        }

        String signupLink =
                prefs.getString(
                        "reseller_signup_link",
                        ResellersActivity.DEFAULT_SIGNUP_LINK
                );

        prefs.edit()
                .putString(
                        "reseller_state_" + contactKey,
                        "WAITING_EMAIL"
                )
                .putString(
                        "last_reseller_status",
                        "Revenda: aguardando e-mail do cadastro"
                )
                .apply();

        MessageSettings.send(
                this,
                notification,
                prefs,
                MessageSettings.MSG_RESELLER_SIGNUP,
                MessageSettings.DEFAULT_RESELLER_SIGNUP,
                MessageSettings.variables(
                        "LINK_CADASTRO",
                        signupLink
                )
        );
    }

    private boolean handlePendingResellerFlow(
            Notification notification,
            String contactKey,
            String incoming,
            SharedPreferences prefs
    ) {
        String state =
                prefs.getString(
                        "reseller_state_" + contactKey,
                        ""
                );

        if (state.isEmpty()
                || "COMPLETE".equals(state)
                || "ERROR".equals(state)
                || "REVIEW_REQUIRED".equals(state)) {
            return false;
        }

        if (MessageSettings.matchesAnyTrigger(
                prefs,
                MessageSettings.KEY_RESELLER_CANCEL_TRIGGER,
                MessageSettings.DEFAULT_RESELLER_CANCEL_TRIGGER,
                incoming
        )) {
            MasterflixResellerAutomation.cancelFind(
                    contactKey
            );

            BackgroundRuntime.clearResellerPending(
                    prefs
            );

            prefs.edit()
                    .remove(
                            "reseller_state_" + contactKey
                    )
                    .putString(
                            "last_reseller_status",
                            "Revenda: fluxo cancelado"
                    )
                    .apply();

            try {
                new SecureStore(this).remove(
                        "pending_reseller_email_" + contactKey
                );
            } catch (Exception ignored) {
            }

            MessageSettings.send(
                    this,
                    notification,
                    prefs,
                    MessageSettings.MSG_RESELLER_CANCELLED,
                    MessageSettings.DEFAULT_RESELLER_CANCELLED
            );

            return true;
        }

        if ("WAITING_EMAIL".equals(state)) {
            if (isNewResellerRequest(
                    incoming
            )) {
                return true;
            }

            String email =
                    incoming == null
                            ? ""
                            : incoming.trim().toLowerCase(Locale.ROOT);

            if (!isValidEmail(email)) {
                MessageSettings.send(
                        this,
                        notification,
                        prefs,
                        MessageSettings.MSG_RESELLER_INVALID_EMAIL,
                        MessageSettings.DEFAULT_RESELLER_INVALID_EMAIL
                );

                return true;
            }

            try {
                new SecureStore(this).put(
                        "pending_reseller_email_" + contactKey,
                        email
                );

            } catch (Exception e) {
                MessageSettings.send(
                        this,
                        notification,
                        prefs,
                        MessageSettings.MSG_RESELLER_SAVE_ERROR,
                        MessageSettings.DEFAULT_RESELLER_SAVE_ERROR
                );

                return true;
            }

            prefs.edit()
                    .putString(
                            "reseller_state_" + contactKey,
                            "SEARCHING"
                    )
                    .putString(
                            "last_reseller_status",
                            "Revenda: verificando e-mail cadastrado no painel Sigma"
                    )
                    .apply();

            MessageSettings.send(
                    this,
                    notification,
                    prefs,
                    MessageSettings.MSG_RESELLER_VERIFYING,
                    MessageSettings.DEFAULT_RESELLER_VERIFYING
            );

            BackgroundRuntime.markResellerPending(
                    prefs,
                    notification,
                    contactKey
            );

            boolean started =
                    MasterflixResellerAutomation.findReseller(
                            this,
                            notification,
                            prefs,
                            contactKey,
                            email
                    );

            if (!started) {
                BackgroundRuntime.clearResellerPending(
                        prefs
                );
                prefs.edit()
                        .putString(
                                "reseller_state_" + contactKey,
                                "WAITING_EMAIL"
                        )
                        .putString(
                                "last_reseller_status",
                                "Revenda: verificação ocupada"
                        )
                        .apply();

                MessageSettings.send(
                        this,
                        notification,
                        prefs,
                        MessageSettings.MSG_RESELLER_BUSY,
                        MessageSettings.DEFAULT_RESELLER_BUSY
                );
            }

            return true;
        }

        if ("SEARCHING".equals(state)
                || "ADDING".equals(state)) {
            // Não responde a republicações/atualizações da notificação
            // enquanto a automação já está trabalhando.
            return true;
        }

        return false;
    }

    private boolean isValidEmail(
            String value
    ) {
        return value != null
                && value.length() <= 254
                && android.util.Patterns.EMAIL_ADDRESS
                .matcher(value)
                .matches();
    }

    private String privateContactKey(
            Notification notification
    ) {
        String raw =
                "";

        if (notification != null) {
            String shortcut =
                    notification.getShortcutId();

            if (shortcut != null
                    && !shortcut.trim().isEmpty()) {
                raw =
                        "shortcut|"
                                + shortcut.trim();
            }

            if (raw.isEmpty()
                    && notification.extras != null) {
                CharSequence title =
                        notification.extras.getCharSequence(
                                Notification.EXTRA_TITLE
                        );

                if (title != null
                        && title.length() > 0) {
                    raw =
                            "title|"
                                    + normalize(
                                            title.toString()
                                    );
                }
            }
        }

        if (raw.isEmpty()) {
            raw =
                    "private";
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] bytes =
                    digest.digest(
                            raw.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder builder =
                    new StringBuilder();

            for (int i = 0;
                 i < 12 && i < bytes.length;
                 i++) {

                builder.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                bytes[i] & 0xff
                        )
                );
            }

            return builder.toString();

        } catch (Exception e) {
            return Integer.toHexString(
                    raw.hashCode()
            );
        }
    }

    private boolean isTestRequest(
            String incoming
    ) {
        SharedPreferences prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        return MessageSettings.matchesAnyTrigger(
                prefs,
                MessageSettings.KEY_TEST_TRIGGERS,
                MessageSettings.DEFAULT_TEST_TRIGGERS,
                incoming
        );
    }

    private void startMasterflixTest(
            Notification notification,
            String notificationKey,
            String incoming,
            SharedPreferences prefs
    ) {
        String contactKey =
                privateContactKey(
                        notification
                );

        String guardKey =
                "test_request_guard_" + contactKey;

        long now =
                System.currentTimeMillis();

        long lastRequest =
                prefs.getLong(
                        guardKey,
                        0L
                );

        if (lastRequest > 0L
                && now - lastRequest < 90000L) {
            return;
        }

        prefs.edit()
                .putLong(
                        guardKey,
                        now
                )
                .apply();

        String requestKey =
                notificationKey
                        + "|"
                        + incoming;

        boolean accepted =
                TestTaskBridge.begin(
                        notification,
                        requestKey
                );

        if (!accepted) {
            return;
        }

        BackgroundRuntime.markTestPending(
                prefs,
                notification,
                requestKey
        );

        boolean started =
                MasterflixBackgroundAutomation.start(
                        this,
                        notification,
                        prefs
                );

        if (!started) {
            TestTaskBridge.complete();

            BackgroundRuntime.clearTestPending(
                    prefs
            );

            prefs.edit()
                    .putString(
                            "last_test_status",
                            MasterflixAutomationGate.isBusy()
                                    ? "Teste: painel Sigma ocupado com outra automação"
                                    : "Teste: outra geração já está em andamento"
                    )
                    .apply();

            MessageSettings.send(
                    this,
                    notification,
                    prefs,
                    MessageSettings.MSG_TEST_FAIL,
                    MessageSettings.DEFAULT_TEST_FAIL
            );

            return;
        }

        boolean waitSent =
                MessageSettings.send(
                        this,
                        notification,
                        prefs,
                        MessageSettings.MSG_TEST_WAIT,
                        MessageSettings.DEFAULT_TEST_WAIT
                );

        prefs.edit()
                .putString(
                        "last_test_status",
                        waitSent
                                ? "Teste: iniciando no motor unificado"
                                : "Teste: iniciando no motor unificado sem mensagem de espera"
                )
                .apply();
    }

    private boolean recoverPendingForCurrentNotification(
            Notification notification,
            SharedPreferences prefs
    ) {
        if (notification == null
                || prefs == null
                || MasterflixAutomationGate.isBusy()
                || MasterflixBackgroundAutomation.isRunning()
                || MasterflixResellerAutomation.isRunning()) {
            return false;
        }

        expireStalePendingTasks(
                prefs
        );

        if (BackgroundRuntime.hasFreshResellerPending(
                prefs
        )) {
            String locator =
                    prefs.getString(
                            BackgroundRuntime.KEY_RESELLER_LOCATOR,
                            ""
                    );

            if (BackgroundRuntime.notificationMatchesLocator(
                    notification,
                    locator
            )) {
                String contactKey =
                        prefs.getString(
                                BackgroundRuntime.KEY_RESELLER_CONTACT,
                                ""
                        );

                String email =
                        "";

                try {
                    email =
                            new SecureStore(
                                    this
                            ).get(
                                    "pending_reseller_email_" + contactKey
                            );

                } catch (Exception ignored) {
                }

                if (email != null
                        && !email.trim().isEmpty()) {
                    boolean started =
                            MasterflixResellerAutomation.findReseller(
                                    this,
                                    notification,
                                    prefs,
                                    contactKey,
                                    email
                            );

                    if (started) {
                        prefs.edit()
                                .putString(
                                        "last_reseller_status",
                                        "Revenda: verificação retomada automaticamente"
                                )
                                .apply();

                        return true;
                    }
                }
            }
        }

        if (BackgroundRuntime.hasFreshTestPending(
                prefs
        )) {
            String locator =
                    prefs.getString(
                            BackgroundRuntime.KEY_TEST_LOCATOR,
                            ""
                    );

            if (BackgroundRuntime.notificationMatchesLocator(
                    notification,
                    locator
            )) {
                String requestKey =
                        prefs.getString(
                                BackgroundRuntime.KEY_TEST_REQUEST_KEY,
                                "recovered-test"
                        );

                if (!TestTaskBridge.hasPending()) {
                    TestTaskBridge.begin(
                            notification,
                            requestKey
                    );
                }

                boolean started =
                        MasterflixBackgroundAutomation.start(
                                this,
                                notification,
                                prefs
                        );

                if (started) {
                    prefs.edit()
                            .putString(
                                    "last_test_status",
                                    "Teste: geração retomada automaticamente"
                            )
                            .apply();

                    return true;
                }
            }
        }

        return false;
    }

    private void recoverPendingAutomations() {
        SharedPreferences prefs =
                getSharedPreferences(
                        "master_responde",
                        MODE_PRIVATE
                );

        if (!prefs.getBoolean(
                "bot_enabled",
                false
        )) {
            return;
        }

        expireStalePendingTasks(
                prefs
        );

        if (MasterflixAutomationGate.isBusy()
                || MasterflixBackgroundAutomation.isRunning()
                || MasterflixResellerAutomation.isRunning()) {
            return;
        }

        long resellerAt =
                prefs.getLong(
                        BackgroundRuntime.KEY_RESELLER_PENDING_AT,
                        0L
                );

        long testAt =
                prefs.getLong(
                        BackgroundRuntime.KEY_TEST_PENDING_AT,
                        0L
                );

        boolean resellerFirst =
                BackgroundRuntime.hasFreshResellerPending(
                        prefs
                )
                        && (!BackgroundRuntime.hasFreshTestPending(
                        prefs
                )
                        || resellerAt <= testAt);

        if (resellerFirst) {
            if (recoverPendingReseller(
                    prefs
            )) {
                return;
            }
        }

        if (BackgroundRuntime.hasFreshTestPending(
                prefs
        )) {
            if (recoverPendingTest(
                    prefs
            )) {
                return;
            }
        }

        if (!resellerFirst
                && BackgroundRuntime.hasFreshResellerPending(
                prefs
        )) {
            recoverPendingReseller(
                    prefs
            );
        }
    }

    private boolean recoverPendingTest(
            SharedPreferences prefs
    ) {
        String locator =
                prefs.getString(
                        BackgroundRuntime.KEY_TEST_LOCATOR,
                        ""
                );

        Notification notification =
                findActiveWhatsAppNotification(
                        locator
                );

        if (notification == null) {
            return false;
        }

        String requestKey =
                prefs.getString(
                        BackgroundRuntime.KEY_TEST_REQUEST_KEY,
                        "recovered-test"
                );

        if (!TestTaskBridge.hasPending()) {
            TestTaskBridge.begin(
                    notification,
                    requestKey
            );
        }

        boolean started =
                MasterflixBackgroundAutomation.start(
                        this,
                        notification,
                        prefs
                );

        if (started) {
            prefs.edit()
                    .putString(
                            "last_test_status",
                            "Teste: tarefa recuperada pelo motor em segundo plano"
                    )
                    .apply();
        }

        return started;
    }

    private boolean recoverPendingReseller(
            SharedPreferences prefs
    ) {
        String contactKey =
                prefs.getString(
                        BackgroundRuntime.KEY_RESELLER_CONTACT,
                        ""
                );

        if (contactKey.isEmpty()) {
            BackgroundRuntime.clearResellerPending(
                    prefs
            );

            return false;
        }

        String email =
                "";

        try {
            email =
                    new SecureStore(
                            this
                    ).get(
                            "pending_reseller_email_" + contactKey
                    );

        } catch (Exception ignored) {
        }

        if (email == null
                || email.trim().isEmpty()) {
            BackgroundRuntime.clearResellerPending(
                    prefs
            );

            prefs.edit()
                    .putString(
                            "reseller_state_" + contactKey,
                            "WAITING_EMAIL"
                    )
                    .putString(
                            "last_reseller_status",
                            "Revenda: tarefa interrompida; aguardando o e-mail novamente"
                    )
                    .apply();

            return false;
        }

        String locator =
                prefs.getString(
                        BackgroundRuntime.KEY_RESELLER_LOCATOR,
                        ""
                );

        Notification notification =
                findActiveWhatsAppNotification(
                        locator
                );

        if (notification == null) {
            return false;
        }

        boolean started =
                MasterflixResellerAutomation.findReseller(
                        this,
                        notification,
                        prefs,
                        contactKey,
                        email
                );

        if (started) {
            prefs.edit()
                    .putString(
                            "last_reseller_status",
                            "Revenda: verificação recuperada pelo motor em segundo plano"
                    )
                    .apply();
        }

        return started;
    }

    private Notification findActiveWhatsAppNotification(
            String locator
    ) {
        if (locator == null
                || locator.isEmpty()) {
            return null;
        }

        try {
            StatusBarNotification[] active =
                    getActiveNotifications();

            if (active == null) {
                return null;
            }

            for (StatusBarNotification sbn : active) {
                if (sbn == null
                        || !WHATSAPP_BUSINESS.equals(
                        sbn.getPackageName()
                )) {
                    continue;
                }

                Notification notification =
                        sbn.getNotification();

                if (BackgroundRuntime.notificationMatchesLocator(
                        notification,
                        locator
                )) {
                    return notification;
                }
            }

        } catch (Throwable ignored) {
        }

        return null;
    }

    private void expireStalePendingTasks(
            SharedPreferences prefs
    ) {
        if (prefs.getBoolean(
                BackgroundRuntime.KEY_TEST_PENDING,
                false
        )
                && !BackgroundRuntime.hasFreshTestPending(
                prefs
        )) {

            BackgroundRuntime.clearTestPending(
                    prefs
            );

            TestTaskBridge.complete();

            prefs.edit()
                    .putString(
                            "last_test_status",
                            "Teste: tarefa anterior expirou e foi liberada"
                    )
                    .apply();
        }

        if (prefs.getBoolean(
                BackgroundRuntime.KEY_RESELLER_PENDING,
                false
        )
                && !BackgroundRuntime.hasFreshResellerPending(
                prefs
        )) {

            String contactKey =
                    prefs.getString(
                            BackgroundRuntime.KEY_RESELLER_CONTACT,
                            ""
                    );

            BackgroundRuntime.clearResellerPending(
                    prefs
            );

            if (!contactKey.isEmpty()) {
                prefs.edit()
                        .putString(
                                "reseller_state_" + contactKey,
                                "WAITING_EMAIL"
                        )
                        .putString(
                                "last_reseller_status",
                                "Revenda: verificação anterior expirou; envie o e-mail novamente"
                        )
                        .apply();
            }
        }
    }

    private String groupConversationKey(Notification notification) {
        CharSequence conversation = notification.extras.getCharSequence(
                Notification.EXTRA_CONVERSATION_TITLE
        );

        if (conversation != null && conversation.length() > 0) {
            return normalize(conversation.toString());
        }

        // Evita usar EXTRA_TITLE, pois em alguns aparelhos ele muda com o remetente.
        return "grupo";
    }

    private JSONArray categoryResponsesById(
            SharedPreferences prefs,
            String categoryId
    ) {
        try {
            JSONArray categories = new JSONArray(
                    prefs.getString("text_categories", "[]")
            );

            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.optJSONObject(i);
                if (category == null) continue;

                if (!categoryId.equals(category.optString("id"))) continue;

                return cleanResponses(
                        category.optJSONArray("responses")
                );
            }

        } catch (Exception ignored) {
        }

        return new JSONArray();
    }

    private JSONArray categoryResponsesByName(
            SharedPreferences prefs,
            String categoryName
    ) {
        try {
            JSONArray categories = new JSONArray(
                    prefs.getString("text_categories", "[]")
            );

            String wanted = normalize(categoryName);

            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.optJSONObject(i);
                if (category == null) continue;

                String name = normalize(
                        category.optString("name", "")
                );

                if (!wanted.equals(name)) continue;

                return cleanResponses(
                        category.optJSONArray("responses")
                );
            }

        } catch (Exception ignored) {
        }

        return new JSONArray();
    }

    private JSONArray cleanResponses(JSONArray responses) {
        JSONArray result = new JSONArray();

        if (responses == null) return result;

        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.optJSONObject(i);
            if (response == null) continue;

            String type =
                    response.optString(
                            "type",
                            "text"
                    );

            if ("link".equals(type)) {
                String name =
                        response.optString(
                                "name",
                                ""
                        ).trim();

                String url =
                        response.optString(
                                "url",
                                ""
                        ).trim();

                if (url.isEmpty()) {
                    continue;
                }

                if (name.isEmpty()) {
                    result.put(url);
                } else {
                    result.put(
                            "🔗 " + name + "\n" + url
                    );
                }

                continue;
            }

            String text =
                    response.optString(
                            "text",
                            ""
                    ).trim();

            if (!text.isEmpty()) {
                result.put(text);
            }
        }

        return result;
    }

    private void sendConfiguredWithProtection(
            Notification notification,
            String notificationKey,
            String ruleKey,
            String incoming,
            SharedPreferences prefs,
            String messageKey,
            String defaultMessage,
            java.util.Map<String, String> variables
    ) {
        if (!MessageSettings.isEnabled(
                prefs,
                messageKey
        )) {
            return;
        }

        String message =
                MessageSettings.getText(
                        prefs,
                        messageKey,
                        defaultMessage
                );

        // Migração transparente do menu antigo do @infor.
        // Se o usuário ainda tiver salvo o texto fixo das versões anteriores,
        // substituímos a lista fixa pela lista dinâmica das categorias reais.
        if (MessageSettings.MSG_GROUP_MENU.equals(
                messageKey
        )
                && !message.contains(
                "{CATEGORIAS}"
        )) {

            if (message.contains(
                    "1️⃣ Tutoriais"
            )
                    || message.contains(
                    "4️⃣ Banners"
            )) {
                message =
                        MessageSettings.DEFAULT_GROUP_MENU;
            } else {
                message =
                        message
                                + "\n\n"
                                + "{CATEGORIAS}";
            }
        }

        message =
                MessageSettings.applyVariables(
                        message,
                        variables
                );

        if (message.trim().isEmpty()) {
            return;
        }

        sendSingleWithProtection(
                notification,
                notificationKey,
                ruleKey,
                incoming,
                message
        );
    }

    private void sendSingleWithProtection(
            Notification notification,
            String notificationKey,
            String ruleKey,
            String incoming,
            String message
    ) {
        String fingerprint =
                notificationKey + "|" + ruleKey + "|" + incoming;

        if (!reserveFingerprint(fingerprint)) return;

        boolean sent = reply(notification, message);

        updateStatus(
                sent
                        ? "Última autoresposta: enviada"
                        : "Última autoresposta: não disponível"
        );
    }

    private void sendSequenceWithProtection(
            Notification notification,
            String notificationKey,
            String ruleKey,
            String incoming,
            JSONArray messages
    ) {
        String fingerprint =
                notificationKey + "|" + ruleKey + "|" + incoming;

        if (!reserveFingerprint(fingerprint)) return;

        updateStatus(
                "Última sequência: iniciada com " +
                        messages.length() +
                        " respostas"
        );

        int lastIndex = messages.length() - 1;

        for (int i = 0; i < messages.length(); i++) {
            final String message = messages.optString(i, "").trim();
            if (message.isEmpty()) continue;

            final boolean last = i == lastIndex;

            SharedPreferences prefs =
                    getSharedPreferences(
                            "master_responde",
                            MODE_PRIVATE
                    );

            long sequenceInterval =
                    MessageSettings.getMilliseconds(
                            prefs,
                            MessageSettings.KEY_SEQUENCE_INTERVAL_SECONDS,
                            MessageSettings.DEFAULT_SEQUENCE_INTERVAL_SECONDS
                    );

            long delay =
                    i * sequenceInterval;

            handler.postDelayed(
                    () -> {
                        boolean sent = reply(notification, message);

                        if (!sent) {
                            updateStatus(
                                    "Última sequência: envio interrompido"
                            );
                        } else if (last) {
                            updateStatus(
                                    "Última sequência: concluída"
                            );
                        }
                    },
                    delay
            );
        }
    }

    private boolean reserveFingerprint(String fingerprint) {
        synchronized (SEND_LOCK) {
            long now = System.currentTimeMillis();

            SharedPreferences prefs =
                    getSharedPreferences(
                            "master_responde",
                            MODE_PRIVATE
                    );

            long duplicateWindow =
                    MessageSettings.getMilliseconds(
                            prefs,
                            MessageSettings.KEY_RULE_DUPLICATE_SECONDS,
                            MessageSettings.DEFAULT_RULE_DUPLICATE_SECONDS
                    );

            Long memoryTime =
                    RECENT.get(
                            fingerprint
                    );

            if (memoryTime != null
                    && now - memoryTime < duplicateWindow) {
                return false;
            }

            String last = prefs.getString(
                    "last_rule_fingerprint",
                    ""
            );

            long lastTime = prefs.getLong(
                    "last_rule_fingerprint_time",
                    0L
            );

            if (fingerprint.equals(last)
                    && now - lastTime < duplicateWindow) {
                RECENT.put(fingerprint, now);
                return false;
            }

            RECENT.put(fingerprint, now);

            prefs.edit()
                    .putString(
                            "last_rule_fingerprint",
                            fingerprint
                    )
                    .putLong(
                            "last_rule_fingerprint_time",
                            now
                    )
                    .commit();

            if (RECENT.size() > 250) {
                RECENT.clear();
                RECENT.put(fingerprint, now);
            }

            return true;
        }
    }

    private void updateStatus(String value) {
        getSharedPreferences(
                "master_responde",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "last_test_status",
                        value
                )
                .apply();
    }

    private boolean reply(
            Notification notification,
            String message
    ) {
        return WhatsAppReply.send(
                this,
                notification,
                message
        );
    }

    private String normalize(String value) {
        if (value == null) return "";

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
