package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.data.di.DefaultDispatcher
import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.core.network.LogRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Source of log data for the feature layer. */
interface LogRepository {

    /**
     * Cold flow that emits the dataset once and completes. The first collection loads it; later
     * collections get the cached copy, so searching never refetches.
     *
     * Failures arrive as [LogLoadException]; collect again to retry (failures are not cached).
     */
    fun fetchLogs(): Flow<LogDataset>
}

/**
 * Loads once, keeps the [LogDataset] in memory for the life of the process and guards against
 * two concurrent first loads with a [Mutex].
 */
@Singleton
class LogRepositoryImpl @Inject constructor(
    private val remote: LogRemoteDataSource,
    @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : LogRepository {

    private val mutex = Mutex()

    @Volatile
    private var cache: LogDataset? = null

    override fun fetchLogs(): Flow<LogDataset> = flow {
        emit(cache ?: loadOnce())
    }
        // Mapping and indexing 5,000 entries is CPU work; the network read hops to IO inside `remote`.
        .flowOn(dispatcher)
        // `catch` never intercepts cancellation of the collector.
        .catch { throw it as? LogLoadException ?: LogLoadException(LogLoadException.Reason.UNKNOWN, it) }

    private suspend fun loadOnce(): LogDataset = mutex.withLock {
        cache ?: remote.fetchLogs().toLogDataset().also { cache = it }
    }
}
