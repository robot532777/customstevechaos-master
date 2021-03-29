package ru.cristalix.csc.game.listener.gameplay

import me.stepbystep.api.chat.*
import me.stepbystep.api.getOrCreateChatView
import me.stepbystep.api.getOrCreateTabView
import me.stepbystep.api.menu.*
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.DyeColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.cristalix.core.chat.ChatTextComponent
import ru.cristalix.core.chat.IChatService
import ru.cristalix.core.tab.ITabService
import ru.cristalix.core.tab.TabTextComponent
import ru.cristalix.core.text.TextFormat
import ru.cristalix.csc.event.PlayersCreationEvent
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.`class`.SelectedClass
import ru.cristalix.csc.packet.RecordPlayerClassPacket
import ru.cristalix.csc.player.CSCPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

class ClassListener(private val gameScheduler: GameScheduler) : Listener {

    private object Messages {
        val classSelection = message0(
            russian = "Выбор класса",
            english = "Class selection",
        )

        val selectClass = message0(
            russian = "${RED}Выберите класс",
            english = "${RED}Select class",
        )

        val successfullySelected = message0(
            russian = "${GREEN}Вы успешно выбрали класс",
            english = "${GREEN}You successfully selected class",
        )
    }

    @EventHandler
    fun PlayersCreationEvent.handle() {
        CSCPlayer.getLivingPlayers().forEach { cscPlayer ->
            getRandomClasses(cscPlayer).thenAccept {
                if (cscPlayer.isAlive) {
                    val player = cscPlayer.asBukkitPlayer()
                    cscPlayer.createClassesMenu(player, it).forceOpenFor(player)
                }
            }
        }
    }

    private fun getRandomClasses(player: CSCPlayer): CompletableFuture<List<SelectedClass>> {
        return player.loadClassData().thenApply { (hasMoreSelection, boughtClasses) ->
            val classAmount = if (hasMoreSelection) 5 else 3
            val unlockedClasses = boughtClasses + SelectedClass.VALUES.filter { it.requiredDonate == null }
            unlockedClasses.shuffled().take(classAmount)
        }
    }

    private fun CSCPlayer.createClassesMenu(player: Player, classes: List<SelectedClass>): Menu = buildSharedMenu {
        size = 9
        title = Messages.classSelection(player)

        fillGlass(DyeColor.GRAY)

        val slots = when (classes.size) {
            3 -> intArrayOf(2, 4, 6)
            5 -> intArrayOf(0, 2, 4, 6, 8)
            else -> error("Unknown classes size: $classes (expected 3 or 5, found: ${classes.size})")
        }
        check(classes.size == slots.size)

        classes.forEachIndexed { index, clazz ->
            val stack = clazz.wrapped.createDisplayItem(asBukkitPlayer())

            slots[index] bindTo menuItem(stack) {
                selectedClass = clazz
                clazz.onSelect(this@createClassesMenu)
                asBukkitPlayer().apply {
                    ITabService.get().update(this)
                    closeInventory()
                }

                val packetData = RecordPlayerClassPacket.ClientData(clazz.wrapped, classes.map { it.wrapped })
                val packet = RecordPlayerClassPacket(packetData)
                gameScheduler.actor.messageTransport.sendPacket(packet)
            }
        }

        onClose {
            if (selectedClass == null) {
                it.sendMessage(Messages.selectClass)
                createClassesMenu(player, classes).openSafeFor(it, gameScheduler.plugin)
            } else {
                it.sendMessage(Messages.successfullySelected)
            }
        }
    }

    private companion object {
        init {
            val suffixFunction = { uuid: UUID ->
                val cscPlayer = CSCPlayer.getOrNull(uuid)
                val selectedClass = cscPlayer?.selectedClass
                // TODO: choose localization basing on receiver
                val text = when (selectedClass != null && cscPlayer.hasTeam()) {
                    true -> {
                        val color = cscPlayer.gameScheduler.gameTypeOptions.getClassPrefixColor(cscPlayer)
                        val chatName = selectedClass.wrapped.chatName(cscPlayer.asBukkitPlayer())
                        "$GRAY[$color$chatName$GRAY]"
                    }
                    false -> ""
                }
                val baseComponents = TextComponent.fromLegacyText(text)
                CompletableFuture.completedFuture(baseComponents)
            }

            val tabSuffix = TabTextComponent(100, TextFormat.NONE, { true }, suffixFunction)
            ITabService.get().getOrCreateTabView().addSuffix(tabSuffix)

            val chatSuffix = ChatTextComponent(100, TextFormat.NONE, { _, _ -> true }, suffixFunction)
            IChatService.get().getOrCreateChatView().addSuffix(chatSuffix)
        }
    }
}
