package ru.cristalix.csc.listener

import me.stepbystep.api.chat.*
import me.stepbystep.api.getNow
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.openSafeFor
import me.stepbystep.api.menu.unusedItem
import me.stepbystep.api.register
import me.stepbystep.mgapi.lobby.LobbyActor
import me.stepbystep.mgapi.lobby.listener.LobbyCustomItemListener
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.PlayerAccount
import ru.cristalix.csc.packet.GetPlayerPlacePacket
import java.util.concurrent.CompletableFuture

class PlayerStatisticsListener(
    private val actor: LobbyActor,
) {
    init {
        LobbyCustomItemListener(
            stack = Material.STRUCTURE_VOID.asNewStack(),
            itemTag = "playerStats",
            itemSlot = 7,
            getDisplayName = message0(
                russian = "${GREEN}Ваша статистика",
                english = "${GREEN}Your statistics",
            ),
        ) {
            it.openMenu()
        }.register(actor.plugin)
    }

    private val statisticsMessage = message0(
        russian = "Статистика",
        english = "Statistics",
    )

    private fun Player.openMenu() {
        val messageTransport = actor.messageTransport
        val soloPlaceFuture = messageTransport.sendPacket(
            GetPlayerPlacePacket(GetPlayerPlacePacket.ClientData(uniqueId, CSCGameType.Solo))
        )
        val duoPlaceFuture = messageTransport.sendPacket(
            GetPlayerPlacePacket(GetPlayerPlacePacket.ClientData(uniqueId, CSCGameType.Duo))
        )

        CompletableFuture.allOf(soloPlaceFuture, duoPlaceFuture).thenAccept {
            val soloPlace = soloPlaceFuture.getNow().serverData.place
            val duoPlace = duoPlaceFuture.getNow().serverData.place
            val player = this
            val account = PlayerAccount[this]

            buildSharedMenu {
                size = 27
                title = Messages.menuTitle(player)

                13 bindTo unusedItem(
                    material = Material.STRUCTURE_VOID,
                    displayName = "$GREEN${Messages.menuTitle(player)}",
                    lore = listOf(
                        Messages.maxWave(player, account.maxWave),
                        Messages.totalGames(player, account.totalGames),
                        Messages.soloRating(player, account.soloRating, soloPlace),
                        Messages.duoRating(player, account.duoRating, duoPlace),
                    )
                )
            }.openSafeFor(this, actor.plugin)
        }
    }

    private object Messages {
        val menuTitle = message0(
            russian = "Статистика",
            english = "Statistics",
        )

        val maxWave = message1<Int>(
            russian = { "${GRAY}Рекордная волна: $WHITE$it" },
            english = { "${GRAY}Max wave: $WHITE$it" },
        )

        val totalGames = message1<Int>(
            russian = { "${GRAY}Всего игр: $WHITE$it" },
            english = { "${GRAY}Total games: $WHITE$it" },
        )

        val soloRating = message2<Int, Int>(
            russian = { rating, place -> "${GRAY}Одиночный рейтинг: $WHITE$rating $GOLD(#$place)" },
            english = { rating, place -> "${GRAY}Solo rating: $WHITE$rating $GOLD(#$place)" },
        )

        val duoRating = message2<Int, Int>(
            russian = { rating, place -> "${GRAY}Двойной рейтинг: $WHITE$rating $GOLD(#$place)" },
            english = { rating, place -> "${GRAY}Duo rating: $WHITE$rating $GOLD(#$place)" },
        )
    }
}
