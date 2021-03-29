package ru.cristalix.csc.listener

import me.stepbystep.api.chat.AQUA
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.register
import me.stepbystep.mgapi.lobby.listener.LobbyCustomItemListener
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import me.stepbystep.api.chat.map
import me.stepbystep.api.chat.message0
import ru.cristalix.csc.shop.item.CSCClass
import ru.cristalix.csc.util.openPagedItemsMenu

class ClassesInfoListener(plugin: Plugin) {
    private val title = message0(
        russian = "Классы",
        english = "Classes",
    )

    init {
        LobbyCustomItemListener(
            stack = Material.PAPER.asNewStack(),
            itemTag = "classesShop",
            itemSlot = 1,
            getDisplayName = title.map { "$AQUA$it" }
        ) {
            it.openMenu()
        }.register(plugin)
    }

    private fun Player.openMenu() {
        openPagedItemsMenu(
            title = title,
            items = CSCClass.values().toList(),
        )
    }
}
