package ru.cristalix.csc.util

import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.chat.message0
import me.stepbystep.api.item.updateLore
import me.stepbystep.api.menu.buildPagedMenu
import me.stepbystep.api.menu.menuItem
import org.bukkit.DyeColor
import org.bukkit.entity.Player
import ru.cristalix.csc.PlayerAccount
import ru.cristalix.csc.shop.item.CSCItem
import kotlin.reflect.KMutableProperty0

private const val ITEMS_ON_PAGE = 8

private val selectedMessage = message0(
    russian = "${GREEN}ВЫБРАНО",
    english = "${GREEN}SELECTED",
)

fun <T : CSCItem> Player.openSelectablePagedItemsMenu(
    title: PMessage0,
    items: List<T>,
    itemProperty: KMutableProperty0<T>,
) {
    openPagedItemsMenu(
        title = title,
        items = items,
        onClick = { item ->
            if (itemProperty.get() != item && PlayerAccount[this].isPurchased(item)) {
                itemProperty.set(item)
                openSelectablePagedItemsMenu(title, items, itemProperty)
            }
        }
    ) { player, item ->
        if (item == itemProperty.get())
            listOf("", selectedMessage(this))
        else
            defaultAvailabilityText(player, item)
    }
}

fun <T : CSCItem> Player.openPagedItemsMenu(
    title: PMessage0,
    items: List<T>,
    onClick: (T) -> Unit = {},
    getAvailabilityText: (Player, T) -> List<String> = ::defaultAvailabilityText,
) {
    val maxPage = items.lastIndex / ITEMS_ON_PAGE

    buildPagedMenu(true, maxPage) { page ->
        size = 45
        this.title = title(this@openPagedItemsMenu)

        fillGlass(DyeColor.GRAY)

        val slots = menuSlotsIterator()
        val firstIndex = ITEMS_ON_PAGE * page
        val lastIndex = (firstIndex + ITEMS_ON_PAGE).coerceAtMost(items.size)
        val openedItems = items.subList(firstIndex, lastIndex)

        for (item in openedItems) {
            val availabilityText = getAvailabilityText(this@openPagedItemsMenu, item)
            val stack = item.createDisplayItem(this@openPagedItemsMenu).updateLore {
                it += availabilityText
            }

            slots.next() bindTo menuItem(stack) {
                onClick(item)
            }
        }
    }.openFor(this)
}

private fun defaultAvailabilityText(player: Player, item: CSCItem) = when {
    PlayerAccount[player].isPurchased(item) -> listOf("", "${GREEN}Доступно")
    else -> emptyList()
}
