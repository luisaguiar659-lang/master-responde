from pathlib import Path

java = Path("projeto/app/src/main/java/com/masterresponde/app")
service = java / "NotificationAccessService.java"

java.mkdir(parents=True, exist_ok=True)

(java / "MasterIboCommandRouter.java").write_text(r'''package com.masterresponde.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;

public final class MasterIboCommandRouter {
    private MasterIboCommandRouter() {}

    public static boolean handle(
            Context context,
            Notification notification,
            String contactKey,
            String incoming,
            SharedPreferences prefs
    ) {
        return MasterIboMotor.handle(
                context,
                notification,
                contactKey,
                incoming,
                prefs
        );
    }
}
''', encoding="utf-8")

if not service.exists():
    raise SystemExit("NotificationAccessService.java não encontrado.")

source = service.read_text(encoding="utf-8")

if "MasterIboCommandRouter.handle(" not in source:
    anchor = '''        String resellerContactKey =
                privateContactKey(
                        notification
                );

'''
    hook = '''        String resellerContactKey =
                privateContactKey(
                        notification
                );

        if (MasterIboCommandRouter.handle(
                this,
                notification,
                resellerContactKey,
                incoming,
                prefs
        )) {
            return;
        }

'''
    if anchor not in source:
        raise SystemExit("Ponto seguro do fluxo privado não encontrado; abortando sem alterar motores existentes.")
    source = source.replace(anchor, hook, 1)

service.write_text(source, encoding="utf-8")
print("Roteador MASTER IBO conectado antes dos fluxos existentes.")
