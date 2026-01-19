package pl.tenfajnybartek.funnymisc.megapickaxe

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.utils.ItemUtils

/**
 * Listener obsługujący funkcjonalność Mega Kilofa
 */
class MegaPickaxeListener(private val plugin: FunnyPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        // Sprawdź czy gracz używa Mega Kilofa
        if (!ItemUtils.isMegaPickaxe(item, plugin)) {
            return
        }

        // Sprawdź czy veinmining jest włączony
        if (!plugin.config.getBoolean("mega-kilof.veinmining.enabled", true)) {
            return
        }

        val centerBlock = event.block

        // Pobierz ustawienia veinminingu
        val radius = plugin.config.getInt("mega-kilof.veinmining.radius", 1) // domyślnie 1 = 3x3
        val breakSameBlockOnly = plugin.config.getBoolean("mega-kilof.veinmining.same-block-only", false)
        val respectToolType = plugin.config.getBoolean("mega-kilof.veinmining.respect-tool-type", true)

        // Znajdź bloki do zniszczenia
        val blocksToBreak = getBlocksToBreak(centerBlock, radius, breakSameBlockOnly, respectToolType)

        // Zniszcz wszystkie bloki
        for (block in blocksToBreak) {
            if (block.location == centerBlock.location) continue // Pomiń główny blok

            // Sprawdź czy blok może być zniszczony
            if (!canBreakBlock(block, player)) continue

            // Symuluj drop
            block.breakNaturally(item)

            // Dodaj efekt cząsteczek
            if (plugin.config.getBoolean("mega-kilof.veinmining.show-particles", true)) {
                block.world.spawnParticle(
                    org.bukkit.Particle.BLOCK,
                    block.location.add(0.5, 0.5, 0.5),
                    10,
                    0.3, 0.3, 0.3,
                    0.1,
                    block.blockData
                )
            }
        }

        // Wyślij wiadomość jeśli włączone
        if (blocksToBreak.size > 1 && plugin.config.getBoolean("mega-kilof.veinmining.show-message", true)) {
            plugin.messageManager.sendMessage(player, "mega-kilof.messages.veinmined",
                "count" to (blocksToBreak.size - 1).toString()
            )
        }
    }

    /**
     * Znajduje wszystkie bloki do zniszczenia w danym promieniu
     */
    private fun getBlocksToBreak(
        center: Block,
        radius: Int,
        sameBlockOnly: Boolean,
        respectToolType: Boolean
    ): List<Block> {
        val blocks = mutableListOf<Block>()
        val centerType = center.type

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val block = center.getRelative(x, y, z)

                    // Sprawdź czy to ten sam typ bloku (jeśli wymagane)
                    if (sameBlockOnly && block.type != centerType) {
                        continue
                    }

                    // Sprawdź czy można zniszczyć tym narzędziem
                    if (respectToolType && !canBeMinedWithPickaxe(block.type)) {
                        continue
                    }

                    // Nie niszczyj powietrza i niezniszczalnych bloków
                    if (block.type == Material.AIR ||
                        block.type == Material.BEDROCK ||
                        block.type == Material.BARRIER ||
                        block.type == Material.COMMAND_BLOCK ||
                        block.type == Material.CHAIN_COMMAND_BLOCK ||
                        block.type == Material.REPEATING_COMMAND_BLOCK) {
                        continue
                    }

                    blocks.add(block)
                }
            }
        }

        return blocks
    }

    /**
     * Sprawdza czy blok może być zniszczony kilofem
     */
    private fun canBeMinedWithPickaxe(material: Material): Boolean {
        // Lista materiałów które można kopać kilofem
        return when {
            material.name.contains("ORE") -> true
            material.name.contains("STONE") -> true
            material.name.contains("DEEPSLATE") -> true
            material.name.contains("CONCRETE") -> true
            material.name.contains("TERRACOTTA") -> true
            material == Material.OBSIDIAN -> true
            material == Material.NETHERITE_BLOCK -> true
            material == Material.ANCIENT_DEBRIS -> true
            material == Material.IRON_BLOCK -> true
            material == Material.GOLD_BLOCK -> true
            material == Material.DIAMOND_BLOCK -> true
            material == Material.EMERALD_BLOCK -> true
            material == Material.COAL_BLOCK -> true
            material == Material.REDSTONE_BLOCK -> true
            material == Material.LAPIS_BLOCK -> true
            material == Material.QUARTZ_BLOCK -> true
            material == Material.PRISMARINE -> true
            material == Material.PRISMARINE_BRICKS -> true
            material == Material.DARK_PRISMARINE -> true
            material == Material.NETHER_BRICKS -> true
            material == Material.RED_NETHER_BRICKS -> true
            material == Material.END_STONE -> true
            material == Material.END_STONE_BRICKS -> true
            material == Material.PURPUR_BLOCK -> true
            material == Material.PURPUR_PILLAR -> true
            material == Material.COBBLESTONE -> true
            material == Material.MOSSY_COBBLESTONE -> true
            material == Material.BRICKS -> true
            material == Material.SANDSTONE -> true
            material == Material.RED_SANDSTONE -> true
            material == Material.ANDESITE -> true
            material == Material.DIORITE -> true
            material == Material.GRANITE -> true
            else -> false
        }
    }

    /**
     * Sprawdza czy gracz może zniszczyć dany blok
     */
    private fun canBreakBlock(block: Block, player: Player): Boolean {
        // Sprawdź regiony (WorldGuard, GriefPrevention, itp.)
        // To wymaga integracji z pluginami ochronnymi
        // Na razie podstawowe sprawdzenie
        return true
    }
}
