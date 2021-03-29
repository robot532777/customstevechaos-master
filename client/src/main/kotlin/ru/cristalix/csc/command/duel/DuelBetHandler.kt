package ru.cristalix.csc.command.duel

import me.stepbystep.api.chat.*
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.MenuItem
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import me.stepbystep.api.menu.unusedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.asCSCPlayer

class DuelBetHandler(private val duelCommand: DuelCommand) {

    private fun shouldCancel(sender: Player): Boolean {
        if (duelCommand.requireDuelPhase().isParticipant(sender)) {
            sender.sendMessage(Messages.cannotBetInDuel)
            return true
        }

        if (!CSCPlayer.isAlive(sender.uniqueId)) {
            sender.sendMessage(CommonMessages.notInGame)
            return true
        }

        return false
    }

    fun openMenu(sender: Player, target: Player) {
        if (shouldCancel(sender)) return

        val phase = duelCommand.requireDuelPhase()
        val participant = phase.findParticipant(target) ?: error("$target is not participant of duel")
        val currentBet = participant.bets[sender.uniqueId] ?: 0
        val cscPlayer = sender.asCSCPlayer()
        var changedBet = false

        fun createChangeBetButton(diff: Int): MenuItem {
            val glassColor = if (diff > 0) Color.GREEN else Color.RED
            val stack = createNMSItem(
                material = Material.STAINED_GLASS_PANE,
                data = glassColor.woolData.toByte(),
                displayName = "${if (diff > 0) "$GREEN+" else RED}$diff"
            )

            return menuItem(stack) {
                if (cscPlayer.balance < diff) return@menuItem
                val otherParticipant = phase.getOtherParticipant(participant)

                otherParticipant.bets[sender.uniqueId]?.let { otherBet ->
                    sender.sendMessage(Messages.betReturned)
                    cscPlayer.changeGold(otherBet)
                    otherParticipant.bets.remove(sender.uniqueId)
                }

                changedBet = true

                var actualDiff = diff
                if (currentBet + actualDiff > cscPlayer.balance - actualDiff) {
                    actualDiff = (cscPlayer.balance - currentBet) / 2
                }

                if (actualDiff == 0) {
                    sender.sendMessage(Messages.tooBigBet)
                    return@menuItem
                }

                cscPlayer.changeGold(-actualDiff)
                val newBet = currentBet + actualDiff
                if (newBet == 0)
                    participant.bets.remove(sender.uniqueId)
                else
                    participant.bets[sender.uniqueId] = newBet

                phase.broadcastBetsInfo()
                openMenu(sender, target)
            }
        }

        val menu = buildSharedMenu {
            size = 27
            title = duelCommand.gameScheduler.gameTypeOptions.betOnTeamMessage(sender)
            createMenuHolder = ::DuelMenuHolder

            onClose {
                if (changedBet) {
                    it.sendMessage(Messages.placedBet)
                }
            }

            13 bindTo createBetInfoButton(sender, currentBet)
            duelCommand.addGoBackButton(this, sender)

            mapOf(2 to 50, 3 to 100, 4 to 200, 5 to 500, 6 to 1000).forEach { (slot, diff) ->
                if (cscPlayer.balance >= diff) {
                    slot bindTo createChangeBetButton(diff)
                }
                if (currentBet >= diff) {
                    (slot + 18) bindTo createChangeBetButton(-diff)
                }
            }
        }

        menu.openFor(sender)
    }

    private fun createBetInfoButton(player: Player, currentBet: Int) = unusedItem(
        material = Material.DOUBLE_PLANT,
        displayName = Messages.yourBet(player, currentBet),
    )

    private object Messages {
        val cannotBetInDuel = message0(
            russian = "${RED}Вы участвуете в дуэли, поэтому не можете делать ставки",
            english = "${RED}You cannot place bets because you participate in duel",
        )

        val betReturned = message0(
            russian = "${GREEN}Вам возвращена ставка на другого участника дуэли",
            english = "${GREEN}Your bet on another duel participant was returned to you",
        )

        val tooBigBet = message0(
            russian = "${RED}Вы не можете поставить больше половины своего баланса",
            english = "${RED}You cannot bet more than half of your balance",
        )

        val placedBet = message0(
            russian = "${GREEN}Вы успешно сделали свою ставку",
            english = "${GREEN}You successfully placed your bet",
        )

        val yourBet = message1<Int>(
            russian = { "${GREEN}Ваша ставка: $GOLD$it золота" },
            english = { "${GREEN}Your bet: $GOLD$it gold" },
        )
    }
}
