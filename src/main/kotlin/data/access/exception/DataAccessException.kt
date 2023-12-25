package data.access.exception

// Базовый класс исключения при доступе к данным
open class DataAccessException : RuntimeException {
    constructor() : super() {
    }
    constructor(mes: String) : super(mes) {
    }
}