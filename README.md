<div align="center">

# 💸 BillBuddy

**Automated Expense Parsing & Frictionless Bill Splitting Powered by AI**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://developer.android.com/)
[![AI Model](https://img.shields.io/badge/AI_Engine-Gemini_2.5_Flash-blue?style=flat-square&logo=googlegemini)](https://ai.google.dev/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI_Framework-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

<p align="center">
  <a href="#key-features">Key Features</a> •
  <a href="#how-it-works">How It Works</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#local-configuration">Configuration</a>
</p>

---
</div>

## 🌟 Overview

**BillBuddy** is an intelligent Android application designed to eliminate the friction of splitting expenses. By actively listening for transactional and financial SMS alerts, BillBuddy uses **Gemini 2.5 Flash** to instantaneously parse merchant names and transaction totals. It then automatically structures personalized UPI deep links alongside pre-configured WhatsApp settlement templates, making bill splitting completely hands-free.

> [!NOTE]  
> This application was bootstrapped and configured via Google AI Studio. View the active build profile on [AI Studio](https://ai.studio/apps/a0d514d1-0f8b-412f-9b3e-32b85474462d).

---

## 🚀 Key Features

* **Real-time SMS Interception:** Securely processes incoming financial and banking text notifications on the device.
* **Intelligent Entity Extraction:** Leverages `Gemini 2.5 Flash` to accurately identify transaction totals, currencies, and vendor entities from raw, unstructured SMS logs.
* **Automated UPI Deep Linking:** Dynamically formats standard payment URLs into actionable UPI intents to instantly transition to apps like Google Pay, PhonePe, or Paytm.
* **Seamless WhatsApp Sharing:** Instantly constructs pre-filled split breakdowns and launches WhatsApp directly with your contacts.

---

## 🔄 How It Works

[ Incoming SMS ] ──> [ SMSReceiver ] ──> [ Gemini 2.5 Flash Engine ]
│
┌────────────────────────────────────────────────┴───────────────────────────────┐
▼ (Structured Data Output)                                                       ▼
[ Direct UPI Intent Link Creation ]                                [ Pre-filled WhatsApp Text ]


---

## 🛠️ Tech Stack

* **Frontend Framework:** Jetpack Compose (Declarative UI)
* **Language:** Kotlin
* **AI Architecture:** Google AI Studio SDK (Server-Side Gemini API Implementation)
* **Data Persistence:** Jetpack Room Database / Local State Managers
* **Intents:** Android BroadcastReceiver (SMS processing Architecture) & Deep Linking API

---

## 🛠️ Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended)
* Android SDK 34+
* A valid Google AI Studio [Gemini API Key](https://aistudio.google.com/)

### Local Installation

1. **Clone the Repository**
   ```bash
   git clone [https://github.com/dakshlohchab/bill-buddy.git](https://github.com/dakshlohchab/bill-buddy.git)
   cd bill-buddy
Import into Android Studio

Open Android Studio.

Select Open and choose the directory containing this project.

Allow Android Studio to fix any incompatibilities as it imports the project.

⚙️ Local Configuration
To safeguard sensitive API tokens, configuration is handled entirely through local environment variables.

1. Configure Environment Variables
Create a file named .env in the project directory (mirroring the structure of .env.example) and append your Gemini credentials:

Code snippet
GEMINI_API_KEY=your_actual_gemini_api_key_here
2. Adjust Build Signatures for Local Emulators
To safely build and debug the environment locally without matching release keystores, open the app's build.gradle.kts file and remove or comment out the following line:

Kotlin
// REMOVE OR COMMENT OUT THIS LINE FOR LOCAL EMULATOR RUNS:
signingConfig = signingConfigs.getByName("debugConfig")
3. Execution
Connect your Android physical test device via ADB (with SMS reading permissions enabled for debugging) or boot an Android Virtual Device (AVD) emulator, and run the app on your emulator or physical device.

🔒 Security & Permissions
BillBuddy parses text content on-demand via the Google Gemini API. Ensure that when running the application on a device, you explicitly grant the required runtime permissions to read incoming SMS logs (android.permission.RECEIVE_SMS).
