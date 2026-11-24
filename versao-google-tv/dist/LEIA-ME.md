# RinoStick (Google TV / Android TV) — pasta de distribuição

O que fica aqui e como instalar o RinoStick numa TV com Google TV ou Android TV.

> **Testado no emulador oficial de Google TV (Android 14)**: instalação, assistente sem computador, botão Home, protetor de tela e ajustes funcionaram. Ainda não foi testado numa TV física; cada marca pode ter pequenas diferenças.

## O que tem aqui

| Arquivo | Para que serve |
|---|---|
| `kit-de-instalacao/` | **O que vai para as pessoas**: o guia passo a passo `COMO-INSTALAR.md` (só pela TV, ou pelo Mac e Windows), os instaladores `instalar-mac.command` e `instalar-windows.bat` e o app. Não tem a chave nem nada pessoal |
| `RinoStick-GoogleTV-kit.zip` | O kit zipado, gerado pelo `build.sh` (fora do git). É o arquivo anexado na release do GitHub |
| `RinoStick-GoogleTV.apk` | O app pronto (fora do git) |
| `chave/firelauncher.keystore` | Chave de assinatura (a mesma da versão Fire TV). **Fica fora do git**. Não perca. Senha: `android` |

## Instalar numa TV Google

Siga o `kit-de-instalacao/COMO-INSTALAR.md`. Há dois caminhos:

- **A — só pela TV:** Downloader + link do APK + assistente "Configurar o RinoStick". O Google TV aceita a conexão ADB vinda do próprio aparelho, ao contrário do Fire TV. Funciona onde a TV oferece a **Depuração USB** (ADB na porta 5555).
- **B — pelo computador:** baixar e extrair o kit, a partir do link da release:
  ```
  https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV-kit.zip
  ```
  - **Mac:** `bash instalar-mac.command` (ou `bash instalar-mac.command 192.168.0.50`);
  - **Windows:** dois cliques em `instalar-windows.bat`.

**TV que só tem "Depuração sem fio"** (Google TV mais novos, como o Google TV Streamer): o caminho A não funciona. No caminho B, pareie uma vez com `adb pair <IP>:<porta do pareamento> <código>` (o `adb` fica na pasta `platform-tools` criada pelo instalador) e rode o instalador digitando o `IP:porta` mostrado na tela da Depuração sem fio.

**Alternativa no Android TV "puro"** (Mi Box, Sony antigas): ao apertar Home, o sistema pode perguntar qual tela inicial usar. Escolha RinoStick e "Sempre". Aí nem precisa da acessibilidade.

## Permissões que o app precisa

| Permissão | Para que serve | Sem ela… |
|---|---|---|
| **Serviço de acessibilidade "RinoStick — botão Home"** | O botão Home abre a tela nova em vez da tela do Google | O Home leva para a tela do Google |
| **Ajustes seguros** (`WRITE_SECURE_SETTINGS`) | Usar o protetor de tela "Aurora" e o "Restaurar original" | Não dá para trocar o protetor de tela |
| **Acesso às notificações** ("RinoStick — Tocando agora") | Mostrar a música do Spotify com capa e controles | O cartão "Tocando agora" não aparece |

Comandos manuais (troque o IP):
```
adb connect 192.168.0.50:5555
adb install -r RinoStick-GoogleTV.apk
adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS
adb shell settings put secure enabled_accessibility_services com.rinostick.app/com.rinostick.app.HomeRedirectService
adb shell settings put secure accessibility_enabled 1
adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener
adb shell am start -n com.rinostick.app/.MainActivity
adb shell settings put secure screensaver_components com.rinostick.app/com.rinostick.app.Screensaver
```

## Diferenças para a versão Fire TV

- O nome da saudação é escolhido em Ajustes → **Seu nome** (o Google não deixa apps lerem o nome da conta).
- Não há limpeza de anúncios ou de apps (era específica da Amazon).
- "Loja e busca da Amazon" virou **Google Play** e **Tela do Google** (libera a tela original por 10 minutos).
- Segurar OK num app abre as opções (Mover, pasta, ocultar), porque o controle do Google TV não tem ☰.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| Assistente da TV: "Não deu certo" | Use o caminho B (computador) |
| Instalador: "Não conectou" | Confira o IP, se a Depuração USB está ligada e se o computador está no mesmo Wi-Fi |
| O Home leva para a tela do Google | Configure de novo. Se continuar, me mande o resultado de `adb shell dumpsys activity activities \| grep mResumedActivity` com a tela do Google aberta |
| "Tocando agora" não aparece | Configure de novo e abra o Spotify de novo |
| Erro de assinatura (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) | `adb uninstall com.rinostick.app` e instale de novo |

## Atualizar ou alterar o app

O código-fonte está em `versao-google-tv/` no repositório (`~/Documents/2 - workspace/rinostick-app`, GitHub `henriquesevero/rinostick-app`). Guia técnico: `versao-google-tv/CLAUDE.md`. Compile com `./build.sh` (gera o APK e o zip do kit).
