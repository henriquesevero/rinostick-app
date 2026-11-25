# RinoStick — como instalar no Fire TV

Uma tela inicial limpa para o Fire TV Stick, no estilo Apple TV:

- **sem anúncios** e só com os seus apps;
- **saudação** com o seu nome, relógio, data e clima;
- a **música que está tocando** no Spotify, com capa e controles;
- **pastas** e um protetor de tela bonito.

**Você vai precisar de um computador (Mac ou Windows) só uma vez**, no mesmo Wi-Fi do Fire TV. O Fire TV não deixa um app liberar as próprias permissões, então o computador faz isso. Leva uns 10 minutos.

---

## Parte 1 — No Fire TV

1. Vá em **Configurações → Minha Fire TV → Sobre**.
2. Fique em cima do nome do aparelho (ex.: "Fire TV Stick 4K") e aperte **OK 7 vezes**, até aparecer "Você agora é um desenvolvedor".
3. Volte uma tela e entre em **Opções do desenvolvedor**. Ligue a **Depuração ADB**.
4. Volte em **Sobre → Rede** e **anote o endereço IP** (algo como `192.168.0.50`).

Deixe a TV ligada nessa tela. Você vai precisar dela na Parte 3.

## Parte 2 — Baixar o kit no computador

1. No computador, abra este link:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-FireTV-kit.zip
   ```
2. Vai baixar o arquivo **RinoStick-FireTV-kit.zip**.
   - **No Mac:** o Safari abre o zip sozinho e cria a pasta **RinoStick-FireTV-kit** em Downloads. Se não abrir, dê dois cliques no zip.
   - **No Windows:** clique com o botão direito no zip → **Extrair tudo…** → **Extrair**. Não rode o instalador de dentro do zip sem extrair.

## Parte 3 — Rodar o instalador

### No Mac

1. Abra o **Terminal**: aperte **Cmd + Espaço**, digite `Terminal` e aperte Enter.
2. Na janela do Terminal, digite `bash` e um **espaço** (não aperte Enter ainda).
3. Abra a pasta **RinoStick-FireTV-kit** no Finder e **arraste o arquivo `instalar-mac.command`** para dentro da janela do Terminal. O caminho do arquivo aparece escrito.
4. Aperte **Enter**.
5. Na primeira vez, ele baixa as ferramentas do Google (uns 15 MB). Espere.
6. Quando pedir, **digite o IP** que você anotou e aperte Enter.
7. **Olhe a TV:** vai aparecer **"Permitir depuração USB?"**. Marque **"Sempre permitir deste computador"** e escolha **OK**.
8. No fim, ele pergunta se quer **desligar anúncios e apps da Amazon sem uso**. Digite `s` e Enter para sim, ou só Enter para não.
9. Apareceu **"Pronto!"**? Pode fechar o Terminal.

### No Windows (10 ou 11)

1. Abra a pasta **RinoStick-FireTV-kit** que você extraiu.
2. Dê **dois cliques** em **`instalar-windows.bat`**.
3. Se aparecer **"O Windows protegeu o computador"**, clique em **Mais informações** → **Executar assim mesmo**.
4. Na primeira vez, ele baixa as ferramentas do Google (uns 15 MB). Espere.
5. Quando pedir, **digite o IP** que você anotou e aperte Enter.
6. Se o Windows perguntar sobre o **Firewall**, clique em **Permitir acesso**.
7. **Olhe a TV:** vai aparecer **"Permitir depuração USB?"**. Marque **"Sempre permitir deste computador"** e escolha **OK**.
8. No fim, ele pergunta se quer **desligar anúncios e apps da Amazon sem uso**. Digite `s` e Enter para sim, ou só Enter para não.
9. Apareceu **"Pronto!"**? Aperte qualquer tecla para fechar.

## Parte 4 — Pronto

1. Aperte o botão **Home** no controle. A tela nova é a sua tela inicial agora.
2. Opcional, por segurança: volte em **Configurações → Minha Fire TV → Opções do desenvolvedor** e **desligue a Depuração ADB**. O RinoStick continua funcionando.

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

## Como atualizar

O RinoStick avisa sozinho quando sai uma versão nova: aparece **"Nova versão… do RinoStick"** embaixo da tela inicial e o item **"Atualização disponível"** nos Ajustes (aperte **☰** e escolha o item).

1. Escolha **"Atualização disponível"**.
2. Só na primeira vez: o sistema pede para **permitir que o RinoStick instale apps**. Na tela que abrir, ligue o RinoStick, volte e escolha "Atualização disponível" de novo.
3. Espere baixar e confirme em **Instalar**.

Suas pastas, a ordem dos apps, os ajustes e as permissões continuam como estavam. Não precisa de computador.

## Se algo der errado

| Problema | Solução |
|---|---|
| "Não conectou" | Confira o IP (ele pode mudar depois de religar o roteador), se a Depuração ADB está ligada e se o computador está no **mesmo Wi-Fi** da TV. Rode o instalador de novo |
| O aviso "Permitir depuração" não apareceu | Desligue e ligue a Depuração ADB na TV e rode o instalador de novo |
| No Mac: "Operation not permitted" ou "Permission denied" | Confira se digitou `bash` e um espaço antes de arrastar o arquivo |
| No Windows: a janela abre e fecha na hora | Extraia o zip antes (Parte 2) e rode o `instalar-windows.bat` da pasta extraída |
| O botão Home voltou para a tela da Amazon (pode acontecer após atualizações do Fire TV) | Ligue a Depuração ADB e rode o instalador de novo. Seus ajustes continuam |
| Anúncios ou apps da Amazon voltaram | Rode o instalador de novo e responda `s` na última pergunta |

## Quero voltar ao original

No Fire TV: **☰ → Restaurar original** e confirme. A tela da Amazon volta e o app é removido.
