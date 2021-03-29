package ru.cristalix.csc.shop.item

import me.stepbystep.api.asCraftMirror
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.NMSItemStack
import org.bukkit.Material
import org.bukkit.SkullType
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.util.mixNewYearColors

enum class CSCMessagePack(
    override val displayName: PMessage0,
    override val material: Material = Material.SKULL_ITEM,
    override val data: Byte = SkullType.PLAYER.ordinal.toByte(),
    private val modifyItem: NMSItemStack.() -> NMSItemStack = { this }
) : CSCItem {

    Default(
        displayName = message0(
            "${GRAY}Сообщения: ${GREEN}Стандарт",
            "${GRAY}Messages: ${GREEN}Default",
        ),
        material = Material.BOOK,
        data = 0,
    ) {
        override val lostGame = message1<String>(
            { "${RED}Игрок $GOLD$it ${RED}проиграл!" },
            { "${RED}Player $GOLD$it ${RED}lost!" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "${RED}Игрок $GOLD$name ${RED}потерял жизнь! Осталось: $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "${RED}Player $GOLD$name ${RED}lost life! Remaining: $GREEN$left$GRAY/$GOLD$max" }
        )

        override val willSummonWitch = message1<String>(
            { "${RED}Игрок $GOLD$it ${RED}призовет ведьм на следующей волне!" },
            { "${RED}Player $GOLD$it ${RED}will summon witches on the next wave!" },
        )

        override val summonedWitch = message1<String>(
            { "${RED}Игрок $GOLD$it ${RED}призвал ведьм!" },
            { "${RED}Player $GOLD$it ${RED}summoned witches!" },
        )

        override val completedWave = message1<String>(
            { "${GREEN}Игрок $GOLD$it ${GREEN}завершил волну" },
            { "${GREEN}Player $GOLD$it ${GREEN}completed wave" },
        )

        override val wonDuel = message1<String>(
            { "${GREEN}Дуэль закончилась победой игрока $GOLD$it" },
            { "${GREEN}The duel ended with the victory of $GOLD$it" },
        )
    },
    Demaster(
        displayName = message0(
            "${GRAY}Сообщения: ${RED}Demaster",
            "${GRAY}Messages: ${RED}Demaster",
        ),
        modifyItem = { makePlayerHead("cscskindemaster") }
    ) {
        override val lostGame = message1<String>(
            { "$GOLD$it ${RED}был забран в иной мир Демастером!" },
            { "$GOLD$it ${RED}was taken to another world by Demaster!" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "$GOLD$name ${RED}потерял жизнь. Демастер дарует ему еще 1, осталось: $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "$GOLD$name ${RED}lost life. Demaster grants him 1 more, remaining: $GREEN$left$GRAY/$GOLD$max" },
        )

        override val willSummonWitch = message1<String>(
            { "$GOLD$it ${RED}с помощью Демастера призовет ведьм на следующей волне!" },
            { "$GOLD$it ${RED}with Demaster help will summon witches on the next wave!" },
        )

        override val summonedWitch = message1<String>(
            { "$GOLD$it ${RED}призвал ведьм с помощью Демастера!" },
            { "$GOLD$it ${RED}summoned witches with Demaster help!" },
        )

        override val completedWave = message1<String>(
            { "$GOLD$it ${GREEN}завершил волну. Ради Демастера!" },
            { "$GOLD$it ${GREEN}completed wave. For Demaster's sake!" },
        )

        override val wonDuel = message1<String>(
            { "${GREEN}Ученик Демастера $GOLD$it ${GREEN}победил в дуэли!" },
            { "${GREEN}Demaster's student $GOLD$it ${GREEN}won the duel!" },
        )
    },
    Ukraine(
        displayName = message0(
            "${GRAY}Сообщения: ${YELLOW}Украина",
            "${GRAY}Messages: ${YELLOW}Ukraine",
        ),
        modifyItem = { makePlayerHead("cscskinukraine1") }
    ) {
        override val lostGame = message1<String>(
            { "$GOLD$it ${RED}завершив гру невдачою!" },
            { "$GOLD$it ${RED}завершив гру невдачою!" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "${RED}Трясця тобі! $GOLD$name ${RED}втратив життя. Залишилось $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "${RED}Трясця тобі! $GOLD$name ${RED}втратив життя. Залишилось $GREEN$left$GRAY/$GOLD$max" },
        )

        override val willSummonWitch = message1<String>(
            { "$GOLD$it ${RED}визвав Бабу Ягу з проклятого лісу! Вони будуть наступну хвилю." },
            { "$GOLD$it ${RED}визвав Бабу Ягу з проклятого лісу! Вони будуть наступну хвилю." },
        )

        override val summonedWitch = message1<String>(
            { "$GOLD$it ${RED}визвав Бабу Ягу. Стережись!" },
            { "$GOLD$it ${RED}визвав Бабу Ягу. Стережись!" },
        )

        override val completedWave = message1<String>(
            { "$GOLD$it ${GREEN}завершив хвилю, було легко!" },
            { "$GOLD$it ${GREEN}завершив хвилю, було легко!" },
        )

        override val wonDuel = message1<String>(
            { "$GOLD$it ${GREEN}випотрошив свого противника на дуелі!" },
            { "$GOLD$it ${GREEN}випотрошив свого противника на дуелі!" },
        )
    },
    Halloween(
        displayName = message0(
            "${GRAY}Сообщения: ${GOLD}Хэллоуин",
            "${GRAY}Messages: ${GOLD}Halloween",
        ),
        modifyItem = { makePlayerHead("cscskinhalloween") }
    ) {
        override val lostGame = message1<String>(
            { "$GOLD$it ${RED}покинул наш мир" },
            { "$GOLD$it ${RED}left our world" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "$GOLD$name ${RED}стал ближе на шаг к миру мертвым, осталось шагов: $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "$GOLD$name ${RED}became one step closer to the world of the dead, steps remaining: $GREEN$left$GRAY/$GOLD$max" },
        )

        override val willSummonWitch = message1<String>(
            { "${RED}В эту ночь, игрок $GOLD$it$RED, с помощью черной магии, призвал ведьм в следующей волне!" },
            { "${RED}On this night, player $GOLD$it$RED, using black magic, summoned witches on the next wave!" },
        )

        override val summonedWitch = message1<String>(
            { "$GOLD$it ${RED}призвал ведьм!" },
            { "$GOLD$it ${RED}summoned witches!" },
        )

        override val completedWave = message1<String>(
            { "$GOLD$it ${GREEN}с легкостью справился с тварями!" },
            { "$GOLD$it ${GREEN}easily coped with the creatures!" },
        )

        override val wonDuel = message1<String>(
            { "$GOLD$it ${GREEN}пустил на кишки своего врага в дуэли!" },
            { "$GOLD$it ${GREEN}ripped his enemy into the guts in a duel!" },
        )
    },
    DemonHunter(
        displayName = message0(
            russian = "${GRAY}Сообщения: ${DARK_PURPLE}Охотник на демонов",
            english = "${GRAY}Messages: ${DARK_PURPLE}Demon hunter",
        ),
        modifyItem = { makePlayerHead("cscskindemon") },
    ) {
        override val lostGame = message1<String>(
            { "$GOLD$it ${RED}покинул сражение" },
            { "$GOLD$it ${RED}left the battle" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "$GOLD$name ${RED}потерял жизнь. Демоны забрали одну из жизней, осталось: $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "$GOLD$name ${RED}lost life. Demons took one life, remaining: $GREEN$left$GRAY/$GOLD$max" },
        )

        override val willSummonWitch = message1<String>(
            { "${RED}Демонская магия помогла игроку $GOLD$it ${RED}призвать ведьм в следующей волне!" },
            { "${RED}Demonic magic helped player $GOLD$it ${RED}summon witches on the next wave!" },
        )

        override val summonedWitch = message1<String>(
            { "$GOLD$it ${RED}призвал ведьм!" },
            { "$GOLD$it ${RED}summoned witches!" },
        )

        override val completedWave = message1<String>(
            { "$GOLD$it ${GREEN}победил демонов на этой волне!" },
            { "$GOLD$it ${GREEN}defeated demons on this wave!" },
        )

        override val wonDuel = message1<String>(
            { "${GREEN}Охотник на демонов $GOLD$it ${GREEN}вновь доказал свою силу на дуэли!" },
            { "${GREEN}Demon hunter $GOLD$it ${GREEN}once again proved his strength in a duel!" },
        )
    },
    NewYear(
        displayName = message0(
            russian = "${GRAY}Сообщения: ${"Новый Год".mixNewYearColors()}",
            english = "${GRAY}Messages: ${"New Year".mixNewYearColors()}",
        ),
        modifyItem = { makePlayerHead("cscskinnewyear") },
    ) {
        override val lostGame = message1<String>(
            { "$GOLD$it ${RED}пропускает Новый Год в этом году" },
            { "$GOLD$it ${RED}skips New Year this year" },
        )

        override val lostLife = message3<String, Int, Int>(
            { name, left, max -> "$GOLD$name ${RED}ушел встречать Новый Год, осталось жизней: $GREEN$left$GRAY/$GOLD$max" },
            { name, left, max -> "$GOLD$name ${RED}left to celebrate New Year, lives remaining: $GREEN$left$GRAY/$GOLD$max" },
        )

        override val willSummonWitch = message1<String>(
            { "${RED}Игрок $GOLD$it ${RED}призывает Новогоднюю ведьму!" },
            { "${RED}Player $GOLD$it ${RED}is summoning New Year witch!" },
        )

        override val summonedWitch = message1<String>(
            { "$GOLD$it ${RED}призвал Новогоднюю ведьму!" },
            { "$GOLD$it ${RED}summoned New Year witch!" },
        )

        override val completedWave = message1<String>(
            { "${GREEN}Игрок $GOLD$it ${GREEN}прикончил всех монстров!" },
            { "${GREEN}Player $GOLD$it ${GREEN}finished off all the monsters!" },
        )

        override val wonDuel = message1<String>(
            { "${GREEN}Враг игрока $GOLD$it ${GREEN}не получит подарки на Новый Год, так как проиграл дуэль!" },
            { "$GOLD$it$GREEN's enemy won't receive gifts for New Year, because he lost the duel!" },
        )
    }
    ;

    abstract val lostGame: PMessage1<String>
    abstract val lostLife: PMessage3<String, Int, Int>
    abstract val willSummonWitch: PMessage1<String>
    abstract val summonedWitch: PMessage1<String>
    abstract val completedWave: PMessage1<String>
    abstract val wonDuel: PMessage1<String>

    override val description get() = emptyList<PMessage0>()

    override val requiredDonate by lazy {
        CSCDonate.VALUES.filterIsInstance<CSCDonate.MessagePackDonate>().find { it.messagePack == this } as CSCDonate?
    }

    override fun createDisplayItem(player: Player) = super.createDisplayItem(player).modifyItem()

    companion object {
        @Suppress("DEPRECATION")
        private fun NMSItemStack.makePlayerHead(ownerName: String): NMSItemStack = apply {
            val craftMirror = asCraftMirror()
            val meta = craftMirror.itemMeta as SkullMeta
            meta.owner = ownerName
            meta.playerProfile
            craftMirror.itemMeta = meta
        }

        fun byName(name: String): CSCMessagePack = values().first { it.name == name }
    }
}
