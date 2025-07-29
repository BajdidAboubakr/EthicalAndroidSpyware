# 🕵️ Android Spyware - Educational Purpose Only

This is a complete Android spyware prototype written in **Kotlin**, developed for **educational and ethical hacking purposes** only.  
It demonstrates how spyware applications can exploit permissions and Android APIs to extract sensitive data.

---

## ⚠️ LEGAL WARNING

> This project is intended **solely for educational use**, self-testing on **your own devices**, or within legal penetration testing labs.  
> **Do not use** this software on anyone else's device **without explicit consent**.  
> Any malicious or illegal use is **strictly prohibited**.  
> The author is **not responsible** for any misuse.

---

## 🎯 Features

| Module                  | Description                            |
|------------------------|----------------------------------------|
| `AudioRecorder.kt`     | Records microphone input               |
| `VideoRecord.kt`       | Accesses and records from camera       |
| `GetSms.kt`            | Retrieves SMS messages                 |
| `CallLogsRetriever.kt` | Accesses call history                  |
| `ContactsRetriever.kt` | Extracts phone contacts                |
| `GetClipboard.kt`      | Reads clipboard content                |
| `LocationUtils.kt`     | Gets GPS location                      |
| `ShellCommandExecutor` | Executes shell commands                |
| `MyForegroundService`  | Keeps app active in background         |
| `Server.kt`            | Sends collected data to remote server  |

---

## 🧠 Educational Objectives

This project aims to:
- Demonstrate how spyware applications work on Android
- Understand the abuse of permission systems
- Raise awareness about mobile security
- Help students develop countermeasures

---

## 🛠️ Tech Stack

- **Kotlin**
- **Android SDK**
- Gradle Kotlin DSL
- Android Foreground Services

---

## 🚀 Getting Started

```bash
git clone https://github.com/<your-username>/android-spyware-educational.git
cd android-spyware-educational/Game
