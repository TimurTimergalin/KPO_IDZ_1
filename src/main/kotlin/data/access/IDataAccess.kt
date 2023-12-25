package data.access

import kotlinx.serialization.Serializable
import java.io.Closeable
import java.time.LocalDateTime

// Интерфейс для непосредственного доступа к данным
// Предоставляет адаптеры для манипуляции объектами в базе данных произвольной природы
interface IDataAccess : Closeable {
    // Методы создания новых объектов (C из CRUD)
    fun createFilm(name: String, description: String, duration: Int): IFilmEntity
    fun createSeance(filmId: Int, startTime: LocalDateTime): ISeanceEntity
    fun createTicket(seanceId: Int, row: Int, seat: Int): ITicketEntity
    fun createUser(login: String, password: String, isAdmin: Boolean): IUserEntity

    // Геттеры (R из CRUD)

    // Одиночные (по id)
    fun getFilm(id: Int): IFilmEntity?
    fun getSeance(id: Int): ISeanceEntity?
    fun getTicket(id: Int): ITicketEntity?
    fun getUser(id: Int): IUserEntity?

    // Общие
    fun getFilms(): Sequence<IFilmEntity>
    fun getSeances(): Sequence<ISeanceEntity>
    fun getTickets(): Sequence<ITicketEntity>
    fun getUsers(): Sequence<IUserEntity>
}