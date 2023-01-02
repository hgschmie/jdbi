package org.jdbi.v3.caffeine;

import org.jdbi.v3.core.cache.internal.InternalJdbiCacheTest;
import org.junit.jupiter.api.BeforeEach;

class CaffeineCacheTest extends InternalJdbiCacheTest {

    @BeforeEach
    void beforeEach() {
        this.builder = CaffeineCache.builder();
    }
}
