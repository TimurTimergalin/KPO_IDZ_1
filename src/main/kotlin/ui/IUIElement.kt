package ui

interface IUIElement {
    fun run(): IUIElement?
    val removePrevious: Boolean
}