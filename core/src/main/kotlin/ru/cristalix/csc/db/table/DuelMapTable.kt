package ru.cristalix.csc.db.table

import me.stepbystep.api.MaterialData
import me.stepbystep.api.db.location
import me.stepbystep.api.db.stringEnum
import me.stepbystep.api.to
import org.bukkit.Material
import org.jetbrains.exposed.dao.id.IntIdTable
import ru.cristalix.csc.db.column.cscGameType
import ru.cristalix.csc.withTablePrefix

object DuelMapTable : IntIdTable("duelMap") {
    override val tableName = super.tableName.withTablePrefix()

    val name = varchar("name", 36)
    val firstLocation = location("first")
    val secondLocation = location("second")
    val viewersLocation = location("viewers")
    val displayMaterial = stringEnum<Material>("display", Material::valueOf).default(Material.GRASS)
    val displayData = byte("displayData").default(0)
    val gameType = cscGameType("gameType").nullable().default(null)

    val defaultMaterialData: MaterialData
        get() {
            val defaultMaterial = displayMaterial.defaultValueFun?.invoke() ?: error("No default material")
            val defaultData = displayData.defaultValueFun?.invoke() ?: error("No default data")

            return defaultMaterial to defaultData
        }
}
