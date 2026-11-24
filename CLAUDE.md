# RinoStick — mapa do repositório

O repositório tem **duas cópias completas e independentes** do app. Idioma do usuário: português do Brasil.

| Pasta | Para | Guia técnico |
|---|---|---|
| `versao-fire-stick-amazon/` | Fire TV Stick (versão principal, em uso e testada) | `versao-fire-stick-amazon/CLAUDE.md` |
| `versao-google-tv/` | Google TV / Android TV (**nunca testada** num aparelho real) | `versao-google-tv/CLAUDE.md` (seção 0 = diferenças) |

Regras:
- Antes de mexer, leia o `CLAUDE.md` da pasta da versão em questão.
- Mudanças comuns (grade, visual, cartão de música, clima, ajustes) devem ser feitas **nas duas pastas**, a menos que o usuário peça só uma.
- Os dois APKs usam o mesmo ID (`com.rinostick.app`) e a mesma chave (`~/.android/firelauncher.keystore`, nunca versionar).
- Release no GitHub: anexar `RinoStick.apk` (Fire) e `RinoStick-GoogleTV.apk` (Google) na mesma release, com esses nomes exatos, porque os links `releases/latest/download/<nome>` dos guias dependem deles.
