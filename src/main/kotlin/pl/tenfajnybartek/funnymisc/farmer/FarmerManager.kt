package pl.tenfajnybartek.funnymisc.farmer

import org.bukkit.Location
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Interfejs dla wszystkich typów farmerów
 */
interface Farmer {
    val id: UUID
    val location: Location
    val ownerUuid: UUID
    fun start(plugin: FunnyPlugin)
    fun stop()
    fun isActive(): Boolean
}

/**
 * Manager zarządzający wszystkimi aktywnymi farmerami
 */
class FarmerManager(private val plugin: FunnyPlugin) {

    private val activeFarmers = ConcurrentHashMap<UUID, Farmer>()

    /**
     * Tworzy i uruchamia nowego BoyFarmera
     */
    fun createBoyFarmer(location: Location, ownerUuid: UUID): BoyFarmer {
        val depth = plugin.config.getInt("boyfarmer.depth", 5)
        val interval = plugin.config.getDouble("boyfarmer.generation-interval", 1.0)

        val farmer = BoyFarmer(
            id = UUID.randomUUID(),
            location = location,
            ownerUuid = ownerUuid,
            depth = depth,
            generationInterval = interval
        )

        activeFarmers[farmer.id] = farmer
        farmer.start(plugin) { farmerId ->
            onFarmerComplete(farmerId, "boyfarmer")
        }

        return farmer
    }

    /**
     * Tworzy i uruchamia nowego SandFarmera
     */
    fun createSandFarmer(location: Location, ownerUuid: UUID): SandFarmer {
        val depth = plugin.config.getInt("sandfarmer.depth", 5)
        val interval = plugin.config.getDouble("sandfarmer.generation-interval", 1.0)

        val farmer = SandFarmer(
            id = UUID.randomUUID(),
            location = location,
            ownerUuid = ownerUuid,
            depth = depth,
            generationInterval = interval
        )

        activeFarmers[farmer.id] = farmer
        farmer.start(plugin) { farmerId ->
            onFarmerComplete(farmerId, "sandfarmer")
        }

        return farmer
    }

    /**
     * Tworzy i uruchamia nowego TrenchDiggera
     */
    fun createTrenchDigger(location: Location, ownerUuid: UUID): TrenchDigger {
        val depth = plugin.config.getInt("trenchdigger.depth", 5)
        val interval = plugin.config.getDouble("trenchdigger.generation-interval", 1.0)

        val digger = TrenchDigger(
            id = UUID.randomUUID(),
            location = location,
            ownerUuid = ownerUuid,
            depth = depth,
            generationInterval = interval
        )

        activeFarmers[digger.id] = digger
        digger.start(plugin) { farmerId ->
            onFarmerComplete(farmerId, "trenchdigger")
        }

        return digger
    }

    /**
     * Usuwa farmera
     */
    fun removeFarmer(id: UUID) {
        activeFarmers[id]?.let { farmer ->
            farmer.stop()
            activeFarmers.remove(id)
        }
    }

    /**
     * Sprawdza czy w danej lokalizacji jest już farmer
     */
    fun hasFarmerAt(location: Location): Boolean {
        return activeFarmers.values.any {
            it.location.world?.uid == location.world?.uid &&
            it.location.blockX == location.blockX &&
            it.location.blockY == location.blockY &&
            it.location.blockZ == location.blockZ
        }
    }

    /**
     * Pobiera farmera w danej lokalizacji
     */
    fun getFarmerAt(location: Location): Farmer? {
        return activeFarmers.values.firstOrNull {
            it.location.world?.uid == location.world?.uid &&
            it.location.blockX == location.blockX &&
            it.location.blockY == location.blockY &&
            it.location.blockZ == location.blockZ
        }
    }

    /**
     * Zatrzymuje wszystkich farmerów (np. przy wyłączeniu pluginu)
     */
    fun stopAll() {
        activeFarmers.values.forEach { it.stop() }
        activeFarmers.clear()
    }

    /**
     * Czyści nieaktywnych farmerów
     */
    fun cleanupInactive() {
        val toRemove = activeFarmers.filter { !it.value.isActive() }.keys
        toRemove.forEach { activeFarmers.remove(it) }
    }

    /**
     * Pobiera wszystkich aktywnych farmerów
     */
    fun getActiveFarmers(): Collection<Farmer> = activeFarmers.values

    /**
     * Callback wywoływany gdy farmer kończy pracę
     */
    private fun onFarmerComplete(farmerId: UUID, farmerType: String) {
        val farmer = activeFarmers[farmerId] ?: return
        val player = plugin.server.getPlayer(farmer.ownerUuid)

        if (player != null && player.isOnline) {
            plugin.messageManager.sendMessage(player, "$farmerType.messages.farmer-complete")
        }

        // Usuń farmera z listy
        removeFarmer(farmerId)
    }
}

