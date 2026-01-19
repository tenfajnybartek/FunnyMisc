package pl.tenfajnybartek.funnymisc.deposit

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin

/**
 * Listener zarządzający automatycznym przenoszeniem nadmiaru itemów do depozytu
 */
class DepositListener(private val plugin: FunnyPlugin) : Listener {

    /**
     * Obsługuje podnoszenie itemów - przenosi nadmiar do depozytu
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item.itemStack

        handleItemPickup(player, item)
    }

    /**
     * Obsługuje klikanie w inventory - sprawdza limity po przeniesieniu
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Delay sprawdzenia - poczekaj aż inventory się zaktualizuje
        // ASYNC - operacje DB nie mogą blokować głównego wątku!
        plugin.server.scheduler.runTaskLaterAsynchronously(plugin, Runnable {
            checkAndMoveExcess(player)
        }, 1L)
    }


    /**
     * Obsługuje podniesienie pojedynczego itemu
     */
    private fun handleItemPickup(player: Player, item: ItemStack) {
        val material = item.type
        val depositManager = plugin.depositManager

        // Sprawdź czy ten materiał ma limit (cache - szybkie)
        if (!depositManager.hasLimit(material)) {
            return
        }

        // Delay sprawdzenia - poczekaj aż item zostanie dodany do ekwipunku
        // ASYNC - operacje DB
        plugin.server.scheduler.runTaskLaterAsynchronously(plugin, Runnable {
            moveExcessToDeposit(player, material)
        }, 2L)
    }

    /**
     * Przenosi nadmiar konkretnego materiału do depozytu
     * ASYNC - tylko jeden sync call na końcu
     */
    private fun moveExcessToDeposit(player: Player, material: Material) {
        val depositManager = plugin.depositManager
        val limit = depositManager.getLimit(material)

        if (limit <= 0) return

        // Policz ile gracz ma tego materiału (read-only, bezpieczne w async)
        val currentAmount = player.inventory.all(material).values.sumOf { it.amount }

        if (currentAmount > limit) {
            val excess = currentAmount - limit

            // DB operation - ASYNC (już jesteśmy w async)
            val added = depositManager.addToDeposit(player.uniqueId, material, excess)

            if (added) {
                // JEDEN sync call - usuń z inventory + wyślij wiadomość
                plugin.server.scheduler.runTask(plugin, Runnable {
                    // Usuń nadmiar z ekwipunku
                    var toRemove = excess
                    for (item in player.inventory.contents) {
                        if (item != null && item.type == material && toRemove > 0) {
                            val removeAmount = minOf(toRemove, item.amount)
                            item.amount -= removeAmount
                            toRemove -= removeAmount

                            if (item.amount <= 0) {
                                player.inventory.remove(item)
                            }
                        }
                    }

                    // Wiadomość w tym samym sync call
                    plugin.messageManager.sendMessage(player, "messages.moved-to-deposit",
                        "amount" to excess.toString(),
                        "item" to getMaterialDisplayName(material),
                        "limit" to limit.toString()
                    )
                })
            }
        }
    }

    /**
     * Sprawdza i przenosi nadmiar wszystkich itemów z limitami
     */
    private fun checkAndMoveExcess(player: Player) {
        val depositManager = plugin.depositManager

        for (material in depositManager.getLimitedMaterials()) {
            moveExcessToDeposit(player, material)
        }
    }

    /**
     * Pobiera nazwę wyświetlaną materiału
     */
    private fun getMaterialDisplayName(material: Material): String {
        // Sprawdź czy jest custom nazwa w configu
        val customName = plugin.config.getString("deposit.display-names.${material.name.lowercase()}")
        if (customName != null) {
            return customName
        }

        // Domyślna nazwa - sformatowana
        return material.name.lowercase()
            .split("_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
