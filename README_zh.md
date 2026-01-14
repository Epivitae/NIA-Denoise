# NIA: Neural Inference Assistant

![NIA Logo](src/main/resources/logo.png)

> **A Native, AI-Powered Denoising Solution for ImageJ/Fiji.**
>
> **无需 Python 环境 | 无需 CUDA 配置 | 开箱即用**

**NIA (Neural Inference Assistant)** 是一个轻量级但功能强大的 ImageJ 插件，旨在将深度学习去噪能力直接引入生物图像分析流程。它内置了高效的推理引擎（基于 ONNX Runtime），让科研人员无需配置复杂的 Python/Conda 环境，即可在任何电脑上运行先进的去噪算法。

---

## ✨ 核心功能 (Key Features)

* **⚡ Native Inference (原生推理)**: 内置 Microsoft ONNX Runtime 引擎，无需安装 Python，无需显卡配置，下载即用。
* **🧠 Built-in AI Model (内置模型)**: 预装 **DnCNN** 去噪模型，针对生物图像优化，即刻上手。
* **🛠️ Custom Model Support (自定义模型)**: 支持加载您自己训练的 `.onnx` 模型，扩展性强。
* **🎞️ 5D Hyperstack Support (全维度支持)**: 完美支持 X, Y, Channel, Z-Slice, Time-frame (5D) 数据，自动批量处理时间序列。
* **📊 Smart Normalization (智能归一化)**: 自动识别 8-bit / 16-bit / 32-bit 图像，动态计算归一化因子，防止数据过曝或视频闪烁。
* **🖥️ Professional GUI (专业界面)**: 采用现代化的 "WinMan" 风格界面，紧凑美观，操作直观。

---

## 📥 安装指南 (Installation)

1.  **下载**: 从 [Releases](../../releases) 页面下载最新的 `NIA_Denoise.jar` 文件。
2.  **安装**: 将下载的 `.jar` 文件复制到您的 ImageJ 或 Fiji 的 `plugins` 文件夹中。
    * *路径示例: `Fiji.app/plugins/`*
3.  **重启**: 重启 ImageJ/Fiji。
4.  **验证**: 在菜单栏找到 `Plugins` > `Biosensor Tool` > `NIA Denoise (AI)`。

---

## 🚀 使用方法 (Usage)

1.  **打开图像**: 在 ImageJ 中打开您需要去噪的图片或 Stack（支持 Tiff, OME-TIFF 等）。
2.  **启动插件**: 点击菜单 `Plugins` > `Biosensor Tool` > `NIA Denoise (AI)`。
3.  **选择模型**:
    * 🔵 **Built-in (DnCNN)**: 推荐初次使用者，适用于通用荧光图像去噪。
    * ⚪ **Custom ONNX**: 如果您有特定的 `.onnx` 模型文件，选择此项并加载。
4.  **运行**: 点击 **Start Denoising**。
    * *底部的蓝色进度条会显示处理进度。*
    * *处理完成后，会自动弹出去噪后的新图像窗口。*

---

## 🛠️ 开发与构建 (Build from Source)

如果您是开发者，想要自行修改或编译本项目，请参考以下步骤：

### 环境要求

* **JDK**: Java 8 (1.8)
* **Maven**: 3.6+
* **ImageJ**: 1.54+ (作为依赖库)

### 构建步骤

1.  克隆仓库：
    ```bash
    git clone [https://github.com/your-username/nia-imagej.git](https://github.com/your-username/nia-imagej.git)
    cd nia-imagej
    ```

2.  使用 Maven 构建 (推荐使用 `mvnd` 加速)：
    ```bash
    # 清理并打包
    mvn clean package
    ```

3.  获取产物：
    构建成功后，在 `target/` 目录下生成的 `NIA_Denoise.jar` (约 80MB+) 即为可发布插件。

---

## ⚠️ 常见问题 (FAQ)

**Q: 为什么插件包这么大 (~80MB)?**
A: 因为我们是一个 "Fat Jar" (胖包)。为了让您免去安装环境的痛苦，我们将整个 AI 推理引擎 (ONNX Runtime Core) 都打包进了插件里。

**Q: 我的图片是 16-bit 的，处理后会变成 8-bit 吗？**
A: **不会。** NIA 拥有智能位深保持功能。输入是 16-bit，输出也是 16-bit，且会保留原始数据的动态范围，确保科研数据的严谨性。

**Q: 处理 Time-lapse 视频时会闪烁吗？**
A: **不会。** NIA 使用全局归一化策略 (Global Normalization)，基于整个 Stack 的最大值进行计算，确保帧与帧之间的亮度一致性。

---

## 📜 许可证 (License)

本项目采用 **MIT License** 开源授权。
Copyright © 2026 **cns.ac.cn**. All rights reserved.

---

**Developed with ❤️ by the Biosensor Tool Team.**