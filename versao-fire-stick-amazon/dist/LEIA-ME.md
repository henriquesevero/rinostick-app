# RinoStick — tela inicial minimalista para Fire TV

Pacote para instalar a tela inicial em outro Fire TV Stick.

## O que tem aqui

| Arquivo | Para que serve |
|---|---|
| `RinoStick.apk` | O app pronto |
| `instalar-sem-computador/` | **Para mandar a qualquer pessoa**: o app, o guia de instalação só pelo Fire TV (sem computador). Não tem a chave nem nada pessoal. É atualizado a cada compilação |
| `instalar.sh` | Instala e configura tudo com um comando |
| `chave/firelauncher.keystore` | Chave de assinatura. **Fica fora do git (não vai para o GitHub)**. **Não perca**: guarde uma cópia no iCloud ou num pendrive. Sem ela não dá para lançar atualizações do app. Senha: `android` |
| `backups/` | Criada pelo `--limpar`: valores originais de cada aparelho limpo (fora do git) |

## Instalar num Fire TV novo

1. **No Fire TV:**
   - Ative as opções de desenvolvedor: Configurações → Minha Fire TV → Sobre → clique **7 vezes** no nome do aparelho.
   - Ligue Configurações → Minha Fire TV → Opções do desenvolvedor → **Depuração ADB**.
   - Anote o IP em Minha Fire TV → Sobre → **Rede**.
2. **No Mac**, na pasta deste pacote, rode:
   ```
   ./instalar.sh 192.168.0.50
   ```
   Troque pelo IP do aparelho. Na primeira vez, a TV pergunta "Permitir depuração?": marque "Sempre permitir" e confirme.
3. Para também desligar anúncios, coleta de dados e os apps da Amazon sem uso (Luna, Amazon Music, Fotos, Jogos etc.):
   ```
   ./instalar.sh 192.168.0.50 --limpar
   ```

Se o Mac não tiver `adb`: `brew install android-platform-tools`.

## Permissões que o app precisa

O `instalar.sh` libera todas elas sozinho. Se algo falhar, ou se quiser fazer à mão, os comandos estão abaixo. Todos rodam no Mac com o Fire TV conectado.

| Permissão | Para que serve | Sem ela… |
|---|---|---|
| **Serviço de acessibilidade "RinoStick — botão Home"** | O botão Home abre a tela nova em vez da tela da Amazon | O Home leva para a tela da Amazon; aparece um aviso laranja na tela nova |
| **Ajustes seguros** (`WRITE_SECURE_SETTINGS`) | Usar o protetor de tela "Aurora" e o "Restaurar original" | Não dá para trocar o protetor de tela |
| **Acesso às notificações** ("RinoStick — Tocando agora") | Mostrar a música do Spotify com capa e controles | O cartão "Tocando agora" não aparece |
| **Internet** | Clima | Já vem no app, não precisa liberar |

Comandos manuais (troque o IP):
```
adb connect 192.168.0.50:5555
adb install -r RinoStick.apk

# Botão Home
adb shell settings put secure enabled_accessibility_services com.rinostick.app/com.rinostick.app.HomeRedirectService
adb shell settings put secure accessibility_enabled 1

# Ajustes seguros (protetor de tela / restaurar)
adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS

# Tocando agora
adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener

# Abrir
adb shell am start -n com.rinostick.app/.MainActivity
```

Se o aparelho já tiver outro serviço de acessibilidade ligado, o primeiro comando do botão Home o substitui. O `instalar.sh` evita isso e mantém os dois.

## Se resetar este Fire TV (ou comprar outro)

O reset de fábrica apaga o app, as permissões, a limpeza de anúncios e os ajustes (pastas, ordem, apps ocultos, cidade do clima). Para refazer:

1. Faça a configuração inicial do Fire TV e entre na conta Amazon.
2. Ative a Depuração ADB (passo 1 de "Instalar num Fire TV novo") e anote o IP, que pode ter mudado.
3. No Mac: `./instalar.sh <IP> --limpar`
4. Na TV: organize pastas e ordem de novo (segure OK num ícone) e, se quiser, escolha a cidade em engrenagem → Clima.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `instalar.sh` diz "Não conectou" | Confira o IP e se a Depuração ADB está ligada. Desligue e ligue a depuração e rode de novo, aceitando o aviso na TV |
| Aviso "unauthorized" | O aviso "Permitir depuração" não foi aceito na TV. Rode de novo e aceite |
| O Home leva para a tela da Amazon | Rode o `instalar.sh` de novo (religa a acessibilidade). Depois de atualizações do Fire OS, isso às vezes é necessário |
| "Tocando agora" não aparece | Rode o comando do "Tocando agora" acima e abra o Spotify de novo |
| Anúncios ou apps da Amazon voltaram após uma atualização | Rode `./instalar.sh <IP> --limpar` de novo |
| Erro de assinatura ao instalar (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) | O aparelho tem uma versão assinada com outra chave. Desinstale: `adb uninstall com.rinostick.app`, depois instale de novo |

## Depois de instalar

- O nome na saudação vem da conta Amazon logada.
- A cidade do clima é detectada pela internet. Se vier errada, troque em engrenagem → Clima → Escolher cidade.
- Para voltar ao original: engrenagem → Restaurar original.

## Atualizar ou alterar o app

O código-fonte completo está no repositório (`~/Documents/2 - workspace/rinostick-app/versao-fire-stick-amazon`, GitHub `henriquesevero/rinostick-app`): `src/`, `res/`, `assets/` e `build.sh`. O guia técnico completo é o `CLAUDE.md` dessa pasta. Para compilar em outro Mac, coloque a chave em `~/.android/firelauncher.keystore` e rode `./build.sh`. É preciso ter Java 17 e o Android SDK (platform 30 e build-tools 34).
