#!/bin/bash
# Instala a tela inicial "RinoStick" num Fire TV Stick e configura tudo.
#
#   ./instalar.sh 192.168.0.50            -> instala e configura
#   ./instalar.sh 192.168.0.50 --limpar   -> idem + desliga anúncios/coleta e apps da Amazon sem uso
#
# Antes: no Fire TV, ative Configurações → Minha Fire TV → Opções do desenvolvedor →
# Depuração ADB (ligado), e anote o IP em Minha Fire TV → Sobre → Rede.
set -uo pipefail
cd "$(dirname "$0")"

IP="${1:-}"
LIMPAR="${2:-}"
PKG="com.rinostick.app"

if [ -z "$IP" ]; then
    echo "Uso: ./instalar.sh <IP do Fire TV> [--limpar]"
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
    echo "Não conectou. Confira o IP, se a Depuração ADB está ligada e se aceitou o aviso na TV."
    exit 1
fi

echo "→ Instalando o app…"
A install -r RinoStick.apk || { echo "Falha na instalação."; exit 1; }

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

if [ "$LIMPAR" = "--limpar" ]; then
    BK="backups/$(echo "$IP" | tr ':.' '__')-$(date +%Y%m%d-%H%M)"
    mkdir -p "$BK"
    echo "→ Desligando anúncios personalizados e coleta de dados (valores originais em $BK)…"
    {
        echo "# Valores originais — para desfazer: adb shell settings put secure <chave> <valor>"
        for k in "advertisingIdApp:interestBasedAds:value" amz_limit_ad_tracking limit_ad_tracking \
                 USAGE_METRICS_UPLOAD_ENABLED pact_usage_collection_enabled; do
            echo "$k $(A shell settings get secure "$k" | tr -d '\r')"
        done
    } > "$BK/settings-original.txt"
    A shell settings put secure "advertisingIdApp:interestBasedAds:value" 0
    A shell settings put secure amz_limit_ad_tracking 1
    A shell settings put secure limit_ad_tracking 1
    A shell settings put secure USAGE_METRICS_UPLOAD_ENABLED 0
    A shell settings put secure pact_usage_collection_enabled 0

    echo "→ Desativando apps da Amazon sem uso (lista em $BK)…"
    echo "# Para reativar: adb shell pm enable <pacote>" > "$BK/apps-desativados.txt"
    for p in com.amazon.spiderpork com.amazon.bueller.music com.amazon.shoptv.firetv.client \
             com.amazon.smarthomemapviewapp com.amazon.bueller.photos com.amazon.tv.earlyaccess \
             com.amazon.minitv.android.app com.amazon.gamehub com.amazon.tahoe \
             com.amazon.whasettings com.amazon.tv.releasenotes com.amazon.tv.ftvambient; do
        if A shell pm list packages | tr -d '\r' | grep -qx "package:$p"; then
            A shell pm disable-user --user 0 "$p" >/dev/null 2>&1 && echo "$p" >> "$BK/apps-desativados.txt"
        fi
    done
fi

echo "→ Abrindo a tela nova…"
A shell am start -n "$PKG/.MainActivity" >/dev/null
echo
echo "Pronto! Aperte Home no controle para conferir."
