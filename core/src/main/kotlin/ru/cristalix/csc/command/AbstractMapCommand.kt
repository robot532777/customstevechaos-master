package ru.cristalix.csc.command

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.command.*
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.map.AbstractMap
import java.util.*

abstract class AbstractMapCommand<T : AbstractMap>(
    name: String,
    description: String,
    protected val maps: MutableList<T>,
) {
    protected val selectedMaps = hashMapOf<UUID, T>()

    init {
        val commandBuilder = CommandHelper.literal(name)
            .description(description)
            .requires(RequirementIsOperator())

        commandBuilder.addChild("select", "Выбрать карту", intParameter("ID карты")) { ctx ->
            val id = ctx.getArgument<Int>("ID карты")
            val map = maps.find { it.id == id } ?: run {
                ctx.sender.sendMessage("${RED}Карты с ID = $GOLD$id ${RED}не существует")
                return@addChild
            }
            selectedMaps[ctx.source] = map
            ctx.sender.sendMessage("${GREEN}Вы успешно выбрали карту ${map.chatName}")
        }

        commandBuilder.addSelectedMapChild("delete", "Удалить выбранную карту") { map, ctx ->
            selectedMaps.values.removeIf { it == map }
            maps.remove(map)
            performDelete(map)
            ctx.sender.sendMessage("${RED}Вы успешно удалили карту ${map.chatName}")
        }

        @Suppress("LeakingThis")
        commandBuilder.configure()
        commandBuilder.register()
    }

    protected abstract fun UUIDLiteralArgumentBuilder.configure()

    protected inline fun UUIDLiteralArgumentBuilder.addSelectedMapChild(
        name: String,
        description: String,
        vararg params: Parameter,
        crossinline action: (map: T, ctx: UUIDCommandContext) -> Unit,
    ) {
        addChild(name, description, *params) { ctx ->
            val map = selectedMaps[ctx.source] ?: run {
                ctx.sender.sendMessage("${RED}Вы не выбрали карту")
                return@addChild
            }
            action(map, ctx)
        }
    }

    protected inline fun UUIDLiteralArgumentBuilder.addBuilderChild(
        name: String,
        description: String,
        vararg params: Parameter,
        crossinline action: (map: T, ctx: UUIDCommandContext) -> T?,
    ) {
        addSelectedMapChild(name, description, *params) { map, ctx ->
            val result = action(map, ctx)
            if (result != null) {
                selectedMaps.replaceAll { _, oldMap ->
                    if (oldMap.id == result.id) result else oldMap
                }
                maps.replaceAll { oldMap ->
                    if (oldMap.id == result.id) result else oldMap
                }
                ctx.sender.sendMessage("${GREEN}Вы успешно обновили карту ${result.chatName}")
            }
        }
    }

    protected abstract fun performDelete(map: T)

    protected val T.chatName
        get() = "$GOLD\"$name\""
}
