package pl.tenfajnybartek.funnymisc.backup

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID

/**
 * Komenda /backup do zarządzania backupami
 *
 * Użycie:
 * - /backup <gracz> - Przeglądaj backupy gracza
 * - /backup create <gracz> - Stwórz manualny backup
 * - /backup cleanup - Wyczyść stare backupy
 * - /backup info <id> - Informacje o backupie
 */
class BackupCommand(private val plugin: FunnyPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        // Sprawdź czy sender ma uprawnienia
        if (!sender.hasPermission("funnymisc.backup.view")) {
            sender.sendMessage(plugin.messageManager.getMessage("backup.messages.no-permission"))
            return true
        }

        when (args.size) {
            0 -> {
                // /backup - pokaż pomoc
                showHelp(sender)
                return true
            }

            1 -> {
                val arg = args[0].lowercase()

                when (arg) {
                    "help", "?" -> {
                        showHelp(sender)
                        return true
                    }

                    "cleanup" -> {
                        // /backup cleanup - wyczyść stare backupy
                        if (!sender.hasPermission("funnymisc.backup.cleanup")) {
                            sender.sendMessage(plugin.messageManager.getMessage("backup.messages.no-permission"))
                            return true
                        }

                        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                            val retentionDays = plugin.config.getInt("backup.cleanup.retention-days", 30)
                            val deleted = plugin.backupManager.cleanupOldBackups(retentionDays)

                            val message = plugin.messageManager.getMessage(
                                "backup.cleanup-complete",
                                "deleted" to deleted.toString(),
                                "days" to retentionDays.toString()
                            )

                            sender.sendMessage(message)
                        })
                        return true
                    }

                    else -> {
                        // /backup <gracz> - pokaż backupy gracza
                        val targetPlayer = Bukkit.getPlayerExact(arg) ?: Bukkit.getOfflinePlayer(arg)

                        if (sender is Player) {
                            // Sprawdź czy może przeglądać backupy innych
                            if (targetPlayer.uniqueId != sender.uniqueId &&
                                !sender.hasPermission("funnymisc.backup.view.others")) {
                                sender.sendMessage(plugin.messageManager.getMessage("backup.messages.no-permission"))
                                return true
                            }

                            // Otwórz GUI
                            BackupGUI(plugin).openBackupList(sender, targetPlayer.uniqueId, 0)
                        } else {
                            // Console - pokaż tekstowo
                            showBackupsText(sender, targetPlayer.uniqueId, targetPlayer.name ?: "Unknown")
                        }
                        return true
                    }
                }
            }

            2 -> {
                val subCommand = args[0].lowercase()
                val target = args[1]

                when (subCommand) {
                    "create" -> {
                        // /backup create <gracz> - stwórz manualny backup
                        if (!sender.hasPermission("funnymisc.backup.create")) {
                            sender.sendMessage(plugin.messageManager.getMessage("backup.messages.no-permission"))
                            return true
                        }

                        val targetPlayer = Bukkit.getPlayerExact(target)
                        if (targetPlayer == null) {
                            sender.sendMessage(
                                plugin.messageManager.getMessage("backup.messages.player-not-online", "player" to target)
                            )
                            return true
                        }

                        plugin.backupManager.createBackup(targetPlayer, BackupReason.MANUAL).thenAccept { success ->
                            if (success) {
                                sender.sendMessage(
                                    plugin.messageManager.getMessage("backup.messages.created-manual", "player" to targetPlayer.name)
                                )
                            } else {
                                sender.sendMessage(plugin.messageManager.getMessage("backup.messages.create-failed"))
                            }
                        }
                        return true
                    }

                    "info" -> {
                        // /backup info <id> - informacje o backupie
                        if (!sender.hasPermission("funnymisc.backup.view")) {
                            sender.sendMessage(plugin.messageManager.getMessage("backup.messages.no-permission"))
                            return true
                        }

                        val backupId = target.toIntOrNull()
                        if (backupId == null) {
                            sender.sendMessage(plugin.messageManager.getMessage("backup.messages.invalid-id"))
                            return true
                        }

                        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                            val backup = plugin.backupManager.getBackup(backupId)
                            if (backup == null) {
                                sender.sendMessage(plugin.messageManager.getMessage("backup.messages.not-found"))
                                return@Runnable
                            }

                            showBackupInfo(sender, backup)
                        })
                        return true
                    }

                    else -> {
                        sender.sendMessage(plugin.messageManager.getMessage("backup.messages.invalid-subcommand"))
                        return true
                    }
                }
            }

            else -> {
                sender.sendMessage(plugin.messageManager.getMessage("backup.messages.invalid-usage"))
                return true
            }
        }
    }

    /**
     * Pokazuje pomoc komendy
     */
    private fun showHelp(sender: CommandSender) {
        BackupMessageUtils.sendHelpMessage(plugin, sender)
    }

    /**
     * Pokazuje backupy w formie tekstowej (dla konsoli)
     */
    private fun showBackupsText(sender: CommandSender, uuid: UUID, playerName: String) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val backups = plugin.backupManager.getBackups(uuid, 10)

            if (backups.isEmpty()) {
                BackupMessageUtils.sendMessage(plugin, sender, "backup.no-backups", "player" to playerName)
                return@Runnable
            }

            BackupMessageUtils.sendBackupsList(plugin, sender, backups, playerName)
        })
    }

    /**
     * Pokazuje szczegółowe informacje o backupie
     */
    private fun showBackupInfo(sender: CommandSender, backup: PlayerBackup) {
        BackupMessageUtils.sendBackupInfo(plugin, sender, backup)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {

        if (!sender.hasPermission("funnymisc.backup.view")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // /backup <tab>
                val suggestions = mutableListOf("help", "cleanup", "create", "info")

                // Dodaj graczy online
                suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })

                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }

            2 -> {
                // /backup create/info <tab>
                when (args[0].lowercase()) {
                    "create" -> {
                        // Lista graczy online
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "info" -> {
                        // Nie podpowiadaj ID
                        emptyList()
                    }
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
