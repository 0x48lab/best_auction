# Implementation Plan: オークションカテゴリの簡素化

**Branch**: `001-simplify-auction-categories` | **Date**: 2026-03-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-simplify-auction-categories/spec.md`

## Summary

現在の10カテゴリ（BUILDING_BLOCKS, DECORATIONS, REDSTONE, TRANSPORTATION, MISCELLANEOUS, FOOD, TOOLS, COMBAT, BREWING, ENCHANTED_BOOKS）を6カテゴリ（ALL, BLOCKS, FOOD, WEAPONS_EQUIPMENT, ENCHANTMENTS, OTHERS）に簡素化する。メインUIを「カテゴリ選択画面」から「全アイテム新着順一覧 + カテゴリフィルタ」に変更。エンチャント付きアイテムは種類を問わずENCHANTMENTSカテゴリに分類する。DBスキーマ変更は不要で、ランタイムマッピングで旧データとの後方互換性を維持する。

## Technical Context

**Language/Version**: Kotlin 1.9.24
**Primary Dependencies**: PaperMC API 1.21.4, Jetbrains Exposed ORM 0.44.1, Vault API
**Storage**: SQLite (Exposed ORM経由)
**Testing**: 手動テスト（Minecraftサーバー上で動作確認）
**Target Platform**: PaperMC / Spigot 1.21.x サーバー
**Project Type**: Minecraft サーバープラグイン
**Performance Goals**: UI操作は即座に応答（体感遅延なし）
**Constraints**: 54スロットインベントリUI制約、子供ユーザーへの配慮、多言語対応（EN/JA）
**Scale/Scope**: 小〜中規模サーバー（数十人〜数百人規模）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution未設定（テンプレートのまま）のため、ゲートチェックはスキップ。

## Project Structure

### Documentation (this feature)

```text
specs/001-simplify-auction-categories/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Technical decisions
├── data-model.md        # Phase 1: Entity model & mapping
├── quickstart.md        # Phase 1: Implementation guide
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
src/main/kotlin/com/hacklab/best_auction/
├── data/
│   └── AuctionData.kt          # ★ AuctionCategory enum変更
├── managers/
│   └── AuctionManager.kt       # ★ getActiveListings()フィルタ更新
├── ui/
│   └── AuctionUI.kt            # ★ メインUI構造変更、カテゴリフィルタ
├── handlers/
│   ├── BidHandler.kt           # 変更なし
│   └── SearchHandler.kt        # 変更なし
├── database/
│   ├── DatabaseManager.kt      # 変更なし
│   └── Tables.kt               # 変更なし（スキーマ不変）
└── Main.kt                     # 変更なし

src/main/resources/
├── lang/
│   ├── en.yml                   # ★ カテゴリ名更新
│   └── ja.yml                   # ★ カテゴリ名更新
├── config.yml                   # 変更なし
└── plugin.yml                   # 変更なし
```

**Structure Decision**: 既存のプロジェクト構造をそのまま維持。新規ファイルの作成は不要。既存ファイルの修正のみで完結する。

## Complexity Tracking

該当なし。Constitution未設定のため violations なし。
