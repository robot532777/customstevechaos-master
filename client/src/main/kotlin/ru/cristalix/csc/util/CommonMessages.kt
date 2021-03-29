package ru.cristalix.csc.util

import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.message0
import me.stepbystep.api.chat.message1
import me.stepbystep.api.defaultMinutesWord
import me.stepbystep.api.defaultSecondsWord
import kotlin.math.absoluteValue

object CommonMessages {
    val balanceWord = message0(
        russian = "Баланс",
        english = "Balance",
    )

    val notInGame = message0(
        russian = "${RED}Вы не находитесь в игре",
        english = "${RED}You are not in game",
    )

    val notEnoughInventorySpace = message0(
        russian = "${RED}У вас недостаточно места в инвентаре",
        english = "${RED}You don't have enough space in inventory",
    )

    val notEnoughBalance = message0(
        russian = "${RED}У вас недостаточно золота на балансе",
        english = "${RED}You don't have enough gold",
    )

    val price = message0(
        russian = "Цена",
        english = "Price",
    )

    val localizedMinutesWord = message1(
        russian = defaultMinutesWord,
        english = { if (it.absoluteValue == 1) "minute" else "minutes" }
    )

    val localizedSecondsWord = message1(
        russian = defaultSecondsWord,
        english = { if (it.absoluteValue == 1) "second" else "seconds" }
    )

    val duelNotActive = message0(
        russian = "${RED}Сейчас не проходит дуэль",
        english = "${RED}Duel is not active right now",
    )

    val heals = message0(
        russian = "Исцеляет",
        english = "Heals",
    )

    val damage = message0(
        russian = "Урон",
        english = "Damage",
    )
}
