package data.manipulation

class MatrixHall(private val rows: Int, private val seatsInRow: Int) : IHall {
    override val seats: Sequence<Pair<Int, Int>>
        get() = sequence {
            for (i in 1..rows) {
                for (j in 1..seatsInRow) {
                    yield(Pair(i, j))
                }
            }
        }

    override fun contains(x: Int, y: Int): Boolean {
        return x in 1..rows && y in 1..seatsInRow
    }

    override fun contains(p: Pair<Int, Int>): Boolean {
        return contains(p.first, p.second)
    }
}