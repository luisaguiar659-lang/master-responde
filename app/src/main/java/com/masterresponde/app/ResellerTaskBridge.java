package com.masterresponde.app;

import android.app.Notification;

public class ResellerTaskBridge {

    private static final Object LOCK =
            new Object();

    private static Notification notification;
    private static String contactKey = "";
    private static String email = "";
    private static long createdAt = 0L;

    public static void begin(
            Notification sourceNotification,
            String sourceContactKey,
            String sourceEmail
    ) {
        synchronized (LOCK) {
            notification =
                    sourceNotification;

            contactKey =
                    sourceContactKey == null
                            ? ""
                            : sourceContactKey;

            email =
                    sourceEmail == null
                            ? ""
                            : sourceEmail;

            createdAt =
                    System.currentTimeMillis();
        }
    }

    public static Notification getNotification() {
        synchronized (LOCK) {
            return notification;
        }
    }

    public static String getContactKey() {
        synchronized (LOCK) {
            return contactKey;
        }
    }

    public static String getEmail() {
        synchronized (LOCK) {
            return email;
        }
    }

    public static boolean hasPending() {
        synchronized (LOCK) {
            return notification != null
                    && !contactKey.isEmpty()
                    && !email.isEmpty()
                    && System.currentTimeMillis() - createdAt < 180000L;
        }
    }

    public static void complete() {
        synchronized (LOCK) {
            notification = null;
            contactKey = "";
            email = "";
            createdAt = 0L;
        }
    }
}
