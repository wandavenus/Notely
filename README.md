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

Build rilis hanya menghasilkan APK **arm64-v8a** (split ABI) agar ukurannya kecil, dan ditandatangani keystore rilis tetap (di CI dibaca dari GitHub Secrets `NOTELY_KEYSTORE_*`; tanpa env tersebut build lokal jatuh ke kunci debug), sehingga update berikutnya selalu bisa dipasang langsung di atas versi sebelumnya.

GitHub Actions punya dua workflow yang berjalan sejajar pada setiap push ke `main`: `test.yml` menjalankan tes unit, dan `build.yml` membangun APK rilis lalu mengunggahnya otomatis ke GitHub Release yang bertag sesuai `versionName`.
