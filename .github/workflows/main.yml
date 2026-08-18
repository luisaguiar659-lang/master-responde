name: Build MASTER RESPONDE APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Instalar Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: "8.7"

      - name: Validar assinatura
        env:
          KEYSTORE_B64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
          STORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
        run: |
          if [ -z "$KEYSTORE_B64" ] || \
             [ -z "$STORE_PASSWORD" ] || \
             [ -z "$KEY_ALIAS" ] || \
             [ -z "$KEY_PASSWORD" ]; then
            echo "ERRO: configure os 4 GitHub Secrets de assinatura antes de compilar."
            exit 1
          fi

      - name: Localizar ZIP da versão
        run: |
          ZIP="MASTER-RESPONDE-v1.17.16-FIX-REGRA-APOS-TESTE.zip"

          if [ ! -f "$ZIP" ]; then
            echo "ERRO: arquivo $ZIP não encontrado na raiz do repositório."
            find . -maxdepth 2 -type f | sort
            exit 1
          fi

      - name: Extrair projeto
        run: |
          rm -rf projeto
          mkdir -p projeto
          unzip -q MASTER-RESPONDE-v1.17.16-FIX-REGRA-APOS-TESTE.zip -d projeto

      - name: Detectar diretório Gradle
        id: project
        shell: bash
        run: |
          SETTINGS=$(find projeto \
            \( -name "settings.gradle" -o -name "settings.gradle.kts" \) \
            -type f | head -1)

          if [ -z "$SETTINGS" ]; then
            echo "ERRO: settings.gradle/settings.gradle.kts não encontrado."
            exit 1
          fi

          PROJECT_DIR=$(dirname "$SETTINGS")
          echo "dir=$PROJECT_DIR" >> "$GITHUB_OUTPUT"

          if [ ! -f "$PROJECT_DIR/app/build.gradle" ] && \
             [ ! -f "$PROJECT_DIR/app/build.gradle.kts" ]; then
            echo "ERRO: módulo app não encontrado."
            exit 1
          fi

      - name: Preparar chave de assinatura
        env:
          KEYSTORE_B64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
        run: |
          KEYSTORE="$RUNNER_TEMP/master-responde-release.jks"
          printf '%s' "$KEYSTORE_B64" | base64 --decode > "$KEYSTORE"

          if [ ! -s "$KEYSTORE" ]; then
            echo "ERRO: falha ao gerar o arquivo de assinatura."
            exit 1
          fi

      - name: Compilar APK release
        working-directory: ${{ steps.project.outputs.dir }}
        env:
          ANDROID_KEYSTORE_PATH: ${{ runner.temp }}/master-responde-release.jks
          ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          ANDROID_KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
        run: |
          if [ -f "./gradlew" ]; then
            chmod +x ./gradlew
            ./gradlew :app:assembleRelease --stacktrace --no-daemon
          else
            gradle :app:assembleRelease --stacktrace --no-daemon
          fi

      - name: Preparar APK final
        run: |
          APK=$(find projeto \
            -type f \
            -path "*/app/build/outputs/apk/release/*.apk" \
            ! -name "*unsigned*" \
            | head -1)

          if [ -z "$APK" ]; then
            APK=$(find projeto \
              -type f \
              -path "*/app/build/outputs/apk/release/*.apk" \
              | head -1)
          fi

          if [ -z "$APK" ]; then
            echo "ERRO: APK release não encontrado."
            find projeto -path "*build/outputs*" -type f -print || true
            exit 1
          fi

          cp "$APK" MASTER-RESPONDE-v1.17.16.apk
          ls -lh MASTER-RESPONDE-v1.17.16.apk

      - name: Enviar APK
        uses: actions/upload-artifact@v4
        with:
          name: MASTER-RESPONDE-v1.17.16
          path: MASTER-RESPONDE-v1.17.16.apk
          if-no-files-found: error
          retention-days: 30
