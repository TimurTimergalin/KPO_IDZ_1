import data.access.*
import data.access.exception.DataAccessException
import data.manipulation.DataManipulator
import data.manipulation.MatrixHall
import data.manipulation.exception.DataManipulationException
import data.representation.fullmemory.read.JsonReader
import ui.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeParseException

fun checkLength(args: List<String>, n: Int) {
    if (n != args.size) {
        throw wrongArgumentsCount()
    }
}

fun checkLength(args: List<String>, ns: IntRange) {
    if (args.size !in ns) {
        throw wrongArgumentsCount()
    }
}

fun toInt(arg: String): Int {
    return arg.toIntOrNull() ?: throw wrongInt()
}

fun toDateTime(arg: String): LocalDateTime {
    try {
        return LocalDateTime.parse(arg, DATETIME_PATTERN)
    } catch (e: DateTimeParseException) {
        throw CommandException("Неверный формат времени")
    }
}

fun authWindow(data: DataManipulator, again: Boolean = false): IUIElement = Window(again) {
    setText { "Добро пожаловать в систему кинотеатра! Чтобы продолжить, авторизуйтесь" }
    command("auth", "Авторизация в систему", "login password") {
        checkLength(it, 2)
        val user = data.authorize(it[0], it[1]) ?: throw CommandException("Неверные логин или пароль")
        mainWindow(data, user)
    }
}

