package ru.cristalix.csc.command

import me.stepbystep.api.command.RequirementIsOperator
import me.stepbystep.api.command.executesWrapped
import me.stepbystep.api.command.register
import org.bukkit.Bukkit
import org.bukkit.World
import ru.cristalix.core.command.CommandHelper

class SaveWorldsCommand {
    init {
        CommandHelper.literal("saveworlds")
            .description("Сохранить все миры")
            .requires(RequirementIsOperator())
            .executesWrapped {
                Bukkit.getWorlds().forEach(World::save)
            }
            .register()
    }
}
