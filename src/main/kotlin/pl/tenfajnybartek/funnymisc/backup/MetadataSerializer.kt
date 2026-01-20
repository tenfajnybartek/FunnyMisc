package pl.tenfajnybartek.funnymisc.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

/**
 * Utility do serializacji i deserializacji metadata backupów
 */
object MetadataSerializer {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Tworzy metadata z gracza
     *
     * @param player Gracz
     * @return BackupMetadata
     */
    fun createFromPlayer(player: Player): BackupMetadata {
        return BackupMetadata(
            location = LocationData.from(player.location),
            xp = XPData(
                level = player.level,
                exp = player.exp
            ),
            health = player.health,
            food = player.foodLevel,
            saturation = player.saturation,
            gamemode = player.gameMode,
            effects = player.activePotionEffects.map { PotionEffectData.from(it) }
        )
    }

    /**
     * Serializuje metadata do JSON string
     *
     * @param metadata Metadata do serializacji
     * @return JSON string
     */
    fun serialize(metadata: BackupMetadata): String {
        val json = JsonObject()

        // Location
        val locationJson = JsonObject()
        locationJson.addProperty("world", metadata.location.world)
        locationJson.addProperty("x", metadata.location.x)
        locationJson.addProperty("y", metadata.location.y)
        locationJson.addProperty("z", metadata.location.z)
        locationJson.addProperty("yaw", metadata.location.yaw)
        locationJson.addProperty("pitch", metadata.location.pitch)
        json.add("location", locationJson)

        // XP
        val xpJson = JsonObject()
        xpJson.addProperty("level", metadata.xp.level)
        xpJson.addProperty("exp", metadata.xp.exp)
        json.add("xp", xpJson)

        // Stats
        json.addProperty("health", metadata.health)
        json.addProperty("food", metadata.food)
        json.addProperty("saturation", metadata.saturation)
        json.addProperty("gamemode", metadata.gamemode.name)

        // Effects
        val effectsArray = com.google.gson.JsonArray()
        metadata.effects.forEach { effect ->
            val effectJson = JsonObject()
            effectJson.addProperty("type", effect.type)
            effectJson.addProperty("duration", effect.duration)
            effectJson.addProperty("amplifier", effect.amplifier)
            effectJson.addProperty("ambient", effect.ambient)
            effectJson.addProperty("particles", effect.particles)
            effectJson.addProperty("icon", effect.icon)
            effectsArray.add(effectJson)
        }
        json.add("effects", effectsArray)

        return gson.toJson(json)
    }

    /**
     * Deserializuje JSON string do metadata
     *
     * @param data JSON string
     * @return BackupMetadata lub null jeśli błąd
     */
    fun deserialize(data: String?): BackupMetadata? {
        if (data.isNullOrEmpty()) return null

        return try {
            val json = gson.fromJson(data, JsonObject::class.java)

            // Location
            val locationJson = json.getAsJsonObject("location")
            val location = LocationData(
                world = locationJson.get("world").asString,
                x = locationJson.get("x").asDouble,
                y = locationJson.get("y").asDouble,
                z = locationJson.get("z").asDouble,
                yaw = locationJson.get("yaw").asFloat,
                pitch = locationJson.get("pitch").asFloat
            )

            // XP
            val xpJson = json.getAsJsonObject("xp")
            val xp = XPData(
                level = xpJson.get("level").asInt,
                exp = xpJson.get("exp").asFloat
            )

            // Stats
            val health = json.get("health").asDouble
            val food = json.get("food").asInt
            val saturation = json.get("saturation").asFloat
            val gamemode = GameMode.valueOf(json.get("gamemode").asString)

            // Effects
            val effectsArray = json.getAsJsonArray("effects")
            val effects = effectsArray.map { effectElement ->
                val effectJson = effectElement.asJsonObject
                PotionEffectData(
                    type = effectJson.get("type").asString,
                    duration = effectJson.get("duration").asInt,
                    amplifier = effectJson.get("amplifier").asInt,
                    ambient = effectJson.get("ambient").asBoolean,
                    particles = effectJson.get("particles").asBoolean,
                    icon = effectJson.get("icon").asBoolean
                )
            }

            BackupMetadata(
                location = location,
                xp = xp,
                health = health,
                food = food,
                saturation = saturation,
                gamemode = gamemode,
                effects = effects
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Przywraca metadata do gracza (bez lokalizacji)
     *
     * @param player Gracz
     * @param metadata Metadata do przywrócenia
     */
    fun restoreToPlayer(player: Player, metadata: BackupMetadata) {
        // XP
        player.level = metadata.xp.level
        player.exp = metadata.xp.exp

        // Stats
        // WAŻNE: Nie przywracaj health jeśli było 0 (gracz zmarł) - daj mu pełne HP
        val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        val restoredHealth = if (metadata.health <= 0.0) maxHealth else metadata.health
        player.health = restoredHealth.coerceIn(1.0, maxHealth)

        player.foodLevel = metadata.food.coerceIn(0, 20)
        player.saturation = metadata.saturation.coerceIn(0f, 20f)

        // Gamemode
        player.gameMode = metadata.gamemode

        // Effects - usuń stare i dodaj nowe
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        metadata.effects.forEach { effectData ->
            try {
                val effectType = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(effectData.type))
                if (effectType != null) {
                    val effect = org.bukkit.potion.PotionEffect(
                        effectType,
                        effectData.duration,
                        effectData.amplifier,
                        effectData.ambient,
                        effectData.particles,
                        effectData.icon
                    )
                    player.addPotionEffect(effect)
                }
            } catch (_: Exception) {
                // Ignoruj błędne efekty
            }
        }
    }
}
