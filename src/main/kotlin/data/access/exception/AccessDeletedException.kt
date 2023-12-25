package data.access.exception

// Возникает в ситуациях, когда данные пытаются поменять у несуществующего в базе объекта
class AccessDeletedException : DataAccessException {
    constructor() : super()
    constructor(mes: String) : super(mes)
}