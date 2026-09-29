package com.example.bananaq.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.net.Uri
import java.util.UUID

class BananaQDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_sessions (
                sessionId TEXT PRIMARY KEY NOT NULL,
                startedAt INTEGER NOT NULL,
                lastActiveAt INTEGER NOT NULL,
                endedAt INTEGER,
                isActive INTEGER NOT NULL CHECK (isActive IN (0, 1))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE scans (
                scanId TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT NOT NULL,
                imagePath TEXT,
                predictedDisease TEXT,
                confidenceScore REAL,
                isBananaLeaf INTEGER NOT NULL CHECK (isBananaLeaf IN (0, 1)),
                scannedAt INTEGER NOT NULL,
                FOREIGN KEY (sessionId) REFERENCES user_sessions(sessionId) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE feedback (
                feedbackId TEXT PRIMARY KEY NOT NULL,
                scanId TEXT NOT NULL UNIQUE,
                accuracyRating TEXT NOT NULL,
                comments TEXT NOT NULL,
                submittedAt INTEGER NOT NULL,
                FOREIGN KEY (scanId) REFERENCES scans(scanId) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_scans_sessionId ON scans(sessionId)")
        db.execSQL("CREATE INDEX index_scans_scannedAt ON scans(scannedAt)")
        db.execSQL("CREATE INDEX index_feedback_scanId ON feedback(scanId)")
        createSyncOutbox(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 2) {
            removeAnonymousUserIdColumn(db)
        }
        if (oldVersion < 4) {
            createSyncOutbox(db)
            enqueueExistingRecords(db)
        }
        appContext.getSharedPreferences("anonymous_identity", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Synchronized
    fun startSession(now: Long = System.currentTimeMillis()): String {
        val db = writableDatabase
        var createdSessionId = ""
        var migratedLegacyData = false
        db.beginTransaction()
        try {
            val previouslyActive = mutableListOf<String>()
            db.query(
                "user_sessions", arrayOf("sessionId"), "isActive = 1",
                null, null, null, null
            ).use { cursor ->
                while (cursor.moveToNext()) previouslyActive += cursor.getString(0)
            }
            val closeValues = ContentValues().apply {
                put("lastActiveAt", now)
                put("endedAt", now)
                put("isActive", 0)
            }
            db.update("user_sessions", closeValues, "isActive = 1", null)
            previouslyActive.forEach { enqueue(db, ENTITY_SESSION, it, now) }

            val sessionId = UUID.randomUUID().toString()
            db.insertOrThrow("user_sessions", null, ContentValues().apply {
                put("sessionId", sessionId)
                put("startedAt", now)
                put("lastActiveAt", now)
                putNull("endedAt")
                put("isActive", 1)
            })
            enqueue(db, ENTITY_SESSION, sessionId, now)
            migratedLegacyData = migrateLegacyData(sessionId)
            db.setTransactionSuccessful()
            createdSessionId = sessionId
        } finally {
            db.endTransaction()
        }
        if (migratedLegacyData) {
            appContext.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(MIGRATION_COMPLETE, true).commit()
        }
        return createdSessionId
    }

    @Synchronized
    fun touchSession(sessionId: String, now: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val updated = db.update("user_sessions", ContentValues().apply {
            put("lastActiveAt", now)
        }, "sessionId = ? AND isActive = 1", arrayOf(sessionId))
        if (updated > 0) enqueue(db, ENTITY_SESSION, sessionId, now)
    }

    @Synchronized
    fun endSession(sessionId: String, now: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val updated = db.update("user_sessions", ContentValues().apply {
            put("lastActiveAt", now)
            put("endedAt", now)
            put("isActive", 0)
        }, "sessionId = ?", arrayOf(sessionId))
        if (updated > 0) enqueue(db, ENTITY_SESSION, sessionId, now)
    }

    @Synchronized
    fun sessions(): List<UserSessionRecord> {
        val result = mutableListOf<UserSessionRecord>()
        readableDatabase.query(
            "user_sessions",
            arrayOf("sessionId", "startedAt", "lastActiveAt", "endedAt", "isActive"),
            null, null, null, null, "startedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val endedAtIndex = cursor.getColumnIndexOrThrow("endedAt")
                result += UserSessionRecord(
                    sessionId = cursor.getString(cursor.getColumnIndexOrThrow("sessionId")),
                    startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("startedAt")),
                    lastActiveAt = cursor.getLong(cursor.getColumnIndexOrThrow("lastActiveAt")),
                    endedAt = if (cursor.isNull(endedAtIndex)) null else cursor.getLong(endedAtIndex),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1
                )
            }
        }
        return result
    }

    @Synchronized
    fun ensureSessionExists(sessionId: String, now: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        readableDatabase.query(
            "user_sessions", arrayOf("sessionId"),
            "sessionId = ?", arrayOf(sessionId), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                db.insertWithOnConflict("user_sessions", null, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("startedAt", now)
                    put("lastActiveAt", now)
                    putNull("endedAt")
                    put("isActive", 1)
                }, SQLiteDatabase.CONFLICT_IGNORE)
                enqueue(db, ENTITY_SESSION, sessionId, now)
            }
        }
    }

    @Synchronized
    fun insertScan(record: ScanRecord): Boolean {
        if (record.confidenceScore != null && record.confidenceScore !in 0.0..1.0) return false
        if (record.isBananaLeaf !=
            (record.predictedDisease != null && record.confidenceScore != null)) return false
        ensureSessionExists(record.sessionId, record.scannedAt)
        val result = writableDatabase.insertWithOnConflict(
            "scans", null, ContentValues().apply {
                put("scanId", record.scanId)
                put("sessionId", record.sessionId)
                put("imagePath", record.imagePath)
                if (record.predictedDisease == null) putNull("predictedDisease")
                else put("predictedDisease", record.predictedDisease)
                if (record.confidenceScore == null) putNull("confidenceScore")
                else put("confidenceScore", record.confidenceScore)
                put("isBananaLeaf", if (record.isBananaLeaf) 1 else 0)
                put("scannedAt", record.scannedAt)
            }, SQLiteDatabase.CONFLICT_IGNORE
        )
        if (result != -1L) {
            enqueue(writableDatabase, ENTITY_SCAN, record.scanId, record.scannedAt)
            val expiredPaths = mutableListOf<String>()
            readableDatabase.rawQuery(
                "SELECT imagePath FROM scans ORDER BY scannedAt DESC LIMIT -1 OFFSET 200",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(0)) expiredPaths += cursor.getString(0)
                }
            }
            writableDatabase.execSQL(
                "DELETE FROM scans WHERE scanId NOT IN " +
                    "(SELECT scanId FROM scans ORDER BY scannedAt DESC LIMIT 200)"
            )
            writableDatabase.execSQL(
                "DELETE FROM sync_outbox WHERE " +
                    "(entityType = '$ENTITY_SCAN' AND entityId NOT IN (SELECT scanId FROM scans)) OR " +
                    "(entityType = '$ENTITY_FEEDBACK' AND entityId NOT IN (SELECT feedbackId FROM feedback))"
            )
            deleteManagedPhotos(expiredPaths)
        }
        return result != -1L
    }

    @Synchronized
    fun scans(): List<ScanRecord> {
        val result = mutableListOf<ScanRecord>()
        readableDatabase.query(
            "scans",
            arrayOf(
                "scanId", "sessionId", "imagePath", "predictedDisease",
                "confidenceScore", "isBananaLeaf", "scannedAt"
            ), null, null, null, null, "scannedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val confidenceIndex = cursor.getColumnIndexOrThrow("confidenceScore")
                result += ScanRecord(
                    scanId = cursor.getString(cursor.getColumnIndexOrThrow("scanId")),
                    sessionId = cursor.getString(cursor.getColumnIndexOrThrow("sessionId")),
                    imagePath = cursor.stringOrNull("imagePath"),
                    predictedDisease = cursor.stringOrNull("predictedDisease"),
                    confidenceScore = if (cursor.isNull(confidenceIndex)) null else cursor.getDouble(confidenceIndex),
                    isBananaLeaf = cursor.getInt(cursor.getColumnIndexOrThrow("isBananaLeaf")) == 1,
                    scannedAt = cursor.getLong(cursor.getColumnIndexOrThrow("scannedAt"))
                )
            }
        }
        return result
    }

    @Synchronized
    fun cleanupOrphanedPhotos() {
        val managedRoot = File(appContext.filesDir, "scan_photos")
        if (!managedRoot.isDirectory) return
        val referenced = scans().mapNotNullTo(hashSetOf()) { record ->
            record.imagePath?.let { value ->
                runCatching {
                    Uri.parse(value).takeIf { it.scheme == "file" }?.path
                        ?.let(::File)?.canonicalPath
                }.getOrNull()
            }
        }
        managedRoot.listFiles()?.forEach { file ->
            if (file.isFile && file.canonicalPath !in referenced) file.delete()
        }
    }

    @Synchronized
    fun insertFeedback(record: FeedbackRecord): Boolean {
        if (record.accuracyRating !in ALLOWED_RATINGS || record.comments.length > 2_000) return false
        val db = writableDatabase
        val inserted = db.insertWithOnConflict(
            "feedback", null, ContentValues().apply {
                put("feedbackId", record.feedbackId)
                put("scanId", record.scanId)
                put("accuracyRating", record.accuracyRating)
                put("comments", record.comments)
                put("submittedAt", record.submittedAt)
            }, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
        if (inserted) enqueue(db, ENTITY_FEEDBACK, record.feedbackId, record.submittedAt)
        return inserted
    }

    @Synchronized
    fun pendingSyncBatch(limit: Int = 100, now: Long = System.currentTimeMillis()): PendingSyncBatch {
        val pending = linkedMapOf<String, MutableSet<String>>()
        readableDatabase.query(
            "sync_outbox", arrayOf("entityType", "entityId"),
            "queuedAt <= ?", arrayOf(now.toString()), null, null, "queuedAt ASC", limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                pending.getOrPut(cursor.getString(0)) { linkedSetOf() }.add(cursor.getString(1))
            }
        }
        val pendingFeedback = feedbackRecords().filter {
            it.feedbackId in pending[ENTITY_FEEDBACK].orEmpty()
        }
        val requiredScanIds = pendingFeedback.mapTo(linkedSetOf()) { it.scanId }
        requiredScanIds += pending[ENTITY_SCAN].orEmpty()
        val pendingScans = scans().filter { it.scanId in requiredScanIds }
        val requiredSessions = pendingScans.mapTo(linkedSetOf()) { it.sessionId }
        requiredSessions += pending[ENTITY_SESSION].orEmpty()
        return PendingSyncBatch(
            sessions = sessions().filter { it.sessionId in requiredSessions },
            scans = pendingScans,
            feedback = pendingFeedback,
            cutoffAt = now
        )
    }

    @Synchronized
    fun markSynced(batch: PendingSyncBatch) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            batch.sessions.forEach { removeQueued(db, ENTITY_SESSION, it.sessionId, batch.cutoffAt) }
            batch.scans.forEach { removeQueued(db, ENTITY_SCAN, it.scanId, batch.cutoffAt) }
            batch.feedback.forEach { removeQueued(db, ENTITY_FEEDBACK, it.feedbackId, batch.cutoffAt) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun feedbackForScan(scanId: String): FeedbackRecord? {
        readableDatabase.query(
            "feedback",
            arrayOf("feedbackId", "scanId", "accuracyRating", "comments", "submittedAt"),
            "scanId = ?", arrayOf(scanId), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return FeedbackRecord(
                feedbackId = cursor.getString(cursor.getColumnIndexOrThrow("feedbackId")),
                scanId = cursor.getString(cursor.getColumnIndexOrThrow("scanId")),
                accuracyRating = cursor.getString(cursor.getColumnIndexOrThrow("accuracyRating")),
                comments = cursor.getString(cursor.getColumnIndexOrThrow("comments")),
                submittedAt = cursor.getLong(cursor.getColumnIndexOrThrow("submittedAt"))
            )
        }
    }

    @Synchronized
    fun feedbackScanIds(): Set<String> {
        val result = linkedSetOf<String>()
        readableDatabase.query(
            "feedback", arrayOf("scanId"), null, null, null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.getString(0)
        }
        return result
    }

    @Synchronized
    fun feedbackRecords(): List<FeedbackRecord> {
        val result = mutableListOf<FeedbackRecord>()
        readableDatabase.query(
            "feedback",
            arrayOf("feedbackId", "scanId", "accuracyRating", "comments", "submittedAt"),
            null, null, null, null, "submittedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += FeedbackRecord(
                    feedbackId = cursor.getString(cursor.getColumnIndexOrThrow("feedbackId")),
                    scanId = cursor.getString(cursor.getColumnIndexOrThrow("scanId")),
                    accuracyRating = cursor.getString(cursor.getColumnIndexOrThrow("accuracyRating")),
                    comments = cursor.getString(cursor.getColumnIndexOrThrow("comments")),
                    submittedAt = cursor.getLong(cursor.getColumnIndexOrThrow("submittedAt"))
                )
            }
        }
        return result
    }

    private fun migrateLegacyData(sessionId: String): Boolean {
        val migrationPrefs = appContext.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean(MIGRATION_COMPLETE, false)) return false

        val db = writableDatabase
        val history = try {
            JSONArray(
                appContext.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
                    .getString("records", "[]")
            )
        } catch (_: Exception) {
            JSONArray()
        }
        for (index in 0 until history.length()) {
            val item = history.optJSONObject(index) ?: continue
            val scanId = item.optString("source")
            if (scanId.isBlank()) continue
            val valid = item.optBoolean("valid", false)
            val inserted = db.insertWithOnConflict("scans", null, ContentValues().apply {
                put("scanId", scanId)
                put("sessionId", sessionId)
                put("imagePath", item.optString("imageUri").takeIf { it.isNotBlank() })
                if (valid) put("predictedDisease", item.optString("disease")) else putNull("predictedDisease")
                if (valid && item.has("confidence")) put("confidenceScore", item.optDouble("confidence"))
                else putNull("confidenceScore")
                put("isBananaLeaf", if (valid) 1 else 0)
                put("scannedAt", item.optLong("time", System.currentTimeMillis()))
            }, SQLiteDatabase.CONFLICT_IGNORE)
            if (inserted != -1L) enqueue(db, ENTITY_SCAN, scanId, item.optLong("time", System.currentTimeMillis()))
        }

        val legacyFeedback = appContext.getSharedPreferences("scan_feedback", Context.MODE_PRIVATE)
        for ((scanId, rawValue) in legacyFeedback.all) {
            val raw = rawValue as? String ?: continue
            val item = try { JSONObject(raw) } catch (_: Exception) { continue }
            val feedbackId = UUID.randomUUID().toString()
            val inserted = db.insertWithOnConflict("feedback", null, ContentValues().apply {
                put("feedbackId", feedbackId)
                put("scanId", scanId)
                put("accuracyRating", item.optString("rating"))
                put("comments", item.optString("comments"))
                put("submittedAt", item.optLong("time", System.currentTimeMillis()))
            }, SQLiteDatabase.CONFLICT_IGNORE)
            if (inserted != -1L) enqueue(
                db, ENTITY_FEEDBACK, feedbackId,
                item.optLong("time", System.currentTimeMillis())
            )
        }
        return true
    }

    private fun android.database.Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun removeAnonymousUserIdColumn(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_sessions_new (
                sessionId TEXT PRIMARY KEY NOT NULL,
                startedAt INTEGER NOT NULL,
                lastActiveAt INTEGER NOT NULL,
                endedAt INTEGER,
                isActive INTEGER NOT NULL CHECK (isActive IN (0, 1))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE scans_new (
                scanId TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT NOT NULL,
                imagePath TEXT,
                predictedDisease TEXT,
                confidenceScore REAL,
                isBananaLeaf INTEGER NOT NULL CHECK (isBananaLeaf IN (0, 1)),
                scannedAt INTEGER NOT NULL,
                FOREIGN KEY (sessionId) REFERENCES user_sessions_new(sessionId) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE feedback_new (
                feedbackId TEXT PRIMARY KEY NOT NULL,
                scanId TEXT NOT NULL UNIQUE,
                accuracyRating TEXT NOT NULL,
                comments TEXT NOT NULL,
                submittedAt INTEGER NOT NULL,
                FOREIGN KEY (scanId) REFERENCES scans_new(scanId) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO user_sessions_new " +
                "(sessionId, startedAt, lastActiveAt, endedAt, isActive) " +
                "SELECT sessionId, startedAt, lastActiveAt, endedAt, isActive FROM user_sessions"
        )
        db.execSQL("INSERT INTO scans_new SELECT * FROM scans")
        db.execSQL("INSERT INTO feedback_new SELECT * FROM feedback")
        db.execSQL("DROP TABLE feedback")
        db.execSQL("DROP TABLE scans")
        db.execSQL("DROP TABLE user_sessions")
        db.execSQL("ALTER TABLE user_sessions_new RENAME TO user_sessions")
        db.execSQL("ALTER TABLE scans_new RENAME TO scans")
        db.execSQL("ALTER TABLE feedback_new RENAME TO feedback")
        db.execSQL("CREATE INDEX index_scans_sessionId ON scans(sessionId)")
        db.execSQL("CREATE INDEX index_scans_scannedAt ON scans(scannedAt)")
        db.execSQL("CREATE INDEX index_feedback_scanId ON feedback(scanId)")
    }

    private fun createSyncOutbox(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_outbox (
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                queuedAt INTEGER NOT NULL,
                PRIMARY KEY (entityType, entityId)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_queuedAt ON sync_outbox(queuedAt)")
    }

    private fun enqueueExistingRecords(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL("INSERT OR REPLACE INTO sync_outbox SELECT '$ENTITY_SESSION', sessionId, $now FROM user_sessions")
        db.execSQL("INSERT OR REPLACE INTO sync_outbox SELECT '$ENTITY_SCAN', scanId, $now FROM scans")
        db.execSQL("INSERT OR REPLACE INTO sync_outbox SELECT '$ENTITY_FEEDBACK', feedbackId, $now FROM feedback")
    }

    private fun enqueue(db: SQLiteDatabase, type: String, id: String, queuedAt: Long) {
        db.insertWithOnConflict("sync_outbox", null, ContentValues().apply {
            put("entityType", type)
            put("entityId", id)
            put("queuedAt", queuedAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun removeQueued(db: SQLiteDatabase, type: String, id: String, cutoffAt: Long) {
        db.delete(
            "sync_outbox", "entityType = ? AND entityId = ? AND queuedAt <= ?",
            arrayOf(type, id, cutoffAt.toString())
        )
    }

    private fun deleteManagedPhotos(paths: List<String>) {
        val managedRoot = File(appContext.filesDir, "scan_photos").canonicalFile
        paths.forEach { value ->
            runCatching {
                val uri = Uri.parse(value)
                if (uri.scheme != "file") return@runCatching
                val file = File(requireNotNull(uri.path)).canonicalFile
                if (file.parentFile == managedRoot) file.delete()
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "bananaq.db"
        private const val DATABASE_VERSION = 4
        private const val ENTITY_SESSION = "session"
        private const val ENTITY_SCAN = "scan"
        private const val ENTITY_FEEDBACK = "feedback"
        private val ALLOWED_RATINGS = setOf(
            "Very Accurate", "Accurate", "Not Sure", "Inaccurate", "Very Inaccurate"
        )
        private const val MIGRATION_PREFS = "database_migration"
        private const val MIGRATION_COMPLETE = "sqlite_v1_complete"

        @Volatile private var instance: BananaQDatabase? = null

        fun get(context: Context): BananaQDatabase = instance ?: synchronized(this) {
            instance ?: BananaQDatabase(context).also { instance = it }
        }

        internal fun resetForTests() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
