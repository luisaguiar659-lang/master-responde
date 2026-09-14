package com.masterresponde.app;

import android.app.Notification;

public class TestTaskBridge {

    private static final Object LOCK =
            new Object();

    private static Notification notification;
    private static long createdAt;
    private static String requestKey = "";

    public static boolean begin(
            Notification sourceNotification,
            String key
    ) {
        synchronized (LOCK) {
            long now =
                    System.currentTimeMillis();

            if (notification != null
                    && now - createdAt < 60000L) {
                return false;
            }

            notification =
                    sourceNotification;

            createdAt =
                    now;

            requestKey =
                    key == null
                            ? ""
                            : key;

            return true;
        }
    }

    public static Notification getNotification() {
        synchronized (LOCK) {
            return notification;
        }
    }

    public static boolean hasPending() {
        synchronized (LOCK) {
            return notification != null
                    && System.currentTimeMillis() - createdAt < 120000L;
        }
    }

    public static String getRequestKey() {
        synchronized (LOCK) {
            return requestKey;
        }
    }

    public static void complete() {
        synchronized (LOCK) {
            notification = null;
            createdAt = 0L;
            requestKey = "";
        }
    }
}
