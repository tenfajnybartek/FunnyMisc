package pl.tenfajnybartek.funnymisc.listeners

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.utils.ItemUtils

/**
 * Listener obsługujący stawianie i niszczenie stoniarek
 */
class StoniarkaListener(private val plugin: FunnyPlugin) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val item = event.itemInHand

        // Sprawdź czy to stoniarka
        if (!ItemUtils.isStoniarka(item, plugin)) {
            return
        }

        val location = event.blockPlaced.location

        // Sprawdź czy już jest stoniarka w tej lokalizacji
        if (plugin.stoniarkaManager.hasStoniarkaAt(location)) {
            plugin.messageManager.sendMessage(player, "messages.cannot-place-here")
            event.isCancelled = true
            return
        }

        // Wyślij wiadomość
        plugin.messageManager.sendMessage(player, "stoniarka.messages.stoniarka-placed")

        // Stwórz stoniarkę
        plugin.stoniarkaManager.createStoniarka(location, player.uniqueId)
    }

    /**
     * Obsługuje zbieranie stoniarek przez PPM z Collector tool lub standardowym narzędziem
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Tylko RIGHT_CLICK na bloku
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        val location = block.location
        val player = event.player
        val item = player.inventory.itemInMainHand

        // Sprawdź czy kliknięty blok to stoniarka
        if (block.type != Material.END_STONE) {
            return
        }

        if (!plugin.stoniarkaManager.hasStoniarkaAt(location)) {
            return
        }

        // Sprawdź czy gracz używa Collector tool
        val isCollectorTool = ItemUtils.isCollector(item, plugin)

        if (isCollectorTool) {
            // Anuluj standardową interakcję
            event.isCancelled = true

            // Użyj specjalnego narzędzia do zebrania stoniarki
            handleCollectorUse(player, item, location, block)
            return
        }

        // Standardowe sprawdzanie - stary system z Silk Touch
        val requiredLevel = plugin.config.getInt("stoniarka.required-silk-touch-level", 10)
        val requirePickaxeOrShovel = plugin.config.getBoolean("stoniarka.require-pickaxe-or-shovel", true)

        // Sprawdź czy narzędzie ma wymagany Silk Touch
        val silkTouchLevel = item.getEnchantmentLevel(Enchantment.SILK_TOUCH)

        // Sprawdź czy to odpowiednie narzędzie
        val isCorrectTool = if (requirePickaxeOrShovel) {
            isPickaxeOrShovel(item.type)
        } else {
            true
        }

        if (isCorrectTool && silkTouchLevel >= requiredLevel) {
            // Anuluj standardową interakcję
            event.isCancelled = true

            // Można zebrać - usuń stoniarkę z managera
            val stoniarka = plugin.stoniarkaManager.getStoniarkaAt(location)
            if (stoniarka != null) {
                plugin.stoniarkaManager.removeStoniarka(stoniarka.id)

                // Usuń blok
                block.type = Material.AIR

                // Daj graczowi item stoniarki
                val stoniarkaItem = ItemUtils.createStoniarkaItem(plugin)
                block.world.dropItemNaturally(location, stoniarkaItem)

                plugin.messageManager.sendMessage(player, "stoniarka.messages.stoniarka-retrieved")
            }
        }
    }

    /**
     * Blokuje niszczenie stoniarek poprzez LPM
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val location = block.location

        // Sprawdź czy to stoniarka
        if (block.type != Material.END_STONE) {
            return
        }

        if (!plugin.stoniarkaManager.hasStoniarkaAt(location)) {
            return
        }

        // Zablokuj niszczenie stoniarki przez LPM
        // Stoniarki można zbierać tylko przez PPM
        plugin.messageManager.sendMessage(event.player, "stoniarka.messages.cannot-break")
        event.isCancelled = true
    }

    /**
     * Blokuje używanie Collector tool do kopania/niszczenia bloków
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCollectorBreakBlock(event: BlockBreakEvent) {
        val item = event.player.inventory.itemInMainHand

        // Sprawdź czy gracz używa Collector tool
        if (ItemUtils.isCollector(item, plugin)) {
            // Sprawdź czy to stoniarka
            val block = event.block
            if (block.type == Material.END_STONE && plugin.stoniarkaManager.hasStoniarkaAt(block.location)) {
                // To jest stoniarka - zablokuj (obsługiwane w onPlayerInteract)
                event.isCancelled = true
                plugin.messageManager.sendMessage(event.player, "stoniarka-collector.messages.use-right-click")
            } else {
                // To nie jest stoniarka - zablokuj używanie do kopania
                event.isCancelled = true
                plugin.messageManager.sendMessage(event.player, "stoniarka-collector.messages.only-for-stoniarki")
            }
        }
    }

    /**
     * Blokuje używanie Collector tool do atakowania
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCollectorAttack(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player) return

        val player = event.damager as Player
        val item = player.inventory.itemInMainHand

        // Sprawdź czy gracz używa Collector tool
        if (ItemUtils.isCollector(item, plugin)) {
            // Zablokuj atakowanie Collector tool
            event.isCancelled = true
            plugin.messageManager.sendMessage(player, "stoniarka-collector.messages.only-for-stoniarki")
        }
    }

    /**
     * Obsługuje użycie Collector tool do zebrania stoniarki (PPM)
     */
    private fun handleCollectorUse(player: Player, item: ItemStack, location: Location, block: org.bukkit.block.Block) {
        val remainingUses = ItemUtils.getCollectorUses(item, plugin)
        val maxUses = plugin.config.getInt("stoniarka-collector.max-uses", 5)

        // Jeśli max-uses = -1, narzędzie ma nieskończone użycia
        if (maxUses == -1) {
            // Nieskończone użycia - po prostu zbierz stoniarkę
            collectStoniarka(player, location, block)
            return
        }

        if (remainingUses <= 0) {
            // Narzędzie zużyte
            plugin.messageManager.sendMessage(player, "stoniarka-collector.messages.tool-broken")

            if (plugin.config.getBoolean("stoniarka-collector.break-on-zero-uses", true)) {
                // Zniszcz narzędzie
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            }
            return
        }

        // Zmniejsz użycia
        val newUses = remainingUses - 1
        ItemUtils.setCollectorUses(item, newUses, plugin)

        // Wyślij wiadomość o pozostałych użyciach
        plugin.messageManager.sendMessage(player, "stoniarka-collector.messages.uses-remaining",
            "uses" to newUses.toString(),
            "max" to maxUses.toString()
        )

        // Zbierz stoniarkę
        collectStoniarka(player, location, block)

        // Jeśli to było ostatnie użycie i break-on-zero-uses = true
        if (newUses <= 0 && plugin.config.getBoolean("stoniarka-collector.break-on-zero-uses", true)) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                plugin.messageManager.sendMessage(player, "stoniarka-collector.messages.tool-broken")
            })
        }
    }

    /**
     * Zbiera stoniarkę z lokalizacji
     */
    private fun collectStoniarka(player: Player, location: Location, block: org.bukkit.block.Block) {
        // Usuń stoniarkę z managera
        val stoniarka = plugin.stoniarkaManager.getStoniarkaAt(location)
        if (stoniarka != null) {
            plugin.stoniarkaManager.removeStoniarka(stoniarka.id)

            // Usuń blok
            block.type = Material.AIR

            // Daj graczowi item stoniarki
            val stoniarkaItem = ItemUtils.createStoniarkaItem(plugin)
            block.world.dropItemNaturally(location, stoniarkaItem)

            plugin.messageManager.sendMessage(player, "stoniarka.messages.stoniarka-retrieved")
        }
    }

    /**
     * Sprawdza czy materiał to kilof lub łopata
     */
    private fun isPickaxeOrShovel(material: Material): Boolean {
        return when (material) {
            // Kilofy
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE,
            // Łopaty
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL -> true
            else -> false
        }
    }
}
