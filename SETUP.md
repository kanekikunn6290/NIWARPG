# Build environment setup for MMORPG Plugin Development

ワークスペース環境:
- Java 21以上がインストール済み
- Paper API 1.20.6対応
- Gradle Kotlinスクリプト形式

## プロジェクト構造確認済み
✓ build.gradle.kts - Gradle設定
✓ settings.gradle.kts - 設定ファイル
✓ src/main/java/com/mmorpg/ - Javaソースコード
✓ src/main/resources/plugin.yml - プラグイン設定

## 次のステップ

1. **ローカル環境でビルド**
   ```
   ./gradlew build
   ```

2. **Paper Serverをダウンロード** (別途)
   - https://papermc.io/downloads/paper

3. **プラグインをデプロイ**
   - build/libs/MMORPG-1.0.0.jar を server/plugins/ にコピー

## VS Code設定推奨事項

VS Code拡張機能:
- Extension Pack for Java
- Gradle for Java
- Minecraft Development (optional)
