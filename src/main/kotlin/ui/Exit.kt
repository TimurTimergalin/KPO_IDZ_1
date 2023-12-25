package ui

class Exit: IUIElement {
    override fun run(): IUIElement? {
        return null
    }

    override val removePrevious: Boolean
        get() = false
}