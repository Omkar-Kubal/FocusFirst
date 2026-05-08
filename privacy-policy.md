# Privacy Policy for Toki

**Last updated: May 8, 2026**

This Privacy Policy explains how Toki ("we", "our", or "the app") collects, uses, and protects your information when you use the Toki focus timer app on Android.

---

## 1. Information We Collect

### 1.1 Data You Create (Stored Locally on Your Device)

All core app data is stored locally on your device and never shared with third parties:

- **Focus sessions** — start time, duration, completion status, and optional tag
- **Tasks** — title, description, status, due date/time, target and completed Pomodoro counts, and optional tag
- **Tags** — name, color, and creation date
- **App preferences** — timer durations, theme, sound/vibration settings, daily goal, ambient sound selection, and blocked apps list (Focus Guard)

### 1.2 Data Synced to the Cloud (Optional)

If you use the **Cloud Backup** feature, focus session records are uploaded to Firebase Firestore (Google) under a randomly generated anonymous ID. This ID is not linked to your name, email, or any personal information. The data synced includes:

- Session start time, duration, completion status, and tag
- A server-generated sync timestamp

You can delete all cloud data at any time from within the app.

### 1.3 Analytics (Automatically Collected)

We use **Firebase Analytics** (Google) to understand how the app is used. This includes:

- **Events:** session started/completed/abandoned, sound selected, break suggestion shown, CSV export, tab viewed, streak milestones reached, billing interactions
- **User properties:** whether you are a Pro user, your session count range (e.g. 0–9, 10–49), your streak range (e.g. 0, 1–6, 7–29, 30+)

Analytics data is aggregated and anonymized. We do not use it to identify individuals.

### 1.4 Crash Reports (Automatically Collected)

We use **Firebase Crashlytics** (Google) to detect and fix crashes. Crashlytics automatically collects:

- Device model, OS version, and app version
- Stack traces and error logs at the time of a crash

No personally identifiable information is included in crash reports.

### 1.5 Purchases

In-app purchases for Toki Pro are processed by **Google Play Billing**. We do not receive, store, or process your payment information. All billing is handled by Google.

---

## 2. Permissions We Request

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Send timer and break reminders |
| `FOREGROUND_SERVICE` | Keep the timer running reliably in the background |
| `RECEIVE_BOOT_COMPLETED` | Resume an active timer after device restart |
| `VIBRATE` / `WAKE_LOCK` | Vibrate and wake screen on timer completion |
| `USE_FULL_SCREEN_INTENT` | Show full-screen timer alert on locked screen |
| `ACCESS_NOTIFICATION_POLICY` | Enable Do Not Disturb during focus sessions (optional) |
| `INTERNET` | Cloud backup and Firebase communication |
| `PACKAGE_USAGE_STATS` | **Focus Guard only (Pro)** — detect which app is in the foreground to block distracting apps during a session. Requires explicit user grant in system settings. |

We do not request access to contacts, location, camera, microphone, files, or any other sensitive data.

---

## 3. How We Use Your Information

- **To provide app functionality** — timers, tasks, stats, streaks, and widgets
- **To sync your data** — optional cloud backup and restore across devices
- **To improve the app** — crash reports and analytics help us identify bugs and prioritize features
- **To process purchases** — verify Pro status via Google Play

We do not sell, rent, or trade your data. We do not use your data for advertising.

---

## 4. Third-Party Services

Toki uses the following third-party services, each governed by their own privacy policies:

| Service | Purpose | Privacy Policy |
|---|---|---|
| Firebase Analytics | Usage analytics | [Google Privacy Policy](https://policies.google.com/privacy) |
| Firebase Crashlytics | Crash reporting | [Google Privacy Policy](https://policies.google.com/privacy) |
| Firebase Firestore | Cloud backup (optional) | [Google Privacy Policy](https://policies.google.com/privacy) |
| Firebase Authentication | Anonymous user ID for cloud sync | [Google Privacy Policy](https://policies.google.com/privacy) |
| Google Play Billing | In-app purchases | [Google Play Terms of Service](https://play.google.com/about/play-terms/) |

---

## 5. Data Retention and Deletion

- **Local data** remains on your device until you uninstall the app or clear app data.
- **Cloud data** is retained until you delete it. You can delete all cloud-synced sessions from **Settings → Cloud Backup → Delete Cloud Data**.
- **Analytics and crash data** is retained by Google per their standard retention policies.

To request deletion of any data associated with your anonymous ID, contact us at the email below.

---

## 6. Children's Privacy

Toki is not directed at children under 13. We do not knowingly collect personal information from children. If you believe a child has provided us data, please contact us and we will delete it promptly.

---

## 7. Data Security

All local data is stored in the app's private storage on your device. Cloud sync uses Firebase's built-in security (TLS in transit, Firestore security rules at rest). Anonymous authentication ensures no account credentials are required or stored.

---

## 8. Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be posted at the URL below with an updated "Last updated" date. Continued use of the app after changes constitutes acceptance of the updated policy.

---

## 9. Contact

Questions or requests regarding your data:

**Email:** omkarkubal@protonmail.com  
**Privacy Policy URL:** https://tokifocus.blogspot.com/privacy-policy
