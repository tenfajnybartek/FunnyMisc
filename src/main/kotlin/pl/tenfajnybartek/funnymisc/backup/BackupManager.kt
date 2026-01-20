package pl.tenfajnybartek.funnymisc.backup

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Manager zarządzający backupami inwentarzy graczy
 */
class BackupManager(private val plugin: FunnyPlugin) {

    private val backupQueue = AsyncBackupQueue(plugin)
    private val inventoryHashes = mutableMapOf<UUID, String>()

    /**
     * Tworzy backup inwentarza gracza
     *
     * @param player Gracz
     * @param reason Powód backupu
     * @return CompletableFuture<Boolean> - true jeśli sukces
     */
    fun createBackup(player: Player, reason: BackupReason): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // Serializuj inwentarz
                val inventoryData = InventorySerializer.serialize(player.inventory.contents)
                val armorData = InventorySerializer.serialize(player.inventory.armorContents)

                // Enderchest (jeśli włączone)
                val enderchestData = if (plugin.config.getBoolean("backup.save-extra-data.enderchest", true)) {
                    InventorySerializer.serialize(player.enderChest.contents)
                } else {
                    null
                }

                // Sprawdź deduplikację
                // UWAGA: Przy śmierci inwentarz jest pusty, więc nie używamy deduplikacji dla DEATH
                if (plugin.config.getBoolean("backup.deduplication.enabled", true) && reason != BackupReason.DEATH) {
                    val hash = InventorySerializer.hashInventory(player.inventory.contents)
                    if (!shouldCreateBackup(player.uniqueId, hash)) {
                        return@supplyAsync false
                    }
                    inventoryHashes[player.uniqueId] = hash
                }

