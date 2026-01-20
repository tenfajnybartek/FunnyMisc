package pl.tenfajnybartek.funnymisc.backup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * GUI do przeglądania i zarządzania backupami
 */
class BackupGUI(private val plugin: FunnyPlugin) : Listener {

    private val confirmations = ConcurrentHashMap<UUID, Pair<Int, Long>>()
    private val restoringInProgress = ConcurrentHashMap<UUID, Boolean>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    companion object {
        private const val BACKUPS_PER_PAGE = 45
        private const val CONFIRMATION_TIMEOUT = 5000L
    }

    fun openBackupList(viewer: Player, targetUUID: UUID, page: Int) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val allBackups = plugin.backupManager.getBackups(targetUUID, 1000)
            val totalPages = (allBackups.size + BACKUPS_PER_PAGE - 1) / BACKUPS_PER_PAGE
            val currentPage = page.coerceIn(0, maxOf(0, totalPages - 1))

            val startIndex = currentPage * BACKUPS_PER_PAGE
            val endIndex = minOf(startIndex + BACKUPS_PER_PAGE, allBackups.size)
            val backupsOnPage = allBackups.subList(startIndex, endIndex)

            val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: "Unknown"

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val holder = BackupInventoryHolder(backupsOnPage, targetUUID, currentPage, totalPages, plugin)
                val inv = holder.inventory

                fillBackupInventory(inv, backupsOnPage, currentPage, totalPages, targetName)

