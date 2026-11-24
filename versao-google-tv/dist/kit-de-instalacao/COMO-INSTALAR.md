# RinoStick — como instalar no Google TV e Android TV

Uma tela inicial limpa para a sua TV com Google TV ou Android TV (Chromecast com Google TV, TVs TCL, Philips, Sony, Xiaomi Mi Box etc.), no estilo Apple TV:

- só com os seus apps, sem fileiras de recomendações;
- **saudação** com o seu nome, relógio, data e clima;
- a **música que está tocando** no Spotify, com capa e controles;
- **pastas** e um protetor de tela bonito.

Há dois caminhos:

- **Caminho A — só pela TV**, sem computador. Funciona na maioria das TVs.
- **Caminho B — com um computador** (Mac ou Windows). Use se o caminho A não funcionar na sua TV.

> Os nomes dos menus mudam um pouco de marca para marca. Se não achar algum, procure um nome parecido.

---

## Antes de tudo — liberar as opções de desenvolvedor (os dois caminhos)

1. Na TV, vá em **Configurações → Sistema → Sobre**.
2. Desça até **"Build do Android TV OS"** (ou "Build") e aperte **OK 7 vezes**, até aparecer "Você agora é um desenvolvedor".
3. Volte para **Configurações → Sistema → Opções do desenvolvedor** e ligue a **Depuração USB**.
4. Anote o **endereço IP** da TV: **Configurações → Rede e Internet →** (sua rede). É algo como `192.168.0.50`.

---

## Caminho A — só pela TV

### 1. Instalar o app Downloader

1. Abra a **Google Play** e procure **Downloader**. É o ícone laranja, da AFTVnews.
2. Instale.
3. Vá em **Configurações → Apps → Segurança e restrições → Fontes desconhecidas** e ligue o **Downloader**.

### 2. Baixar e instalar o RinoStick

1. Abra o **Downloader**. Se ele pedir acesso a arquivos, permita.
2. No campo de endereço, digite o link abaixo e aperte **Go**:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV.apk
   ```
3. Quando terminar de baixar, aperte **Instalar** e depois **Abrir**.
4. O Downloader pergunta se quer apagar o arquivo baixado: escolha **Delete**. O app já está instalado.

### 3. Configurar

1. Ao abrir, o RinoStick mostra **"Configurar o RinoStick"**. Escolha **"Configurar agora"**.
2. Vai aparecer na TV o aviso **"Permitir depuração USB?"**. Marque **"Sempre permitir"** e aperte **OK**.
3. Apareceu **"Tudo pronto! ✓"**? Aperte o botão **Home** no controle. Pronto!

Apareceu **"Não deu certo"**? A sua TV não aceita esse caminho. Siga o **Caminho B**. O app já instalado não atrapalha.

---

## Caminho B — com um computador

O computador precisa estar no **mesmo Wi-Fi** da TV.

### 1. Baixar o kit no computador

1. No computador, abra este link:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV-kit.zip
   ```
2. Vai baixar o arquivo **RinoStick-GoogleTV-kit.zip**.
   - **No Mac:** o Safari abre o zip sozinho e cria a pasta **RinoStick-GoogleTV-kit** em Downloads. Se não abrir, dê dois cliques no zip.
   - **No Windows:** clique com o botão direito no zip → **Extrair tudo…** → **Extrair**. Não rode o instalador de dentro do zip sem extrair.

### 2a. No Mac

1. Abra o **Terminal**: aperte **Cmd + Espaço**, digite `Terminal` e aperte Enter.
2. Na janela do Terminal, digite `bash` e um **espaço** (não aperte Enter ainda).
3. Abra a pasta **RinoStick-GoogleTV-kit** no Finder e **arraste o arquivo `instalar-mac.command`** para dentro da janela do Terminal.
4. Aperte **Enter**.
5. Na primeira vez, ele baixa as ferramentas do Google (uns 15 MB). Espere.
6. Quando pedir, **digite o IP** da TV e aperte Enter.
7. **Olhe a TV:** vai aparecer **"Permitir depuração USB?"**. Marque **"Sempre permitir"** e escolha **OK**.
8. Apareceu **"Pronto!"**? Pode fechar o Terminal.

