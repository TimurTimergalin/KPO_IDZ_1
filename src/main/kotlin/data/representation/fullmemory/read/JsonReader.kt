package data.representation.fullmemory.read

import data.access.IDataAccess
import data.read.IDataReader
import data.representation.fullmemory.FullMemoryAccess
import data.representation.fullmemory.save.JsonSaver
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import java.io.File

// Считывает базу данных из json-файла в оперативную память
class JsonReader : IDataReader {
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(FullMemoryAccess::class, FullMemoryAccess.JsonSerializer())
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun read(filename: String): IDataAccess {
        val saver = JsonSaver(filename, json)
        val file = File(filename)
        val data = try {
            if (file.exists()) file.inputStream().use {
                json.decodeFromStream<FullMemoryAccess>(it)
            } else {
                println("База данных не найдена. Создаём новую")
                FullMemoryAccess()
            }
        } catch (e: IllegalArgumentException) {
            println("Не удалось прочитать базу данных. Перезаписываем данные")
            FullMemoryAccess()
        }
        data.boundSaver(saver)
        return data
    }
}