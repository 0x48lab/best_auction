# Quickstart: オークションカテゴリの簡素化

**Date**: 2026-03-24
**Feature**: 001-simplify-auction-categories

## 変更対象ファイル一覧

### 必須変更

1. **`src/main/kotlin/com/hacklab/best_auction/data/AuctionData.kt`**
   - AuctionCategory enumの値を10→6に変更
   - fromItemStack()のロジック書き換え（エンチャント判定を最優先に）
   - 旧カテゴリ名マッピング用のcompanion objectメソッド追加

2. **`src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`**
   - openMainUI(): カテゴリ一覧 → アイテム一覧 + カテゴリフィルタバー
   - openCategoryUI(): 新カテゴリ対応
   - getCategoryDisplayName(): 新カテゴリの表示名マッピング
   - createCategoryItem(): 新カテゴリのアイコン生成
   - handleMainMenuClick(): クリック処理をカテゴリフィルタ対応に変更
   - handleCategoryClick(): 必要に応じて調整

3. **`src/main/kotlin/com/hacklab/best_auction/managers/AuctionManager.kt`**
   - getActiveListings(): カテゴリフィルタを複数旧カテゴリ名対応に
   - ソート順をcreatedAt DESC（新着順）に変更

4. **`src/main/resources/lang/en.yml`**
   - categoryセクションの値を新カテゴリに更新

5. **`src/main/resources/lang/ja.yml`**
   - categoryセクションの値を新カテゴリに更新

### 変更不要

- `Tables.kt` - スキーマ変更なし
- `DatabaseManager.kt` - DB初期化ロジック変更なし
- `BidHandler.kt` - 入札処理はカテゴリに依存しない
- `SearchHandler.kt` - 検索はアイテム名ベースでカテゴリに依存しない

## 実装順序

1. AuctionCategory enum変更（データ層）
2. AuctionManager.getActiveListings()更新（ビジネスロジック層）
3. 言語ファイル更新（リソース層）
4. AuctionUI全体更新（UI層）

## ビルド・テスト

```bash
./gradlew build
# 生成物: build/libs/best_auction-*-all.jar
# サーバーのplugins/に配置してテスト
```
