package ru.cristalix.csc.game

import com.google.gson.JsonArray
import io.netty.buffer.Unpooled
import me.stepbystep.api.loadJavaScriptResource
import me.stepbystep.api.runDelayed
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.cristalix.core.display.IDisplayService
import ru.cristalix.core.internal.BukkitInternals
import ru.cristalix.core.util.UtilNetty
import ru.cristalix.csc.player.CSCPlayer
import kotlin.time.seconds

sealed class TabHandler(
    scriptName: String?,
    private val gameScheduler: GameScheduler,
) {

    private val script = scriptName?.let(gameScheduler.plugin::loadJavaScriptResource)

    fun broadcastScript() {
        Bukkit.getOnlinePlayers().forEach(::sendScript)
    }

    fun sendScript(player: Player) {
        if (script == null) return

        IDisplayService.get().sendScripts(player.uniqueId, script)

        // make sure script reaches player
        repeat(5) {
            gameScheduler.plugin.runDelayed((2 * it).seconds) {
                sendTeams(player)
            }
        }
    }

    abstract fun sendTeams(player: Player)
    abstract fun updatePlayer(cscPlayer: CSCPlayer)

    class Solo(gameScheduler: GameScheduler) : TabHandler("solotab.bundle.js", gameScheduler) {
        override fun sendTeams(player: Player) {
            val allPlayers = JsonArray()
            val buf = Unpooled.buffer()

            CSCPlayer.getAllPlayers().forEach { allPlayers.add(it.createTabDescriptor()) }
            UtilNetty.writeString(buf, allPlayers.toString())
            BukkitInternals.internals().sendPluginMessage(player, "csc:tab-create", buf)
        }

        override fun updatePlayer(cscPlayer: CSCPlayer) {
            val playerDescriptor = cscPlayer.createTabDescriptor().toString()
            Bukkit.getOnlinePlayers().forEach {
                val buf = Unpooled.buffer()
                UtilNetty.writeString(buf, playerDescriptor)
                BukkitInternals.internals().sendPluginMessage(it, "csc:tab-updateplayer", buf)
            }
        }
    }

    class Team(gameScheduler: GameScheduler) : TabHandler(null, gameScheduler) {
        override fun sendTeams(player: Player) {
            // TODO
        }

        override fun updatePlayer(cscPlayer: CSCPlayer) {
            // TODO
        }
    }
}
