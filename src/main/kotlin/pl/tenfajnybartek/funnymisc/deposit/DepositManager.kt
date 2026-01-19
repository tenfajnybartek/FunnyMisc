package pl.tenfajnybartek.funnymisc.deposit

import org.bukkit.Material
import org.bukkit.entity.Player
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.database.DatabaseManager
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager zarządzający depozytami graczy
 */
class DepositManager(private val plugin: FunnyPlugin) {

    private val databaseManager: DatabaseManager = plugin.databaseManager

    // Cache dla limitów (z config.yml)
    private val itemLimits = ConcurrentHashMap<Material, Int>()

    /**
     * Ładuje limity itemów z configu
     */
    fun loadLimits() {
        itemLimits.clear()

        val limitsSection = plugin.config.getConfigurationSection("deposit.limits")
        if (limitsSection != null) {
            for (key in limitsSection.getKeys(false)) {
                try {
                    val material = Material.valueOf(key.uppercase())
                    val limit = limitsSection.getInt(key, 0)
                    if (limit > 0) {
                        itemLimits[material] = limit
                        plugin.logger.info("Załadowano limit dla $material: $limit")
                    }
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Nieprawidłowy materiał w limitach: $key")
                }
            }
        }

        plugin.logger.info("Załadowano ${itemLimits.size} limitów itemów")
    }

    /**
     * Pobiera limit dla danego materiału
     */
    fun getLimit(material: Material): Int {
        return itemLimits[material] ?: 0
    }

    /**
     * Sprawdza czy materiał ma limit
     */
    fun hasLimit(material: Material): Boolean {
        return itemLimits.containsKey(material)
    }

    /**
     * Pobiera wszystkie materiały z limitami
     */
    fun getLimitedMaterials(): Set<Material> {
        return itemLimits.keys
    }

    /**
     * Pobiera ilość itemów w depozycie gracza
     */
    fun getDepositAmount(playerUuid: UUID, material: Material): Int {
        databaseManager.getConnection()?.use { connection ->
            val query = "SELECT amount FROM player_deposits WHERE player_uuid = ? AND material = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, playerUuid.toString())
                statement.setString(2, material.name)

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt("amount")
                    }
                }
            }
        }
        return 0
    }

    /**
     * Dodaje itemy do depozytu gracza
     */
    fun addToDeposit(playerUuid: UUID, material: Material, amount: Int): Boolean {
        if (amount <= 0) return false

        try {
            databaseManager.getConnection()?.use { connection ->
                val currentAmount = getDepositAmount(playerUuid, material)
                val newAmount = currentAmount + amount

                val query = """
                    INSERT INTO player_deposits (player_uuid, material, amount) 
                    VALUES (?, ?, ?)
                    ON CONFLICT(player_uuid, material) DO UPDATE SET amount = ?
                """.trimIndent()

                // MySQL używa ON DUPLICATE KEY UPDATE
                val mysqlQuery = """
                    INSERT INTO player_deposits (player_uuid, material, amount) 
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE amount = ?
                """.trimIndent()

                val finalQuery = if (plugin.databaseManager.isConnected()) {
                    // Sprawdź typ bazy danych
                    val dbType = plugin.config.getString("database.type", "sqlite")?.uppercase()
                    if (dbType == "MYSQL") mysqlQuery else query
                } else {
                    query
                }

                connection.prepareStatement(finalQuery).use { statement ->
                    statement.setString(1, playerUuid.toString())
                    statement.setString(2, material.name)
                    statement.setInt(3, newAmount)
                    statement.setInt(4, newAmount)
                    statement.executeUpdate()
                }

                return true
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Błąd dodawania do depozytu: ${e.message}")
            e.printStackTrace()
        }

        return false
    }

    /**
     * Usuwa itemy z depozytu gracza
     */
    fun removeFromDeposit(playerUuid: UUID, material: Material, amount: Int): Boolean {
        if (amount <= 0) return false

        try {
            databaseManager.getConnection()?.use { connection ->
                val currentAmount = getDepositAmount(playerUuid, material)
                if (currentAmount < amount) {
                    return false // Nie ma wystarczającej ilości
                }

                val newAmount = currentAmount - amount

                if (newAmount <= 0) {
                    // Usuń wpis jeśli ilość = 0
                    val deleteQuery = "DELETE FROM player_deposits WHERE player_uuid = ? AND material = ?"
                    connection.prepareStatement(deleteQuery).use { statement ->
                        statement.setString(1, playerUuid.toString())
                        statement.setString(2, material.name)
                        statement.executeUpdate()
                    }
                } else {
                    // Zaktualizuj ilość
                    val updateQuery = "UPDATE player_deposits SET amount = ? WHERE player_uuid = ? AND material = ?"
                    connection.prepareStatement(updateQuery).use { statement ->
                        statement.setInt(1, newAmount)
                        statement.setString(2, playerUuid.toString())
                        statement.setString(3, material.name)
                        statement.executeUpdate()
                    }
                }

                return true
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Błąd usuwania z depozytu: ${e.message}")
            e.printStackTrace()
        }

        return false
    }

    /**
     * Pobiera wszystkie itemy w depozycie gracza
     */
    fun getAllDeposits(playerUuid: UUID): Map<Material, Int> {
        val deposits = mutableMapOf<Material, Int>()

        databaseManager.getConnection()?.use { connection ->
            val query = "SELECT material, amount FROM player_deposits WHERE player_uuid = ?"
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, playerUuid.toString())

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val materialName = resultSet.getString("material")
                        val amount = resultSet.getInt("amount")

                        try {
                            val material = Material.valueOf(materialName)
                            deposits[material] = amount
                        } catch (e: IllegalArgumentException) {
                            plugin.logger.warning("Nieprawidłowy materiał w bazie: $materialName")
                        }
                    }
                }
            }
        }

        return deposits
    }

    /**
     * Sprawdza czy gracz ma miejsce w ekwipunku na dany item
     */
    fun canAddToInventory(player: Player, material: Material, amount: Int): Boolean {
        val limit = getLimit(material)
        if (limit <= 0) return true // Brak limitu

        val currentAmount = player.inventory.all(material).values.sumOf { it.amount }
        return (currentAmount + amount) <= limit
    }

    /**
     * Sprawdza ile itemów można dodać do ekwipunku (uwzględniając limit)
     */
    fun getAvailableSpace(player: Player, material: Material): Int {
        val limit = getLimit(material)
        if (limit <= 0) return Int.MAX_VALUE // Brak limitu

        val currentAmount = player.inventory.all(material).values.sumOf { it.amount }
        return maxOf(0, limit - currentAmount)
    }

    /**
     * Czyści pusty depozyt gracza
     */
    fun clearEmptyDeposits(playerUuid: UUID) {
        try {
            databaseManager.getConnection()?.use { connection ->
                val query = "DELETE FROM player_deposits WHERE player_uuid = ? AND amount <= 0"
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, playerUuid.toString())
                    statement.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Błąd czyszczenia depozytu: ${e.message}")
        }
    }
}
