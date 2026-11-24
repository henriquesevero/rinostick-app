#!/bin/bash
# Instala a tela inicial "RinoStick" numa TV Google TV / Android TV e configura tudo.
#
#   ./instalar.sh 192.168.0.50          -> instala e configura (ADB na porta 5555)
#   ./instalar.sh 192.168.0.50:41234    -> TVs com "Depuração sem fio": use o IP:porta mostrado
#                                          na TV (antes, pareie uma vez: adb pair IP:PORTA CÓDIGO)
#
# Antes: na TV, Configurações → Sistema → Sobre → clique 7x em "Build do Android TV OS";
# depois Sistema → Opções do desenvolvedor → Depuração USB (ligado).
# O IP fica em Configurações → Rede e Internet → (sua rede).
set -uo pipefail
cd "$(dirname "$0")"

IP="${1:-}"
PKG="com.rinostick.app"

if [ -z "$IP" ]; then
    echo "Uso: ./instalar.sh <IP da TV>[:porta]"
    exit 1
fi

ADB="$(command -v adb || true)"
[ -z "$ADB" ] && [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ] && ADB="$HOME/Library/Android/sdk/platform-tools/adb"
if [ -z "$ADB" ]; then
    echo "adb não encontrado. Instale com:  brew install android-platform-tools"
    exit 1
fi

TARGET="$IP"
[[ "$IP" != *:* ]] && TARGET="$IP:5555"
A() { "$ADB" -s "$TARGET" "$@"; }

echo "→ Conectando em $TARGET (aceite o aviso \"Permitir depuração\" na TV)…"
"$ADB" connect "$TARGET" >/dev/null
for i in $(seq 1 30); do
    STATE="$("$ADB" -s "$TARGET" get-state 2>/dev/null || true)"
    [ "$STATE" = "device" ] && break
    sleep 2
done
if [ "$STATE" != "device" ]; then
    echo "Não conectou. Confira o IP, se a Depuração USB está ligada e se aceitou o aviso na TV."
    exit 1
fi

echo "→ Instalando o app…"
A install -r RinoStick-GoogleTV.apk || { echo "Falha na instalação."; exit 1; }

echo "→ Liberando permissões…"
A shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS

# Botão Home abre a tela nova (serviço de acessibilidade)
SVC="$PKG/$PKG.HomeRedirectService"
CUR="$(A shell settings get secure enabled_accessibility_services | tr -d '\r')"
if [[ "$CUR" != *"$SVC"* ]]; then
    if [ -z "$CUR" ] || [ "$CUR" = "null" ]; then NEW="$SVC"; else NEW="$CUR:$SVC"; fi
    A shell settings put secure enabled_accessibility_services "$NEW"
fi
A shell settings put secure accessibility_enabled 1

# "Tocando agora" (acesso às sessões de mídia)
A shell cmd notification allow_listener "$PKG/$PKG.NowPlayingListener" || true

echo "→ Abrindo a tela nova…"
A shell am start -n "$PKG/.MainActivity" >/dev/null
echo
echo "Pronto! Aperte Home no controle para conferir."
