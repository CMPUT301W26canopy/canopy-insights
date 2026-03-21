# canopy-insights

# About this project: 

This app alows users to sign up and login an account where they can register for events that uses a lottery system to decide participation. Organizers can make thier specifications, and let users enter the lottery, where if chosen, they can decline or accept the invitation to the event.

Documentation:

[Link to our Wiki](https://github.com/CMPUT301W26canopy/canopy-insights/wiki)

---

# Features

### Logging in and updating account

"https://github.com/CMPUT301W26canopy/canopy-insights/raw/main/doc/pt3Videos/signInUpdate.mp4"

### Check update and delete

"https://github.com/CMPUT301W26canopy/canopy-insights/raw/main/doc/pt3Videos/updateCheckAndDelete.mp4"
## Setup Instructions

### 1. Clone the Repository

**Option A — Android Studio (recommended):**
1. Open Android Studio
2. Click **File → New → Project from Version Control**
3. Paste the repo URL: `https://github.com/CMPUT301W26canopy/canopy-insights`
4. Choose a local directory and click **Clone**
5. Wait for Gradle to sync

---

### 2. Open the Project

- In Android Studio → **File → Open**
- Navigate to the cloned folder and select the **`code`** folder (not the root)
- Wait for Gradle to finish syncing

---

### 3. Set Up Firebase

This project uses Firebase Firestore. The `google-services.json` file is **not included** in the repository for security reasons. You need to get it from a team member or set it up yourself.

**Steps:**
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Open the **canopy-insights** project
3. Click the gear icon → **Project Settings**
4. Under **Your apps**, find the Android app
5. Download `google-services.json`
6. Place it at:
```
canopy-insights/code/app/google-services.json
```

> **Important:** The file must be inside the `app/` folder, not the root `code/` folder.

---

### 4. Connect Your Device

**Physical device:**
1. Enable **Developer Options** on your Android phone
   - Go to Settings → About Phone → tap **Build Number** 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect phone via USB cable
4. In Android Studio, select your device from the device dropdown at the top

**Emulator:**
1. In Android Studio → **Tools → Device Manager**
2. Click **Create Device**
3. Choose a phone (e.g., Pixel 6) with API level 24 or higher
4. Click **Finish** and start the emulator

---

### 5. Run the App

1. Make sure your device or emulator is selected at the top of Android Studio
2. Click the **Run** button (green play icon) or press `Shift + F10`
3. The app will build and install on your device

---

### 6. Dependencies

All dependencies are managed in `app/build.gradle.kts`. No manual installation needed — Gradle handles everything on first build.

Key dependencies:
- Firebase Firestore
- ZXing (QR code)
- AndroidX RecyclerView
- Material Components

---

### Notes

- Minimum SDK: **API 24 (Android 7.0)**
- Target SDK: **API 35**
- The `google-services.json` file is gitignored — share it manually with teammates
- If build fails, try **File → Invalidate Caches → Restart**
