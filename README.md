# Media multitool

A lightweight, powerful cross-platform media multitool built with Java and JavaFX. This application allows users to easily convert images, audio, and video files between various formats with customizable parameters and compression media files.

## 🚀 Features

### 🖼️ Image Conversion
- **Supported Formats:** JPG, PNG, WEBP, ICO, BMP, TIFF, and more.
- **ICO Support:** Full support for reading and writing `.ico` files with custom sizing (16x16, 32x32, 48x48, etc.).
- **WebP Support:** Efficient conversion to and from the WebP format.
- **Smart Resizing:** High-quality interpolation and anti-aliasing during resizing for ICO generation.
- **Transparency Handling:** Automatically converts transparent backgrounds to white when converting to JPEG.

### 🎥 Video & Audio Conversion
- **Powered by JAVE2 (FFmpeg):** Reliable and fast conversion using industrial-grade codecs.
- **Customizable Video Settings:**
  - Bitrate control (up to 8000+ kbps).
  - Frame rate (FPS) adjustment.
  - Resolution scaling (preserves aspect ratio and ensures even dimensions for compatibility).
  - Codec selection (H.264, etc.).
- **Customizable Audio Settings:**
  - Bitrate (up to 320 kbps).
  - Sampling rate (up to 48kHz).
  - Channel selection (Mono/Stereo).
- **Asynchronous Processing:** Conversions run in the background, keeping the UI responsive.
- **Cancellation Support:** Stop any ongoing conversion at any time with automatic partial file cleanup.

### 🗜️ Compression Image & Video
- **Supported Formats Image:** JPG, PNG, WEBP, ICO, BMP, TIFF, and more.
- **Supported Formats Video:** MP4, MKV, AVI, MOV, WebM, FLV, and more.
- **Preview of the compressed image after compression.**
- **Outputting the size of a compressed file**
- **Without watermark.**

### 🎨 User Interface
- **Modern JavaFX GUI:** Intuitive and easy-to-use interface.
- **Drag & Drop:** Simply drag files into the application to start.
- **Progress Tracking:** Real-time progress bars for media encoding.
- **Detailed Logging:** Built-in logging for troubleshooting and monitoring conversion status.

## 🛠️ Tech Stack

- **Java 25** (OpenJDK)
- **JavaFX 25/21** (GUI Framework)
- **Maven** (Build Tool)
- **JAVE2** (FFmpeg wrapper for media encoding)
- **Image4j** (ICO format support)
- **TwelveMonkeys & Sejda ImageIO and etc.** (Advanced image format support including WebP)
- **Logback & SLF4J** (Logging)

## 📋 Prerequisites

- **Java Development Kit (JDK) 25** or higher.
- **Maven** 3.8 or higher.

## 📥 Installation & Running

**👉 [Download latest release](https://github.com/Jussi699/media-multitool/releases/latest)**

---

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Jussi699/media-multitool.git
   cd media-multitool
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn javafx:run
   ```
   
## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Developed by [Jussi669](https://github.com/Jussi699)
