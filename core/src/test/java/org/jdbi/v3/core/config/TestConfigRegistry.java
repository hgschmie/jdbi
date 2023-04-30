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
package org.jdbi.v3.core.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdbi.v3.core.config.internal.JdbiConfigList;
import org.jdbi.v3.core.config.internal.JdbiConfigMap;
import org.jdbi.v3.core.config.internal.JdbiConfigSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestConfigRegistry {

    private ConfigRegistry parent;
    private ConfigRegistry child1;
    private ConfigRegistry child2;
    private ConfigRegistry grandchild1;
    private ConfigRegistry grandchild2;

    private TestConfig parentConfig;

    @BeforeEach
    public void setUp() {
        this.parent = new ConfigRegistry();
        this.parentConfig = parent.get(TestConfig.class);
        this.parentConfig.addList("list1");
        this.parentConfig.addSet("set1");
        this.parentConfig.addMap("key1", "value1");
    }

    @Test
    public void testInheritParentValues() {
        validateSingleConfig(parentConfig);

        child1 = parent.createCopy();
        TestConfig child1Config = child1.get(TestConfig.class);
        validateSingleConfig(child1Config);

        child2 = parent.createCopy();
        TestConfig child2Config = child1.get(TestConfig.class);
        validateSingleConfig(child2Config);

        grandchild1 = child1.createCopy();
        TestConfig grandchild1Config = grandchild1.get(TestConfig.class);
        validateSingleConfig(grandchild1Config);

        grandchild2 = child2.createCopy();
        TestConfig grandchild2Config = grandchild2.get(TestConfig.class);
        validateSingleConfig(grandchild2Config);
    }

    @Test
    public void testModifyParentAfterCopy() {
        validateSingleConfig(parentConfig);

        child1 = parent.createCopy();
        TestConfig child1Config = child1.get(TestConfig.class);
        validateSingleConfig(child1Config);

        parentConfig.addList("list2");
        parentConfig.addSet("set2");

        validateDoubleConfig(parentConfig);
        validateSingleConfig(child1Config);

        grandchild1 = child1.createCopy();
        TestConfig grandchild1Config = grandchild1.get(TestConfig.class);

        child1Config.addList("list2");
        child1Config.addSet("set2");

        validateDoubleConfig(parentConfig);
        validateDoubleConfig(child1Config);
        validateSingleConfig(grandchild1Config);
    }

    @Test
    public void testModifyChildAfterCopy() {
        validateSingleConfig(parentConfig);

        child1 = parent.createCopy();
        TestConfig child1Config = child1.get(TestConfig.class);
        validateSingleConfig(child1Config);

        child1Config.addList("list2");
        child1Config.addSet("set2");

        validateDoubleConfig(child1Config);
        validateSingleConfig(parentConfig);

        grandchild1 = child1.createCopy();
        TestConfig grandchild1Config = grandchild1.get(TestConfig.class);

        grandchild1Config.addList("list3");
        grandchild1Config.addSet("set3");

        validateTripleConfig(grandchild1Config);
        validateDoubleConfig(child1Config);
        validateSingleConfig(parentConfig);
    }

    @Test
    public void testSiblingsAreIndependent() {
        validateSingleConfig(parentConfig);

        child1 = parent.createCopy();
        TestConfig child1Config = child1.get(TestConfig.class);
        validateSingleConfig(child1Config);

        child2 = parent.createCopy();
        TestConfig child2Config = child2.get(TestConfig.class);
        validateSingleConfig(child2Config);

        child1Config.addList("list2");
        child1Config.addSet("set2");

        validateDoubleConfig(child1Config);
        validateSingleConfig(parentConfig);
        validateSingleConfig(child2Config);
    }


    @Test
    public void testSiblingsInheritChanges() {
        validateSingleConfig(parentConfig);

        child1 = parent.createCopy();
        TestConfig child1Config = child1.get(TestConfig.class);
        validateSingleConfig(child1Config);

        parentConfig.addList("list2");
        parentConfig.addSet("set2");

        child2 = parent.createCopy();
        TestConfig child2Config = child2.get(TestConfig.class);

        validateDoubleConfig(parentConfig);
        validateSingleConfig(child1Config);
        validateDoubleConfig(child2Config);

        parentConfig.addList("list3");
        parentConfig.addSet("set3");

        validateTripleConfig(parentConfig);
        validateSingleConfig(child1Config);
        validateDoubleConfig(child2Config);
    }

    private static void validateSingleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(1)
                .containsExactly("list1");
        assertThat(config.getSet())
                .hasSize(1)
                .containsExactlyInAnyOrder("set1");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    private static void validateDoubleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(2)
                .containsExactly("list1", "list2");
        assertThat(config.getSet())
                .hasSize(2)
                .containsExactlyInAnyOrder("set1", "set2");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    private static void validateTripleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(3)
                .containsExactly("list1", "list2", "list3");
        assertThat(config.getSet())
                .hasSize(3)
                .containsExactlyInAnyOrder("set1", "set2", "set3");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    public static class TestConfig implements JdbiConfig<TestConfig> {

        private JdbiConfigList<String> list;
        private JdbiConfigSet<String> set;
        private JdbiConfigMap<String, String> map;

        public TestConfig() {
            this.list = JdbiConfigList.create();
            this.set = JdbiConfigSet.create();
            this.map = JdbiConfigMap.create();
        }

        private TestConfig(TestConfig that) {
            this.list = that.list;
            this.set = that.set;
            this.map = that.map;
        }

        @Override
        public TestConfig createCopy() {
            return new TestConfig(this);
        }

        public TestConfig addList(String key) {
            this.list = list.addLast(key);
            return this;
        }

        public TestConfig addSet(String key) {
            this.set = set.addElements(key);
            return this;
        }

        public TestConfig addMap(String key, String value) {
            this.map = map.putElement(key, value);
            return this;
        }

        public Set<String> getSet() {
            return set.asUnmodifiableSet();
        }

        public List<String> getList() {
            return list.asUnmodifiableList();
        }

        public Map<String, String> getMap() {
            return map.asUnmodifiableMap();
        }
    }
}
