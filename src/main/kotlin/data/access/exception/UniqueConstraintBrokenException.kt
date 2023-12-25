package data.access.exception

// Возникает в ситуациях, когда в базу пытаются добавить запись, значение поля которого должно быть уникальным,
// но таковым не является (т.е. в базе уже есть запись с таким занчением)
class UniqueConstraintBrokenException : DataAccessException {
    constructor() : super()
    constructor(mes: String) : super(mes)
}