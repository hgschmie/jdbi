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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class JdbiConfigMap<K, V> {

    private final Map<K, V> backingMap;

    public static <K, V> JdbiConfigMap<K, V> create() {
        return new JdbiConfigMap<>();
    }

    private JdbiConfigMap() {
        this.backingMap = Collections.emptyMap();
    }

    private JdbiConfigMap(Map<K, V> backingMap) {
        this.backingMap = Collections.unmodifiableMap(backingMap);
    }

    public Map<K, V> asUnmodifiableMap() {
        return backingMap;
    }

    public JdbiConfigMap<K, V> putElement(K key, V value) {
        if (value == null) {
            return this;
        } else {
            Map<K, V> newMap = new HashMap<>(backingMap);
            newMap.put(key, value);
            return new JdbiConfigMap<>(newMap);
        }
    }

    public JdbiConfigMap<K, V> putElements(Map<K, V> elements) {
        Map<K, V> newMap = new HashMap<>(backingMap);
        newMap.putAll(elements);
        return new JdbiConfigMap<>(newMap);
    }

    public V getElement(K key) {
        return backingMap.get(key);
    }

    public boolean hasKey(K key) {
        return backingMap.containsKey(key);
    }

    public JdbiConfigMap<K, V> clear() {
        if (backingMap.isEmpty()) {
            return this;
        }
        return JdbiConfigMap.create();
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        backingMap.forEach(action);
    }
}
