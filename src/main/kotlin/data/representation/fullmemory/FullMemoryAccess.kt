package data.representation.fullmemory

import data.access.*
import data.access.exception.AccessDeletedException
import data.access.exception.UniqueConstraintBrokenException
import data.save.IDataSaver
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

private class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        return LocalDateTime.parse(string, DATETIME_PATTERN)
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val string = value.format(DATETIME_PATTERN)
        encoder.encodeString(string)
    }
}

// Классы для хранения объектов в памяти

@Serializable
private data class FilmData(var name: String, var description: String, var duration: Int)

@Serializable
private data class SeanceData(
    val filmId: Int,
    @Serializable(with = LocalDateTimeSerializer::class) var startTime: LocalDateTime
)

@Serializable
private data class TicketData(val seanceId: Int, val row: Int, val seat: Int) {
    var seatTaken: Boolean = false
}

@Serializable
private data class UserData(val login: String, val password: String, val isAdmin: Boolean)

// Снимок состояния базы данных, нужен для сериализации
@Serializable
private class DataSnapshot(
    val films: Map<Int, FilmData>,
    val seances: Map<Int, SeanceData>,
    val tickets: Map<Int, TicketData>,
    val users: Map<Int, UserData>,
    val filmsAutoIncrement: Int,
    val seancesAutoIncrement: Int,
    val ticketsAutoIncrement: Int,
    val usersAutoIncrement: Int
)

// Объект базы данных, осуществляющий управление объектами, храня их всех в оперативной памяти (отсюда название)
class FullMemoryAccess internal constructor(private var saver: IDataSaver? = null) : IDataAccess {
    private val films: MutableMap<Int, FilmData> = hashMapOf()
    private val seances: MutableMap<Int, SeanceData> = hashMapOf()
    private val tickets: MutableMap<Int, TicketData> = hashMapOf()
    private val users: MutableMap<Int, UserData> = hashMapOf()

    private var filmsAutoIncrement = 1
    private var seancesAutoIncrement = 1
    private var ticketsAutoIncrement = 1
    private var usersAutoIncrement = 1

    init {
        createUser("admin", "admin", true)
    }

    private constructor(  // Конструктор на основе снимка, нужен для десериализации
        dataSnapshot: DataSnapshot,
        saver: IDataSaver? = null
    ) : this(saver) {
        this.films.putAll(dataSnapshot.films)
        this.seances.putAll(dataSnapshot.seances)
        this.tickets.putAll(dataSnapshot.tickets)
        this.users.putAll(dataSnapshot.users)
        this.filmsAutoIncrement = dataSnapshot.filmsAutoIncrement
        this.seancesAutoIncrement = dataSnapshot.seancesAutoIncrement
        this.ticketsAutoIncrement = dataSnapshot.ticketsAutoIncrement
        this.usersAutoIncrement = dataSnapshot.usersAutoIncrement
    }

    // Создание снимка
    private fun makeSnapshot(): DataSnapshot {
        return DataSnapshot(
            films,
            seances,
            tickets,
            users,
            filmsAutoIncrement,
            seancesAutoIncrement,
            ticketsAutoIncrement,
            usersAutoIncrement
        )
    }

    // Сериализатор в JSON, сделан внутренним классом для доступа к приватным полям
    class JsonSerializer : KSerializer<FullMemoryAccess> {
        override fun deserialize(decoder: Decoder): FullMemoryAccess {
            val string = decoder.decodeString()
            val snapshot = Json.decodeFromString<DataSnapshot>(string)
            return FullMemoryAccess(snapshot)
        }

        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("FullMemoryAccess", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: FullMemoryAccess) {
            val string = Json.encodeToString(value.makeSnapshot())
            encoder.encodeString(string)
        }
    }

