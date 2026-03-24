# Data Model: オークションカテゴリの簡素化

**Date**: 2026-03-24
**Feature**: 001-simplify-auction-categories

## Entity: AuctionCategory (enum)

### 新カテゴリ定義

| Enum値 | 表示名 (EN) | 表示名 (JA) | アイコン Material | 含まれる旧カテゴリ |
|--------|------------|------------|-----------------|-----------------|
| ALL | All | すべて | CHEST | (フィルタなし) |
| BLOCKS | Blocks | ブロック | STONE | BUILDING_BLOCKS, DECORATIONS |
| FOOD | Food | 食品 | APPLE | FOOD |
| WEAPONS_EQUIPMENT | Weapons & Equipment | 武器装備 | DIAMOND_SWORD | COMBAT, TOOLS |
| ENCHANTMENTS | Enchantments | エンチャント | ENCHANTED_BOOK | ENCHANTED_BOOKS + エンチャント付き全アイテム |
| OTHERS | Others | その他 | LAVA_BUCKET | REDSTONE, TRANSPORTATION, BREWING, MISCELLANEOUS |

### 分類ルール（優先度順）

1. **エンチャント判定（最優先）**: `ItemMeta.hasEnchants()` または `Material == ENCHANTED_BOOK` → ENCHANTMENTS
2. **食品判定**: `Material.isEdible` → FOOD
3. **武器・装備判定**: 剣・弓・防具・ツール（ピッケル・斧・シャベル・鍬）等 → WEAPONS_EQUIPMENT
4. **ブロック判定**: `Material.isBlock` または装飾系アイテム → BLOCKS
5. **フォールバック**: 上記に該当しない → OTHERS

### 旧カテゴリマッピング（DB互換用）

旧カテゴリ名からの変換マップ:

```
BUILDING_BLOCKS → BLOCKS
DECORATIONS     → BLOCKS
FOOD            → FOOD
COMBAT          → WEAPONS_EQUIPMENT (※エンチャント付きはENCHANTMENTS)
TOOLS           → WEAPONS_EQUIPMENT (※エンチャント付きはENCHANTMENTS)
ENCHANTED_BOOKS → ENCHANTMENTS
REDSTONE        → OTHERS
TRANSPORTATION  → OTHERS
BREWING         → OTHERS
MISCELLANEOUS   → OTHERS
```

**注意**: 旧カテゴリではエンチャント判定を行っていなかったため、旧COMBAT/TOOLSのアイテムにエンチャントが付いているかはDBカテゴリ名だけでは判断できない。表示時にアイテムデータをデシリアライズしてエンチャント有無を再判定する必要がある。

## Entity: AuctionItems (テーブル - 変更なし)

スキーマ変更は不要。`category` カラム（varchar 50）はそのまま使用。

- **新規出品**: 新カテゴリ名（BLOCKS, FOOD等）で保存
- **既存データ**: 旧カテゴリ名（BUILDING_BLOCKS等）のまま保持
- **読み取り**: クエリ時に旧→新のマッピングを適用

### クエリパターン

カテゴリフィルタリングのSQL条件:

| 新カテゴリ | WHERE条件 |
|-----------|----------|
| ALL | (フィルタなし) |
| BLOCKS | category IN ('BLOCKS', 'BUILDING_BLOCKS', 'DECORATIONS') |
| FOOD | category IN ('FOOD') |
| WEAPONS_EQUIPMENT | category IN ('WEAPONS_EQUIPMENT', 'COMBAT', 'TOOLS') |
| ENCHANTMENTS | category IN ('ENCHANTMENTS', 'ENCHANTED_BOOKS') ※ + ランタイムでエンチャント再判定 |
| OTHERS | category IN ('OTHERS', 'REDSTONE', 'TRANSPORTATION', 'BREWING', 'MISCELLANEOUS') |

**ENCHANTMENTS特殊処理**: 旧COMBAT/TOOLSのエンチャント付きアイテムはDB上では旧カテゴリ名のまま。完全な分類にはアイテムデシリアライズ後のエンチャント判定が必要だが、パフォーマンスとの兼ね合いで、旧データについてはDB上のカテゴリ名を信頼するか、初回起動時にマイグレーションするかのトレードオフがある。推奨: 旧データはDB上のカテゴリを信頼し、新規出品のみ正確なエンチャント判定を適用する。
