package ru.cristalix.csc.command

import me.stepbystep.api.command.*
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import ru.cristalix.core.command.CommandHelper

class TpWorldCommand {
    init {
        CommandHelper.literal("tpworld")
            .requires(RequirementIsOperator())
            .thenWithParameters(stringParameter("мир")) {
                it.executesWrapped { ctx ->
                    val world = Bukkit.createWorld(WorldCreator(ctx.getArgument("мир")))
                    ctx.sender.teleport(world.spawnLocation)
                }
            }
            .register()
    }
}
