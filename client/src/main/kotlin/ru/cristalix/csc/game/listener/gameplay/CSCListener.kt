package ru.cristalix.csc.game.listener.gameplay

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.amount
import me.stepbystep.api.item.itemInHand
import me.stepbystep.api.item.updateLore
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.minecraft.server.v1_12_R1.*
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.metadata.FixedMetadataValue
import ru.cristalix.core.command.ICommandService
import ru.cristalix.csc.command.duel.DuelCommand
import ru.cristalix.csc.event.PlayerEnchantItemEvent
import ru.cristalix.csc.event.PlayerSpendMoneyEvent
import ru.cristalix.csc.event.PrePlayerSpendMoneyEvent
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.phase.*
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.*

class CSCListener(private val gameScheduler: GameScheduler) : Listener {

    private companion object {
        private const val DUEL_NPC_ID = 2
    }

    private val upgradeNpcID: Int

    init {
        val config = gameScheduler.plugin.getOrCreateConfig("config.yml")
        upgradeNpcID = config.getInt("upgradeNpcID")
    }

    @EventHandler
    fun EntityCombustEvent.handle() {
        cancelLobbyActionIfNeed(entity)

        if (isCancelled) return
        if (this !is EntityCombustByEntityEvent) return
        val projectile = combuster as? Projectile ?: return
        val shooter = projectile.shooter as? Player ?: return
        val player = entity as? Player ?: return

        if (player.hasSameTeam(shooter)) {
            cancel()
        }
    }

    @EventHandler
    fun PlayerInteractEvent.handle() {
        if (hand != EquipmentSlot.HAND) return
        if (item?.type != Material.SPLASH_POTION) return

        cancelLobbyActionIfNeed(player)
    }

    @EventHandler
    fun VehicleEnterEvent.handle() {
        if (entered is Player) {
            cancel()
        }
    }

    private fun Cancellable.cancelLobbyActionIfNeed(entity: Entity) {
        if (entity !is Player) return

        val shouldCancel = when (val phase = gameScheduler.currentPhase) {
            is SpawnMobsPhase -> {
                val duelPhase = phase.duelPhase ?: return
                if (duelPhase.stage == DuelStage.Active) return

                duelPhase.isParticipant(entity)
            }
            else -> true
        }

        if (shouldCancel) {
            cancel()
        }
    }

    @EventHandler
    fun PlayerDeathEvent.handle() {
        gameScheduler.plugin.runDelayed(2, entity.spigot()::respawn)
        keepInventory = true
        deathMessage = null
    }

    @EventHandler
    fun EntityDeathEvent.handle() {
        drops.clear()
        droppedExp = 0
    }

    @EventHandler
    fun NPCRightClickEvent.handle() {
        when (npc.id) {
            upgradeNpcID -> handleUpgrade(clicker)
            DUEL_NPC_ID -> // Bukkit cannot handle this command
                ICommandService.get().commandDispatcher.execute(DuelCommand.NAME, clicker.uniqueId)
        }
    }

    @EventHandler
    fun EntityDamageEvent.handle() {
        isCancelled = shouldCancel(gameScheduler.currentPhase)

        if (isCancelled) return

        val damager = damager ?: return
        val player = damager.getActualDamager() as? Player ?: return
        val cscPlayer = player.asCSCPlayerOrNull() ?: return

        val forceMetadata = damager.getMetadata(BOW_FORCE_METADATA)?.firstOrNull()?.asFloat()
        val additionalDamageModifier = if (forceMetadata != null) forceMetadata / 2 else 1f
        damage += 1.0 + gameScheduler.waveIndex / 6 + cscPlayer.additionalDamage * additionalDamageModifier
    }

    @EventHandler
    fun PlayerDropItemEvent.handle() {
        cancel()
    }

    @EventHandler
    fun PlayerInteractEvent.handleContainerClick() {
        val clickedBlock = clickedBlock ?: return
        if (clickedBlock.state is Container || clickedBlock.type == Material.ANVIL) {
            cancel()
        }
    }

    @EventHandler
    fun EntityShootBowEvent.handle() {
        val player = entity as? Player ?: return

        consumeArrow = false
        projectile.setMetadata(BOW_FORCE_METADATA, FixedMetadataValue(gameScheduler.plugin, force))
        gameScheduler.plugin.runTask(player::updateInventory)
    }

