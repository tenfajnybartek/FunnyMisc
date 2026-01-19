package pl.tenfajnybartek.funnymisc.deposit

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pl.tenfajnybartek.funnymisc.base.FunnyPlugin
import java.util.UUID

/**
 * GUI Manager dla systemu depozytów
 */
class DepositGUI(private val plugin: FunnyPlugin) : Listener {

    private val openGUIs = mutableMapOf<UUID, Inventory>()

    /**
     * Otwiera GUI depozytu dla gracza
     */
    fun openDepositGUI(player: Player) {
        val depositManager = plugin.depositManager
        val deposits = depositManager.getAllDeposits(player.uniqueId)

        // Rozmiar GUI z configu (domyślnie 54)
        val guiSize = plugin.config.getInt("deposit.gui.size", 54)
        // Walidacja rozmiaru (musi być wielokrotnością 9)
        val validSize = if (guiSize % 9 == 0 && guiSize in 9..54) guiSize else 54

        val title = plugin.messageManager.getMessage("deposit.gui.title")
        val inventory = Bukkit.createInventory(null, validSize, title)

        // Oblicz ile slotów dostępnych dla itemów (ostatni rząd to dolny pasek)
        val availableSlots = validSize - 9

        // Pobierz konfigurację slotów
        val customSlots = mutableMapOf<Material, Int>()
        val itemSlotsSection = plugin.config.getConfigurationSection("deposit.gui.item-slots")
        if (itemSlotsSection != null) {
            for (key in itemSlotsSection.getKeys(false)) {
                try {
                    val material = Material.valueOf(key.uppercase())
                    val slot = itemSlotsSection.getInt(key)
                    if (slot in 0 until availableSlots) {
                        customSlots[material] = slot
                    }
                } catch (_: IllegalArgumentException) {
                    // Ignoruj nieprawidłowe materiały
                }
            }
        }

        val autoFill = plugin.config.getBoolean("deposit.gui.auto-fill", true)

        // Dodaj itemy z limitami do GUI
        val materials = depositManager.getLimitedMaterials().sortedBy { it.name }
        val usedSlots = mutableSetOf<Int>()

        // Najpierw dodaj itemy z custom slotami
        for (material in materials) {
            val customSlot = customSlots[material]
            if (customSlot != null && customSlot !in usedSlots) {
                val depositAmount = deposits[material] ?: 0
                val limit = depositManager.getLimit(material)
                val currentInInventory = player.inventory.all(material).values.sumOf { it.amount }

                val itemStack = createDepositItem(material, depositAmount, limit, currentInInventory)
                inventory.setItem(customSlot, itemStack)
                usedSlots.add(customSlot)
            }
        }

        // Potem dodaj pozostałe itemy automatycznie (od lewej)
        if (autoFill) {
            val materialsToAdd = materials.filter { !customSlots.containsKey(it) }
            var autoSlot = 0

            for (material in materialsToAdd) {
                // Znajdź wolny slot
                while (autoSlot in usedSlots && autoSlot < availableSlots) {
                    autoSlot++
                }

                if (autoSlot >= availableSlots) break

                val depositAmount = deposits[material] ?: 0
                val limit = depositManager.getLimit(material)
                val currentInInventory = player.inventory.all(material).values.sumOf { it.amount }

                val itemStack = createDepositItem(material, depositAmount, limit, currentInInventory)
                inventory.setItem(autoSlot, itemStack)
                usedSlots.add(autoSlot)
                autoSlot++
            }
        }

        // Wypełnij puste sloty (jeśli włączone)
        addFillerItems(inventory, usedSlots, availableSlots)

        // Dolny pasek - info i przyciski
        addBottomBar(inventory, validSize)

        // Otwórz GUI
        player.openInventory(inventory)
        openGUIs[player.uniqueId] = inventory
    }

