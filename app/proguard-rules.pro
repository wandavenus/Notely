# Aturan R8 khusus Notely.
# Room menyertakan aturan consumer-nya sendiri; entitas dan DAO dipertahankan
# oleh aturan tersebut. Baris di bawah hanya jaring pengaman untuk refleksi
# pada class database yang dihasilkan KSP.
-keep class dev.wndavenz.notely.data.NotelyDatabase_Impl { *; }
-keep class dev.wndavenz.notely.data.*_Impl { *; }
