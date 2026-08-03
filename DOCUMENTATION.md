# WatDaSoil: Technical & Operational Manual

## 1. Project Overview
**WatDaSoil** is an AI-powered Android application designed to identify different types of soil (Black, Red, and Yellow) using machine learning. The app provides detailed agricultural information, including descriptions, primary uses, and advantages/disadvantages for each identified soil type.

## 2. Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Android XML (Material Design)
*   **AI Engine:** TensorFlow Lite (TFLite)
*   **Model:** Custom-trained CNN (`watdasoil_model.tflite`)
*   **Minimum SDK:** API 24 (Android 7.0)
*   **Target SDK:** API 35 (Android 15)

## 3. Core Features
*   **Real-time AI Classification:** High-speed soil identification.
*   **Dual Image Source:** Supports live camera capture and gallery uploads.
*   **Dynamic UI Theming:** The results page automatically changes its color palette to match the identified soil.
*   **Information Tabs:** Categorized data into Description, Uses, and Pros/Cons.
*   **Dynamic Data Display:** Real-time clock and date updates on the main dashboard.

## 4. Operational Procedure (User Guide)

### Step 1: Image Acquisition
*   **Capture:** Tap the **Capture** button to launch the camera and take a photo of the soil.
*   **Upload:** Tap the **Upload** button to select an existing soil image from your gallery.

### Step 2: Identification
*   Once an image is selected, tap the large **SCAN** button.
*   The AI will process the image and calculate the most likely soil type.

### Step 3: Reviewing Results
*   View the **Accuracy Percentage** and prediction.
*   Switch between **Description**, **Uses**, and **Pros** tabs to read detailed data.
*   Tap **Back to Home** to perform another scan.

## 5. Technical Workflow

### I. Image Processing
1.  **Resizing:** Bitmap is scaled to **224x224 pixels**.
2.  **Normalization:** Pixel values are converted to (0.0 - 1.0).
3.  **Inference:** The TFLite Interpreter runs the model against the input buffer.

### II. UI Management
1.  **Edge-to-Edge:** Uses `WindowInsetsCompat` to avoid overlapping with system bars.
2.  **Dynamic Content:** Content is injected into a `LinearLayout` container based on the active tab.

---
*Generated for WatDaSoil Project Documentation.*