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
package org.jdbi.v3.core.cache.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;
import org.jdbi.v3.core.cache.JdbiCacheStats;

public final class InternalJdbiCache<K, V> implements JdbiCache<K, V> {

    private final ConcurrentMap<K, V> cache;
    private final ConcurrentLinkedQueue<K> expungeQueue;
    private final JdbiCacheLoader<K, V> cacheLoader;

    private final int maxSize;

    private InternalJdbiCache(Builder<K, V> builder, JdbiCacheLoader<K, V> cacheLoader) {
        this.cache = new ConcurrentHashMap<>();
        this.expungeQueue = new ConcurrentLinkedQueue<>();
        this.cacheLoader = cacheLoader;

        this.maxSize = builder.maxSize;

    }


    @Override
    public V get(K key) {
        try {
            if (cacheLoader == null) {
                return cache.get(key);
            } else {
                return cache.computeIfAbsent(key, compute(cacheLoader)::create);
            }
        } finally {
            expunge();
        }
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> cacheLoader) {
        try {
            return cache.computeIfAbsent(key, compute(cacheLoader)::create);
        } finally {
            expunge();
        }
    }

    @Override
    public JdbiCacheStats getStats() {
        int size = cache.size();
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

    private JdbiCacheLoader<K, V> compute(JdbiCacheLoader<K, V> delegate) {
        if (delegate == null) {
            return null;
        }
        if (maxSize <= 0) {
            return delegate;
        }

        return key -> {
            V value = delegate.create(key);
            if (value != null) {
                expungeQueue.add(key);
            }
            return value;
        };
    }

    private void expunge() {
        if (maxSize <= 0) {
            // unbounded
            return;
        }

        // quick and dirty implementation that runs in
        // linear time to remove a few elements from the cache
        // if the cache really exceeded its size. This is a simple
        // FIFO. Anything more sophisticated should probably use a better
        // implementation of the cache code.
        int cacheSize = expungeQueue.size();
        while (cacheSize > maxSize) {
            K key = expungeQueue.poll();
            if (key == null) {
                // expunge queue is empty. This should never happen.
                // Reset the cache as well.
                cache.clear();
                return;
            }
            cache.remove(key);
            // one element removed.
            cacheSize--;
        }
    }

    public static <K, V> InternalJdbiCache.Builder<K, V> builder() {
        return new InternalJdbiCache.Builder<>();
    }

    public static final class Builder<K, V> implements JdbiCacheBuilder<K, V> {

        private int maxSize = -1;

        @Override
        public JdbiCache<K, V> build() {
            return new InternalJdbiCache<>(this, null);
        }

        @Override
        public JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> loader) {
            return new InternalJdbiCache<>(this, loader);
        }

        @Override
        public InternalJdbiCache.Builder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
    }
}
