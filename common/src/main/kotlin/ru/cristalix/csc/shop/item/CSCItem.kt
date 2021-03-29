package ru.cristalix.csc.shop.item

import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.addItemFlag
import me.stepbystep.api.item.createNMSItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.chat.invoke
import ru.cristalix.csc.shop.CSCDonate

interface CSCItem {
    val material: Material
    val displayName: PMessage0
    val description: List<PMessage0>
    val data: Byte

    val requiredDonate: CSCDonate?

    fun createDisplayItem(player: Player): NMSItemStack = createNMSItem(
        material = material,
        displayName = displayName(player),
        lore = description(player),
        data = data,
    ).apply {
        addItemFlag(ItemFlag.HIDE_POTION_EFFECTS)
        addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
    }
}
