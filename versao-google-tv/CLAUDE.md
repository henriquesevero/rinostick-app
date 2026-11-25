# RinoStick — guia técnico completo (para pessoas e para o Claude)

> **Versão Google TV / Android TV** (pasta `versao-google-tv/`). É uma cópia da versão Fire TV
> (`versao-fire-stick-amazon/`) adaptada. **Testada no emulador oficial de Google TV (Android 14)**, ainda não numa TV física (o dono não tem TV Google).
> As diferenças estão na seção 0; o resto do guia vale para as duas versões.
> Idioma do usuário: **português do Brasil**.

---

## 0. Diferenças para a versão Fire TV

| Parte | Fire TV | Google TV (esta pasta) |
|---|---|---|
| APK | `RinoStick.apk` | `RinoStick-GoogleTV.apk` (mesmo ID `com.rinostick.app`, mesma chave) |
| `HomeRedirectService` | Só `com.amazon.tv.launcher` / `HomeActivity` | Descobre as telas iniciais do sistema (`queryIntentActivities(CATEGORY_HOME)`, menos a nossa e as de Ajustes) + conhecidas: `com.google.android.apps.tv.launcherx` (Google TV), `com.google.android.tvlauncher` e `leanbacklauncher` (Android TV). `systemLauncher()` devolve a original |
| `res/xml/accessibility.xml` | `packageNames` = launcher da Amazon | Sem `packageNames` (o serviço filtra sozinho) |
| Saudação | Nome da conta Amazon | Pref `userName` (Ajustes → **Seu nome**) ou nome do aparelho ("TV de Maria"). O Google não expõe o nome da conta |
| Ajustes → Apps | "Loja e busca da Amazon" | **Google Play** (`openPlayStore`) e **Tela do Google** (`openSystemHome`, pausa o redirecionamento por 10 min) |
| Ajustes → seção | "Fire TV" / "Configurações do Fire TV" | "TV" / "Configurações da TV" (`openTvSettings`) |
| `Setup` | Configurar / Configurar e remover anúncios | Só "Configurar agora"; sem limpeza. Textos com o caminho do Google TV (Sistema → Opções do desenvolvedor → Depuração USB) |
| `AdbSelf` | 127.0.0.1:5555 | Igual. Funciona onde o adbd escuta na 5555 (Chromecast com Google TV, maioria dos Android TV). TVs só com "Depuração sem fio" (pareamento TLS) **não** funcionam pelo assistente: use o instalador do kit com `IP:PORTA` depois de `adb pair` |
| `DEFAULT_HIDDEN` / `DEFAULT_ORDER` | Apps da Amazon ocultos; Apple TV, Netflix, Firebat… | Nada oculto; Apple TV (`com.apple.atve.androidtv.appletv`), Netflix, Prime Video, YouTube (`com.google.android.youtube.tv`), Spotify |
| Instaladores do kit (Mac/Windows) | Perguntam se quer remover anúncios | Sem limpeza; aceitam `IP:porta` (Depuração sem fio) |
| Segurar OK num app | Entra direto no modo mover; ☰/⏯ abre as opções | Abre as opções com **"Mover"** primeiro (`longPressOptions`): o controle do Google TV não tem ☰ |
| Aviso de configuração pendente | "aperte ☰" | "vá na engrenagem" |

**Ao mudar algo comum às duas versões** (grade, cartão de música, clima, visual), faça a mesma mudança nas duas pastas.

**Testado no emulador de Google TV 14** (já apagado do Mac, a pedido do usuário; para recriar: `sdkmanager "emulator" "system-images;android-34;google-tv;arm64-v8a"`, criar o AVD com `hw.dPad=yes`, `hw.lcd.width=1920`, `hw.lcd.height=1080` e, depois de ligar, `adb -s emulator-5554 tcpip 5555` para simular a Depuração USB): assistente sem computador (aviso "Permitir depuração" → 3 permissões → "Tudo pronto"), botão Home (launcherx `.home.HomeActivity` → RinoStick), protetor Aurora, painel de Ajustes, segurar OK, Tela do Google (pausa funciona).

**Numa TV física, confira também:** o botão Home redireciona (se não, veja a classe com `adb shell dumpsys activity activities | grep mResumedActivity` e ajuste `onAccessibilityEvent`); o protetor de tela troca (`settings get secure screensaver_components`); o assistente conecta na 5555; nenhum app de sistema estranho aparece na grade (ocultar ou filtrar em `loadAll`).

