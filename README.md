<div align="center">

# 收纳管家 · Storage Manager

**把家里每一件东西放对地方，到期前提醒你，用完前提醒你补货。**

一款本地优先的 Android 收纳与库存管理应用：物品建档、区域归档、保质期/临期提醒、消耗品补货清单、桌面小组件，全离线可用。

<br/>

<img src="https://img.shields.io/badge/Kotlin-2.2.21-2E9E76?logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.12-2E9E76"/>
<img src="https://img.shields.io/badge/Room-2.8.2-2E9E76"/>
<img src="https://img.shields.io/badge/minSdk-26-2E9E76"/>
<img src="https://img.shields.io/badge/License-MIT-2E9E76"/>

<br/>

</div>

---

## 为什么做它

家里的日用品、药品、囤货总是：「东西在哪？」「这个是不是过期了？」「抽纸是不是快没了？」

收纳管家把这些变成三件自动发生的事：

1. **归档** —— 物品放进「主区域 · 子区域」（如 `卫生间 · 台盆`），从哪拿的放回哪，找东西不再是翻箱倒柜。
2. **临期提醒** —— 设置有效期，到点提醒你；每件物品还能单独设置提前量（药品提前 30 天、食品 3 天就够）。
3. **补货清单** —— 标记为消耗品，数量低于阈值自动进「需补货」，用完了就知道该买。

数据全部存在本地，无账号、无联网、无广告，你的库存清单永远属于你。

---

## 功能特性

| 模块 | 说明 |
|---|---|
| **物品管理** | 名称 / 标签 / 数量 / 金额 / 规格 / 备注，拍照或相册存图 |
| **区域归档** | 主区域 → 子区域两级，存放位置显示为 `卫生间 · 台盆` |
| **到期提醒** | 每日通知临期/过期物品；每件可单独设提前量（1/3/7/15/30 天） |
| **补货清单** | 消耗品 + 阈值，数量不足自动入「需补货」，一键筛选 |
| **浪费分析** | 近 3 个月过期丢弃的件数 / 金额 / 高频品类，帮你看见浪费 |
| **多标签** | 自定义标签（食品 / 可囤货 / 电子…），按标签筛选 |
| **历年今日** | 每年同一天的购买记录回溯 |
| **搜索 / 筛选** | 名称搜索 + 状态 / 标签 / 补货多维筛选 |
| **桌面小组件** | 桌面直接看临期 / 需补货数量，点击直达临期页 |
| **扫保质期** | 本地离线 OCR（PaddleOCR PP-OCRv4）识别生产日期 / 保质期自动回填（测试中） |
| **自动备份** | 每周自动备份到指定目录，支持手动备份 / 恢复 |

---

## 技术栈

- **语言 / 框架**：Kotlin 2.2.21 · Jetpack Compose（BOM 2025.12）· Material 3 · 单 Activity
- **构建**：AGP 8.13.2 · Gradle 8.14.3 · Version Catalog（`libs.versions.toml`）
- **数据**：Room 2.8.2（Kapt）+ DataStore Preferences，无网络依赖
- **导航**：Navigation 3
- **通知 / 提醒**：AlarmManager 精确闹钟 + BootReceiver 重启重排，Android 12+ 权限降级链
- **桌面小组件**：Glance AppWidget
- **离线 OCR**：OnnxRuntime 1.20 + OpenCV 4.9 + PaddleOCR PP-OCRv4（本地推理）
- **架构**：`data` / `domain` / `ui` 分层，手动单例注入（无 Hilt/Koin），`domain` 层纯函数全单测

---

## 工程结构

```
app/src/main/java/com/shouna/manager/
├── data/          # Room 实体 / DAO / 仓库 / 设置
├── domain/        # 纯 Kotlin 业务逻辑（有效期计算、浪费分析、标签等）
├── ui/            # Compose 界面（screens / components / theme）
├── navigation/    # AppNav 底部导航 + 路由
├── reminder/      # 到期提醒（闹钟 / 通知 / 开机重排）
├── backup/        # 自动备份 / 恢复
├── ocr/           # PaddleOCR 本地推理引擎
├── widget/        # 桌面小组件
└── ShounaApp.kt   # 应用入口
```

---

## 构建

```bash
# 需要 JDK 17+ 与 Android SDK（compileSdk 36）
./gradlew assembleDebug
# 安装
./gradlew installDebug
```

> **关于 OCR 模型**：识别所需的 `det.onnx` / `rec.onnx` 来自 [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)（Apache-2.0）的 PP-OCRv4 mobile 模型，已置于 `app/src/main/assets/ppocr/`。

---

## 开源协议

本项目以 **MIT** 协议开源，详见 [LICENSE](LICENSE)。

<br/>

<div align="center">

Made with love by [@benyichan](https://github.com/benyichan) — 一个把 AI 用在真实生活里的财务人。

</div>
