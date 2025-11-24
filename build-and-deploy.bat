@echo off
REM MMORPG Plugin ビルド・デプロイスクリプト (バッチファイル版)

setlocal enabledelayedexpansion

set PROJECT_PATH=C:\Users\hikar\OneDrive\デスクトップ\MMORPG
set SERVER_PATH=C:\Users\hikar\Downloads\TestNIWARPG\TestNIWARPG
set PLUGINS_DIR=%SERVER_PATH%\plugins
set JAR_FILE=MMORPG-1.0.0.jar
set JAR_SOURCE=%PROJECT_PATH%\target\%JAR_FILE%
set JAR_DEST=%PLUGINS_DIR%\%JAR_FILE%

echo ===============================================
echo MMORPG Plugin ビルド・デプロイスクリプト
echo ===============================================
echo.

REM ===== ステップ1: Maven ビルド =====
echo [1/3] Maven ビルド実行中...
cd /d %PROJECT_PATH%

call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [エラー] ビルドに失敗しました
    pause
    exit /b 1
)

echo [完了] ビルド成功！
echo.

REM ===== ステップ2: JAR ファイルコピー =====
echo [2/3] JAR ファイルをプラグインフォルダにコピー中...

if not exist "%JAR_SOURCE%" (
    echo [エラー] ソースJARが見つかりません: %JAR_SOURCE%
    pause
    exit /b 1
)

if not exist "%PLUGINS_DIR%" (
    echo [エラー] プラグインディレクトリが見つかりません: %PLUGINS_DIR%
    pause
    exit /b 1
)

REM 既存のJARファイルがあれば削除
if exist "%JAR_DEST%" (
    del "%JAR_DEST%"
    echo 旧ファイルを削除しました
)

REM JAR をコピー
copy "%JAR_SOURCE%" "%JAR_DEST%"
if %errorlevel% neq 0 (
    echo [エラー] JAR コピーに失敗しました
    pause
    exit /b 1
)

echo [完了] JAR ファイルをコピーしました
echo コピー先: %JAR_DEST%
echo.

REM ===== ステップ3: サーバーリロード案内 =====
echo [3/3] サーバーをリロードしてください
echo.
echo サーバーコンソールまたはゲーム内で以下のコマンドを実行してください:
echo   /reload confirm
echo.
echo または RCON でリモートリロード:
echo   rcon-cli -H 127.0.0.1 -P 25575 -p [password] "reload confirm"
echo.

echo ===============================================
echo ✓ ビルド・デプロイが完了しました！
echo ===============================================
pause
