package jp.girky.taskmanage.cephalonGTD.data.local

import androidx.room.TypeConverter
import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromWbsSubTaskList(value: List<WbsSubTask>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toWbsSubTaskList(value: String?): List<WbsSubTask> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String {
        return status?.name ?: TaskStatus.TODO.name
    }

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus {
        return try {
            if (value != null) TaskStatus.valueOf(value) else TaskStatus.TODO
        } catch (e: Exception) {
            TaskStatus.TODO
        }
    }

    @TypeConverter
    fun fromTaskContextTag(tag: TaskContextTag?): String {
        return tag?.name ?: TaskContextTag.OFFICE.name
    }

    @TypeConverter
    fun toTaskContextTag(value: String?): TaskContextTag {
        return try {
            if (value != null) TaskContextTag.valueOf(value) else TaskContextTag.OFFICE
        } catch (e: Exception) {
            TaskContextTag.OFFICE
        }
    }
}
