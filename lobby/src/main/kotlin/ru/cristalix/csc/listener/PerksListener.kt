package ru.cristalix.csc.listener

import me.stepbystep.api.chat.GRAY
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import me.stepbystep.api.register
import me.stepbystep.mgapi.lobby.listener.LobbyCustomItemListener
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.plugin.Plugin
import ru.cristalix.csc.PlayerAccount
import me.stepbystep.api.chat.message0
import ru.cristalix.csc.shop.item.CSCCage
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.util.openSelectablePagedItemsMenu

class PerksListener(plugin: Plugin) {

    init {
        LobbyCustomItemListener(
            stack = Material.ENDER_CHEST.asNewStack(),
            itemTag = "perksItem",
            itemSlot = 4,
            getDisplayName = message0(
                russian = "${GREEN}Перки",
                english = "${GREEN}Perks",
            ),
            interactAction = ::openPerksMenu,
        ).register(plugin)
    }

    private fun HumanEntity.asPlayer(): Player = this as Player

    private val armorSetsMessage = message0(
        russian = "${GREEN}Сеты брони",
        english = "${GREEN}Armor sets",
    )

    private val soonMessage = message0(
        russian = "${GRAY}Скоро",
        english = "${GRAY}Soon",
    )

    private fun openPerksMenu(player: Player) {
        buildSharedMenu {
            size = 27
            title = "Перки"

            10 bindTo menuItem(
                material = Material.GOLD_CHESTPLATE,
                displayName = armorSetsMessage(player),
                lore = listOf(soonMessage(player)),
                flags = ItemFlag.values(),
            ) {
                whoClicked.asPlayer().openArmorMenu()
            }

            12 bindTo menuItem(
                material = Material.IRON_SWORD,
                displayName = "$GREEN${weaponsTitle(player)}",
                flags = ItemFlag.values(),
            ) {
                whoClicked.asPlayer().openWeaponsMenu()
            }

            14 bindTo menuItem(
                material = Material.IRON_FENCE,
                displayName = "$GREEN${cagesTitle(player)}",
            ) {
                whoClicked.asPlayer().openCagesMenu()
            }

            16 bindTo menuItem(
                material = Material.CHEST,
                displayName = "$GREEN${messagePacksTitle(player)}",
            ) {
                whoClicked.asPlayer().openMessagePacksMenu()
            }
        }.openFor(player)
    }

    private fun Player.openArmorMenu() {

    }

    private val weaponsTitle = message0(
        russian = "Паки оружия",
        english = "Weapon packs",
    )
    private fun Player.openWeaponsMenu() {
        openSelectablePagedItemsMenu(
            title = weaponsTitle,
            items = CSCWeaponSkin.values().toList(),
            itemProperty = PlayerAccount[this]::weaponSkin,
        )
    }

    private val messagePacksTitle = message0(
        russian = "Сообщения",
        english = "Message packs",
    )
    private fun Player.openMessagePacksMenu() {
        openSelectablePagedItemsMenu(
            title = messagePacksTitle,
            items = CSCMessagePack.values().toList(),
            itemProperty = PlayerAccount[this]::messagePack,
        )
    }

    private val cagesTitle = message0(
        russian = "Клетки",
        english = "Cages",
    )
    private fun Player.openCagesMenu() {
        openSelectablePagedItemsMenu(
            title = cagesTitle,
            items = CSCCage.values().toList(),
            itemProperty = PlayerAccount[this]::selectedCage,
        )
    }
}
