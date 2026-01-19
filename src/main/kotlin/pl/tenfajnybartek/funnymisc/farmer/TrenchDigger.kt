package pl.tenfajnybartek.funnymisc.farmer

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID

/**
 * Reprezentuje aktywnego kopacza fosy (usuwa bloki)
 */
class TrenchDigger(
    override val id: UUID,
    override val location: Location,
    override val ownerUuid: UUID,
    val depth: Int,
    val generationInterval: Double,
    var currentDepth: Int = 0,
    var task: BukkitTask? = null,
    var onComplete: ((UUID) -> Unit)? = null
) : Farmer {

    /**
     * Startuje usuwanie bloków
     */
    override fun start(plugin: FunnyPlugin) {
        start(plugin, null)
    }

    /**
     * Startuje usuwanie bloków z callbackiem
     */
    fun start(plugin: FunnyPlugin, onComplete: ((UUID) -> Unit)?) {
        if (task != null) return
        this.onComplete = onComplete

        val intervalTicks = (generationInterval * 20).toLong() // Konwersja sekund na ticki

        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (currentDepth >= depth) {
                stop()
                this.onComplete?.invoke(id)
                return@Runnable
            }

            removeBlock()
            currentDepth++
        }, intervalTicks, intervalTicks)
    }

    /**
     * Zatrzymuje usuwanie
     */
    override fun stop() {
        task?.cancel()
        task = null
    }

    /**
     * Usuwa blok
     */
    private fun removeBlock() {
        val targetLocation = location.clone().subtract(0.0, (currentDepth + 1).toDouble(), 0.0)
        val block = targetLocation.block

        // Usuń blok (oprócz bedrocku i barier)
        if (block.type != Material.BEDROCK && block.type != Material.BARRIER && block.type != Material.AIR) {
            block.type = Material.AIR
        }
    }

    /**
     * Sprawdza czy digger jest nadal aktywny
     */
    override fun isActive(): Boolean = task != null && currentDepth < depth
}
