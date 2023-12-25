package data.manipulation

interface IHall {
    val seats: Sequence<Pair<Int, Int>>
    fun contains(x: Int, y: Int): Boolean
    fun contains(p: Pair<Int, Int>): Boolean
}