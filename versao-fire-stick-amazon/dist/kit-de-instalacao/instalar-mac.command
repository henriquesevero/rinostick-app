#!/bin/bash
# RinoStick — instalador para Mac (versão Fire TV)
#
# Como usar: abra o Terminal, digite "bash " (com espaço), arraste este arquivo
# para a janela do Terminal e aperte Enter. Ou, se já souber o IP:
#   bash instalar-mac.command 192.168.0.50
#
# O que faz: instala o RinoStick no Fire TV, libera as permissões (botão Home,
# "Tocando agora" e protetor de tela) e, se você quiser, desliga anúncios e apps
# da Amazon sem uso. Baixa sozinho o que faltar (ferramentas ADB do Google e o app).

APK="RinoStick.apk"
APK_URL="https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick.apk"
PT_URL="https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
PKG="com.rinostick.app"

cd "$(dirname "$0")" || exit 1
echo
echo "=========================================="
echo "   RinoStick — instalação no Fire TV"
echo "=========================================="
echo

# 1. Ferramentas ADB (do Google)
ADB="$(command -v adb || true)"
[ -z "$ADB" ] && [ -x "./platform-tools/adb" ] && ADB="./platform-tools/adb"
if [ -z "$ADB" ]; then
    echo "→ Baixando as ferramentas ADB do Google (só na primeira vez, ~15 MB)…"
    curl -fsSL -o platform-tools.zip "$PT_URL" && unzip -q -o platform-tools.zip && rm -f platform-tools.zip
    ADB="./platform-tools/adb"
    xattr -dr com.apple.quarantine ./platform-tools 2>/dev/null
    if [ ! -x "$ADB" ]; then echo "Não consegui baixar. Confira a internet e tente de novo."; exit 1; fi
fi

# 2. O app
if [ ! -f "$APK" ]; then
    echo "→ Baixando o RinoStick…"
    curl -fsSL -o "$APK" "$APK_URL" || { echo "Não consegui baixar o app. Confira a internet."; exit 1; }
fi

# 3. IP do Fire TV
IP="${1:-}"
if [ -z "$IP" ]; then
    echo "No Fire TV, o IP fica em Configurações → Minha Fire TV → Sobre → Rede."
    read -r -p "Digite o IP do Fire TV (ex.: 192.168.0.50): " IP
fi
IP="$(echo "$IP" | tr -d ' ')"
[ -z "$IP" ] && { echo "Sem IP, não dá para continuar."; exit 1; }
T="$IP"; [[ "$IP" != *:* ]] && T="$IP:5555"
A() { "$ADB" -s "$T" "$@"; }

echo
echo "→ Conectando em $T…"
echo "  Olhe a TV: vai aparecer \"Permitir depuração USB?\"."
echo "  Marque \"Sempre permitir deste computador\" e escolha OK."
"$ADB" connect "$T" >/dev/null 2>&1
STATE=""
for i in $(seq 1 60); do
    STATE="$(A get-state 2>/dev/null)"
    [ "$STATE" = "device" ] && break
    [ $((i % 10)) = 0 ] && "$ADB" connect "$T" >/dev/null 2>&1
    sleep 2
done
if [ "$STATE" != "device" ]; then
    echo
    echo "Não conectou. Confira:"
    echo " • se o IP está certo;"
    echo " • se a Depuração ADB está ligada (Configurações → Minha Fire TV → Opções do desenvolvedor);"
    echo " • se o computador e a TV estão no mesmo Wi-Fi;"
    echo " • se você aceitou o aviso na TV."
    exit 1
fi

echo "→ Instalando o RinoStick…"
A install -r "$APK" >/dev/null || { echo "Falha na instalação."; exit 1; }

echo "→ Liberando as permissões…"
A shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS
SVC="$PKG/$PKG.HomeRedirectService"
A shell "cur=\$(settings get secure enabled_accessibility_services); case \"\$cur\" in *$SVC*) ;; null|'') settings put secure enabled_accessibility_services $SVC ;; *) settings put secure enabled_accessibility_services \"\$cur:$SVC\" ;; esac; settings put secure accessibility_enabled 1"
A shell cmd notification allow_listener "$PKG/$PKG.NowPlayingListener" >/dev/null 2>&1

# Fire OS / Android antigo (ex.: Fire OS 6): "settings put" e "cmd notification" não gravam.
# Confere cada permissão e, se faltar, grava direto no banco de ajustes.
fix_secure() {
    local cur new
    cur="$(A shell settings get secure "$1" | tr -d '\r')"
    [[ "$cur" == *"$2"* ]] && return
    if [ -z "$cur" ] || [ "$cur" = "null" ]; then new="$2"; else new="$cur:$2"; fi
    A shell content insert --uri content://settings/secure --bind "name:s:$1" --bind "value:s:$new"
}
fix_secure enabled_accessibility_services "$SVC"
fix_secure enabled_notification_listeners "$PKG/$PKG.NowPlayingListener"

echo "→ Abrindo o RinoStick…"
A shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1
sleep 3
# Protetor de tela "Aurora" (o app guarda o original na primeira abertura)
SS="$(A shell settings get secure screensaver_components | tr -d '\r')"
[ "$SS" != "$PKG/$PKG.Screensaver" ] && A shell settings put secure screensaver_components "$PKG/$PKG.Screensaver"

echo
echo "Quer também desligar os anúncios personalizados, a coleta de dados de uso e os"
echo "apps da Amazon que quase ninguém usa (Luna, Amazon Music, Fotos, Shop, Mini TV,"
echo "Amazon Kids, Jogos, Acesso antecipado, Novidades, Tela ambiente, Casa inteligente"
echo "e Áudio multiambiente)? Dá para desfazer depois."
read -r -p "Digite s para sim ou só Enter para não: " LIMPAR
if [ "$LIMPAR" = "s" ] || [ "$LIMPAR" = "S" ]; then
    BK="backups/$(echo "$IP" | tr ':.' '__')-$(date +%Y%m%d-%H%M)"
    mkdir -p "$BK"
    echo "→ Guardando os valores originais em $BK…"
    A shell 'for k in advertisingIdApp:interestBasedAds:value amz_limit_ad_tracking limit_ad_tracking USAGE_METRICS_UPLOAD_ENABLED pact_usage_collection_enabled; do echo "$k $(settings get secure $k)"; done' > "$BK/ajustes-originais.txt"
    echo "→ Desligando anúncios e coleta de dados…"
    A shell 'settings put secure advertisingIdApp:interestBasedAds:value 0; settings put secure amz_limit_ad_tracking 1; settings put secure limit_ad_tracking 1; settings put secure USAGE_METRICS_UPLOAD_ENABLED 0; settings put secure pact_usage_collection_enabled 0'
    echo "→ Desativando os apps sem uso…"
    A shell 'for p in com.amazon.spiderpork com.amazon.bueller.music com.amazon.shoptv.firetv.client com.amazon.smarthomemapviewapp com.amazon.bueller.photos com.amazon.tv.earlyaccess com.amazon.minitv.android.app com.amazon.gamehub com.amazon.tahoe com.amazon.whasettings com.amazon.tv.releasenotes com.amazon.tv.ftvambient; do pm disable-user --user 0 $p >/dev/null 2>&1 && echo $p; done' > "$BK/apps-desativados.txt"
fi

echo
echo "=========================================="
echo "   Pronto! Aperte o botão Home no controle."
echo "=========================================="
echo "Dica: por segurança, você já pode desligar a Depuração ADB na TV."
echo "O RinoStick continua funcionando."
