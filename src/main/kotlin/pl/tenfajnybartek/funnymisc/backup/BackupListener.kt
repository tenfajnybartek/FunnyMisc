package pl.tenfajnybartek.funnymisc.backup

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin

/**
 * Listener do automatycznego tworzenia backupów
 */
class BackupListener(private val plugin: FunnyPlugin) : Listener {

    /**
     * Tworzy backup przy śmierci gracza
     * WAŻNE: HIGHEST priority + synchroniczne zapisywanie aby backup był tworzony PRZED zrzuceniem itemów!
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!plugin.config.getBoolean("backup.auto-backup.on-death", true)) {
            return
        }

        val player = event.entity

        // KRYTYCZNE: Zapisz SNAPSHOT inwentarza i zbroi TERAZ (synchronicznie)
        // zanim event się zakończy i zbroja zostanie zrzucona!
        val inventorySnapshot = player.inventory.contents.clone()
        val armorSnapshot = player.inventory.armorContents.clone()
        val enderchestSnapshot = if (plugin.config.getBoolean("backup.save-extra-data.enderchest", true)) {
            player.enderChest.contents.clone()
        } else {
            null
        }

        // Teraz możemy utworzyć backup asynchronicznie ze snapshotami
        plugin.backupManager.createBackupFromSnapshot(
            player,
            BackupReason.DEATH,
            inventorySnapshot,
            armorSnapshot,
            enderchestSnapshot
        ).thenAccept { success ->
            if (success && plugin.config.getBoolean("backup.debug", false)) {
                plugin.logger.info("Utworzono backup dla ${player.name} (śmierć)")
            }
        }
    }

    /**
     * Tworzy backup przy wylogowaniu (opcjonalnie)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // Aktualizuj aktywność gracza
        plugin.backupManager.updatePlayerActivity(player.uniqueId, player.name)

        // Backup przy wylogowaniu (opcjonalnie, nie zalecane dla dużych serwerów)
        if (plugin.config.getBoolean("backup.auto-backup.on-logout", false)) {
            plugin.backupManager.createBackup(player, BackupReason.LOGOUT).thenAccept { success ->
                if (success && plugin.config.getBoolean("backup.debug", false)) {
                    plugin.logger.info("Utworzono backup dla ${player.name} (wylogowanie)")
                }
            }
        }
    }

    /**
     * Aktualizuje aktywność przy logowaniu
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Aktualizuj aktywność gracza
        plugin.backupManager.updatePlayerActivity(player.uniqueId, player.name)
    }
}
