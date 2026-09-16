# MyExpenses – Offline Income & Expense Tracker

**Version 4.1.1 (Fixed)**  
Fully offline • Entry saving fixed • Printing fixed

---

## How to build the APK using only GitHub (no PC needed)

### Step 1 – Create a new repository on GitHub
1. Go to https://github.com/new
2. Repository name: `MyExpenses` (or any name)
3. Choose **Public**
4. **Do NOT** add README, .gitignore or license
5. Click **Create repository**

### Step 2 – Upload the project files
1. On the empty repository page click **uploading an existing file**
2. Unzip `MyExpenses-GitHub-Ready.zip` on your phone/computer
3. Drag & drop **all files and folders** from the `MyExpenses-Fixed` folder into GitHub
4. Click **Commit changes**

### Step 3 – Wait for the APK to build (automatic)
1. Go to the **Actions** tab of your repository
2. You will see a workflow called **Build MyExpenses APK** running
3. Wait 5–10 minutes until it shows a green check ✓
4. Click on the finished workflow run
5. Scroll down to **Artifacts**
6. Download **MyExpenses-debug-apk**
7. Unzip the downloaded file → you get the `.apk`

The APK is also published under the **Releases** section of the repository.

---

## What this app does

- Add Income / Expense entries
- Categories + notes
- Daily & Monthly summary
- Account Ledger with running balance
- Print Summary & Print Ledger
- Backup to phone storage
- Dark mode
- Calculator

**Works 100% offline** – no internet required after installation.

---

## Fixes included

| Problem              | Status  |
|----------------------|---------|
| Entry not saving     | ✅ Fixed |
| Printing not working | ✅ Fixed |

---

## Project structure

```
MyExpenses-Fixed/
├── .github/workflows/build-apk.yml   ← automatic APK build
├── config.xml
├── package.json
├── README.md
├── plugin/                           ← native print + backup plugin
│   ├── plugin.xml
│   ├── www/myexpenses-native.js
│   └── src/android/MyExpensesNative.java
└── www/
    ├── index.html                    ← main app (fixed)
    ├── sql-wasm.js / sql-wasm.wasm
    └── icons...
```

---

## License
MIT
