@echo off
setlocal
REM RinoStick - instalador para Windows (versao Fire TV)
REM Como usar: de dois cliques neste arquivo. Ele baixa sozinho o que faltar
REM (ferramentas ADB do Google e o app), instala o RinoStick no Fire TV e libera
REM as permissoes. Requer Windows 10 ou 11.

cd /d "%~dp0"
set "APK=RinoStick.apk"
set "APK_URL=https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick.apk"
set "PT_URL=https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
set "PKG=com.rinostick.app"
set "ADB=%~dp0platform-tools\adb.exe"

echo.
echo ==========================================
echo    RinoStick - instalacao no Fire TV
echo ==========================================
echo.

REM 1. Ferramentas ADB (do Google)
if not exist "%ADB%" (
    echo - Baixando as ferramentas ADB do Google ^(so na primeira vez, ~15 MB^)...
    curl -fsSL -o "%TEMP%\platform-tools.zip" "%PT_URL%"
    tar -xf "%TEMP%\platform-tools.zip" -C "%~dp0"
    del "%TEMP%\platform-tools.zip" >nul 2>&1
)
if not exist "%ADB%" (
    echo Nao consegui baixar as ferramentas. Confira a internet e tente de novo.
    goto :fim
)

REM 2. O app
if not exist "%APK%" (
    echo - Baixando o RinoStick...
    curl -fsSL -o "%APK%" "%APK_URL%"
)
if not exist "%APK%" (
    echo Nao consegui baixar o app. Confira a internet e tente de novo.
    goto :fim
)

REM 3. IP do Fire TV
echo No Fire TV, o IP fica em Configuracoes - Minha Fire TV - Sobre - Rede.
set "IP="
set /p "IP=Digite o IP do Fire TV (ex.: 192.168.0.50) e aperte Enter: "
if "%IP%"=="" (
    echo Sem IP, nao da para continuar.
    goto :fim
)
set "T=%IP%"
echo %IP% | find ":" >nul || set "T=%IP%:5555"

echo.
echo - Conectando em %T%...
echo   Olhe a TV: vai aparecer "Permitir depuracao USB?".
echo   Marque "Sempre permitir deste computador" e escolha OK.
echo   ^(Se o Windows perguntar sobre o Firewall, clique em Permitir.^)
"%ADB%" connect %T% >nul 2>&1
set /a N=0
:espera
set "STATE="
for /f "delims=" %%s in ('call "%ADB%" -s %T% get-state 2^>nul') do set "STATE=%%s"
if "%STATE%"=="device" goto :conectado
set /a N+=1
if %N% GEQ 60 goto :semconexao
set /a R=N %% 10
if %R%==0 "%ADB%" connect %T% >nul 2>&1
timeout /t 2 /nobreak >nul
goto :espera

:semconexao
echo.
echo Nao conectou. Confira:
echo  - se o IP esta certo;
echo  - se a Depuracao ADB esta ligada ^(Configuracoes - Minha Fire TV - Opcoes do desenvolvedor^);
echo  - se o computador e a TV estao no mesmo Wi-Fi;
echo  - se voce aceitou o aviso na TV.
goto :fim

:conectado
echo - Instalando o RinoStick...
"%ADB%" -s %T% install -r "%APK%" >nul
if errorlevel 1 (
    echo Falha na instalacao.
    goto :fim
)

echo - Liberando as permissoes...
"%ADB%" -s %T% shell pm grant %PKG% android.permission.WRITE_SECURE_SETTINGS
"%ADB%" -s %T% shell "s=%PKG%/%PKG%.HomeRedirectService; cur=$(settings get secure enabled_accessibility_services); case \"$cur\" in *$s*) ;; null|'') settings put secure enabled_accessibility_services $s ;; *) settings put secure enabled_accessibility_services \"$cur:$s\" ;; esac; settings put secure accessibility_enabled 1"
"%ADB%" -s %T% shell cmd notification allow_listener %PKG%/%PKG%.NowPlayingListener >nul 2>&1

echo - Abrindo o RinoStick...
"%ADB%" -s %T% shell am start -n %PKG%/.MainActivity >nul 2>&1
timeout /t 3 /nobreak >nul
REM Protetor de tela "Aurora" (o app guarda o original na primeira abertura)
"%ADB%" -s %T% shell "ss=%PKG%/%PKG%.Screensaver; [ \"$(settings get secure screensaver_components)\" = \"$ss\" ] || settings put secure screensaver_components $ss"

echo.
echo Quer tambem desligar os anuncios personalizados, a coleta de dados de uso e os
echo apps da Amazon que quase ninguem usa ^(Luna, Amazon Music, Fotos, Shop, Mini TV,
echo Amazon Kids, Jogos, Acesso antecipado, Novidades, Tela ambiente, Casa inteligente
echo e Audio multiambiente^)? Da para desfazer depois.
set "LIMPAR="
set /p "LIMPAR=Digite s para sim ou so Enter para nao: "
if /i not "%LIMPAR%"=="s" goto :pronto

set "BK=backups\%IP::=_%"
set "BK=%BK:.=_%"
if not exist "%BK%" mkdir "%BK%"
echo - Guardando os valores originais em %BK%...
"%ADB%" -s %T% shell "for k in advertisingIdApp:interestBasedAds:value amz_limit_ad_tracking limit_ad_tracking USAGE_METRICS_UPLOAD_ENABLED pact_usage_collection_enabled; do echo $k $(settings get secure $k); done" > "%BK%\ajustes-originais.txt"
echo - Desligando anuncios e coleta de dados...
"%ADB%" -s %T% shell "settings put secure advertisingIdApp:interestBasedAds:value 0; settings put secure amz_limit_ad_tracking 1; settings put secure limit_ad_tracking 1; settings put secure USAGE_METRICS_UPLOAD_ENABLED 0; settings put secure pact_usage_collection_enabled 0"
echo - Desativando os apps sem uso...
"%ADB%" -s %T% shell "for p in com.amazon.spiderpork com.amazon.bueller.music com.amazon.shoptv.firetv.client com.amazon.smarthomemapviewapp com.amazon.bueller.photos com.amazon.tv.earlyaccess com.amazon.minitv.android.app com.amazon.gamehub com.amazon.tahoe com.amazon.whasettings com.amazon.tv.releasenotes com.amazon.tv.ftvambient; do pm disable-user --user 0 $p >/dev/null 2>&1 && echo $p; done" > "%BK%\apps-desativados.txt"

:pronto
echo.
echo ==========================================
echo    Pronto! Aperte o botao Home no controle.
echo ==========================================
echo Dica: por seguranca, voce ja pode desligar a Depuracao ADB na TV.
echo O RinoStick continua funcionando.

:fim
"%ADB%" kill-server >nul 2>&1
echo.
pause
