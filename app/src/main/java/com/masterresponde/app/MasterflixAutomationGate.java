package com.masterresponde.app;

public final class MasterflixAutomationGate {

    private static final Object LOCK =
            new Object();

    private static String owner =
            "";

    private static long acquiredAt =
            0L;

    private static final long STALE_AFTER_MS =
            120000L;

    private MasterflixAutomationGate() {
    }

    public static boolean tryAcquire(
            String requestedOwner
    ) {
        synchronized (LOCK) {
            long now =
                    System.currentTimeMillis();

            if (!owner.isEmpty()
                    && now - acquiredAt < STALE_AFTER_MS) {
                return false;
            }

            owner =
                    requestedOwner == null
                            ? ""
                            : requestedOwner;

            acquiredAt =
                    now;

            return true;
        }
    }

    public static void release(
            String requestedOwner
    ) {
        synchronized (LOCK) {
            if (owner.isEmpty()) {
                return;
            }

            if (requestedOwner == null
                    || requestedOwner.isEmpty()
                    || owner.equals(
                    requestedOwner
            )) {
                owner =
                        "";

                acquiredAt =
                        0L;
            }
        }
    }

    public static boolean isBusy() {
        synchronized (LOCK) {
            if (owner.isEmpty()) {
                return false;
            }

            if (System.currentTimeMillis() - acquiredAt >= STALE_AFTER_MS) {
                owner =
                        "";

                acquiredAt =
                        0L;

                return false;
            }

            return true;
        }
    }

    public static String getOwner() {
        synchronized (LOCK) {
            return owner;
        }
    }
}
