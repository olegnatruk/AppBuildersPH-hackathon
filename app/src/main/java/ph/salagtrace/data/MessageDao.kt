package ph.salagtrace.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ph.salagtrace.domain.AnalysisState

@Dao
interface MessageDao {
    @Query("SELECT * FROM scanned_messages WHERE (:filter IS NULL OR verdict = :filter) ORDER BY receivedAt DESC")
    fun observeAll(filter: String? = null): Flow<List<ScannedMessageEntity>>
    @Query("SELECT * FROM scanned_messages WHERE id = :id") fun observeById(id: Long): Flow<ScannedMessageEntity?>
    @Query("SELECT * FROM scanned_messages WHERE id = :id") suspend fun byId(id: Long): ScannedMessageEntity?
    @Query("SELECT id FROM scanned_messages WHERE dedupKey = :key") suspend fun idForKey(key: String): Long?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(message: ScannedMessageEntity): Long
    @Update suspend fun update(message: ScannedMessageEntity): Int
    @Query("DELETE FROM scanned_messages WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM scanned_messages") suspend fun deleteAll()
    @Query("DELETE FROM scanned_messages WHERE receivedAt < :timestamp") suspend fun deleteOlderThan(timestamp: Long)
    @Query("SELECT COUNT(*) FROM scanned_messages WHERE receivedAt >= :timestamp") fun countSince(timestamp: Long): Flow<Int>
    @Query("SELECT * FROM scanned_messages WHERE state IN ('QUEUED_AI','ANALYZING')") suspend fun pendingAi(): List<ScannedMessageEntity>
    @Query("UPDATE scanned_messages SET state = 'QUEUED_AI', analysisStartedAt = NULL WHERE id = :id AND state IN ('QUEUED_AI', 'ANALYZING')")
    suspend fun requeueStopped(id: Long): Int
    @Query("UPDATE scanned_messages SET state = :state, analysisStartedAt = :startedAt WHERE id = :id")
    suspend fun setState(id: Long, state: AnalysisState, startedAt: Long? = null)
}
