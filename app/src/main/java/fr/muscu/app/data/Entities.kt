package fr.muscu.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val endImagePath: String? = null,
)

/** Une entrée de séance : exercice réalisé un jour donné, ex. 3×10 @ 10 kg. */
@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("exerciseId"), Index("epochDay")],
)
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val epochDay: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Une prise de mensurations (en cm) à une date donnée ; chaque mesure est facultative. */
@Entity(tableName = "measurements", indices = [Index("epochDay")])
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val bicepsLeftCm: Double? = null,
    val bicepsRightCm: Double? = null,
    val shouldersCm: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
