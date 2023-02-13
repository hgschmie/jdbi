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
package org.jdbi.v3.core.extension;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.meta.Beta;

import static java.lang.String.format;

@FunctionalInterface
public interface ExtensionHandler {

    ExtensionHandler EQUALS_HANDLER = (handleSupplier, target, args) -> target == args[0];
    ExtensionHandler HASHCODE_HANDLER = (handleSupplier, target, args) -> System.identityHashCode(target);
    ExtensionHandler NULL_HANDLER = (handleSupplier, target, args) -> null;

    Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception;

    @Beta
    default void warm(ConfigRegistry config) {}

    static ExtensionHandler missingExtensionHandler(Method method) {
        return (target, args, handleSupplier) -> {
            throw new IllegalStateException(format(
                    "Method %s.%s has no registered extension handler!",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName()));
        };
    }

    interface ExtensionHandlerFactory {

        boolean accepts(Class<?> extensionType, Method method);

        Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method);
    }
}
