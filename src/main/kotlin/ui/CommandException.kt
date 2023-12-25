package ui

import data.access.exception.DataAccessException
import data.manipulation.exception.DataManipulationException

class CommandException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}

fun wrongArgumentsCount(): CommandException {
    return CommandException("Неверное количество параметров")
}

fun wrongInt(): CommandException {
    return CommandException("Аргумент должен быть числом")
}

fun wrongData(e: DataManipulationException): CommandException {
    return CommandException(e.message)
}

fun wrongData(e: DataAccessException): CommandException {
    return CommandException(e.message)
}