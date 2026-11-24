# RinoStick

Tela inicial minimalista para **Fire TV Stick**, no estilo Apple TV: sem anúncios, só com os seus apps.

- Grade de apps com o "dock" de vidro na primeira fileira; descendo, a tela fica só com os apps.
- Saudação com o nome da conta, relógio, data e clima.
- "Tocando agora" (Spotify etc.), com capa, ⏯ ⏭ e ✕.
- Pastas, reordenação, apps ocultos e temas claro, escuro ou automático.
- Painel único de Ajustes, protetor de tela "Aurora" e sons suaves.
- Assistente que libera as permissões **sem computador**.

Feito para Fire OS 8 (Android 11). Compila sem Gradle.

## Estrutura

| Caminho | Conteúdo |
|---|---|
| `src/`, `res/`, `assets/`, `AndroidManifest.xml` | Código-fonte do app |
| `build.sh` | Compila (`./build.sh`) ou compila e instala no aparelho conectado (`./build.sh install`) |
| `tools/` | Utilitários (gerador do som de navegação) |
| `dist/` | Instalação pelo Mac (`instalar.sh`) e o guia `LEIA-ME.md` |
| `dist/compartilhar/` | Guia e mensagem para instalar **só pelo Fire TV** (app Downloader + link do APK) |
| `CLAUDE.md` | Guia técnico completo: arquitetura, limites do Fire OS, decisões e preferências de design |

## Compilar

Requisitos: Java 17, Android SDK (platform 30 e build-tools 34), `adb` e a chave de assinatura em `~/.android/firelauncher.keystore`. A chave **não está no repositório**.

```
./build.sh            # gera build/RinoStick.apk (e copia para dist/ e dist/compartilhar/)
./build.sh install    # compila, instala via adb, libera as permissões e abre o app
```

Detalhes em [`CLAUDE.md`](CLAUDE.md) e [`dist/LEIA-ME.md`](dist/LEIA-ME.md).
