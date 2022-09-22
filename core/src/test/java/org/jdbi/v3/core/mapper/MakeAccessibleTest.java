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
package org.jdbi.v3.core.mapper;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MakeAccessibleTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    protected Handle handle;

    @BeforeEach
    void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testMakeFieldsAccessible() {
        handle.getConfig(ReflectionMappers.class).setMakeAttributesAccessible(true);
        TestFieldBean testBean = handle.select("select 1 as id, 2 as inaccessible").map(FieldMapper.of(TestFieldBean.class)).one();

        assertThat(testBean).isNotNull();
        assertThat(testBean.id).isOne();
        assertThat(testBean.inaccessible).isEqualTo(2);
    }

    @Test
    public void testMakeFieldsInaccessible() {
        handle.getConfig(ReflectionMappers.class).setMakeAttributesAccessible(false);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            handle.select("select 1 as id, 2 as inaccessible").map(FieldMapper.of(TestFieldBean.class)).one();
        });

        assertThat(e.getMessage()).contains("Unable to access property 'inaccessible'");
    }

    public static class TestFieldBean {

        public int id = 0;
        private int inaccessible = 0;
    }
}