    /**
     * Tworzy item reprezentujący depozyt
     */
    private fun createDepositItem(material: Material, depositAmount: Int, limit: Int, currentInInventory: Int): ItemStack {
        val item = ItemStack(material, 1)

        item.editMeta { meta ->
            // Nazwa
            val displayName = getMaterialDisplayName(material)
            val nameFormat = plugin.config.getString("deposit.gui.item-name-format") ?: "<yellow><bold><item></bold></yellow>"
            val nameComponent = plugin.messageManager.parse(
                nameFormat.replace("<item>", displayName)
            ).decoration(TextDecoration.ITALIC, false)
            meta.displayName(nameComponent)

            // Lore
            val loreList = mutableListOf<Component>()

            // Informacje
            val limitFormat = plugin.config.getString("deposit.gui.lore.limit") ?: "<gray>Limit: <white><limit></white></gray>"
            loreList.add(plugin.messageManager.parse(
                limitFormat.replace("<limit>", limit.toString())
            ).decoration(TextDecoration.ITALIC, false))

            val currentFormat = plugin.config.getString("deposit.gui.lore.current") ?: "<gray>W ekwipunku: <white><current></white>/<limit></gray>"
            loreList.add(plugin.messageManager.parse(
                currentFormat.replace("<current>", currentInInventory.toString())
                    .replace("<limit>", limit.toString())
            ).decoration(TextDecoration.ITALIC, false))

            val depositFormat = plugin.config.getString("deposit.gui.lore.deposit") ?: "<gray>W depozycie: <gold><deposit></gold></gray>"
            loreList.add(plugin.messageManager.parse(
                depositFormat.replace("<deposit>", depositAmount.toString())
            ).decoration(TextDecoration.ITALIC, false))

            loreList.add(Component.empty())

            // Instrukcje
            if (depositAmount > 0 && currentInInventory < limit) {
                val clickFormat = plugin.config.getString("deposit.gui.lore.click-withdraw") ?: "<green>» Kliknij aby wypłacić"
                loreList.add(plugin.messageManager.parse(clickFormat).decoration(TextDecoration.ITALIC, false))

                val canWithdraw = minOf(depositAmount, limit - currentInInventory)
                val withdrawFormat = plugin.config.getString("deposit.gui.lore.withdraw-amount") ?: "<gray>  Wypłacisz: <white><amount></white></gray>"
                loreList.add(plugin.messageManager.parse(
                    withdrawFormat.replace("<amount>", canWithdraw.toString())
                ).decoration(TextDecoration.ITALIC, false))
            } else if (currentInInventory >= limit) {
                val fullFormat = plugin.config.getString("deposit.gui.lore.inventory-full") ?: "<red>» Ekwipunek pełny!"
                loreList.add(plugin.messageManager.parse(fullFormat).decoration(TextDecoration.ITALIC, false))
            } else {
                val noDepositFormat = plugin.config.getString("deposit.gui.lore.no-deposit") ?: "<red>» Brak itemów w depozycie"
                loreList.add(plugin.messageManager.parse(noDepositFormat).decoration(TextDecoration.ITALIC, false))
            }

            meta.lore(loreList)
        }

        return item
    }

    /**
     * Dodaje dolny pasek z informacjami
     */
    private fun addBottomBar(inventory: Inventory, guiSize: Int) {
        val grayPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1)
        grayPane.editMeta { meta ->
            meta.displayName(Component.text(" "))
        }

        // Ostatni rząd (9 slotów)
        val lastRowStart = guiSize - 9
        for (i in lastRowStart until guiSize) {
            inventory.setItem(i, grayPane)
        }

        // Info item na środku ostatniego rzędu
        val middleSlot = lastRowStart + 4 // Środkowy slot ostatniego rzędu
        val infoItem = ItemStack(Material.BOOK, 1)
        infoItem.editMeta { meta ->
            val nameFormat = plugin.config.getString("deposit.gui.info.name") ?: "<gold><bold>ℹ Informacje</bold></gold>"
            val nameComponent = plugin.messageManager.parse(nameFormat).decoration(TextDecoration.ITALIC, false)
            meta.displayName(nameComponent)

            val loreList = plugin.config.getStringList("deposit.gui.info.lore").map {
                plugin.messageManager.parse(it).decoration(TextDecoration.ITALIC, false)
            }
            meta.lore(loreList)
        }

        inventory.setItem(middleSlot, infoItem)

