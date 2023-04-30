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
package org.jdbi.v3.core.collector;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.config.internal.JdbiConfigList;
import org.jdbi.v3.core.config.internal.JdbiConfigMap;

/**
 * Registry of collector factories.
 * Contains a set of collector factories, registered by the application.
 */
public final class JdbiCollectors implements JdbiConfig<JdbiCollectors> {
    private JdbiConfigList<CollectorFactory> factories;
    private JdbiConfigMap<Type, Optional<CollectorFactory>> factoryCache;

    public JdbiCollectors() {
        this.factories = JdbiConfigList.create();
        this.factoryCache = JdbiConfigMap.create();

        register(new MapCollectorFactory());
        register(new OptionalCollectorFactory());
        register(new ListCollectorFactory());
        register(new SetCollectorFactory());
        register(new OptionalPrimitiveCollectorFactory());
        register(new ArrayCollectorFactory());
        register(new EnumSetCollectorFactory());
    }

    private JdbiCollectors(JdbiCollectors that) {
        this.factoryCache = that.factoryCache;
        this.factories = that.factories;
    }

    /**
     * Register a new {@link CollectorFactory}.
     * @param factory A collector factory
     * @return this
     */
    public JdbiCollectors register(CollectorFactory factory) {
        this.factories = factories.addFirst(factory);

        this.factoryCache = factoryCache.clear();
        return this;
    }

    /**
     * Register a new {@link Collector} for the given type.
     * @param collectionType The type that this collector will return
     * @param collector A {@link Collector} implementation
     * @return this
     * @since 3.38.0
     * @see org.jdbi.v3.core.config.Configurable#registerCollector(CollectorFactory)
     */
    public JdbiCollectors registerCollector(Type collectionType, Collector<?, ?, ?> collector) {
        return register(CollectorFactory.collectorFactory(collectionType, collector));
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    public Optional<Collector<?, ?, ?>> findFor(Type containerType) {
        return findFactoryFor(containerType)
                .map(f -> f.build(containerType));
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    public Optional<Type> findElementTypeFor(Type containerType) {
        return findFactoryFor(containerType)
                .flatMap(f -> f.elementType(containerType));
    }

    private Optional<CollectorFactory> findFactoryFor(Type containerType) {
        if (factoryCache.hasKey(containerType)) {
            return factoryCache.getElement(containerType);
        }

        Optional<CollectorFactory> factory = factories.stream()
                .filter(f -> f.accepts(containerType))
                .findFirst();
        this.factoryCache = factoryCache.putElement(containerType, factory);
        return factory;
    }

    @Override
    public JdbiCollectors createCopy() {
        return new JdbiCollectors(this);
    }
}
