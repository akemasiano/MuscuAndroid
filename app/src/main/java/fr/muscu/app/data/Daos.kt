package fr.muscu.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT COUNT(*) FROM exercises WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId")
    suspend fun countWithName(name: String, excludeId: Long): Int

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WorkoutEntryDao {
    @Query("SELECT * FROM workout_entries ORDER BY epochDay, id")
    fun observeAll(): Flow<List<WorkoutEntry>>

    @Query(
        """SELECT * FROM workout_entries
           WHERE exerciseId = :exerciseId AND id != :excludeId
           ORDER BY epochDay DESC, id DESC LIMIT 1"""
    )
    suspend fun latestFor(exerciseId: Long, excludeId: Long): WorkoutEntry?

    @Insert
    suspend fun insert(entry: WorkoutEntry): Long

    @Update
    suspend fun update(entry: WorkoutEntry)

    @Query("DELETE FROM workout_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_entries WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: Long)
}
