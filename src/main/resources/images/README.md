# 應用程式圖示資源

本目錄包含 ETF Holdings Tracker 應用程式所需的圖示與啟動畫面資源。

## 檔案說明

| 檔案 | 用途 | 格式 |
|------|------|------|
| `app-icon.svg` | 應用程式圖示原始檔 | SVG 向量圖 |
| `app-icon.ico` | Windows 應用程式圖示 | ICO (多尺寸) |
| `app-icon.png` | PNG 格式圖示 (256x256) | PNG |
| `splash.svg` | 啟動畫面原始檔 | SVG 向量圖 |
| `splash.png` | 啟動畫面圖片 (600x400) | PNG |

## 圖示轉換

### 從 SVG 轉換為 ICO

使用線上工具或 ImageMagick：

```bash
# 使用 ImageMagick 轉換
magick convert app-icon.svg -define icon:auto-resize=256,128,64,48,32,16 app-icon.ico
```

### 線上轉換工具

- [CloudConvert](https://cloudconvert.com/svg-to-ico)
- [ConvertICO](https://convertico.com/svg-to-ico/)
- [RealFaviconGenerator](https://realfavicongenerator.net/)

## 設計規範

### 圖示尺寸要求

| 平台 | 尺寸 |
|------|------|
| Windows 桌面 | 256x256, 128x128, 64x64, 48x48, 32x32, 16x16 |
| Windows 工作列 | 32x32, 24x24, 16x16 |
| Windows 安裝程式 | 256x256 |

### 色彩規範

| 元素 | 顏色 | 用途 |
|------|------|------|
| 主色 | #2563EB (Blue 600) | 主要背景 |
| 深色 | #1E40AF (Blue 800) | 內層背景 |
| 強調色 | #60A5FA (Blue 400) | 趨勢線、資料點 |
| 成功色 | #10B981 (Emerald 500) | 上升指標 |
| 文字色 | #FFFFFF | 白色文字 |

## 使用方式

### 在 jpackage 中使用

pom.xml 配置：

```xml
<icon>${project.basedir}/src/main/resources/images/app-icon.ico</icon>
```

### 在 JavaFX 中使用

```java
// 設定應用程式圖示
stage.getIcons().add(new Image(
    getClass().getResourceAsStream("/images/app-icon.png")
));
```

## 注意事項

1. ICO 檔案必須包含多種尺寸以支援不同的 Windows DPI 設定
2. 啟動畫面建議使用 PNG 格式以確保相容性
3. 圖示應在淺色和深色背景上都清晰可見
