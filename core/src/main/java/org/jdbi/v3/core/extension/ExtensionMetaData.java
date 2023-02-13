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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCustomizerChain;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.meta.Alpha;

/**
 * Represents a resolved extension type with all config customizers and method handlers.
 */
@Alpha
public final class ExtensionMetaData {

    private final Class<?> extensionType;
    private final ConfigCustomizer instanceConfigCustomizer;
    private final Map<Method, ? extends ConfigCustomizer> methodConfigCustomizers;
    private final Map<Method, ExtensionHandler> methodHandlers;

    public static ExtensionMetaData.Builder builder(ConfigRegistry config, Class<?> extensionType) {
        return new Builder(config, extensionType);
    }

    private ExtensionMetaData(
            Class<?> extensionType,
            ConfigCustomizer instanceConfigCustomizer,
            Map<Method, ? extends ConfigCustomizer> methodConfigCustomizers,
            Map<Method, ExtensionHandler> methodHandlers) {
        this.extensionType = extensionType;
        this.instanceConfigCustomizer = instanceConfigCustomizer;
        this.methodConfigCustomizers = Collections.unmodifiableMap(methodConfigCustomizers);
        this.methodHandlers = Collections.unmodifiableMap(methodHandlers);
    }

    public Class<?> extensionType() {
        return extensionType;
    }

    /**
     * Apply an instance specific {@link ConfigCustomizer} to the configuration.
     *
     * @param config A configuration object. The object is not changed.
     * @return A new configuration object with all changes applied.
     */
    public ConfigRegistry createInstanceConfiguration(ConfigRegistry config) {
        ConfigRegistry instanceConfiguration = config.createCopy();
        instanceConfigCustomizer.customize(instanceConfiguration);
        return instanceConfiguration;
    }

    /**
     * Apply a method specific {@link ConfigCustomizer} to the configuration.
     *
     * @param method The method that is about to be called.
     * @param config A configuration object. The object is not changed.
     * @return A new configuration object with all changes applied.
     */
    public ConfigRegistry createMethodConfiguration(Method method, ConfigRegistry config) {
        ConfigRegistry methodConfiguration = config.createCopy();
        ConfigCustomizer methodConfigCustomizer = methodConfigCustomizers.get(method);
        if (methodConfigCustomizer != null) {
            methodConfigCustomizer.customize(methodConfiguration);
        }
        return methodConfiguration;
    }

    public Map<Method, ExtensionHandler> getExtensionHandlers() {
        return methodHandlers;
    }

    public <E> ExtensionHandlerInvoker createExtensionHandlerInvoker(E target, Method method,
            HandleSupplier handleSupplier, ConfigRegistry instanceConfig) {
        return new ExtensionHandlerInvoker(target, method, methodHandlers.get(method), handleSupplier, instanceConfig);
    }

    public static final class Builder {

        private final Class<?> extensionType;
        private final ConfigCustomizerChain instanceConfigCustomizer = new ConfigCustomizerChain();
        private final Map<Method, ConfigCustomizerChain> methodConfigCustomizers = new HashMap<>();
        private final Map<Method, ExtensionHandler> methodHandlers = new HashMap<>();
        private final ConfigRegistry config;

        private final Set<Method> extensionTypeMethods = new LinkedHashSet<>();


        Builder(ConfigRegistry config, Class<?> extensionType) {
            this.config = config;
            this.extensionType = extensionType;

            this.extensionTypeMethods.addAll(Arrays.asList(extensionType.getMethods()));
            this.extensionTypeMethods.addAll(Arrays.asList(extensionType.getDeclaredMethods()));

            this.extensionTypeMethods.stream()
                    .filter(m -> !m.isSynthetic())
                    .collect(Collectors.groupingBy(m -> Arrays.asList(m.getName(), Arrays.asList(m.getParameterTypes()))))
                    .values()
                    .stream()
                    .filter(methodCount -> methodCount.size() > 1)
                    .findAny()
                    .ifPresent(methods -> {
                        throw new UnableToCreateExtensionException("%s has ambiguous methods (%s) found, please resolve with an explicit override",
                                extensionType, methods);
                    });
        }

        public Builder addInstanceConfigCustomizer(ConfigCustomizer configCustomizer) {
            instanceConfigCustomizer.addCustomizer(configCustomizer);
            return this;
        }

        public Builder addMethodConfigCustomizer(Method method, ConfigCustomizer configCustomizer) {
            ConfigCustomizerChain methodConfigCustomizer = methodConfigCustomizers.computeIfAbsent(method, m -> new ConfigCustomizerChain());
            methodConfigCustomizer.addCustomizer(configCustomizer);
            return this;
        }

        public Builder addMethodHandler(Method method, ExtensionHandler handler) {
            methodHandlers.put(method, handler);
            return this;
        }

        public ExtensionMetaData build() {
            final Extensions extensions = config.get(Extensions.class);

            // add all methods that are declared on the extension type and
            // are not static and don't already have a handler

            final Set<Method> seen = new HashSet<>(methodHandlers.keySet());
            for (Method method : extensionTypeMethods) {
                // skip static methods and methods that already have method handlers
                if (Modifier.isStatic(method.getModifiers()) || !seen.add(method)) {
                    continue;
                }

                // look through the registered factories (either from the factory itself or the backstop factory)
                // to find extension handlers
                ExtensionHandler handler = extensions.findExtensionHandlerFor(extensionType, method)
                        .orElseGet(() -> ExtensionHandler.missingExtensionHandler(method));

                handler.warm(config);
                methodHandlers.put(method, extensions.applyExtensionHandlerCustomizers(handler, extensionType, method));
            }

            return new ExtensionMetaData(extensionType, instanceConfigCustomizer, methodConfigCustomizers, methodHandlers);
        }
    }

    public final class ExtensionHandlerInvoker {
        private final Object target;
        private final HandleSupplier handleSupplier;
        private final ExtensionContext extensionContext;
        private final ExtensionHandler extensionHandler;

        ExtensionHandlerInvoker(Object target, Method method, ExtensionHandler extensionHandler, HandleSupplier handleSupplier, ConfigRegistry config) {
            this.target = target;
            this.handleSupplier = handleSupplier;
            ConfigRegistry methodConfig = createMethodConfiguration(method, config);
            this.extensionContext = ExtensionContext.forExtensionMethod(methodConfig, extensionType, method);
            this.extensionHandler = extensionHandler;
        }

        public Object invoke(Object... args) {
            final Object[] handlerArgs = JdbiClassUtils.safeVarargs(args);
            final Callable<Object> callable = () -> extensionHandler.invoke(handleSupplier, target, handlerArgs);
            return call(callable);
        }

        public Object call(Callable<?> callable) {
            try {
                return handleSupplier.invokeInContext(extensionContext, callable);
            } catch (Exception x) {
                throw Sneaky.throwAnyway(x);
            }
        }

        public Object call(Runnable runnable) {
            return call(() -> {
                runnable.run();
                return null;
            });
        }
    }
}
