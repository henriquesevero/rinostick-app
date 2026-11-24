# RinoStick — guia técnico completo (para pessoas e para o Claude)

> Leia isto antes de mexer no projeto. Aqui estão: o que o app faz, como está organizado, como
> compilar e instalar, os limites do Fire OS que já foram testados e as preferências de design
> do dono do projeto. Idioma do usuário: **português do Brasil**.

---

## 1. O que é

Tela inicial (launcher) minimalista no estilo **Apple TV / tvOS** para **Fire TV Stick (Fire OS 8, Android 11 / API 30)**.

- Sem anúncios e sem as fileiras da Amazon: mostra só os apps instalados, numa grade de **6 por linha**.
- A primeira fileira fica dentro de um "dock" de vidro.
- Fundo com a cor desfocada do app em foco.
- Saudação com o nome da conta Amazon, relógio, data e clima.
- Cartão "Tocando agora" (Spotify etc.).
- Painel lateral único de Ajustes.
- Protetor de tela próprio ("Aurora").
- Sons suaves de navegação.

Pacote Java / ID do app: `com.rinostick.app` (mudou na v1.1; **não mudar de novo**: trocar o ID faz o Fire TV ver um app novo, sem atualizar o antigo) · Nome do app: **RinoStick** (já se chamou "Início" e "RinoFire") · Repositório: `~/Documents/2 - workspace/rinostick-app` → GitHub `henriquesevero/rinostick-app`. **Esta é a pasta `versao-fire-stick-amazon/`**; existe uma cópia adaptada em `versao-google-tv/` (veja o `CLAUDE.md` da raiz).

**Fora do git (`.gitignore`):** `build/`, todos os `*.apk`, `dist/chave/` (chave de assinatura), `backup/` e `dist/backups/` (valores de aparelhos). Para distribuir o APK, use os *Releases* do GitHub ou o Google Drive.

---

## 2. Como substitui a tela da Amazon (importante)

O Fire OS **não deixa** trocar o launcher padrão: o HOME sempre abre `com.amazon.tv.launcher`. A solução:

- `HomeRedirectService` é um **AccessibilityService** que escuta `TYPE_WINDOW_STATE_CHANGED` só do pacote `com.amazon.tv.launcher`.
- Quando a classe contém `HomeActivity`, ele abre a nossa `MainActivity` (flags `NEW_TASK | REORDER_TO_FRONT | NO_ANIMATION`).
- `HomeRedirectService.pauseUntil` desliga o redirecionamento temporariamente. É usado por "Loja e busca da Amazon", que libera a tela da Amazon por 10 minutos, e é zerado em `MainActivity.onResume`.
- O serviço é habilitado via ADB, sem passar pela tela de Acessibilidade:
  ```
  adb shell settings put secure enabled_accessibility_services com.rinostick.app/com.rinostick.app.HomeRedirectService
  adb shell settings put secure accessibility_enabled 1
  ```

---

## 3. Permissões concedidas via ADB (não dá pelo app)

| O quê | Comando | Para quê |
|---|---|---|
| `WRITE_SECURE_SETTINGS` | `adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS` | Trocar o protetor de tela; desfazer a acessibilidade no "Restaurar original" |
| Acessibilidade | ver seção 2 | Botão Home → tela nova |
| Ouvinte de notificações | `adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener` | Ler sessões de mídia ("Tocando agora") |

O `build.sh install` e os instaladores do kit (`dist/kit-de-instalacao/instalar-mac.command` e `instalar-windows.bat`) já fazem tudo isso.

**O Fire OS 8.1.x bloqueia a configuração pelo próprio aparelho** (testado em 24/11/2026 nos dois Fire TV do dono, Fire OS 8.1.8.5): o adbd aceita conexões TCP vindas do próprio aparelho (127.0.0.1 ou o próprio IP), mas não responde nada e nem registra no log; o app recebe "broken pipe". Conexões de **outro** aparelho da rede funcionam normalmente. Por isso:
- a instalação de outras pessoas é **sempre por computador** (Mac ou Windows), com o kit (`dist/kit-de-instalacao/`, guia `COMO-INSTALAR.md`);
- o assistente `Setup` só explica isso e mostra o IP da TV (`Device.localIp()`); a opção "Tentar configurar aqui mesmo" mantém o `AdbSelf` para Fire OS antigos;
- ideia aceita para o futuro, se pedirem: um "configurador" num celular Android (o mesmo `AdbSelf`, apontando para o IP da TV). Não funciona para quem só tem iPhone.

