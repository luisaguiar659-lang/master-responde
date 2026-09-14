package com.masterresponde.app;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;

public final class BackgroundRuntime {

    public static final String KEY_LISTENER_CONNECTED =
            "background_listener_connected";

    public static final String KEY_LISTENER_LAST_CONNECTED =
            "background_listener_last_connected";

    public static final String KEY_LAST_EVENT_TIME =
            "background_last_event_time";

    public static final String KEY_TEST_PENDING =
            "background_test_pending";

    public static final String KEY_TEST_PENDING_AT =
            "background_test_pending_at";

    public static final String KEY_TEST_LOCATOR =
            "background_test_locator";

    public static final String KEY_TEST_REQUEST_KEY =
            "background_test_request_key";

    public static final String KEY_RESELLER_PENDING =
            "background_reseller_pending";

    public static final String KEY_RESELLER_PENDING_AT =
            "background_reseller_pending_at";

    public static final String KEY_RESELLER_LOCATOR =
            "background_reseller_locator";

    public static final String KEY_RESELLER_CONTACT =
            "background_reseller_contact";

    private static final long PENDING_TIMEOUT_MS =
            120000L;

    private BackgroundRuntime() {
    }

    public static SharedPreferences prefs(
            Context context
    ) {
        return context.getSharedPreferences(
                "master_responde",
                Context.MODE_PRIVATE
        );
    }

    public static boolean hasNotificationAccess(
            Context context
    ) {
        if (context == null) {
            return false;
        }

        String enabled =
                Settings.Secure.getString(
                        context.getContentResolver(),
                        "enabled_notification_listeners"
                );

        if (enabled == null
                || enabled.trim().isEmpty()) {
            return false;
        }

        ComponentName component =
                new ComponentName(
                        context,
                        NotificationAccessService.class
                );

        String full =
                component.flattenToString();

        String shortName =
                component.flattenToShortString();

        for (String item
                : enabled.split(":")) {

            if (full.equalsIgnoreCase(item)
                    || shortName.equalsIgnoreCase(item)) {
                return true;
            }
        }

        return enabled.contains(
                context.getPackageName()
        );
    }

    public static void requestListenerRebind(
            Context context
    ) {
        if (context == null
                || !hasNotificationAccess(context)) {
            return;
        }

        try {
            NotificationListenerService.requestRebind(
                    new ComponentName(
                            context,
                            NotificationAccessService.class
                    )
            );
        } catch (Throwable ignored) {
        }
    }

    public static void markListenerConnected(
            Context context,
            boolean connected
    ) {
        SharedPreferences prefs =
                prefs(context);

        SharedPreferences.Editor editor =
                prefs.edit()
                        .putBoolean(
                                KEY_LISTENER_CONNECTED,
                                connected
                        );

        if (connected) {
            editor.putLong(
                    KEY_LISTENER_LAST_CONNECTED,
                    System.currentTimeMillis()
            );
        }

        editor.apply();
    }

    public static void markEvent(
            Context context
    ) {
        prefs(context)
                .edit()
                .putLong(
                        KEY_LAST_EVENT_TIME,
                        System.currentTimeMillis()
                )
                .apply();
    }

    public static void markTestPending(
            SharedPreferences prefs,
            Notification notification,
            String requestKey
    ) {
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .putBoolean(
                        KEY_TEST_PENDING,
                        true
                )
                .putLong(
                        KEY_TEST_PENDING_AT,
                        System.currentTimeMillis()
                )
                .putString(
                        KEY_TEST_LOCATOR,
                        notificationLocator(
                                notification
                        )
                )
                .putString(
                        KEY_TEST_REQUEST_KEY,
                        requestKey == null
                                ? ""
                                : requestKey
                )
                .apply();
    }

    public static void clearTestPending(
            SharedPreferences prefs
    ) {
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .remove(KEY_TEST_PENDING)
                .remove(KEY_TEST_PENDING_AT)
                .remove(KEY_TEST_LOCATOR)
                .remove(KEY_TEST_REQUEST_KEY)
                .apply();
    }

    public static boolean hasFreshTestPending(
            SharedPreferences prefs
    ) {
        return isFresh(
                prefs,
                KEY_TEST_PENDING,
                KEY_TEST_PENDING_AT
        );
    }

    public static void markResellerPending(
            SharedPreferences prefs,
            Notification notification,
            String contactKey
    ) {
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .putBoolean(
                        KEY_RESELLER_PENDING,
                        true
                )
                .putLong(
                        KEY_RESELLER_PENDING_AT,
                        System.currentTimeMillis()
                )
                .putString(
                        KEY_RESELLER_LOCATOR,
                        notificationLocator(
                                notification
                        )
                )
                .putString(
                        KEY_RESELLER_CONTACT,
                        contactKey == null
                                ? ""
                                : contactKey
                )
                .apply();
    }

    public static void clearResellerPending(
            SharedPreferences prefs
    ) {
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .remove(KEY_RESELLER_PENDING)
                .remove(KEY_RESELLER_PENDING_AT)
                .remove(KEY_RESELLER_LOCATOR)
                .remove(KEY_RESELLER_CONTACT)
                .apply();
    }

    public static boolean hasFreshResellerPending(
            SharedPreferences prefs
    ) {
        return isFresh(
                prefs,
                KEY_RESELLER_PENDING,
                KEY_RESELLER_PENDING_AT
        );
    }

    public static boolean notificationMatchesLocator(
            Notification notification,
            String locator
    ) {
        if (locator == null
                || locator.isEmpty()) {
            return false;
        }

        return locator.equals(
                notificationLocator(
                        notification
                )
        );
    }

    public static String notificationLocator(
            Notification notification
    ) {
        if (notification == null) {
            return "";
        }

        String shortcut =
                notification.getShortcutId();

        if (shortcut != null
                && !shortcut.trim().isEmpty()) {
            return "s|"
                    + digest(
                    shortcut.trim()
            );
        }

        if (notification.extras != null) {
            CharSequence conversation =
                    notification.extras.getCharSequence(
                            Notification.EXTRA_CONVERSATION_TITLE
                    );

            if (conversation != null
                    && conversation.length() > 0) {
                return "c|"
                        + digest(
                        normalize(
                                conversation.toString()
                        )
                );
            }

            CharSequence title =
                    notification.extras.getCharSequence(
                            Notification.EXTRA_TITLE
                    );

            if (title != null
                    && title.length() > 0) {
                return "t|"
                        + digest(
                        normalize(
                                title.toString()
                        )
                );
            }
        }

        return "";
    }

    private static boolean isFresh(
            SharedPreferences prefs,
            String activeKey,
            String timeKey
    ) {
        if (prefs == null
                || !prefs.getBoolean(
                activeKey,
                false
        )) {
            return false;
        }

        long time =
                prefs.getLong(
                        timeKey,
                        0L
                );

        return time > 0L
                && System.currentTimeMillis() - time < PENDING_TIMEOUT_MS;
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }

    private static String digest(
            String value
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] bytes =
                    digest.digest(
                            value.getBytes(
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
                    value.hashCode()
            );
        }
    }
}
