from pathlib import Path

app = Path('projeto/app')
java = app / 'src/main/java/com/masterresponde/app'
manifest = app / 'src/main/AndroidManifest.xml'

java.mkdir(parents=True, exist_ok=True)

# Integração IBO como módulo isolado. Não altera motores existentes.
(java / 'MasterIboAutomation.java').write_text(r'''package com.masterresponde.app;

public final class MasterIboAutomation {
    private MasterIboAutomation() {}

    public static String detect(String text) {
        if (text == null) return "";
        String t = text.toLowerCase();
        if (t.contains("ibo")) return "IBO";
        return "";
    }

    public static String status() {
        return "MASTER IBO MODULE READY";
    }
}
''', encoding='utf-8')

(java / 'MasterIboCommands.java').write_text(r'''package com.masterresponde.app;

public final class MasterIboCommands {
    private MasterIboCommands() {}

    public static boolean isIboCommand(String text) {
        if (text == null) return false;
        String t = text.toLowerCase();
        return t.contains("ibo") || t.contains("ativar ibo") || t.contains("renovar ibo") || t.contains("remover ibo");
    }
}
''', encoding='utf-8')

if manifest.exists():
    m = manifest.read_text(encoding='utf-8')
    manifest.write_text(m, encoding='utf-8')

print('MASTER IBO integration module added without touching existing engines')
