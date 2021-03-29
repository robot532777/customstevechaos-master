package ru.cristalix.csc.player

import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.chat.map
import me.stepbystep.api.chat.message0
import ru.cristalix.core.formatting.Color
import ru.cristalix.core.formatting.Colors

enum class TeamColor(
    wrapped: Color,
    val displayName: PMessage0,
) {
    Purple(
        wrapped = Color.PURPLE,
        displayName = message0(
            russian = "Фиолетовые рыбки",
            english = "Purple fish",
        ),
    ),
    Yellow(
        wrapped = Color.YELLOW,
        displayName = message0(
            russian = "Желтые мопсы",
            english = "Yellow pugs",
        ),
    ),
    White(
        wrapped = Color.WHITE,
        displayName = message0(
            russian = "Белые пингвины",
            english = "White penguins",
        ),
    ),
    Green(
        wrapped = Color.GREEN,
        displayName = message0(
            russian = "Зеленые хамелеоны",
            english = "Green chameleons",
        ),
    ),
    Blue(
        wrapped = Color.BLUE,
        displayName = message0(
            russian = "Синие киты",
            english = "Blue whales",
        ),
    ),
    Black(
        wrapped = Color.BLACK,
        displayName = message0(
            russian = "Черные котики",
            english = "Black cats",
        ),
    ),
    Brown(
        wrapped = Color.BROWN,
        displayName = message0(
            russian = "Коричневые муравьеды",
            english = "Brown anteaters",
        ),
    ) {
        override val chatColor: String = Colors.custom(181, 108, 21)
    },
    Gray(
        wrapped = Color.GRAY,
        displayName = message0(
            russian = "Серые мышки",
            english = "Gray mice",
        ),
    ),
//    Aqua(
//        wrapped = Color.AQUA,
//        displayName = message0(
//            russian = "Голубые призраки",
//            english = "Aqua ghosts",
//        )
//    ),
//    Orange(
//        wrapped = Color.ORANGE,
//        displayName = message0(
//            russian = "Оранжевые львы",
//            english = "Orange lions",
//        )
//    ),
    ;

    val woolData = wrapped.woolData.toByte()
    open val chatColor: String = wrapped.chatFormat

    val coloredName: PMessage0 by lazy { // overridden chatColor can return null
        displayName.map { "$chatColor$it" }
    }
}
