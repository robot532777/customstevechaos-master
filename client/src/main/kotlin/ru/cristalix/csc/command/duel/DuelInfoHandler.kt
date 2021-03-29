package ru.cristalix.csc.command.duel

import me.stepbystep.api.asNMS
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.invoke
import me.stepbystep.api.chat.message0
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.MenuItem
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.unusedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.util.asCSCPlayer

class DuelInfoHandler(private val duelCommand: DuelCommand) {

    fun openMenu(sender: Player, target: Player) {
        val targetInventory = target.inventory
        val menu = buildSharedMenu {
            size = 54
            title = Messages.inventoryTitle(sender)
            createMenuHolder = ::DuelMenuHolder

            1..17 bindTo unusedItem(
                material = Material.STAINED_GLASS_PANE,
                data = Color.GRAY.woolData.toByte(),
                displayName = " "
            )
            2 bindTo sender.createArmorItem(targetInventory.helmet)
            3 bindTo sender.createArmorItem(targetInventory.chestplate)
            5 bindTo sender.createArmorItem(targetInventory.leggings)
            6 bindTo sender.createArmorItem(targetInventory.boots)

            13 bindTo createClassItem(target)

            val itemOnCursor = target.asNMS().inventory.carried
            if (!itemOnCursor.isEmpty) {
                8 bindTo unusedItem(itemOnCursor)
            }

            duelCommand.addGoBackButton(this, sender)

            targetInventory.asNMS().items.forEachIndexed { index, stack ->
                val slot = index + (if (index in 0..8) 45 else 9)
                slot bindTo unusedItem(stack)
            }
        }
        menu.openFor(sender)
    }

    private fun Player.createArmorItem(stack: ItemStack?): MenuItem {
        val stackOrDefault = stack?.asNMS() ?: createNMSItem(
            material = Material.STAINED_GLASS_PANE,
            data = Color.RED.woolData.toByte(),
            displayName = Messages.noArmor(this),
        )

        return unusedItem(stackOrDefault)
    }

    private fun createClassItem(target: Player): MenuItem {
        val clazz = target.asCSCPlayer().selectedClass ?: error("$target did not select class")
        val stack = createNMSItem(
            material = Material.THIN_GLASS,
            displayName = clazz.wrapped.displayName(target),
            lore = clazz.wrapped.description(target),
        )

        return unusedItem(stack)
    }

    private object Messages {
        val noArmor = message0(
            russian = "${RED}Отсутствует",
            english = "${RED}Nothing",
        )

        val inventoryTitle = message0(
            russian = "Просмотр инвентаря",
            english = "Inventory view",
        )
    }
}