---

## 1. O que é

Tela inicial (launcher) minimalista no estilo **Apple TV / tvOS** para **Google TV e Android TV** (compilada para API 30, roda a partir do Android 7.1 / API 25).

- Sem as fileiras de recomendações: mostra só os apps instalados, numa grade de **5 por linha** (ícones de 308 px, como no Apple TV; até a v1.5 eram 6 de 256 px).
- A primeira fileira fica dentro de um "dock" de vidro.
- Fundo com a cor desfocada do app em foco.
- Saudação com o nome escolhido em Ajustes, relógio, data e clima.
- Cartão "Tocando agora" (Spotify etc.).
- Painel lateral único de Ajustes.
- Protetor de tela próprio ("Aurora").
- Sons suaves de navegação.

Pacote Java / ID do app: `com.rinostick.app` (mudou na v1.1; **não mudar de novo**: trocar o ID faz o Fire TV ver um app novo, sem atualizar o antigo) · Nome do app: **RinoStick** (já se chamou "Início" e "RinoFire") · Repositório: `~/Documents/2 - workspace/rinostick-app` → GitHub `henriquesevero/rinostick-app`, pasta `versao-google-tv/`.

**Fora do git (`.gitignore`):** `build/`, todos os `*.apk`, `dist/chave/` (chave de assinatura), `backup/` e `dist/backups/` (valores de aparelhos). Para distribuir o APK, use os *Releases* do GitHub ou o Google Drive.

---

## 2. Como substitui a tela do Google (importante)

O Google TV **não deixa** trocar a tela inicial pelas configurações. A solução é a mesma do Fire TV:

- `HomeRedirectService` (AccessibilityService) escuta `TYPE_WINDOW_STATE_CHANGED`. Quando a janela da frente é a atividade principal de uma tela inicial do sistema (lista na seção 0), abre a nossa `MainActivity` (flags `NEW_TASK | REORDER_TO_FRONT | NO_ANIMATION`).
- `HomeRedirectService.pauseUntil` desliga o redirecionamento temporariamente ("Tela do Google", 10 min) e é zerado em `MainActivity.onResume`.
- Em Android TV "puro", o `MainActivity` também tem o filtro `HOME`: o usuário pode escolher o RinoStick como tela inicial padrão e o serviço nem age.
- Habilitar via ADB:
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

