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

import java.util.LinkedHashSet;
import java.util.Set;

import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.config.ConfigRegistry;

public final class ConfigCustomizerChain implements ConfigCustomizer {

    private final Set<ConfigCustomizer> configCustomizers = new LinkedHashSet<>();

    public void addCustomizer(ConfigCustomizer configCustomizer) {
        configCustomizers.add(configCustomizer);
    }

    @Override
    public void customize(final ConfigRegistry config) {
        for (ConfigCustomizer configCustomizer : configCustomizers) {
            configCustomizer.customize(config);
        }
    }
}
