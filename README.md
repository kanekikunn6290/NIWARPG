# MMORPG Plugin Project

Minecraft 1.20.6 Paper用のMMORPGプラグイン開発プロジェクトです。

## プロジェクト構成

- `build.gradle.kts` - Gradle設定ファイル (Paper API依存管理)
- `src/main/java/com/mmorpg/` - Javaソースコードディレクトリ
- `src/main/resources/plugin.yml` - プラグイン設定ファイル

## セットアップ手順

### 1. 必要な環境
- Java 21以上
- Gradle (自動ダウンロード可能)
- Git (推奨)

### 2. プロジェクト初期化

```bash
# Gradleラッパーがない場合
gradle wrapper

# 依存関係をダウンロード
./gradlew build
```

### 3. ビルド方法

```bash
# プラグインをビルド
./gradlew build

# ビルド後のJARファイルはbuild/libs/MMORPG-1.0.0.jarに出力されます
```

### 4. Paperサーバーで実行

```bash
# Paper serverをダウンロード (初回のみ)
java -Xmx1024M -Xms1024M -jar paper-1.20.6.jar nogui

# プラグインJARをserverのpluginsフォルダにコピー
cp build/libs/MMORPG-1.0.0.jar ./server/plugins/

# サーバーを起動
cd server
java -Xmx1024M -Xms1024M -jar paper-1.20.6.jar nogui
```

## 開発ガイド

### プラグインの基本構成

1. **MMORPGPlugin.java** - メインのプラグインクラス
   - `onEnable()` - プラグイン起動時の処理
   - `onDisable()` - プラグイン停止時の処理

2. **PlayerEventListener.java** - イベントリスナーの例
   - プレイヤーのJoin/Leave時の処理

### 機能追加例

#### コマンド実装
```java
getCommand("mmorpg").setExecutor((sender, cmd, label, args) -> {
    // コマンド処理
    return true;
});
```

#### イベントリスナー
```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    // イベント処理
}
```

#### タスクスケジューリング
```java
getServer().getScheduler().runTaskTimer(this, () -> {
    // 定期実行処理
}, 0L, 20L); // 1秒ごとに実行
```

## 推奨される開発構造

```
src/main/
├── java/com/mmorpg/
│   ├── MMORPGPlugin.java (メインクラス)
│   ├── listener/
│   │   ├── PlayerEventListener.java
│   │   └── BlockEventListener.java
│   ├── command/
│   │   ├── MainCommand.java
│   │   └── AdminCommand.java
│   ├── manager/
│   │   ├── PlayerManager.java
│   │   ├── QuestManager.java
│   │   └── InventoryManager.java
│   ├── model/
│   │   ├── Player.java
│   │   ├── Quest.java
│   │   └── Item.java
│   └── util/
│       ├── ConfigUtil.java
│       └── MessageUtil.java
└── resources/
    ├── plugin.yml
    └── config.yml
```

## 参考リンク

- [Paper API Documentation](https://papermc.io/docs/paper/latest/)
- [Spigot Wiki](https://www.spigotmc.org/wiki)
- [Paper GitHub](https://github.com/PaperMC/Paper)

## トラブルシューティング

### Gradle同期エラー
```bash
./gradlew clean
./gradlew build
```

### Paper APIが見つからない
- `build.gradle.kts`のmavenリポジトリ設定を確認
- インターネット接続を確認

### プラグインが読み込まれない
- `plugin.yml`の`main`クラス名が正しいか確認
- コンソールでエラーメッセージを確認
