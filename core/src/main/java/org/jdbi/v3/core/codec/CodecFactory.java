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
package org.jdbi.v3.core.codec;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.google.errorprone.annotations.ThreadSafe;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

import static java.util.Objects.requireNonNull;

/**
 * CodecFactory provides column mappers and arguments for bidirectional mapping types to database columns.
 * <p>
 * This class is immutable and thread safe.
 * <p><i>Alpha: this class as public API is redundant with the existing way of managing mappers: we should try to combine them
 * to make the new public API as small as possible</i>
 */
@ThreadSafe
@Beta
public class CodecFactory implements QualifiedColumnMapperFactory, QualifiedArgumentFactory.Preparable {

    /**
     * Map of all known codecs in this factory.
     * <p>
     * ALPHA: the fact that this is a Map from type to Codec makes it hard to implement Codecs that target wildcard
     * or varying types e.g. mapping both {@code Sub<T>} and {@code Super<T>} with one codec.
     * It might be nice to re-imagine this as itself a JdbiPlugin and have it do all registration through the existing flows.
     */
    protected final ConcurrentMap<QualifiedType<?>, Codec<?>> codecMap = new ConcurrentHashMap<>();

    /**
     * Returns a builder for fluent API.
     * @return A {@link CodecFactory.Builder} instance.
     */
    public static Builder builder() {
        return new Builder(CodecFactory::new);
    }

    /**
     * Creates a {@link CodecFactory} for a single type.
     * @param type The type for which the factory is created.
     * @param codec The {@link Codec} to use.
     * @return A new {@link CodecFactory} that will be used if the given type is requested.
     */
    public static CodecFactory forSingleCodec(QualifiedType<?> type, Codec<?> codec) {
        return new CodecFactory(Collections.singletonMap(type, codec));
    }

    /**
     * Create a new CodecFactory.
     */
    public CodecFactory(final Map<QualifiedType<?>, Codec<?>> codecMap) {
        requireNonNull(codecMap, "codecMap is null");
        this.codecMap.putAll(codecMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Optional<Function<Object, Argument>> prepare(final QualifiedType<?> type, final ConfigRegistry config) {
        return Optional.of(type).map(this::resolveType).map(key -> (Function<Object, Argument>) key.getArgumentFunction(config));
    }

    /**
     * @deprecated no longer used
     */
    @Deprecated(since = "3.39.0", forRemoval = true)
    @Override
    public final Collection<QualifiedType<?>> prePreparedTypes() {
        return codecMap.keySet();
    }

    @Override
    public final Optional<Argument> build(final QualifiedType<?> type, final Object value, final ConfigRegistry config) {
        return prepare(type, config).map(f -> f.apply(value));
    }

    @Override
    public final Optional<ColumnMapper<?>> build(final QualifiedType<?> type, final ConfigRegistry config) {
        return Optional.of(type).map(this::resolveType).map(c -> c.getColumnMapper(config));
    }

    /**
     * Extension point for type resolution.
     *
     * @param qualifiedType Requested type
     * @return A {@link Codec} for the requested type or null if no codec is suitable.
     */
    protected Codec<?> resolveType(QualifiedType<?> qualifiedType) {
        return codecMap.get(qualifiedType);
    }

    /**
     * Fluent Builder for {@link CodecFactory}.
     */
    @Beta
    public static final class Builder {

        private final Map<QualifiedType<?>, Codec<?>> codecMap = new HashMap<>();

        private final Function<Map<QualifiedType<?>, Codec<?>>, CodecFactory> factory;

        public Builder(Function<Map<QualifiedType<?>, Codec<?>>, CodecFactory> factory) {
            this.factory = requireNonNull(factory, "factory is null");
        }

        /**
         * Add a codec for a {@link QualifiedType}.
         */
        public Builder addCodec(final QualifiedType<?> type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(type, codec);

            return this;
        }

        /**
         * Add a codec for a {@link Type}.
         */
        public Builder addCodec(final Type type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(QualifiedType.of(type), codec);

            return this;
        }

        /**
         * Add a codec for a {@link GenericType}.
         */
        public Builder addCodec(final GenericType<?> type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(QualifiedType.of(type.getType()), codec);

            return this;
        }

        public CodecFactory build() {
            return factory.apply(codecMap);
        }
    }

}