                viewer.openInventory(inv)
            })
        })
    }

    private fun fillBackupInventory(
        inv: Inventory,
        backups: List<PlayerBackup>,
        currentPage: Int,
        totalPages: Int,
        targetName: String
    ) {
        inv.clear()

        backups.forEachIndexed { index, backup ->
            inv.setItem(index, createBackupItem(backup))
        }

        val filler = createFillerItem()
        for (i in 45..53) {
            inv.setItem(i, filler)
        }

        if (currentPage > 0) {
            inv.setItem(45, createPreviousPageItem())
        }

        inv.setItem(49, createInfoItem(targetName, backups.size, currentPage + 1, totalPages))

        if (currentPage < totalPages - 1) {
            inv.setItem(53, createNextPageItem())
        }
    }

    private fun createBackupItem(backup: PlayerBackup): ItemStack {
        val material = if (backup.isRestored) Material.LIME_STAINED_GLASS_PANE else Material.CHEST
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        val date = dateFormat.format(Date(backup.timestamp))
        meta.displayName(BackupMessageUtils.createBackupItemName(plugin, backup, date))
        meta.lore(BackupMessageUtils.createBackupItemLore(plugin, backup, date))

        item.itemMeta = meta
        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

    private fun createPreviousPageItem(): ItemStack {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta ?: return item
        meta.displayName(plugin.messageManager.getMessage("backup.messages.gui.navigation.previous"))
        item.itemMeta = meta
        return item
    }

    private fun createNextPageItem(): ItemStack {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta ?: return item
        meta.displayName(plugin.messageManager.getMessage("backup.messages.gui.navigation.next"))
        item.itemMeta = meta
        return item
    }

    private fun createInfoItem(playerName: String, backupCount: Int, currentPage: Int, totalPages: Int): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta ?: return item

        meta.displayName(plugin.messageManager.getMessage("backup.messages.gui.info.name"))

        val lore = mutableListOf<Component>()
        lore.add(plugin.messageManager.getMessage("backup.messages.gui.info.player", "player" to playerName))
        lore.add(plugin.messageManager.getMessage("backup.messages.gui.info.count", "count" to backupCount.toString()))
        lore.add(plugin.messageManager.getMessage("backup.messages.gui.info.page", "page" to "$currentPage/$totalPages"))

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // Obsługa głównego GUI backupów
        if (event.inventory.holder is BackupInventoryHolder) {
            val holder = event.inventory.holder as BackupInventoryHolder
            event.isCancelled = true

            val player = event.whoClicked as? Player ?: return
            val slot = event.slot

            if (!player.hasPermission("funnymisc.backup.view")) {
                BackupMessageUtils.sendMessage(plugin, player, "backup.messages.no-permission")
                player.closeInventory()
                return
            }

            when (slot) {
                45 -> {
                    if (holder.page > 0) {
                        openBackupList(player, holder.targetUUID, holder.page - 1)
                    }
                }

                53 -> {
                    if (holder.page < holder.totalPages - 1) {
                        openBackupList(player, holder.targetUUID, holder.page + 1)
                    }
                }

                49 -> return

                in 0..44 -> {
                    if (slot >= holder.backups.size) return

                    val backup = holder.backups[slot]

                    if (event.click.isShiftClick) {
                        handleRestoreBackup(player, backup, holder)
                    } else {
                        openBackupPreview(player, backup, holder.targetUUID, holder.page)
                    }
                }
            }
            return
        }

        // Obsługa preview inventory
        if (event.inventory.holder is BackupPreviewHolder) {
            event.isCancelled = true

            val holder = event.inventory.holder as BackupPreviewHolder
            val player = event.whoClicked as? Player ?: return
            val slot = event.slot

            // Przycisk powrotu (slot 49)
            if (slot == 49) {
                player.closeInventory()
                // Wróć do listy backupów gracza
                openBackupList(player, holder.targetUUID, holder.page)
            }
            return
        }
    }

    private fun handleRestoreBackup(player: Player, backup: PlayerBackup, holder: BackupInventoryHolder) {
        if (!player.hasPermission("funnymisc.backup.restore")) {
            BackupMessageUtils.sendMessage(plugin, player, "backup.no-permission")
            return
        }

        val targetPlayer = Bukkit.getPlayer(holder.targetUUID)
        if (targetPlayer == null) {
            BackupMessageUtils.sendMessage(plugin, player, "backup.messages.player-not-online", "player" to backup.playerName)
            return
        }

        if (targetPlayer.uniqueId != player.uniqueId &&
            !player.hasPermission("funnymisc.backup.restore.others")) {
            BackupMessageUtils.sendMessage(plugin, player, "backup.no-permission")
            return
        }

        if (backup.isRestored && !player.hasPermission("funnymisc.backup.bypass.single-use")) {
            BackupMessageUtils.sendMessage(plugin, player, "backup.messages.already-restored")
            return
        }

        // Sprawdź czy przywracanie już nie jest w toku (zapobiega double-click)
        if (restoringInProgress.getOrDefault(player.uniqueId, false)) {
            return // Ciche ignorowanie - przywracanie w toku
        }

        val confirmation = confirmations[player.uniqueId]
        val now = System.currentTimeMillis()

        if (confirmation != null && confirmation.first == backup.id &&
            now - confirmation.second < CONFIRMATION_TIMEOUT) {
            confirmations.remove(player.uniqueId)
            restoringInProgress[player.uniqueId] = true // BLOKADA
            player.closeInventory()

            // Przywracanie MUSI być sync (setHealth, inventory operations)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val success = plugin.backupManager.restoreBackup(targetPlayer, backup.id, player)

                // Zdejmij blokadę
                restoringInProgress.remove(player.uniqueId)

                if (success) {
                    val date = dateFormat.format(Date(backup.timestamp))
                    BackupMessageUtils.sendMessage(
                        plugin, player, "backup.messages.restored",
                        "date" to date,
                        "player" to targetPlayer.name
                    )

                    if (targetPlayer.uniqueId != player.uniqueId) {
                        BackupMessageUtils.sendMessage(
                            plugin, targetPlayer, "backup.messages.restored-target",
                            "date" to date,
                            "admin" to player.name
                        )
                    }
                } else {
                    BackupMessageUtils.sendMessage(plugin, player, "backup.messages.restore-failed")
                }
            })
        } else {
            confirmations[player.uniqueId] = backup.id to now
            BackupMessageUtils.sendMessage(plugin, player, "backup.messages.confirm-restore")

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                confirmations.remove(player.uniqueId)
            }, (CONFIRMATION_TIMEOUT / 50))
        }
    }

    private fun openBackupPreview(player: Player, backup: PlayerBackup, targetUUID: UUID, page: Int) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val inventoryItems = InventorySerializer.deserialize(backup.inventoryData)
                val armorItems = InventorySerializer.deserialize(backup.armorData)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val date = dateFormat.format(Date(backup.timestamp))
                    val title = BackupMessageUtils.createPreviewTitle(plugin, date)
                    val previewHolder = BackupPreviewHolder(title, targetUUID, page)
                    val previewInv = previewHolder.inventory

                    for (i in 0 until minOf(36, inventoryItems.size)) {
                        inventoryItems[i]?.let { item ->
                            previewInv.setItem(i, item)
                        }
                    }

                    for (i in 0 until minOf(4, armorItems.size)) {
                        armorItems[i]?.let { item ->
                            previewInv.setItem(36 + i, item)
                        }
                    }

                    if (inventoryItems.size > 40) {
                        inventoryItems[40]?.let { item ->
                            previewInv.setItem(40, item)
                        }
                    }

                    val backButton = ItemStack(Material.BARRIER)
                    val backMeta = backButton.itemMeta
                    backMeta?.displayName(plugin.messageManager.getMessage("backup.messages.gui.navigation.back"))
                    backButton.itemMeta = backMeta
                    previewInv.setItem(49, backButton)

                    player.openInventory(previewInv)
                })
            } catch (e: Exception) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    BackupMessageUtils.sendMessage(plugin, player, "backup.messages.preview-failed")
                })
            }
        })
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        confirmations.remove(player.uniqueId)
    }
}

/**
 * Holder dla preview backupu
 */
class BackupPreviewHolder(
    private val title: Component,
    val targetUUID: UUID,
    val page: Int = 0
) : InventoryHolder {
    override fun getInventory(): Inventory {
        return Bukkit.createInventory(this, 54, title)
    }
}

class BackupInventoryHolder(
    val backups: List<PlayerBackup>,
    val targetUUID: UUID,
    val page: Int,
    val totalPages: Int,
    private val plugin: FunnyPlugin
) : InventoryHolder {

    override fun getInventory(): Inventory {
        val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: "Unknown"
        val title = BackupMessageUtils.createGUITitle(plugin, targetName)

        return Bukkit.createInventory(this, 54, title)
    }
}
