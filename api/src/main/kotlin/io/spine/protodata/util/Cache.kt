/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.protodata.util

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.runBlocking

/**
 * Abstract base for classes caching instances of [V] created for keys of the type [K].
 *
 * @param K The type in response to which cached values are created.
 * @param V The type of values stored in the cache.
 *
 * @param initialCapacity The initial capacity of the cache.
 */
public abstract class Cache<K: Any, V: Any>(private val initialCapacity: Int = 100) {

    private val syncCache = Caffeine.newBuilder()
        .initialCapacity(this@Cache.initialCapacity)
        .build<K, V>()

    /**
     * Creates an instance of the cached value for the given key.
     *
     * @param key The key for creating the value.
     * @param param Additional parameter for creating the value.
     * @see get
     */
    protected abstract fun create(key: K, param: Any?): V

    /**
     * Obtains or creates an instance for the given file.
     *
     * @param key The key for obtaining the value.
     * @param param Additional parameter for creating the value.
     * @see create
     */
    protected fun get(key: K, param: Any? = null): V {
        return synchronized(this) {
            runBlocking {
                syncCache.get(key) {
                    create(key, param)
                }
            }
        }
    }

    /**
     * Clears the cache.
     *
     * Clearing the cache may be useful in between tests to avoid stale instances.
     */
    public fun clearCache() {
        synchronized(this) {
            syncCache.invalidateAll()
        }
    }
}