---

## 4. Estrutura do código (`src/com/rinostick/app/`)

| Arquivo | Responsabilidade |
|---|---|
| `MainActivity.java` | A tela inicial (ver detalhes abaixo) |
| `SettingsActivity.java` | **Painel único de Ajustes**, à esquerda (detalhes abaixo). Também contém `settingsIcon()` |
| `Ui.java` | Fontes Inter, escala `px()` (base 1920 de largura), preferências (`prefs "launcher"`), aparência claro/escuro/auto (escuro das 18h às 6h), paleta, `SoundPool` (só `tick.wav`), diálogos `menu()` (itens com `!` saem em vermelho) e `prompt()` |
| `Glyph.java` | Ícones desenhados em código. A lista está abaixo da tabela. `kindOverride()` troca o ícone em tempo real |
| `ShelfView.java` | Fundo: cor base + imagem desfocada do app em foco, com cross-fade. Renderiza num bitmap 192×108 ampliado e só redesenha quando algo muda |
| `Images.java` | Cor dominante, `backdrop()` (desfoque 64×36), `boxBlur` |
| `NowPlayingView.java` | Cartão "Tocando agora" (detalhes abaixo) |
| `NowPlayingListener.java` | `NotificationListenerService` vazio; só precisa existir e estar habilitado |
| `Weather.java` | Clima (detalhes abaixo) |
| `Device.java` | Ações de sistema (detalhes abaixo) |
| `HomeRedirectService.java` | Redirecionamento do HOME (seção 2) |
| `AdbSelf.java` | Cliente ADB mínimo que conecta no adbd do **próprio** aparelho (127.0.0.1:5555, chave RSA própria em `files/adbkey`). No Fire OS 8.1.x atual **não funciona** (veja a seção 3); fica para Fire OS antigos |
| `Setup.java` | Assistente "Configurar o RinoStick": via `AdbSelf`, faz o `pm grant`, liga a acessibilidade e o ouvinte de notificações e, opcionalmente, a limpeza (backup em prefs `cleanBackup`/`cleanDisabled`). Abre sozinho se faltar permissão; também fica em Ajustes → Fire TV |
| `Screensaver.java` + `AuroraView.java` | Protetor de tela `DreamService` "Aurora": aurora animada em baixa resolução + relógio |

Ícones disponíveis em `Glyph.java`: GEAR, POWER, APPEARANCE, SPEAKER, WIFI, BAG, TV, EYE, SORT, RESTORE, CLOSE, PLAY, PAUSE, NEXT, XMARK, SUN, MOON, CLOUD, PARTLY, RAIN, STORM, FOG.

**`MainActivity.java`**
- Grade `COLS = 6`, com o card do app = banner da TV (ou gradiente + ícone).
- Pastas guardadas em JSON em `prefs "folders"`; ordem em `"order"`; ocultos em `"hidden"`. `DEFAULT_HIDDEN` inclui Luna (`spiderpork`), Amazon Music (`bueller.music`) e outros apps da Amazon. `DEFAULT_ORDER`: Apple TV, Netflix, Firebat, YouTube, Spotify.
- Foco: escala 1.07 + leve inclinação + brilho. Rolagem própria (`GridScroll`).
- Fileiras: a tela inicial mostra só o dock + a 2ª fileira; a 3ª em diante fica apagada (alpha 0.25) como dica. Ao focar a 3ª fileira ou abaixo, entra no modo "só apps" (`setAppsOnly`): o topo (`header`), o fundo colorido e o dock somem, e a grade sobe. Voltar à 1ª/2ª fileira desfaz tudo.
- Segurar OK abre o modo mover (ícones tremendo); ☰ ou ⏯ abre as opções do item (nova pasta, ocultar, informações do app).
- Topo:
  - Esquerda: engrenagem + saudação "Bom dia/Boa tarde/Boa noite, <nome>".
  - Centro: `NowPlayingView`.
  - Direita: relógio, data e clima.
- Engrenagem ou ☰ → `openSettings()`.

