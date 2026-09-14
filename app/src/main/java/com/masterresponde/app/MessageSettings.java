package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessageSettings {

    public static final String KEY_TEST_TRIGGERS =
            "cfg_test_triggers";

    public static final String KEY_RESELLER_TRIGGERS =
            "cfg_reseller_triggers";

    public static final String KEY_RESELLER_CANCEL_TRIGGER =
            "cfg_reseller_cancel_trigger";

    public static final String KEY_DIAGNOSTIC_TRIGGER =
            "cfg_diagnostic_trigger";

    public static final String KEY_GROUP_TRIGGER =
            "cfg_group_trigger";

    public static final String KEY_GROUP_OPTION_0 =
            "cfg_group_option_0";

    public static final String KEY_GROUP_OPTION_1 =
            "cfg_group_option_1";

    public static final String KEY_GROUP_OPTION_2 =
            "cfg_group_option_2";

    public static final String KEY_GROUP_OPTION_3 =
            "cfg_group_option_3";

    public static final String KEY_GROUP_OPTION_4 =
            "cfg_group_option_4";

    public static final String KEY_GROUP_OPTION_5 =
            "cfg_group_option_5";

    public static final String KEY_GROUP_OPTION_6 =
            "cfg_group_option_6";

    public static final String KEY_GROUP_OPTION_7 =
            "cfg_group_option_7";

    public static final String KEY_GROUP_OPTION_8 =
            "cfg_group_option_8";

    public static final String KEY_REPLY_DELAY_SECONDS =
            "cfg_reply_delay_seconds";

    public static final String KEY_MIN_REPLY_INTERVAL_SECONDS =
            "cfg_min_reply_interval_seconds";

    public static final String KEY_SEQUENCE_INTERVAL_SECONDS =
            "cfg_sequence_interval_seconds";

    public static final String KEY_PRIVATE_DUPLICATE_SECONDS =
            "cfg_private_duplicate_seconds";

    public static final String KEY_RULE_DUPLICATE_SECONDS =
            "cfg_rule_duplicate_seconds";

    public static final String KEY_GROUP_DUPLICATE_SECONDS =
            "cfg_group_duplicate_seconds";

    public static final String KEY_GROUP_MENU_SECONDS =
            "cfg_group_menu_seconds";

    public static final String MSG_DIAGNOSTIC =
            "msg_diagnostic";

    public static final String MSG_TEST_WAIT =
            "msg_test_wait";

    public static final String MSG_TEST_FAIL =
            "msg_test_fail";

    public static final String MSG_RESELLER_SIGNUP =
            "msg_reseller_signup";

    public static final String MSG_RESELLER_VERIFYING =
            "msg_reseller_verifying";

    public static final String MSG_RESELLER_CONFIRMED =
            "msg_reseller_confirmed";

    public static final String MSG_RESELLER_NOT_FOUND =
            "msg_reseller_not_found";

    public static final String MSG_RESELLER_CANCELLED =
            "msg_reseller_cancelled";

    public static final String MSG_RESELLER_ALREADY_LINKED =
            "msg_reseller_already_linked";

    public static final String MSG_RESELLER_REVIEW =
            "msg_reseller_review";

    public static final String MSG_RESELLER_INVALID_EMAIL =
            "msg_reseller_invalid_email";

    public static final String MSG_RESELLER_SAVE_ERROR =
            "msg_reseller_save_error";

    public static final String MSG_RESELLER_BUSY =
            "msg_reseller_busy";

    public static final String MSG_GROUP_MENU =
            "msg_group_menu";

    public static final String MSG_GROUP_CATEGORY_MISSING =
            "msg_group_category_missing";

    public static final String MSG_GROUP_FUTURE =
            "msg_group_future";

    public static final String MSG_GROUP_SUPPORT =
            "msg_group_support";

    public static final String MSG_GROUP_UNKNOWN =
            "msg_group_unknown";

    public static final String DEFAULT_TEST_TRIGGERS =
            "teste\n" +
            "quero um teste\n" +
            "quero teste\n" +
            "gerar teste\n" +
            "gerar um teste";

    public static final String DEFAULT_RESELLER_TRIGGERS =
            "quero ser revenda\n" +
            "quero ser uma revenda\n" +
            "quero revenda\n" +
            "ser revenda";

    public static final String DEFAULT_RESELLER_CANCEL_TRIGGER =
            "cancelar";

    public static final String DEFAULT_DIAGNOSTIC_TRIGGER =
            "teste bot";

    public static final String DEFAULT_GROUP_TRIGGER =
            "@infor";

    public static final String DEFAULT_GROUP_OPTION_0 =
            "0\nsuporte";

    public static final String DEFAULT_GROUP_OPTION_1 =
            "1\ntutorial\ntutoriais";

    public static final String DEFAULT_GROUP_OPTION_2 =
            "2\nplano\nplanos\npromocao\npromocoes";

    public static final String DEFAULT_GROUP_OPTION_3 =
            "3\naplicativo\naplicativos\napps\napp parceiro";

    public static final String DEFAULT_GROUP_OPTION_4 =
            "4\nbanner\nbanners";

    public static final String DEFAULT_GROUP_OPTION_5 =
            "5\naviso\navisos";

    public static final String DEFAULT_GROUP_OPTION_6 =
            "6\napresentacao\napresentacao do servidor";

    public static final String DEFAULT_GROUP_OPTION_7 =
            "7\ncredito\ncreditos";

    public static final String DEFAULT_GROUP_OPTION_8 =
            "8\nteste";

    public static final String DEFAULT_DIAGNOSTIC =
            "✅ MASTER RESPONDE ativo e funcionando.";

    public static final String DEFAULT_TEST_WAIT =
            "Gerando seu teste, aguarde...";

    public static final String DEFAULT_TEST_FAIL =
            "Não consegui concluir o teste automaticamente. " +
            "Confira o painel Sigma no MASTER RESPONDE.";

    public static final String DEFAULT_RESELLER_SIGNUP =
            "🚀 Para ser Revenda, faça seu cadastro pelo link:\n\n" +
            "{LINK_CADASTRO}\n\n" +
            "Após concluir seu cadastro, envie o e-mail usado no cadastro para ativação do painel.";

    public static final String DEFAULT_RESELLER_VERIFYING =
            "Recebi seu e-mail. Verificando seu cadastro de revenda, aguarde...";

    public static final String DEFAULT_RESELLER_CONFIRMED =
            "✅ Cadastro de revenda confirmado!\n\n" +
            "Entre no nosso grupo de suporte e solicite a ativação do seu painel.\n\n" +
            "📲 *Grupo de suporte:*\n" +
            "{LINK_GRUPO}";

    public static final String DEFAULT_RESELLER_NOT_FOUND =
            "Não consegui confirmar seu cadastro de revenda no painel Sigma.\n\n" +
            "{MOTIVO}";

    public static final String DEFAULT_RESELLER_CANCELLED =
            "Fluxo de revenda cancelado.";

    public static final String DEFAULT_RESELLER_ALREADY_LINKED =
            "✅ Este WhatsApp já possui uma revenda vinculada no MASTER RESPONDE.";

    public static final String DEFAULT_RESELLER_REVIEW =
            "⚠️ Existe uma operação anterior dessa revenda que precisa ser conferida manualmente no painel Sigma antes de iniciar outra.";

    public static final String DEFAULT_RESELLER_INVALID_EMAIL =
            "Envie somente o mesmo e-mail usado no cadastro da revenda.";

    public static final String DEFAULT_RESELLER_SAVE_ERROR =
            "Não consegui salvar o e-mail com segurança. Tente novamente.";

    public static final String DEFAULT_RESELLER_BUSY =
            "O MASTER RESPONDE está concluindo outra verificação. " +
            "Envie o e-mail novamente em alguns instantes.";

    public static final String DEFAULT_GROUP_MENU =
            "🤖 *MASTER RESPONDE — CENTRAL DE SUPORTE*\n\n" +
            "{CATEGORIAS}\n\n" +
            "Digite o número da opção desejada.\n" +
            "Atalho: {COMANDO_MENU} + número ou nome da categoria.";

    public static final String DEFAULT_GROUP_CATEGORY_MISSING =
            "⚠️ A categoria \"{CATEGORIA}\" ainda não foi configurada no MASTER RESPONDE.";

    public static final String DEFAULT_GROUP_FUTURE =
            "🔒 Essa opção será liberada nas próximas etapas do MASTER RESPONDE.";

    public static final String DEFAULT_GROUP_SUPPORT =
            "👤 Um atendente continuará o suporte.";

    public static final String DEFAULT_GROUP_UNKNOWN =
            "⚠️ Opção não reconhecida. Envie {COMANDO_MENU} para ver o menu.";

    public static final int DEFAULT_REPLY_DELAY_SECONDS = 1;
    public static final int DEFAULT_MIN_REPLY_INTERVAL_SECONDS = 1;
    public static final int DEFAULT_SEQUENCE_INTERVAL_SECONDS = 1;
    public static final int DEFAULT_PRIVATE_DUPLICATE_SECONDS = 12;
    public static final int DEFAULT_RULE_DUPLICATE_SECONDS = 10;
    public static final int DEFAULT_GROUP_DUPLICATE_SECONDS = 120;
    public static final int DEFAULT_GROUP_MENU_SECONDS = 120;

    public static String getText(
            SharedPreferences prefs,
            String key,
            String defaultValue
    ) {
        if (prefs == null) {
            return defaultValue;
        }

        return prefs.getString(
                key,
                defaultValue
        );
    }

    public static boolean isEnabled(
            SharedPreferences prefs,
            String messageKey
    ) {
        if (prefs == null) {
            return true;
        }

        return prefs.getBoolean(
                enabledKey(messageKey),
                defaultEnabled(
                        messageKey
                )
        );
    }

    public static boolean defaultEnabled(
            String messageKey
    ) {
        return !MSG_RESELLER_INVALID_EMAIL.equals(
                messageKey
        );
    }

    public static String enabledKey(
            String messageKey
    ) {
        return "enabled_" + messageKey;
    }

    public static int getSeconds(
            SharedPreferences prefs,
            String key,
            int defaultValue
    ) {
        if (prefs == null) {
            return defaultValue;
        }

        int value =
                prefs.getInt(
                        key,
                        defaultValue
                );

        if (value < 0) return 0;
        if (value > 3600) return 3600;

        return value;
    }

    public static long getMilliseconds(
            SharedPreferences prefs,
            String key,
            int defaultValue
    ) {
        return getSeconds(
                prefs,
                key,
                defaultValue
        ) * 1000L;
    }

    public static boolean matchesAnyTrigger(
            SharedPreferences prefs,
            String key,
            String defaultTriggers,
            String incoming
    ) {
        String wanted =
                normalize(
                        incoming
                );

        if (wanted.isEmpty()) {
            return false;
        }

        String configured =
                getText(
                        prefs,
                        key,
                        defaultTriggers
                );

        String[] lines =
                configured.split(
                        "[\\n;]+"
                );

        for (String line : lines) {
            String trigger =
                    normalize(
                            line
                    );

            if (!trigger.isEmpty()
                    && wanted.equals(trigger)) {
                return true;
            }
        }

        return false;
    }

    public static String normalizedSingleTrigger(
            SharedPreferences prefs,
            String key,
            String defaultValue
    ) {
        String configured =
                getText(
                        prefs,
                        key,
                        defaultValue
                );

        String[] lines =
                configured.split(
                        "[\\n;]+"
                );

        for (String line : lines) {
            String value =
                    normalize(
                            line
                    );

            if (!value.isEmpty()) {
                return value;
            }
        }

        return normalize(
                defaultValue
        );
    }

    public static boolean send(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String messageKey,
            String defaultValue
    ) {
        return send(
                context,
                notification,
                prefs,
                messageKey,
                defaultValue,
                null
        );
    }

    public static boolean send(
            Context context,
            Notification notification,
            SharedPreferences prefs,
            String messageKey,
            String defaultValue,
            Map<String, String> variables
    ) {
        if (!isEnabled(
                prefs,
                messageKey
        )) {
            return true;
        }

        String message =
                getText(
                        prefs,
                        messageKey,
                        defaultValue
                );

        message =
                applyVariables(
                        message,
                        variables
                );

        if (message.trim().isEmpty()) {
            return true;
        }

        return WhatsAppReply.send(
                context,
                notification,
                message
        );
    }

    public static Map<String, String> variables(
            String key,
            String value
    ) {
        Map<String, String> result =
                new HashMap<>();

        result.put(
                key,
                value == null
                        ? ""
                        : value
        );

        return result;
    }

    public static Map<String, String> variables(
            String key1,
            String value1,
            String key2,
            String value2
    ) {
        Map<String, String> result =
                new HashMap<>();

        result.put(
                key1,
                value1 == null
                        ? ""
                        : value1
        );

        result.put(
                key2,
                value2 == null
                        ? ""
                        : value2
        );

        return result;
    }

    public static String applyVariables(
            String value,
            Map<String, String> variables
    ) {
        if (value == null) {
            return "";
        }

        String result =
                value;

        if (variables == null) {
            return result;
        }

        for (Map.Entry<String, String> entry
                : variables.entrySet()) {

            String marker =
                    "{"
                            + entry.getKey()
                            + "}";

            result =
                    result.replace(
                            marker,
                            entry.getValue() == null
                                    ? ""
                                    : entry.getValue()
                    );
        }

        return result;
    }

    public static String normalize(
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
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }
}
