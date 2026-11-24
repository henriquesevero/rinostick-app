# RinoStick — tela inicial estilo Apple TV para Google TV e Android TV

Uma tela inicial limpa para a sua TV com Google TV ou Android TV (Chromecast com Google TV, TVs TCL, Philips, Sony, Xiaomi Mi Box etc.):

- só com os seus apps, sem fileiras de recomendações;
- **saudação** com o seu nome, relógio, data e clima;
- a **música que está tocando** no Spotify, com capa e controles;
- **pastas** e um protetor de tela bonito.

**Tudo é feito pelo controle, sem computador.** Leva uns 10 minutos.

> Os nomes dos menus mudam um pouco de marca para marca. Se não achar algum, procure um nome parecido.

---

## 1. Liberar as opções de desenvolvedor

1. Vá em **Configurações → Sistema → Sobre**.
2. Desça até **"Build do Android TV OS"** (ou "Build") e aperte **OK 7 vezes**, até aparecer "Você agora é um desenvolvedor".
3. Volte para **Configurações → Sistema → Opções do desenvolvedor**.
4. Ligue a **Depuração USB**.

## 2. Instalar o app Downloader

1. Abra a **Google Play** e procure **Downloader**. É o ícone laranja, da AFTVnews.
2. Instale.
3. Vá em **Configurações → Apps → Segurança e restrições → Fontes desconhecidas** e ligue o **Downloader**.

## 3. Baixar e instalar o RinoStick

1. Abra o **Downloader**. Se ele pedir acesso a arquivos, permita.
2. No campo de endereço, digite o link abaixo e aperte **Go**. Ele sempre baixa a versão mais recente:
   ```
   https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV.apk
   ```
3. Quando terminar de baixar, aperte **Instalar** e depois **Abrir**.
4. O Downloader pergunta se quer apagar o arquivo baixado: escolha **Delete**. O app já está instalado.

## 4. Configurar (só na primeira vez)

1. Ao abrir, o RinoStick mostra **"Configurar o RinoStick"**. Escolha **"Configurar agora"**.
2. Vai aparecer na TV o aviso **"Permitir depuração?"**. Marque **"Sempre permitir"** e aperte **OK**.
   O "computador", no caso, é a própria TV. É assim que o app libera as permissões sozinho.
3. Apareceu **"Tudo pronto! ✓"**? Aperte o botão **Home** no controle. A tela nova é a sua tela inicial agora.
4. Opcional, por segurança: volte em **Opções do desenvolvedor** e **desligue a Depuração USB**. O app continua funcionando.

Se aparecer **"Não deu certo"** mesmo com a Depuração USB ligada, a sua TV só aceita a "Depuração sem fio" com código. Nesse caso, a configuração precisa ser feita uma vez por um computador (veja `LEIA-ME.md`).

---

## Como usar

| Quero… | Faço assim |
|---|---|
| Abrir um app | Selecione e aperte **OK** |
| Mudar apps de lugar | **Segure OK** no ícone (eles tremem), mova com as setas e aperte **OK** para terminar |
| Criar pasta, ocultar app ou ver informações | Selecione o app e aperte o botão de **menu/opções**, se o controle tiver |
| Abrir os ajustes | Vá na **engrenagem** no canto de cima |
| Colocar o seu nome na saudação | Ajustes → **Seu nome** |
| Tema claro, escuro ou automático | Ajustes → **Aparência** |
| Corrigir a cidade do clima | Ajustes → **Clima → Escolher cidade** |
| Instalar apps novos | Ajustes → **Google Play** |
| Usar a busca do Google | Ajustes → **Tela do Google** (libera a tela original por 10 minutos) |
| Controlar a música | Suba até o cartão da música no topo: ⏯ pausa, ⏭ avança, ✕ fecha |

## Se algo der errado

| Problema | Solução |
|---|---|
| "A TV não respondeu na porta 5555" | Confira se a **Depuração USB** está ligada e escolha **Tentar de novo**. Se continuar, a TV precisa do computador (veja acima) |
| O aviso "Permitir depuração" não apareceu ou sumiu | Escolha **Tentar de novo**. O aviso fica na tela por até 2 minutos |
| O botão Home voltou para a tela do Google (pode acontecer após atualizações) | Ligue a Depuração USB, abra o RinoStick, vá na engrenagem → **Configurar o RinoStick** e configure de novo |

## Quero voltar ao original

Engrenagem → **Restaurar original** e confirme. A tela do Google volta e o app é removido.
