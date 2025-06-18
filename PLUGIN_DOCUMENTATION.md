# Best Auction Plugin ドキュメント

## 📋 概要

Best AuctionはMinecraft Paper/Spigotサーバー用のオークションハウスプラグインです。プレイヤーがアイテムを出品・入札・購入できる完全な市場システムを提供します。

### 主な機能
- 📦 アイテムの出品・入札システム
- 💰 Vault連携による経済システム
- 📬 メールボックスシステム
- 🌐 多言語対応（日本語・英語）
- ⚙️ 高度な設定機能
- 🔍 検索・カテゴリ機能

---

## 🛠️ インストール・設定

### 必要なプラグイン
- **Paper/Spigot** 1.20.6以上
- **Vault** （経済プラグイン連携用）
- **経済プラグイン**（EssentialsX等）

### インストール手順
1. `best_auction-1.0-SNAPSHOT-all.jar` を `plugins/` フォルダに配置
2. サーバーを再起動
3. `plugins/best_auction/config.yml` で設定を調整

---

## ⚙️ 設定ファイル

### config.yml 主要設定

```yaml
# 言語設定
language: "ja"  # ja: 日本語, en: 英語

# オークション設定
auction:
  default_duration: 228  # オークション期間（時間）
  listing_fee_rate: 0.05 # 出品手数料率（5%）
  min_listing_fee: 10    # 最小手数料
  max_listing_fee: 1000  # 最大手数料

# 通貨設定
currency:
  use_vault_format: true      # Vault形式を使用
  fallback_currency: "gil"    # フォールバック通貨名
  currency_symbol: ""         # 通貨記号

# UI設定
ui:
  items_per_page: 45          # 1ページあたりのアイテム数
  date_format: "yyyy年MM月dd日" # 日付表示形式

# デバッグ設定
debug:
  enable_debug_commands: false # デバッグコマンド有効化
```

### 日付フォーマット例
- `"yyyy-MM-dd"` → 2024-12-18
- `"yyyy年MM月dd日"` → 2024年12月18日
- `"MM/dd/yyyy"` → 12/18/2024
- `"dd/MM/yyyy"` → 18/12/2024

---

## 💬 コマンド一覧

### 基本コマンド

| コマンド | 説明 | 例 |
|---------|------|-----|
| `/ah` | オークションハウスGUIを開く | `/ah` |
| `/ah sell <価格> [即決価格]` | 手に持ったアイテムを出品 | `/ah sell 1000 2000` |
| `/ah bid <ID> <金額>` | アイテムに入札 | `/ah bid 23 1500` |
| `/ah cancel <ID>` | 出品をキャンセル | `/ah cancel 23` |
| `/ah search <キーワード>` | アイテムを検索 | `/ah search ダイヤモンド` |
| `/ah mail` | メールボックスを開く | `/ah mail` |
| `/ah confirm` | 操作を確認実行 | `/ah confirm` |
| `/ah help` | ヘルプを表示 | `/ah help` |

### 管理者コマンド

| コマンド | 権限 | 説明 |
|---------|------|-----|
| `/ah testdata [個数]` | `auction.admin` | テストデータ生成（デバッグモード時のみ） |

### 金額表記について
- **K/M/B表記対応**: `7.2K`（7,200）、`1.5M`（1,500,000）
- **自動省略表示**: 1000以上は自動でK/M表記に変換

---

## 🎮 プレイヤー向け使い方

### 1. アイテムの出品

1. 出品したいアイテムを手に持つ
2. `/ah sell <開始価格> [即決価格]` を実行
3. 出品手数料が自動で徴収される
4. オークション期間は設定で統一（デフォルト228時間）

**例:**
```
/ah sell 1000      # 1000gilから開始、即決なし
/ah sell 1K 2.5K   # 1000gilから開始、2500gilで即決
```

### 2. 入札・購入

1. `/ah` でオークションハウスを開く
2. 気になるアイテムを左クリック
3. 入札情報を確認して金額を入力
4. 右クリックで即決購入（即決価格設定時）

