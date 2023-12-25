package data.access


// Адаптер для манипуляции объектом билета
// Вызов сеттера свойств приводит к изменению в базе данных (U из CRUD)
interface ITicketEntity : IDataEntity {
    val seance: ISeanceEntity
    val row: Int
    val seat: Int
    var seatTaken: Boolean
}