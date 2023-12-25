package data.representation.fullmemory.save

import data.access.IDataAccess
import data.representation.fullmemory.FullMemoryAccess
import data.save.IDataSaver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


// Сохраняет базу данных в json-файл
class JsonSaver(private val path: String, private val json: Json) : IDataSaver {
    override fun save(data: IDataAccess) {
        assert(data is FullMemoryAccess) {"This saver is for FullMemoryAccessOnly"}
        File(path).printWriter().use {
            out -> out.println(json.encodeToString(data as FullMemoryAccess))
        }
    }
}