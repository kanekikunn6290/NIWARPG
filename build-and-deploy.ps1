# MMORPG Plugin ビルド・デプロイスクリプト
# 機能: Maven ビルド → JAR コピー → サーバーリロード

param(
    [string]$ServerPath = "C:\Users\hikar\Downloads\TestNIWARPG\TestNIWARPG",
    [string]$ProjectPath = "C:\Users\hikar\OneDrive\デスクトップ\MMORPG"
)

$pluginsDir = Join-Path $ServerPath "plugins"
$jarFile = "MMORPG-1.0.0.jar"
$jarSource = Join-Path $ProjectPath "target" $jarFile
$jarDest = Join-Path $pluginsDir $jarFile

# 色出力用
$successColor = 'Green'
$errorColor = 'Red'
$infoColor = 'Cyan'

Write-Host "===============================================" -ForegroundColor $infoColor
Write-Host "MMORPG Plugin ビルド・デプロイスクリプト" -ForegroundColor $infoColor
Write-Host "===============================================" -ForegroundColor $infoColor
Write-Host ""

# ===== ステップ1: Maven ビルド =====
Write-Host "[1/3] Maven ビルド実行中..." -ForegroundColor $infoColor
Set-Location $ProjectPath

$buildOutput = & mvn clean package -DskipTests 2>&1
$buildSuccess = $LASTEXITCODE -eq 0

if ($buildSuccess) {
    Write-Host "[✓] ビルド成功！" -ForegroundColor $successColor
} else {
    Write-Host "[✗] ビルド失敗" -ForegroundColor $errorColor
    Write-Host "ビルド出力:" -ForegroundColor $errorColor
    Write-Host $buildOutput
    exit 1
}

Write-Host ""

# ===== ステップ2: JAR ファイルコピー =====
Write-Host "[2/3] JAR ファイルをプラグインフォルダにコピー中..." -ForegroundColor $infoColor

if (-not (Test-Path $jarSource)) {
    Write-Host "[✗] ソースJARが見つかりません: $jarSource" -ForegroundColor $errorColor
    exit 1
}

if (-not (Test-Path $pluginsDir)) {
    Write-Host "[✗] プラグインディレクトリが見つかりません: $pluginsDir" -ForegroundColor $errorColor
    exit 1
}

try {
    # 既存のJARファイルがあれば削除
    if (Test-Path $jarDest) {
        Remove-Item $jarDest -Force
        Write-Host "  - 旧ファイルを削除しました" -ForegroundColor $infoColor
    }
    
    # JAR をコピー
    Copy-Item $jarSource $jarDest -Force
    Write-Host "[✓] JAR ファイルをコピーしました" -ForegroundColor $successColor
    Write-Host "  - コピー先: $jarDest" -ForegroundColor $infoColor
} catch {
    Write-Host "[✗] コピー処理でエラー: $_" -ForegroundColor $errorColor
    exit 1
}

Write-Host ""

# ===== ステップ3: サーバーリロード =====
Write-Host "[3/3] サーバーをリロード中..." -ForegroundColor $infoColor

$rconPath = Join-Path $ServerPath "rcon-cli.exe"
$serverPropsPath = Join-Path $ServerPath "server.properties"

# RCON ポート取得
if (Test-Path $serverPropsPath) {
    $rconPort = (Select-String -Path $serverPropsPath -Pattern "rcon\.port=(\d+)" | ForEach-Object { $_.Matches.Groups[1].Value }) -join ""
    $rconPassword = (Select-String -Path $serverPropsPath -Pattern "rcon\.password=(.+)" | ForEach-Object { $_.Matches.Groups[1].Value }) -join ""
} else {
    Write-Host "[⚠] server.properties が見つかりません。RCON 情報を使用できません" -ForegroundColor 'Yellow'
    Write-Host "    server.properties パス: $serverPropsPath" -ForegroundColor 'Yellow'
}

# RCON でリロードコマンド実行
if (-not (Test-Path $rconPath)) {
    Write-Host "[⚠] rcon-cli.exe が見つかりません。手動でリロードしてください" -ForegroundColor 'Yellow'
    Write-Host "    サーバーコンソールで以下のコマンドを実行してください:" -ForegroundColor 'Yellow'
    Write-Host "    /reload confirm" -ForegroundColor 'Yellow'
} else {
    try {
        $reloadOutput = & $rconPath -H 127.0.0.1 -P 25575 -p password "reload confirm" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[✓] サーバーをリロードしました" -ForegroundColor $successColor
        } else {
            Write-Host "[⚠] RCON でリロードに失敗しました" -ForegroundColor 'Yellow'
            Write-Host "    手動でリロードしてください: /reload confirm" -ForegroundColor 'Yellow'
        }
    } catch {
        Write-Host "[⚠] リロード実行時にエラー: $_" -ForegroundColor 'Yellow'
        Write-Host "    手動でリロードしてください: /reload confirm" -ForegroundColor 'Yellow'
    }
}

Write-Host ""
Write-Host "===============================================" -ForegroundColor $successColor
Write-Host "✓ ビルド・デプロイが完了しました！" -ForegroundColor $successColor
Write-Host "===============================================" -ForegroundColor $successColor
