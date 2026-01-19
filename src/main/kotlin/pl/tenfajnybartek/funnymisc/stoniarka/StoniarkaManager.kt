package pl.tenfajnybartek.funnymisc.stoniarka

import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager zarządzający wszystkimi stoniarkami
 * Obsługuje tworzenie, zapisywanie i ładowanie stoniarek
 */
class StoniarkaManager(private val plugin: FunnyPlugin) {

    private val stoniarki = ConcurrentHashMap<UUID, Stoniarka>()
    private val storageFile = File(plugin.dataFolder, "stoniarki.yml")

    init {
        // Załaduj stoniarki przy starcie
        loadStoniarki()
    }

    /**
     * Tworzy i uruchamia nową stoniarkę
     */
    fun createStoniarka(location: Location, ownerUuid: UUID): Stoniarka {
        val interval = plugin.config.getDouble("stoniarka.regeneration-interval", 1.0)

        val stoniarka = Stoniarka(
            id = UUID.randomUUID(),
            location = location,
            ownerUuid = ownerUuid,
            regenerationInterval = interval
        )

        stoniarki[stoniarka.id] = stoniarka
        stoniarka.start(plugin)

        // Zapisz do pliku
        saveStoniarki()

        return stoniarka
    }

    /**
     * Usuwa stoniarkę
     */
    fun removeStoniarka(id: UUID) {
        stoniarki[id]?.let { stoniarka ->
            stoniarka.stop()
            stoniarki.remove(id)
            saveStoniarki()
        }
    }

    /**
     * Sprawdza czy w danej lokalizacji jest stoniarka
     */
    fun hasStoniarkaAt(location: Location): Boolean {
        return stoniarki.values.any {
            it.location.world?.uid == location.world?.uid &&
                    it.location.blockX == location.blockX &&
                    it.location.blockY == location.blockY &&
                    it.location.blockZ == location.blockZ
        }
    }

    /**
     * Pobiera stoniarkę w danej lokalizacji
     */
    fun getStoniarkaAt(location: Location): Stoniarka? {
        return stoniarki.values.firstOrNull {
            it.location.world?.uid == location.world?.uid &&
                    it.location.blockX == location.blockX &&
                    it.location.blockY == location.blockY &&
                    it.location.blockZ == location.blockZ
        }
    }

    /**
     * Zatrzymuje wszystkie stoniarki
     */
    fun stopAll() {
        stoniarki.values.forEach { it.stop() }
    }

    /**
     * Uruchamia wszystkie stoniarki
     */
    fun startAll() {
        stoniarki.values.forEach { it.start(plugin) }
    }

    /**
     * Pobiera wszystkie stoniarki
     */
    fun getAllStoniarki(): Collection<Stoniarka> = stoniarki.values

    /**
     * Zapisuje stoniarki do pliku
     */
    fun saveStoniarki() {
        val config = YamlConfiguration()

        val stoniarkaList = stoniarki.values.map { it.serialize() }
        config.set("stoniarki", stoniarkaList)

        try {
            if (!storageFile.parentFile.exists()) {
                storageFile.parentFile.mkdirs()
            }
            config.save(storageFile)
        } catch (e: Exception) {
            plugin.logger.severe("Nie udało się zapisać stoniarek: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Ładuje stoniarki z pliku
     */
    private fun loadStoniarki() {
        if (!storageFile.exists()) {
            return
        }

        try {
            val config = YamlConfiguration.loadConfiguration(storageFile)
            val stoniarkaList = config.getMapList("stoniarki")

            stoniarkaList.forEach { map ->
                @Suppress("UNCHECKED_CAST")
                val stringMap = map as? Map<String, Any>
                if (stringMap != null) {
                    val stoniarka = Stoniarka.deserialize(stringMap, plugin)
                    if (stoniarka != null) {
                        stoniarki[stoniarka.id] = stoniarka
                        stoniarka.start(plugin)
                    }
                }
            }

            plugin.logger.info("Załadowano ${stoniarki.size} stoniarek")
        } catch (e: Exception) {
            plugin.logger.severe("Nie udało się załadować stoniarek: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Czyści nieaktywne stoniarki
     */
    fun cleanupInactive() {
        val toRemove = stoniarki.filter { !it.value.isActive() }.keys
        toRemove.forEach {
            stoniarki.remove(it)
        }

        if (toRemove.isNotEmpty()) {
            saveStoniarki()
        }
    }
}
