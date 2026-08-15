package jp.girky.taskmanage.cephalonGTD.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.girky.taskmanage.cephalonGTD.ai.AiCoreEngineImpl
import jp.girky.taskmanage.cephalonGTD.ai.AiEngine
import jp.girky.taskmanage.cephalonGTD.ai.MockAiEngineImpl
import jp.girky.taskmanage.cephalonGTD.data.local.AppDatabase
import jp.girky.taskmanage.cephalonGTD.data.local.TaskDao
import jp.girky.taskmanage.cephalonGTD.data.repository.TaskRepository
import jp.girky.taskmanage.cephalonGTD.data.repository.TaskRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cephalon_gtd.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAiEngine(
        aiCoreEngineImpl: AiCoreEngineImpl
    ): AiEngine
}
