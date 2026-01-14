# NIA: Neural Inference Assistant

<div align="center">
  <img src="src/main/resources/NIA.png" width="100" />
</div>

> **A Native, AI-Powered Denoising Solution for ImageJ/Fiji.**
>
> **No Python | No CUDA Setup | Zero Configuration**

**NIA (Neural Inference Assistant)** is a lightweight yet powerful ImageJ plugin designed to bring deep learning denoising directly into biological image analysis workflows. Built on the Microsoft ONNX Runtime, it allows researchers to run advanced denoising algorithms on any computer without the need for complex Python environments or GPU configurations.

---

## ✨ Key Features

* **⚡ Native Inference**: Powered by the embedded ONNX Runtime engine. Run AI models directly within Java.
* **🧠 Built-in AI Model**: Pre-loaded with a **DnCNN** model optimized for biological fluorescence microscopy.
* **🛠️ Custom Model Support**: Fully supports loading your own `.onnx` models for specialized tasks.
* **🎞️ 5D Hyperstack Support**: Seamlessly processes X, Y, Channel, Z-Slice, and Time-frame (5D) data. Automated batch processing for time-lapse videos.
* **📊 Smart Normalization**: Automatically detects 8-bit, 16-bit, and 32-bit images. Uses dynamic global normalization to prevent data clipping and ensure temporal consistency (no flickering).
* **🖥️ Professional GUI**: Features a modern, compact "WinMan" style interface for intuitive operation.

---

## 📥 Installation

1.  **Download**: Get the latest `NIA_Denoise.jar` file from the [Releases](../../releases) page.
2.  **Install**: Copy the downloaded `.jar` file into the `plugins` folder of your ImageJ or Fiji installation.
    * *Example Path: `Fiji.app/plugins/`*
3.  **Restart**: Restart ImageJ/Fiji.
4.  **Verify**: Navigate to the menu bar: `Plugins` > `Biosensor Tool` > `NIA Denoise (AI)`.

---

## 🚀 Usage

1.  **Open Image**: Open the image or stack you want to denoise in ImageJ (supports Tiff, OME-TIFF, etc.).
2.  **Launch Plugin**: Go to `Plugins` > `Biosensor Tool` > `NIA Denoise (AI)`.
3.  **Select Model**:
    * 🔵 **Built-in (DnCNN)**: Recommended for general fluorescence denoising.
    * ⚪ **Custom ONNX**: Select this to load your own trained `.onnx` file.
4.  **Run**: Click **Start Denoising**.
    * *The blue progress bar at the bottom will indicate the status.*
    * *Once finished, the denoised result will appear in a new window.*

---

## 🛠️ Build from Source

If you are a developer and wish to modify or compile the project from source, follow these steps:

### Prerequisites
* **JDK**: Java 8 (1.8)
* **Maven**: 3.6+
* **ImageJ**: 1.54+ (Provided as dependency)

### Build Steps
1.  Clone the repository:
    ```bash
    git clone [https://github.com/your-username/nia-imagej.git](https://github.com/your-username/nia-imagej.git)
    cd nia-imagej
    ```

2.  Build with Maven (using `mvnd` is recommended for speed):
    ```bash
    # Clean and Package
    mvn clean package
    ```

3.  Locate the Artifact:
    After a successful build, the plugin file `NIA_Denoise.jar` (approx. 80MB+) will be generated in the `target/` directory.

---

## ⚠️ FAQ

**Q: Why is the plugin file size so large (~80MB)?**
A: NIA is distributed as a "Fat Jar". To ensure a zero-configuration experience, we bundle the complete ONNX Runtime Core (including native libraries for Windows, Linux, and Mac) inside the plugin.

**Q: My images are 16-bit. Will the output be downsampled to 8-bit?**
A: **No.** NIA features Smart Bit-Depth Preservation. If you input a 16-bit image, the output will remain 16-bit with the correct dynamic range preserved, ensuring data integrity for quantification.

**Q: Does it cause flickering in Time-lapse videos?**
A: **No.** NIA uses a Global Normalization strategy. It scans the entire stack for the global maximum value before inference, ensuring brightness consistency across all frames.

---

## 📜 License

This project is licensed under the **MIT License**.
Copyright © 2026 **cns.ac.cn**. All rights reserved.

---

**Developed with ❤️ by the Biosensor Tool Team.**