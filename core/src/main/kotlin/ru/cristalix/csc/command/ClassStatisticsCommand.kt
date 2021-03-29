package ru.cristalix.csc.command

import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.command.addChild
import me.stepbystep.api.command.register
import me.stepbystep.api.command.sender
import me.stepbystep.api.wordForNum
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.ClassSelectionTable
import ru.cristalix.csc.shop.item.CSCClass
import kotlin.math.roundToInt

class ClassStatisticsCommand(dataBase: DataBase) {
    init {
        val commandBuilder = CommandHelper.literal("cscclass")

        commandBuilder.addChild("delete", "Удалить данные из таблицы") {
            dataBase.asyncTransaction {
                ClassSelectionTable.deleteAll()
                it.sender.sendMessage("${GREEN}Таблица успешно очищена")
            }
        }

        commandBuilder.addChild("search", "Получить данные по классам") {
            dataBase.asyncTransaction {
                val sender = it.sender
                val allRows = ClassSelectionTable.selectAll().map { row ->
                    val selectedClass = row[ClassSelectionTable.selectedClass]
                    val allClasses = row[ClassSelectionTable.allClasses].split(";").map(CSCClass::valueOf)

                    selectedClass to allClasses
                }

                sender.sendMessage("${GREEN}Данные по классам:")
                for (clazz in CSCClass.values()) {
                    val acceptedCount = allRows.count { it.first == clazz }
                    val suggestedCount = allRows.count { clazz in it.second }
                    val selectionRate = when (suggestedCount) {
                        0 -> 0.0
                        else -> acceptedCount * 100.0 / suggestedCount
                    }.roundToThousands()

                    val message = buildString {
                        append("   ").append(clazz.displayName(sender)).append(": ").append(GREEN)
                        append("выбран ").append(acceptedCount).append(' ').append(acceptedCount.countWord())
                        append(", предложен ").append(suggestedCount).append(' ').append(suggestedCount.countWord())
                        append(", частота выбора = ").append(selectionRate).append('%')
                    }

                    sender.sendMessage(message)
                }
            }
        }
        commandBuilder.register()
    }

    private fun Int.countWord(): String = wordForNum("раз", "раза", "раз")

    private fun Double.roundToThousands(): Double = (this * 1000.0).roundToInt() / 1000.0
}
