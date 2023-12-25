package data.save

import data.access.IDataAccess

// Объект для сохранения базы данных
interface IDataSaver {
    fun save(data: IDataAccess)
}