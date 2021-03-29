package ru.cristalix.csc.lootbox

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import me.stepbystep.mgapi.lobby.LobbyActor
import me.stepbystep.mgapi.lobby.listener.LobbyCustomItemListener
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.PlayerAccount
import me.stepbystep.api.chat.map
import me.stepbystep.api.chat.message0
import me.stepbystep.api.chat.message1
import java.util.*

class LootBoxListener(
    plugin: Plugin,
    private val actor: LobbyActor,
) : Listener {
    private val activeLootBoxes = hashMapOf<UUID, LootBox>()

    private object Messages {
        val title = message0(
            russian = "Лутбокс",
            english = "Lootbox",
        )

        val openLootbox = message0(
            russian = "${GREEN}Открыть лутбокс",
            english = "${GREEN}Open lootbox",
        )

        val lore1 = message1<String>(
            russian = { "${GRAY}Цена: $it${LootBox.PRICE} монет" },
            english = { "${GRAY}Price: $it${LootBox.PRICE} coins" },
        )

        val lore2 = message0(
            russian = "${YELLOW}Клик$GRAY: Открыть лутбокс",
            english = "${YELLOW}Click$GRAY: open lootbox",
        )
    }
    init {
        LobbyCustomItemListener(
            stack = Material.CHEST.asNewStack(),
            itemTag = "lootBoxItem",
            itemSlot = 6,
            getDisplayName = Messages.title.map { "$GOLD$it" }
        ) {
            it.openConfirmMenu()
        }.register(plugin)
    }

    private fun Player.openConfirmMenu() {
        val account = PlayerAccount[this]

        buildSharedMenu {
            size = 27
            title = Messages.title(this@openConfirmMenu)

            val priceColor = when {
                account.balance >= LootBox.PRICE -> GREEN
                else -> RED
            }

            13 bindTo menuItem(
                material = Material.STAINED_GLASS_PANE,
                displayName = Messages.openLootbox(this@openConfirmMenu),
                data = Color.GREEN.woolData.toByte(),
                lore = listOf(
                    Messages.lore1(this@openConfirmMenu, priceColor),
                    "",
                    Messages.lore2(this@openConfirmMenu),
                )
            ) {
                openLootBox(account)
            }
        }.openFor(this)
    }

    private fun openLootBox(account: PlayerAccount) {
        val lootBox = LootBox(actor, account) ?: return
        lootBox.start()
        activeLootBoxes[account.uuid] = lootBox

        lootBox.onComplete = {
            activeLootBoxes -= account.uuid
        }
    }

    @EventHandler
    fun PlayerQuitEvent.handle() {
        activeLootBoxes[player.uniqueId]?.interrupt()
    }
}
