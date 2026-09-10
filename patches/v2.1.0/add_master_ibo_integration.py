from pathlib import Path

app = Path("projeto/app")
java = app / "src/main/java/com/masterresponde/app"
manifest = app / "src/main/AndroidManifest.xml"
gradle = app / "build.gradle"
main_activity = java / "MainActivity.java"
sources = Path(__file__).parent / "master_ibo"

java.mkdir(parents=True, exist_ok=True)

for name in (
    "MasterIboSettingsActivity.java",
    "MasterIboMotor.java",
    "MasterIboWebAutomation.java",
):
    src = sources / name
    if not src.exists():
        raise SystemExit(f"Fonte MASTER IBO ausente: {src}")
    (java / name).write_text(src.read_text(encoding="utf-8"), encoding="utf-8")

if gradle.exists():
    g = gradle.read_text(encoding="utf-8")
    dep = "implementation 'androidx.security:security-crypto:1.1.0-alpha06'"
    if dep not in g:
        if "dependencies {" in g:
            g = g.replace("dependencies {", "dependencies {\n    " + dep, 1)
        else:
            g = g.rstrip() + "\n\ndependencies {\n    " + dep + "\n}\n"
    gradle.write_text(g, encoding="utf-8")

if manifest.exists():
    m = manifest.read_text(encoding="utf-8")
    if "android.permission.INTERNET" not in m:
        m = m.replace(
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.INTERNET"/>',
            1,
        )
    if ".MasterIboSettingsActivity" not in m:
        entry = '''
        <activity
            android:name=".MasterIboSettingsActivity"
            android:screenOrientation="portrait"
            android:exported="false"/>
'''
        marker = '''
        <activity
            android:name=".SigmaPanelSettingsActivity"
'''
        if marker in m:
            m = m.replace(marker, entry + "\n" + marker, 1)
        else:
            m = m.replace("</application>", entry + "\n    </application>", 1)
    manifest.write_text(m, encoding="utf-8")

if main_activity.exists():
    source = main_activity.read_text(encoding="utf-8")
    if '"MASTER IBO"' not in source:
        anchor = '''        drawerItem(
                "MÍDIAS / LINKS",
'''
        item = '''        drawerItem(
                "MASTER IBO",
                true,
                () -> launchFromMenu(
                        new Intent(
                                this,
                                MasterIboSettingsActivity.class
                        )
                )
        );

'''
        if anchor not in source:
            raise SystemExit("Ponto do menu principal não encontrado; abortando sem tocar nos motores existentes.")
        source = source.replace(anchor, item + anchor, 1)
    main_activity.write_text(source, encoding="utf-8")

print("MASTER IBO real integrado como motor isolado.")
