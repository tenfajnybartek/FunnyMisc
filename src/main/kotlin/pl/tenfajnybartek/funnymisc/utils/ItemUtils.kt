package pl.tenfajnybartek.funnymisc.utils

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin

/**
 * Utility class do tworzenia i zarządzania itemami farmerów
 */
object ItemUtils {

    private const val BOYFARMER_KEY = "boyfarmer"
    private const val SANDFARMER_KEY = "sandfarmer"
    private const val TRENCHDIGGER_KEY = "trenchdigger"
    private const val STONIARKA_KEY = "stoniarka"
    private const val COLLECTOR_KEY = "stoniarka_collector"
    private const val COLLECTOR_USES_KEY = "collector_uses"
    private const val MEGA_PICKAXE_KEY = "mega_pickaxe"

    /**
     * Tworzy item BoyFarmer na podstawie konfiguracji
     */
    fun createBoyFarmerItem(plugin: FunnyPlugin): ItemStack {
        // Pobierz materiał z configu
        val materialName = plugin.config.getString("boyfarmer.base-material") ?: "END_PORTAL_FRAME"
        val material = Material.getMaterial(materialName) ?: Material.END_PORTAL_FRAME

        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa - używamy MessageManager do parsowania MiniMessage
            val displayName = plugin.messageManager.getMessage("boyfarmer.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Lore
            val loreList = plugin.messageManager.getMessageList("boyfarmer.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)

            // Dodaj NBT tag do identyfikacji
            val key = NamespacedKey(plugin, BOYFARMER_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1.toByte())
        }

        return item
    }

    /**
     * Sprawdza czy item jest BoyFarmerem
     */
    fun isBoyFarmer(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, BOYFARMER_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Tworzy item SandFarmer na podstawie konfiguracji
     */
    fun createSandFarmerItem(plugin: FunnyPlugin): ItemStack {
        // Pobierz materiał z configu
        val materialName = plugin.config.getString("sandfarmer.base-material") ?: "END_PORTAL_FRAME"
        val material = Material.getMaterial(materialName) ?: Material.END_PORTAL_FRAME

        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = plugin.messageManager.getMessage("sandfarmer.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Lore
            val loreList = plugin.messageManager.getMessageList("sandfarmer.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)

            // Dodaj NBT tag do identyfikacji
            val key = NamespacedKey(plugin, SANDFARMER_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1.toByte())
        }

        return item
    }

    /**
     * Sprawdza czy item jest SandFarmerem
     */
    fun isSandFarmer(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, SANDFARMER_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Tworzy item TrenchDigger na podstawie konfiguracji
     */
    fun createTrenchDiggerItem(plugin: FunnyPlugin): ItemStack {
        // Pobierz materiał z configu
        val materialName = plugin.config.getString("trenchdigger.base-material") ?: "END_PORTAL_FRAME"
        val material = Material.getMaterial(materialName) ?: Material.END_PORTAL_FRAME

        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = plugin.messageManager.getMessage("trenchdigger.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Lore
            val loreList = plugin.messageManager.getMessageList("trenchdigger.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)

            // Dodaj NBT tag do identyfikacji
            val key = NamespacedKey(plugin, TRENCHDIGGER_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1.toByte())
        }

        return item
    }

    /**
     * Sprawdza czy item jest TrenchDiggerem
     */
    fun isTrenchDigger(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, TRENCHDIGGER_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Tworzy item Stoniarka na podstawie konfiguracji
     */
    fun createStoniarkaItem(plugin: FunnyPlugin): ItemStack {
        // Pobierz materiał z configu
        val materialName = plugin.config.getString("stoniarka.base-material") ?: "END_STONE"
        val material = Material.getMaterial(materialName) ?: Material.END_STONE

        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = plugin.messageManager.getMessage("stoniarka.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Lore
            val loreList = plugin.messageManager.getMessageList("stoniarka.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)

            // Dodaj NBT tag do identyfikacji
            val key = NamespacedKey(plugin, STONIARKA_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1.toByte())
        }

        return item
    }

    /**
     * Sprawdza czy item jest Stoniarką
     */
    fun isStoniarka(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, STONIARKA_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Tworzy Mega Kilof
     */
    fun createMegaPickaxe(plugin: FunnyPlugin): ItemStack {
        val material = Material.DIAMOND_PICKAXE
        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = plugin.messageManager.getMessage("mega-kilof.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Lore
            val loreList = plugin.messageManager.getMessageList("mega-kilof.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)

            // Enchanty
            val efficiency = plugin.config.getInt("mega-kilof.enchants.efficiency", 10)
            val fortune = plugin.config.getInt("mega-kilof.enchants.fortune", 5)
            val unbreaking = plugin.config.getInt("mega-kilof.enchants.unbreaking", 10)

            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, efficiency, true)
            meta.addEnchant(org.bukkit.enchantments.Enchantment.FORTUNE, fortune, true)
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, unbreaking, true)

            // Unbreakable jeśli włączone
            if (plugin.config.getBoolean("mega-kilof.unbreakable", true)) {
                meta.isUnbreakable = true
            }

            // Dodaj NBT tag do identyfikacji
            val key = NamespacedKey(plugin, MEGA_PICKAXE_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1.toByte())
        }

        return item
    }

    /**
     * Sprawdza czy item jest Mega Kilofem
     */
    fun isMegaPickaxe(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, MEGA_PICKAXE_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Tworzy item Diamentowy Blok (do craftingu)
     */
    fun createDiamondBlock(amount: Int = 1): ItemStack {
        return ItemStack(Material.DIAMOND_BLOCK, amount)
    }

    /**
     * Tworzy item Patyk (do craftingu)
     */
    fun createStick(amount: Int = 1): ItemStack {
        return ItemStack(Material.STICK, amount)
    }

    /**
     * Tworzy Collector item (Stoniarka Collector)
     */
    fun createCollectorItem(plugin: FunnyPlugin): ItemStack {
        // Pobierz materiał z configu
        val materialName = plugin.config.getString("stoniarka-collector.base-material") ?: "GOLDEN_SHOVEL"
        val material = Material.getMaterial(materialName) ?: Material.GOLDEN_SHOVEL

        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = plugin.messageManager.getMessage("stoniarka-collector.display-name")
                .decoration(TextDecoration.ITALIC, false)
            meta.displayName(displayName)

            // Silk Touch level
            val silkTouchLevel = plugin.config.getInt("stoniarka-collector.silk-touch-level", 10)
            meta.addEnchant(org.bukkit.enchantments.Enchantment.SILK_TOUCH, silkTouchLevel, true)

            // Unbreakable
            val unbreakable = plugin.config.getBoolean("stoniarka-collector.unbreakable", true)
            meta.isUnbreakable = unbreakable

            // Maksymalne użycia
            val maxUses = plugin.config.getInt("stoniarka-collector.max-uses", 5)

            // Lore
            val loreList = plugin.messageManager.getMessageList("stoniarka-collector.lore").map {
                it.decoration(TextDecoration.ITALIC, false)
            }.toMutableList()

            // Dodaj info o użyciach do lore jeśli włączone
            if (plugin.config.getBoolean("stoniarka-collector.show-uses-in-lore", true) && maxUses > 0) {
                val usesFormat = plugin.config.getString("stoniarka-collector.uses-lore-format")
                    ?: "<gray>Pozostałe użycia: <green><uses></green>/<max></gray>"
                val usesText = plugin.messageManager.parse(
                    usesFormat.replace("<uses>", maxUses.toString()).replace("<max>", maxUses.toString())
                ).decoration(TextDecoration.ITALIC, false)
                loreList.add(usesText)
            }

            meta.lore(loreList)

            // Dodaj NBT tagi do identyfikacji
            val collectorKey = NamespacedKey(plugin, COLLECTOR_KEY)
            meta.persistentDataContainer.set(collectorKey, PersistentDataType.BYTE, 1.toByte())

            // Zapisz pozostałe użycia
            if (maxUses > 0) {
                val usesKey = NamespacedKey(plugin, COLLECTOR_USES_KEY)
                meta.persistentDataContainer.set(usesKey, PersistentDataType.INTEGER, maxUses)
            }
        }

        return item
    }

    /**
     * Sprawdza czy item jest Stoniarka Collector
     */
    fun isCollector(item: ItemStack?, plugin: FunnyPlugin): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, COLLECTOR_KEY)

        return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
    }

    /**
     * Pobiera pozostałe użycia narzędzia
     */
    fun getCollectorUses(item: ItemStack?, plugin: FunnyPlugin): Int {
        if (item == null) return 0

        val meta = item.itemMeta ?: return 0
        val key = NamespacedKey(plugin, COLLECTOR_USES_KEY)

        return meta.persistentDataContainer.get(key, PersistentDataType.INTEGER) ?: 0
    }

    /**
     * Ustawia pozostałe użycia narzędzia
     */
    fun setCollectorUses(item: ItemStack, uses: Int, plugin: FunnyPlugin) {
        item.editMeta { meta ->
            val key = NamespacedKey(plugin, COLLECTOR_USES_KEY)
            meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, uses)

            // Aktualizuj lore jeśli włączone
            if (plugin.config.getBoolean("stoniarka-collector.show-uses-in-lore", true)) {
                val maxUses = plugin.config.getInt("stoniarka-collector.max-uses", 5)
                val loreList = plugin.messageManager.getMessageList("stoniarka-collector.lore").map {
                    it.decoration(TextDecoration.ITALIC, false)
                }.toMutableList()

                // Dodaj zaktualizowane info o użyciach
                if (maxUses > 0) {
                    val usesFormat = plugin.config.getString("stoniarka-collector.uses-lore-format")
                        ?: "<gray>Pozostałe użycia: <green><uses></green>/<max></gray>"
                    val usesText = plugin.messageManager.parse(
                        usesFormat.replace("<uses>", uses.toString()).replace("<max>", maxUses.toString())
                    ).decoration(TextDecoration.ITALIC, false)
                    loreList.add(usesText)
                }

                meta.lore(loreList)
            }
        }
    }
}
