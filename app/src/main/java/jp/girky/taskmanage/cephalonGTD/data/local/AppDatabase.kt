package jp.girky.taskmanage.cephalonGTD.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem

@Database(entities = [TaskItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
