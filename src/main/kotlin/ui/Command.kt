package ui

data class Command(val description: String, val args: String, val action: (args: List<String>) -> IUIElement?)