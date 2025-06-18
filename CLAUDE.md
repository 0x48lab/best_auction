# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェク概要

Best Auctionは、Minecraft Paper/Spigotサーバー用のオークションハウスプラグインです。Kotlinで書かれており、プレイヤーがアイテムを出品・入札・購入できるシステムを提供します。

## ビルド・実行コマンド

### 基本的なGradleコマンド
- `./gradlew build` - プロジェクトをビルド（shadowJarを含む）
- `./gradlew shadowJar` - Fat JARファイルを生成（全依存関係を含む）
- `./gradlew runServer` - 開発用Minecraftサーバーを起動（1.20版）

### 開発用コマンド
- プラグインJAR: `build/libs/best_auction-1.0-SNAPSHOT-all.jar`
- サーバー設定ディレクトリ: `run/`
- プラグイン設定ディレクトリ: `run/plugins/best_auction/`

## アーキテクチャ

このプラグインは以下の主要コンポーネントで構成されています：

### データベース層
- **DatabaseManager**: SQLiteデータベースの初期化・管理
- **Tables.kt**: Exposed ORMを使用したテーブル定義
  - AuctionItems: オークションアイテム情報
  - Bids: 入札情報  
  - MailBox: メールシステム
  - AuctionSettings: オークション設定
  - PlayerLanguageSettings: プレイヤー言語設定

### 管理クラス
- **AuctionManager**: オークションの出品・入札・キャンセル処理
- **MailManager**: アイテムメール送信システム

### UI・ハンドラー
- **AuctionUI**: メインオークションハウスGUI
- **LanguageSettingsUI**: 言語設定GUI
- **BidHandler**: 入札処理イベントハンドラー
- **SearchHandler**: 検索機能ハンドラー

### データクラス
- **AuctionData.kt**: オークション関連のデータクラス定義
- **ItemUtils**: アイテム関連のユーティリティ（シリアライゼーション、価格計算等）

### 多言語対応
- **LangManager**: 言語管理システム
- 言語ファイル: `src/main/resources/lang/` (en.yml, ja.yml)

## 技術スタック

- **言語**: Kotlin 1.9.24
- **ビルドツール**: Gradle with Kotlin DSL
- **フレームワーク**: PaperMC API 1.20.6
- **データベース**: Jetbrains Exposed ORM + SQLite
- **経済プラグイン**: Vault API連携

## 設定ファイル

- `src/main/resources/config.yml`: メイン設定（手数料、期間、UI設定等）
- `src/main/resources/plugin.yml`: Bukkitプラグイン定義
- `run/plugins/best_auction/config.yml`: 実行時設定ファイル

## 開発時の注意事項

- データベース操作は全て`transaction`ブロック内で実行
- アイテムのシリアライゼーション/デシリアライゼーションは`ItemUtils`を使用
- 多言語メッセージは`LangManager`経由で取得
- 経済システムはVault APIを通じてアクセス
- UIはMinecraftのインベントリGUIシステムを使用