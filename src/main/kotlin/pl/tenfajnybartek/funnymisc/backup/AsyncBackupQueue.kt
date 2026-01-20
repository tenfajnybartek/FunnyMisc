package pl.tenfajnybartek.funnymisc.backup

import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Asynchroniczna kolejka do przetwarzania backupów
 * Zapewnia zero lagów przy tworzeniu backupów
 */
class AsyncBackupQueue(private val plugin: FunnyPlugin) {

    private val queue = ConcurrentLinkedQueue<BackupTask>()
    private val executor = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "FunnyMisc-Backup-Thread").apply {
            isDaemon = true
        }
    }
    private val rateLimiter = ConcurrentHashMap<UUID, Long>()
    private val minInterval: Long = 1000 // 1 sekunda minimum między backupami tego samego gracza

    /**
     * Dodaje backup do kolejki
     *
     * @param task Task backupu
     * @return CompletableFuture z wynikiem (true = sukces)
     */
    fun queueBackup(task: BackupTask): CompletableFuture<Boolean> {
        // Rate limiting - max 1 backup/sekundę per gracz
        if (!canCreateBackup(task.uuid)) {
            return CompletableFuture.completedFuture(false)
        }

        queue.offer(task)
        return CompletableFuture.supplyAsync({ processTask(task) }, executor)
    }

    /**
     * Sprawdza czy można utworzyć backup (rate limiting)
     */
    private fun canCreateBackup(uuid: UUID): Boolean {
        val lastBackup = rateLimiter[uuid] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastBackup < minInterval) {
            return false
        }

        rateLimiter[uuid] = now
        return true
    }

    /**
     * Przetwarza task z kolejki
     */
    private fun processTask(task: BackupTask): Boolean {
        return try {
            // Usuń task z kolejki
            queue.poll()

            // Zapisz do bazy danych
            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = when (plugin.databaseManager) {
                    else -> """
                        INSERT INTO player_backups 
                        (uuid, player_name, timestamp, reason, inventory, armor, enderchest, metadata, is_restored)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """.trimIndent()
                }

                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, task.uuid.toString())
                    stmt.setString(2, task.playerName)
                    stmt.setLong(3, task.timestamp)
                    stmt.setInt(4, task.reason.id)
                    stmt.setString(5, task.inventoryData)
                    stmt.setString(6, task.armorData)
                    stmt.setString(7, task.enderchestData)
                    stmt.setString(8, task.metadataJson)
                    stmt.executeUpdate()
                }

                // Aktualizuj aktywność gracza
                updatePlayerActivity(connection, task.uuid, task.playerName)

                true
            } ?: false
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy tworzeniu backupu dla ${task.playerName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Aktualizuje aktywność gracza
     */
    private fun updatePlayerActivity(connection: java.sql.Connection, uuid: UUID, playerName: String) {
        val now = System.currentTimeMillis()
        val sql = """
            INSERT INTO player_activity (uuid, player_name, last_seen, first_seen, total_backups)
            VALUES (?, ?, ?, ?, 1)
            ON DUPLICATE KEY UPDATE
                player_name = VALUES(player_name),
                last_seen = VALUES(last_seen),
                total_backups = total_backups + 1
        """.trimIndent()

        // Dla SQLite trzeba użyć innej składni
        val sqliteSQL = """
            INSERT INTO player_activity (uuid, player_name, last_seen, first_seen, total_backups)
            VALUES (?, ?, ?, ?, 1)
            ON CONFLICT(uuid) DO UPDATE SET
                player_name = excluded.player_name,
                last_seen = excluded.last_seen,
                total_backups = total_backups + 1
        """.trimIndent()

        try {
            connection.prepareStatement(sqliteSQL).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, playerName)
                stmt.setLong(3, now)
                stmt.setLong(4, now)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            // Jeśli SQLite syntax nie działa, spróbuj MySQL
            try {
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, playerName)
                    stmt.setLong(3, now)
                    stmt.setLong(4, now)
                    stmt.executeUpdate()
                }
            } catch (e2: Exception) {
                plugin.logger.warning("Nie udało się zaktualizować aktywności gracza: ${e2.message}")
            }
        }
    }

    /**
     * Pobiera rozmiar kolejki
     */
    fun getQueueSize(): Int {
        return queue.size
    }

    /**
     * Zatrzymuje executor i przetwarza pozostałe taski
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * Task reprezentujący backup do przetworzenia
 */
data class BackupTask(
    val uuid: UUID,
    val playerName: String,
    val timestamp: Long,
    val reason: BackupReason,
    val inventoryData: String,
    val armorData: String,
    val enderchestData: String?,
    val metadataJson: String?
)
