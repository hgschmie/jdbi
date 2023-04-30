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
package org.jdbi.v3.core.config.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public final class JdbiConfigSet<T> implements Iterable<T> {

    private final Set<T> backingSet;

    public static <T> JdbiConfigSet<T> create() {
        return new JdbiConfigSet<>();
    }

    private JdbiConfigSet() {
        this.backingSet = Collections.emptySet();
    }

    private JdbiConfigSet(Set<T> backingSet) {
        this.backingSet = Collections.unmodifiableSet(backingSet);
    }

    public Set<T> asUnmodifiableSet() {
        return backingSet;
    }

    public Stream<T> stream() {
        return backingSet.stream();
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return backingSet.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        backingSet.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return backingSet.spliterator();
    }

    @SafeVarargs
    public final JdbiConfigSet<T> addElements(T... elements) {
        return addElements(Arrays.asList(elements));
    }

    public JdbiConfigSet<T> addElements(Collection<T> elements) {
        Set<T> newSet = new HashSet<>(backingSet);
        newSet.addAll(elements);
        return new JdbiConfigSet<>(newSet);
    }

    @SafeVarargs
    public final JdbiConfigSet<T> removeElements(T... elements) {
        return removeElements(Arrays.asList(elements));
    }

    public JdbiConfigSet<T> removeElements(Collection<T> elements) {
        Set<T> newSet = new HashSet<>(backingSet);
        newSet.removeAll(elements);
        return new JdbiConfigSet<>(newSet);
    }
}
