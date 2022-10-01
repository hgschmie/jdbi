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
package org.jdbi.v3.core.statement;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPreparedBatchPG {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    private Handle handle;

    @BeforeEach
    void setUp() {
        this.handle = pgExtension.getSharedHandle();
    }

    @Test
    public void emptyBatch() {
        assertThat(handle.prepareBatch("insert into something (id, name) values (:id, :name)").execute()).isEmpty();
    }

    // This would be a test in `TestPreparedBatch` but H2 has a bug (?) that returns a generated key even when there wasn't one.
    @Test
    public void emptyBatchGeneratedKeys() {

        PreparedBatch batch = handle.prepareBatch("insert into something (id, name) values (:id, :name)");
        StatementContext ctx = batch.getContext();
        assertThat(
                batch.executeAndReturnGeneratedKeys("id")
                .mapTo(int.class)
                .list())
            .isEmpty();

        assertThat(ctx.isClosed()).isTrue();
    }
}
