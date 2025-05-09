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
package org.jdbi.v3.jpa.internal;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.persistence.Column;

import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.jpa.EntityMemberAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class JpaMember {
    private static final Qualifiers QUALIFIERS = new Qualifiers();
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMember.class);

    private final Class<?> clazz;
    private final String columnName;
    private final QualifiedType<?> qualifiedType;
    private final Getter accessor;
    private final Setter mutator;

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    JpaMember(Class<?> clazz, Column column, Field field) {
        this.clazz = requireNonNull(clazz);
        this.columnName = nameOf(column, field.getName());
        this.qualifiedType = QualifiedType.of(field.getGenericType()).withAnnotations(QUALIFIERS.findFor(field));
        field.setAccessible(true);
        this.accessor = field::get;
        this.mutator = field::set;
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    JpaMember(Class<?> clazz, Column column, PropertyDescriptor property) {
        this.clazz = requireNonNull(clazz);
        this.columnName = nameOf(column, property.getName());

        Method getter = property.getReadMethod();
        Method setter = property.getWriteMethod();
        Parameter setterParam = setter.getParameters()[0];
        getter.setAccessible(true);
        setter.setAccessible(true);

        this.qualifiedType = QualifiedType.of(getter.getGenericReturnType())
            .withAnnotations(QUALIFIERS.findFor(getter, setter, setterParam));

        this.accessor = getter::invoke;
        this.mutator = setter::invoke;
    }

    public String getColumnName() {
        return columnName;
    }

    public QualifiedType<?> getQualifiedType() {
        return qualifiedType;
    }

    public Type getType() {
        return qualifiedType.getType();
    }

    public Object read(Object obj) throws IllegalAccessException, InvocationTargetException {
        return accessor.get(obj);
    }

    public void write(Object obj, Object value) {
        LOGGER.debug("write {}/{}/{}/{}", clazz, columnName, qualifiedType, value);

        try {
            mutator.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new EntityMemberAccessException("Couldn't set " + clazz + "#" + columnName, e);
        }
    }

    private static String nameOf(Column column, String memberName) {
        return Optional.ofNullable(column)
                .map(Column::name)
                .filter(name -> name.length() > 0)
                .orElse(memberName);
    }

    @FunctionalInterface
    interface Getter {
        Object get(Object obj) throws IllegalAccessException, InvocationTargetException;
    }

    @FunctionalInterface
    interface Setter {
        void set(Object obj, Object value) throws IllegalAccessException, InvocationTargetException;
    }
}
