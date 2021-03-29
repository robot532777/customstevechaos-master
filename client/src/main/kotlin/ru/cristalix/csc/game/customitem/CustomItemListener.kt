package ru.cristalix.csc.game.customitem

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.DyeColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.compactMenuSlotsIterator

class CustomItemListener(plugin: Plugin) : Listener {
    private val shopNpcID = plugin.getOrCreateConfig("config.yml").getInt("shopNpcID")

    @EventHandler
    fun NPCRightClickEvent.handle() {
        if (npc.id != shopNpcID) return

        val cscPlayer = CSCPlayer.getOrNull(clicker.uniqueId)?.takeIf { it.isAlive }
        if (cscPlayer == null) {
            clicker.sendMessage(CommonMessages.notInGame)
            return
        }

        buildSharedMenu {
            title = Messages.itemShop(clicker)

            fillGlass(DyeColor.GRAY)

            val slots = compactMenuSlotsIterator()

            for (item in CustomItem.VALUES) {
                val shopInfo = item.shopInfo ?: continue

                slots.next() bindTo menuItem(item.createDisplayStack(clicker, cscPlayer.balance)) {
                    when {
                        cscPlayer.balance < shopInfo.price -> {
                            clicker.sendMessage(CommonMessages.notEnoughBalance)
                        }
                        item.alreadyPurchased(clicker) -> {
                            clicker.sendMessage(Messages.alreadyBought)
                        }
                        !item.canBePurchased(clicker) -> return@menuItem
                        clicker.inventory.asNMS().addItem(item.createStack(clicker)) -> {
                            cscPlayer.additionalWorth += shopInfo.price
                            cscPlayer.changeGold(-shopInfo.price)
                            item.onPurchase(clicker)
                            clicker.sendMessage(Messages.successfullyBought)
                        }
                        else -> {
                            clicker.sendMessage(CommonMessages.notEnoughInventorySpace)
                        }
                    }
                }
            }
        }.openFor(clicker)
    }

    private object Messages {
        val itemShop = message0(
            russian = "Магазин предметов",
            english = "Item shop",
        )

        val alreadyBought = message0(
            russian = "${RED}Вы уже купили этот предмет",
            english = "${RED}You have already bought this item",
        )

        val successfullyBought = message0(
            russian = "${GREEN}Вы успешно купили предмет",
            english = "${GREEN}You successfully bought an item",
        )
    }
}
