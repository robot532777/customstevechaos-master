package ru.cristalix.csc.game.customitem

import com.destroystokyo.paper.profile.PlayerProfile
import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.*
import net.minecraft.server.v1_12_R1.DamageSource
import net.minecraft.server.v1_12_R1.NBTTagInt
import net.minecraft.server.v1_12_R1.NBTTagString
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.SpawnEggMeta
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import ru.cristalix.csc.CustomSteveChaos
import ru.cristalix.csc.entity.CSCEntityIronGolem
import ru.cristalix.csc.event.CSCGameStartEvent
import ru.cristalix.csc.event.PlayerSpendMoneyEvent
import ru.cristalix.csc.event.PrePlayerSpendMoneyEvent
import ru.cristalix.csc.event.WaveStatusEvent
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.Wave
import ru.cristalix.csc.phase.SpawnMobsPhase
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.util.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.minutes
import kotlin.time.seconds

@Suppress("unused")
sealed class CustomItem(
    val shopInfo: ShopInfo? = null,
) : Listener {

    companion object {
        val VALUES = CustomItem::class.sealedSubclasses.map { it.objectInstance ?: error("$it is not object") }
    }

    private val tagID = javaClass.simpleName.toLowerCase()
    private val boughtItem = hashSetOf<UUID>()
    protected val lastUsages = hashMapOf<UUID, Long>()

    protected abstract fun createStackWithoutTag(player: Player): NMSItemStack

    private fun requireShopInfo() = shopInfo ?: error("$this does not have ShopInfo")

    fun createStack(player: Player): NMSItemStack = createStackWithoutTag(player).also {
        it.accessTag {
            setBoolean(tagID, true)
        }
    }

    fun createDisplayStack(player: Player, balance: Int): NMSItemStack = createStackWithoutTag(player).also {
        it.updateLore { lore ->
            val price = requireShopInfo().price
            val color = if (balance >= price) GREEN else RED
            val priceWord = CommonMessages.price(player)
            lore += "$color$priceWord: $GOLD$price"
        }
    }

    open fun canBePurchased(player: Player): Boolean = true

    fun alreadyPurchased(player: Player): Boolean {
        return requireShopInfo().forceUnique && player.uniqueId in boughtItem
    }

    open fun onPurchase(player: Player) {
        boughtItem += player.uniqueId
    }

    open fun clearData() {
        boughtItem.clear()
    }

    open fun setup(plugin: CustomSteveChaos) {
        register(plugin)
    }

    protected fun NMSItemStack.isWrongItem(): Boolean {
        val tag = tag ?: return true
        return tag.getNullableBoolean(tagID) != true || isCurrentlyTransferred
    }

    protected fun PlayerInteractEvent.isWrongInteract(): Boolean {
        if (hand != EquipmentSlot.HAND) return true
        if (action.isLeftClick()) return true
        if (player.asNMS().itemInHand.isWrongItem()) return true
        if (player.isSneaking) return true

        return false
    }

    protected fun Player.sendCooldownMessage(leftMillis: Long, prefix: PMessage0 = Messages.cooldownDefaultPrefix) {
        val cooldownWord = Messages.leftCooldownWord(this)
        val timeLeftText = leftMillis.toMinutesAndSecondsString(
            getMinutesWord = CommonMessages.localizedMinutesWord(this),
            getSecondsWord = CommonMessages.localizedSecondsWord(this),
        )
        sendMessage("$RED${prefix(this)}$cooldownWord: $GOLD$timeLeftText")
    }

    protected fun Player.findCSCPlayer(): CSCPlayer? {
        val cscPlayer = asCSCPlayerOrNull()?.takeIf { it.isAlive }
        return if (cscPlayer == null) {
            player.sendMessage(CommonMessages.notInGame)
            null
        } else
            cscPlayer
    }

    protected fun Player.consumeItemInHand() {
        val nmsPlayer = asNMS()
        if (nmsPlayer.itemInHand.amount > 1) {
            nmsPlayer.itemInHand.amount--
        } else {
            nmsPlayer.itemInHand = NMSItemStack.a
        }
    }

    protected fun isOnCooldown(player: Player, cooldown: Duration): Boolean {
        val lastUsage = lastUsages[player.uniqueId] ?: 0
        val leftMillis = lastUsage + cooldown.inMilliseconds.toLong() - System.currentTimeMillis()
        return if (leftMillis > 0) {
            player.sendCooldownMessage(leftMillis)
            true
        } else
            false
    }

    protected fun putOnCooldown(player: Player, cooldown: Duration, material: Material) {
        lastUsages[player.uniqueId] = millisNow()
        player.setCooldown(material, cooldown.inTicksInt)
    }

    private object Messages {
        val cooldownDefaultPrefix = message0(
            russian = "Предмет находится на перезарядке. ",
            english = "Item is on cooldown. ",
        )

        val leftCooldownWord = message0("Осталось", "Cooldown left")
    }

    object Midas : CustomItem(ShopInfo(1200, true)) {
        private val material = Material.GOLD_NUGGET
        private val cooldown = 95.seconds

        private val displayName = message0(
            russian = "${GOLD}Мидас",
            english = "${GOLD}Midas",
        )
        private val lore = listOf(
            message0(
                russian = "${GRAY}При ПКМ на моба убивает его и даёт вам 180 золота",
                english = "${GRAY}Right clicking on mob kills it and gives you 180 gold",
            ),
            message0(
                russian = "${GRAY}Перезарядка: ${cooldown.toInt(TimeUnit.SECONDS)} секунд",
                english = "${GRAY}Cooldown: ${cooldown.toInt(TimeUnit.SECONDS)} seconds"
            )
        )

        private val onlyForMonsters = message0(
            russian = "${RED}Этот предмет можно применять только к монстрам",
            english = "${RED}This item can only be used on monsters",
        )
        private val usedOnBoss = message0(
            russian = "${RED}Вы не можете использовать этот предмет на босса",
            english = "${RED}This item cannot be used on boss",
        )
        private val successfullyUsed = message0(
            russian = "${GREEN}Вы успешно убили монстра",
            english = "${GREEN}You successfully killed a monster",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = material,
            displayName = displayName(player),
            lore = lore(player),
        )

        @EventHandler
        fun PlayerInteractEntityEvent.handle() {
            if (hand != EquipmentSlot.HAND) return
            if (player.asNMS().itemInHand.isWrongItem()) return
            if (isOnCooldown(player, cooldown)) return

            val rightClicked = rightClicked
            if (rightClicked !is Creature) {
                player.sendMessage(onlyForMonsters)
                return
            }

            if (rightClicked.asNMS() is CSCEntityIronGolem) {
                player.sendMessage(usedOnBoss)
                return
            }

            val cscPlayer = player.findCSCPlayer() ?: return
            rightClicked.health = 0.0
            cscPlayer.changeGold(180)

            player.sendMessage(successfullyUsed)
            player.playSound(player.location, Sound.ENTITY_BLAZE_HURT, 1f, 1f)
            putOnCooldown(player, cooldown, material)
        }

        @EventHandler
        fun PlayerDeathEvent.handle() {
            lastUsages -= entity.uniqueId
        }
    }

    object DamageBook : CustomItem(ShopInfo(1650, false)) {
        private val name = message0(
            russian = "${RED}Книга урона",
            english = "${RED}Damage book",
        )
        private val lore = message0(
            russian = "${GRAY}Дает +1 урон мечам и +0.5 урона лукам на всю игру",
            english = "${GRAY}Gives +1 melee damage and +0.5 range damage for the whole game",
        )
        private val successfullyUsed = message0(
            russian = "${GREEN}Ваш урон увеличен",
            english = "${GREEN}Your damage has been increased",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.BOOK,
            displayName = name(player),
            lore = listOf(lore(player)),
        )

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            cscPlayer.additionalDamage++
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).baseValue++
            player.sendTitle(successfullyUsed(player), "", 5, 20, 5)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            player.consumeItemInHand()
        }
    }

    object HealthBook : CustomItem(ShopInfo(1400, false)) {
        private val name = message0(
            russian = "${GREEN}Книга жизни",
            english = "${GREEN}Health book",
        )
        private val lore = message0(
            russian = "${GRAY}Дает вам +2 сердца на всю игру",
            english = "${GRAY}Gives you +2 hearts for the whole game",
        )
        private val successfullyUsed = message0(
            russian = "${GREEN}Вы получили +2 сердца",
            english = "${GREEN}You received +2 hearts",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.BOOK,
            displayName = name(player),
            lore = listOf(lore(player)),
        )

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            cscPlayer.additionalHealthFromBook += 4
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue += 4
            player.sendTitle(successfullyUsed(player), "", 5, 20, 5)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            player.consumeItemInHand()
        }
    }

    object RegenerationBook : CustomItem(ShopInfo(1350, false)) {
        private val name = message0(
            russian = "${GREEN}Книга регенерации",
            english = "${GREEN}Regeneration book",
        )
        private val lore = message0(
            russian = "${GRAY}Увеличивает восстановление здоровья на 0.5 сердца",
            english = "${GRAY}Increases health regeneration by 0.5 hearts",
        )
        private val successfullyUsed = message0(
            russian = "${GREEN}Вы получили +0.5 регенерации",
            english = "${GREEN}You received +0.5 regeneration",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.BOOK,
            displayName = name(player),
            lore = listOf(lore(player)),
        )

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            cscPlayer.additionalRegeneration++
            player.sendTitle(successfullyUsed(player), "", 5, 20, 5)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            player.consumeItemInHand()
        }

        private lateinit var plugin: CustomSteveChaos

        private val healTimes = HashMap<UUID, Long>()
        private const val HEAL_FREQUENCY = 3
        private const val EXHAUSTION = 3

        override fun setup(plugin: CustomSteveChaos) {
            super.setup(plugin)
            this.plugin = plugin
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun EntityRegainHealthEvent.handle() {
            if (regainReason != EntityRegainHealthEvent.RegainReason.SATIATED) return
            val player = entity as? Player ?: return
            val cscPlayer = player.findCSCPlayer() ?: return
            cancel()

            val currentTime = System.currentTimeMillis() / 1000
            val lastHealTime = getLastHealTime(player)
            if (currentTime - lastHealTime < HEAL_FREQUENCY) return

            val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
            if (player.health < maxHealth) {
                player.heal(1.0 + cscPlayer.additionalRegeneration)
                healTimes[player.uniqueId] = currentTime
            }

            // This is because bukkit doesn't stop the exhaustion change when cancelling the event
            val previousExhaustion = player.exhaustion
            plugin.runTask {
                player.exhaustion = previousExhaustion + EXHAUSTION
            }
        }

        @EventHandler
        fun PlayerQuitEvent.handle() {
            healTimes -= player.uniqueId
        }

        private fun getLastHealTime(p: Player): Long {
            return healTimes.computeIfAbsent(p.uniqueId) { System.currentTimeMillis() / 1000 }
        }
    }

    object UpgradeBook : CustomItem() {
        private val name = message0(
            russian = "${GREEN}Книга апгрейда",
            english = "${GREEN}Upgrade book",
        )
        private val lore = message0(
            russian = "${GRAY}Следующий апгрейд предмета будет для вас бесплатным",
            english = "${GRAY}The next item upgrade will be free for you",
        )

        private val successfullyUsedMessage = message0(
            russian = "${GREEN}Вы использовали книгу апгрейда и бесплатно улучшили предмет",
            english = "${GREEN}You used an upgrade book and upgraded an item for free",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.BOOK,
            displayName = name(player),
            lore = listOf(lore(player)),
        )

        @EventHandler
        fun PrePlayerSpendMoneyEvent.handle() {
            if (cause != PlayerSpendMoneyEvent.Cause.UpgradeItem) return
            if (player.inventory.asNMS().findUpgradeBookSlot() == null) return

            amount = 0
        }

        @EventHandler
        fun PlayerSpendMoneyEvent.handle() {
            if (cause != PlayerSpendMoneyEvent.Cause.UpgradeItem) return
            if (amount != 0) return

            if (player.clearSlotIfPossible() || player.clearCursorIfPossible()) {
                player.sendMessage(successfullyUsedMessage)
            }
        }

        private fun Player.clearSlotIfPossible(): Boolean {
            val inventory = inventory.asNMS()
            val upgradeBookSlot = inventory.findUpgradeBookSlot() ?: return false
            val newBookItem = inventory.getItemOrCursor(upgradeBookSlot)
            newBookItem.amount--
            inventory.setItemOrCursor(upgradeBookSlot, newBookItem)
            return true
        }

        private fun Player.clearCursorIfPossible(): Boolean {
            val itemOnCursor = itemOnCursor
            if (itemOnCursor.asNMS().isWrongItem()) return false
            itemOnCursor.amount--
            this.itemOnCursor = itemOnCursor

            return true
        }

        private fun NMSPlayerInventory.findUpgradeBookSlot(): Int? {
            for (slot in CURSOR_SLOT until size) {
                val item = getItemOrCursor(slot)
                if (item.isWrongItem()) continue

                return slot
            }

            return null
        }
    }

    object Thorns : CustomItem(ShopInfo(1800, true)) {
        private val cooldown = 40.seconds

        private val name = message0(
            russian = "${RED}Шипы",
            english = "${RED}Thorns",
        )
        private val lore = listOf(
            message0(
                russian = "${GRAY}Следующие 6 сек. вы блокируете и",
                english = "${GRAY}For the next 6 seconds you block and",
            ),
            message0(
                russian = "${GRAY}возвращаете 50% полученного урона врагу",
                english = "${GRAY}return 50% of received damage to enemy",
            ),
            message0(
                russian = "${GRAY}Перезарядка: ${cooldown.toInt(TimeUnit.SECONDS)} секунд",
                english = "${GRAY}Cooldown: ${cooldown.toInt(TimeUnit.SECONDS)} seconds"
            )
        )

        private val startedReflect = message0(
            russian = "${GREEN}Вы начали отражать урон",
            english = "${GREEN}You started reflecting damage",
        )
        private val stoppedReflect = message0(
            russian = "${RED}Вы перестали отражать урон",
            english = "${RED}You stopped reflecting damage",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.NETHER_STAR,
            displayName = name(player),
            lore = lore(player),
        )

        private val activeRunnables = hashMapOf<UUID, DisplayThornsRunnable>()

        private lateinit var plugin: Plugin

        override fun setup(plugin: CustomSteveChaos) {
            super.setup(plugin)
            this.plugin = plugin

            plugin.runRepeating(Duration.ZERO, 0.5.seconds) {
                for (uuid in activeRunnables.keys.toSet()) {
                    val player = CSCPlayer.getOrNull(uuid)
                    if (player == null || !player.isAlive) {
                        activeRunnables -= uuid
                    }
                }
            }
        }

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            if (isOnCooldown(player, cooldown)) return

            activeRunnables[player.uniqueId] = DisplayThornsRunnable(player).start(plugin)
            player.sendMessage(startedReflect)
            player.playSound(player.location, Sound.ENTITY_ENDERDRAGON_AMBIENT, 0.4f, 1.5f)
            putOnCooldown(player, cooldown, material)
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (entity.uniqueId !in activeRunnables) return
            if (damage == 0.0) return

            val damager = damager.getActualDamager() as? LivingEntity ?: return
            damager.asNMS().damageEntity(DamageSource.OUT_OF_WORLD, damage.toFloat() / 2)
            damage /= 2
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun EntityDamageEvent.handleForCreature() {
            if (entity is Creature && cause == EntityDamageEvent.DamageCause.VOID) {
                isCancelled = false
            }
        }

        @EventHandler
        fun PlayerDeathEvent.handle() {
            lastUsages -= entity.uniqueId
            activeRunnables.remove(entity.uniqueId)?.cancel()
        }

        private class DisplayThornsRunnable(private val player: Player) : BukkitRunnable() {
            private companion object {
                private const val TOTAL_TICKS = 6 * 4 // 4 times in each of 6 seconds

                private val actionBars = Array(TOTAL_TICKS) { tick ->
                    buildString {
                        append("${GREEN}Шипы: $DARK_GRAY[")
                        append(GREEN)
                        append("|".repeat(TOTAL_TICKS - 1 - tick))
                        append(GRAY)
                        append("|".repeat(tick))
                        append("$DARK_GRAY]")
                    }
                }

                private val particleBlockFaces = BlockFace.values().take(4).toTypedArray()
            }

            private var ticksPassed = 0

            fun start(plugin: Plugin) = apply {
                runTaskTimer(plugin, 0, 20 / 4)
            }

            override fun run() {
                player.spawnParticlesAround(Particle.SPELL_WITCH, particleBlockFaces)
                player.sendActionBar(actionBars[ticksPassed])

                ticksPassed++
                if (ticksPassed >= TOTAL_TICKS) {
                    cancel()
                }
            }

            override fun cancel() {
                super.cancel()

                player.sendMessage(stoppedReflect)
                activeRunnables -= player.uniqueId
            }
        }
    }

    object Banana : CustomItem() {
        private const val MONEY_AMOUNT = 5

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.CLAY_BALL,
            displayName = "$GREEN+$MONEY_AMOUNT ${coinsMessage(player, MONEY_AMOUNT)}",
            customTag = ItemTag("thief", NBTTagInt(18))
        )

        private lateinit var plugin: CustomSteveChaos

        override fun setup(plugin: CustomSteveChaos) {
            super.setup(plugin)
            this.plugin = plugin
        }

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            cscPlayer.earnedCoins += MONEY_AMOUNT
            player.consumeItemInHand()
        }
    }

    object Rebirth : CustomItem(ShopInfo(8000, true)) {
        private val name = message0(
            russian = "${GREEN}Перерождение",
            english = "${GREEN}Rebirth",
        )
        private val lore = listOf(
            message0(
                russian = "${GRAY}Дает 1 дополнительную жизнь",
                english = "${GRAY}Gives 1 additional life",
            ),
            message0(
                russian = "${GRAY}Можно покупать только 1 раз за игру!",
                english = "${GRAY}Can only be purchased once per game!",
            )
        )

        private val successfullyUsed = message0(
            russian = "${GREEN}Вы получили +1 жизнь",
            english = "${GREEN}You received +1 life",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.MAGMA_CREAM,
            displayName = name(player),
            lore = lore(player),
        )

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            cscPlayer.addLife()
            player.consumeItemInHand()
            player.sendMessage(successfullyUsed)
        }
    }

    object Witch : CustomItem(ShopInfo(3000, false)) {
        private val name = message0(
            russian = "${GREEN}Ведьма",
            english = "${GREEN}Witch",
        )
        private val lore = message0(
            russian = "${YELLOW}Спавнит в следующем раунде всем врагам ведьму на карте",
            english = "${YELLOW}Spawns witch for all enemies in the next round",
        )

        override fun createStackWithoutTag(player: Player) = createBukkitItem(
            material = Material.MONSTER_EGG,
            displayName = name(player),
            lore = listOf(lore(player)),
        ).also {
            it.itemMeta = it.itemMeta.also { meta ->
                val eggMeta = meta as SpawnEggMeta
                eggMeta.spawnedType = EntityType.WITCH
            }
        }.asNMS()

        private const val PLAYER_DELAY_MINUTES = 6
        private const val TOTAL_DELAY_MINUTES = 2

        private val selfCooldownPrefix = message0(
            russian = "Вы можете покупать этот предмет раз в $PLAYER_DELAY_MINUTES минут. ",
            english = "You can buy this item every $PLAYER_DELAY_MINUTES minutes. ",
        )
        private val totalDelayPrefix = message0(
            russian = "Все игроки могут покупать этот предмет раз в $TOTAL_DELAY_MINUTES минуты. ",
            english = "All players can buy this item every $TOTAL_DELAY_MINUTES minutes. "
        )
        private val blazesAlreadyScheduled = message0(
            russian = "${RED}На следующей волне уже запланировано появление ведьм",
            english = "${RED}Witches are already scheduled for next wave",
        )
        private val successfullyUsed = message0(
            russian = "${GREEN}Вы успешно призвали ведьм на следующую волну",
            english = "${GREEN}You successfully scheduled witches spawn for next wave",
        )

        private val lastPurchases = hashMapOf<UUID, Long>()
        private var lastPurchase: Long = 0
        private var targetDisplayWave = -1

        private lateinit var lastUsedPlayer: LastUsedPlayer
        private lateinit var lastMessagePack: CSCMessagePack
        private lateinit var gameScheduler: GameScheduler

        override fun canBePurchased(player: Player): Boolean {
            val playerLastPurchase = lastPurchases[player.uniqueId] ?: 0
            if (playerLastPurchase.milliseconds.betweenNow() < PLAYER_DELAY_MINUTES.minutes) {
                player.sendCooldownMessage(
                    leftMillis = (playerLastPurchase + PLAYER_DELAY_MINUTES * 60 * 1000) - millisNow(),
                    prefix = selfCooldownPrefix,
                )
                return false
            }

            if (lastPurchase.milliseconds.betweenNow() < TOTAL_DELAY_MINUTES.minutes) {
                player.sendCooldownMessage(
                    leftMillis = (lastPurchase + TOTAL_DELAY_MINUTES * 60 * 1000) - millisNow(),
                    prefix = totalDelayPrefix,
                )
                return false
            }

            return true
        }

        override fun onPurchase(player: Player) {
            super.onPurchase(player)
            lastPurchases[player.uniqueId] = millisNow()
            lastPurchase = millisNow()
        }

        override fun clearData() {
            super.clearData()
            lastPurchases.clear()
            lastPurchase = 0
            targetDisplayWave = -1
        }

        @EventHandler
        fun CSCGameStartEvent.handle() {
            this@Witch.gameScheduler = gameScheduler
        }

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return

            if (gameScheduler.displayWaveIndex + 1 == targetDisplayWave) {
                player.sendMessage(blazesAlreadyScheduled)
                return
            }

            lastUsedPlayer = LastUsedPlayer(player.playerProfile, player.asCSCPlayer().team)
            targetDisplayWave = gameScheduler.displayWaveIndex + 1
            player.consumeItemInHand()
            player.sendMessage(successfullyUsed)

            lastMessagePack = player.asCSCPlayer().messagePack
            lastMessagePack.willSummonWitch.broadcast(player.name)
        }

        @EventHandler
        fun WaveStatusEvent.handle() {
            if (status != WaveStatusEvent.Status.MobsSpawn) return
            if (displayWaveIndex != targetDisplayWave) return

            val lastUsedPlayer = lastUsedPlayer
            val lastPlayerName = lastUsedPlayer.profile.name ?: error("$lastUsedPlayer doesn't have name")
            lastMessagePack.summonedWitch.broadcast(lastPlayerName)

            for (cscPlayer in CSCPlayer.getLivingPlayers()) {
                if (cscPlayer.team == lastUsedPlayer.team) continue
                val player = cscPlayer.asBukkitPlayerOrNull() ?: continue
                if (player.skipSpawnMobs()) continue

                val spawnLocation = cscPlayer.team.room.mobsLocation.clone().add(0.0, 3.0, 0.0)
                val entity = Wave.Witch.createEntityWithBuffs(player.world.asNMS(), targetDisplayWave)
                entity.spawnAt(spawnLocation)
            }
        }

        private fun Player.skipSpawnMobs(): Boolean {
            if (uniqueId == lastUsedPlayer.profile.id) return true

            val phase = gameScheduler.currentPhase.let { it as? SpawnMobsPhase ?: error("$it is not SpawnMobsPhase") }
            return phase.duelPhase?.isParticipant(this) == true
        }

        private data class LastUsedPlayer(
            val profile: PlayerProfile,
            val team: CSCTeam,
        )
    }

    object Jump : CustomItem(ShopInfo(1250, true)) {
        private val cooldown = 30.seconds

        private lateinit var gameScheduler: GameScheduler

        private val name = message0(
            russian = "${GREEN}Прыжок",
            english = "${GREEN}Jump",
        )
        private val lore = listOf(
            message0(
                russian = "${GRAY}Прыгает на среднее расстояние вперед",
                english = "${GRAY}Throws you forward for middle distance",
            ),
            message0(
                russian = "${RED}Только на дуэли!",
                english = "${RED}Only for duel!",
            ),
        )

        private val notParticipant = message0(
            russian = "${RED}Вы не являетесь участником дуэли",
            english = "${RED}You are not a duel participant",
        )

        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.FEATHER,
            displayName = name(player),
            lore = lore(player),
        )

        @EventHandler
        fun CSCGameStartEvent.handle() {
            this@Jump.gameScheduler = gameScheduler
        }

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            if (isOnCooldown(player, cooldown)) return

            val duelPhase = gameScheduler.duelPhase
            if (duelPhase == null || duelPhase.stage != DuelStage.Active) {
                player.sendMessage(CommonMessages.duelNotActive)
                return
            }

            if (!duelPhase.isParticipant(player)) {
                player.sendMessage(notParticipant)
                return
            }

            val direction = player.location.direction
            val vector = Vector(direction.x * 3, (direction.y + 1) * 0.3, direction.z * 3)
            player.velocity = vector

            putOnCooldown(player, cooldown, material)
        }
    }

    object GoldenCoins : CustomItem() {
        private val name = message0(
            russian = "${GREEN}Золотые монетки",
            english = "${GREEN}Golden coins",
        )
        private val lore = message0(
            russian = "${GRAY}Дает (75 + 2-4 * № волны) золота",
            english = "${GRAY}Gives (75 + 2-4 * wave number) gold",
        )
        override fun createStackWithoutTag(player: Player) = createNMSItem(
            material = Material.CLAY_BALL,
            displayName = name(player),
            lore = listOf(lore(player)),
            customTag = ItemTag("other", NBTTagString("coin3")),
        )

        @EventHandler
        fun PlayerInteractEvent.handle() {
            if (isWrongInteract()) return
            val cscPlayer = player.findCSCPlayer() ?: return

            val displayWaveIndex = cscPlayer.gameScheduler.displayWaveIndex
            val goldForWave = RANDOM.nextInt(displayWaveIndex * 2, displayWaveIndex * 4 + 1)
            val addedGold = 75 + goldForWave
            cscPlayer.changeGold(addedGold)
            player.consumeItemInHand()
        }
    }
}
