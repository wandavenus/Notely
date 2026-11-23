package dev.wndavenz.notely

import android.app.Application
import dev.wndavenz.notely.data.NotelyDatabase
import dev.wndavenz.notely.data.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Penyedia dependensi manual (tanpa framework DI) untuk aplikasi kecil ini. */
class NotelyApplication : Application() {

    lateinit var repository: NotesRepository
        private set

    /** Cakupan seumur aplikasi, untuk pekerjaan yang harus selesai walau layar ditutup. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = NotesRepository(NotelyDatabase.getInstance(this))
    }
}
