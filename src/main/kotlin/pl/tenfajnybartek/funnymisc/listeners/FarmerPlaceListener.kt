package pl.tenfajnybartek.funnymisc.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockBreakEvent
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.utils.ItemUtils

/**
 * Listener obsługujący stawianie i niszczenie bloków farmerów
 */
class FarmerPlaceListener(private val plugin: FunnyPlugin) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val item = event.itemInHand

        // Określ typ farmera
        val farmerType = when {
            ItemUtils.isBoyFarmer(item, plugin) -> "boyfarmer"
            ItemUtils.isSandFarmer(item, plugin) -> "sandfarmer"
            ItemUtils.isTrenchDigger(item, plugin) -> "trenchdigger"
            else -> return
        }

        val location = event.blockPlaced.location

        // Wysłaj wiadomość
        plugin.messageManager.sendMessage(player, "$farmerType.messages.farmer-placed")

        // Pobierz ustawienia
        val removeFrame = plugin.config.getBoolean("$farmerType.remove-frame-after-place", true)
        val removalDelay = plugin.config.getDouble("$farmerType.frame-removal-delay", 1.0)

        if (removeFrame) {
            // Usuń blok po opóźnieniu i rozpocznij generowanie
            val delayTicks = (removalDelay * 20).toLong()

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val block = location.block
                block.type = org.bukkit.Material.AIR

                // Stwórz odpowiedni typ farmera
                when (farmerType) {
                    "boyfarmer" -> plugin.farmerManager.createBoyFarmer(location, player.uniqueId)
                    "sandfarmer" -> plugin.farmerManager.createSandFarmer(location, player.uniqueId)
                    "trenchdigger" -> plugin.farmerManager.createTrenchDigger(location, player.uniqueId)
                }
            }, delayTicks)
        } else {
            // Od razu stwórz farmera
            when (farmerType) {
                "boyfarmer" -> plugin.farmerManager.createBoyFarmer(location, player.uniqueId)
                "sandfarmer" -> plugin.farmerManager.createSandFarmer(location, player.uniqueId)
                "trenchdigger" -> plugin.farmerManager.createTrenchDigger(location, player.uniqueId)
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        // Usunięta logika - nie używana już
    }
}