                // Rate limiting dla śmierci - zapobiega spam'owaniu backupów
                if (reason == BackupReason.DEATH) {
                    val rateLimitMs = plugin.config.getLong("backup.rate-limit.death-interval-ms", 100L)
                    if (rateLimitMs > 0) {
                        val lastBackupTime = getLastBackupTimestamp(player.uniqueId)
                        if (lastBackupTime != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastBackupTime < rateLimitMs) {
                                if (plugin.config.getBoolean("backup.debug", false)) {
                                    plugin.logger.info("Rate limit: Pominięto backup dla ${player.name} (zbyt szybko: ${now - lastBackupTime}ms < ${rateLimitMs}ms)")
                                }
                                return@supplyAsync false
                            }
                        }
                    }
                }

                // Metadata (jeśli włączone)
                val metadata = if (plugin.config.getBoolean("backup.save-extra-data.xp", true)) {
                    MetadataSerializer.createFromPlayer(player)
                } else {
                    null
                }

                val metadataJson = metadata?.let { MetadataSerializer.serialize(it) }

                // Sprawdź limit backupów
                val maxBackups = getMaxBackupsForPlayer(player)
                checkAndEnforceLimit(player.uniqueId, maxBackups)

                // Dodaj do kolejki
                val task = BackupTask(
                    uuid = player.uniqueId,
                    playerName = player.name,
                    timestamp = System.currentTimeMillis(),
                    reason = reason,
                    inventoryData = inventoryData,
                    armorData = armorData,
                    enderchestData = enderchestData,
                    metadataJson = metadataJson
                )

                backupQueue.queueBackup(task).get()
            } catch (e: Exception) {
                plugin.logger.severe("Błąd przy tworzeniu backupu dla ${player.name}: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Tworzy backup z gotowych snapshotów inwentarza (dla DEATH - synchroniczne zapisywanie)
     *
     * @param player Gracz
     * @param reason Powód backupu
     * @param inventorySnapshot Snapshot głównego inwentarza
     * @param armorSnapshot Snapshot zbroi
     * @param enderchestSnapshot Snapshot enderchesta (nullable)
     * @return CompletableFuture<Boolean> - true jeśli sukces
     */
    fun createBackupFromSnapshot(
        player: Player,
        reason: BackupReason,
        inventorySnapshot: Array<ItemStack?>,
        armorSnapshot: Array<ItemStack?>,
        enderchestSnapshot: Array<ItemStack?>?
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // Serializuj snapshoty (już mamy dane, nie czytamy z gracza)
                val inventoryData = InventorySerializer.serialize(inventorySnapshot)
                val armorData = InventorySerializer.serialize(armorSnapshot)
                val enderchestData = enderchestSnapshot?.let { InventorySerializer.serialize(it) }

                // Rate limiting dla śmierci
                if (reason == BackupReason.DEATH) {
                    val rateLimitMs = plugin.config.getLong("backup.rate-limit.death-interval-ms", 100L)
                    if (rateLimitMs > 0) {
                        val lastBackupTime = getLastBackupTimestamp(player.uniqueId)
                        if (lastBackupTime != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastBackupTime < rateLimitMs) {
                                if (plugin.config.getBoolean("backup.debug", false)) {
                                    plugin.logger.info("Rate limit: Pominięto backup dla ${player.name} (zbyt szybko: ${now - lastBackupTime}ms < ${rateLimitMs}ms)")
                                }
                                return@supplyAsync false
                            }
                        }
                    }
                }

                // Metadata (musimy pobrać synchronicznie z głównego wątku, ale to OK - gracz jeszcze żyje)
                val metadata = if (plugin.config.getBoolean("backup.save-extra-data.xp", true)) {
                    MetadataSerializer.createFromPlayer(player)
                } else {
                    null
                }

                val metadataJson = metadata?.let { MetadataSerializer.serialize(it) }

                // Sprawdź limit backupów
                val maxBackups = getMaxBackupsForPlayer(player)
                checkAndEnforceLimit(player.uniqueId, maxBackups)

                // Dodaj do kolejki
                val task = BackupTask(
                    uuid = player.uniqueId,
                    playerName = player.name,
                    timestamp = System.currentTimeMillis(),
                    reason = reason,
                    inventoryData = inventoryData,
                    armorData = armorData,
                    enderchestData = enderchestData,
                    metadataJson = metadataJson
                )

                backupQueue.queueBackup(task).get()
            } catch (e: Exception) {
                plugin.logger.severe("Błąd przy tworzeniu backupu ze snapshotów dla ${player.name}: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Sprawdza czy można utworzyć backup (deduplikacja)
     */
    private fun shouldCreateBackup(uuid: UUID, inventoryHash: String): Boolean {
        val lastHash = inventoryHashes[uuid] ?: return true

        // Jeśli hash identyczny, nie twórz backupu
        if (lastHash == inventoryHash) {
            return false
        }

        // Sprawdź minimalny interval
        val minInterval = plugin.config.getInt("backup.deduplication.min-interval", 30) * 1000L
        val lastBackupTime = getLastBackupTimestamp(uuid)

        if (lastBackupTime != null) {
            val now = System.currentTimeMillis()
            if (now - lastBackupTime < minInterval) {
                return false
            }
        }

        return true
    }

    /**
     * Pobiera timestamp ostatniego backupu gracza
     */
    private fun getLastBackupTimestamp(uuid: UUID): Long? {
        return try {
            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = "SELECT timestamp FROM player_backups WHERE uuid = ? ORDER BY timestamp DESC LIMIT 1"
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getLong("timestamp") else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sprawdza i egzekwuje limit backupów dla gracza
     */
    private fun checkAndEnforceLimit(uuid: UUID, maxBackups: Int) {
        try {
            plugin.databaseManager.getConnection()?.use { connection ->
                // Policz backupy gracza
                val countSql = "SELECT COUNT(*) as count FROM player_backups WHERE uuid = ?"
                val count = connection.prepareStatement(countSql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt("count") else 0
                }

                // Jeśli przekroczony limit, usuń najstarsze
                if (count >= maxBackups) {
                    val deleteSql = """
                        DELETE FROM player_backups
                        WHERE id IN (
                            SELECT id FROM (
                                SELECT id FROM player_backups
                                WHERE uuid = ?
                                ORDER BY timestamp ASC
                                LIMIT ?
                            ) AS old_backups
                        )
                    """.trimIndent()

                    connection.prepareStatement(deleteSql).use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.setInt(2, count - maxBackups + 1)
                        stmt.executeUpdate()
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Błąd przy sprawdzaniu limitu backupów: ${e.message}")
        }
    }

    /**
     * Pobiera backupy gracza
     *
     * @param uuid UUID gracza
     * @param limit Maksymalna liczba backupów do pobrania
     * @return Lista backupów
     */
    fun getBackups(uuid: UUID, limit: Int = 50): List<PlayerBackup> {
        val backups = mutableListOf<PlayerBackup>()

        try {
            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = """
                    SELECT * FROM player_backups
                    WHERE uuid = ?
                    ORDER BY timestamp DESC
                    LIMIT ?
                """.trimIndent()

                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setInt(2, limit)

                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        val backup = PlayerBackup(
                            id = rs.getInt("id"),
                            uuid = UUID.fromString(rs.getString("uuid")),
                            playerName = rs.getString("player_name"),
                            timestamp = rs.getLong("timestamp"),
                            reason = BackupReason.fromId(rs.getInt("reason")),
                            inventoryData = rs.getString("inventory"),
                            armorData = rs.getString("armor"),
                            enderchestData = rs.getString("enderchest"),
                            metadata = MetadataSerializer.deserialize(rs.getString("metadata")),
                            isRestored = rs.getBoolean("is_restored"),
                            restoredBy = rs.getString("restored_by")?.let { UUID.fromString(it) },
                            restoredAt = rs.getLong("restored_at").takeIf { it > 0 }
                        )
                        backups.add(backup)
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy pobieraniu backupów: ${e.message}")
            e.printStackTrace()
        }

        return backups
    }

    /**
     * Pobiera pojedynczy backup po ID
     */
    fun getBackup(id: Int): PlayerBackup? {
        return try {
            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = "SELECT * FROM player_backups WHERE id = ?"
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, id)
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        PlayerBackup(
                            id = rs.getInt("id"),
                            uuid = UUID.fromString(rs.getString("uuid")),
                            playerName = rs.getString("player_name"),
                            timestamp = rs.getLong("timestamp"),
                            reason = BackupReason.fromId(rs.getInt("reason")),
                            inventoryData = rs.getString("inventory"),
                            armorData = rs.getString("armor"),
                            enderchestData = rs.getString("enderchest"),
                            metadata = MetadataSerializer.deserialize(rs.getString("metadata")),
                            isRestored = rs.getBoolean("is_restored"),
                            restoredBy = rs.getString("restored_by")?.let { UUID.fromString(it) },
                            restoredAt = rs.getLong("restored_at").takeIf { it > 0 }
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy pobieraniu backupu #$id: ${e.message}")
            null
        }
    }

    /**
     * Przywraca backup gracza
     *
     * @param player Gracz
     * @param backupId ID backupu
     * @param admin Admin wykonujący przywrócenie
     * @return true jeśli sukces
     */
    fun restoreBackup(player: Player, backupId: Int, admin: Player): Boolean {
        val backup = getBackup(backupId) ?: return false

        // Sprawdź czy backup już użyty
        // WAŻNE: Jeśli single-use włączone w config, backup może być użyty tylko raz
        // (nawet admin z OP nie może ponownie przywrócić, chyba że ma explicite permission)
        val singleUseEnabled = plugin.config.getBoolean("backup.security.single-use", true)
        if (singleUseEnabled && backup.isRestored) {
            // Sprawdź czy admin ma EXPLICITE nadane permission (nie przez OP)
            val hasBypass = admin.isPermissionSet("funnymisc.backup.bypass.single-use") &&
                           admin.hasPermission("funnymisc.backup.bypass.single-use")
            if (!hasBypass) {
                return false
            }
        }

        return try {
            // Przywróć inwentarz (NIE czyść - nadpisz)
            val inventoryItems = InventorySerializer.deserialize(backup.inventoryData)

            // Przywróć zbroję
            val armorItems = InventorySerializer.deserialize(backup.armorData)

            // Wyczyść i ustaw
            player.inventory.clear()
            player.inventory.contents = inventoryItems
            player.inventory.armorContents = armorItems

            // Enderchest
            player.enderChest.clear()
            backup.enderchestData?.let { data ->
                val enderItems = InventorySerializer.deserialize(data)
                player.enderChest.contents = enderItems
            }

            // Przywróć metadata (jeśli jest)
            backup.metadata?.let { metadata ->
                MetadataSerializer.restoreToPlayer(player, metadata)
            }

            // Oznacz jako restored
            markAsRestored(backupId, admin.uniqueId)

            player.updateInventory()
            true
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy przywracaniu backupu #$backupId: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Oznacza backup jako przywrócony
     */
    private fun markAsRestored(backupId: Int, adminUuid: UUID) {
        try {
            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = """
                    UPDATE player_backups
                    SET is_restored = 1, restored_by = ?, restored_at = ?
                    WHERE id = ?
                """.trimIndent()

                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, adminUuid.toString())
                    stmt.setLong(2, System.currentTimeMillis())
                    stmt.setInt(3, backupId)
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Błąd przy oznaczaniu backupu jako restored: ${e.message}")
        }
    }

    /**
     * Pobiera maksymalną liczbę backupów dla gracza na podstawie uprawnień
     */
    fun getMaxBackupsForPlayer(player: Player): Int {
        return when {
            player.hasPermission("funnymisc.backup.limit.admin") ->
                plugin.config.getInt("backup.max-backups.admin", 100)
            player.hasPermission("funnymisc.backup.limit.svip") ->
                plugin.config.getInt("backup.max-backups.svip", 30)
            player.hasPermission("funnymisc.backup.limit.vip") ->
                plugin.config.getInt("backup.max-backups.vip", 15)
            else ->
                plugin.config.getInt("backup.max-backups.default", 5)
        }
    }

    /**
     * Usuwa stare backupy
     *
     * @param olderThanDays Usuń backupy starsze niż X dni
     * @return Liczba usuniętych backupów
     */
    fun cleanupOldBackups(olderThanDays: Int): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = "DELETE FROM player_backups WHERE timestamp < ?"
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setLong(1, cutoffTime)
                    stmt.executeUpdate()
                }
            } ?: 0
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy czyszczeniu starych backupów: ${e.message}")
            0
        }
    }

    /**
     * Usuwa backupy nieaktywnych graczy
     *
     * @param daysOffline Usuń backupy graczy offline X+ dni
     * @param keepLatest Czy zachować najnowszy backup
     * @param minToKeep Minimalna liczba backupów do zachowania
     * @return Liczba usuniętych backupów
     */
    fun cleanupInactivePlayerBackups(daysOffline: Int, keepLatest: Boolean, minToKeep: Int): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysOffline * 24 * 60 * 60 * 1000L)
            var deleted = 0

            plugin.databaseManager.getConnection()?.use { connection ->
                // Znajdź nieaktywnych graczy
                val findInactiveSql = "SELECT uuid FROM player_activity WHERE last_seen < ?"
                val inactiveUUIDs = mutableListOf<String>()

                connection.prepareStatement(findInactiveSql).use { stmt ->
                    stmt.setLong(1, cutoffTime)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        inactiveUUIDs.add(rs.getString("uuid"))
                    }
                }

                // Usuń backupy dla każdego nieaktywnego gracza
                inactiveUUIDs.forEach { uuid ->
                    if (keepLatest) {
                        // Usuń wszystkie oprócz najnowszych X
                        val deleteSql = """
                            DELETE FROM player_backups
                            WHERE uuid = ?
                            AND id NOT IN (
                                SELECT id FROM (
                                    SELECT id FROM player_backups
                                    WHERE uuid = ?
                                    ORDER BY timestamp DESC
                                    LIMIT ?
                                ) AS kept_backups
                            )
                        """.trimIndent()

                        connection.prepareStatement(deleteSql).use { stmt ->
                            stmt.setString(1, uuid)
                            stmt.setString(2, uuid)
                            stmt.setInt(3, minToKeep)
                            deleted += stmt.executeUpdate()
                        }
                    } else {
                        // Usuń wszystkie
                        val deleteSql = "DELETE FROM player_backups WHERE uuid = ?"
                        connection.prepareStatement(deleteSql).use { stmt ->
                            stmt.setString(1, uuid)
                            deleted += stmt.executeUpdate()
                        }
                    }
                }
            }

            deleted
        } catch (e: Exception) {
            plugin.logger.severe("Błąd przy czyszczeniu backupów nieaktywnych graczy: ${e.message}")
            0
        }
    }

    /**
     * Sprawdza czy gracz jest nieaktywny
     */
    fun isPlayerInactive(uuid: UUID, daysOffline: Int): Boolean {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysOffline * 24 * 60 * 60 * 1000L)

            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = "SELECT last_seen FROM player_activity WHERE uuid = ?"
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        val lastSeen = rs.getLong("last_seen")
                        lastSeen < cutoffTime
                    } else {
                        false
                    }
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Aktualizuje aktywność gracza
     */
    fun updatePlayerActivity(uuid: UUID, playerName: String) {
        try {
            val now = System.currentTimeMillis()

            plugin.databaseManager.getConnection()?.use { connection ->
                val sql = """
                    INSERT INTO player_activity (uuid, player_name, last_seen, first_seen, total_backups)
                    VALUES (?, ?, ?, ?, 0)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name = excluded.player_name,
                        last_seen = excluded.last_seen
                """.trimIndent()

                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, playerName)
                    stmt.setLong(3, now)
                    stmt.setLong(4, now)
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            // Ignore - nie krytyczne
        }
    }

    /**
     * Zamyka queue i zwalnia zasoby
     */
    fun shutdown() {
        backupQueue.shutdown()
    }
}
