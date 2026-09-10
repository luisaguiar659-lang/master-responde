from pathlib import Path

app = Path('projeto/app/src/main/java/com/masterresponde/app')

# Integração IBO sem alteração dos motores existentes.
# Cria apenas uma camada de roteamento.

(app / 'MasterIboCommandRouter.java').write_text(r'''package com.masterresponde.app;

import java.util.Locale;

public final class MasterIboCommandRouter {
    private MasterIboCommandRouter() {}

    public static boolean isIboCommand(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("ibo") ||
                t.contains("ativar ibo") ||
                t.contains("renovar ibo") ||
                t.contains("remover ibo") ||
                t.contains("status ibo");
    }

    public static String route(String text) {
        if (!isIboCommand(text)) return "NONE";
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("renovar")) return "IBO_RENEW";
        if (t.contains("remover") || t.contains("excluir")) return "IBO_REMOVE";
        if (t.contains("status") || t.contains("consulta")) return "IBO_STATUS";
        return "IBO_ACTIVATE";
    }
}
''', encoding='utf-8')

print('Master IBO command router criado sem alterar motores existentes')
