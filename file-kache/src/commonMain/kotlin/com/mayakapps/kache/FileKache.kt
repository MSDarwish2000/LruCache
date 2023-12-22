package com.mayakapps.kache

import com.mayakapps.kache.FileKache.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okio.Path
import okio.Path.Companion.toPath

/**
 * A persistent coroutine-safe [ContainerKache] implementation that uses [Okio](https://square.github.io/okio/) to
 * store files under the hood and exposes a simple path-based API.
 *
 * It uses a journal file to keep track of the cache state and to ensure that the cache is always in a consistent state.
 *
 * It can be built using the following syntax:
 * ```
 * val cache = OkioFileKache(directoryPath = "cache", maxSize = 100L * 1024L * 1024L) {
 *     strategy = KacheStrategy.LRU
 *     // ...
 * }
 * ```
 *
 * @see Configuration
 */
public class FileKache internal constructor(
    private val baseKache: ContainerKache<String, Path>,
    private val creationScope: CoroutineScope,
) : ContainerKache<String, String> {

    override suspend fun get(key: String): String? =
        baseKache.get(key)?.toString()

    override suspend fun getIfAvailable(key: String): String? =
        baseKache.getIfAvailable(key)?.toString()

    override suspend fun getOrPut(key: String, creationFunction: suspend (String) -> Boolean): String? =
        baseKache.getOrPut(key) { file ->
            creationFunction(file.toString())
        }?.toString()

    override suspend fun put(key: String, creationFunction: suspend (String) -> Boolean): String? =
        baseKache.put(key) { file ->
            creationFunction(file.toString())
        }?.toString()

    override suspend fun putAsync(key: String, creationFunction: suspend (String) -> Boolean): Deferred<String?> =
        creationScope.async {
            baseKache.putAsync(key) { file ->
                creationFunction(file.toString())
            }.await()?.toString()
        }

    override suspend fun remove(key: String): Unit =
        baseKache.remove(key)

    override suspend fun clear(): Unit =
        baseKache.clear()

    override suspend fun close(): Unit =
        baseKache.close()

    /**
     * Configuration for [FileKache]. It is used as a receiver of [FileKache] builder which is [invoke].
     */
    public class Configuration(

        /**
         * The path of the directory where the cache files and the journal are stored.
         */
        public var directoryPath: String,

        /**
         * The maximum size of the cache in bytes.
         */
        public var maxSize: Long,

        /**
         * The strategy used to evict entries from the cache.
         */
        public var strategy: KacheStrategy = KacheStrategy.LRU,

        /**
         * The coroutine dispatcher used for executing `creationFunction` of put requests.
         */
        public var creationScope: CoroutineScope = CoroutineScope(getIODispatcher()),

        /**
         * The version of the cache. This is useful to invalidate the cache when the format of the data stored in the
         * cache changes.
         */
        public var cacheVersion: Int = 1,

        /**
         * The [KeyTransformer] used to transform the keys before they are used to store and retrieve data. It is
         * needed to avoid using invalid characters in the file names.
         */
        public var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )
}

/**
 * Creates a new [FileKache] instance with the given [directoryPath] and [maxSize] and is configured by
 * [configuration].
 *
 * @see FileKache.Configuration
 */
public suspend fun FileKache(
    directoryPath: String,
    maxSize: Long,
    configuration: Configuration.() -> Unit = {},
): FileKache {
    val config = Configuration(directoryPath, maxSize).apply(configuration)

    val baseKache = OkioFileKache(
        directory = config.directoryPath.toPath(),
        maxSize = config.maxSize,
    ) {
        strategy = config.strategy
        creationScope = config.creationScope
        cacheVersion = config.cacheVersion
        keyTransformer = config.keyTransformer
    }

    return FileKache(baseKache, config.creationScope)
}