**`SettingsActivity.java`**
- Activity translúcida (`OverlayTheme`); fecha com Voltar ou ☰.
- Cabeçalho: hora grande + data. A ordem das opções:
  1. Aparência · Sons de navegação · Protetor de tela · Clima
  2. **Apps:** Apps ocultos · Restaurar ordem · Loja e busca da Amazon
  3. **Fire TV:** Rede · Configurações do Fire TV · Restaurar original (vermelho, com confirmação)
  4. **Dormir**, sempre a última opção.

**`NowPlayingView.java`**
- Capa, música, artista e os botões ⏯ ⏭ ✕, via `MediaSessionManager.getActiveSessions(NowPlayingListener)`.
- Ignora sessões sem título (ex.: YouTube).
- **Não some sozinho** quando pausado (o usuário pediu). Só some no ✕ ou quando o app de música fecha a sessão.
- O ✕ esconde o cartão até a próxima música, ou até voltar a tocar se estava pausado.
- Só escuta enquanto a tela inicial está aberta.

**`Weather.java`**
- Clima pelo Open-Meteo (sem chave), atualizado a cada 30 minutos em uma thread.
- Local automático pelo ipwho.is ou cidade escolhida em Ajustes (geocoding do Open-Meteo).
- Dados guardados em prefs (`wTemp`, `wCode`, `wDay`, `wLat`, `wLon`, `wCity`, `wManual`, `wOn`, `wTs`).

**`Device.java`**
- Protetor de tela: `initScreensaver`, `setOurScreensaver`.
- Atalhos para Rede e Configurações; `openAmazonHome`; `appInfo`; `sleep`.
- `firstName()`: nome da conta `com.amazon.account`, com o nome do aparelho como alternativa.
- `restoreOriginal()`: remove o serviço de acessibilidade, devolve o protetor da Amazon e pede a desinstalação. `undoRestoreIfNeeded()` reativa tudo se o usuário cancelar.

**Outros arquivos**
- `AndroidManifest.xml`:
  - Permissões: `WRITE_SECURE_SETTINGS`, `REQUEST_DELETE_PACKAGES`, `INTERNET`.
  - `<queries>` para LEANBACK_LAUNCHER, LAUNCHER e HOME.
  - Serviços: Screensaver, NowPlayingListener, HomeRedirectService.
- `tools/make_sfx.py`: gera o `res/raw/tick.wav` (estalo de madeira sintetizado).
- `res/`: `styles.xml` (AppTheme, OverlayTheme), `xml/accessibility.xml`, `raw/tick.wav` (o único som), `drawable-xxxhdpi/rinostick_icon.png` (192 px) e `drawable-xhdpi/rinostick_banner.png` (640×360; também declarado na `MainActivity`). Os nomes mudaram de `icon`/`banner` porque a tela da Amazon guardava o banner antigo em cache pelo nome do recurso, mesmo após reiniciar e limpar caches, gerados por `tools/make_icons.swift` (rodar `swift tools/make_icons.swift` na pasta da versão).
- `tools/make_icons.swift`: desenha o ícone e o banner (`swift tools/make_icons.swift` na pasta da versão; `MOCK=/caminho/quadro` gera simulações do quadro da Amazon). **Visual aprovado (v1.4.5): tema `apple`**, claro e monocromático — só o nome, "Rino" em #1D1D1F e "Stick" em #86868B, dentro de uma cápsula de vidro fosco branco (estilo Liquid Glass). Cores em sRGB exato (`CGColor(srgbRed:)`).
  - **Banner** (640×360): fundo de alumínio claro + cápsula com o nome numa linha. Usado pelo Google TV / Android TV.
  - **Ícone no Fire TV** (`iconFire`): fundo **#5A5A5A** sem cantos + cápsula com "RinoStick" numa linha. Motivo: a tela da Amazon **ignora o banner** de apps instalados fora da loja e desenha o **ícone do app** espremido num quadrado no meio de um quadro cinza #5A5A5A (medido pelos bytes da captura; ferramentas que convertem cor dão valor errado). Ícone retangular fica achatado; só apps da Appstore preenchem o quadro (a imagem vem da loja).
  - **Ícone no Google TV** (`icon`): fundo claro arredondado, "Rino"/"Stick" em duas linhas.
  - **Cache da Amazon:** a tela da Amazon grava a imagem do app **na instalação** e não atualiza com `install -r`, reinício, `pm trim-caches` nem troca de nome do recurso. Só desinstalar + instalar atualiza. Para não perder os ajustes do usuário: instalar por cima uma build com `android:debuggable="true"` (temporária, nunca commitada), copiar `shared_prefs/launcher.xml` com `adb exec-out run-as com.rinostick.app cat ...`, desinstalar, instalar a build debug, devolver com `adb exec-in run-as ... sh -c 'cat > shared_prefs/launcher.xml'`, instalar a build normal por cima e liberar as permissões de novo.
  - Rejeitados: rinoceronte desenhado (simples e detalhado), temas `knicks` (azul/laranja) e `goldengate` (guardados no script), versões coloridas.
