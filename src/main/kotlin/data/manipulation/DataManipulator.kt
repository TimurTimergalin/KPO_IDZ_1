package data.manipulation

import data.access.*
import data.access.exception.DataAccessException
import data.access.exception.UniqueConstraintBrokenException
import data.manipulation.exception.DataManipulationException
import java.time.LocalDateTime

// Класс со всеми запросами, которые использует приложение
class DataManipulator(private val access: IDataAccess, private val hall: IHall) {
    fun getAllFilms(): Sequence<IFilmEntity> {
        return access.getFilms()
    }

    fun getFilmById(id: Int): IFilmEntity {
        return access.getFilm(id) ?: throw DataManipulationException("Фильма с таким номером нет")
    }

    fun getSeanceById(id: Int): ISeanceEntity {
        return access.getSeance(id) ?: throw DataManipulationException("Сеанса с таким номером нет")
    }

    fun getSeances(filter: (ISeanceEntity) -> Boolean): Sequence<ISeanceEntity> {
        return access.getSeances().filter(filter)
    }

    fun authorize(login: String, password: String): IUserEntity? {
        for (user in access.getUsers()) {
            if (user.login == login) {
                if (user.password == hash(password)) {
                    return user
                }
                return null
            }
        }
        return null
    }

    private fun validateFilmDuration(duration: Int) {
        if (duration <= 0) {
            throw DataManipulationException("Неправильная длительность фильма")
        }
    }

    private fun checkIntersectionsWithNewDuration(duration: Int, film: IFilmEntity): Boolean {
        if (duration < film.duration) {
            return true
        }
        val futureSeances = access.getSeances().filter { it.endTime().isAfter(LocalDateTime.now()) }.toList()
        for (s1 in futureSeances) {
            for (s2 in futureSeances) {
                if (s1 == s2) {
                    continue
                }
                if (s1.film != film) {
                    continue
                }
                if (!checkDateTimeInclude(s1.startTime, duration, s2.startTime)) {
                    return false
                }
            }
        }
        return true
    }

    fun checkFilmDuration(duration: Int, film: IFilmEntity?) {
        validateFilmDuration(duration)
        if (film != null && !checkIntersectionsWithNewDuration(duration, film)) {
            throw DataManipulationException("При изменении длительности фильма в существующих сеансах появляются пересечения - операция не была выполнена")
        }
    }

    private fun checkDateTimeInclude(start: LocalDateTime, durationMinutes: Int, other: LocalDateTime): Boolean {
        return other.isAfter(start) && other.isBefore(start.plusMinutes(durationMinutes.toLong()))
    }

    fun checkSeanceStartTime(time: LocalDateTime, seance: ISeanceEntity) {
        if (time.isBefore(LocalDateTime.now())) {
            throw DataManipulationException("Нельзя создать сеанс в прошлом")
        }
        if (!validateSeanceStartTime(time, seance)) {
            throw DataManipulationException("В это время зал будет занят")
        }
    }

    fun checkSeanceStartTime(time: LocalDateTime, duration: Int) {
        if (time.isBefore(LocalDateTime.now())) {
            throw DataManipulationException("Нельзя создать сеанс в прошлом")
        }
        if (!validateSeanceStartTime(time, duration)) {
            throw DataManipulationException("В это время зал будет занят")
        }
    }

        private fun validateSeanceStartTime(time: LocalDateTime, seance: ISeanceEntity): Boolean {
        for (s in access.getSeances().filter { it.endTime().isAfter(LocalDateTime.now()) }) {
            if (s == seance) {
                continue
            }
            if (checkDateTimeInclude(s.startTime, s.film.duration, time)) {
                return false
            }
            if (checkDateTimeInclude(time, seance.film.duration, s.startTime)) {
                return false
            }
        }
        return true
    }

    private fun validateSeanceStartTime(time: LocalDateTime, duration: Int): Boolean {
        for (s in access.getSeances().filter { it.endTime().isAfter(LocalDateTime.now()) }) {
            if (checkDateTimeInclude(s.startTime, s.film.duration, time)) {
                return false
            }
            if (checkDateTimeInclude(time, duration, s.startTime)) {
                return false
            }
        }
        return true
    }

    fun newSeance(film: IFilmEntity, startTime: LocalDateTime): ISeanceEntity {
        checkSeanceStartTime(startTime, film.duration)
        return access.createSeance(film.id, startTime)
    }

    fun newSeance(filmId: Int, startTime: LocalDateTime): ISeanceEntity {
        return access.getFilm(filmId)?.let { newSeance(it, startTime) }
            ?: throw DataManipulationException("Фильма с таким номером нет")
    }

    fun newFilm(name: String, description: String, duration: Int): IFilmEntity {
        validateFilmDuration(duration)
        return try {
            access.createFilm(name, description, duration)
        } catch (e: DataAccessException) {
            throw DataManipulationException(e.message)
        }
    }

    private fun isBought(x: Int, y: Int, seance: ISeanceEntity): Boolean {
        for (t in access.getTickets().filter { it.seance == seance }) {
            if (t.row == x && t.seat == y) {
                return true
            }
        }
        return false
    }

    fun freeSeats(seance: ISeanceEntity): Sequence<Pair<Int, Int>> =
        hall.seats.filter { (x, y) -> !isBought(x, y, seance) }

    private fun validateSeat(x: Int, y: Int) = hall.contains(x, y)
    fun newTicket(x: Int, y: Int, seance: ISeanceEntity): ITicketEntity {
        if (!validateSeat(x, y)) {
            throw DataManipulationException("Такого места в зале нет")
        }

        try {
            return access.createTicket(seance.id, x, y)
        } catch (e: UniqueConstraintBrokenException) {
            throw DataManipulationException(e.message)
        }
    }

    fun takenSeats(seance: ISeanceEntity): Sequence<Pair<Int, Int>> =
        access.getTickets().filter { it.seatTaken && it.seance == seance }.map { Pair(it.row, it.seat) }

    fun leftSeats(seance: ISeanceEntity): Sequence<Pair<Int, Int>> =
        access.getTickets().filter { !it.seatTaken && it.seance == seance }.map { Pair(it.row, it.seat) }

    fun getTicketById(id: Int): ITicketEntity {
        return access.getTicket(id) ?: throw DataManipulationException("Билета с таким номером нет")
    }

    fun checkRefundable(ticket: ITicketEntity) {
        if (!ticket.seance.startTime.isAfter(LocalDateTime.now())) {
            throw DataManipulationException("Сеанс уже начался - за этот билет уже нельзя вернуть деньги")
        }
    }

    private fun checkUsable(ticket: ITicketEntity): Boolean {
        return ticket.seance.endTime().isAfter(LocalDateTime.now())
    }

    fun checkTicketToUse(ticket: ITicketEntity) {
        if (ticket.seatTaken) {
            throw DataManipulationException("Этот билет уже был использован")
        }
        if (!checkUsable(ticket)) {
            throw DataManipulationException("Сеанс уже закончился")
        }
    }

    fun newUser(login: String, password: String, isAdmin: Boolean = false): IUserEntity {
        try {
            return access.createUser(login, password, isAdmin)
        } catch (e: DataAccessException) {
            throw DataManipulationException(e)
        }
    }

    fun getUserByLogin(login: String): IUserEntity {
        return access.getUsers().filter { it.login == login }.firstOrNull()
            ?: throw DataManipulationException("Такого пользователя не существует")
    }
}