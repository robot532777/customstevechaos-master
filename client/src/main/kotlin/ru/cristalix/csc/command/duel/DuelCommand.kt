package ru.cristalix.csc.command.duel

import me.stepbystep.api.chat.*
import me.stepbystep.api.command.UUIDCommandContext
import me.stepbystep.api.command.executesWrapped
import me.stepbystep.api.command.register
import me.stepbystep.api.command.sender
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.lore
import me.stepbystep.api.menu.*
import net.minecraft.server.v1_12_R1.NBTTagString
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.phase.DuelPhase
import ru.cristalix.csc.phase.SpawnMobsPhase
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.createHeadItem
import ru.cristalix.csc.util.duelPhase

class DuelCommand(val gameScheduler: GameScheduler) {

    private val leftClickHandler = DuelBetHandler(this)
    private val rightClickHandler = DuelInfoHandler(this)

    init {
        CommandHelper.literal(NAME)
            .description("Поставить ставку на дуэлянтов и посмотреть их инвентарь")
            .executesWrapped(::execute)
            .register()
    }

    private fun execute(ctx: UUIDCommandContext) {
        execute(ctx.sender)
    }

    private fun execute(sender: Player) {
        val phase = gameScheduler.duelPhase

        if (phase == null) {
            sender.sendMessage(CommonMessages.duelNotActive)
            return
        }

        if (isWaveStarted) {
            sender.sendMessage(Messages.waveStarted)
            return
        }

        if (phase.stage == DuelStage.Finished) {
            sender.sendMessage(Messages.duelFinished)
            return
        }

        openSelectPlayerMenu(sender)
    }

    private fun openSelectPlayerMenu(sender: Player) {
        fun createSelectItem(player: CSCPlayer) = menuItem(player.createHeadItemWithInfo()) {
            when {
                click.isLeftClick -> leftClickHandler.openMenu(sender, player.asBukkitPlayer())
                click.isRightClick -> rightClickHandler.openMenu(sender, player.asBukkitPlayer())
            }
        }

        fun MenuBuilder.bindSelectItems(participant: DuelPhase.Participant, initialSlot: Int, slotDirection: Int) {
            participant.team.livingPlayers.forEachIndexed { index, cscPlayer ->
                (initialSlot + index * slotDirection) bindTo createSelectItem(cscPlayer)
            }
        }

        val menu = buildSharedMenu {
            size = 27
            title = gameScheduler.gameTypeOptions.selectDuelTeamMessage(sender)
            createMenuHolder = ::DuelMenuHolder

            val phase = requireDuelPhase()

            4 bindTo phase.createMapItem()

            bindSelectItems(phase.first, 11, -1)
            bindSelectItems(phase.second, 15, 1)
        }

        menu.openFor(sender)
    }

    private fun CSCPlayer.createHeadItemWithInfo(): NMSItemStack =
        asBukkitPlayer().createHeadItem().also {
            it.lore = mutableListOf(
                "${WHITE}Побед: $GREEN$duelWins",
                "${WHITE}Поражений: $RED$duelLoses",
                "",
                "${WHITE}Доп. урон: $GOLD$additionalDamage",
                "${WHITE}Доп. ХП: $GOLD${additionalHealthFromBook / 2} ${RED}❤",
                "${WHITE}Доп. реген: $GOLD${additionalRegeneration / 2} ${RED}❤",
                "",
                "${GRAY}ЛКМ, чтобы сделать ставку",
                "${GRAY}ПКМ, чтобы посмотреть инвентарь",
            )
        }

    private fun DuelPhase.createMapItem(): MenuItem {
        val (material, data) = map.displayMaterialData
        return unusedItem(
            material = material,
            data = data,
            displayName = "$GREEN${map.name}",
        )
    }

    private val isWaveStarted: Boolean
        get() = gameScheduler.duelPhase?.stage?.isStarted == true || gameScheduler.currentPhase is SpawnMobsPhase

    fun requireDuelPhase(): DuelPhase =
        gameScheduler.duelPhase ?: error("Duel phase is not active: ${gameScheduler.currentPhase}")

    fun addGoBackButton(builder: MenuBuilder, player: Player) {
        val stack = createNMSItem(
            material = Material.CLAY_BALL,
            displayName = "${RED}Назад",
            customTag = ItemTag("other", NBTTagString("arrow_left"))
        )

        with(builder) {
            0 bindTo menuItem(stack) {
                execute(player)
            }
        }
    }

    companion object {
        const val NAME = "duelsettings"
        const val NAME_WITH_SLASH = "/$NAME"
    }

    private object Messages {
        val waveStarted = message0(
            russian = "${RED}Волна уже началась",
            english = "${RED}Wave already started",
        )

        val duelFinished = message0(
            russian = "${RED}Дуэль уже закончилась",
            english = "${RED}Duel is already finished",
        )
    }
}
