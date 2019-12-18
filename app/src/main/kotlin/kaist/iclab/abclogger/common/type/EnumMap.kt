package kaist.iclab.abclogger.common.type

abstract class EnumMap<E: Enum<E>> (private val valueMap: Map<Int, E>) {
    fun fromValue (value: Int?, default: E): E = if (value == null) default else (valueMap[value] ?: default)
}

abstract class EnumTable<E: Enum<E>> (private val valueTable: Map<Pair<Int, Int>, E>) {
    fun fromValue (row: Int?, col: Int?, default: E): E = if (row == null || col == null) default else (valueTable[Pair(row, col)] ?: default)
}

interface HasId {
    val id: Int
}

interface HasRowCol {
    val row : Int
    val col: Int
}

inline fun <reified E> buildValueMap(): Map<Int, E> where E: HasId, E: Enum<E> = enumValues<E>().associateBy { it.id }

inline fun <reified E> buildValueTable(): Map<Pair<Int, Int>, E> where E: HasRowCol, E: Enum<E> = enumValues<E>().associateBy { Pair(it.row, it.col) }
