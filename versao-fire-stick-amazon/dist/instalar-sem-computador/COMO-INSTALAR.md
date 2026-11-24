# RinoStick — tela inicial estilo Apple TV para o Fire TV

Uma tela inicial limpa para o Fire TV Stick:

- **sem anúncios** e só com os seus apps;
- **saudação** com o seu nome, relógio, data e clima;
- a **música que está tocando** no Spotify, com capa e controles;
- **pastas** e um protetor de tela bonito.

**Tudo é feito pelo controle, sem computador.** Leva uns 10 minutos.

---

## 1. Liberar as opções de desenvolvedor

1. Vá em **Configurações → Minha Fire TV → Sobre**.
2. Fique em cima do nome do aparelho (ex.: "Fire TV Stick 4K") e aperte **OK 7 vezes**, até aparecer "Você agora é um desenvolvedor".
3. Volte uma tela e entre em **Opções do desenvolvedor**.
4. Ligue a **Depuração ADB**.

## 2. Instalar o app Downloader

1. Na tela inicial da Amazon, vá em **Buscar** e procure **Downloader**. É o ícone laranja, da AFTVnews.
2. Instale.
3. Volte em **Configurações → Minha Fire TV → Opções do desenvolvedor → Instalar apps desconhecidos** e ligue o **Downloader**.

## 3. Baixar e instalar o RinoStick

1. Abra o **Downloader**. Se ele pedir acesso a arquivos, permita.
2. No campo de endereço, digite o link abaixo e aperte **Go**. Ele sempre baixa a versão mais recente:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick.apk
   ```
3. Quando terminar de baixar, aperte **Instalar** e depois **Abrir**.
4. O Downloader pergunta se quer apagar o arquivo baixado: escolha **Delete**. O app já está instalado.

## 4. Configurar (só na primeira vez)

1. Ao abrir, o RinoStick mostra **"Configurar o RinoStick"**.
2. Escolha **"Configurar e remover anúncios"** (recomendado) ou só **"Configurar agora"**.
3. Vai aparecer na TV o aviso **"Permitir depuração?"**. Marque **"Sempre permitir deste computador"** e aperte **OK**.
   O "computador", no caso, é o próprio Fire TV. É assim que o app libera as permissões sozinho.
4. Apareceu **"Tudo pronto! ✓"**? Aperte o botão **Home** no controle. A tela nova é a sua tela inicial agora.
5. Opcional, por segurança: volte em **Opções do desenvolvedor** e **desligue a Depuração ADB**. O app continua funcionando.

---

## Como usar

| Quero… | Faço assim |
|---|---|
| Abrir um app | Selecione e aperte **OK** |
| Mudar apps de lugar | **Segure OK** no ícone (eles tremem), mova com as setas e aperte **OK** para terminar |
| Criar pasta, ocultar app ou ver informações | Selecione o app e aperte **☰** (botão menu) |
| Abrir os ajustes | Aperte **☰** ou vá na **engrenagem** no canto de cima |
| Tema claro, escuro ou automático | Ajustes → **Aparência** |
| Corrigir a cidade do clima | Ajustes → **Clima → Escolher cidade** |
| Instalar apps novos | Ajustes → **Loja e busca da Amazon** (libera a tela da Amazon por 10 minutos) |
| Controlar a música | Suba até o cartão da música no topo: ⏯ pausa, ⏭ avança, ✕ fecha |
| Ver os apps de baixo | Desça a partir da 2ª fileira: o topo some e aparecem todos os apps |

## Se algo der errado

| Problema | Solução |
|---|---|
| "A Depuração ADB está desligada" | Ligue em Opções do desenvolvedor (passo 1) e escolha **Tentar de novo** |
| O aviso "Permitir depuração" não apareceu ou sumiu | Escolha **Tentar de novo**. O aviso fica na tela por até 2 minutos |
| O botão Home voltou para a tela da Amazon (pode acontecer após atualizações do Fire TV) | Ligue a Depuração ADB, abra o RinoStick, aperte **☰ → Configurar o RinoStick** e configure de novo |
| Anúncios ou apps da Amazon voltaram | O mesmo: **☰ → Configurar o RinoStick → Configurar e remover anúncios** |

## Quero voltar ao original

Aperte **☰ → Restaurar original** e confirme. A tela da Amazon volta e o app é removido.
