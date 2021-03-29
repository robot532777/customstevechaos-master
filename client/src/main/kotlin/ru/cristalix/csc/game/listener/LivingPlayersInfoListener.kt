package ru.cristalix.csc.game.listener

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.amount
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.MenuBuilder
import me.stepbystep.api.menu.buildPagedMenu
import me.stepbystep.api.menu.unusedItem
import me.stepbystep.mgapi.client.event.GameStartEvent
import net.minecraft.server.v1_12_R1.NBTTagByte
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.GameTypeOptions
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.createHeadItem

class LivingPlayersInfoListener(private val gameScheduler: GameScheduler) : Listener {
    private companion object {
        private const val PLAYER_INFO_TAG = "playerInfo"
        private const val ITEM_SLOT = 8
    }

    private object Messages {
        val players = message0(
            russian = "${GREEN}Игроки",
            english = "${GREEN}Players",
        )

        val playerInfo = message0(
            russian = "Информация об игроках",
            english = "Players information",
        )

        val curseLevel = message1<Int>(
            russian = { "${GRAY}Уровень проклятья: $GOLD$it" },
            english = { "${GRAY}Curse level: $GOLD$it" },
        )
    }

    private val teamComparator = compareByDescending<CSCTeam> { it.livingPlayers.size }
        .thenByDescending { it.livesLeft }

    @EventHandler(priority = EventPriority.HIGH)
    fun GameStartEvent.handle() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val item = createNMSItem(
                material = Material.COMPASS,
                displayName = Messages.players(player),
                customTag = ItemTag(PLAYER_INFO_TAG, NBTTagByte(1))
            )
            player.asNMS().inventory.setItem(ITEM_SLOT, item)
        }
    }

    @EventHandler
    fun InventoryClickEvent.handle() {
        if (clickedInventory !is PlayerInventory) return
        if (currentItem.isPlayerInfoItem() || hotbarButton == ITEM_SLOT) {
            cancel()
        }
    }

    @EventHandler
    fun PlayerDropItemEvent.handle() {
        if (itemDrop.itemStack.isPlayerInfoItem()) {
            cancel()
        }
    }

    @EventHandler
    fun PlayerInteractEvent.handle() {
        if (hand != EquipmentSlot.HAND) return
        if (action.isLeftClick()) return
        if (item?.isPlayerInfoItem() != true) return

        val player = player
        Bukkit.getScheduler().runTaskAsynchronously(gameScheduler.plugin) {
            val aliveTeams = CSCTeam.allAlive().sortedWith(teamComparator)
            val teamsOnPage = gameScheduler.gameTypeOptions.teamsOnCompassPage
            val maxPage = aliveTeams.lastIndex / teamsOnPage

            val menu = buildPagedMenu(true, maxPage) {
                renderTeams(it, teamsOnPage, player)
            }

            // do not open menu asynchronously
            gameScheduler.plugin.runTask {
                menu.openFor(player)
            }
        }
    }

    private fun MenuBuilder.renderTeams(page: Int, teamsOnPage: Int, player: Player) {
        val aliveTeams = CSCTeam.allAlive().sortedWith(teamComparator)
        val endTeamIndex = ((page + 1) * teamsOnPage).coerceAtMost(aliveTeams.size)
        val shownTeams = aliveTeams.subList(page * teamsOnPage, endTeamIndex)

        shownTeams.forEachIndexed { teamIndex, team ->
            val livingPlayers = team.livingPlayers

            val slotDiff = when (gameScheduler.gameTypeOptions) {
                GameTypeOptions.Solo -> 0
                else -> {
                    teamIndex bindTo unusedItem(team.createDisplayStack(player))
                    9
                }
            }

            for ((index, cscPlayer) in livingPlayers.withIndex()) {
                val bukkitPlayer = cscPlayer.asBukkitPlayerOrNull() ?: continue
                val stack = bukkitPlayer.createHeadItem()
                stack.amount = cscPlayer.livesLeft
                stack.lore = mutableListOf(Messages.curseLevel(player, cscPlayer.curse))

                val slot = teamIndex + index * 9 + slotDiff
                slot bindTo unusedItem(stack)
            }
        }
    }

    private fun ItemStack.isPlayerInfoItem(): Boolean =
        asNMS().tag?.getBoolean(PLAYER_INFO_TAG) == true
}
