package fr.muscu.app

import android.app.Application
import fr.muscu.app.data.AppDatabase
import fr.muscu.app.data.MuscuRepository

class MuscuApp : Application() {
    val repository: MuscuRepository by lazy {
        MuscuRepository(this, AppDatabase.create(this))
    }
}
