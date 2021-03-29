package ru.cristalix.csc

import me.stepbystep.mgapi.common.game.GameTypeHandler

class CSCGameTypeHandler : GameTypeHandler {
    override val allTypes = listOf(CSCGameType.Solo, CSCGameType.Duo)
}
