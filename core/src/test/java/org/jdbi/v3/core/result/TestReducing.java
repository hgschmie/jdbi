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
package org.jdbi.v3.core.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReducing {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @BeforeEach
    public void setUp() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("CREATE TABLE something_location (id int, location varchar)");
        h.execute("INSERT INTO something (id, name) VALUES (1, 'tree')");
        h.execute("INSERT INTO something (id, name) VALUES (2, 'apple')");
        h.execute("INSERT INTO something_location (id, location) VALUES (1, 'outside')");
        h.execute("INSERT INTO something_location (id, location) VALUES (2, 'tree')");
        h.execute("INSERT INTO something_location (id, location) VALUES (2, 'pie')");
        h.registerRowMapper(new SomethingMapper());
    }

    @Test
    public void testReduceRowsWithSeed() {
        Map<Integer, SomethingWithLocations> result = h2Extension.getSharedHandle()
            .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
            .reduceRows(new HashMap<>(), (map, rr) -> {
                map.computeIfAbsent(rr.getColumn("id", Integer.class),
                        id -> new SomethingWithLocations(rr.getRow(Something.class)))
                    .locations
                    .add(rr.getColumn("location", String.class));
                return map;
            });

        assertThat(result).hasSize(2)
            .containsEntry(1, new SomethingWithLocations(new Something(1, "tree")).at("outside"))
            .containsEntry(2, new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
    }

    @Test
    public void testCollectRows() {
        Iterable<SomethingWithLocations> result = h2Extension.getSharedHandle()
            .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
            .collectRows(Collector.<RowView, Map<Integer, SomethingWithLocations>, Iterable<SomethingWithLocations>>of(
                LinkedHashMap::new,
                (Map<Integer, SomethingWithLocations> map, RowView rv) ->
                    map.computeIfAbsent(rv.getColumn("id", Integer.class),
                            id -> new SomethingWithLocations(rv.getRow(Something.class)))
                        .locations
                        .add(rv.getColumn("location", String.class)),
                (a, b) -> {
                    throw new UnsupportedOperationException("shouldn't use combiner");
                },
                Map::values));

        assertThat(result).containsExactly(
            new SomethingWithLocations(new Something(1, "tree")).at("outside"),
            new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
    }

    @Test
    public void testReduceRowsStreamClean() {
        try (Stream<SomethingWithLocations> stream = h2Extension.getSharedHandle()
                .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
                .reduceRows((Map<Integer, SomethingWithLocations> map, RowView rv) ->
                        map.computeIfAbsent(rv.getColumn("id", Integer.class),
                                        id -> new SomethingWithLocations(rv.getRow(Something.class)))
                                .locations
                                .add(rv.getColumn("location", String.class)))) {
            List<SomethingWithLocations> result = stream.collect(toList());
            assertThat(result).containsExactly(
                    new SomethingWithLocations(new Something(1, "tree")).at("outside"),
                    new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
        }
    }

    @Test
    public void testReduceRowsStatementClean() {
        try (Query query = h2Extension.getSharedHandle()
                .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")) {
            List<SomethingWithLocations> result = query.reduceRows((Map<Integer, SomethingWithLocations> map, RowView rv) ->
                            map.computeIfAbsent(rv.getColumn("id", Integer.class),
                                            id -> new SomethingWithLocations(rv.getRow(Something.class)))
                                    .locations
                                    .add(rv.getColumn("location", String.class)))
                    .collect(toList());

            assertThat(result).containsExactly(
                    new SomethingWithLocations(new Something(1, "tree")).at("outside"),
                    new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
        }
    }

    @Test
    public void testReduceResultSet() {
        Map<Integer, SomethingWithLocations> result = h2Extension.getSharedHandle()
            .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
            .reduceResultSet(new HashMap<>(), (map, rs, ctx) -> {
                final String name = rs.getString("name");
                map.computeIfAbsent(rs.getInt("id"),
                        id -> new SomethingWithLocations(new Something(id, name)))
                    .at(rs.getString("location"));
                return map;
            });

        assertThat(result).hasSize(2)
            .containsEntry(1, new SomethingWithLocations(new Something(1, "tree")).at("outside"))
            .containsEntry(2, new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
    }

    static class SomethingWithLocations {

        final Something something;
        final List<String> locations = new ArrayList<>();

        SomethingWithLocations(Something something) {
            this.something = something;
        }

        SomethingWithLocations at(String where) {
            locations.add(where);
            return this;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SomethingWithLocations)) {
                return false;
            }
            SomethingWithLocations o = (SomethingWithLocations) other;
            return o.something.equals(something) && o.locations.equals(locations);
        }

        @Override
        public int hashCode() {
            return something.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Something %s with locations %s", something, locations);
        }
    }
}
