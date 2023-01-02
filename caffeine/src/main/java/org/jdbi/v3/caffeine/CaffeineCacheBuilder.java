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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

public class CaffeineCacheBuilder<K, V> implements JdbiCacheBuilder<K, V> {

    private final Caffeine<Object, Object> caffeine;
    private int maxSize = -1;

    CaffeineCacheBuilder() {
        this.caffeine = Caffeine.newBuilder();
    }

    Caffeine<Object, Object> getCaffeine() {
        return caffeine;
    }

    @Override
    public JdbiCache<K, V> build() {
        return new CaffeineCache<>(this);
    }

    @Override
    public JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> cacheLoader) {
        return new CaffeineCache<>(this, cacheLoader);
    }

    @Override
    public JdbiCacheBuilder<K, V> maxSize(int maxSize) {
        this.maxSize = maxSize;
        caffeine.maximumSize(maxSize);
        return this;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
