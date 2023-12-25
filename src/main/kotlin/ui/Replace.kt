package ui

class Replace : IUIElement {
    override fun run(): IUIElement? {
        return null
    }

    override val removePrevious: Boolean
        get() = false
}