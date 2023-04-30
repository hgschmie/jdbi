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
package org.jdbi.v3.core.qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.config.internal.ConfigCache;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.internal.CollectionCollectors;

/**
 * Utility class for type qualifiers supported by Jdbi core.
 */
public final class Qualifiers implements JdbiConfig<Qualifiers> {
    private static final ConfigCache<AnnotatedElement[], Set<Annotation>> QUALIFIER_CACHE = ConfigCaches.declare(
            elements -> elements.length == 1 ? elements[0] : new HashSet<>(Arrays.asList(elements)),
            (Function<AnnotatedElement[], Set<Annotation>>) Qualifiers::getQualifiers);
    private static final ConfigCache<AnnotatedElement, QualifiedType<?>> QUALIFIED_TYPE_CACHE = ConfigCaches.declare(
            type -> QualifiedType.of((Type) type).withAnnotations(getQualifiers(type)));
    private ConfigRegistry registry;

    public Qualifiers() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    public <ELEM extends AnnotatedElement & Type> QualifiedType<?> qualifiedTypeOf(ELEM type) {
        if (registry == null) {
            return QualifiedType.of(type).withAnnotations(getQualifiers(type));
        } else {
            return QUALIFIED_TYPE_CACHE.get(type, registry);
        }
    }

    /**
     * Returns the set of qualifying annotations on the given elements.
     * @param elements the annotated elements. Null elements are ignored.
     * @return the set of qualifying annotations on the given elements.
     */
    public Set<Annotation> findFor(AnnotatedElement... elements) {
        if (registry == null) {
            return getQualifiers(elements);
        }
        return QUALIFIER_CACHE.get(elements, registry);
    }

    private static Set<Annotation> getQualifiers(AnnotatedElement... elements) {
        return Arrays.stream(elements)
                .filter(Objects::nonNull)
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Arrays::stream)
                .filter(anno -> anno.annotationType().isAnnotationPresent(Qualifier.class))
                .collect(CollectionCollectors.toUnmodifiableSet());
    }

    @Override
    public Qualifiers createCopy() {
        return new Qualifiers();
    }
}
