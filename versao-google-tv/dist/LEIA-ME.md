# RinoStick para Google TV e Android TV

Pacote para instalar a tela inicial numa TV com Google TV ou Android TV.

> **Ainda não testado num aparelho real.** O código foi adaptado do RinoStick do Fire TV e compila, mas ninguém rodou numa TV Google ainda. Teste primeiro na sua TV antes de mandar para outras pessoas.

## O que tem aqui

| Arquivo | Para que serve |
|---|---|
| `RinoStick-GoogleTV.apk` | O app pronto |
| `instalar-sem-computador/` | **Para mandar a qualquer pessoa**: o app, o guia de instalação só pela TV (sem computador). É atualizado a cada compilação |
| `instalar.sh` | Instala e configura tudo com um comando, pelo Mac |
| `chave/firelauncher.keystore` | Chave de assinatura (a mesma da versão Fire TV). **Fica fora do git**. Não perca. Senha: `android` |

## Instalar pelo Mac

1. **Na TV:**
   - Configurações → Sistema → Sobre → clique **7 vezes** em "Build do Android TV OS".
   - Configurações → Sistema → Opções do desenvolvedor → ligue a **Depuração USB**.
   - Anote o IP em Configurações → Rede e Internet → (sua rede).
2. **No Mac**, na pasta deste pacote:
   ```
   ./instalar.sh 192.168.0.50
   ```
   Na primeira vez, a TV pergunta "Permitir depuração?": marque "Sempre permitir" e confirme.

**TV que só tem "Depuração sem fio"** (Google TV mais novos, como o Google TV Streamer): ligue a Depuração sem fio, escolha "Parear dispositivo com código" e rode no Mac:
```
adb pair <IP>:<porta do pareamento> <código>
./instalar.sh <IP>:<porta mostrada na tela da Depuração sem fio>
```

Se o Mac não tiver `adb`: `brew install android-platform-tools`.

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
adb shell settings put secure enabled_accessibility_services com.rinostick.app/com.rinostick.app.HomeRedirectService
adb shell settings put secure accessibility_enabled 1
adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS
adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener
adb shell am start -n com.rinostick.app/.MainActivity
```

**Alternativa no Android TV "puro"** (Mi Box, Sony antigas): ao apertar Home, o sistema pode perguntar qual tela inicial usar. Escolha RinoStick e "Sempre". Aí nem precisa da acessibilidade.

## Diferenças para a versão Fire TV

- O nome da saudação é escolhido em Ajustes → **Seu nome** (o Google não deixa apps lerem o nome da conta).
- Não há limpeza de anúncios ou de apps (era específica da Amazon).
- "Loja e busca da Amazon" virou **Google Play** e **Tela do Google** (libera a tela original por 10 minutos).
- O assistente sem computador funciona onde a TV aceita ADB na porta 5555 (Depuração USB). TVs só com "Depuração sem fio" precisam do Mac uma vez.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `instalar.sh` diz "Não conectou" | Confira o IP e se a Depuração USB está ligada. Desligue e ligue a depuração e rode de novo, aceitando o aviso na TV |
| O Home leva para a tela do Google | Rode o `instalar.sh` de novo. Se continuar, me mande o resultado de `adb shell dumpsys activity activities \| grep mResumedActivity` com a tela do Google aberta |
| "Tocando agora" não aparece | Rode o comando do "Tocando agora" acima e abra o Spotify de novo |
| Erro de assinatura (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) | `adb uninstall com.rinostick.app` e instale de novo |

## Atualizar ou alterar o app

O código-fonte está em `versao-google-tv/` no repositório (`~/Documents/2 - workspace/rinostick-app`, GitHub `henriquesevero/rinostick-app`). Guia técnico: `versao-google-tv/CLAUDE.md`. Compile com `./build.sh`.
