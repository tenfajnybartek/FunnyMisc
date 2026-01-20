package pl.tenfajnybartek.funnymisc.base

import org.bukkit.plugin.java.JavaPlugin
import pl.tenfajnybartek.funnymisc.backup.BackupCommand
import pl.tenfajnybartek.funnymisc.backup.BackupGUI
import pl.tenfajnybartek.funnymisc.backup.BackupListener
import pl.tenfajnybartek.funnymisc.backup.BackupManager
import pl.tenfajnybartek.funnymisc.commands.DepositCommand
import pl.tenfajnybartek.funnymisc.commands.FunnyMiscCommand
import pl.tenfajnybartek.funnymisc.database.DatabaseManager
import pl.tenfajnybartek.funnymisc.deposit.DepositGUI
import pl.tenfajnybartek.funnymisc.deposit.DepositListener
import pl.tenfajnybartek.funnymisc.deposit.DepositManager
import pl.tenfajnybartek.funnymisc.farmer.FarmerManager
import pl.tenfajnybartek.funnymisc.listeners.FarmerCraftingListener
import pl.tenfajnybartek.funnymisc.listeners.FarmerPlaceListener
import pl.tenfajnybartek.funnymisc.listeners.StoniarkaListener
import pl.tenfajnybartek.funnymisc.megapickaxe.MegaPickaxeListener
import pl.tenfajnybartek.funnymisc.stoniarka.StoniarkaManager
import pl.tenfajnybartek.funnymisc.utils.MessageManager

class FunnyPlugin : JavaPlugin() {

    lateinit var farmerManager: FarmerManager
        private set

    lateinit var messageManager: MessageManager
        private set

    lateinit var stoniarkaManager: StoniarkaManager
        private set

    lateinit var databaseManager: DatabaseManager
        private set

    lateinit var depositManager: DepositManager
        private set

    lateinit var depositGUI: DepositGUI
        private set

    lateinit var backupManager: BackupManager
        private set

    private lateinit var craftingListener: FarmerCraftingListener

    override fun onEnable() {
        // Zapisz domyślną konfigurację
        saveDefaultConfig()

        // Inicjalizuj managery
        messageManager = MessageManager(this)

        // Baza danych - musi być przed DepositManager
        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        farmerManager = FarmerManager(this)
        stoniarkaManager = StoniarkaManager(this)

        // System depozytów
        depositManager = DepositManager(this)
        depositManager.loadLimits()

        depositGUI = DepositGUI(this)

        // System backupów
        if (config.getBoolean("backup.enabled", true)) {
            backupManager = BackupManager(this)

            val backupListener = BackupListener(this)
            server.pluginManager.registerEvents(backupListener, this)

            // Rejestruj GUI listener
            val backupGUI = BackupGUI(this)
            server.pluginManager.registerEvents(backupGUI, this)

            // Cleanup task dla backupów - uruchamia się co X godzin
            val cleanupInterval = config.getInt("backup.cleanup.check-interval", 6)
            server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                if (config.getBoolean("backup.cleanup.enabled", true)) {
                    val retentionDays = config.getInt("backup.cleanup.retention-days", 30)
                    val deleted = backupManager.cleanupOldBackups(retentionDays)
                    if (deleted > 0) {
                        logger.info("Usunięto $deleted starych backupów (>$retentionDays dni)")
                    }

                    // Czyszczenie backupów nieaktywnych graczy
                    if (config.getBoolean("backup.cleanup.inactive-players.enabled", true)) {
                        val daysOffline = config.getInt("backup.cleanup.inactive-players.days-offline", 7)
                        val keepLatest = config.getBoolean("backup.cleanup.inactive-players.keep-latest", true)
                        val minToKeep = config.getInt("backup.cleanup.inactive-players.min-backups-to-keep", 1)

                        val deletedInactive = backupManager.cleanupInactivePlayerBackups(daysOffline, keepLatest, minToKeep)
                        if (deletedInactive > 0) {
                            logger.info("Usunięto $deletedInactive backupów nieaktywnych graczy")
                        }
                    }
                }
            }, 20L * 60 * 60 * cleanupInterval, 20L * 60 * 60 * cleanupInterval) // Co X godzin
        }

        // Rejestruj listenery
        val placeListener = FarmerPlaceListener(this)
        server.pluginManager.registerEvents(placeListener, this)

        val stoniarkaListener = StoniarkaListener(this)
        server.pluginManager.registerEvents(stoniarkaListener, this)

        val megaPickaxeListener = MegaPickaxeListener(this)
        server.pluginManager.registerEvents(megaPickaxeListener, this)

        craftingListener = FarmerCraftingListener(this)
        server.pluginManager.registerEvents(craftingListener, this)
        craftingListener.registerRecipes()

        // Deposit listeners
        val depositListener = DepositListener(this)
        server.pluginManager.registerEvents(depositListener, this)
        server.pluginManager.registerEvents(depositGUI, this)

        // Rejestruj komendy
        val command = FunnyMiscCommand(this)
        getCommand("funnymisc")?.setExecutor(command)
        getCommand("funnymisc")?.tabCompleter = command

        // Komenda depozyt (z aliasami: limity, schowek)
        val depositCommand = DepositCommand(this)
        getCommand("depozyt")?.setExecutor(depositCommand)
        getCommand("depozyt")?.tabCompleter = depositCommand

        // Komenda backup (jeśli system włączony)
        if (config.getBoolean("backup.enabled", true)) {
            val backupCommand = BackupCommand(this)
            getCommand("backup")?.setExecutor(backupCommand)
            getCommand("backup")?.tabCompleter = backupCommand
        }

        // Cleanup task - czyści nieaktywnych farmerów co 5 minut
        server.scheduler.runTaskTimer(this, Runnable {
            farmerManager.cleanupInactive()
            stoniarkaManager.cleanupInactive()
        }, 6000L, 6000L) // 6000 ticks = 5 minut

        logger.info("FunnyMisc plugin został włączony!")
    }

    override fun onDisable() {
        // Zatrzymaj wszystkich farmerów (tylko jeśli zostali zainicjalizowani)
        if (::farmerManager.isInitialized) {
            farmerManager.stopAll()
        }

        // Zatrzymaj wszystkie stoniarki i zapisz je (tylko jeśli zostały zainicjalizowane)
        if (::stoniarkaManager.isInitialized) {
            stoniarkaManager.stopAll()
            stoniarkaManager.saveStoniarki()
        }

        // Zamknij backup manager (tylko jeśli został zainicjalizowany)
        if (::backupManager.isInitialized) {
            backupManager.shutdown()
        }

        // Zamknij połączenie z bazą danych (tylko jeśli zostało zainicjalizowane)
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }

        logger.info("FunnyMisc plugin został wyłączony!")
    }

    /**
     * Przeładowuje plugin (np. po zmianie konfiguracji)
     */
    fun reload() {
        // Przeładuj config
        reloadConfig()

        // Zatrzymaj starych farmerów
        farmerManager.stopAll()

        // Zatrzymaj stoniarki, zapisz je i uruchom ponownie
        stoniarkaManager.stopAll()
        stoniarkaManager.saveStoniarki()
        stoniarkaManager.startAll()

        // Przeładuj receptury
        craftingListener.registerRecipes()

        // Przeładuj limity depozytów
        depositManager.loadLimits()

        logger.info("Plugin przeładowany!")
    }
}
