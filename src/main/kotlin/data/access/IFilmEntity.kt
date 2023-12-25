package data.access


// Адаптер для манипуляцией объектом фильма
// Вызов сеттера свойств приводит к изменению в базе данных (U из CRUD)
interface IFilmEntity : IDataEntity {
    var name: String  // Название фильма
    var description: String  // Описание фильма
    var duration: Int  // Длительность в минутах
}