# 24点计算器

一款基于 Jetpack Compose 的 Android 24 点游戏求解器。输入四个数字，枚举所有可能的四则运算组合，找出能得到 24 的解法。

## 功能

- 输入 4 个数字，一键计算所有 24 点解法
- 多线程并行枚举（Kotlin Coroutines），搜索 7680 种表达式
- 精确分数运算（Rational），避免浮点误差
- Material 3 设计，支持 Dynamic Color

## 技术栈

- Kotlin + Jetpack Compose
- MVVM 架构
- Coroutines 并行计算
- 版本目录（Version Catalog）依赖管理

## 构建

```bash
export ANDROID_HOME=/path/to/sdk
./gradlew assembleDebug
```

或直接在 Android Studio 中打开本项目运行。

## 算法

4 个数字通过 `+` `-` `×` `÷` 求 24：

| 维度 | 数量 | 说明 |
|------|------|------|
| 数字排列 | 4! = 24 | 全排列 |
| 运算符 | 4³ = 64 | 3 个位置各 4 种 |
| 括号结构 | 5 | 二叉树形态 |
| **总计** | **7680** | |

## 许可

MIT License
