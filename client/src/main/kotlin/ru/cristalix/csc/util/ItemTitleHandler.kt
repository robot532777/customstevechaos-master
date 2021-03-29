package ru.cristalix.csc.util

import io.netty.buffer.Unpooled
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.loadJavaScriptResource
import me.stepbystep.api.runDelayed
import net.minecraft.server.v1_12_R1.PacketDataSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import ru.cristalix.core.display.IDisplayService
import ru.cristalix.core.internal.BukkitInternals
import ru.cristalix.core.util.UtilNetty
import kotlin.time.seconds

class ItemTitleHandler(private val plugin: Plugin) : Listener {
    private val scriptMessage = plugin.loadJavaScriptResource("itemtitle.js")

    @EventHandler
    fun PlayerJoinEvent.handle() {
        IDisplayService.get().sendScripts(player.uniqueId, scriptMessage)
    }

    fun sendTitle(player: Player, stack: NMSItemStack, title: String, subTitle: String = "") {
        plugin.runDelayed(0.5.seconds) {
            val dataSerializer = PacketDataSerializer(Unpooled.buffer())
            dataSerializer.a(stack)
            UtilNetty.writeString(dataSerializer, title)
            UtilNetty.writeString(dataSerializer, subTitle)

            BukkitInternals.internals().sendPluginMessage(player, "itemtitle", dataSerializer)
        }
    }
}
