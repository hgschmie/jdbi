/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.cache;

/**
 * Builder class for {@link JdbiCache} implementations.
 *
 * @param <K> A key type for the cache.
 * @param <V> A value type for the cache.
 */
public interface JdbiCacheBuilder<K, V> {

    /**
     * Creates an cache instance from the values in the builder.
     *
     * @return A cache instance.
     */
    JdbiCache<K, V> build();

    /**
     * Creates an cache instance from the values in the builder and a supplied cache loader.
     *
     * @param cacheLoader A {@link JdbiCacheLoader} instance that is used to create a new value if no value is currently stored in the cache.
     */
    JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> cacheLoader);

    /**
     * Sets an upper boundary to the cache size.
     *
     * @param maxSize Sets the maximum size of the cache. If the value is zero or negative, the cache is unbounded.
     *
     * @return The instance of the builder itself.
     */
    JdbiCacheBuilder<K, V> maxSize(int maxSize);
}
