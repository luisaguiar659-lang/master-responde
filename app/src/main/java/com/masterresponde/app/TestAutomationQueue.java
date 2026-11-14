package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Filas em memória para serializar gerações em cada painel.
 * Cada painel mantém sua própria fila e sua própria sessão WebView.
 */
public final class TestAutomationQueue {
    private static final Object LOCK = new Object();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final ArrayDeque<Task> MASTERFLIX = new ArrayDeque<>();
    private static final ArrayDeque<Task> STV = new ArrayDeque<>();
    private static final Set<String> MASTERFLIX_KEYS = new HashSet<>();
    private static final Set<String> STV_KEYS = new HashSet<>();
    private static boolean masterflixActive;
    private static boolean stvActive;
    private static String activeMasterflixKey = "";
    private static String activeStvKey = "";

    private TestAutomationQueue() {}

    private static final class Task {
        final Context context;
        final Notification notification;
        final SharedPreferences prefs;
        final String requestKey;
        final String stvMode;

        Task(Context c, Notification n, SharedPreferences p, String key) {
            context = c.getApplicationContext();
            notification = n;
            prefs = p;
            requestKey = key == null ? "" : key;
            stvMode = StvPanelConfig.getMode(context);
        }
    }

    public static boolean enqueueMasterflix(Context c, Notification n,
                                             SharedPreferences p, String key) {
        if (c == null || n == null || p == null) return false;
        synchronized (LOCK) {
            if (MASTERFLIX.size() >= 100) return false;
            if (!MASTERFLIX_KEYS.add(key == null ? "" : key)) return false;
            MASTERFLIX.addLast(new Task(c, n, p, key));
        }
        HANDLER.post(TestAutomationQueue::pumpMasterflix);
        return true;
    }

    public static boolean enqueueStv(Context c, Notification n,
                                     SharedPreferences p, String key) {
        if (c == null || n == null || p == null) return false;
        synchronized (LOCK) {
            if (STV.size() >= 100) return false;
            if (!STV_KEYS.add(key == null ? "" : key)) return false;
            STV.addLast(new Task(c, n, p, key));
        }
        HANDLER.post(TestAutomationQueue::pumpStv);
        return true;
    }

    private static void pumpMasterflix() {
        final Task task;
        synchronized (LOCK) {
            if (masterflixActive || MASTERFLIX.isEmpty()) return;
            if (MasterflixAutomationGate.isBusy()
                    || MasterflixBackgroundAutomation.isRunning()
                    || MasterflixResellerAutomation.isRunning()) {
                HANDLER.postDelayed(TestAutomationQueue::pumpMasterflix, 2000L);
                return;
            }
            task = MASTERFLIX.removeFirst();
            masterflixActive = true;
            activeMasterflixKey = task.requestKey;
        }

        if (!TestTaskBridge.begin(task.notification, task.requestKey)) {
            retryMasterflix(task);
            return;
        }
        BackgroundRuntime.markTestPending(task.prefs, task.notification, task.requestKey);
        boolean started = MasterflixBackgroundAutomation.start(
                task.context, task.notification, task.prefs);
        if (!started) {
            TestTaskBridge.complete();
            BackgroundRuntime.clearTestPending(task.prefs);
            retryMasterflix(task);
        }
    }

    private static void retryMasterflix(Task task) {
        synchronized (LOCK) {
            MASTERFLIX.addFirst(task);
            masterflixActive = false;
        }
        HANDLER.postDelayed(TestAutomationQueue::pumpMasterflix, 2000L);
    }

    private static void pumpStv() {
        final Task task;
        synchronized (LOCK) {
            if (stvActive || STV.isEmpty()) return;
            if (StvPlusBackgroundAutomation.isRunning()) {
                HANDLER.postDelayed(TestAutomationQueue::pumpStv, 1500L);
                return;
            }
            task = STV.removeFirst();
            stvActive = true;
            activeStvKey = task.requestKey;
        }
        boolean started = StvPlusBackgroundAutomation.start(
                task.context, task.notification, task.prefs, task.stvMode);
        if (!started) {
            synchronized (LOCK) {
                STV.addFirst(task);
                stvActive = false;
            }
            HANDLER.postDelayed(TestAutomationQueue::pumpStv, 1500L);
        }
    }

    public static void onMasterflixFinished() {
        synchronized (LOCK) {
            masterflixActive = false;
            MASTERFLIX_KEYS.remove(activeMasterflixKey);
            activeMasterflixKey = "";
        }
        HANDLER.post(TestAutomationQueue::pumpMasterflix);
    }

    public static void onStvFinished() {
        synchronized (LOCK) {
            stvActive = false;
            STV_KEYS.remove(activeStvKey);
            activeStvKey = "";
        }
        HANDLER.post(TestAutomationQueue::pumpStv);
    }

    public static int masterflixWaiting() {
        synchronized (LOCK) { return MASTERFLIX.size(); }
    }

    public static int stvWaiting() {
        synchronized (LOCK) { return STV.size(); }
    }
}
