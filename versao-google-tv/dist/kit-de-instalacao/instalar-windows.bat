@echo off
setlocal
REM RinoStick - instalador para Windows (versao Google TV / Android TV)
REM Como usar: de dois cliques neste arquivo. Ele baixa sozinho o que faltar
REM (ferramentas ADB do Google e o app), instala o RinoStick na TV e libera
REM as permissoes. Requer Windows 10 ou 11.

cd /d "%~dp0"
set "APK=RinoStick-GoogleTV.apk"
set "APK_URL=https://github.com/henriquesevero/rinostick-app/releases/latest/download/RinoStick-GoogleTV.apk"
set "PT_URL=https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
set "PKG=com.rinostick.app"
set "ADB=%~dp0platform-tools\adb.exe"

echo.
echo ==========================================
echo    RinoStick - instalacao na TV (Google)
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

REM 3. IP da TV
echo Na TV, o IP fica em Configuracoes - Rede e Internet - ^(sua rede^).
echo Se a TV usar "Depuracao sem fio", digite o IP:porta mostrado nela.
set "IP="
set /p "IP=Digite o IP da TV (ex.: 192.168.0.50) e aperte Enter: "
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
echo  - se a Depuracao USB esta ligada ^(Configuracoes - Sistema - Opcoes do desenvolvedor^);
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
REM Fire OS / Android antigo (ex.: Fire OS 6): confere e, se faltar, grava direto no banco de ajustes
call :fixsecure enabled_accessibility_services %PKG%/%PKG%.HomeRedirectService
call :fixsecure enabled_notification_listeners %PKG%/%PKG%.NowPlayingListener

echo - Abrindo o RinoStick...
"%ADB%" -s %T% shell am start -n %PKG%/.MainActivity >nul 2>&1
timeout /t 3 /nobreak >nul
REM Protetor de tela "Aurora" (o app guarda o original na primeira abertura)
"%ADB%" -s %T% shell "ss=%PKG%/%PKG%.Screensaver; [ \"$(settings get secure screensaver_components)\" = \"$ss\" ] || settings put secure screensaver_components $ss"

:pronto
echo.
echo ==========================================
echo    Pronto! Aperte o botao Home no controle.
echo ==========================================
echo Dica: por seguranca, voce ja pode desligar a Depuracao USB na TV.
echo O RinoStick continua funcionando.

:fim
"%ADB%" kill-server >nul 2>&1
echo.
pause
exit /b 0

:fixsecure
set "CUR="
for /f "delims=" %%v in ('call "%ADB%" -s %T% shell settings get secure %1') do set "CUR=%%v"
echo %CUR% | find "%~2" >nul && exit /b 0
set "NEW=%~2"
echo %CUR% | find "/" >nul && set "NEW=%CUR%:%~2"
"%ADB%" -s %T% shell content insert --uri content://settings/secure --bind name:s:%1 --bind value:s:%NEW%
exit /b 0