    // Представление объекта фильма. При любых изменениях его свойств эти изменения отразятся в базе данных
    inner class FilmEntity internal constructor(override val id: Int) : IFilmEntity {
        override val isDeleted: Boolean
            get() = !films.containsKey(id)

        override fun delete() {
            if (isDeleted) {
                throw AccessDeletedException("Attempted to delete an entity mapping to a non-existing object")
            }

            // Удаление происходит как cascade
            val toDelete: MutableSet<ISeanceEntity> = (getSeances().filter { it.film == this }).toHashSet()

            toDelete.forEach { it.delete() }
            films.remove(id)
        }

        override var name: String
            get() = films[id]?.name
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
            set(value) {
                if (isDeleted) {
                    throw AccessDeletedException("Attempted to modify an entity mapping to a non-existing object")
                }
                if (getFilms().filter { it.name == value }.iterator().hasNext()) {
                    throw UniqueConstraintBrokenException("Фильм с таким названием уже существует")
                }

                // Проверки isDeleted (выше) достаточно, чтобы заключить, что films[id] != null,
                // однако компилятор Kotlin такое не видит, поэтому используем !!
                // Если в будущем код будет меняться, и проверки станет не хватать, сработает assert (только в debug),
                // который точно укажет на проблему (не снижайте за !!, пожалуйста - здесь он уместен)

                val filmData = films[id]
                assert(filmData != null) { "isDeleted check works improperly" }
                films[id]!!.name = value
            }
        override var description: String
            get() = films[id]?.description
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
            set(value) {
                if (isDeleted) {
                    throw AccessDeletedException("Attempted to modify an entity mapping to a non-existing object")
                }

                // Проверки isDeleted (выше) достаточно, чтобы заключить, что films[id] != null,
                // однако компилятор Kotlin такое не видит, поэтому используем !!
                // Если в будущем код будет меняться, и проверки станет не хватать, сработает assert (только в debug),
                // который точно укажет на проблему (не снижайте за !!, пожалуйста - здесь он уместен)
                val filmData = films[id]
                assert(filmData != null) { "isDeleted check works improperly" }
                films[id]!!.description = value
            }
        override var duration: Int
            get() = films[id]?.duration
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
            set(value) {
                if (isDeleted) {
                    throw AccessDeletedException("Attempted to modify an entity mapping to a non-existing object")
                }

                // Проверки isDeleted (выше) достаточно, чтобы заключить, что films[id] != null,
                // однако компилятор Kotlin такое не видит, поэтому используем !!
                // Если в будущем код будет меняться, и проверки станет не хватать, сработает assert (только в debug),
                // который точно укажет на проблему (не снижайте за !!, пожалуйста - здесь он уместен)
                films[id].let {
                    assert(it != null)
                    it!!.duration = value
                }
            }

        override fun equals(other: Any?): Boolean {
            return other is FilmEntity && id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    // Представление объекта сеанса. При любых изменениях его свойств эти изменения отразятся в базе данных
    inner class SeanceEntity internal constructor(override val id: Int) : ISeanceEntity {
        override val isDeleted: Boolean
            get() = !seances.containsKey(id)

        override fun delete() {
            if (isDeleted) {
                throw AccessDeletedException("Attempted to delete an entity mapping to a non-existing object")
            }

            // Удаление происходит как cascade
            val toDelete: MutableSet<ITicketEntity> = (getTickets().filter { it.seance == this }).toHashSet()

            toDelete.forEach { it.delete() }
            seances.remove(id)
        }

        override val film: IFilmEntity
            get() = seances[id]?.filmId?.let { getFilm(it) }
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override var startTime: LocalDateTime
            get() = seances[id]?.startTime
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
            set(value) {
                if (isDeleted) {
                    throw AccessDeletedException("Attempted to modify an entity mapping to a non-existing object")
                }

                // Проверки isDeleted (выше) достаточно, чтобы заключить, что seances[id] != null,
                // однако компилятор Kotlin такое не видит, поэтому используем !!
                // Если в будущем код будет меняться, и проверки станет не хватать, сработает assert (только в debug),
                // который точно укажет на проблему (не снижайте за !!, пожалуйста - здесь он уместен)
                seances[id].let {
                    assert(it != null)
                    it!!.startTime = value
                }
            }

        override fun equals(other: Any?): Boolean {
            return other is SeanceEntity && id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    // Представление объекта билета. При любых изменениях его свойств эти изменения отразятся в базе данных
    inner class TicketEntity internal constructor(override val id: Int) : ITicketEntity {
        override val isDeleted: Boolean
            get() = !seances.containsKey(id)

        override fun delete() {
            if (isDeleted) {
                throw AccessDeletedException("Attempted to delete an entity mapping to a non-existing object")
            }
            tickets.remove(id)
        }

        override val seance: ISeanceEntity
            get() = tickets[id]?.seanceId?.let { getSeance(it) }
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override val row: Int
            get() = tickets[id]?.row
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override val seat: Int
            get() = tickets[id]?.seat
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override var seatTaken: Boolean
            get() = tickets[id]?.seatTaken
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
            set(value) {
                if (isDeleted) {
                    throw AccessDeletedException("Attempted to modify an entity mapping to a non-existing object")
                }

                // Проверки isDeleted (выше) достаточно, чтобы заключить, что tickets[id] != null,
                // однако компилятор Kotlin такое не видит, поэтому используем !!
                // Если в будущем код будет меняться, и проверки станет не хватать, сработает assert (только в debug),
                // который точно укажет на проблему (не снижайте за !!, пожалуйста - здесь он уместен)

                tickets[id].let {
                    assert(it != null)
                    it!!.seatTaken = value
                }
            }

        override fun equals(other: Any?): Boolean {
            return other is TicketEntity && id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    inner class UserEntity internal constructor(override val id: Int) : IUserEntity {
        override val isDeleted: Boolean
            get() = !users.containsKey(id)

        override fun delete() {
            if (isDeleted) {
                throw AccessDeletedException("Attempted to delete an entity mapping to a non-existing object")
            }
            users.remove(id)
        }

        override val login: String
            get() = users[id]?.login
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override val password: String
            get() = users[id]?.password
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")
        override val isAdmin: Boolean
            get() = users[id]?.isAdmin
                ?: throw AccessDeletedException("Attempted to access a field of an entity mapping to a non-existing object")

        override fun equals(other: Any?): Boolean {
            return other is UserEntity && id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    override fun createFilm(name: String, description: String, duration: Int): IFilmEntity {
        for (film in getFilms()) {
            if (film.name == name) {
                throw UniqueConstraintBrokenException("Фильм с таким названием уже существует")
            }
        }
        return with(filmsAutoIncrement++) {
            films[this] = FilmData(name, description, duration)
            FilmEntity(this)
        }
    }

    override fun createSeance(filmId: Int, startTime: LocalDateTime): ISeanceEntity {
        return with(seancesAutoIncrement++) {
            seances[this] = SeanceData(filmId, startTime)
            SeanceEntity(this)
        }
    }

    override fun createTicket(seanceId: Int, row: Int, seat: Int): ITicketEntity {
        for (ticket in getTickets()) {
            if (ticket.seance.id == seanceId && ticket.row == row && ticket.seat == seat) {
                throw UniqueConstraintBrokenException("Билет на это место уже продан")
            }
        }
        return with(ticketsAutoIncrement++) {
            tickets[this] = TicketData(seanceId, row, seat)
            TicketEntity(this)
        }
    }

    override fun createUser(login: String, password: String, isAdmin: Boolean): IUserEntity {
        for (user in getUsers()) {
            if (user.login == login) {
                throw UniqueConstraintBrokenException("Такой пользователь уже есть")
            }
        }
        return with(usersAutoIncrement++) {
            users[this] = UserData(login, hash(password), isAdmin)
            UserEntity(this)
        }
    }

    override fun getFilm(id: Int): IFilmEntity? {
        return films[id]?.let { FilmEntity(id) }
    }

    override fun getSeance(id: Int): ISeanceEntity? {
        return seances[id]?.let { SeanceEntity(id) }
    }

    override fun getTicket(id: Int): ITicketEntity? {
        return tickets[id]?.let { TicketEntity(id) }
    }

    override fun getUser(id: Int): IUserEntity? {
        return users[id]?.let { UserEntity(id) }
    }

    override fun getFilms(): Sequence<IFilmEntity> {
        return films.keys.asSequence().map { FilmEntity(it) }
    }

    override fun getSeances(): Sequence<ISeanceEntity> {
        return seances.keys.asSequence().map { SeanceEntity(it) }
    }

    override fun getTickets(): Sequence<ITicketEntity> {
        return tickets.keys.asSequence().map { TicketEntity(it) }
    }

    override fun getUsers(): Sequence<IUserEntity> {
        return users.keys.asSequence().map { UserEntity(it) }
    }

    // Сохранение данных, происходит гарантированно при использовании use
    override fun close() {
        assert(saver != null) { "Saver was never set" }
        saver?.save(this)
    }

    // Привязка saver-а к объекту базы данных
    fun boundSaver(saver: IDataSaver) {
        this.saver = saver
    }
}
