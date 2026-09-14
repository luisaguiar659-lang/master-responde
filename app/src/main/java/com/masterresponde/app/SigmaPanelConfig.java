package com.masterresponde.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class SigmaPanelConfig {

    public static final String DEFAULT_PANEL_NAME =
            "MASTERFLIX";

    public static final String DEFAULT_BASE_URL =
            "https://masterflix.sigmab.pro";

    public static final String KEY_PANEL_NAME =
            "sigma_panel_name";

    public static final String KEY_BASE_URL =
            "sigma_base_url";

    public static final String KEY_PREFERRED_TEST =
            "sigma_preferred_test";

    private SigmaPanelConfig() {
    }

    public static SharedPreferences prefs(
            Context context
    ) {
        return context.getSharedPreferences(
                "master_responde",
                Context.MODE_PRIVATE
        );
    }

    public static String getPanelName(
            Context context
    ) {
        String value =
                prefs(context).getString(
                        KEY_PANEL_NAME,
                        DEFAULT_PANEL_NAME
                );

        value =
                value == null
                        ? ""
                        : value.trim();

        return value.isEmpty()
                ? DEFAULT_PANEL_NAME
                : value;
    }

    public static String getBaseUrl(
            Context context
    ) {
        String value =
                prefs(context).getString(
                        KEY_BASE_URL,
                        DEFAULT_BASE_URL
                );

        return normalizeBaseUrl(
                value
        );
    }

    public static String getPreferredTest(
            Context context
    ) {
        String value =
                prefs(context).getString(
                        KEY_PREFERRED_TEST,
                        ""
                );

        return value == null
                ? ""
                : value.trim();
    }

    public static String loginUrl(
            Context context
    ) {
        return getBaseUrl(context)
                + "/#/sign-in";
    }

    public static String dashboardUrl(
            Context context
    ) {
        return getBaseUrl(context)
                + "/#/dashboard";
    }

    public static String resellersUrl(
            Context context
    ) {
        return getBaseUrl(context)
                + "/#/resellers";
    }

    public static String normalizeBaseUrl(
            String value
    ) {
        String base =
                value == null
                        ? ""
                        : value.trim();

        if (base.isEmpty()) {
            base =
                    DEFAULT_BASE_URL;
        }

        if (!base.startsWith("http://")
                && !base.startsWith("https://")) {
            base =
                    "https://" + base;
        }

        int hash =
                base.indexOf(
                        "#"
                );

        if (hash >= 0) {
            base =
                    base.substring(
                            0,
                            hash
                    );
        }

        while (base.endsWith("/")) {
            base =
                    base.substring(
                            0,
                            base.length() - 1
                    );
        }

        return base;
    }

    public static String getUser(
            Context context
    ) {
        SecureStore store =
                new SecureStore(
                        context
                );

        String value =
                store.get(
                        "sigma_user"
                );

        if (value == null
                || value.isEmpty()) {
            value =
                    store.get(
                            "masterflix_user"
                    );

            if (value != null
                    && !value.isEmpty()) {
                try {
                    store.put(
                            "sigma_user",
                            value
                    );
                } catch (Exception ignored) {
                    // A migração é apenas uma conveniência.
                    // Mesmo que falhe, a credencial antiga já foi lida.
                }
            }
        }

        return value == null
                ? ""
                : value;
    }

    public static String getPass(
            Context context
    ) {
        SecureStore store =
                new SecureStore(
                        context
                );

        String value =
                store.get(
                        "sigma_pass"
                );

        if (value == null
                || value.isEmpty()) {
            value =
                    store.get(
                            "masterflix_pass"
                    );

            if (value != null
                    && !value.isEmpty()) {
                try {
                    store.put(
                            "sigma_pass",
                            value
                    );
                } catch (Exception ignored) {
                    // A migração é apenas uma conveniência.
                    // Mesmo que falhe, a credencial antiga já foi lida.
                }
            }
        }

        return value == null
                ? ""
                : value;
    }

    public static void saveCredentials(
            Context context,
            String user,
            String pass
    ) {
        SecureStore store =
                new SecureStore(
                        context
                );

        boolean saved =
                false;

        try {
            store.put(
                    "sigma_user",
                    user == null
                            ? ""
                            : user.trim()
            );

            store.put(
                    "sigma_pass",
                    pass == null
                            ? ""
                            : pass
            );

            // Mantém compatibilidade com as versões anteriores.
            store.put(
                    "masterflix_user",
                    user == null
                            ? ""
                            : user.trim()
            );

            store.put(
                    "masterflix_pass",
                    pass == null
                            ? ""
                            : pass
            );

            saved =
                    user != null
                            && !user.trim().isEmpty()
                            && pass != null
                            && !pass.isEmpty();

        } catch (Exception ignored) {
            saved =
                    false;
        }

        prefs(context)
                .edit()
                .putBoolean(
                        "masterflix_credentials_saved",
                        saved
                )
                .apply();
    }
}
