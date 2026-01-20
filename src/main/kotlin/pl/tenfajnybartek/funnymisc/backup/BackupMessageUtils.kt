package pl.tenfajnybartek.funnymisc.backup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Utility do zarządzania wiadomościami systemu backupów
 * Wszystkie teksty można skonfigurować w config.yml
 */
object BackupMessageUtils {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    /**
     * Wysyła wiadomość z config.yml z placeholderami
     */
    fun sendMessage(plugin: FunnyPlugin, sender: CommandSender, path: String, vararg placeholders: Pair<String, String>) {
        val message = plugin.messageManager.getMessage(path, *placeholders)
        sender.sendMessage(message)
    }

    /**
     * Formatuje datę backupu
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Tworzy komponent z konfiguracją
     */
    fun createComponent(plugin: FunnyPlugin, path: String, vararg placeholders: Pair<String, String>): Component {
        return plugin.messageManager.getMessage(path, *placeholders)
    }

    /**
     * Tworzy komponent tekstowy z kolorami (fallback jeśli nie ma w config)
     */
    fun createTextComponent(text: String, color: NamedTextColor, decoration: TextDecoration? = null): Component {
        return if (decoration != null) {
            Component.text(text, color, decoration)
        } else {
            Component.text(text, color)
        }
    }

    /**
     * Wysyła pomoc komendy /backup
     */
    fun sendHelpMessage(plugin: FunnyPlugin, sender: CommandSender) {
        val helpLines = plugin.config.getStringList("backup.messages.help.lines")

        if (helpLines.isEmpty()) {
            // Fallback jeśli nie ma w configu
            sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
            sender.sendMessage(plugin.messageManager.parse("<gold><bold>Backup System</bold></gold> <dark_gray>-</dark_gray> <gray>Pomoc</gray>"))
            sender.sendMessage(Component.empty())
            sender.sendMessage(plugin.messageManager.parse("<yellow>/backup <gracz></yellow> <dark_gray>-</dark_gray> <gray>Przeglądaj backupy gracza</gray>"))
            sender.sendMessage(plugin.messageManager.parse("<yellow>/backup create <gracz></yellow> <dark_gray>-</dark_gray> <gray>Stwórz manualny backup</gray>"))
            sender.sendMessage(plugin.messageManager.parse("<yellow>/backup cleanup</yellow> <dark_gray>-</dark_gray> <gray>Wyczyść stare backupy</gray>"))
            sender.sendMessage(plugin.messageManager.parse("<yellow>/backup info <id></yellow> <dark_gray>-</dark_gray> <gray>Informacje o backupie</gray>"))
            sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
        } else {
            // Z configu
            helpLines.forEach { line ->
                sender.sendMessage(plugin.messageManager.parse(line))
            }
        }
    }

    /**
     * Wysyła informacje o backupie (tekstowo)
     */
    fun sendBackupInfo(plugin: FunnyPlugin, sender: CommandSender, backup: PlayerBackup) {
        val date = formatDate(backup.timestamp)

        val infoLines = plugin.config.getStringList("backup.messages.info.lines")

        if (infoLines.isEmpty()) {
            // Fallback
            sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
            sender.sendMessage(plugin.messageManager.parse("<gold><bold>Backup #${backup.id}</bold></gold>"))
            sender.sendMessage(Component.empty())
            sender.sendMessage(plugin.messageManager.parse("<gray>Gracz:</gray> <yellow>${backup.playerName}</yellow>"))
            sender.sendMessage(plugin.messageManager.parse("<gray>Data:</gray> <white>$date</white>"))
            sender.sendMessage(plugin.messageManager.parse("<gray>Powód:</gray> <white>${backup.reason.displayName}</white>"))

            val restoredStatus = if (backup.isRestored) "<green>✔ TAK</green>" else "<red>✖ NIE</red>"
            sender.sendMessage(plugin.messageManager.parse("<gray>Przywrócony:</gray> $restoredStatus"))

            if (backup.isRestored) {
                backup.restoredBy?.let { adminUuid ->
                    val adminName = org.bukkit.Bukkit.getOfflinePlayer(adminUuid).name ?: "Unknown"
                    sender.sendMessage(plugin.messageManager.parse("<gray>Przywrócony przez:</gray> <yellow>$adminName</yellow>"))
                }

                backup.restoredAt?.let { timestamp ->
                    val restoredDate = formatDate(timestamp)
                    sender.sendMessage(plugin.messageManager.parse("<gray>Data przywrócenia:</gray> <white>$restoredDate</white>"))
                }
            }

            backup.metadata?.let { meta ->
                sender.sendMessage(Component.empty())
                sender.sendMessage(plugin.messageManager.parse("<gray>Lokalizacja:</gray> <white>${meta.location.format()}</white>"))
                sender.sendMessage(plugin.messageManager.parse("<gray>XP:</gray> <white>Poziom ${meta.xp.level} (${(meta.xp.exp * 100).toInt()}%)</white>"))
                sender.sendMessage(plugin.messageManager.parse("<gray>Zdrowie:</gray> <white>${meta.health}/20</white>"))
                sender.sendMessage(plugin.messageManager.parse("<gray>Jedzenie:</gray> <white>${meta.food}/20</white>"))
                sender.sendMessage(plugin.messageManager.parse("<gray>Tryb gry:</gray> <white>${meta.gamemode}</white>"))

                if (meta.effects.isNotEmpty()) {
                    sender.sendMessage(plugin.messageManager.parse("<gray>Efekty:</gray> <white>${meta.effects.size}</white>"))
                    meta.effects.forEach { effect ->
                        sender.sendMessage(plugin.messageManager.parse("  <dark_gray>└</dark_gray> <white>${effect.type} ${effect.amplifier + 1}</white> <gray>(${effect.duration / 20}s)</gray>"))
                    }
                }
            }

            sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
        } else {
            // Z configu - z placeholderami
            val placeholders = mapOf(
                "id" to backup.id.toString(),
                "player" to backup.playerName,
                "date" to date,
                "reason" to backup.reason.displayName,
                "restored" to if (backup.isRestored) "TAK" else "NIE"
            )

            infoLines.forEach { line ->
                var processedLine = line
                placeholders.forEach { (key, value) ->
                    processedLine = processedLine.replace("{$key}", value)
                }
                sender.sendMessage(plugin.messageManager.parse(processedLine))
            }
        }
    }

