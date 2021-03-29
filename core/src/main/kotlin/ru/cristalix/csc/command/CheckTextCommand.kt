package ru.cristalix.csc.command

import io.netty.buffer.ByteBuf
import me.stepbystep.api.command.*
import me.stepbystep.api.sendPluginMessage
import org.apache.commons.io.IOUtils
import org.bukkit.ChatColor
import org.bukkit.plugin.Plugin
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.core.display.IDisplayService
import ru.cristalix.core.display.messages.JavaScriptMessage

class CheckTextCommand(private val plugin: Plugin) {
    init {
        val commandBuilder = CommandHelper.literal("checktext")
            .requires(RequirementIsOperator())
        val text = arrayListOf<String>()

        commandBuilder.addChild("receive", "Получить скрипт") {
            val duelScriptMessage = JavaScriptMessage(
                arrayOf(IOUtils.toString(plugin.getResource("duelscript.js")))
            )
            IDisplayService.get().sendScripts(it.source, duelScriptMessage)
        }

        fun ByteBuf.writeJSString(text: String) {
            writeInt(text.length)
            text.forEach { writeInt(it.toInt()) }
        }

        commandBuilder.addChild("add", "Добавить строку", stringParameter("text")) {
            text.add(ChatColor.translateAlternateColorCodes('-', it.getArgument("text")))
            it.sender.sendPluginMessage("stepbystep:duelinfo") {
                writeBoolean(true)
                writeInt(text.size)
                text.forEach(::writeJSString)
            }
        }

        commandBuilder.addChild("disable", "Выключить текст") {
            text.clear()
            it.sender.sendPluginMessage("stepbystep:duelinfo") {
                writeBoolean(false)
            }
        }

        commandBuilder.register()
    }
}
