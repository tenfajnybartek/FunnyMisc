package pl.tenfajnybartek.funnymisc.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.io.File
import java.sql.Connection
import java.sql.SQLException

/**
 * Manager bazy danych obsługujący SQLite i MySQL z HikariCP
 */
class DatabaseManager(private val plugin: FunnyPlugin) {

    private var dataSource: HikariDataSource? = null
    private var databaseType: DatabaseType = DatabaseType.SQLITE

    enum class DatabaseType {
        SQLITE,
        MYSQL
    }

    /**
     * Inicjalizuje połączenie z bazą danych
     */
    fun initialize() {
        val type = plugin.config.getString("database.type", "sqlite")?.uppercase() ?: "SQLITE"
        databaseType = try {
            DatabaseType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("Nieprawidłowy typ bazy danych: $type, używam SQLite")
            DatabaseType.SQLITE
        }

        val config = HikariConfig()

        when (databaseType) {
            DatabaseType.SQLITE -> setupSQLite(config)
            DatabaseType.MYSQL -> setupMySQL(config)
        }

        try {
            dataSource = HikariDataSource(config)
            plugin.logger.info("Połączono z bazą danych: $databaseType")
            createTables()
        } catch (e: Exception) {
            plugin.logger.severe("Nie udało się połączyć z bazą danych: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Konfiguracja SQLite
     */
    private fun setupSQLite(config: HikariConfig) {
        val dbFile = File(plugin.dataFolder, "database.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
            dbFile.createNewFile()
        }

        config.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        config.driverClassName = "org.sqlite.JDBC"
        config.connectionTestQuery = "SELECT 1"

        // Zwiększony pool size - SQLite w WAL mode wspiera wiele połączeń read
        config.maximumPoolSize = 5
        config.minimumIdle = 2
        config.connectionTimeout = 30000  // 30s timeout
        config.idleTimeout = 600000       // 10min idle
        config.maxLifetime = 1800000      // 30min max lifetime

        // SQLite specific settings
        config.isAutoCommit = true
        config.poolName = "FunnyMisc-SQLite-Pool"
    }

    /**
     * Konfiguracja MySQL
     */
    private fun setupMySQL(config: HikariConfig) {
        val host = plugin.config.getString("database.mysql.host", "localhost")
        val port = plugin.config.getInt("database.mysql.port", 3306)
        val database = plugin.config.getString("database.mysql.database", "funnymisc")
        val username = plugin.config.getString("database.mysql.username", "root")
        val password = plugin.config.getString("database.mysql.password", "")
        val useSSL = plugin.config.getBoolean("database.mysql.use-ssl", false)

        // Konfigurowalny pool size (domyślnie 25 dla 200 graczy)
        // Dla 300+ graczy: ustaw pool-size: 35-40
        // Dla 500+ graczy: ustaw pool-size: 50
        val poolSize = plugin.config.getInt("database.mysql.pool-size", 25)
        val minIdle = plugin.config.getInt("database.mysql.min-idle", 5)

        config.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=$useSSL&allowPublicKeyRetrieval=true&useServerPrepStmts=true&rewriteBatchedStatements=true&cachePrepStmts=true"
        config.driverClassName = "com.mysql.cj.jdbc.Driver"
        config.username = username
        config.password = password
        config.connectionTestQuery = "SELECT 1"

        // Pool configuration (skalowalna dla różnych wielkości serwerów)
        config.maximumPoolSize = poolSize
        config.minimumIdle = minIdle
        config.maxLifetime = 1800000       // 30 min
        config.connectionTimeout = 30000   // 30s timeout
        config.idleTimeout = 600000        // 10 min idle
        config.keepaliveTime = 300000      // 5 min keepalive
        config.leakDetectionThreshold = 60000  // Wykrywanie leaków po 60s

        // MySQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "500")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096")
        config.addDataSourceProperty("useServerPrepStmts", "true")
        config.addDataSourceProperty("useLocalSessionState", "true")
        config.addDataSourceProperty("rewriteBatchedStatements", "true")
        config.addDataSourceProperty("cacheResultSetMetadata", "true")
        config.addDataSourceProperty("cacheServerConfiguration", "true")
        config.addDataSourceProperty("elideSetAutoCommits", "true")
        config.addDataSourceProperty("maintainTimeStats", "false")

        config.poolName = "FunnyMisc-MySQL-Pool"

        plugin.logger.info("MySQL pool configured: size=$poolSize, minIdle=$minIdle")
    }

    /**
     * Tworzy tabele w bazie danych
     */
    private fun createTables() {
        getConnection()?.use { connection ->
            // Włącz WAL mode dla SQLite (lepsze współbieżność)
            if (databaseType == DatabaseType.SQLITE) {
                connection.createStatement().use { stmt ->
                    stmt.execute("PRAGMA journal_mode=WAL")
                    stmt.execute("PRAGMA synchronous=NORMAL")
                    stmt.execute("PRAGMA cache_size=10000")
                    stmt.execute("PRAGMA temp_store=MEMORY")
                    plugin.logger.info("SQLite WAL mode włączony dla lepszej wydajności")
                }
            }

            // Tabela dla depozytów graczy
            val createDepositTable = when (databaseType) {
                DatabaseType.SQLITE -> """
                    CREATE TABLE IF NOT EXISTS player_deposits (
                        player_uuid TEXT NOT NULL,
                        material TEXT NOT NULL,
                        amount INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, material)
                    )
                """.trimIndent()

                DatabaseType.MYSQL -> """
                    CREATE TABLE IF NOT EXISTS player_deposits (
                        player_uuid VARCHAR(36) NOT NULL,
                        material VARCHAR(50) NOT NULL,
                        amount INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, material)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.trimIndent()
            }

            // Tabela dla backupów inwentarzy
            val createBackupsTable = when (databaseType) {
                DatabaseType.SQLITE -> """
                    CREATE TABLE IF NOT EXISTS player_backups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        reason INTEGER NOT NULL,
                        inventory TEXT NOT NULL,
                        armor TEXT NOT NULL,
                        enderchest TEXT,
                        metadata TEXT,
                        is_restored INTEGER DEFAULT 0,
                        restored_by TEXT,
                        restored_at INTEGER
                    )
                """.trimIndent()

                DatabaseType.MYSQL -> """
                    CREATE TABLE IF NOT EXISTS player_backups (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        timestamp BIGINT NOT NULL,
                        reason TINYINT NOT NULL,
                        inventory MEDIUMTEXT NOT NULL,
                        armor MEDIUMTEXT NOT NULL,
                        enderchest MEDIUMTEXT,
                        metadata TEXT,
                        is_restored BOOLEAN DEFAULT FALSE,
                        restored_by VARCHAR(36),
                        restored_at BIGINT,
                        INDEX idx_uuid (uuid),
                        INDEX idx_timestamp (timestamp),
                        INDEX idx_restored (is_restored)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.trimIndent()
            }

            // Tabela dla aktywności graczy
            val createActivityTable = when (databaseType) {
                DatabaseType.SQLITE -> """
                    CREATE TABLE IF NOT EXISTS player_activity (
                        uuid TEXT PRIMARY KEY,
                        player_name TEXT NOT NULL,
                        last_seen INTEGER NOT NULL,
                        first_seen INTEGER NOT NULL,
                        total_backups INTEGER DEFAULT 0
                    )
                """.trimIndent()

                DatabaseType.MYSQL -> """
                    CREATE TABLE IF NOT EXISTS player_activity (
                        uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL,
                        last_seen BIGINT NOT NULL,
                        first_seen BIGINT NOT NULL,
                        total_backups INT DEFAULT 0,
                        INDEX idx_last_seen (last_seen)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.trimIndent()
            }

            connection.createStatement().use { statement ->
                statement.execute(createDepositTable)
                statement.execute(createBackupsTable)
                statement.execute(createActivityTable)
            }

            // Tworzenie indeksów dla SQLite (po utworzeniu tabel)
            if (databaseType == DatabaseType.SQLITE) {
                connection.createStatement().use { statement ->
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_backups_uuid ON player_backups(uuid)")
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_backups_timestamp ON player_backups(timestamp DESC)")
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_backups_restored ON player_backups(is_restored)")
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_activity_last_seen ON player_activity(last_seen)")
                }
            }

            plugin.logger.info("Tabele bazy danych zostały utworzone")
        }
    }

    /**
     * Pobiera połączenie z bazy danych
     */
    fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            plugin.logger.severe("Błąd pobierania połączenia: ${e.message}")
            null
        }
    }

    /**
     * Zamyka połączenie z bazą danych
     */
    fun close() {
        dataSource?.close()
        plugin.logger.info("Połączenie z bazą danych zostało zamknięte")
    }

    /**
     * Sprawdza czy baza danych jest połączona
     */
    fun isConnected(): Boolean {
        return dataSource?.isRunning == true
    }
}
