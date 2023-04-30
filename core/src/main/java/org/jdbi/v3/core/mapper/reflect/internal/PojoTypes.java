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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.config.internal.JdbiConfigMap;
import org.jdbi.v3.core.generic.GenericTypes;

public final class PojoTypes implements JdbiConfig<PojoTypes> {
    private JdbiConfigMap<Class<?>, PojoPropertiesFactory> factories;
    private ConfigRegistry registry;

    public PojoTypes() {
        this.factories = JdbiConfigMap.create();
    }

    private PojoTypes(PojoTypes that) {
        this.factories = that.factories;
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    public PojoTypes register(Class<?> key, PojoPropertiesFactory factory) {
        this.factories = factories.putElement(key, factory);
        return this;
    }

    public Optional<PojoProperties<?>> findFor(Type type) {
        return Optional.ofNullable(factories.getElement(GenericTypes.getErasedType(type)))
                .map(ppf -> ppf.create(type, registry));
    }

    @Override
    public PojoTypes createCopy() {
        return new PojoTypes(this);
    }
}
