package ph.salagtrace

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ph.salagtrace.data.*
import ph.salagtrace.domain.*

class MessageDaoTest {
    @Test fun dedupFiltersRetentionAndDeletion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val dao = db.messages()
            val m = ScannedMessageEntity(dedupKey = "test", source = Source.SAMPLE, sender = "test", maskedBody = "OTP [OTP]", maskSpansJson = "[]", receivedAt = 10,
                ruleScore = 0, ruleSignalsJson = "[]", urlsJson = "[]", state = AnalysisState.SCREENED, verdict = Verdict.LIKELY_SAFE)
            val id = dao.insertIgnore(m)
            assertTrue(id > 0); assertEquals(-1L, dao.insertIgnore(m))
            assertEquals(1, dao.observeAll(Verdict.LIKELY_SAFE.name).first().size)
            assertEquals(0, dao.observeAll(Verdict.LIKELY_SCAM.name).first().size)
            assertEquals("OTP [OTP]", dao.byId(id)?.maskedBody)
            dao.deleteOlderThan(11); assertNull(dao.byId(id))
            dao.insertIgnore(m.copy(dedupKey = "another")); dao.deleteAll(); assertTrue(dao.observeAll().first().isEmpty())
        } finally { db.close() }
    }
}
