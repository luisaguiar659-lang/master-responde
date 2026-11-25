package com.masterresponde.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WhatsAppReply {

    public static final String KEY_LAST_SEND_DIAGNOSTIC = "last_send_diagnostic";
    public static final String KEY_LAST_SEND_DIAGNOSTIC_TIME = "last_send_diagnostic_time";

    private static final Object LOCK =
            new Object();

    private static final Handler HANDLER =
            new Handler(
                    Looper.getMainLooper()
            );

    private static final Map<String, Long> LAST_SCHEDULED =
            new ConcurrentHashMap<>();

    public static boolean send(
            Context context,
            Notification notification,
            String message
    ) {
        if (context == null) return false;

        if (notification == null) {
            recordDiagnostic(context, "FALHA: notificação original ausente");
            return false;
        }
        if (message == null || message.trim().isEmpty()) {
            recordDiagnostic(context, "FALHA: texto da resposta vazio");
            return false;
        }

        ReplyActionInfo actionInfo = inspectReplyActions(notification);
        if (actionInfo.freeFormInputs == 0) {
            recordDiagnostic(context, "FALHA: sem RemoteInput de texto livre; ações="
                    + actionInfo.actions + ", RemoteInputs=" + actionInfo.remoteInputs);
            return false;
        }
        recordDiagnostic(context, "RESPOSTA AGENDADA: ação=" + actionInfo.actions
                + ", RemoteInputs=" + actionInfo.remoteInputs
                + ", texto livre=" + actionInfo.freeFormInputs);

        Context appContext =
                context.getApplicationContext();

        SharedPreferences prefs =
                appContext.getSharedPreferences(
                        "master_responde",
                        Context.MODE_PRIVATE
                );

        long replyDelay =
                MessageSettings.getMilliseconds(
                        prefs,
                        MessageSettings.KEY_REPLY_DELAY_SECONDS,
                        MessageSettings.DEFAULT_REPLY_DELAY_SECONDS
                );

        long minimumInterval =
                MessageSettings.getMilliseconds(
                        prefs,
                        MessageSettings.KEY_MIN_REPLY_INTERVAL_SECONDS,
                        MessageSettings.DEFAULT_MIN_REPLY_INTERVAL_SECONDS
                );

        String conversationKey =
                conversationKey(
                        notification
                );

        long now =
                System.currentTimeMillis();

        long scheduledAt;

        String queueKey =
                "reply_queue_"
                        + hashKey(
                        conversationKey
                );

        synchronized (LOCK) {
            Long memoryLast =
                    LAST_SCHEDULED.get(
                            conversationKey
                    );

            long persistentLast =
                    prefs.getLong(
                            queueKey,
                            0L
                    );

            long last =
                    persistentLast;

            if (memoryLast != null
                    && memoryLast > last) {
                last =
                        memoryLast;
            }

            long earliest =
                    now + replyDelay;

            if (last > 0L) {
                earliest =
                        Math.max(
                                earliest,
                                last + minimumInterval
                        );
            }

            scheduledAt =
                    earliest;

            LAST_SCHEDULED.put(
                    conversationKey,
                    scheduledAt
            );

            prefs.edit()
                    .putLong(
                            queueKey,
                            scheduledAt
                    )
                    .apply();

            if (LAST_SCHEDULED.size() > 300) {
                LAST_SCHEDULED.clear();

                LAST_SCHEDULED.put(
                        conversationKey,
                        scheduledAt
                );
            }
        }

        long delay =
                Math.max(
                        0L,
                        scheduledAt - now
                );

        HANDLER.postDelayed(
                () -> sendNow(
                        appContext,
                        notification,
                        message
                ),
                delay
        );

        return true;
    }

    private static final class ReplyActionInfo {
        int actions;
        int actionsWithIntent;
        int remoteInputs;
        int freeFormInputs;
    }

    private static ReplyActionInfo inspectReplyActions(Notification notification) {
        ReplyActionInfo info = new ReplyActionInfo();
        Notification.Action[] actions = notification == null ? null : notification.actions;
        if (actions == null) return info;
        info.actions = actions.length;
        for (Notification.Action action : actions) {
            if (action == null || action.actionIntent == null) continue;
            info.actionsWithIntent++;
            RemoteInput[] inputs = action.getRemoteInputs();
            if (inputs == null) continue;
            for (RemoteInput input : inputs) {
                if (input == null) continue;
                info.remoteInputs++;
                if (input.getAllowFreeFormInput()) info.freeFormInputs++;
            }
        }
        return info;
    }

    private static void recordDiagnostic(Context context, String value) {
        if (context == null) return;
        try {
            context.getApplicationContext().getSharedPreferences(
                    "master_responde", Context.MODE_PRIVATE).edit()
                    .putString(KEY_LAST_SEND_DIAGNOSTIC, value)
                    .putLong(KEY_LAST_SEND_DIAGNOSTIC_TIME, System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) { }
    }

    private static boolean sendNow(
            Context context,
            Notification notification,
            String message
    ) {
        Notification.Action[] actions = notification == null ? null : notification.actions;

        if (actions == null) {
            recordDiagnostic(context, "FALHA NO ENVIO: ações da notificação indisponíveis no momento do envio");
            return false;
        }

        for (Notification.Action action : actions) {
            if (action == null
                    || action.actionIntent == null) {
                continue;
            }

            RemoteInput[] inputs =
                    action.getRemoteInputs();

            if (inputs == null
                    || inputs.length == 0) {
                continue;
            }

            for (RemoteInput input : inputs) {
                if (input == null
                        || !input.getAllowFreeFormInput()) {
                    continue;
                }

                try {
                    Intent intent =
                            new Intent();

                    Bundle results =
                            new Bundle();

                    results.putCharSequence(
                            input.getResultKey(),
                            message
                    );

                    RemoteInput.addResultsToIntent(
                            new RemoteInput[]{input},
                            intent,
                            results
                    );

                    // Registre ANTES de disparar o PendingIntent. Em alguns
                    // aparelhos o callback da nova notificação pode chegar quase
                    // imediatamente e, se o registro vier depois, a própria
                    // resposta do bot pode ser tratada como entrada.
                    rememberSent(
                            context,
                            notification,
                            message
                    );

                    try {
                        action.actionIntent.send(context, 0, intent);
                        ConversationStore.recordOutgoing(context, notification, message);
                        recordDiagnostic(context, "ENVIO ACEITO pelo Android: PendingIntent executado");
                    } catch (PendingIntent.CanceledException e) {
                        forgetSent(context, notification);
                        recordDiagnostic(context, "FALHA NO ENVIO: PendingIntent cancelado antes da execução");
                        return false;
                    } catch (SecurityException e) {
                        forgetSent(context, notification);
                        recordDiagnostic(context, "FALHA NO ENVIO: bloqueio de segurança ao executar PendingIntent");
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    forgetSent(context, notification);
                    recordDiagnostic(context, "FALHA NO ENVIO: exceção " + e.getClass().getSimpleName());
                    return false;
                }
            }
        }

        return false;
    }

    public static boolean hasRecentOutgoing(
            Context context,
            Notification notification
    ) {
        if (context == null
                || notification == null) {
            return false;
        }

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                "master_responde",
                                Context.MODE_PRIVATE
                        );

        String key =
                sentEchoKey(
                        notification
                );

        long sentAt =
                prefs.getLong(
                        key + "_at",
                        0L
                );

        return sentAt > 0L
                && System.currentTimeMillis() - sentAt
                <= AutomationTiming.getMilliseconds(prefs, AutomationTiming.KEY_ECHO_WINDOW_MS, AutomationTiming.DEFAULT_ECHO_WINDOW_MS);
    }

    public static boolean isRecentlySent(
            Context context,
            Notification notification,
            String message
    ) {
        if (context == null
                || notification == null
                || message == null
                || message.trim().isEmpty()) {
            return false;
        }

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                "master_responde",
                                Context.MODE_PRIVATE
                        );

        String key =
                sentEchoKey(
                        notification
                );

        long sentAt =
                prefs.getLong(
                        key + "_at",
                        0L
                );

        if (sentAt <= 0L
                || System.currentTimeMillis() - sentAt
                > AutomationTiming.getMilliseconds(prefs, AutomationTiming.KEY_ECHO_WINDOW_MS, AutomationTiming.DEFAULT_ECHO_WINDOW_MS)) {
            return false;
        }

        String expected =
                prefs.getString(
                        key + "_hash",
                        ""
                );

        return !expected.isEmpty()
                && expected.equals(
                hashMessage(
                        message
                )
        );
    }

    private static void rememberSent(
            Context context,
            Notification notification,
            String message
    ) {
        if (context == null
                || notification == null
                || message == null
                || message.trim().isEmpty()) {
            return;
        }

        String key =
                sentEchoKey(
                        notification
                );

        context.getApplicationContext()
                .getSharedPreferences(
                        "master_responde",
                        Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                        key + "_hash",
                        hashMessage(
                                message
                        )
                )
                .putLong(
                        key + "_at",
                        System.currentTimeMillis()
                )
                .apply();
    }

    private static void forgetSent(
            Context context,
            Notification notification
    ) {
        if (context == null
                || notification == null) {
            return;
        }

        String key =
                sentEchoKey(
                        notification
                );

        context.getApplicationContext()
                .getSharedPreferences(
                        "master_responde",
                        Context.MODE_PRIVATE
                )
                .edit()
                .remove(
                        key + "_hash"
                )
                .remove(
                        key + "_at"
                )
                .apply();
    }

    private static String sentEchoKey(
            Notification notification
    ) {
        return "sent_echo_"
                + hashKey(
                conversationKey(
                        notification
                )
        );
    }

    private static String hashKey(
            String value
    ) {
        String safe =
                value == null
                        ? ""
                        : value.trim();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] bytes =
                    digest.digest(
                            safe.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder out =
                    new StringBuilder(24);

            // 96 bits são suficientes para chave local e evitam colisões
            // práticas entre conversas, sem persistir nome/telefone.
            for (int i = 0;
                 i < 12 && i < bytes.length;
                 i++) {
                out.append(
                        String.format(
                                Locale.ROOT,
                                "%02x",
                                bytes[i] & 0xff
                        )
                );
            }

            return out.toString();

        } catch (Exception ignored) {
            return Integer.toHexString(
                    safe.hashCode()
            );
        }
    }

    private static String hashMessage(
            String value
    ) {
        String normalized =
                normalizeMessage(
                        value
                );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] bytes =
                    digest.digest(
                            normalized.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder out =
                    new StringBuilder(
                            bytes.length * 2
                    );

            for (byte item : bytes) {
                out.append(
                        String.format(
                                Locale.ROOT,
                                "%02x",
                                item & 0xff
                        )
                );
            }

            return out.toString();

        } catch (Exception ignored) {
            // Fallback determinístico. Não persiste o conteúdo da mensagem.
            return Integer.toHexString(
                    normalized.hashCode()
            );
        }
    }

    private static String normalizeMessage(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }

    private static String conversationKey(
            Notification notification
    ) {
        if (notification == null) {
            return "unknown";
        }

        String shortcut =
                notification.getShortcutId();

        if (shortcut != null
                && !shortcut.trim().isEmpty()) {
            return "shortcut|"
                    + shortcut.trim();
        }

        if (notification.extras != null) {
            CharSequence conversation =
                    notification.extras
                            .getCharSequence(
                                    Notification.EXTRA_CONVERSATION_TITLE
                            );

            if (conversation != null
                    && conversation.length() > 0) {
                return "conversation|"
                        + conversation.toString();
            }

            CharSequence title =
                    notification.extras
                            .getCharSequence(
                                    Notification.EXTRA_TITLE
                            );

            if (title != null
                    && title.length() > 0) {
                return "title|"
                        + title.toString();
            }
        }

        return "unknown";
    }
}
