# 🎨 Best Auction Plugin - アイコンデザイン提案

## 📋 デザインコンセプト

### 🎯 デザインの方向性
- **Minecraftスタイル**: ピクセルアート風のデザイン
- **オークション要素**: ハンマー、コイン、チェストなどの象徴的アイテム
- **プロフェッショナル**: 信頼性を感じられる色合い
- **視認性**: 小さなサイズでも認識しやすい

## 🎨 アイコンデザイン案

### 案1: オークションハンマー + コイン
```
背景: 深いブルー (#1a237e)
メイン: 金色のハンマー (#ffd700)
アクセント: 銀色のコイン (#c0c0c0)
```

**説明**: クラシックなオークションハンマーを中央に配置し、周りにコインを散りばめたデザイン。オークションの象徴的なアイテムで一目でわかりやすい。

### 案2: チェスト + 価格タグ
```
背景: 暖かいオレンジ (#ff8f00)
メイン: 茶色のチェスト (#8d6e63)
アクセント: 緑色の価格タグ (#4caf50)
```

**説明**: Minecraftのチェストに価格タグが付いたデザイン。アイテム保管と価格設定を同時に表現。

### 案3: ダイヤモンド + ハンマー (推奨)
```
背景: グラデーション (濃紺から紫 #1a237e → #4a148c)
メイン: 水色のダイヤモンド (#00bcd4)
シンボル: 金色のハンマー (#ffd700)
```

**説明**: Minecraftで最も価値のあるダイヤモンドとオークションハンマーを組み合わせ。高級感と機能性を両立。

## 🖼️ 推奨デザイン仕様

### サイズ要件
- **Modrinth**: 最小128x128px、推奨512x512px
- **GitHub**: 128x128px
- **フォーマット**: PNG (透明背景対応)

### カラーパレット
```css
プライマリ: #1a237e (深いブルー)
セカンダリ: #ffd700 (ゴールド)
アクセント: #00bcd4 (水色)
背景: #4a148c (パープル)
テキスト: #ffffff (ホワイト)
```

## 🎯 最終推奨デザイン

### デザイン: "ダイヤモンド・オークション"
- **中央**: 大きな水色ダイヤモンド (Minecraftスタイル)
- **右上**: 小さな金色ハンマー
- **背景**: 濃紺から紫へのグラデーション
- **エフェクト**: ダイヤモンドの周りに微細な光る粒子
- **フォント**: Minecraft風のピクセルフォント

### SVGコード (ベース)
```svg
<svg width="512" height="512" xmlns="http://www.w3.org/2000/svg">
  <!-- 背景グラデーション -->
  <defs>
    <radialGradient id="bg" cx="50%" cy="30%">
      <stop offset="0%" style="stop-color:#4a148c"/>
      <stop offset="100%" style="stop-color:#1a237e"/>
    </radialGradient>
  </defs>
  
  <!-- 背景 -->
  <rect width="512" height="512" fill="url(#bg)" rx="64"/>
  
  <!-- メインダイヤモンド -->
  <path d="M256 80 L380 200 L256 380 L132 200 Z" 
        fill="#00bcd4" stroke="#ffffff" stroke-width="4"/>
  
  <!-- ダイヤモンドの内部線 -->
  <path d="M194 200 L256 140 L318 200 M194 200 L318 200" 
        stroke="#ffffff" stroke-width="2" opacity="0.6"/>
  
  <!-- ハンマー -->
  <g transform="translate(360,120) scale(0.8)">
    <rect x="0" y="0" width="40" height="20" fill="#ffd700"/>
    <rect x="15" y="20" width="10" height="60" fill="#8d6e63"/>
  </g>
  
  <!-- 光る粒子 -->
  <circle cx="200" cy="150" r="3" fill="#ffffff" opacity="0.8"/>
  <circle cx="320" cy="180" r="2" fill="#ffffff" opacity="0.6"/>
  <circle cx="180" cy="280" r="2" fill="#ffffff" opacity="0.7"/>
  <circle cx="340" cy="300" r="3" fill="#ffffff" opacity="0.5"/>
</svg>
```

## 🛠️ 作成方法

### オプション1: AI画像生成
```
プロンプト: "Minecraft style pixel art icon for auction house plugin, featuring a cyan diamond and golden hammer, dark blue to purple gradient background, 512x512px, clean and professional"
```

### オプション2: 手動作成ツール
- **Figma**: ベクターデザイン
- **Photoshop**: ピクセルアート
- **GIMP**: 無料代替
- **Aseprite**: ピクセルアート専用

### オプション3: Minecraft風ジェネレーター
- MinecraftSkinEditor
- Blockbench (3Dモデル→2Dアイコン)

## 📁 ファイル出力

以下のファイルを準備：
```
icons/
├── icon-512.png    # Modrinth用高解像度
├── icon-128.png    # GitHub用標準
├── icon-64.png     # 小サイズ用
├── icon-32.png     # ファビコン用
└── icon.svg        # ベクター版（編集用）
```

## 🎨 代替案

時間が限られている場合のシンプル案：

### シンプル版1: テキストロゴ
- 背景: 濃紺
- テキスト: "BA" (Best Auction)
- フォント: Minecraft風
- 装飾: 小さなハンマーアイコン

### シンプル版2: Emojiベース
- 背景: グラデーション
- メイン: 🔨 (ハンマー絵文字を大きく)
- アクセント: 💎 (ダイヤモンド絵文字)

これらのデザイン案から、Best Auctionにふさわしいプロフェッショナルなアイコンを作成できます！