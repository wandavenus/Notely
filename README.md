# Notely

Aplikasi catatan gaya iOS untuk Android, ditulis native dengan **Kotlin + Jetpack Compose**.

- Antarmuka meniru Apple Notes (mode gelap, aksen biru) dan dibangun murni dengan Compose Foundation — tanpa Material.
- Font **Inter** dibundel sebagai resource.
- Catatan tersimpan lokal di perangkat dengan **Room (SQLite)** — sepenuhnya luring.
- Fitur: folder, sematan (pin), pencarian, keranjang sampah (Baru Dihapus), simpan otomatis di editor, judul otomatis dari baris pertama.

## Build

```bash
./gradlew :app:assembleDebug     # APK debug
./gradlew :app:assembleRelease   # APK rilis (minify + R8 + shrinkResources)
./gradlew :app:testDebugUnitTest # tes unit
```

Build rilis hanya menghasilkan APK **arm64-v8a** (split ABI) agar ukurannya kecil, dan sementara ditandatangani kunci debug agar langsung bisa dipasang. Untuk publikasi, ganti dengan keystore rilis di `app/build.gradle.kts`.

GitHub Actions (`.github/workflows/build.yml`) menjalankan tes unit lalu membangun APK debug + rilis pada setiap push ke `main` atau lewat *workflow dispatch*; hasilnya tersedia sebagai artefak `notely-apks`.
