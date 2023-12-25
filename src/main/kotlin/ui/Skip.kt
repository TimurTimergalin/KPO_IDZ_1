package ui

class Skip : IUIElement {
    override fun run(): IUIElement? {
        return null
    }

    override val removePrevious: Boolean
        get() = false
}