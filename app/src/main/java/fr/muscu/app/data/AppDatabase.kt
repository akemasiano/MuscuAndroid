package fr.muscu.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Exercise::class, WorkoutEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun entryDao(): WorkoutEntryDao

    companion object {
        private val DEFAULT_EXERCISES = listOf(
            "Développé couché" to 0,
            "Rowing barre" to 1,
            "Curl biceps" to 2,
            "Soulevé de terre" to 3,
            "Squat barre" to 4,
            "Tractions" to 1,
        )

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "muscu.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        DEFAULT_EXERCISES.forEach { (name, color) ->
                            db.execSQL(
                                "INSERT INTO exercises (name, colorIndex, imagePath, createdAt) VALUES (?, ?, NULL, ?)",
                                arrayOf<Any>(name, color, now),
                            )
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}
