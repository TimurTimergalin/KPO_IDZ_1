package ui

class Message(private val message: String, override val removePrevious: Boolean = false) : IUIElement {
    override fun run(): IUIElement? {
        println(message)
        return null
    }
}