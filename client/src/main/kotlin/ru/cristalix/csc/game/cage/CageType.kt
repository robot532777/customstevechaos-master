package ru.cristalix.csc.game.cage

import me.stepbystep.api.MaterialData
import me.stepbystep.api.asGlassData
import me.stepbystep.api.asMaterialData
import me.stepbystep.api.materialData
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.util.Vector
import ru.cristalix.csc.shop.item.CSCCage

sealed class CageType(
    private val cage: CSCCage,
    private val points: Array<Vector>,
) {
    companion object {
        private val byCage by lazy {
            CageType::class.nestedClasses
                .mapNotNull { it.objectInstance as? CageType }
                .associateBy { it.cage }
        }

        fun get(cage: CSCCage): CageType = byCage[cage] ?: error("Cage for $cage does not exist")
    }

    abstract fun fill(center: Location)

    fun clear(center: Location) {
        points.forEachPoint(center) {
            it.type = Material.AIR
        }
    }

    private fun Array<Vector>.forEachPoint(center: Location, action: (Block) -> Unit) {
        val centerBlock = center.block
        forEach {
            val otherBlock = centerBlock.getRelative(it.blockX, it.blockY, it.blockZ)
            action(otherBlock)
        }
    }

    protected fun Array<Vector>.fillMaterialData(center: Location, materialData: MaterialData) {
        forEachPoint(center) {
            it.materialData = materialData
        }
    }

    open class FillAndCrossCageType(
        cage: CSCCage,
        private val platformData: MaterialData,
        private val wallsData: MaterialData,
    ) : CageType(cage, CagePoints.FillAndCross) {

        constructor(cage: CSCCage, materialData: MaterialData) : this(cage, materialData, materialData)

        override fun fill(center: Location) {
            CagePoints.TopAndBottom.fillMaterialData(center, platformData)
            CagePoints.CrossWalls.fillMaterialData(center, wallsData)
        }
    }

    open class FillCageType(
        cage: CSCCage,
        private val platformData: MaterialData,
        private val wallsData: MaterialData,
    ) : CageType(cage, CagePoints.Fill) {
        override fun fill(center: Location) {
            CagePoints.TopAndBottom.fillMaterialData(center, platformData)
            CagePoints.Walls.fillMaterialData(center, wallsData)
        }
    }

    open class ColumnsCageType(
        cage: CSCCage,
        private val platformData: MaterialData,
        private val wallsData: MaterialData,
        private val columnsData: MaterialData,
    ) : CageType(cage, CagePoints.Fill) {

        override fun fill(center: Location) {
            CagePoints.TopAndBottom.fillMaterialData(center, platformData)
            CagePoints.CrossWalls.fillMaterialData(center, wallsData)
            CagePoints.CornerWalls.fillMaterialData(center, columnsData)
        }
    }

    object Default : FillAndCrossCageType(
        cage = CSCCage.Default,
        materialData = Material.GLASS.asMaterialData(),
    )

    object Prison : FillCageType(
        cage = CSCCage.Prison,
        platformData = Material.IRON_BLOCK.asMaterialData(),
        wallsData = Material.IRON_FENCE.asMaterialData(),
    )

    object Watermelon : FillAndCrossCageType(
        cage = CSCCage.Watermelon,
        platformData = Material.MELON_BLOCK.asMaterialData(),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.GREEN),
    )

    object Golden : FillAndCrossCageType(
        cage = CSCCage.Golden,
        platformData = Material.GOLD_BLOCK.asMaterialData(),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.YELLOW),
    )

    object Olympus : ColumnsCageType(
        cage = CSCCage.Olympus,
        platformData = Material.STAINED_GLASS.asGlassData(DyeColor.WHITE),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.WHITE),
        columnsData = Material.QUARTZ_BLOCK.asMaterialData()
    )

    object Woodcutter : FillAndCrossCageType(
        cage = CSCCage.Woodcutter,
        platformData = Material.LOG.asMaterialData(),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.BROWN),
    )

    object Dragon : FillAndCrossCageType(
        cage = CSCCage.Dragon,
        platformData = Material.OBSIDIAN.asMaterialData(),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.PURPLE),
    )

    object Nether : FillAndCrossCageType(
        cage = CSCCage.Nether,
        platformData = Material.NETHERRACK.asMaterialData(),
        wallsData = Material.STAINED_GLASS.asGlassData(DyeColor.RED),
    )
}
