<div align="center">

<img src="src/main/resources/NIA.png" width="120" alt="NIA Logo" />

# NIA: Neural Inference Assistant
### A Native, AI-Powered Denoising Solution for ImageJ/Fiji

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.18244343.svg)](https://doi.org/10.5281/zenodo.18244343)
[![Platform](https://img.shields.io/badge/Platform-ImageJ%20%7C%20Fiji-blue?logo=imagej)](https://imagej.net/)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?logo=openjdk)](https://openjdk.org/)
[![Engine](https://img.shields.io/badge/Engine-ONNX%20Runtime-blueviolet?logo=onnx)](https://onnxruntime.ai/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-brightgreen.svg)](https://github.com/Epivitae)
![Size](https://img.shields.io/badge/Size-Lightweight-success)

<br/>

**No Python &nbsp;|&nbsp; No CUDA Setup &nbsp;|&nbsp; Zero Configuration**

</div>

---

**NIA (Neural Inference Assistant)** brings deep learning denoising directly to your biological image analysis workflow. Built on the embedded ONNX Runtime, it runs advanced AI models natively within ImageJ/Fiji without requiring complex environment configurations or dedicated GPUs.

## ✨ Key Features

* **⚡ Native Inference**: Run AI models directly in Java. No external Python/Conda required.
* **🧠 Built-in & Custom**: Comes with an optimized **DnCNN** model (based on Zhang et al., 2017) for microscopy; supports custom `.onnx` models.
* **🎞️ 5D Hyperstack**: Seamlessly handles X, Y, Channel, Z-Slice, and Time-lapse data.
* **📊 Smart Normalization**: Auto-detects bit-depth (8/16/32-bit) and prevents flickering in time-lapse videos.
* **🖥️ Modern UI**: Clean, "WinMan" style interface for intuitive operation.

---

## 📥 Installation

You can install NIA via the Fiji Update Site (Recommended) or manually.

### Option 1: Fiji Update Site (Recommended)
1.  Open **Fiji**.
2.  Go to `Help` > `Update...`.
3.  Click **Manage update sites**.
4.  Click **Add update site**.
5.  **Name:** `Biosensor-Tools`
6.  **URL:** `https://sites.imagej.net/Biosensor-Tools/`
7.  Click **Close** and then **Apply changes**. Restart Fiji.

### Option 2: Manual Installation
1.  Download the latest `NIA_Denoise.jar` from [**Releases**](../../releases).
2.  **Drag and drop** the `.jar` file directly into the Fiji main window (or copy to `Fiji.app/plugins/`).
3.  Restart Fiji.

---

## 🚀 Usage

1.  **Open Image**: Load your stack/image in Fiji.
2.  **Launch**: `Plugins` > `Biosensor Tool` > `NIA Denoise (AI)`.
3.  **Select Model**:
    * 🔵 **Built-in (DnCNN)**: Uses the classic DnCNN architecture (Zhang et al., 2017). Best for general fluorescence.
    * ⚪ **Custom ONNX**: Load your own trained model.
4.  **Run**: Click **Start Denoising**.

---

## 📚 Citation

If you use NIA in your research, please cite our software via Zenodo:

> **Wang, K. (2026). NIA Denoise: User-Friendly AI Denoising Plugin for ImageJ/Fiji.** > *Zenodo.* https://doi.org/10.5281/zenodo.18244343

**BibTeX:**

```bibtex
@software{nia_denoise_2026,
  author       = {Wang, Kui},
  title        = {NIA Denoise: User-Friendly AI Denoising Plugin for ImageJ/Fiji},
  year         = {2026},
  publisher    = {Zenodo},
  doi          = {10.5281/zenodo.18244343},
  url          = {[https://doi.org/10.5281/zenodo.18244343](https://doi.org/10.5281/zenodo.18244343)}
}
```

### Acknowledgments & References

The built-in model is based on the **DnCNN** architecture. If you use the built-in model, please also credit the original authors:

> **Zhang, K., Zuo, W., Chen, Y., Meng, D., & Zhang, L. (2017).** Beyond a Gaussian Denoiser: Residual Learning of Deep CNN for Image Denoising. *IEEE Transactions on Image Processing*, 26(7), 3142–3155.

---
