package ru.cristalix.csc

import me.stepbystep.mgapi.core.event.PlayerJoinGameEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.PlayerTable

class CoreListener(private val dataBase: DataBase) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockBreakEvent.handle() {
        isCancelled = false
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockPlaceEvent.handle() {
        isCancelled = false
    }
    
    @EventHandler
    fun PlayerJoinGameEvent.handle() {
        dataBase.asyncTransaction {
            with(PlayerTable) { insertIfAbsent(playerUUID) }
        }
    }
}
