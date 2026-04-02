# JPEG XL Camera App

## Overview
The JPEG XL Camera app is a modern Android application designed to capture photos and save them in the high-efficiency JPEG XL (.jxl) format. It uses Jetpack Compose for a premium UI and CameraX for reliable camera capture.

To view your files, I recommend Fossify Gallery.

## App Goal
To provide a fast, high-quality camera experience that leverages the benefits of the JPEG XL format.

## Summary of Completed Instructions
- **Save to Public Storage**: Modified the app to save pictures to public storage instead of app-private storage. Due to limitations with `MediaStore.Images` and non-standard MIME types, files are saved to the `Download` directory.
- **Save Location**: Pictures are saved to `/sdcard/Download/JxlCamera`.
- **Portrait Only**: The app is locked to portrait orientation.

---

## Usage Instructions

### Step 1: Launch the App
Open the app from your launcher. It will request Camera permission if not already granted.

### Step 2: Grant Permissions
When prompted, allow the app to access your Camera.

### Step 3: Capture an Image
Point your camera at the subject and tap the **Capture Button** (large red button at the bottom).

---

## What to Expect

### Output Files
When you take a picture, the app encodes the image as a JPEG XL file and saves it.

- **File Extension**: `.jxl`
- **Location**: `/sdcard/Download/JxlCamera/`

You can verify the saved files by pulling them using ADB:
```bash
adb pull /sdcard/Download/JxlCamera .
```

---

## Troubleshooting

### App Crashes or Preview is Black
- Ensure you have granted Camera permission. You can check App Info in system settings.
- If the preview is black, try restarting the app or checking if another app is using the camera.

### Files Not Found
- Check the ADB path correctly. The path might vary slightly based on the device, but it should be under `Download/JxlCamera`.

---

This app relies on https://github.com/awxkee/jxl-coder huge thanks to Radzivon Bartoshyk!