**入札画面の情報:**
- 現在の最高入札額
- 最低入札額
- 即決価格（設定時）
- 残り時間

### 3. 入札管理

1. `/ah` → 「入札中アイテム」をクリック
2. **左クリック**: 入札額を変更
3. **右クリック**: 入札をキャンセル

**入札状態の表示:**
- ✓ 最高入札中（緑色）
- ✗ 上回られています（赤色）

### 4. 検索機能

```
/ah search ダイヤモンド   # キーワード検索
/ah search エンチャント   # 部分一致検索
```

### 5. メールボックス

- 落札したアイテム
- 返金されたお金
- キャンセルされたアイテム

すべてメールボックスに送信されます。

---

## 🔧 管理者向け設定

### 1. 経済設定の調整

```yaml
auction:
  listing_fee_rate: 0.05  # 5%の手数料
  min_listing_fee: 10     # 最小10gil
  max_listing_fee: 1000   # 最大1000gil
```

### 2. オークション期間の変更

```yaml
auction:
  default_duration: 168   # 7日間（168時間）
  # default_duration: 72  # 3日間
  # default_duration: 24  # 1日間
```

### 3. 言語の切り替え

```yaml
language: "en"  # 英語に変更
language: "ja"  # 日本語に変更
```

### 4. デバッグモードの有効化

```yaml
debug:
  enable_debug_commands: true  # 開発環境でのみtrue
```

---

## 🛡️ 権限システム

### 基本権限
- `bestauction.use` - 基本的な使用権限
- `auction.admin` - 管理者権限（testdataコマンド等）

### 権限の設定例（LuckPerms）
```
/lp group default permission set bestauction.use true
/lp group admin permission set auction.admin true
```

---

## 📊 データベース

### SQLiteテーブル構造

**AuctionItems（オークションアイテム）**
- ID、出品者、アイテムデータ、価格、期限など

**Bids（入札）**
- 入札者、入札額、作成日時など

**MailBox（メール）**
- 受信者、アイテム、理由、日時など

**設定関連**
- プレイヤー言語設定
- その他設定データ

---

## 🔧 トラブルシューティング

### よくある問題

**Q: オークションハウスが開かない**
A: Vaultと経済プラグインがインストールされているか確認

**Q: 通貨表示がおかしい**
A: `config.yml`の`currency`設定を確認

**Q: 日付表示を変更したい**
A: `ui.date_format`を変更してサーバー再起動

**Q: testdataコマンドが使えない**
A: `debug.enable_debug_commands: true`に設定

### ログの確認
```
tail -f logs/latest.log | grep "best_auction"
```

---

## 🚀 開発者向け情報

### ビルド方法
```bash
./gradlew build           # プロジェクトをビルド
./gradlew shadowJar       # Fat JARを生成
./gradlew runServer       # 開発サーバー起動
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

### 技術スタック
- **Kotlin** 1.9.24
- **Gradle** with Kotlin DSL
- **PaperMC API** 1.20.6
- **Jetbrains Exposed** ORM
- **Vault API** 経済連携

---

## 📝 更新履歴

### v1.0.0
- 基本的なオークション機能
- 入札システム
- メールボックス
- 多言語対応

### v1.1.0（最新）
- 設定可能な日付フォーマット
- 設定可能なオークション期間
- 残り時間表示機能
- デバッグモード制御
- K/M/B表記対応
- Vault通貨連携強化

---

## 🆘 サポート

### 問題報告
- [GitHub Issues](https://github.com/your-repo/best_auction/issues)
- プラグイン作者まで連絡

### 設定ファイルの場所
- `plugins/best_auction/config.yml`
- `plugins/best_auction/lang/`

### バックアップ推奨
- データベースファイル: `plugins/best_auction/database.db`
- 設定ファイル: `plugins/best_auction/config.yml`

---

## 📄 ライセンス

MIT License - 詳細は LICENSE ファイルを参照

---

*このドキュメントは Best Auction Plugin v1.1.0 に基づいています。最新情報は公式リポジトリをご確認ください。*