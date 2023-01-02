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
package org.jdbi.v3.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;
import org.jdbi.v3.core.cache.JdbiCacheStats;

public class CaffeineCache<K, V> implements JdbiCache<K, V> {

    private final Cache<K, V> cache;
    private final int maxSize;
    private final boolean isLoadingCache;

    public static <K, V> JdbiCacheBuilder<K, V> builder() {
        return new CaffeineCacheBuilder<>();
    }

    CaffeineCache(CaffeineCacheBuilder<K, V> cacheBuilder) {
        this.cache = cacheBuilder.getCaffeine().build();
        this.maxSize = cacheBuilder.getMaxSize();
        this.isLoadingCache = false;
    }

    CaffeineCache(CaffeineCacheBuilder<K, V> cacheBuilder, JdbiCacheLoader<K, V> cacheLoader) {
        this.cache = cacheBuilder.getCaffeine().build(cacheLoader::create);
        this.maxSize = cacheBuilder.getMaxSize();
        this.isLoadingCache = true;
    }

    @Override
    public V get(K key) {
        if (isLoadingCache) {
            LoadingCache<K, V> loadingCache = (LoadingCache<K, V>) cache;
            return loadingCache.get(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> loader) {
        return cache.get(key, loader::create);
    }

    @Override
    public JdbiCacheStats getStats() {
        int size = (int) cache.estimatedSize();
        return new JdbiCacheStats() {
            @Override
            public int cacheSize() {
                return size;
            }

            @Override
            public int maxSize() {
                return maxSize;
            }
        };
    }
}
