package ui

class WindowManager(window: IUIElement) {
    val windows: MutableList<IUIElement> = mutableListOf()

    init {
        windows.add(window)

    }

    fun run() {
        while (windows.size != 0) {
            val next = windows.last().run()
            if (next is Exit) {
                windows.clear()
                continue
            }
            if (next is Replace) {
                val first = windows[0]
                windows.clear()
                windows.add(first)
                continue
            }
            if (next != null) {
                if (next.removePrevious) {
                    windows.removeLast()
                }
                windows.add(next)
            } else {
                windows.removeLast()
            }
            println()
        }
    }
}