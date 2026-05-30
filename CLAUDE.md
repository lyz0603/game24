# CLAUDE.md

## 项目概述

24点计算器 — Android Jetpack Compose 应用，枚举四则运算组合求解 24 点。

## 构建命令

```bash
export ANDROID_HOME=/Users/mac/Library/Android/sdk
export JAVA_HOME=$(/usr/libexec/java_home)
gradle assembleDebug         # 构建 Debug APK
gradle assembleDebug --no-daemon  # 无守护进程构建
```

## 核心架构

- **入口**：[MainActivity.kt](app/src/main/java/com/game24/MainActivity.kt)
- **求解引擎**：[Solver.kt](app/src/main/java/com/game24/engine/Solver.kt) — Rational 分数类 + 5 种括号结构 + Coroutines 并行
- **状态管理**：[GameViewModel.kt](app/src/main/java/com/game24/viewmodel/GameViewModel.kt) — MutableStateFlow + 输入校验
- **UI**：[GameScreen.kt](app/src/main/java/com/game24/ui/screen/GameScreen.kt) — 4×OutlinedTextField + Button + LazyColumn

## 图标

前景 PNG 由 `/tmp/gen_icons.py` 用思源黑体 Heavy 渲染，需 Pillow 库。

## 关键注意事项

- Java 版本：系统 Java 23（valhalla），已移除 foojay-resolver-convention 插件避免下载 JDK 21
- 构建使用系统 Gradle（9.4.1），非 wrapper（wrapper 下载可能超时）
- compileSdk = 36，targetSdk = 36，minSdk = 24
