# Research: オークションカテゴリの簡素化

**Date**: 2026-03-24
**Feature**: 001-simplify-auction-categories

## Decision 1: カテゴリ分類のアーキテクチャ

**Decision**: 新しい`AuctionCategory` enumを6値（ALL, BLOCKS, FOOD, WEAPONS_EQUIPMENT, ENCHANTMENTS, OTHERS）で再定義し、旧enumから置き換える。

**Rationale**: 既存のenum構造を維持しつつ値を変更することで、コード全体への影響を最小限に抑えられる。enumの`fromItemStack()`メソッドに「エンチャント判定を最優先」というロジックを追加することで、新しい分類要件を満たせる。

**Alternatives considered**:
- 旧enumを残して新enumへのマッピング層を追加 → 二重管理になるため却下
- カテゴリをconfig.ymlで設定可能にする → 過度な汎用化。YAGNIに反する

## Decision 2: エンチャント判定の優先度

**Decision**: `fromItemStack()`でエンチャントの有無を**最初に**チェックし、エンチャント付きアイテムはすべてENCHANTMENTSに分類する。

**Rationale**: 仕様上「エンチャントが付与された全アイテム」が対象。ItemStackの`enchantments`プロパティとItemMetaの`hasEnchants()`で判定可能。エンチャント本はMaterial.ENCHANTED_BOOKで、それ以外のエンチャント付きアイテムはItemMeta.hasEnchants()で判定する。

**Alternatives considered**:
- アイテムごとに個別判定 → 複雑で漏れやすい
- StoredEnchantMeta のみチェック → 通常のエンチャント付きアイテムを見落とす

## Decision 3: データベース互換性戦略

**Decision**: ランタイムマッピング方式。DBの旧カテゴリ名はそのまま保持し、読み取り時に新カテゴリへ変換する。新規出品は新カテゴリ名で保存する。

**Rationale**: DBマイグレーションを不要にすることで、プラグイン更新時のリスクを最小化。`getActiveListings()`のクエリで複数の旧カテゴリ名をOR条件でフィルタリングすれば実現可能。

**Alternatives considered**:
- DB一括UPDATE → プラグイン起動時にマイグレーション実行が必要。失敗時のリスクが高い
- 新テーブル作成 → 過度な変更。既存スキーマで十分対応可能

## Decision 4: UI構造の変更

**Decision**: メインUI（openMainUI）を「全アイテム一覧 + カテゴリフィルタボタン」構造に変更する。

**Rationale**: 仕様FR-003「初期表示はすべてカテゴリ」を満たすため。54スロットインベントリで、上部36スロットにアイテム一覧、下段にカテゴリフィルタボタン（6つ）＋ナビゲーションボタンを配置。

**Layout**:
- Slot 0-35: アイテム一覧（最大36件/ページ）
- Slot 45: すべて (CHEST)
- Slot 46: ブロック (STONE)
- Slot 47: 食品 (APPLE)
- Slot 48: 武器装備 (DIAMOND_SWORD)
- Slot 49: エンチャント (ENCHANTED_BOOK)
- Slot 50: その他 (LAVA_BUCKET)
- Slot 51: 検索 (SPYGLASS)
- Slot 52: メールボックス (CHEST → ENDER_CHEST に変更して「すべて」のCHESTと区別)
- Slot 53: 設定 (WRITABLE_BOOK)

**Alternatives considered**:
- 2段階UI維持（カテゴリ選択→アイテム一覧）→ 仕様の「初期表示は全アイテム」に反する
- カテゴリを上段、アイテムを下段 → Minecraftの慣習に反する（アイテムが上、ナビが下）

## Decision 5: ソート順

**Decision**: すべてのカテゴリビューで新着順（createdAtの降順）をデフォルトにする。

**Rationale**: 仕様FR-002で「すべて」カテゴリは新着順と明記。他カテゴリでもユーザー体験を統一するため同じソート順を採用。現在のソートは`currentPrice ASC, createdAt ASC`だが、新着順に変更する。