fun mainWindow(data: DataManipulator, user: IUserEntity) = Window(true) {
    setText { "Выберете, что нужно сделать" }
    command("films", "Перейти в режим управления фильмами", "") {
        checkLength(it, 0)
        filmsWindow(data)
    }
    command(
        "seances", """Просмотреть информацию о сеансах в кинотеатре
        |Возможные параметры:
        |past - прошедшие сеансы
        |today - сеансы сегодня
        |week - сеансы на ближайшие 7 дней
        |month сеансы на ближайшие 30 дней
        |all - все сеансы 
    """.trimMargin(), "past|today|week|month|all\""
    ) {
        checkLength(it, 1)
        val filter = getSeanceFilter(it[0])
        seancesWindow(data, null, filter)
    }

    command("refund", "Вернуть билет", "ticket-id") {
        checkLength(it, 1)
        val ticketId = toInt(it[0])
        val ticket: ITicketEntity
        try {
            ticket = data.getTicketById(ticketId)
            data.checkRefundable(ticket)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        ticket.delete()
        Message("Билет успешно возвращен")
    }

    command("take-seat", "Использовать билет", "ticket-id") {
        checkLength(it, 1)
        val ticketId = toInt(it[0])
        val ticket: ITicketEntity
        try {
            ticket = data.getTicketById(ticketId)
            data.checkTicketToUse(ticket)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        ticket.seatTaken = true
        Message("Место помечено как занятое")
    }

    command("logout", "Выйти из аккаунта", "") {
        checkLength(it, 0)
        authWindow(data, true)
    }

    if (user.isAdmin) {
        command("admin", "Перейти в режим администратора", "") {
            checkLength(it, 0)
            adminWindow(data, user)
        }
    }
}

fun adminWindow(data: DataManipulator, reg: IUserEntity) = Window {
    var admin = reg
    setText { "Выберите команду" }
    command("new-user", "Создать нового пользователя", "login password") {
        checkLength(it, 2)
        try {
            data.newUser(it[0], it[1])
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        Message("Пользователь ${it[0]} добавлен")
    }

    command("change-password", "Сменить пароль пользователя", "login new-password") {
        checkLength(it, 2)
        try {
            val user = data.getUserByLogin(it[0])
            user.delete()
            data.newUser(it[0], it[1])
            Message("Пароль успешно сменен")
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
    }

    command("change-admin-password", "Сменить пароль администратора", "new-password") {
        checkLength(it, 1)
        admin.delete()
        admin = data.newUser("admin", it[0], true)
        Message("Пароль успешно сменён")
    }

}

fun getSeanceFilter(arg: String): (ISeanceEntity) -> Boolean = when (arg) {
    "today" -> {
        { s -> s.startTime.toLocalDate() == LocalDate.now() }
    }

    "week" -> { s ->

        val days = Period.between(LocalDate.now(), s.startTime.toLocalDate()).days
        days in 0..7
    }

    "month" -> { s ->
        val days = Period.between(LocalDate.now(), s.startTime.toLocalDate()).days
        days in 0..30
    }

    "past" -> {
        { s -> Duration.between(LocalDateTime.now(), s.startTime).isNegative }
    }

    "all" -> {
        { true }
    }

    else -> throw CommandException("Неверный аргумент")
}

fun filmsWindow(data: DataManipulator) = Window {
    setText {
        val text = "Список всех фильмов:\n" + data.getAllFilms().map { film -> "${film.id}. ${film.name}" }
            .joinToString("\n") + "\nВыберите, что вы хотите сделать с фильмами"
        text
    }
    command("add", "Добавить новый фильм", "") {
        checkLength(it, 0)
        print("Введите название фильма: ")
        val name = readln()
        print("Введите описание фильма: ")
        val description = readln()
        print("Введите длительность фильма в минутах: ")
        val sDuration = readln()
        val duration = toInt(sDuration)
        try {
            data.newFilm(name, description, duration)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        Message("Фильм успешно добавлен")
    }
    command("select", "Перейти к редактированию фильма", "film-id") {
        checkLength(it, 1)
        val filmId = toInt(it[0])
        val film = try {
            data.getFilmById(filmId)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        selectedFilmWindow(data, film)
    }
}

fun selectedFilmWindow(data: DataManipulator, film: IFilmEntity) = Window {
    setText {
        val text = """Выбранный фильм:
                                |Имя: ${film.name}
                                |Описание: ${film.description}
                                |Длительность в минутах: ${film.duration}
                                |Выберите, что вы хотите сделать с данным фильмом
                            """.trimMargin()
        text
    }
    command("delete", "Удалить фильм", "") {
        checkLength(it, 0)
        film.delete()
        Message("Фильм успешно удалён", true)
    }
    command("change-name", "Изменить им фильма", "") {
        checkLength(it, 0)
        try {
            print("Введите новое название фильма: ")
            film.name = readln()
        } catch (e: DataAccessException) {
            throw wrongData(e)
        }
        Skip()
    }
    command("change-description", "Изменить описание фильма", "") {
        checkLength(it, 0)
        print("Введите новое описание фильма: ")
        film.description = readln()
        Skip()
    }
    command("change-duration", "Изменить длительность фильма", "new-duration-minutes") {
        checkLength(it, 1)
        val duration = toInt(it[0])
        try {
            data.checkFilmDuration(duration, film)
            film.duration = duration
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        Skip()
    }
    command(
        "seances", """Просмотреть информацию о сеансах в кинотеатре
        |Возможные параметры:
        |past - прошедшие сеансы
        |today - сеансы сегодня
        |week - сеансы на ближайшие 7 дней
        |month сеансы на ближайшие 30 дней
        |all - все сеансы 
    """.trimMargin(), "past|today|week|month|all"
    ) {
        checkLength(it, 1)
        val filter: (ISeanceEntity) -> Boolean = getSeanceFilter(it[0])
        seancesWindow(data, film) { s -> filter(s) && s.film == film }
    }

}

fun seancesWindow(data: DataManipulator, film: IFilmEntity? = null, filter: (ISeanceEntity) -> Boolean) = Window {
    setText {
        val text = "Выбранные сеансы:\n" + data.getSeances(filter).map { s ->
            "${s.id}. ${s.film.name} ${s.startTime.format(DATETIME_PATTERN)}"
        }.joinToString("\n")
        text
    }
    if (film != null) {
        command("add", "Добавить новый сеанс", "start-time(hh:mm|dd.mm.yyyy)") {
            checkLength(it, 1)
            val time = toDateTime(it[0])
            try {
                data.newSeance(film, time)
            } catch (e: DataManipulationException) {
                throw wrongData(e)
            }
            Message("Сеанс успешно добавлен")
        }
    } else {
        command("add", "Перейти в режим добавления сенса", "") {
            checkLength(it, 0)
            Window {
                setText { "Фильмы:\n" + data.getAllFilms().map { f -> "${f.id}. ${f.name}" }.joinToString("\n") }
                command("add", "Добавить новый сеанс", "film-id start-time(hh:mm|dd.mm.yyyy)") {
                    checkLength(it, 2)
                    val filmId = toInt(it[0])
                    val time = toDateTime(it[1])
                    try {
                        data.newSeance(filmId, time)
                    } catch (e: DataManipulationException) {
                        throw wrongData(e)
                    }
                    Message("Сеанс успешно добавлен")
                }
            }
        }
    }

    command("select", "Перейти к редактированию сеанса", "seance-id") {
        checkLength(it, 1)
        val seanceId = toInt(it[0])
        val seance = try {
            data.getSeanceById(seanceId)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        selectedSeanceWindow(data, seance)
    }
}

fun selectedSeanceWindow(data: DataManipulator, seance: ISeanceEntity): IUIElement = Window {
    setText {
        val text = """Выбранный сеанс:
                |Фильм: ${seance.film.name}
                |Время начала: ${seance.startTime.format(DATETIME_PATTERN)}
                |Время окончания: ${seance.endTime().format(DATETIME_PATTERN)}
               """.trimMargin()
        text
    }
    command("delete", "Удалить сеанс", "") {
        checkLength(it, 0)
        seance.delete()
        Message("Сеанс успешно удалён", true)
    }

    command("film", "Перейти к редактированию фильма сеанса", "") {
        checkLength(it, 0)
        selectedFilmWindow(data, seance.film)
    }

    command("change-start-time", "Изменить время начала сеанса", "new-start-time(hh:mm|dd.mm.yyyy)") {
        checkLength(it, 1)
        val time = toDateTime(it[0])
        try {
            data.checkSeanceStartTime(time, seance)
        } catch (e: DataManipulationException) {
            throw wrongData(e)
        }
        seance.startTime = time
        Skip()
    }

    command("free-seats", "Показать свободные места на сеанс", "") {
        checkLength(it, 0)
        Window {
            setText {
                val text1 = "Свободные места на сеанс:\nРяд Место" + data.freeSeats(seance).map { (x, y) -> "$x $y" }
                    .joinToString("\n") + if (seance.endTime().isBefore(LocalDateTime.now())) {
                    "\nЭто прошедший сеанс. Продажа билетов недоступна"
                } else {
                    ""
                }
                text1
            }
            if (seance.endTime().isAfter(LocalDateTime.now())) {
                command(
                    "sell",
                    "Продать билет на этот сеанс. Если передать yes в качетсве последнего аргумента, билет будет сразу помечен как занятый",
                    "row seat [yes|no]"
                ) {
                    checkLength(it, 2..3)
                    val x = toInt(it[0])
                    val y = toInt(it[1])
                    if (it.size == 3 && it[2] != "yes" && it[2] != "no") {
                        throw CommandException("Неверный аргумент")
                    }
                    val takeSeat = it.size == 3 && it[2] == "yes"
                    val ticket = try {
                        data.newTicket(x, y, seance)
                    } catch (e: DataManipulationException) {
                        throw wrongData(e)
                    }
                    if (takeSeat) {
                        ticket.seatTaken = true
                    }
                    Message("Билет №${ticket.id} продан. Идёт печать...")
                }
            }
        }
    }

    command("taken-seats", "Показать занятые на сеансе места", "") {
        checkLength(it, 0)
        Message("Занятые места на этом сеансе:\n" + data.takenSeats(seance).map { (x, y) -> "$x $y" }
            .joinToString("\n"))
    }

    command("left-seats", "Показать ещё не занятые места", "") {
        checkLength(it, 0)
        Message("Занятые места на этом сеансе:\n" + data.leftSeats(seance).map { (x, y) -> "$x $y" }
            .joinToString("\n"))
    }
}

fun main(args: Array<String>) {
    val access = JsonReader().read("db.json")
    val hall = MatrixHall(10, 30)
    access.use {
        val data = DataManipulator(it, hall)
        WindowManager(authWindow(data)).run()
    }
}