        // Przycisk "Wypłać wszystko" (jeśli włączony)
        val withdrawAllEnabled = plugin.config.getBoolean("deposit.gui.withdraw-all.enabled", true)
        if (withdrawAllEnabled) {
            val withdrawAllSlot = plugin.config.getInt("deposit.gui.withdraw-all.slot", 48)
            val materialName = plugin.config.getString("deposit.gui.withdraw-all.material") ?: "CHEST"
            val material = try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                Material.CHEST
            }

            val withdrawAllItem = ItemStack(material, 1)
            withdrawAllItem.editMeta { meta ->
                val nameFormat = plugin.config.getString("deposit.gui.withdraw-all.name") ?: "<green><bold>↓ Wypłać Wszystko</bold></green>"
                val nameComponent = plugin.messageManager.parse(nameFormat).decoration(TextDecoration.ITALIC, false)
                meta.displayName(nameComponent)

                val loreList = plugin.config.getStringList("deposit.gui.withdraw-all.lore").map {
                    plugin.messageManager.parse(it).decoration(TextDecoration.ITALIC, false)
                }
                meta.lore(loreList)
            }

            inventory.setItem(withdrawAllSlot, withdrawAllItem)
        }
    }

    /**
     * Obsługuje kliknięcie w GUI
     * Tylko cancel eventu tutaj, reszta CAŁKOWICIE ASYNC
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Sprawdź czy to nasze GUI
        if (!openGUIs.containsKey(player.uniqueId)) return

        event.isCancelled = true // Jedyna rzecz w sync - cancel eventu

        val clickedItem = event.currentItem ?: return
        if (clickedItem.type.isAir) return

        val clickedSlot = event.rawSlot
        val material = clickedItem.type

        // WSZYSTKO INNE ASYNC - zero przełączeń sync/async w logice!
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val withdrawAllSlot = plugin.config.getInt("deposit.gui.withdraw-all.slot", 48)
                val withdrawAllEnabled = plugin.config.getBoolean("deposit.gui.withdraw-all.enabled", true)

                if (withdrawAllEnabled && clickedSlot == withdrawAllSlot) {
                    handleWithdrawAll(player)
                } else {
                    handleWithdrawSingle(player, material)
                }
            } catch (e: Exception) {
                plugin.logger.warning("Błąd w async handler kliknięcia GUI: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    /**
     * Obsługuje wypłacenie pojedynczego itemu
     * CAŁKOWICIE ASYNC - tylko finalne dodanie do inventory w sync
     */
    private fun handleWithdrawSingle(player: Player, material: Material) {
        val depositManager = plugin.depositManager

        // Sprawdź czy to item z limitem (cache - instant)
        if (!depositManager.hasLimit(material)) return

        // WSZYSTKIE operacje DB w ASYNC (już jesteśmy w async wątku)
        val depositAmount = depositManager.getDepositAmount(player.uniqueId, material)
        val limit = depositManager.getLimit(material)

        // Policz inventory - bezpieczne w async (read-only)
        val currentInInventory = player.inventory.all(material).values.sumOf { it.amount }

        // Walidacja - wszystko w async
        if (depositAmount <= 0) {
            sendMessageSync(player, "deposit.messages.no-items-in-deposit")
            return
        }

        if (currentInInventory >= limit) {
            sendMessageSync(player, "messages.inventory-limit-reached",
                "item" to getMaterialDisplayName(material),
                "limit" to limit.toString()
            )
            return
        }

        // Oblicz ile można wypłacić
        val canWithdraw = minOf(depositAmount, limit - currentInInventory, material.maxStackSize)

        if (canWithdraw <= 0) {
            sendMessageSync(player, "messages.cannot-withdraw")
            return
        }

        // Usuń z depozytu (ASYNC DB operation)
        val removed = depositManager.removeFromDeposit(player.uniqueId, material, canWithdraw)

        if (removed) {
            // JEDYNY sync call - dodaj item i odśwież GUI
            plugin.server.scheduler.runTask(plugin, Runnable {
                val itemToGive = ItemStack(material, canWithdraw)
                player.inventory.addItem(itemToGive)

                plugin.messageManager.sendMessage(player, "messages.withdrawn",
                    "amount" to canWithdraw.toString(),
                    "item" to getMaterialDisplayName(material)
                )

                // Odśwież GUI (async open - nie blokuje)
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    openDepositGUI(player)
                }, 1L)
            })
        }
    }

    /**
     * Pomocnicza funkcja do wysyłania wiadomości sync z async contextu
     */
    private fun sendMessageSync(player: Player, key: String, vararg placeholders: Pair<String, String>) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.messageManager.sendMessage(player, key, *placeholders)
        })
    }

    /**
     * Obsługuje zamknięcie GUI
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openGUIs.remove(player.uniqueId)
    }

    /**
     * Pobiera nazwę wyświetlaną materiału
     */
    private fun getMaterialDisplayName(material: Material): String {
        val customName = plugin.config.getString("deposit.display-names.${material.name.lowercase()}")
        if (customName != null) {
            return customName
        }

        return material.name.lowercase()
            .split("_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }


    /**
     * Wypełnia puste sloty wypełniaczem
     */
    private fun addFillerItems(inventory: Inventory, usedSlots: Set<Int>, availableSlots: Int) {
        val fillerEnabled = plugin.config.getBoolean("deposit.gui.filler.enabled", true)
        if (!fillerEnabled) return

        val materialName = plugin.config.getString("deposit.gui.filler.material") ?: "GRAY_STAINED_GLASS_PANE"
        val material = try {
            Material.valueOf(materialName.uppercase())
        } catch (e: IllegalArgumentException) {
            Material.GRAY_STAINED_GLASS_PANE
        }

        val fillerName = plugin.config.getString("deposit.gui.filler.name") ?: " "

        val fillerItem = ItemStack(material, 1)
        fillerItem.editMeta { meta ->
            meta.displayName(Component.text(fillerName))
        }

        // Wypełnij wszystkie puste sloty (oprócz dolnego paska)
        for (slot in 0 until availableSlots) {
            if (slot !in usedSlots) {
                inventory.setItem(slot, fillerItem)
            }
        }
    }

    /**
     * Wypłaca wszystkie możliwe itemy z depozytu
     * CAŁKOWICIE ASYNC - tylko batch inventory operations w jednym sync call
     */
    private fun handleWithdrawAll(player: Player) {
        val depositManager = plugin.depositManager

        // WSZYSTKO w ASYNC
        val deposits = depositManager.getAllDeposits(player.uniqueId)

        if (deposits.isEmpty()) {
            sendMessageSync(player, "messages.withdraw-all-nothing")
            return
        }

        // Przygotuj batch itemów do dodania (wszystko w async)
        val itemsToAdd = mutableListOf<Pair<Material, Int>>()
        var hasFullLimits = false

        // Cała logika w ASYNC
        for ((material, depositAmount) in deposits) {
            if (depositAmount <= 0) continue

            val limit = depositManager.getLimit(material)
            val currentInInventory = player.inventory.all(material).values.sumOf { it.amount }

            if (currentInInventory >= limit) {
                hasFullLimits = true
                continue
            }

            val canWithdraw = minOf(depositAmount, limit - currentInInventory, material.maxStackSize)

            if (canWithdraw <= 0) {
                hasFullLimits = true
                continue
            }

            // DB operation - w async
            if (depositManager.removeFromDeposit(player.uniqueId, material, canWithdraw)) {
                itemsToAdd.add(material to canWithdraw)
            }
        }

        val withdrawnCount = itemsToAdd.size
        val finalHasFullLimits = hasFullLimits

        // JEDYNY sync call - batch wszystkich operacji inventory
        plugin.server.scheduler.runTask(plugin, Runnable {
            // Dodaj wszystkie itemy jednocześnie
            for ((material, amount) in itemsToAdd) {
                val itemToGive = ItemStack(material, amount)
                player.inventory.addItem(itemToGive)
            }

            // Wiadomość
            when {
                withdrawnCount == 0 -> {
                    plugin.messageManager.sendMessage(player, "messages.withdraw-all-nothing")
                }
                finalHasFullLimits -> {
                    plugin.messageManager.sendMessage(player, "messages.withdraw-all-partial",
                        "count" to withdrawnCount.toString()
                    )
                }
                else -> {
                    plugin.messageManager.sendMessage(player, "messages.withdraw-all-success",
                        "count" to withdrawnCount.toString()
                    )
                }
            }

            // Odśwież GUI
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                openDepositGUI(player)
            }, 1L)
        })
    }
}
