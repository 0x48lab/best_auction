# Tasks: オークションカテゴリの簡素化

**Input**: Design documents from `/specs/001-simplify-auction-categories/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Not requested - test tasks are not included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project structure changes needed. Existing codebase is the starting point.

(No setup tasks - existing project structure is maintained as-is)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: AuctionCategory enum再定義と関連インフラの更新。全User Storyの前提条件。

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T001 AuctionCategory enumを6値（ALL, BLOCKS, FOOD, WEAPONS_EQUIPMENT, ENCHANTMENTS, OTHERS）に再定義し、displayNameとmaterialプロパティを新カテゴリに合わせて更新する in `src/main/kotlin/com/hacklab/best_auction/data/AuctionData.kt`
- [x] T002 fromItemStack()メソッドを新カテゴリ用に書き換える。判定優先度: (1)エンチャント判定（hasEnchants() or ENCHANTED_BOOK → ENCHANTMENTS）、(2)食品（isEdible → FOOD）、(3)武器装備（剣・弓・防具・ツール → WEAPONS_EQUIPMENT）、(4)ブロック（isBlock or 装飾系 → BLOCKS）、(5)フォールバック → OTHERS in `src/main/kotlin/com/hacklab/best_auction/data/AuctionData.kt`
- [x] T003 旧カテゴリ名から新カテゴリへの変換マップをAuctionCategoryのcompanion objectに追加する。fromLegacyName(name: String): AuctionCategory メソッドを実装 in `src/main/kotlin/com/hacklab/best_auction/data/AuctionData.kt`
- [x] T004 [P] en.ymlのcategoryセクションを新カテゴリに更新する（all: "All", blocks: "Blocks", food: "Food", weapons_equipment: "Weapons & Equipment", enchantments: "Enchantments", others: "Others"）。不要な旧カテゴリキーを削除する in `src/main/resources/lang/en.yml`
- [x] T005 [P] ja.ymlのcategoryセクションを新カテゴリに更新する（all: "すべて", blocks: "ブロック", food: "食品", weapons_equipment: "武器装備", enchantments: "エンチャント", others: "その他"）。不要な旧カテゴリキーを削除する in `src/main/resources/lang/ja.yml`

**Checkpoint**: AuctionCategory enumが新6カテゴリで定義され、fromItemStack()が新ロジックで動作し、言語ファイルが更新されている

---

## Phase 3: User Story 1 - すべてのアイテムを新着順で閲覧 (Priority: P1) 🎯 MVP

**Goal**: オークションハウスを開くと、全アイテムが新着順で一覧表示される

**Independent Test**: オークションハウスを開き、全アイテムが出品日時の新しい順で表示されることを確認。アイテムがない場合は空メッセージが表示される。

### Implementation for User Story 1

- [x] T006 [US1] getActiveListings()メソッドのソート順をcreatedAt DESC（新着順）に変更する。category=nullの場合は全アイテムを返す既存動作を維持 in `src/main/kotlin/com/hacklab/best_auction/managers/AuctionManager.kt`
- [x] T007 [US1] openMainUI()メソッドを書き換え、カテゴリ選択画面から全アイテム一覧表示に変更する。上部スロット(0-35)にgetActiveListings(category=null)の結果をページネーション表示、下段(45-53)にナビゲーションボタンを配置する in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T008 [US1] getCategoryDisplayName()メソッドを新6カテゴリ対応に更新する。ALLカテゴリのLangManagerキーも追加 in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T009 [US1] handleMainMenuClick()メソッドを更新し、アイテム一覧画面でのアイテムクリック（入札/購入）とナビゲーションボタンクリックを処理するよう変更する in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`

**Checkpoint**: オークションハウスを開くと全アイテムが新着順で表示される。ページネーション、アイテムクリック（入札/購入）が動作する。

---

## Phase 4: User Story 2 - 簡素化されたカテゴリでフィルタリング (Priority: P1)

**Goal**: 6つのカテゴリフィルタボタンで、アイテムをカテゴリ別に絞り込める

**Independent Test**: 各カテゴリボタンをクリックし、対応するカテゴリのアイテムのみが表示されることを確認。「すべて」ボタンでフィルタ解除。

### Implementation for User Story 2