- `assets/fonts/`: Inter (Light, Regular, Medium, SemiBold, Bold), licença OFL. É o substituto do San Francisco.

---

## 5. Compilar (sem Gradle)

Requisitos no Mac:
- Java 17: `brew install openjdk@17`
- Android SDK em `~/Library/Android/sdk`, com `platforms;android-30` e `build-tools;34.0.0` (via `brew install android-commandlinetools` + `sdkmanager`)
- `adb`: `brew install android-platform-tools`
- Chave em `~/.android/firelauncher.keystore` (cópia em `dist/chave/`; alias `launcher`; senha `android`)

```
cd ~/Documents/2 - workspace/rinostick-app/versao-fire-stick-amazon
./build.sh            # só compila  -> build/RinoStick.apk, dist/kit-de-instalacao/ e dist/RinoStick-FireTV-kit.zip
./build.sh install    # compila, instala no aparelho conectado, concede permissões e abre o app
```

Por dentro, a sequência é: aapt2 compile/link (com `-A assets`) → `javac --release 8 -classpath android.jar` → `d8 --min-api 25` → zipalign → apksigner.

**Sempre use a mesma chave.** Com outra chave, o Android recusa a atualização e obriga a desinstalar, perdendo pastas, ordem e ajustes.

---

## 6. Instalar num Fire TV novo

1. No Fire TV, ative as opções de desenvolvedor: Configurações → Minha Fire TV → Sobre → clique 7× no nome do aparelho. Depois ligue Opções do desenvolvedor → **Depuração ADB** e anote o IP em Sobre → Rede.
2. No computador: rodar o instalador do kit (`bash dist/kit-de-instalacao/instalar-mac.command <IP>` no Mac; `instalar-windows.bat` no Windows). Aceite "Permitir depuração" na TV. Passo a passo para pessoas comuns: `dist/kit-de-instalacao/COMO-INSTALAR.md`.
3. No fim, o instalador pergunta se quer tirar anúncios, coleta e apps sem uso (resposta `s`). Os valores originais ficam em `backups/<ip>-<data>/`, ao lado do instalador.

O que a limpeza faz:
- Settings secure:
  - `advertisingIdApp:interestBasedAds:value` → 0
  - `amz_limit_ad_tracking` → 1
  - `limit_ad_tracking` → 1
  - `USAGE_METRICS_UPLOAD_ENABLED` → 0
  - `pact_usage_collection_enabled` → 0
- `pm disable-user --user 0` em: `com.amazon.spiderpork`, `bueller.music`, `shoptv.firetv.client`, `smarthomemapviewapp`, `bueller.photos`, `tv.earlyaccess`, `minitv.android.app`, `gamehub`, `tahoe`, `whasettings`, `tv.releasenotes`, `tv.ftvambient`.
- **Não mexe** em atualizações (OTA), Alexa, Silk ou espelhamento de tela. O usuário aprovou só o grupo "apps que não usa".

Para desfazer:
- Apps: `adb shell pm enable <pacote>`
- Ajustes: `adb shell settings put secure <chave> <valor original>`

O aparelho original (o primeiro) tem os backups em `backup/` (fora do git).

---

## 7. Limites do Fire OS já testados (não tente de novo)

