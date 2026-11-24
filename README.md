# RinoStick

Tela inicial minimalista para TV, no estilo Apple TV: sem anúncios, só com os seus apps.

- Grade de apps com o "dock" de vidro na primeira fileira; descendo, a tela fica só com os apps.
- Saudação com o seu nome, relógio, data e clima.
- "Tocando agora" (Spotify etc.), com capa, ⏯ ⏭ e ✕.
- Pastas, reordenação, apps ocultos e temas claro, escuro ou automático.
- Painel único de Ajustes, protetor de tela "Aurora" e sons suaves.
- Instalador de um clique para **Mac e Windows** (e, no Google TV, também direto pela TV).

## Duas versões

| Pasta | Aparelho | APK | Situação |
|---|---|---|---|
| [`versao-fire-stick-amazon/`](versao-fire-stick-amazon) | Fire TV Stick (Fire OS 8) | `RinoStick.apk` | Em uso e testada |
| [`versao-google-tv/`](versao-google-tv) | Google TV e Android TV | `RinoStick-GoogleTV.apk` | Testada no emulador de Google TV; ainda não numa TV física |

Cada pasta é um projeto completo e independente (código-fonte, `build.sh`, `dist/` e o guia `CLAUDE.md`).

## Estrutura de cada versão

| Caminho | Conteúdo |
|---|---|
| `src/`, `res/`, `assets/`, `AndroidManifest.xml` | Código-fonte do app |
| `build.sh` | Compila (`./build.sh`) ou compila e instala no aparelho conectado (`./build.sh install`) |
| `tools/` | Utilitários (gerador do som de navegação) |
| `dist/` | Guia de quem mantém o projeto (`LEIA-ME.md`) |
| `dist/kit-de-instalacao/` | O que vai para as pessoas: guia passo a passo (Mac e Windows), instaladores `instalar-mac.command` e `instalar-windows.bat` e o app |
| `CLAUDE.md` | Guia técnico completo: arquitetura, limites do sistema, decisões e preferências de design |

## Compilar

Requisitos: Java 17, Android SDK (platform 30 e build-tools 34), `adb` e a chave de assinatura em `~/.android/firelauncher.keystore`. A chave **não está no repositório**.

```
cd versao-fire-stick-amazon   # ou versao-google-tv
./build.sh                    # gera o APK em build/ (e copia para dist/)
./build.sh install            # compila, instala via adb, libera as permissões e abre o app
```

## Baixar

Tudo fica nos *Releases*. Links que sempre trazem a versão mais nova:

| | Kit de instalação (Mac e Windows) | Só o app |
|---|---|---|
| Fire TV | `https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-FireTV-kit.zip` | `…/releases/latest/download/RinoStick.apk` |
| Google TV | `https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV-kit.zip` | `…/releases/latest/download/RinoStick-GoogleTV.apk` |

Passo a passo: `dist/kit-de-instalacao/COMO-INSTALAR.md` de cada versão.
