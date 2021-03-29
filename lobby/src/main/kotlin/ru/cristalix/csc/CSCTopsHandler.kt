package ru.cristalix.csc

import me.stepbystep.api.inTicksLong
import me.stepbystep.api.registerServiceIfAbsent
import me.stepbystep.api.top.CristalixTop
import me.stepbystep.api.top.TopColumn
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import ru.cristalix.core.render.BukkitRenderService
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.PlayerTable
import kotlin.time.minutes

class CSCTopsHandler(
    private val plugin: Plugin,
    private val dataBase: DataBase,
) {
    init {
        registerServiceIfAbsent { BukkitRenderService(Bukkit.getServer()) }

        val allTops = listOf(soloRatingTop(), duoRatingTop(), waveTop())
        val delay = 5.minutes.inTicksLong

        allTops.forEach { top ->
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, top::refresh, delay, delay)
        }
    }

    private fun soloRatingTop() = CristalixTop(
        selectQuery = PlayerTable.selectAll().orderBy(PlayerTable.soloRating, SortOrder.DESC),
        dataBase = dataBase,
        columns = arrayOf(TopColumn.Nickname(PlayerTable.uuid), TopColumn.Exposed("Рейтинг", PlayerTable.soloRating)),
        name = "Топ по одиночному рейтингу",
        location = Location(Bukkit.getWorld("world"), -766.0, 167.0, 304.0, -90f, 0f),
        widthBlocks = 5.0,
    )

    private fun duoRatingTop() = CristalixTop(
        selectQuery = PlayerTable.selectAll().orderBy(PlayerTable.duoRating, SortOrder.DESC),
        dataBase = dataBase,
        columns = arrayOf(TopColumn.Nickname(PlayerTable.uuid), TopColumn.Exposed("Рейтинг", PlayerTable.duoRating)),
        name = "Топ по двойному рейтингу",
        location = Location(Bukkit.getWorld("world"), -766.0, 167.0, 296.0, -90f, 0f),
        widthBlocks = 5.0,
    )

    private fun waveTop() = CristalixTop(
        selectQuery = PlayerTable.selectAll().orderBy(PlayerTable.maxWave, SortOrder.DESC),
        dataBase = dataBase,
        columns = arrayOf(TopColumn.Nickname(PlayerTable.uuid), TopColumn.Exposed("Волна", PlayerTable.maxWave)),
        name = "Топ по макс. волне",
        location = Location(Bukkit.getWorld("world"), -749.0, 167.0, 291.0, 90f, 0f),
        widthBlocks = 5.0,
    )
}
