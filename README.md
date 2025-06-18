# 🏪 Best Auction Plugin

[![Release](https://img.shields.io/github/v/release/0x48lab/best_auction)](https://github.com/0x48lab/best_auction/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/best-auction?logo=modrinth&label=downloads)](https://modrinth.com/plugin/best-auction)
[![License](https://img.shields.io/github/license/0x48lab/best_auction)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)

完全な機能を備えたMinecraft Paper/Spigotサーバー用オークションハウスプラグインです。プレイヤーがアイテムを出品・入札・購入できる包括的な市場システムを提供します。

## ✨ 主な機能

- 🏪 **完全なオークションシステム** - 出品、入札、即決購入
- 💰 **Vault経済連携** - 既存の経済プラグインとシームレスに統合
- 📬 **メールシステム** - アイテムと返金の自動配送
- 🌐 **多言語対応** - 日本語・英語完全対応
- 🔍 **高度な検索** - カテゴリ別検索とキーワード検索
- ⏰ **残り時間表示** - リアルタイムでオークション終了時間を表示
- ⚙️ **高度な設定** - 手数料、期間、通貨表示など細かく調整可能
- 📊 **K/M/B表記** - 大きな金額の見やすい表示

## 🛠️ 必要要件

- **Minecraft**: 1.20.1+
- **サーバー**: Paper/Spigot
- **Java**: 17+
- **依存関係**: 
  - Vault（推奨）
  - 経済プラグイン（EssentialsX等）

## 📥 インストール

### Modrinthから（推奨）
1. [Modrinthページ](https://modrinth.com/plugin/best-auction)からダウンロード
2. `plugins/`フォルダに配置
3. サーバーを再起動

### GitHubから
1. [Releases](https://github.com/0x48lab/best_auction/releases)から最新版をダウンロード
2. `plugins/`フォルダに配置
3. サーバーを再起動

## 🎮 使い方

### 基本コマンド
```
/ah                          # オークションハウスを開く
/ah sell <価格> [即決価格]      # アイテムを出品
/ah bid <ID> <金額>           # アイテムに入札
/ah search <キーワード>        # アイテムを検索
/ah mail                     # メールボックスを開く
```

### 設定例
```yaml
# config.yml
auction:
  default_duration: 168      # 7日間（時間単位）
  listing_fee_rate: 0.05     # 5%の手数料
  
ui:
  date_format: "yyyy年MM月dd日"  # 日付表示形式
  
language: "ja"               # 言語設定
```

## 📚 ドキュメント

詳細な使用方法と設定については、[PLUGIN_DOCUMENTATION.md](PLUGIN_DOCUMENTATION.md)をご覧ください。

## 🔧 開発

### ビルド
```bash
git clone https://github.com/0x48lab/best_auction.git
cd best_auction
./gradlew shadowJar
```

### 開発サーバー起動
```bash
./gradlew runServer
```

### プロジェクト構造
```
src/main/kotlin/com/hacklab/best_auction/
├── Main.kt                 # メインクラス
├── commands/              # コマンド処理
├── managers/              # ビジネスロジック
├── ui/                    # GUI関連
├── utils/                 # ユーティリティ
├── data/                  # データクラス
└── database/              # データベース定義
```

## 🚀 デプロイメント

このプロジェクトは GitHub Actions を使用して自動デプロイを行います：

1. `v*`タグをプッシュ（例：`v1.0.0`）
2. 自動的にビルドが実行
3. GitHub Releases にリリース作成
4. Modrinth に自動デプロイ

### リリース手順
```bash
git tag v1.0.0
git push origin v1.0.0
```

## 🤝 コントリビューション

1. このリポジトリをフォーク
2. フィーチャーブランチを作成 (`git checkout -b feature/amazing-feature`)
3. 変更をコミット (`git commit -m 'Add amazing feature'`)
4. ブランチにプッシュ (`git push origin feature/amazing-feature`)
5. プルリクエストを作成

## 📄 ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。詳細は [LICENSE](LICENSE) ファイルをご覧ください。

## 🆘 サポート

- 🐛 バグ報告: [GitHub Issues](https://github.com/0x48lab/best_auction/issues)
- 💡 機能要望: [GitHub Discussions](https://github.com/0x48lab/best_auction/discussions)
- 📧 直接連絡: プロジェクト管理者まで

## 🏆 謝辞

このプラグインは [Claude Code](https://claude.ai/code) の支援を受けて開発されました。

---

**🌟 このプラグインが役に立ったら、ぜひスターをお願いします！**