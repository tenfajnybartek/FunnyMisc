package pl.tenfajnybartek.funnymisc.backup

import org.bukkit.Location
import org.bukkit.GameMode
import org.bukkit.potion.PotionEffect
import java.util.UUID

/**
 * Model reprezentujący backup inwentarza gracza
 */
data class PlayerBackup(
    val id: Int,
    val uuid: UUID,
    val playerName: String,
    val timestamp: Long,
    val reason: BackupReason,
    val inventoryData: String,
    val armorData: String,
    val enderchestData: String?,
    val metadata: BackupMetadata?,
    val isRestored: Boolean = false,
    val restoredBy: UUID? = null,
    val restoredAt: Long? = null
)

/**
 * Powody tworzenia backupu
 */
enum class BackupReason(val id: Int, val displayName: String) {
    DEATH(0, "Śmierć"),
    LOGOUT(1, "Wylogowanie"),
    LAG(2, "Lag serwera"),
    MANUAL(3, "Ręczny"),
    PLUGIN_ERROR(4, "Błąd pluginu"),
    LOGOUT_PUNISHMENT(5, "Kara za wylogowanie");

    companion object {
        fun fromId(id: Int): BackupReason {
            return entries.firstOrNull { it.id == id } ?: MANUAL
        }
    }
}

/**
 * Metadata backupu z dodatkowymi informacjami
 */
data class BackupMetadata(
    val location: LocationData,
    val xp: XPData,
    val health: Double,
    val food: Int,
    val saturation: Float,
    val gamemode: GameMode,
    val effects: List<PotionEffectData>
)

/**
 * Dane lokalizacji
 */
data class LocationData(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    companion object {
        fun from(location: Location): LocationData {
            return LocationData(
                world = location.world?.name ?: "world",
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            )
        }
    }

    fun format(): String {
        return "${world} (${x.toInt()}, ${y.toInt()}, ${z.toInt()})"
    }
}

/**
 * Dane XP gracza
 */
data class XPData(
    val level: Int,
    val exp: Float
)

/**
 * Dane efektu
 */
data class PotionEffectData(
    val type: String,
    val duration: Int,
    val amplifier: Int,
    val ambient: Boolean,
    val particles: Boolean,
    val icon: Boolean
) {
    companion object {
        fun from(effect: PotionEffect): PotionEffectData {
            return PotionEffectData(
                type = effect.type.key.key,
                duration = effect.duration,
                amplifier = effect.amplifier,
                ambient = effect.isAmbient,
                particles = effect.hasParticles(),
                icon = effect.hasIcon()
            )
        }
    }
}
