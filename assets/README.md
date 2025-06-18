# 🎨 Best Auction - アイコンアセット

## 📁 ファイル構成

```
assets/
├── icon.svg           # ベクター版（編集用）
├── icon-512.png       # Modrinth用高解像度
├── icon-128.png       # GitHub用標準
├── icon-64.png        # 小サイズ用
└── icon-32.png        # ファビコン用
```

## 🎨 デザイン仕様

### カラーパレット
- **背景**: 濃紺→紫のグラデーション (#1a237e → #4a148c)
- **ダイヤモンド**: 水色グラデーション (#4dd0e1 → #0097a7)
- **ハンマー**: ゴールドグラデーション (#ffd54f → #ffb300)
- **コイン**: ゴールド (#ffd700)
- **ハイライト**: ホワイト (#ffffff)

### デザイン要素
1. **メインダイヤモンド**: Minecraftスタイルの3Dダイヤモンド
2. **オークションハンマー**: 右上に配置された金色のハンマー
3. **通貨コイン**: 複数通貨対応を示すコイン ($、¥、€)
4. **スパークル効果**: 高級感を演出する光る粒子
5. **テキスト**: "AUCTION" の文字（オプション）

## 🖼️ PNG変換方法

### オプション1: オンラインツール
1. [Convertio](https://convertio.co/svg-png/) でSVGをアップロード
2. サイズを512x512に設定
3. PNGでダウンロード

### オプション2: コマンドライン (ImageMagick)
```bash
# 512x512
magick icon.svg -resize 512x512 icon-512.png

# 128x128
magick icon.svg -resize 128x128 icon-128.png

# 64x64
magick icon.svg -resize 64x64 icon-64.png

# 32x32
magick icon.svg -resize 32x32 icon-32.png
```

### オプション3: Inkscape
```bash
# 512x512
inkscape icon.svg --export-png=icon-512.png --export-width=512 --export-height=512

# 128x128
inkscape icon.svg --export-png=icon-128.png --export-width=128 --export-height=128
```

## 📋 使用場所

### Modrinth
- **プロジェクトアイコン**: icon-512.png
- **ギャラリー**: 複数サイズ対応

### GitHub
- **リポジトリアイコン**: icon-128.png
- **READMEバッジ**: icon-64.png

### プラグイン内
- **GUIアイコン**: icon-32.png
- **通知アイコン**: icon-64.png

## 🎯 アイコンの特徴

1. **視認性**: 小さなサイズでも認識しやすいシンプルなデザイン
2. **Minecraftらしさ**: ダイヤモンドとピクセルアート風の表現
3. **オークション要素**: ハンマーと通貨で機能を明確に表現
4. **プロフェッショナル**: 信頼性を感じられる高級感のある色合い
5. **国際性**: 複数通貨表示で多言語対応をアピール

## 🔧 編集方法

SVGファイルは以下で編集可能：
- **Figma**: ブラウザベースのデザインツール
- **Inkscape**: 無料のベクターエディタ
- **Adobe Illustrator**: プロフェッショナル版
- **VS Code**: SVG直接編集

色やサイズの調整は、SVGファイル内の該当部分を変更してから再出力してください。