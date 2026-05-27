# RIOS - Empacotamento Android

Esta pasta e uma copia de empacotamento. A versao HTML estavel original permanece intacta em:

`C:\Users\sandr\Documents\Codex\retorno-inteligente-os-v47-final-campo\www\index.html`

## O que esta preparado

- App Android nativo simples com WebView.
- Abertura em tela cheia, sem visual de Chrome.
- HTML validado copiado para `app/src/main/assets/www/index.html`.
- Armazenamento local do WebView habilitado.
- Seletor de arquivos habilitado para PDFs, imagens e multiplos arquivos quando o Android permitir.
- Permissoes basicas para camera e imagens.

## Como gerar o APK fora deste ambiente

Opcao 1, com Android Studio:

1. Abrir esta pasta como projeto Android:
   `C:\Users\sandr\Documents\Codex\RIOS_APK_PACKAGE`
2. Aguardar o Android Studio baixar o Gradle e o plugin Android.
3. Usar `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. O APK gerado ficara em:
   `app\build\outputs\apk\debug\app-debug.apk`

Opcao 2, sem Android Studio, via linha de comando:

1. Instalar JDK 17 ou superior.
2. Instalar Android SDK com plataforma Android 35.
3. Instalar Gradle.
4. Na pasta `C:\Users\sandr\Documents\Codex\RIOS_APK_PACKAGE`, executar:

```bat
gradle assembleDebug
```

O APK ficara em:

`C:\Users\sandr\Documents\Codex\RIOS_APK_PACKAGE\app\build\outputs\apk\debug\app-debug.apk`

## Observacao importante sobre uso offline

O HTML atual ainda referencia bibliotecas externas de PDF pela internet. Para o APK funcionar 100% offline, estas bibliotecas devem ser salvas dentro de `app/src/main/assets/www/vendor/` e os scripts do HTML devem apontar para esses arquivos locais:

- `pdf.min.js`
- `pdf.worker.min.js`
- `pdf-lib.min.js`

Sem isso, o app abre offline, mas as funcoes que dependem dessas bibliotecas podem falhar se o aparelho estiver sem internet.
