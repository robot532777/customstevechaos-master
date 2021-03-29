package ru.cristalix.csc.command

import me.stepbystep.api.command.*
import me.stepbystep.mgapi.common.ServerSide
import me.stepbystep.mgapi.core.CoreActor
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.CustomSteveChaosCore

class SetSaleCommand(
    private val actor: CoreActor,
    private val plugin: CustomSteveChaosCore,
) {
    companion object {
        const val CONFIG_KEY = "currentSale"
    }

    init {
        CommandHelper.literal("setsale")
            .requires(RequirementIsOperator())
            .description("Установить скидку на все донаты")
            .apply {
                thenWithParameters(uIntParameter("процент скидки")) {
                    it.executesWrapped { ctx ->
                        actor.plugin.config.set(CONFIG_KEY, ctx.getArgument("процент скидки"))
                        actor.plugin.saveConfig()
                        broadcastSale()
                    }
                }
            }
            .register()
    }

    private fun broadcastSale() {
        actor.messageTransport.broadcastPacket(plugin.createSalePacket(), ServerSide.Lobby)
    }
}
