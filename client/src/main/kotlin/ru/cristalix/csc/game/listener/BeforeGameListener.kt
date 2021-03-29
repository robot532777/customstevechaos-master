package ru.cristalix.csc.game.listener

import me.stepbystep.api.*
import me.stepbystep.api.chat.GRAY
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.message0
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.*
import me.stepbystep.mgapi.client.event.GameStartEvent
import net.minecraft.server.v1_12_R1.NBTTagByte
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import ru.cristalix.core.party.IPartyService
import ru.cristalix.core.party.PartySnapshot
import ru.cristalix.csc.event.PlayersCreationEvent
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.asCSCPlayerOrNull
import ru.cristalix.csc.util.getOnlinePlayersWithOpenedMenu
import java.util.*

class BeforeGameListener(private val gameScheduler: GameScheduler) : Listener {
    private companion object {
        private const val ITEM_TAG = "chooseTeam"
    }

    private val maxPlayersOnTeam = gameScheduler.gameTypeOptions.maxPlayersOnTeam
    private val allowChooseTeam = gameScheduler.gameTypeOptions.allowChooseTeam

    private val playerTeams = hashMapOf<UUID, CSCTeam>()

    init {
        if (allowChooseTeam) {
            Bukkit.getOnlinePlayers().forEach { it.giveChooseTeamItem() }
        }
    }

    private class ChooseTeamMenuHolder(menu: Menu) : MenuHolder(menu)

    private fun Player.openChooseTeamMenu() {
        buildSharedMenu {
            size = 18
            title = Messages.teamSelection(this@openChooseTeamMenu)
            createMenuHolder = ::ChooseTeamMenuHolder

            CSCTeam.all().forEachIndexed { index, team ->
                val players = playerTeams.mapNotNull { (uuid, playerTeam) ->
                    if (playerTeam != team) return@mapNotNull null
                    Bukkit.getPlayer(uuid)
                }

                val stack = team.createDisplayStack(this@openChooseTeamMenu)
                stack.lore = players.map { "   $GRAY${it.name}" }
                index bindTo menuItem(stack) {
                    if (players.size < maxPlayersOnTeam) {
                        whoClicked.addToTeam(team)
                    }
                }
            }
        }.openFor(this)
    }

    @EventHandler
    fun PlayerJoinEvent.handle() {
        if (!allowChooseTeam) return

        player.giveChooseTeamItem()
        player.addPartyMemberToTeam()
    }

    @EventHandler
    fun PlayerQuitEvent.handle() {
        playerTeams -= player.uniqueId
    }

    @EventHandler
    fun PlayerInteractEvent.handle() {
        if (action.isLeftClick()) return
        if (hand != EquipmentSlot.HAND) return
        if (item?.asNMS()?.tag?.hasKey(ITEM_TAG) != true) return

        player.openChooseTeamMenu()
    }

    @EventHandler
    fun PlayersCreationEvent.handle() {
        CSCPlayer.getAllPlayers().forEach { it.updateTeam() }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun GameStartEvent.handle() {
        for (cscPlayer in CSCPlayer.getAllPlayers()) {
            if (cscPlayer.hasTeam()) continue
            if (!cscPlayer.isOnline()) continue
            val team = CSCTeam.all().minByOrNull { it.players.size } ?: error("Team list is empty")
            val freeTeam = when {
                team.players.size < gameScheduler.gameTypeOptions.maxPlayersOnTeam -> team
                else -> CSCTeam.createFakeTeam(gameScheduler)
            }
            cscPlayer.updateTeam(freeTeam)
        }

        CSCTeam.all().toList().forEach { it.unregisterIfNeed(false) }

        unregister()
        Bukkit.getOnlinePlayers().forEach {
            it.inventory.setItem(0, null)
        }
    }

    private fun Player.giveChooseTeamItem() {
        val chooseTeamItem = createNMSItem(
            material = Material.PAPER,
            displayName = Messages.chooseTeam(this),
            customTag = ItemTag(ITEM_TAG, NBTTagByte(1))
        )
        inventory.asNMS().setItem(0, chooseTeamItem)
    }

    private fun Player.addPartyMemberToTeam() {
        IPartyService.get().getPartyByMember(uniqueId).thenAccept {
            if (!it.isPresent) return@thenAccept

            val party = it.get()
            gameScheduler.plugin.runTask {
                addPartyMemberToTeam(party)
            }
        }
    }

    private fun Player.addPartyMemberToTeam(party: PartySnapshot) {
        val possibleTeammates = party.members.filter {
            it != uniqueId && it !in playerTeams
        }.mapNotNull(Bukkit::getPlayer)

        if (possibleTeammates.isEmpty()) return
        val mostEmptyTeam = CSCTeam.all().minByOrNull { it.countOfMembers() } ?: error("No teams are created")
        val leastCountOfMembers = mostEmptyTeam.countOfMembers()
        val freeMembersSlots = maxPlayersOnTeam - leastCountOfMembers - 1
        if (freeMembersSlots <= 0) return

        addToTeam(mostEmptyTeam)
        val partyMembersAdded = minOf(possibleTeammates.size, freeMembersSlots)
        for (i in 0 until partyMembersAdded) {
            possibleTeammates[i].addToTeam(mostEmptyTeam)
        }
    }

    private fun HumanEntity.addToTeam(team: CSCTeam) {
        playerTeams[uniqueId] = team
        asCSCPlayerOrNull()?.updateTeam()

        refreshAllMenu()
    }

    private fun refreshAllMenu() {
        getOnlinePlayersWithOpenedMenu<ChooseTeamMenuHolder>().forEach {
            it.openChooseTeamMenu()
        }
    }

    private fun CSCTeam.countOfMembers(): Int = playerTeams.values.count { it == this }

    private fun CSCPlayer.updateTeam() {
        if (!isOnline()) return

        val team = playerTeams[uuid] ?: return
        updateTeam(team)
    }

    private object Messages {
        val chooseTeam = message0(
            russian = "${GREEN}Выбрать команду",
            english = "${GREEN}Choose team",
        )

        val teamSelection = message0(
            russian = "Выбор команды",
            english = "Team selection",
        )
    }
}
