package pl.tenfajnybartek.funnymisc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin

/**
 * Komenda do otwierania GUI depozytu/schowka
 */
class DepositCommand(private val plugin: FunnyPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            plugin.messageManager.sendMessage(sender, "messages.player-only")
            return true
        }

        if (!sender.hasPermission("funnymisc.deposit")) {
            plugin.messageManager.sendMessage(sender, "messages.no-permission")
            return true
        }

        // Otwórz GUI depozytu
        plugin.depositGUI.openDepositGUI(sender)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        // Brak argumentów - po prostu otwiera GUI
        return emptyList()
    }
}
