package fr.muscu.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, WorkoutEntry::class, Measurement::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun entryDao(): WorkoutEntryDao
    abstract fun measurementDao(): MeasurementDao

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
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE exercises ADD COLUMN endImagePath TEXT")
                    }
                }, object : Migration(2, 3) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        ExerciseCatalog.seed(db)
                    }
                }, object : Migration(3, 4) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS measurements (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "epochDay INTEGER NOT NULL, " +
                                "bicepsCm REAL, " +
                                "shouldersCm REAL, " +
                                "createdAt INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_measurements_epochDay ON measurements (epochDay)"
                        )
                    }
                }, object : Migration(4, 5) {
                    // Biceps gauche / droit séparés. Les anciennes valeurs « biceps » (sans côté)
                    // sont conservées comme biceps droit. La table est recréée : Room refuse une
                    // colonne résiduelle (bicepsCm) absente de l'entité.
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE measurements_new (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "epochDay INTEGER NOT NULL, " +
                                "bicepsLeftCm REAL, " +
                                "bicepsRightCm REAL, " +
                                "shouldersCm REAL, " +
                                "createdAt INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "INSERT INTO measurements_new (id, epochDay, bicepsLeftCm, bicepsRightCm, shouldersCm, createdAt) " +
                                "SELECT id, epochDay, NULL, bicepsCm, shouldersCm, createdAt FROM measurements"
                        )
                        db.execSQL("DROP TABLE measurements")
                        db.execSQL("ALTER TABLE measurements_new RENAME TO measurements")
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_measurements_epochDay ON measurements (epochDay)"
                        )
                    }
                })
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        DEFAULT_EXERCISES.forEach { (name, color) ->
                            db.execSQL(
                                "INSERT INTO exercises (name, colorIndex, imagePath, createdAt) VALUES (?, ?, NULL, ?)",
                                arrayOf<Any>(name, color, now),
                            )
                        }
                        ExerciseCatalog.seed(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}
