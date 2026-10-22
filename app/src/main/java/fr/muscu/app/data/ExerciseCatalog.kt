package fr.muscu.app.data

import androidx.sqlite.db.SupportSQLiteDatabase

internal const val EXERCISE_ASSET_PREFIX = "file:///android_asset/exercises/"

internal object ExerciseCatalog {
    data class Entry(val name: String, val assetName: String, val colorIndex: Int) {
        val startImagePath get() = "$EXERCISE_ASSET_PREFIX${assetName}_start.png"
        val endImagePath get() = "$EXERCISE_ASSET_PREFIX${assetName}_end.png"
    }

    val entries = listOf(
        Entry("Développé militaire", "military_press", 0),
        Entry("Développé couché", "bench_press", 0),
        Entry("Curl haltère incliné", "incline_dumbbell_curl", 2),
        Entry("Curl à la barre", "barbell_curl", 2),
        Entry("Élévations latérales", "lateral_raise", 4),
        Entry("Barre au front", "skull_crusher", 3),
        Entry("Rowing barre", "barbell_row", 1),
        Entry("Curl concentré", "concentration_curl", 2),
        Entry("Développé incliné avec haltères", "incline_dumbbell_press", 0),
        Entry("Curl haltère debout sur banc incliné", "standing_incline_bench_curl", 2),
        Entry("Curl Spider", "spider_curl", 2),
        Entry("Tractions", "pull_up", 1),
        Entry("Dips sur banc", "bench_dips", 3),
        Entry("Curl marteau", "hammer_curl", 2),
        Entry("Croix de fer avec haltères", "dumbbell_iron_cross", 4),
        Entry("Kickback", "kickback", 3),
        Entry("Waiter curl", "waiter_curl", 2),
        Entry("Curl inversé à la barre", "reverse_barbell_curl", 2),
        Entry("Tirage bûcheron", "one_arm_row", 1),
        Entry("Élévation frontale avec un haltère", "single_dumbbell_front_raise", 4),
    )

    fun seed(db: SupportSQLiteDatabase) {
        val existing = db.query("SELECT id, name FROM exercises").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getString(1))
            }
        }
        val now = System.currentTimeMillis()
        entries.forEach { entry ->
            val matches = existing.filter { (_, name) -> name.trim().equals(entry.name, ignoreCase = true) }
            if (matches.isEmpty()) {
                db.execSQL(
                    "INSERT INTO exercises (name, colorIndex, imagePath, endImagePath, createdAt) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(entry.name, entry.colorIndex, entry.startImagePath, entry.endImagePath, now),
                )
            } else {
                matches.forEach { (id, _) ->
                    db.execSQL(
                        "UPDATE exercises SET imagePath = COALESCE(imagePath, ?), endImagePath = COALESCE(endImagePath, ?) WHERE id = ?",
                        arrayOf(entry.startImagePath, entry.endImagePath, id),
                    )
                }
            }
        }
    }
}
