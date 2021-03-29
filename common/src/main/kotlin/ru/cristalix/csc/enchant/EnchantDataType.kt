package ru.cristalix.csc.enchant

import com.google.common.primitives.Ints
import java.nio.ByteBuffer
import java.nio.IntBuffer

@Suppress("UNCHECKED_CAST")
sealed class EnchantDataType {
    class Integer(private val asString: (Int) -> String) : EnchantDataType() {
        override fun doParse(rawData: ByteArray) = Ints.fromByteArray(rawData)
        override fun parseOrNull(rawData: String) = rawData.toIntOrNull()
        override fun toString(data: Any) = asString(data as Int)
        override fun encode(data: Any): ByteArray = Ints.toByteArray(data as Int)
    }

    open class IntegerArray(
        private val expectedSize: Int,
        private val asString: (IntArray) -> String,
    ) : EnchantDataType() {
        override fun doParse(rawData: ByteArray): IntArray {
            check(rawData.size % 4 == 0) { "Raw data is not applicable to IntegerRange: $rawData" }
            return ByteBuffer.wrap(rawData).asIntBuffer().copyToArray()
        }

        override fun parseOrNull(rawData: String): IntArray? {
            val split = rawData.split("-")
            if (split.size != expectedSize) return null
            return split.map { it.toInt() }.toIntArray()
        }

        override fun toString(data: Any): String {
            return asString(data as IntArray)
        }

        override fun encode(data: Any): ByteArray {
            check(data is IntArray) { "Data is not IntArray ($data)" }
            return data.flatMap { Ints.toByteArray(it).asIterable() }.toByteArray()
        }

        private fun IntBuffer.copyToArray(): IntArray {
            position(0)
            return IntArray(limit()).also(::get)
        }
    }

    object IntegerRange : IntegerArray(2, { "${it[0]}-${it[1]}" })

    fun <T> parse(rawData: ByteArray): T = doParse(rawData) as T

    protected abstract fun doParse(rawData: ByteArray): Any
    abstract fun parseOrNull(rawData: String): Any?
    abstract fun toString(data: Any): String
    abstract fun encode(data: Any): ByteArray
}