### 2b. No Windows (10 ou 11)

1. Abra a pasta **RinoStick-GoogleTV-kit** que você extraiu.
2. Dê **dois cliques** em **`instalar-windows.bat`**.
3. Se aparecer **"O Windows protegeu o computador"**, clique em **Mais informações** → **Executar assim mesmo**.
4. Na primeira vez, ele baixa as ferramentas do Google (uns 15 MB). Espere.
5. Quando pedir, **digite o IP** da TV e aperte Enter.
6. Se o Windows perguntar sobre o **Firewall**, clique em **Permitir acesso**.
7. **Olhe a TV:** vai aparecer **"Permitir depuração USB?"**. Marque **"Sempre permitir"** e escolha **OK**.
8. Apareceu **"Pronto!"**? Aperte qualquer tecla para fechar.

### TV com "Depuração sem fio" (Google TV mais novos)

Algumas TVs novas, como o Google TV Streamer, não têm "Depuração USB" pela rede, só **"Depuração sem fio"** com código de pareamento. Nesse caso, peça ajuda para alguém acostumado com computador: é preciso parear uma vez (`adb pair IP:porta código`, com o `adb` da pasta `platform-tools` que o instalador baixou) e depois rodar o instalador digitando o **IP:porta** mostrado na tela da Depuração sem fio.

---

## Depois de instalar

1. Aperte o botão **Home**. A tela nova é a sua tela inicial agora.
2. Opcional, por segurança: volte em **Opções do desenvolvedor** e **desligue a Depuração USB**. O RinoStick continua funcionando.

## Como usar

| Quero… | Faço assim |
|---|---|
| Abrir um app | Selecione e aperte **OK** |
| Mudar apps de lugar | **Segure OK** no ícone e escolha **Mover**. Mova com as setas e aperte **OK** para terminar |
| Criar pasta, ocultar app ou ver informações | **Segure OK** no ícone e escolha a opção |
| Abrir os ajustes | Vá na **engrenagem** no canto de cima |
| Colocar o seu nome na saudação | Ajustes → **Seu nome** |
| Tema claro, escuro ou automático | Ajustes → **Aparência** |
| Corrigir a cidade do clima | Ajustes → **Clima → Escolher cidade** |
| Instalar apps novos | Ajustes → **Google Play** |
| Usar a busca do Google | Ajustes → **Tela do Google** (libera a tela original por 10 minutos) |
| Controlar a música | Suba até o cartão da música no topo: ⏯ pausa, ⏭ avança, ✕ fecha |

## Como atualizar

O RinoStick avisa sozinho quando sai uma versão nova: aparece **"Nova versão… do RinoStick"** embaixo da tela inicial e o item **"Atualização disponível"** nos Ajustes (vá na **engrenagem** e escolha o item).

1. Escolha **"Atualização disponível"**.
2. Só na primeira vez: o sistema pede para **permitir que o RinoStick instale apps**. Na tela que abrir, ligue o RinoStick, volte e escolha "Atualização disponível" de novo.
3. Espere baixar e confirme em **Instalar**.

Suas pastas, a ordem dos apps, os ajustes e as permissões continuam como estavam. Não precisa de computador.

## Se algo der errado

| Problema | Solução |
|---|---|
| Caminho A: "Não deu certo" | Use o Caminho B |
| Caminho B: "Não conectou" | Confira o IP, se a Depuração USB está ligada e se o computador está no **mesmo Wi-Fi** da TV. Rode o instalador de novo |
| O aviso "Permitir depuração" não apareceu | Desligue e ligue a Depuração USB e tente de novo |
| No Mac: "Operation not permitted" ou "Permission denied" | Confira se digitou `bash` e um espaço antes de arrastar o arquivo |
| No Windows: a janela abre e fecha na hora | Extraia o zip antes e rode o `instalar-windows.bat` da pasta extraída |
| O botão Home voltou para a tela do Google (pode acontecer após atualizações) | Ligue a Depuração USB e configure de novo (Caminho A, pela engrenagem → Configurar o RinoStick, ou Caminho B) |

## Quero voltar ao original

Engrenagem → **Restaurar original** e confirme. A tela do Google volta e o app é removido.