| Ideia | Resultado |
|---|---|
| Ser o launcher padrão de verdade | Bloqueado → usa o redirecionamento por acessibilidade |
| Busca e loja da Amazon dentro do nosso app | Protegidas por assinatura. Solução: "Loja e busca da Amazon" libera a tela da Amazon por 10 min |
| Interceptar a tecla HOME na acessibilidade | O Fire OS não entrega a tecla HOME |
| Encerrar apps em segundo plano | `killBackgroundProcesses` é recusado para apps de terceiros. Removido |
| Limpar cache de outros apps | Exige permissão de sistema. Só pelo ADB (`pm trim-caches`). Total de cache era ~54 MB, sem ganho real |
| Prévias de séries/filmes (Top Shelf) | `TvProvider` exige `ACCESS_ALL_EPG_DATA` (sistema), e os apps não publicam recomendações em notificações |
| Desfoque real de janela (blur) | Não existe na API 30 → painéis sólidos/semitransparentes |
| "Tocando agora" com YouTube / YouTube Music | O app do YouTube para TV (no Fire, `com.amazon.firetv.youtube`; YouTube Music toca dentro dele) **para a música e apaga título/artista da sessão ao sair do app** (testado em 24/11/2026: título some ~2 s após o Home, velocidade 0, sessão inativa). O cartão aparece e some porque a música de fato para. Não tem conserto no RinoStick; para música em segundo plano: Spotify, Amazon Music, Deezer, Tidal |
| Pôster animado do app no fundo | Feito e **rejeitado pelo usuário duas vezes**, nas versões grande e pequena. Também pesava (17 ms/quadro). Não refazer |

---

## 8. Preferências de design do usuário (respeitar)

- **Fidelidade ao Apple TV**, minimalista, limpo e "profissional".
- **Sem nomes** de apps abaixo ou acima dos ícones; nada de texto ao focar.
- Ícones **padronizados**, com foco discreto (escala 1.07).
- Grade de 6 por linha; a tela inicial mostra **2 fileiras**; descendo, o topo some e fica só a grade (estilo tvOS). Carrossel foi testado e rejeitado.
- Topo:
  - Engrenagem discreta à **esquerda**, com a saudação ao lado.
  - Relógio à **direita**, com a data e o clima abaixo.
  - "Tocando agora" no **centro**.
- Primeira fileira dentro do **dock de vidro**; as demais sem borda.
- **Painel único** de Ajustes à **esquerda**, deslizando da esquerda e **sólido** (nada vazando por trás), sem funções repetidas; **Dormir é a última opção**.
- Sons: **um só som** (estalo de madeira suave, `tick.wav`) para tudo. Sons "8-bit" foram rejeitados.
- Fundo: só a cor desfocada do app em foco (sem pôster/imagem animada).
- Desempenho é prioridade: nada pode travar. Nenhuma animação contínua na tela inicial. Meça antes de entregar:
  ```
  adb shell dumpsys gfxinfo com.rinostick.app reset
  # ... navegar ...
  adb shell dumpsys gfxinfo com.rinostick.app
  ```
  Referência atual: tela parada = 0 quadros; navegação ~6 ms/quadro; ~56 MB de memória.
- Antes de desativar apps ou mudar ajustes do sistema: **mostrar a lista e pedir aprovação**.
- Comunicação: português, explicações simples e honestas (dizer o que não foi testado).

---

## 9. Dicas de trabalho com o aparelho

- Captura de tela: `adb exec-out screencap -p > tela.png`
- O usuário costuma estar **usando a TV**. Evite apertar teclas com `adb shell input keyevent`. Se precisar, confira antes se a tela inicial está na frente:
  ```
  adb shell dumpsys activity activities | grep mResumedActivity
  ```
- Sessões de mídia: `adb shell dumpsys media_session`
- Logs do app aparecem só a partir do nível I (Log.d não aparece).
- A `SettingsActivity` é `exported=false`: abra pelo app (tecla ☰), não por `am start`.
- Atualizações do Fire OS podem religar anúncios, coleta e apps desativados. Confira após cada atualização.

---

## 10. Pendências / ideias aceitas para o futuro

- Ainda não testado: o botão **Dormir** (`GLOBAL_ACTION_LOCK_SCREEN`) e o comportamento após **reiniciar** o aparelho.
- Ideias sugeridas e ainda não feitas:
  - Capa do álbum desfocada no fundo enquanto toca música.
  - Protetor de tela mostrando a música.
  - Cards melhores para apps sem banner de TV.
