package pl.tenfajnybartek.funnymisc.listeners

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.NamespacedKey
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import pl.tenfajnybartek.funnymisc.utils.ItemUtils

/**
 * Listener i manager dla craftingu wszystkich typów farmerów
 */
class FarmerCraftingListener(private val plugin: FunnyPlugin) : Listener {

    /**
     * Rejestruje wszystkie receptury craftingu
     */
    fun registerRecipes() {
        registerBoyFarmerRecipe()
        registerSandFarmerRecipe()
        registerTrenchDiggerRecipe()
        registerStoniarkaRecipe()
        registerCollectorRecipe()
        registerMegaPickaxeRecipe()
    }

    /**
     * Rejestruje recepturę craftingu BoyFarmer
     */
    private fun registerBoyFarmerRecipe() {
        if (!plugin.config.getBoolean("boyfarmer.crafting.enabled", true)) {
            return
        }

        val boyFarmerItem = ItemUtils.createBoyFarmerItem(plugin)
        val key = NamespacedKey(plugin, "boyfarmer_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, boyFarmerItem)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("boyfarmer.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("OOO", "OEO", "OOO")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("boyfarmer.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('O', Material.OBSIDIAN)
            recipe.setIngredient('E', Material.END_PORTAL_FRAME)
        }

        plugin.server.addRecipe(recipe)
    }

    /**
     * Rejestruje recepturę craftingu SandFarmer
     */
    private fun registerSandFarmerRecipe() {
        if (!plugin.config.getBoolean("sandfarmer.crafting.enabled", true)) {
            return
        }

        val sandFarmerItem = ItemUtils.createSandFarmerItem(plugin)
        val key = NamespacedKey(plugin, "sandfarmer_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, sandFarmerItem)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("sandfarmer.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("SSS", "SES", "SSS")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("sandfarmer.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('S', Material.SAND)
            recipe.setIngredient('E', Material.END_PORTAL_FRAME)
        }

        plugin.server.addRecipe(recipe)
    }

    /**
     * Rejestruje recepturę craftingu TrenchDigger
     */
    private fun registerTrenchDiggerRecipe() {
        if (!plugin.config.getBoolean("trenchdigger.crafting.enabled", true)) {
            return
        }

        val trenchDiggerItem = ItemUtils.createTrenchDiggerItem(plugin)
        val key = NamespacedKey(plugin, "trenchdigger_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, trenchDiggerItem)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("trenchdigger.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("DDD", "DED", "DDD")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("trenchdigger.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('D', Material.DIAMOND_PICKAXE)
            recipe.setIngredient('E', Material.END_PORTAL_FRAME)
        }

        plugin.server.addRecipe(recipe)
    }

    /**
     * Rejestruje recepturę craftingu Stoniarki
     */
    private fun registerStoniarkaRecipe() {
        if (!plugin.config.getBoolean("stoniarka.crafting.enabled", true)) {
            return
        }

        val stoniarkaItem = ItemUtils.createStoniarkaItem(plugin)
        val key = NamespacedKey(plugin, "stoniarka_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, stoniarkaItem)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("stoniarka.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("RIR", "ISI", "RIR")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("stoniarka.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('R', Material.REDSTONE)
            recipe.setIngredient('I', Material.IRON_INGOT)
            recipe.setIngredient('S', Material.STONE)
        }

        plugin.server.addRecipe(recipe)
    }

    /**
     * Rejestruje recepturę craftingu Stoniarka Collector
     */
    private fun registerCollectorRecipe() {
        if (!plugin.config.getBoolean("stoniarka-collector.crafting.enabled", true)) {
            return
        }

        val collectorItem = ItemUtils.createCollectorItem(plugin)
        val key = NamespacedKey(plugin, "stoniarka_collector_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, collectorItem)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("stoniarka-collector.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("DDD", "DED", " S ")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("stoniarka-collector.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('D', Material.DIAMOND)
            recipe.setIngredient('E', Material.ENDER_EYE)
            recipe.setIngredient('S', Material.STICK)
        }

        plugin.server.addRecipe(recipe)
    }

    /**
     * Rejestruje recepturę craftingu Mega Kilofa
     */
    private fun registerMegaPickaxeRecipe() {
        if (!plugin.config.getBoolean("mega-kilof.crafting.enabled", true)) {
            return
        }

        val megaPickaxe = ItemUtils.createMegaPickaxe(plugin)
        val key = NamespacedKey(plugin, "mega_pickaxe_recipe")

        // Usuń starą recepturę jeśli istnieje
        plugin.server.removeRecipe(key)

        val recipe = ShapedRecipe(key, megaPickaxe)

        // Pobierz shape z configu
        val shapeList = plugin.config.getStringList("mega-kilof.crafting.shape")
        if (shapeList.size == 3) {
            recipe.shape(shapeList[0], shapeList[1], shapeList[2])
        } else {
            // Domyślny shape
            recipe.shape("BBB", " S ", " S ")
        }

        // Pobierz ingredienty z configu
        val ingredientsSection = plugin.config.getConfigurationSection("mega-kilof.crafting.ingredients")
        if (ingredientsSection != null) {
            for (ingredientKey in ingredientsSection.getKeys(false)) {
                val materialName = ingredientsSection.getString(ingredientKey)
                val material = materialName?.let { Material.getMaterial(it) }

                if (material != null) {
                    recipe.setIngredient(ingredientKey[0], material)
                }
            }
        } else {
            // Domyślne ingredienty
            recipe.setIngredient('B', Material.DIAMOND_BLOCK)
            recipe.setIngredient('S', Material.STICK)
        }

        plugin.server.addRecipe(recipe)
    }

    @EventHandler
    fun onCraft(event: PrepareItemCraftEvent) {
        val result = event.inventory.result ?: return

        // Sprawdź czy craftowany item to któryś z farmerów, narzędzi lub Mega Kilof
        if (ItemUtils.isBoyFarmer(result, plugin) ||
            ItemUtils.isSandFarmer(result, plugin) ||
            ItemUtils.isTrenchDigger(result, plugin) ||
            ItemUtils.isStoniarka(result, plugin) ||
            ItemUtils.isCollector(result, plugin) ||
            ItemUtils.isMegaPickaxe(result, plugin)) {
            // Możesz tutaj dodać dodatkową logikę jeśli potrzeba
            // np. sprawdzenie permisji podczas craftingu
        }
    }
}
