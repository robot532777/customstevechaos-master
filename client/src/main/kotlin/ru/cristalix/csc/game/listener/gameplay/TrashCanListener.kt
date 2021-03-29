package ru.cristalix.csc.game.listener.gameplay

import me.stepbystep.api.*
import me.stepbystep.api.chat.sendMessage
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.menu.Menu
import me.stepbystep.api.menu.MenuHolder
import me.stepbystep.api.menu.buildSharedMenu
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.minecraft.server.v1_12_R1.IInventory
import net.minecraft.server.v1_12_R1.ItemArrow
import net.minecraft.server.v1_12_R1.Items
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.asCSCPlayer

class TrashCanListener(private val plugin: Plugin) : Listener {
    private val npcID = plugin.getOrCreateConfig("config.yml").getInt("trashNpcID")

    @EventHandler
    fun NPCRightClickEvent.handle() {
        if (npc.id != npcID) return

        if (!CSCPlayer.isAlive(clicker.uniqueId)) {
            clicker.sendMessage(CommonMessages.notInGame)
            return
        }

        buildSharedMenu {
            title = "Мусорка"
            allowAllClicks = true
            cancelClickDefault = false
            createMenuHolder = ::TrashCanMenuHolder

            onAnyClick {
                val cscPlayer = it.whoClicked.asCSCPlayer()
                plugin.runDelayed(3) {
                    it.inventory.asNMS().clearTrashItems(cscPlayer)
                    (it.whoClicked as Player).updateInventory()
                }
            }
        }.openFor(clicker)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun InventoryClickEvent.handle() {
        if (inventory.holder !is TrashCanMenuHolder) return
        val action = action
        val isInTopInventory = isInTopInventory()

        if (action == InventoryAction.HOTBAR_SWAP) {
            val bottomInventory = view.bottomInventory.asNMS()
            if (!bottomInventory.getItem(hotbarButton).shouldBeCleared()) {
                cancel()
                return
            }
        }

        if (isInTopInventory) return

        val currentItem = currentItem?.asNMS() ?: return
        if (currentItem.shouldBeCleared()) return

        cancel()
    }

    private fun IInventory.clearTrashItems(cscPlayer: CSCPlayer) {
        for (i in 0 until size) {
            val currentStack = getItem(i)
            if (currentStack.shouldBeCleared()) {
                setItem(i, NMSItemStack.a)
                cscPlayer.refillables.remove(currentStack)
            }
        }
    }

    private fun NMSItemStack.shouldBeCleared(): Boolean = item != Items.COMPASS && item !is ItemArrow

    private class TrashCanMenuHolder(menu: Menu) : MenuHolder(menu)
}
