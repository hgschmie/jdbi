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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public final class JdbiConfigList<T> implements Iterable<T> {

    private final List<T> backingList;

    public static <T> JdbiConfigList<T> create() {
        return new JdbiConfigList<>();
    }

    public static <T> JdbiConfigList<T> create(int initialSize) {
        List<T> newList = new ArrayList<>(initialSize);
        for (int i = 0; i < initialSize; i++) {
            newList.add(null);
        }
        return new JdbiConfigList<>(newList);
    }

    private JdbiConfigList() {
        this.backingList = Collections.emptyList();
    }

    private JdbiConfigList(List<T> backingList) {
        this.backingList = Collections.unmodifiableList(backingList);
    }

    public List<T> asUnmodifiableList() {
        return backingList;
    }

    public Stream<T> stream() {
        return backingList.stream();
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return backingList.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        backingList.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return backingList.spliterator();
    }

    @SafeVarargs
    public final JdbiConfigList<T> addFirst(T... elements) {
        return addFirst(Arrays.asList(elements));
    }

    @SafeVarargs
    public final JdbiConfigList<T> addLast(T... elements) {
        return addLast(Arrays.asList(elements));
    }

    public JdbiConfigList<T> addFirst(Collection<T> elements) {
        List<T> newList = new LinkedList<>(backingList);
        newList.addAll(0, elements);
        return new JdbiConfigList<>(newList);
    }

    public JdbiConfigList<T> addLast(Collection<T> elements) {
        List<T> newList = new LinkedList<>(backingList);
        newList.addAll(elements);
        return new JdbiConfigList<>(newList);
    }

    public JdbiConfigList<T> setValue(int index, T element) {
        List<T> newList;
        if (index < backingList.size()) {
            newList = new ArrayList<>(backingList);
        } else {
            newList = new ArrayList<>(index + 1);
            newList.addAll(backingList);
        }

        newList.set(index, element);
        return new JdbiConfigList<>(newList);
    }

    public T getValue(int index) {
        return backingList.get(index);
    }
}
