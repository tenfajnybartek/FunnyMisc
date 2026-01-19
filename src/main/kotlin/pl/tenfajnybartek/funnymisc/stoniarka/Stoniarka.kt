package pl.tenfajnybartek.funnymisc.stoniarka

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID

/**
 * Klasa reprezentująca pojedynczą Stoniarkę
 *
 * Stoniarka to blok, który automatycznie regeneruje kamień nad sobą
 */
data class Stoniarka(
    val id: UUID,
    val location: Location,
    val ownerUuid: UUID,
    val regenerationInterval: Double // w sekundach
) {
    @Transient
    private var task: BukkitTask? = null

    @Transient
    private var active: Boolean = false

    /**
     * Uruchamia stoniarkę - rozpoczyna regenerację kamienia
     */
    fun start(plugin: FunnyPlugin) {
        if (active) return

        active = true
        val intervalTicks = (regenerationInterval * 20).toLong()

        // Sprawdź czy lokalizacja jest załadowana
        if (!location.isChunkLoaded) {
            location.chunk.load()
        }

        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            regenerateStone()
        }, intervalTicks, intervalTicks)
    }

    /**
     * Zatrzymuje stoniarkę
     */
    fun stop() {
        active = false
        task?.cancel()
        task = null
    }

    /**
     * Regeneruje kamień nad stoniarką
     */
    private fun regenerateStone() {
        val world = location.world ?: return

        // Sprawdź czy chunk jest załadowany
        if (!location.isChunkLoaded) {
            return
        }

        // Sprawdź czy stoniarka nadal istnieje
        val stoniarkaBlock = world.getBlockAt(location)
        if (stoniarkaBlock.type != Material.END_STONE) {
            stop()
            return
        }

        // Regeneruj kamień nad stoniarką
        val aboveLocation = location.clone().add(0.0, 1.0, 0.0)
        val aboveBlock = world.getBlockAt(aboveLocation)

        // Regeneruj tylko jeśli blok nad stoniarką jest powietrzem lub jest już kamieniem
        if (aboveBlock.type == Material.AIR || aboveBlock.type == Material.STONE) {
            aboveBlock.type = Material.STONE
        }
    }

    /**
     * Sprawdza czy stoniarka jest aktywna
     */
    fun isActive(): Boolean = active

    /**
     * Serializuje stoniarkę do mapy (do zapisu)
     */
    fun serialize(): Map<String, Any> {
        return mapOf(
            "id" to id.toString(),
            "world" to location.world!!.name,
            "x" to location.blockX,
            "y" to location.blockY,
            "z" to location.blockZ,
            "owner" to ownerUuid.toString(),
            "interval" to regenerationInterval
        )
    }

    companion object {
        /**
         * Deserializuje stoniarkę z mapy (z odczytu)
         */
        fun deserialize(map: Map<String, Any>, plugin: FunnyPlugin): Stoniarka? {
            try {
                val id = UUID.fromString(map["id"] as String)
                val worldName = map["world"] as String
                val world = plugin.server.getWorld(worldName) ?: return null
                val x = (map["x"] as Number).toInt()
                val y = (map["y"] as Number).toInt()
                val z = (map["z"] as Number).toInt()
                val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                val ownerUuid = UUID.fromString(map["owner"] as String)
                val interval = (map["interval"] as Number).toDouble()

                return Stoniarka(id, location, ownerUuid, interval)
            } catch (e: Exception) {
                plugin.logger.warning("Nie udało się załadować stoniarki: ${e.message}")
                return null
            }
        }
    }
}
