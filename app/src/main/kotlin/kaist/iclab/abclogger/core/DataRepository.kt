package kaist.iclab.abclogger.core

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.exception.*
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kaist.iclab.abclogger.collector.MyObjectBox
import kaist.iclab.abclogger.commons.EntityError
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class DataRepository(private val context: Context) {
    private val mutex = Mutex()
    private var boxStore: BoxStore? = null

    private fun buildBoxStore(versionOrder: Int, dbName: String): BoxStore {
        val size = UNIT_SIZE_IN_KB * versionOrder.coerceAtLeast(1)

        return MyObjectBox.builder()
            .androidContext(context)
            .maxSizeInKByte(size)
            .name(dbName)
            .build()
    }

    suspend fun <R> op(block: suspend BoxStore.() -> R): R? {
        var throwable: Throwable? = null

        for (i in (0 until MAX_RETRIES)) {
            val dbName = "$DEFAULT_DB_NAME-${Preference.dbNameSuffix}"
            val dbVersion = Preference.dbVersion
            try {
                boxStore = mutex.withLock {
                    boxStore?.takeUnless { it.isClosed } ?: buildBoxStore(
                        versionOrder = dbVersion, dbName = dbName
                    )
                }

                return block.invoke(boxStore!!)
            } catch (e: DbFullException) {
                throwable = e
                Preference.dbVersion++
            } catch (e: FileCorruptException) {
                throwable = e
                Preference.dbNameSuffix++
            } catch (e: DbShutdownException) {
                throwable = e
                Log.e(javaClass, e)
            } catch (e: DbDetachedException) {
                throwable = e
                Log.e(javaClass, e)
            } catch (e: Exception) {
                throwable = e
                Log.e(javaClass, e)
            }
            delay(1000)
        }

        if (throwable != null) {
            Log.e(javaClass, throwable, report = true)
            if (throwable is DbException) {
                throw EntityError.fromDbException(throwable)
            } else {
                throw throwable
            }
        }

        return null
    }

    suspend fun flush() = op {
        close()
        deleteAllFiles()
    }

    suspend fun sizeOnDisk(): Long = op { sizeOnDisk() } ?: 0L

    suspend inline fun <reified T : Any> put(entity: T): Long = op {
        boxFor<T>().put(entity)
    } ?: -1L

    suspend inline fun <reified T : Any> query(crossinline block: QueryBuilder<T>.() -> Unit): Query<T>? =
        op {
            boxFor<T>().query(block)
        }

    suspend inline fun <reified T : Any> findFirst(crossinline block: QueryBuilder<T>.() -> Unit): T? =
        op {
            boxFor<T>().query(block).findFirst()
        }

    suspend inline fun <reified T : Any> find(crossinline block: QueryBuilder<T>.() -> Unit): List<T> =
        op {
            boxFor<T>().query(block).find()
        } ?: listOf()

    suspend inline fun <reified T : Any> find(
        offset: Long,
        limit: Long,
        crossinline block: QueryBuilder<T>.() -> Unit
    ): List<T> = op {
        boxFor<T>().query(block).find(offset, limit)
    } ?: listOf()

    suspend inline fun <reified T : Any> find(
        offset: Long,
        limit: Long
    ): List<T> = op {
        boxFor<T>().query().build().find(offset, limit)
    } ?: listOf()

    suspend fun countAll(): Long = op {
        allEntityClasses.sumOf {
            try {
                boxFor(it).count()
            } catch (e: Exception) {
                0L
            }
        }
    } ?: 0

    suspend inline fun <reified T : Any> count(): Long = op {
        boxFor<T>().count()
    } ?: -1

    suspend inline fun <reified T : Any> count(crossinline block: QueryBuilder<T>.() -> Unit): Long =
        op {
            boxFor<T>().query(block).count()
        } ?: -1

    suspend inline fun <reified T : Any> remove(entity: T) = op {
        boxFor<T>().remove(entity)
    }

    suspend inline fun <reified T : Any> remove(entities: Collection<T>) = op {
        boxFor<T>().remove(entities)
    }

    suspend inline fun <reified T : Any> remove(vararg entity: T) = op {
        boxFor<T>().remove(*entity)
    }

    suspend inline fun <reified T : Any> remove(crossinline block: QueryBuilder<T>.() -> Unit): Long =
        op { boxFor<T>().query(block).remove() } ?: -1

    suspend inline fun <reified T : Any> removeById(id: Long) = op {
        boxFor<T>().remove(id)
    }

    suspend inline fun <reified T : Any> removeById(ids: Collection<Long>) = op {
        boxFor<T>().removeByIds(ids)
    }

    suspend inline fun <reified T : Any> removeById(vararg id: Long) = op {
        boxFor<T>().remove(*id)
    }

    suspend inline fun <reified T : Any> get(id: Long): T? = op {
        boxFor<T>().get(id)
    }

    suspend inline fun <reified T : Any> get(ids: Collection<Long>): List<T> = op {
        boxFor<T>().get(ids)
    } ?: listOf()

    suspend inline fun <reified T : Any> get(vararg id: Long): List<T> = op {
        boxFor<T>().get(id)
    } ?: listOf()

    companion object {
        private const val MAX_RETRIES = 30
        private const val UNIT_SIZE_IN_KB = 1024 * 1024L
        private const val DEFAULT_DB_NAME = "ABC-LOGGER-DB"
    }
}