O `build.sh install` e os instaladores do kit (`dist/kit-de-instalacao/instalar-mac.command` e `instalar-windows.bat`) já fazem tudo isso. **Sem computador:** o assistente do app (`Setup`) faz o mesmo pelo ADB do próprio aparelho; o usuário liga a Depuração USB e aceita "Permitir depuração" na TV. Funcionou no emulador de Google TV. (No **Fire TV** isso é bloqueado pelo Fire OS 8.1.x; no Google TV não.) Instalação só pela TV: Downloader + link do APK. Guia para pessoas comuns: `dist/kit-de-instalacao/COMO-INSTALAR.md` (caminho A pela TV, caminho B por Mac/Windows). Num reset de fábrica, as permissões somem: reinstale e configure de novo.

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
| `AdbSelf.java` | Cliente ADB mínimo que conecta no adbd do **próprio** aparelho (127.0.0.1:5555, chave RSA própria em `files/adbkey`). No Google TV funciona (testado no emulador); no Fire OS 8.1.x atual, não |
| `Setup.java` | Assistente "Configurar o RinoStick": via `AdbSelf`, faz o `pm grant`, liga a acessibilidade e o ouvinte de notificações e, opcionalmente, a limpeza (backup em prefs `cleanBackup`/`cleanDisabled`). Abre sozinho se faltar permissão; também fica em Ajustes → Fire TV |
| `Updater.java` | **Atualização pelo próprio app** (desde a v1.5). Uma vez por dia (`MainActivity.onResume`) consulta `api.github.com/repos/henriquesevero/rinostick-app/releases/latest`; se `tag_name` (sem o "v") for maior que o `versionName`, mostra aviso na dica da tela inicial e o item "Atualização disponível" em Ajustes. Ao escolher: pede a permissão "instalar apps desconhecidos" para o RinoStick na 1ª vez (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`), baixa `releases/latest/download/RinoStick-GoogleTV.apk` e instala com `PackageInstaller` (o sistema mostra a confirmação; `Updater.Result` abre a tela). Prefs: `updLatest`, `updTs`. **Para publicar uma versão:** subir `versionCode` e `versionName` nas duas pastas, criar a release com a tag `v<versionName>` (ex.: `v1.5.1`) e anexar os 4 arquivos com os nomes de sempre |
| `Screensaver.java` + `AuroraView.java` | Protetor de tela `DreamService` "Aurora": aurora animada em baixa resolução + relógio |

Ícones disponíveis em `Glyph.java`: GEAR, POWER, APPEARANCE, SPEAKER, WIFI, BAG, TV, EYE, SORT, RESTORE, CLOSE, PLAY, PAUSE, NEXT, XMARK, SUN, MOON, CLOUD, PARTLY, RAIN, STORM, FOG.

**`MainActivity.java`**
- Grade `COLS = 5` (`tileW` 308, `gap` 42; aprovado na v1.6), com o card do app = banner da TV (ou gradiente + ícone).
- Pastas guardadas em JSON em `prefs "folders"`; ordem em `"order"`; ocultos em `"hidden"`. `DEFAULT_HIDDEN` vazio. `DEFAULT_ORDER`: Apple TV, Netflix, Prime Video, YouTube, Spotify.
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
  1b. Seu nome (só na versão Google)
  2. **Apps:** Apps ocultos · Restaurar ordem · Google Play · Tela do Google
  3. **TV:** Rede · Configurações da TV · Configurar o RinoStick · Restaurar original (vermelho, com confirmação)
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
cd ~/Documents/2 - workspace/rinostick-app/versao-google-tv
./build.sh            # só compila  -> build/RinoStick-GoogleTV.apk, dist/kit-de-instalacao/ e dist/RinoStick-GoogleTV-kit.zip
./build.sh install    # compila, instala no aparelho conectado, concede permissões e abre o app
```

Por dentro, a sequência é: aapt2 compile/link (com `-A assets`) → `javac --release 8 -classpath android.jar` → `d8 --min-api 25` → zipalign → apksigner.

**Sempre use a mesma chave.** Com outra chave, o Android recusa a atualização e obriga a desinstalar, perdendo pastas, ordem e ajustes.

---

## 6. Instalar numa TV Google

Veja `dist/LEIA-ME.md` (inclusive "Depuração sem fio") e `dist/kit-de-instalacao/COMO-INSTALAR.md` (pela TV, Mac ou Windows). Não existe limpeza de anúncios nesta versão.

---

## 7. Limites já testados no Fire OS (no Google TV, provavelmente iguais, mas não testados)

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
- Grade de **5 por linha**, ícones grandes (pedido do usuário na v1.6, inspirado no Apple TV); a tela inicial mostra **2 fileiras**; descendo, o topo some e fica só a grade (estilo tvOS). Carrossel foi testado e rejeitado.
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
  Referência (v1.5, Fire TV Stick 4K Max, 24/11/2026): tela parada = 0 quadros e 0% de CPU; navegação: mediana 7 ms, 90% ≤ 14–17 ms, 99% ≈ 48 ms; ~61–66 MB de memória. O modo mover (ícones tremendo) fica em 17–18 ms/quadro e reconstrói a grade a cada passo — aceito, porque é raro.
- **Otimizações e correções da revisão da v1.5:**
  - `MainActivity.reload()` só reconstrói a grade se a assinatura (`signature()`: itens, nomes, apps das pastas, tema) mudou; senão mantém os objetos antigos (a grade, o modo mover e as pastas comparam `Item`/`Folder` por identidade). `pkgRx` zera `gridSig` (banner pode ter mudado). Tirou o engasgo de ~150 ms ao voltar com Home.
  - `Weather.refresh`: sem internet, espera 5 min antes de tentar de novo (antes tentava a cada minuto, chamado pelo relógio).
  - `Updater.check`: sem internet ou erro do GitHub, tenta de novo em 1 h (não a cada volta à tela inicial).
  - `HomeRedirectService.reopenAfterUpdate()`: se o serviço religar até 2 min depois de uma atualização do app, abre o RinoStick (antes a tela original ficava na frente até o próximo Home).
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
