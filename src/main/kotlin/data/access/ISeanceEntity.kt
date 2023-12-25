package data.access

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// Адаптер для манипуляции объектом сеанса
// Вызов сеттера свойств приводит к изменению в базе данных (U из CRUD)
interface ISeanceEntity : IDataEntity {
    val film: IFilmEntity  // Фильм сеанса
    var startTime: LocalDateTime  // Начало сеанса
}

val DATETIME_PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm|dd.MM.uuuu")

fun ISeanceEntity.endTime(): LocalDateTime = startTime.plusMinutes(film.duration.toLong())