    // TODO: move this to CustomEnchantsListener
    @EventHandler
    fun PlayerArmorChangeEvent.handle() {
        gameScheduler.plugin.runDelayed(5) {
            val player = CSCPlayer.getOrNull(player.uniqueId) ?: return@runDelayed
            player.updateMaxHealth()
        }
    }

    private fun EntityDamageEvent.shouldCancel(phase: Phase): Boolean {
        val entity = entity
        val actualDamager = damager?.getActualDamager()
        return when (phase) {
            is SpawnMobsPhase -> {
                val duelPhase = phase.duelPhase
                when {
                    entity !is Player -> false
                    duelPhase?.isActive == true && duelPhase.isParticipant(entity) -> shouldCancel(duelPhase)
                    actualDamager is Player -> true
                    else -> entity.asCSCPlayerOrNull()?.team !in phase.leftTeams
                }
            }
            is LobbyPhase, is ScheduleNextWavePhase, is WaitingPhase -> true
            is DuelPhase -> when {
                phase.stage != DuelStage.Active -> true
                entity !is Player -> true
                !phase.isParticipant(entity) -> true
                this !is EntityDamageByEntityEvent -> false
                actualDamager !is Player -> true
                !phase.isParticipant(actualDamager) -> true
                else -> actualDamager.hasSameTeam(entity)
            }
            else -> error("Unknown phase: $phase")
        }
    }

    private fun handleUpgrade(player: Player) {
        if (!CSCPlayer.isAlive(player.uniqueId)) {
            player.sendMessage(CommonMessages.notInGame)
            return
        }

        val itemInHand = player.asNMS().itemInHand

        if (itemInHand.isEmpty) {
            player.sendMessage(Messages.noItemInHand)
            return
        }

        if (itemInHand.amount != 1) {
            player.sendMessage(Messages.multipleItems)
            return
        }

        openUpgradeMenu(player, itemInHand)
    }

    private fun openUpgradeMenu(player: Player, currentItem: NMSItemStack) {
        val currentLevel = currentItem.upgradeLevel
        val newLevel = currentLevel + 1
        val isArmor = currentItem.item is ItemArmor
        val isSword = currentItem.item is ItemSword
        val isBow = currentItem.item == Items.BOW
        val isShield = currentItem.item == Items.SHIELD
        val enchants = currentItem.asBukkitMirror().enchantments

        if (isArmor && currentLevel >= 5 || isShield && currentLevel >= 4) {
            player.sendMessage(Messages.maxLevel)
            return
        }

        fun getPriceAndEnchants(
            eachPrice: Int,
            vararg enchantments: Enchantment,
            priceLimit: Int = Int.MAX_VALUE
        ): Pair<Int, Map<Enchantment, Int>> {
            val price = newLevel * eachPrice
            val newEnchants = enchants.toMutableMap()
            for (enchantment in enchantments) {
                val oldLevel = newEnchants[enchantment] ?: 0
                newEnchants[enchantment] = oldLevel + 1
            }

            var totalPrice = price.coerceAtMost(priceLimit)
            if (isArmor && currentLevel == 4) {
                totalPrice += 100
            }
            if (isSword && currentItem.isFrozen) {
                totalPrice += 50
            }
            return totalPrice to newEnchants
        }

        val hasArthropodsDamage = Enchantment.DAMAGE_ARTHROPODS in enchants
        val hasUndeadDamage = Enchantment.DAMAGE_UNDEAD in enchants

        val (price, newEnchants) = when {
            isSword && hasArthropodsDamage && hasUndeadDamage ->
                getPriceAndEnchants(350, Enchantment.DAMAGE_ARTHROPODS, Enchantment.DAMAGE_UNDEAD, Enchantment.DAMAGE_ALL, priceLimit = 1600)

            isSword && hasArthropodsDamage ->
                getPriceAndEnchants(250, Enchantment.DAMAGE_ARTHROPODS, Enchantment.DAMAGE_ALL, priceLimit = 1600)

            isSword && hasUndeadDamage ->
                getPriceAndEnchants(250, Enchantment.DAMAGE_UNDEAD, Enchantment.DAMAGE_ALL, priceLimit = 1600)

            isSword -> getPriceAndEnchants(200, Enchantment.DAMAGE_ALL, priceLimit = 1600)

            isBow -> getPriceAndEnchants(350, Enchantment.ARROW_DAMAGE, priceLimit = 1600)

            isArmor && Enchantment.THORNS in enchants ->
                getPriceAndEnchants(325, Enchantment.THORNS, Enchantment.PROTECTION_ENVIRONMENTAL)

            isArmor && Enchantment.PROTECTION_PROJECTILE in enchants ->
                getPriceAndEnchants(300, Enchantment.PROTECTION_PROJECTILE, Enchantment.PROTECTION_ENVIRONMENTAL)

            isArmor -> getPriceAndEnchants(250, Enchantment.PROTECTION_ENVIRONMENTAL)

            isShield -> getPriceAndEnchants(300)

            else -> {
                player.sendMessage(Messages.wrongItem)
                return
            }
        }

        val actualPrice = PrePlayerSpendMoneyEvent(player, PlayerSpendMoneyEvent.Cause.UpgradeItem, price).let {
            it.callEvent()
            it.amount
        }

        openUpgradeMenu(player.asCSCPlayer(), price, actualPrice, newLevel, currentItem, newEnchants)
    }

