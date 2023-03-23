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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectorsTest {
    @RegisterExtension
    H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    Handle handle;

    @BeforeEach
    void setup() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table collection (k varchar)");
        handle.execute("insert into collection (k) values('a')");
        handle.execute("insert into collection (k) values('b')");
        handle.execute("insert into collection (k) values('c')");
    }

    @Test
    void testSet() {
        assertThat(queryString().set())
                .isInstanceOf(Set.class)
                .containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void collectIntoLinkedList() {
        assertThat(queryString().collectInto(new GenericType<LinkedList<String>>() {}))
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void testToCollectionWithList() {
        assertThat(queryString().list())
                .isNotInstanceOf(LinkedList.class);


        List<String> result = queryString().toCollection(() -> new LinkedList<>());

        assertThat(result)
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void testToCollectionWithCollection() {
        assertThat(queryString().list())
                .isNotInstanceOf(LinkedList.class);

        Collection<String> result = queryString().toCollection(() -> new LinkedList<>());

        assertThat(result)
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");
    }


    @Test
    void testCollectIntoList() {
        assertThat(queryString().collectIntoList())
                .isNotInstanceOf(LinkedList.class);

        List<String> stringResult = queryString().collectIntoList();

        assertThat(stringResult)
                .isInstanceOf(ArrayList.class)
                .containsExactly("a", "b", "c");

        handle.registerCollector(List.class, Collectors.toCollection(LinkedList::new));

        stringResult = queryString().collectIntoList();

        assertThat(stringResult)
                .isInstanceOf(LinkedList.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void testCollectIntoGenericTypeSet() {
        assertThat(queryString().set())
                .isNotInstanceOf(LinkedHashSet.class);

        final Set<String> result = queryString().collectInto(new GenericType<LinkedHashSet<String>>() {});

        assertThat(result)
                .isInstanceOf(LinkedHashSet.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void testCollectIntoTypeSet() {
        assertThat(queryString().set())
                .isNotInstanceOf(LinkedHashSet.class);

        final Set<String> result = queryString().collectInto(LinkedHashSet.class);

        assertThat(result)
                .isInstanceOf(LinkedHashSet.class)
                .containsExactly("a", "b", "c");
    }

    @Test
    void testCollectIntoCollection() {
        assertThat(queryString().set())
                .isNotInstanceOf(LinkedHashSet.class);

        final Collection<String> result = queryString().collectInto(new GenericType<LinkedHashSet<String>>() {});

        assertThat(result)
                .isInstanceOf(LinkedHashSet.class)
                .containsExactly("a", "b", "c");
    }

    private ResultIterable<String> queryString() {
        return baseQuery().mapTo(String.class);
    }

    private Query baseQuery() {
        return handle.createQuery("select * from collection");
    }
}
