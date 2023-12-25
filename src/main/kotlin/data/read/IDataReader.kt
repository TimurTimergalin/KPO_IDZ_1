package data.read

import data.access.IDataAccess

// Фабричный метод для IDataAccess
// Читает данные из файла и создаёт на его основе объект для доступа к данным
interface IDataReader {
    fun read(filename: String): IDataAccess
}