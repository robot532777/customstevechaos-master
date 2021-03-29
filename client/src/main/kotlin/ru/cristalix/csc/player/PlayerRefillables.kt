package ru.cristalix.csc.player

import me.stepbystep.api.addItem
import me.stepbystep.api.asBukkit
import me.stepbystep.api.asNMS
import me.stepbystep.api.item.NMSItemStack
import ru.cristalix.csc.util.*

class PlayerRefillables(private val cscPlayer: CSCPlayer) {
    val allEntries = arrayListOf<RefillableEntry>()

    fun bindSlot(slot: Int, stack: NMSItemStack) {
        bindSlot(slot) { it.isSimilarWithoutDisplayAndUUID(stack) }
    }

    inline fun bindSlot(slot: Int, isApplicable: (NMSItemStack) -> Boolean) {
        val applicableEntry = allEntries.maxByOrNull {
            when {
                !isApplicable(it.stack) -> 0
                it.slot != CURSOR_SLOT -> 1
                else -> 2
            }
        }
        applicableEntry?.slot = slot
    }

    fun updateEntry(oldStack: NMSItemStack, newStack: NMSItemStack) {
        allEntries.firstOrNull { oldStack.isSimilarWithoutDisplayAndUUID(it.stack) }?.stack = newStack.cloneItemStack()
    }

    fun add(stack: NMSItemStack) {
        allEntries += RefillableEntry(stack, CURSOR_SLOT)
    }

    fun remove(stack: NMSItemStack) {
        val entry = allEntries.firstOrNull { it.stack.isSimilarWithoutDisplayAndUUID(stack) } ?: return
        allEntries -= entry
    }

    fun updateInventory() {
        val player = cscPlayer.asBukkitPlayerOrNull() ?: return

        val inventory = player.inventory.asNMS()
        for (slot in CURSOR_SLOT until inventory.size) {
            val stack = inventory.getItemOrCursor(slot)
            val material = stack.item?.asBukkit() ?: continue
            if (material.isRefillable()) {
                inventory.setItemOrCursor(slot, NMSItemStack.a)
                bindSlot(slot, stack)
            }
        }

        allEntries.forEach { (stack, slot) ->
            stack.updateLoreIfNeed(player, cscPlayer.gameScheduler)
            if (slot == CURSOR_SLOT || !inventory.getItem(slot).isEmpty) {
                inventory.addItem(stack)
            } else {
                inventory.setItem(slot, stack.cloneItemStack())
            }
        }

        player.updateInventory()
    }

    data class RefillableEntry(
        var stack: NMSItemStack,
        var slot: Int,
    )
}
