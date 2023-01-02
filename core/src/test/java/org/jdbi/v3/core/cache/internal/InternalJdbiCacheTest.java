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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;
import org.jdbi.v3.core.cache.JdbiCacheStats;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InternalJdbiCacheTest {

    protected JdbiCacheBuilder<String, String> builder;

    TestingCacheLoader cacheLoader = new TestingCacheLoader();

    @BeforeEach
    void beforeEach() {
        this.builder = InternalJdbiCache.builder();
    }

    @Test
    void testWithGlobalLoader() {
        JdbiCache<String, String> cache = builder.buildWithLoader(cacheLoader);

        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // cache hit. Same value, no creation event.
        value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        String key2 = UUID.randomUUID().toString();

        // cache creation. second creation event.
        String value2 = cache.get(key2);
        assertThat(cacheLoader.created()).isEqualTo(2);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));
    }

    @Test
    void testWithDirectLoader() {
        JdbiCache<String, String> cache = builder.build();

        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // cache hit. Same value, no creation event.
        value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        String key2 = UUID.randomUUID().toString();

        // cache creation. second creation event.
        String value2 = cache.getWithLoader(key2, cacheLoader);
        assertThat(cacheLoader.created()).isEqualTo(2);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));
    }

    @Test
    void testInternalCacheExpunge() {
        int size = 10;
        JdbiCache<String, String> cache = builder.maxSize(size).buildWithLoader(cacheLoader);

        // tests specific InternalJdbiCache FIFO behavior. Skip for all other cache types.
        Assumptions.assumeTrue(cache instanceof InternalJdbiCache);

        assertThat(cache.getStats().maxSize()).isEqualTo(size);

        String[] keys = new String[size * 2];

        for (int i = 0; i < keys.length; i++) {
            keys[i] = UUID.randomUUID().toString();
            cache.get(keys[i]);
        }

        assertThat(cacheLoader.created()).isEqualTo(size * 2);

        JdbiCacheStats stats = cache.getStats();
        assertThat(stats.cacheSize()).isEqualTo(stats.maxSize());

        // test FIFO behavior. Change this part if the algorithm changes

        // last elements are still in the cache
        for (int i = keys.length - 1; i >= size; i--) {
            cache.get(keys[i]);
            // no additional cache hit
            assertThat(cacheLoader.created()).isEqualTo(size * 2);
        }

        // first were expunged
        for (int i = 0; i < size; i++) {
            int creations = cacheLoader.created();
            cache.get(keys[i]);
            // additional cache hit
            assertThat(creations + 1).isEqualTo(cacheLoader.created());
        }
    }


    static class TestingCacheLoader implements JdbiCacheLoader<String, String> {

        private final Map<String, String> values = new HashMap<>();
        private int creations = 0;


        @Override
        public String create(String key) {
            String value = UUID.randomUUID().toString();
            values.put(key, value);
            creations++;
            return value;
        }

        public int created() {
            return creations;
        }

        public String checkKey(String key) {
            return values.get(key);
        }
    }
}
