# Best Auction クラウド連携設定ガイド

このガイドでは、Best Auctionプラグインのクラウド連携機能を設定する手順を説明します。

## 📋 前提条件

- Best Auctionプラグインがインストール済み
- サーバーがインターネットに接続可能
- Cloudflare Workersアカウント（無料プランでOK）

## 🚀 設定手順

### ステップ1: Cloudflare Workersのセットアップ

1. [Cloudflare](https://www.cloudflare.com/)にアクセスしてアカウントを作成
2. ダッシュボードから「Workers & Pages」を選択
3. 新しいWorkerを作成
4. Best Auction用のWorkerコードをデプロイ（[こちら](https://github.com/0x48lab/best-auction-cloud-worker)から取得）

### ステップ2: APIトークンの取得

#### 方法1: コンソールコマンドを使用（推奨）

1. Minecraftサーバーのコンソールで以下を実行：
   ```
   ah cloud gettoken
   ```

2. 表示されるURLとインストラクションに従ってトークンを取得

3. 取得したトークンをコンソールで設定：
   ```
   ah cloud settoken YOUR_API_TOKEN_HERE
   ```

#### 方法2: 手動で設定ファイルを編集

1. サーバーを停止
2. `plugins/best_auction/config.yml`を開く
3. 以下の設定を編集

### ステップ3: config.ymlの設定

```yaml
# クラウド連携設定
cloud:
  # クラウド連携を有効化
  enabled: true
  
  # Cloudflare WorkersのベースURL
  # 例: https://best-auction.your-username.workers.dev
  base_url: "https://your-worker.your-subdomain.workers.dev"
  
  # APIトークン（ステップ2で取得したもの）
  api_token: "your-api-token-here"
  
  # サーバー識別子（複数サーバーがある場合は各サーバーで異なる値に）
  server_id: "server-1"
  
  # 認証設定
  auth:
    # 起動時にトークンを自動検証
    validate_on_startup: true
    
    # トークン再検証間隔（分）
    revalidate_interval: 60
  
  # データ同期設定
  sync:
    # 初回起動時に既存データを自動同期
    auto_sync_on_startup: true
    
    # バッチサイズ（一度に送信するオークション数）
    batch_size: 50
    
    # バッチ間の待機時間（ミリ秒）
    batch_delay: 1000
    
    # 入札情報も同期する
    include_bids: true
  
  # HTTPリクエストタイムアウト（ミリ秒）
  timeout: 5000
  
  # リトライ設定
  retry_attempts: 3
  retry_delay: 1000
  
  # イベントタイプ別の有効/無効設定
  events:
    item_listed: true      # アイテム出品時
    bid_placed: true       # 入札時
    bid_cancelled: true    # 入札キャンセル時
    auction_cancelled: true # オークションキャンセル時
    item_sold: true        # 落札時
```

### ステップ4: 設定の確認

1. サーバーを起動

2. 以下のコマンドで連携状態を確認：
   ```
   /ah cloud status
   ```

3. トークンの検証：
   ```
   /ah cloud validate
   ```

### ステップ5: 初回データ同期（オプション）

既存のオークションデータをクラウドに同期する場合：

```
/ah cloud sync
```

強制的に全データを再同期する場合：

```
/ah cloud sync force
```

## 🔍 動作確認

1. アイテムを出品してみる：
   ```
   /ah sell 1000
   ```

2. サーバーログを確認：
   - `[CloudEvent] Event sent successfully: ITEM_LISTED` が表示されればOK

3. Cloudflare Workersのログでリクエストを確認

## ❌ トラブルシューティング

### エラー: "Cloud integration is disabled in config"
→ `config.yml`で`cloud.enabled: true`に設定

### エラー: "Cloud base URL not configured"
→ `cloud.base_url`にWorkerのURLを設定

### エラー: "Cloud API token not configured"
→ `cloud.api_token`にトークンを設定

### エラー: "Cloud API token is not valid"
→ トークンが無効。新しいトークンを取得して設定

### エラー: "timestamp: Invalid datetime format"
→ プラグインを最新版にアップデート（タイムスタンプ形式が修正済み）

### イベントが送信されない
1. `/ah cloud status`で状態を確認
2. `cloud.events`セクションで該当イベントがtrueになっているか確認
3. サーバーログで`[CloudEvent]`メッセージを確認

## 📊 ログの確認方法

詳細なログを表示するには、`server.properties`で以下を設定：

```properties
# ログレベルをFINEに設定（デバッグ用）
java.util.logging.ConsoleHandler.level=FINE
```

または、`plugins/best_auction/`にログファイルが生成される場合はそちらを確認。

## 🔒 セキュリティ上の注意

- APIトークンは秘密情報です。GitHubなどに公開しないでください
- トークンが漏洩した場合は、Cloudflare Workersで新しいトークンを生成してください
- HTTPSを使用してデータは暗号化されています

## 📞 サポート

問題が解決しない場合は：
1. [GitHub Issues](https://github.com/0x48lab/best_auction/issues)で報告
2. 詳細なエラーログを含めてください