package pl.tenfajnybartek.funnymisc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.utils.ItemUtils

/**
 * Główna komenda pluginu
 */
class FunnyMiscCommand(private val plugin: FunnyPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "give" -> {
                if (sender !is Player) {
                    plugin.messageManager.sendMessage(sender, "messages.player-only")
                    return true
                }

                if (!sender.hasPermission("funnymisc.give")) {
                    plugin.messageManager.sendMessage(sender, "messages.no-permission")
                    return true
                }

                if (args.size < 2) {
                    plugin.messageManager.sendMessageNoPrefix(sender, "messages.usage-give")
                    return true
                }

                val itemType = args[1].lowercase()
                val itemName = when(itemType) {
                    "boyfarmer" -> "Boy Farmer"
                    "sandfarmer" -> "Sand Farmer"
                    "trenchdigger" -> "Trench Digger"
                    "stoniarka" -> "Stoniarka"
                    "stoniarka-collector" -> "Zbieracz Stoniarek"
                    "mega-kilof", "megakilof" -> "Mega Kilof"
                    else -> {
                        plugin.messageManager.sendMessage(sender, "messages.unknown-item", "item" to args[1])
                        plugin.messageManager.sendMessageNoPrefix(sender, "messages.available-items")
                        return true
                    }
                }

                val target = if (args.size >= 3) {
                    plugin.server.getPlayer(args[2])
                } else {
                    sender
                }

                if (target == null) {
                    plugin.messageManager.sendMessage(sender, "messages.player-not-found")
                    return true
                }

                val amount = if (args.size >= 4) {
                    args[3].toIntOrNull() ?: 1
                } else {
                    1
                }

                val item = when (itemType) {
                    "boyfarmer" -> ItemUtils.createBoyFarmerItem(plugin)
                    "sandfarmer" -> ItemUtils.createSandFarmerItem(plugin)
                    "trenchdigger" -> ItemUtils.createTrenchDiggerItem(plugin)
                    "stoniarka" -> ItemUtils.createStoniarkaItem(plugin)
                    "stoniarka-collector" -> ItemUtils.createCollectorItem(plugin)
                    "mega-kilof", "megakilof" -> ItemUtils.createMegaPickaxe(plugin)
                    else -> return true
                }

                item.amount = amount
                target.inventory.addItem(item)

                plugin.messageManager.sendMessage(sender, "messages.item-given",
                    "amount" to amount.toString(),
                    "item" to itemName,
                    "player" to target.name
                )

                if (target != sender) {
                    plugin.messageManager.sendMessage(target, "messages.item-received",
                        "amount" to amount.toString(),
                        "item" to itemName
                    )
                }
            }

            "reload" -> {
                if (!sender.hasPermission("funnymisc.reload")) {
                    plugin.messageManager.sendMessage(sender, "messages.no-permission")
                    return true
                }

                plugin.reloadConfig()
                plugin.reload()
                plugin.messageManager.sendMessage(sender, "messages.config-reloaded")
            }

            "info" -> {
                val farmers = plugin.farmerManager.getActiveFarmers()
                plugin.messageManager.sendMessageNoPrefix(sender, "messages.info-header")
                plugin.messageManager.sendMessageNoPrefix(sender, "messages.info-active-farmers", "count" to farmers.size.toString())
                plugin.messageManager.sendMessageNoPrefix(sender, "messages.info-version", "version" to plugin.pluginMeta.version)
            }

            "dajkkilof" -> {
                if (sender !is Player) {
                    plugin.messageManager.sendMessage(sender, "messages.player-only")
                    return true
                }

                if (!sender.hasPermission("funnymisc.dajkkilof")) {
                    plugin.messageManager.sendMessage(sender, "messages.no-permission")
                    return true
                }

                // Daj materiały do craftingu Mega Kilofa
                // 3x Diamond Block + 2x Stick
                val diamondBlocks = ItemUtils.createDiamondBlock(3)
                val sticks = ItemUtils.createStick(2)

                sender.inventory.addItem(diamondBlocks)
                sender.inventory.addItem(sticks)

                plugin.messageManager.sendMessage(sender, "mega-kilof.messages.crafting-items-given")
            }

            "deposit", "depozyt", "schowek" -> {
                if (sender !is Player) {
                    plugin.messageManager.sendMessage(sender, "messages.player-only")
                    return true
                }

                if (!sender.hasPermission("funnymisc.deposit")) {
                    plugin.messageManager.sendMessage(sender, "messages.no-permission")
                    return true
                }

                plugin.depositGUI.openDepositGUI(sender)
            }

            else -> sendHelp(sender)
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("give", "reload", "info", "dajkkilof", "deposit").filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0].equals("give", true)) {
                listOf("boyfarmer", "sandfarmer", "trenchdigger", "stoniarka", "stoniarka-collector", "mega-kilof").filter { it.startsWith(args[1].lowercase()) }
            } else emptyList()
            3 -> if (args[0].equals("give", true)) {
                plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], true) }
            } else emptyList()
            else -> emptyList()
        }
    }

    private fun sendHelp(sender: CommandSender) {
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-header")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-give-boyfarmer")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-give-sandfarmer")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-give-trenchdigger")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-give-stoniarka")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-give-collector")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-deposit")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-reload")
        plugin.messageManager.sendMessageNoPrefix(sender, "messages.help-info")
    }
}