- [x] T010 [US2] getActiveListings()にカテゴリフィルタロジックを追加する。新カテゴリ名を受け取り、対応する旧カテゴリ名も含むIN句でフィルタする（例: BLOCKS → category IN ('BLOCKS', 'BUILDING_BLOCKS', 'DECORATIONS')）。ALLの場合はフィルタなし in `src/main/kotlin/com/hacklab/best_auction/managers/AuctionManager.kt`
- [x] T011 [US2] openMainUI()の下段にカテゴリフィルタボタン6つを追加する。スロット45:すべて(CHEST), 46:ブロック(STONE), 47:食品(APPLE), 48:武器装備(DIAMOND_SWORD), 49:エンチャント(ENCHANTED_BOOK), 50:その他(LAVA_BUCKET)。選択中カテゴリはエンチャントグロウで強調表示 in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T012 [US2] createCategoryItem()メソッドを新カテゴリ対応に更新し、各カテゴリのアイコンアイテム生成ロジックを変更する in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T013 [US2] handleMainMenuClick()にカテゴリフィルタボタンのクリック処理を追加する。カテゴリボタンクリック時に選択カテゴリでアイテム一覧を再描画する in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T014 [US2] openMainUI()にカテゴリパラメータを追加し、選択中カテゴリを保持してページネーション時にもフィルタが維持されるようにする in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`
- [x] T015 [US2] メールボックス・出品中・入札中・設定・検索ボタンの配置を新UIレイアウトに合わせて調整する（スロット51-53等） in `src/main/kotlin/com/hacklab/best_auction/ui/AuctionUI.kt`

**Checkpoint**: カテゴリボタンクリックで対応するアイテムのみ表示される。「すべて」でフィルタ解除。ページ遷移でもカテゴリ維持。

---

## Phase 5: User Story 3 - 既存データの互換性維持 (Priority: P2)

**Goal**: 旧カテゴリ名でDBに保存されたアイテムが新カテゴリ体系で正しく表示される

**Independent Test**: 旧カテゴリ名（BUILDING_BLOCKS, REDSTONE等）のデータがある状態で、新カテゴリUIで正しくフィルタリングされることを確認。全アイテムが漏れなく表示される。

### Implementation for User Story 3

- [x] T016 [US3] getActiveListings()のカテゴリフィルタIN句に全旧カテゴリ名が漏れなく含まれていることを確認・修正する。特にENCHANTMENTS→('ENCHANTMENTS','ENCHANTED_BOOKS')のマッピングを検証 in `src/main/kotlin/com/hacklab/best_auction/managers/AuctionManager.kt`
- [x] T017 [US3] 出品時（listItemメソッド）にAuctionCategory.fromItemStack()で取得した新カテゴリ名がDBに保存されることを確認する。category.nameが新enum値名になっていることを検証 in `src/main/kotlin/com/hacklab/best_auction/managers/AuctionManager.kt`

**Checkpoint**: 旧カテゴリ名のアイテムが新カテゴリで正しく表示される。新規出品は新カテゴリ名で保存される。データ消失ゼロ。

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: ビルド確認と最終調整

- [x] T018 プロジェクトをビルドしてコンパイルエラーがないことを確認する (`./gradlew build`)
- [x] T019 AuctionCategory enumの全参照箇所を検索し、旧カテゴリ値への参照が残っていないことを確認する。SearchHandler等の関連ファイルに影響がないか検証

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: 開始可能 - AuctionCategory enumと言語ファイルの更新
- **US1 (Phase 3)**: Phase 2完了後 - 新enumに依存
- **US2 (Phase 4)**: Phase 3完了後 - メインUI構造に依存
- **US3 (Phase 5)**: Phase 4完了後 - フィルタロジックに依存
- **Polish (Phase 6)**: Phase 5完了後 - 全変更完了後にビルド確認

### User Story Dependencies

- **US1 (P1)**: Phase 2完了後に開始可能。他のUS非依存。
- **US2 (P1)**: US1のUI構造変更に依存（同じファイルを大幅変更するため）
- **US3 (P2)**: US2のフィルタロジック完了に依存（IN句の正確性を検証するため）

### Within Each User Story

- AuctionManager変更 → AuctionUI変更の順（データ層→UI層）
- 同一ファイル内のタスクは順序実行

### Parallel Opportunities

- T004とT005は並列実行可能（en.ymlとja.ymlは別ファイル）
- Phase 2内でT001-T003（AuctionData.kt）とT004-T005（lang files）は並列実行可能

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational（enum + lang files）
2. Complete Phase 3: User Story 1（全アイテム新着順表示）
3. **STOP and VALIDATE**: オークションハウスを開いて全アイテムが新着順で表示されることを確認
4. この時点で最小限の動作するプラグインが得られる

### Incremental Delivery

1. Phase 2 → enum基盤完成
2. Phase 3 (US1) → 全アイテム一覧表示 → 動作確認（MVP!）
3. Phase 4 (US2) → カテゴリフィルタ追加 → 動作確認
4. Phase 5 (US3) → 旧データ互換性確認 → 動作確認
5. Phase 6 → ビルド確認・最終検証

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1とUS2は同一ファイル（AuctionUI.kt）を大幅変更するため、並列実行は非推奨
- 旧カテゴリのエンチャント付きアイテム（旧COMBAT/TOOLSでDB保存済み）は、DB上のカテゴリ名を信頼する方針。新規出品のみ正確なエンチャント判定を適用
- Minecraftサーバーの起動はユーザー自身が行う。`./gradlew runServer`は使用しない
