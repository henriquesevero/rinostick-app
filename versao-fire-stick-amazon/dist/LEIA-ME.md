# RinoStick (Fire TV) — pasta de distribuição

O que fica aqui e como instalar o RinoStick em qualquer Fire TV.

> **Importante:** o Fire OS atual (8.1.x) ignora conexões ADB vindas do próprio aparelho. Por isso as permissões **só podem ser liberadas por um computador** (Mac ou Windows). O assistente dentro do app explica isso e mostra o IP da TV.

## O que tem aqui

| Arquivo | Para que serve |
|---|---|
| `kit-de-instalacao/` | **O que vai para as pessoas**: o guia passo a passo `COMO-INSTALAR.md` (Mac e Windows), os instaladores `instalar-mac.command` e `instalar-windows.bat` e o app. Não tem a chave nem nada pessoal |
| `RinoStick-FireTV-kit.zip` | O kit zipado, gerado pelo `build.sh` (fora do git). É o arquivo anexado na release do GitHub |
| `RinoStick.apk` | O app pronto (fora do git) |
| `chave/firelauncher.keystore` | Chave de assinatura. **Fica fora do git**. **Não perca**: guarde uma cópia no iCloud ou num pendrive. Sem ela não dá para lançar atualizações. Senha: `android` |

Quando alguém roda o instalador, ele cria ao lado dele (fora do git):
- `platform-tools/`: as ferramentas ADB do Google, baixadas na primeira vez;
- `backups/<ip>-<data>/`: os valores originais, se a pessoa escolher remover anúncios.

## Instalar num Fire TV (o seu ou de outra pessoa)

Siga o `kit-de-instalacao/COMO-INSTALAR.md`. Resumo:

1. Na TV: ligar as Opções do desenvolvedor e a **Depuração ADB**, e anotar o IP.
2. No computador: baixar e extrair o `RinoStick-FireTV-kit.zip`, a partir do link da release:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-FireTV-kit.zip
   ```
3. Rodar o instalador:
   - **Mac:** `bash instalar-mac.command` (ou `bash instalar-mac.command 192.168.0.50`);
   - **Windows:** dois cliques em `instalar-windows.bat`.
4. Aceitar "Permitir depuração" na TV e responder se quer remover anúncios.

Os instaladores baixam sozinhos as ferramentas ADB do Google e o app (se o APK não estiver na pasta). Não precisam de Homebrew nem de nada instalado antes.

## Permissões que o app precisa

| Permissão | Para que serve | Sem ela… |
|---|---|---|
| **Serviço de acessibilidade "RinoStick — botão Home"** | O botão Home abre a tela nova em vez da tela da Amazon | O Home leva para a tela da Amazon; aparece um aviso laranja na tela nova |
| **Ajustes seguros** (`WRITE_SECURE_SETTINGS`) | Usar o protetor de tela "Aurora" e o "Restaurar original" | Não dá para trocar o protetor de tela |
| **Acesso às notificações** ("RinoStick — Tocando agora") | Mostrar a música do Spotify com capa e controles | O cartão "Tocando agora" não aparece |
| **Internet** | Clima | Já vem no app, não precisa liberar |

Comandos manuais, se quiser fazer à mão (troque o IP):
```
adb connect 192.168.0.50:5555
adb install -r RinoStick.apk
adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS
adb shell settings put secure enabled_accessibility_services com.rinostick.app/com.rinostick.app.HomeRedirectService
adb shell settings put secure accessibility_enabled 1
adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener
adb shell am start -n com.rinostick.app/.MainActivity
adb shell settings put secure screensaver_components com.rinostick.app/com.rinostick.app.Screensaver
```

O comando de acessibilidade acima substitui outros serviços já ligados. Os instaladores evitam isso e mantêm todos.

## Se resetar um Fire TV (ou comprar outro)

O reset de fábrica apaga o app, as permissões, a limpeza de anúncios e os ajustes (pastas, ordem, apps ocultos, cidade do clima). Para refazer, faça a configuração inicial do Fire TV e rode o instalador de novo. Depois, organize pastas e ordem de novo e, se quiser, escolha a cidade em engrenagem → Clima.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| "Não conectou" | Confira o IP, se a Depuração ADB está ligada e se o computador está no mesmo Wi-Fi. Desligue e ligue a depuração e rode de novo |
| Aviso "unauthorized" | O aviso "Permitir depuração" não foi aceito na TV. Rode de novo e aceite |
| O Home leva para a tela da Amazon | Rode o instalador de novo (religa a acessibilidade). Depois de atualizações do Fire OS, isso às vezes é necessário |
| "Tocando agora" não aparece | Rode o instalador de novo e abra o Spotify de novo |
| Anúncios ou apps da Amazon voltaram após uma atualização | Rode o instalador de novo e responda `s` na última pergunta |
| Erro de assinatura (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) | O aparelho tem uma versão assinada com outra chave. `adb uninstall com.rinostick.app` e rode o instalador de novo |

## Atualizar ou alterar o app

O código-fonte está em `~/Documents/2 - workspace/rinostick-app/versao-fire-stick-amazon` (GitHub `henriquesevero/rinostick-app`). O guia técnico é o `CLAUDE.md` dessa pasta. Para compilar em outro Mac, coloque a chave em `~/.android/firelauncher.keystore` e rode `./build.sh` (gera o APK e o zip do kit). É preciso ter Java 17 e o Android SDK (platform 30 e build-tools 34).
