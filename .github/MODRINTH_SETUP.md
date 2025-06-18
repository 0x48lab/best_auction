# Modrinth Auto-Deploy Setup Guide

このガイドでは、GitHub ActionsからModrinthへの自動デプロイを設定する方法を説明します。

## 📋 必要な設定

### 1. Modrinthプロジェクトの作成

1. [Modrinth](https://modrinth.com) にアカウントを作成
2. 新しいプロジェクトを作成
3. プロジェクトIDをメモ（例：`best-auction`）

### 2. Modrinth APIトークンの取得

1. [Modrinth Settings](https://modrinth.com/settings/pats) にアクセス
2. 「Create a PAT」をクリック
3. 以下の権限を付与：
   - `CREATE_VERSION` - 新しいバージョンの作成
   - `EDIT_DETAILS` - プロジェクト詳細の編集
4. 生成されたトークンをコピー

### 3. GitHub Secretsの設定

GitHubリポジトリの Settings > Secrets and variables > Actions で以下を追加：

```
MODRINTH_TOKEN=<あなたのModrinthトークン>
MODRINTH_PROJECT_ID=<あなたのプロジェクトID>
```

### 4. ワークフローファイルの調整

`.github/workflows/release.yml` の以下の部分を調整：

```yaml
# Modrinth configuration
modrinth-id: ${{ secrets.MODRINTH_PROJECT_ID }}  # あなたのプロジェクトID
modrinth-token: ${{ secrets.MODRINTH_TOKEN }}

# Game versions（サポートするバージョンを調整）
game-versions: |
  1.20.6
  1.20.5
  1.20.4
  1.20.1

# Mod loaders（サポートするプラットフォームを調整）
loaders: |
  paper
  spigot
  bukkit
```

## 🚀 デプロイ手順

### 自動デプロイ
vタグをプッシュすると自動でデプロイされます：

```bash
# 例：バージョン1.0.0をリリース
git tag v1.0.0
git push origin v1.0.0
```

### 手動確認項目

デプロイ前に以下を確認：

- [ ] `build.gradle.kts`のバージョンが正しい
- [ ] `plugin.yml`の情報が最新
- [ ] ドキュメントが更新されている
- [ ] テストが通る（`./gradlew test`）

## 📝 Modrinthプロジェクト設定

### プロジェクト情報
- **名前**: Best Auction
- **説明**: Complete auction house plugin for Minecraft servers
- **カテゴリ**: Economy, Utility
- **ライセンス**: MIT
- **Issues URL**: `https://github.com/0x48lab/best_auction/issues`
- **Source URL**: `https://github.com/0x48lab/best_auction`
- **Wiki URL**: `https://github.com/0x48lab/best_auction/blob/main/PLUGIN_DOCUMENTATION.md`

### ギャラリー画像
以下のスクリーンショットを追加することを推奨：
- オークションハウスのメインGUI
- 入札画面
- 設定ファイルの例

## 🔧 トラブルシューティング

### よくある問題

**1. デプロイが失敗する**
- Modrinth APIトークンが正しく設定されているか確認
- プロジェクトIDが正しいか確認
- 権限が適切に設定されているか確認

**2. JARファイルが見つからない**
- Gradleビルドが成功しているか確認
- ファイルパスが正しいか確認（`build/libs/`）

**3. バージョンが重複している**
- 同じバージョン番号で複数回デプロイしていないか確認
- タグが正しく設定されているか確認

### ログの確認

GitHub Actions の実行ログでエラーの詳細を確認できます：
1. GitHub リポジトリの「Actions」タブ
2. 失敗したワークフローをクリック
3. 「deploy」ジョブのログを確認

## 📊 メトリクス

デプロイ後、以下のメトリクスを確認：
- ダウンロード数
- プロジェクトのフォロワー数
- バージョン別の利用状況

Modrinthのプロジェクトダッシュボードでこれらの情報を確認できます。

## 🔄 更新サイクル

推奨される更新サイクル：
- **パッチ版**（v1.0.1）: バグ修正
- **マイナー版**（v1.1.0）: 新機能追加
- **メジャー版**（v2.0.0）: 破壊的変更

各リリースでは適切なchangelogを提供し、ユーザーに変更内容を明確に伝えましょう。