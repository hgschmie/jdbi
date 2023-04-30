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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.array.SqlArrayMapperFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.config.internal.JdbiConfigList;
import org.jdbi.v3.core.config.internal.JdbiConfigMap;
import org.jdbi.v3.core.enums.internal.EnumMapperFactory;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Alpha;

/**
 * Configuration registry for {@link ColumnMapperFactory} instances.
 */
public final class ColumnMappers implements JdbiConfig<ColumnMappers> {

    private final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors;
    private JdbiConfigList<QualifiedColumnMapperFactory> factories;
    private JdbiConfigMap<QualifiedType<?>, Optional<? extends ColumnMapper<?>>> cache;

    private boolean coalesceNullPrimitivesToDefaults = true;
    private ConfigRegistry registry;

    public ColumnMappers() {
        this.inferenceInterceptors = new JdbiInterceptionChainHolder<>(InferredColumnMapperFactory::new);
        this.factories = JdbiConfigList.create();
        this.cache = JdbiConfigMap.create();

        register(new SqlArrayMapperFactory());
        register(new JavaTimeMapperFactory());
        register(new SqlTimeMapperFactory());
        register(new InternetMapperFactory());
        register(new EssentialsMapperFactory());
        register(new BoxedMapperFactory());
        register(new PrimitiveMapperFactory());
        register(new OptionalMapperFactory());
        register(new EnumMapperFactory());
        register(new NVarcharMapper());
    }

    private ColumnMappers(ColumnMappers that) {
        this.factories = that.factories;
        this.cache = that.cache;
        this.inferenceInterceptors = new JdbiInterceptionChainHolder<>(that.inferenceInterceptors);
        this.coalesceNullPrimitivesToDefaults = that.coalesceNullPrimitivesToDefaults;
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the {@link JdbiInterceptionChainHolder} for the ColumnMapper inference. This chain allows registration of custom interceptors to change the standard
     * type inference for the {@link ColumnMappers#register(ColumnMapper)} method.
     */
    @Alpha
    public JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> getInferenceInterceptors() {
        return inferenceInterceptors;
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     * <p>
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the column mapper
     * @return this
     * @throws UnsupportedOperationException if the ColumnMapper is not a concretely parameterized type
     */
    public ColumnMappers register(ColumnMapper<?> mapper) {
        QualifiedColumnMapperFactory factory = inferenceInterceptors.process(mapper);

        return this.register(factory);
    }

    /**
     * Register a column mapper for a given explicit {@link GenericType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param <T> the type
     * @param type the generic type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public <T> ColumnMappers register(GenericType<T> type, ColumnMapper<T> mapper) {
        return this.register(ColumnMapperFactory.of(type.getType(), mapper));
    }

    /**
     * Register a column mapper for a given explicit {@link Type}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public ColumnMappers register(Type type, ColumnMapper<?> mapper) {
        return this.register(ColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper for a given {@link QualifiedType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public <T> ColumnMappers register(QualifiedType<T> type, ColumnMapper<T> mapper) {
        return this.register(QualifiedColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return this
     */
    public ColumnMappers register(ColumnMapperFactory factory) {
        return register(QualifiedColumnMapperFactory.adapt(factory));
    }

    /**
     * Register a qualified column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the qualified column mapper factory
     * @return this
     */
    public ColumnMappers register(QualifiedColumnMapperFactory factory) {
        this.factories = factories.addFirst(factory);
        this.cache = cache.clear();
        return this;
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<ColumnMapper<T>> findFor(Class<T> type) {
        ColumnMapper<T> mapper = (ColumnMapper<T>) findFor((Type) type).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<ColumnMapper<T>> findFor(GenericType<T> type) {
        ColumnMapper<T> mapper = (ColumnMapper<T>) findFor(type.getType()).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findFor(Type type) {
        return findFor(QualifiedType.of(type)).map(m -> (ColumnMapper<?>) m);
    }

    /**
     * Obtain a column mapper for the given qualified type.
     *
     * @param type the qualified target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Optional<ColumnMapper<T>> findFor(QualifiedType<T> type) {
        if (cache.hasKey(type)) {
            return (Optional<ColumnMapper<T>>) cache.getElement(type);
        }

        Optional<ColumnMapper<T>> mapper = (Optional) factories.stream()
                .flatMap(factory -> JdbiOptionals.stream(factory.build(type, registry)))
                .findFirst();

        mapper.ifPresent(m -> m.init(registry));

        this.cache = cache.putElement(type, mapper);

        return mapper;
    }

    /**
     * Returns true if database {@code null} values should be transformed to the default value for primitives.
     *
     * @return {@code true} if database {@code null}s should translate to the Java defaults for primitives, or throw an exception otherwise.
     *
     * Default value is true: nulls will be coalesced to defaults.
     */
    public boolean getCoalesceNullPrimitivesToDefaults() {
        return coalesceNullPrimitivesToDefaults;
    }

    public void setCoalesceNullPrimitivesToDefaults(boolean coalesceNullPrimitivesToDefaults) {
        this.coalesceNullPrimitivesToDefaults = coalesceNullPrimitivesToDefaults;
    }

    @Override
    public ColumnMappers createCopy() {
        return new ColumnMappers(this);
    }
}
