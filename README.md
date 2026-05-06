# AnimaForge

<div align="center">

**🎨 Anima 文生图模型的移动端 ComfyUI 提示词工作站**

[中文](#中文) | [English](#english) | [日本語](#日本語)

</div>

---

## 📱 截图 Preview / プレビュー

<div align="center">

| 主页 Home | 侧边栏 Drawer | 编辑 Editor | 详情 Detail | 设置 Settings |
|:---:|:---:|:---:|:---:|:---:|
| ![主页](p1.png) | ![侧边栏](p2.png) | ![编辑](p3.png) | ![详情](p4.png) | ![设置](p5.png) |

</div>

---

## 中文

### 简介

**AnimaForge** 是一款专为 [Anima](https://anima.mooshieblob.com/) 文生图模型设计的 Android 提示词管理工具，深度集成 ComfyUI 工作流，让你在手机上高效管理、创作和生成 AI 图片。

### 核心功能

- **提示词管理** — 本地数据库存储正向/负向/风格提示词，支持标签分类、搜索筛选、收藏置顶
- **ComfyUI 集成** — 一键连接 ComfyUI 服务器，提交生成任务，实时查看进度与历史
- **风格库** — 内置上千位画师风格数据，支持搜索、预览、一键添加
- **多主题** — 蓝白二次元 / 樱花物语 / 暗夜幻境 / 薄荷深海 四套主题，随心切换
- **工作流管理** — 保存和管理 ComfyUI 工作流 JSON，快速切换
- **数据安全** — SQLite + Room 本地存储，支持 JSON 导入导出和自动备份

### 技术栈

- **语言**: Kotlin 100%
- **UI**: Jetpack Compose (Material 3)
- **架构**: MVVM (ViewModel + StateFlow)
- **数据库**: Room (SQLite) + DataStore Preferences
- **网络**: OkHttp + Retrofit
- **图片**: Coil
- **最低支持**: Android 8.0 (API 26)

### 致谢

本应用的风格标签数据来源于 **[Anima](https://anima.mooshieblob.com/)** 项目。感谢 Anima 团队为 AI 艺术社区提供的优秀模型与数据支持。

---

## English

### Introduction

**AnimaForge** is an Android prompt management tool purpose-built for the [Anima](https://anima.mooshieblob.com/) text-to-image model, with deep ComfyUI workflow integration. Manage, craft, and generate AI images right from your phone.

### Core Features

- **Prompt Management** — Local SQLite storage for positive/negative/style prompts with tag categorization, search, favorites, and pinning
- **ComfyUI Integration** — One-tap connection to ComfyUI servers, submit generation tasks, monitor progress and history in real-time
- **Artist Style Library** — 1000+ artist style tags with search, preview, and one-tap addition
- **Multi-Theme** — Choose from Blue-White Anime / Sakura / Dark Neon / Mint Ocean
- **Workflow Manager** — Save and manage ComfyUI workflow JSON presets
- **Data Safety** — SQLite + Room local storage, JSON import/export, automatic backups

### Tech Stack

- **Language**: Kotlin 100%
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Database**: Room (SQLite) + DataStore Preferences
- **Network**: OkHttp + Retrofit
- **Image Loading**: Coil
- **Min SDK**: Android 8.0 (API 26)

### Acknowledgments

Artist style data is sourced from the **[Anima](https://anima.mooshieblob.com/)** project. Special thanks to the Anima team for their outstanding model and data contributions to the AI art community.

---

## 日本語

### 概要

**AnimaForge** は、[Anima](https://anima.mooshieblob.com/) テキスト画像生成モデル専用の Android プロンプト管理ツールです。ComfyUI ワークフローと深く統合し、スマートフォンから AI 画像の管理・作成・生成を効率的に行えます。

### 主な機能

- **プロンプト管理** — ポジティブ/ネガティブ/スタイルのプロンプトをローカル SQLite に保存。タグ分類、検索フィルター、お気に入り登録、ピン留めに対応
- **ComfyUI 連携** — ワンタップで ComfyUI サーバーに接続し、生成タスクを送信。進捗と履歴をリアルタイムで確認
- **アーティストスタイルライブラリ** — 1000以上のアーティストスタイルタグを搭載。検索、プレビュー、ワンタップ追加が可能
- **マルチテーマ** — 青白アニメ / 桜物語 / ダークネオン / ミントオーシャンの4テーマを自由に切替
- **ワークフロー管理** — ComfyUI ワークフロー JSON を保存・管理し、素早く切替
- **データ保護** — SQLite + Room によるローカル保存、JSON インポート/エクスポート、自動バックアップ対応

### 技術スタック

- **言語**: Kotlin 100%
- **UI**: Jetpack Compose (Material 3)
- **アーキテクチャ**: MVVM (ViewModel + StateFlow)
- **データベース**: Room (SQLite) + DataStore Preferences
- **ネットワーク**: OkHttp + Retrofit
- **画像読込**: Coil
- **最小 SDK**: Android 8.0 (API 26)

### 謝辞

本アプリのスタイルタグデータは **[Anima](https://anima.mooshieblob.com/)** プロジェクトから提供されています。Anima チームの AI アートコミュニティへの素晴らしい貢献に感謝いたします。

---

## 🚀 快速开始 Quick Start / クイックスタート

1. Clone 项目 / Clone repository / リポジトリをクローン
```bash
git clone https://github.com/yourusername/AnimaForge.git
```
2. Android Studio 打开项目 / Open in Android Studio / Android Studio で開く
3. Sync Gradle → Run on device

ComfyUI 服务端需自行部署。请在设置中填写你的 ComfyUI 服务器地址。

---

<div align="center">
Made with ❤️ for the AI art community
</div>
