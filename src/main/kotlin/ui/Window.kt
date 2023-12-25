package ui

class Window internal constructor(
    private val text: () -> String,
    private val commands: Map<String, Command>,
    override val removePrevious: Boolean = false
) : IUIElement {
    override fun run(): IUIElement? {
        println(text())
        println()
        println("Доступны следующие команды:")
        for ((n, c) in commands) {
            println("$n: ${c.description}. Использование: $n ${c.args}")
            println()
        }
        do {
            val args = readln().split(' ')
            val command = args[0]

            val func = commands[command]
            if (func != null) {
                try {
                    return func.action(args.slice(1..<args.size))
                } catch (e: CommandException) {
                    println("${e.message}. Повторите ввод")
                }
            } else {
                println("Неизвестная команда. Повторите ввод")
            }
            continue
        } while (true)
    }

    class Builder {
        private var text: (() -> String)? = null
        private var commands: MutableMap<String, Command> = hashMapOf()

        fun setText(text: () -> String) {
            this.text = text
        }

        fun command(name: String, description: String, args: String, action: (args: List<String>) -> IUIElement?) {
            assert(!commands.containsKey(name))
            commands[name] = Command(description, args, action)
        }

        fun make(removePrevious: Boolean = false): Window {
            assert(text != null) { "Text was never set" }
            command(
                "back",
                "Возврат к предыдущему окну, или выход из программы, если такового нет",
                ""
            ) { null }
            command("exit", "Выход из программы", "") { Exit() }
            command("main", "Возврат к стартовому окну", "") { Replace() }
            return Window(text!!, commands, removePrevious)
        }
    }
}

fun Window(builderAction: Window.Builder.() -> Unit): Window {
    val builder = Window.Builder()
    builder.builderAction()
    return builder.make()
}

fun Window(removePrevious: Boolean, builderAction: Window.Builder.() -> Unit): Window {
    val builder = Window.Builder()
    builder.builderAction()
    return builder.make(removePrevious)
}