    private fun openUpgradeMenu(
        cscPlayer: CSCPlayer,
        rawPrice: Int,
        price: Int,
        newLevel: Int,
        currentItem: NMSItemStack,
        newEnchants: Map<Enchantment, Int>
    ) {
        val player = cscPlayer.asBukkitPlayer()

        val menu = buildSharedMenu {
            size = 54
            title = Messages.itemUpgrade(player)

            fillGlass(DyeColor.GRAY)

            val upgradeItem = currentItem.asBukkitCopy().apply {
                itemMeta = itemMeta.also { meta ->
                    newEnchants.forEach { (enchant, level) ->
                        meta.addEnchant(enchant, level, true)
                    }
                }
            }.asNMS().apply {
                upgradeLevel = newLevel
                if (item == Items.SHIELD) {
                    maxShieldDurability += 2
                    shieldDurability = maxShieldDurability
                }
            }

            val stackWithPrice = upgradeItem.cloneItemStack().apply {
                updateLoreIfNeed(player, gameScheduler)
                updateLore {
                    val color = if (cscPlayer.balance >= price) GREEN else RED
                    val priceWord = CommonMessages.price(player)
                    it += "$color$priceWord: $GOLD$price золота"
                }
            }
            22 bindTo menuItem(stackWithPrice) {
                performUpgrade(cscPlayer, rawPrice, price, currentItem, upgradeItem)
            }
        }

        menu.openFor(player)
    }

    private fun performUpgrade(cscPlayer: CSCPlayer, rawPrice: Int, price: Int, previous: NMSItemStack, upgrade: NMSItemStack) {
        val player = cscPlayer.asBukkitPlayer()

        if (cscPlayer.balance < price) {
            player.sendMessage(CommonMessages.notEnoughBalance)
            return
        }

        player.closeInventory()

        if (!NMSItemStack.equals(player.asNMS().itemInHand, previous)) {
            player.sendMessage(Messages.removedItemFromHand)
            return
        }

        PlayerSpendMoneyEvent(player, PlayerSpendMoneyEvent.Cause.UpgradeItem, price).callEvent()
        val upgradedItem = upgrade.cloneItemStack().updateLoreIfNeed(player, gameScheduler)

        cscPlayer.additionalWorth += rawPrice
        cscPlayer.changeGold(-price)
        cscPlayer.refillables.updateEntry(previous, upgradedItem)
        player.asNMS().itemInHand = upgradedItem
        player.sendMessage(Messages.itemSuccessfullyUpgraded)
        PlayerEnchantItemEvent(player, upgrade).callEvent()
    }

    private object Messages {
        val noItemInHand = message0(
            russian = "${RED}Возьмите предмет в руку",
            english = "${RED}Take item in your hand",
        )

        val multipleItems = message0(
            russian = "${RED}Вы не можете прокачивать несколько предметов одновременно",
            english = "${RED}You cannot upgrade multiple items at the same time",
        )

        val maxLevel = message0(
            russian = "${RED}Этот предмет улучшен до максимального уровня",
            english = "${RED}This item was upgraded to maximum level",
        )

        val wrongItem = message0(
            russian = "${RED}Этот предмет нельзя улучшить",
            english = "${RED}This item cannot be upgraded",
        )

        val itemUpgrade = message0(
            russian = "Улучшение предмета",
            english = "Item upgrade",
        )

        val removedItemFromHand = message0(
            russian = "${RED}Вы убрали предмет из руки",
            english = "${RED}You removed item from your hand",
        )

        val itemSuccessfullyUpgraded = message0(
            russian = "${GREEN}Вы успешно улучшили предмет",
            english = "${GREEN}You successfully upgraded an item",
        )
    }
}
