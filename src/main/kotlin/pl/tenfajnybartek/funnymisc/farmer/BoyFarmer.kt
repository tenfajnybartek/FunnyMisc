package pl.tenfajnybartek.funnymisc.farmer

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID

/**
 * Reprezentuje aktywnego farmera obsydianu
 */
class BoyFarmer(
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
     * Startuje generowanie obsydianu
     */
    override fun start(plugin: FunnyPlugin) {
        start(plugin, null)
    }

    /**
     * Startuje generowanie obsydianu z callbackiem
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

            generateObsidian()
            currentDepth++
        }, intervalTicks, intervalTicks)
    }

    /**
     * Zatrzymuje generowanie
     */
    override fun stop() {
        task?.cancel()
        task = null
    }

    /**
     * Generuje blok obsydianu
     */
    private fun generateObsidian() {
        val targetLocation = location.clone().subtract(0.0, (currentDepth + 1).toDouble(), 0.0)
        val block = targetLocation.block

        // Zamień każdy blok na obsydian (oprócz bedrocku i barier)
        if (block.type != Material.BEDROCK && block.type != Material.BARRIER) {
            block.type = Material.OBSIDIAN
        }
    }

    /**
     * Sprawdza czy farmer jest nadal aktywny
     */
    override fun isActive(): Boolean = task != null && currentDepth < depth
}

