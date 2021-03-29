package ru.cristalix.csc.game.runnable

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import me.stepbystep.api.asNMS
import me.stepbystep.api.cancel
import me.stepbystep.api.register
import me.stepbystep.api.unregister
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable

class HidePlayersRunnable(private val plugin: Plugin) : BukkitRunnable(), Listener {
    private val hiddenPlayers = hashSetOf<Int>()

    init {
        val packetTypes = PacketType.Play.Server.getInstance().filter {
            it.name().contains("ENTITY", ignoreCase = true)
        } - PacketType.Play.Server.PLAYER_INFO

        val packetAdapter = object : PacketAdapter(plugin, packetTypes) {
            override fun onPacketSending(event: PacketEvent) {
                val integers = event.packet.integers
                if (integers.size() < 1) return
                if (integers.read(0) !in hiddenPlayers) return

                event.cancel()
            }
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(packetAdapter)
    }

    fun start() = apply {
        runTaskTimer(plugin, 0, 10)
        register(plugin)
    }

    @EventHandler
    fun PlayerTeleportEvent.handle() {
        hiddenPlayers -= player.entityId
    }

    override fun run() {
        val npcRegistry = CitizensAPI.getNPCRegistry()

        processPlayers { player ->
            val location = player.location
            npcRegistry.any { npc ->
                val storedLocation = npc.storedLocation
                location.world == storedLocation.world && location.distanceSquared(storedLocation) <= 9
            }
        }
    }

    override fun cancel() {
        super.cancel()

        processPlayers { false }
        unregister()
    }

    private inline fun processPlayers(shouldHide: (Player) -> Boolean) {
        forEachDifferentPlayer { player ->
            // if we swap 'player' and 'it' below, it doesn't work at all
            val nmsPlayer = player.asNMS()
            if (shouldHide(player)) {
                hiddenPlayers += player.entityId
                forEachDifferentPlayer(player) { nmsPlayer.tracker.clear(it.asNMS()) }
            } else {
                hiddenPlayers -= player.entityId
                forEachDifferentPlayer(player) { nmsPlayer.tracker.updatePlayer(it.asNMS()) }
            }
        }
    }

    private inline fun forEachDifferentPlayer(current: Player? = null, action: (Player) -> Unit) {
        Bukkit.getOnlinePlayers().forEach {
            if (it != current) {
                action(it)
            }
        }
    }
}
