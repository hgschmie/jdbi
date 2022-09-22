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
package org.jdbi.v3.core.mapper.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class for reflective mappers.
 */
public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {
    private List<ColumnNameMatcher> columnNameMatchers;
    private boolean strictMatching;

    private boolean makeAttributesAccessible;

    /**
     * Create a default configuration that attempts case insensitive and
     * snake_case matching for names.
     */
    public ReflectionMappers() {
        this.columnNameMatchers = Arrays.asList(
                new CaseInsensitiveColumnNameMatcher(),
                new SnakeCaseColumnNameMatcher());
        this.strictMatching = false;
        this.makeAttributesAccessible = true;
    }

    private ReflectionMappers(ReflectionMappers that) {
        this.columnNameMatchers = new ArrayList<>(that.columnNameMatchers);
        this.strictMatching = that.strictMatching;
        this.makeAttributesAccessible = that.makeAttributesAccessible;
    }

    /**
     * Returns the registered column name mappers.
     *
     * @return the registered column name mappers.
     */
    public List<ColumnNameMatcher> getColumnNameMatchers() {
        return Collections.unmodifiableList(columnNameMatchers);
    }

    /**
     * Replace all column name matchers with the given list.
     * @param columnNameMatchers the column name matchers to use
     * @return this
     */
    public ReflectionMappers setColumnNameMatchers(List<ColumnNameMatcher> columnNameMatchers) {
        this.columnNameMatchers = new ArrayList<>(columnNameMatchers);
        return this;
    }

    /**
     * Returns whether strict column name matching is enabled. See {@link #setStrictMatching(boolean)} for
     * a detailed explanation.
     *
     * @return True if strict column name matching is enabled.
     */
    public boolean isStrictMatching() {
        return this.strictMatching;
    }

    /**
     * If set to true, all columns in a result set must be consumed by a reflection mapper. Any columns
     * consumed by a nested mapper will count towards the total number of columns.
     *
     * Reflection mappers with prefixes will only check those columns that
     * begin with the mapper's prefix.
     *
     * @param strictMatching whether to enable strict matching
     * @return this
     */
    public ReflectionMappers setStrictMatching(boolean strictMatching) {
        this.strictMatching = strictMatching;
        return this;
    }

    /**
     * Returns true if reflection mappers will try to make fields or methods accessible when trying
     * to write values. <b>Note that the mechanism to do so is deprecated and will be removed in a future
     * JDK version</b>. It is recommended to make production code work with this flag set to <code>false</code>.
     *
     * @return True if making fields and methods accessible is enabled.
     */
    public boolean isMakeAttributesAccessible() {
        return makeAttributesAccessible;
    }

    /**
     * If set to true, reflection mappers will try to make fields or methods accessible when trying
     * to write values. <b>Note that the mechanism to do so is deprecated and will be removed in a future
     * JDK version</b>. It is recommended to make production code work with this flag set to <code>false</code>.
     * <br>
     * The default for this flag is <code>undefined</code> and version specific. Code that
     * relies on this flag being set to a specific value should do so explicitly.
     *
     * @return True if making fields and methods accessible is enabled.
     */
    public ReflectionMappers setMakeAttributesAccessible(boolean makeAttributesAccessible) {
        this.makeAttributesAccessible = makeAttributesAccessible;
        return this;
    }

    @Override
    public ReflectionMappers createCopy() {
        return new ReflectionMappers(this);
    }
}