    /**
     * Wysyła listę backupów (tekstowo - dla konsoli)
     */
    fun sendBackupsList(plugin: FunnyPlugin, sender: CommandSender, backups: List<PlayerBackup>, playerName: String) {
        sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
        sender.sendMessage(plugin.messageManager.parse("<gold><bold>Backupy gracza</bold></gold> <yellow>$playerName</yellow> <dark_gray>(${backups.size})</dark_gray>"))
        sender.sendMessage(Component.empty())

        backups.forEach { backup ->
            val date = formatDate(backup.timestamp)
            val restored = if (backup.isRestored) "<green>✔ Przywrócony</green>" else "<gray>✖ Aktywny</gray>"

            sender.sendMessage(plugin.messageManager.parse("<yellow>#${backup.id}</yellow> <dark_gray>-</dark_gray> <gray>$date</gray> <dark_gray>-</dark_gray> <white>${backup.reason.displayName}</white> <dark_gray>-</dark_gray> $restored"))

            backup.metadata?.let { meta ->
                sender.sendMessage(plugin.messageManager.parse("  <dark_gray>└</dark_gray> <gray>Lokalizacja:</gray> <white>${meta.location.format()}</white>"))
            }
        }

        sender.sendMessage(plugin.messageManager.parse("<gray><strikethrough>                                                </strikethrough></gray>"))
    }

    /**
     * Tworzy nazwę itemu backupu dla GUI
     */
    fun createBackupItemName(plugin: FunnyPlugin, backup: PlayerBackup, date: String): Component {
        val path = if (backup.isRestored) "backup.messages.gui.item.restored.name" else "backup.messages.gui.item.active.name"
        return plugin.messageManager.getMessage(path, "date" to date)
    }

    /**
     * Tworzy lore itemu backupu dla GUI
     */
    fun createBackupItemLore(plugin: FunnyPlugin, backup: PlayerBackup, date: String): List<Component> {
        val lore = mutableListOf<Component>()

        // Powód
        lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.reason", "reason" to backup.reason.displayName))

        // Lokalizacja i XP
        backup.metadata?.let { metadata ->
            lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.location", "location" to metadata.location.format()))
            lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.xp", "level" to metadata.xp.level.toString()))
        }

        lore.add(Component.empty())

        if (backup.isRestored) {
            // Już przywrócony
            backup.restoredBy?.let { adminUuid ->
                val adminName = org.bukkit.Bukkit.getOfflinePlayer(adminUuid).name ?: "Unknown"
                lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.restored.by", "admin" to adminName))
            }

            backup.restoredAt?.let { timestamp ->
                val restoredDate = formatDate(timestamp)
                lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.restored.date", "date" to restoredDate))
            }
        } else {
            // Aktywny backup
            lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.active.click"))
            lore.add(plugin.messageManager.getMessage("backup.messages.gui.item.active.shift"))
        }

        return lore
    }

    /**
     * Tworzy nazwę GUI
     */
    fun createGUITitle(plugin: FunnyPlugin, playerName: String): Component {
        return plugin.messageManager.getMessage("backup.messages.gui.title", "player" to playerName)
    }

    /**
     * Tworzy nazwę preview GUI
     */
    fun createPreviewTitle(plugin: FunnyPlugin, date: String): Component {
        return plugin.messageManager.getMessage("backup.messages.gui.preview.title", "date" to date)
    }
}
