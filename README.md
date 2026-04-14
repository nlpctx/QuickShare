# QuickShare (Anonymous Peer-to-Peer File Sharing)


QuickShare is a secure, 100% anonymous Android application designed for high-speed peer-to-peer sharing. It allows you to share photos and videos directly from your device via a secure SSH tunnel—without accounts, without clouds, and without tracking.

---

### ❤️ Supporting the Ecosystem
If you enjoy this project and find the seamless sharing useful, please consider supporting the providers that make anonymous tunneling possible. You can visit and support the [localhost.run repository](https://github.com/mjp/localhost.run) to help keep these services alive and free for everyone.

---

## 📑 Index

1. [How It Works](#-how-it-works)
2. [No-Tracking Policy](#-no-tracking-policy)
3. [Security Architecture](#-security-architecture)
4. [Features](#-features)
5. [Technical Stack](#-technical-stack)

---

## 🚀 How It Works

1. **Select Media**: Choose images or videos from your gallery using the secure system picker.
2. **Start Session**: The app launches a local Ktor server on your device.
3. **Secure Tunnel**: A reverse SSH tunnel is established to `localhost.run`, providing a secure public HTTPS URL.
4. **Share**: Generate a QR code or link with an ephemeral session token.
5. **Direct Transfer**: Recipients download files directly from your device. No intermediate servers ever store your data.

---

## 🕵️ Privacy & Analytics

QuickShare is built for absolute privacy.
- **Minimal Analytics**: We use PostHog for service health monitoring only. We capture anonymous events like when a session starts and the number of files shared.
- **No User Details**: We do not capture lifecycle events, screen views, IP addresses, names, or emails.
- **No Logs**: We do not collect filenames or media metadata.
- **Anonymous Release**: The application is signed with an anonymous certificate (`nlpctx`).

---

## 🔒 Security Architecture

- **Ephemeral Tunnels**: All traffic is encrypted via SSH and served over SSL/TLS.
- **Session Tokens**: Every link requires a unique, randomly generated token. Without the token, the server returns `401 Unauthorized`.
- **In-Memory Operation**: Shared file metadata is kept in memory. Once you stop the session, all access is wiped instantly.
- **Scoped Permissions**: Uses modern Android Photo Picker to avoid broad "All Files" access.

---

## ✨ Features

- **Jetpack Compose UI**: A premium, modern dark-themed interface.
- **QR Code Integration**: Scan to download instantly.
- **Multi-Link Support**: Create multiple unique sharing links for different groups of files.
- **Background Sharing**: Keeps sharing active even when the app is in the background via a Foreground Service.
- **Zero Install**: Recipients only need a web browser.

---

## 🛠 Technical Stack

- **Kotlin Multiplatform**: Core logic shared across platforms.
- **Ktor (CIO)**: High-performance asynchronous HTTP server.
- **JSch**: SSH2 implementation for secure tunneling.
- **Material 3**: Modern design system.

---

*Built for the community by nlpctx. Stay anonymous.*


