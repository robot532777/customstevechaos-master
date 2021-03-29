package ru.cristalix.csc.game

import me.stepbystep.api.NMSWorld
import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.chat.message0
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.registerAttributeIfAbsent
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Material
import ru.cristalix.csc.entity.*
import ru.cristalix.csc.entity.animal.*
import ru.cristalix.csc.util.damage
import ru.cristalix.csc.util.maximumHealth
import ru.cristalix.csc.util.speed
import kotlin.math.pow

enum class Wave(val displayName: PMessage0) {
    Zombie(message0(
        russian = "Зомби",
        english = "Zombie",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            EntityZombie(world).withHealthAndDamage(5, 1, wave).also {
                it.speed *= 1.05
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(8) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    IronZombie(message0(
        russian = "Зомби-воины",
        english = "Zombie warriors",
    )) {
        override val requiredWave: Int get() = 9

        override fun createEntity(world: NMSWorld, wave: Int) =
            EntityZombie(world).withHealthAndDamage(7, 2, wave).apply {
                setSlot(EnumItemSlot.HEAD, Material.IRON_HELMET.asNewStack())
                setSlot(EnumItemSlot.CHEST, Material.IRON_CHESTPLATE.asNewStack())
                setSlot(EnumItemSlot.LEGS, Material.IRON_LEGGINGS.asNewStack())
                setSlot(EnumItemSlot.FEET, Material.IRON_BOOTS.asNewStack())
                setSlot(EnumItemSlot.MAINHAND, Material.IRON_SWORD.asNewStack())
                speed *= 1.04
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(4) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Skeleton(message0(
        russian = "Скелеты-лучники",
        english = "Skeleton archers",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            EntitySkeleton(world).withHealthAndDamage(3, 1, wave).apply {
                setSlot(EnumItemSlot.MAINHAND, Material.BOW.asNewStack())
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(6) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    BabyZombie(message0(
        russian = "Маленькие зомби",
        english = "Baby zombies",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            EntityZombie(world).withHealthAndDamage(2, 1, wave).apply {
                isBaby = true
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(11) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Spider(message0(
        russian = "Арахниды",
        english = "Arachnida",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntitySpider(world).withHealthAndDamage(3, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(8) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    CaveSpider(message0(
        russian = "Пещерные пауки",
        english = "Cave spiders",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityCaveSpider(world).withHealthAndDamage(3, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(7) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    WitherSkeleton(message0(
        russian = "Воины ада",
        english = "Nether warriors",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntitySkeletonWither(world).withHealthAndDamage(6, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(6) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Wolf(message0(
        russian = "Волки",
        english = "Wolves",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityWolf(world).withHealthAndDamage(3, 2, wave).apply {
                isAngry = true
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(7) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    PolarBear(message0(
        russian = "Полярные медведи",
        english = "Polar bears",
    )) {
        override val requiredWave: Int get() = 4

        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityPolarBear(world).withHealthAndDamage(8, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(5) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Silverfish(message0(
        russian = "Чешуйницы",
        english = "Silverfish",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            EntitySilverfish(world).withHealthAndDamage(1, 1, wave).also {
                it.speed *= 1.07
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(20) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Blaze(message0(
        russian = "Огненная западня",
        english = "Fire trap",
    )) {
        override val requiredWave: Int get() = 1

        // also update witch stats (they are the same)
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityBlaze(world).withHealthAndDamage(5, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(5) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Endermite(message0(
        russian = "Чешуйницы края",
        english = "Endermites",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityEndermite(world).withHealthAndDamage(3, 1, wave).also {
                it.speed *= 1.07
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(16) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    Animal(message0(
        russian = "Ферма",
        english = "Farm",
    )) {
        override val requiredWave: Int get() = 8

        override fun createEntity(world: NMSWorld, wave: Int) =
            throw UnsupportedOperationException("Unable to create single entity for Wave.Animals")

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            fun chicken() = CSCEntityChicken(world)
                .withHealthAndDamage(3, 2, wave)
                .withBuffs().also { it.speed *= 1.25 }

            fun horse() = CSCEntityHorse(world)
                .withHealthAndDamage(5, 1, wave)
                .withBuffs().also { it.speed *= 1.05 }

            fun cow() = CSCEntityCow(world)
                .withHealthAndDamage(7, 1, wave)
                .withBuffs().also { it.speed *= 1.1 }

            fun pig() = CSCEntityPig(world)
                .withHealthAndDamage(7, 2, wave)
                .withBuffs()

            fun sheep() = CSCEntitySheep(world)
                .withHealthAndDamage(6, 1, wave)
                .withBuffs()

            return listOf(chicken(), chicken(), horse(), cow(), pig(), sheep())
        }
    },
    PigZombie(message0(
        russian = "Свинозомби",
        english = "Pigmen",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityPigZombie(world).withHealthAndDamage(10, 1, wave).also {
                it.speed *= 1.15
            }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(6) {
                createEntityWithBuffs(world, wave)
            }
        }
    },
    SkeletonOnSpider(message0(
        russian = "Всадники апокалипсиса",
        english = "Horsemen of the Apocalypse",
    )) {
        override fun createEntity(world: NMSWorld, wave: Int): EntityInsentient {
            throw UnsupportedOperationException("Cannot create entity for $name")
        }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            return List(3) {
                val spider = Spider.createEntity(world, wave).withBuffs(false).also {
                    (it as CSCEntitySpider).modifySpeed(0.65)
                    it.maximumHealth *= 2f / 3
                }
                val skeleton = Skeleton.createEntityWithBuffs(world, wave).also {
                    it.maximumHealth *= 2f / 3
                    it.damage *= 2f / 3
                }
                skeleton.a(spider, true) // make skeleton sit on spider

                listOf(spider, skeleton)
            }.flatten()
        }
    },
    Mixed(message0(
        russian = "Микс",
        english = "Mix",
    )) {
        override val requiredWave: Int get() = 8

        private val waves = listOf(
            Zombie, IronZombie, Skeleton, Spider, CaveSpider, WitherSkeleton,
            Wolf, PolarBear, Silverfish, Blaze
        )

        override fun createEntity(world: NMSWorld, wave: Int): EntityInsentient {
            throw UnsupportedOperationException("Cannot create entity for Mixed type")
        }

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> =
            waves.map { it.createEntityWithBuffs(world, wave) }
    },
    IronGolem(message0(
        russian = "Босс",
        english = "Boss",
    )) {
        override val requiredWave get() = Int.MAX_VALUE

        override fun createEntity(world: NMSWorld, wave: Int) =
            CSCEntityIronGolem(world).withHealthAndDamage(25, 4, wave).also {
                it.damage /= 1.9
                it.speed *= 1.05
            }

        override fun createEntities(world: NMSWorld, wave: Int) = listOf(createEntityWithBuffs(world, wave))
    },
    Witch(message0(
        russian = "Ведьма",
        english = "Witch",
    )) {
        // Make sure it's fake wave!
        override val requiredWave: Int get() = Int.MAX_VALUE

        override fun createEntity(world: NMSWorld, wave: Int) =
            EntityWitch(world).withHealthAndDamage(5, 1, wave)

        override fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient> {
            throw UnsupportedOperationException("Cannot create multiple entities for Witch type")
        }
    }
    ;

    abstract fun createEntity(world: NMSWorld, wave: Int): EntityInsentient
    abstract fun createEntities(world: NMSWorld, wave: Int): List<EntityInsentient>

    // required previous waveDisplayIndex (for example, if we need to always allow this wave, requiredWave = 0)
    open val requiredWave: Int get() = 0

    fun createEntityWithBuffs(world: NMSWorld, wave: Int): EntityInsentient = createEntity(world, wave).withBuffs()

    protected fun EntityInsentient.withBuffs(withMovementSpeed: Boolean = true) = apply {
        if (withMovementSpeed && needsMovementSpeedBoost(this)) {
            speed *= 1.3
        }
        getAttributeInstance(GenericAttributes.FOLLOW_RANGE).value *= 1.5
    }

    protected fun <T : EntityInsentient> T.withHealthAndDamage(health: Int, damage: Int, wave: Int): T {
        registerAttributeIfAbsent(GenericAttributes.ATTACK_DAMAGE)
        return withHealth(health, wave).withDamage(damage, wave)
    }

    protected fun <T : EntityInsentient> T.withHealth(health: Int, wave: Int) = apply {
        val healthWithWave = health * getMultiplier(wave)
        getAttributeInstance(GenericAttributes.maxHealth).value = healthWithWave
        setHealth(healthWithWave.toFloat())
    }

    protected fun <T : EntityInsentient> T.withDamage(damage: Int, wave: Int) = apply {
        val damageWithWave = damage * getMultiplier(wave)
        getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).value = damageWithWave
    }

    private fun getMultiplier(wave: Int): Double {
        // 13 wave = 15 display wave; first wave == 0
        return (multiplierInWaves(wave, 1.2, 0..4) *
                multiplierInWaves(wave, 1.22, 4..13) *
                multiplierInWaves(wave, 1.17, 13..18) *
                multiplierInWaves(wave, 1.11, 18..23) *
                multiplierInWaves(wave, 1.09, 23..28) *
                multiplierInWaves(wave, 1.05, 28..Int.MAX_VALUE))
    }

    private fun multiplierInWaves(wave: Int, multiplier: Double, waveRange: IntRange): Double {
        return when {
            wave >= waveRange.last -> multiplier.pow(waveRange.last - waveRange.first)
            wave > waveRange.first -> multiplier.pow(wave - waveRange.first)
            else -> 1.0
        }
    }

    private fun needsMovementSpeedBoost(entity: Entity): Boolean =
        (entity !is EntityZombie || !entity.isBaby) && entity !is EntitySkeleton
}
