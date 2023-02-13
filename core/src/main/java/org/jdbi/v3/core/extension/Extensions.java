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
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.extension.ExtensionMetaData.ExtensionHandlerInvoker;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.meta.Beta;

import static org.jdbi.v3.core.extension.ExtensionHandler.EQUALS_HANDLER;
import static org.jdbi.v3.core.extension.ExtensionHandler.HASHCODE_HANDLER;
import static org.jdbi.v3.core.extension.ExtensionHandler.NULL_HANDLER;

/**
 * Configuration class for defining {@code Jdbi} extensions via {@link ExtensionFactory}
 * instances.
 */
public class Extensions implements JdbiConfig<Extensions> {

    private final List<ExtensionFactoryDelegate> factories = new CopyOnWriteArrayList<>();
    private final List<ExtensionHandler.ExtensionHandlerFactory> extensionHandlerFactories = new CopyOnWriteArrayList<>();
    private final List<ExtensionHandlerCustomizer> extensionHandlerCustomizers = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<Class<?>, ExtensionMetaData> extensionMetaDataCache = new ConcurrentHashMap<>();
    private boolean allowProxy = true;

    /**
     * Create an empty {@link ExtensionFactory} configuration.
     */
    public Extensions() {
        // backstop for default methods, bridge methods and delegates to concrete implementations.
        register(new InternalExtensionFactory());
    }

    /**
     * Create an extension configuration by cloning another
     *
     * @param that the configuration to clone
     */
    private Extensions(Extensions that) {
        allowProxy = that.allowProxy;
        factories.addAll(that.factories);

        extensionHandlerFactories.addAll(that.extensionHandlerFactories);
        extensionHandlerCustomizers.addAll(that.extensionHandlerCustomizers);
        extensionMetaDataCache.putAll(that.extensionMetaDataCache);
    }

    /**
     * Register an extension factory.
     *
     * @param factory the factory to register
     * @return this
     */
    public Extensions register(ExtensionFactory factory) {

        factories.add(0, new ExtensionFactoryDelegate(factory));

        extensionHandlerFactories.addAll(0, factory.getExtensionHandlerFactories());
        extensionHandlerCustomizers.addAll(0, factory.getExtensionHandlerCustomizers());

        return this;
    }

    /**
     * Returns true if an extension is registered for the given type.
     *
     * @param extensionType the type to query.
     * @return true if a registered extension handles the type.
     */
    public boolean hasExtensionFor(Class<?> extensionType) {
        return findFactoryFor(extensionType).isPresent();
    }

    /**
     * Create an extension instance if we have a factory that understands
     * the extension type which has access to a {@code Handle} through a {@link HandleSupplier}.
     *
     * @param <E>            the extension type to create
     * @param extensionType  the extension type to create
     * @param handleSupplier the handle supplier
     * @return an attached extension instance if a factory is found
     */
    public <E> Optional<E> findFor(Class<E> extensionType, HandleSupplier handleSupplier) {
        return findFactoryFor(extensionType)
                .map(factory -> factory.attach(extensionType, handleSupplier));
    }

    public Optional<ExtensionHandler> findExtensionHandlerFor(Class<?> extensionType, Method method) {
        Optional<ExtensionHandler> result = Optional.empty();
        for (ExtensionHandlerFactory extensionHandlerFactory : extensionHandlerFactories) {
            if (extensionHandlerFactory.accepts(extensionType, method)) {
                result = extensionHandlerFactory.buildExtensionHandler(extensionType, method);
                if (result.isPresent()) {
                    break; // for
                }
            }
        }
        return result;
    }

    public ExtensionHandler applyExtensionHandlerCustomizers(ExtensionHandler extensionHandler, Class<?> extensionType, Method method) {
        ExtensionHandler handler = extensionHandler;
        for (ExtensionHandlerCustomizer extensionHandlerCustomizer : extensionHandlerCustomizers) {
            handler = extensionHandlerCustomizer.customize(handler, extensionType, method);
        }
        return handler;
    }

    private Optional<ExtensionFactory> findFactoryFor(Class<?> extensionType) {
        Optional<ExtensionFactory> result = Optional.empty();
        for (ExtensionFactory factory : factories) {
            if (factory.accepts(extensionType)) {
                result = Optional.of(factory);
                break;
            }
        }

        return result;
    }

    /**
     * Find the registered factory of the given type, if any
     *
     * @param factoryType the factory's type to find
     * @return the found factory, if any
     */
    public Optional<ExtensionFactory> findFactory(Class<? extends ExtensionFactory> factoryType) {
        Optional<ExtensionFactory> result = Optional.empty();
        for (ExtensionFactoryDelegate factory : factories) {
            if (factoryType.isInstance(factory.delegatedFactory)) {
                result = Optional.of(factory);
                break;
            }
        }

        return result;
    }

    /**
     * Allow using {@link java.lang.reflect.Proxy} to implement extensions.
     *
     * @param allowProxy whether to allow use of Proxy types
     * @return this
     */
    @Beta
    public Extensions setAllowProxy(boolean allowProxy) {
        this.allowProxy = allowProxy;
        return this;
    }

    /**
     * Returns whether Proxy classes are allowed to be used.
     *
     * @return whether Proxy classes are allowed to be used.
     */
    @Beta
    public boolean isAllowProxy() {
        return allowProxy;
    }

    @Override
    public Extensions createCopy() {
        return new Extensions(this);
    }

    /**
     * Throw if proxy creation is disallowed.
     */
    @Beta
    public void onCreateProxy() {
        if (!isAllowProxy()) {
            throw new IllegalStateException(
                    "Creating onDemand proxy disallowed. Ensure @GenerateSqlObject annotation is being processed by `jdbi3-generator` annotation processor.");
        }
    }

    public class ExtensionFactoryDelegate implements ExtensionFactory {

        private final ExtensionFactory delegatedFactory;

        ExtensionFactoryDelegate(ExtensionFactory delegatedFactory) {
            this.delegatedFactory = delegatedFactory;
        }

        Class<?> factoryClass() {
            return delegatedFactory.getClass();
        }

        @Override
        public boolean accepts(Class<?> extensionType) {
            return delegatedFactory.accepts(extensionType);
        }

        @Override
        public Collection<? extends ExtensionHandlerFactory> getExtensionHandlerFactories() {
            return delegatedFactory.getExtensionHandlerFactories();
        }

        @Override
        public Collection<? extends ExtensionHandlerCustomizer> getExtensionHandlerCustomizers() {
            return delegatedFactory.getExtensionHandlerCustomizers();
        }

        @Override
        public boolean isProxyFactory() {
            return true;
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            ConfigRegistry config = handleSupplier.getConfig();

            final ExtensionMetaData extensionMetaData = extensionMetaDataCache.computeIfAbsent(extensionType, e -> {
                ExtensionMetaData.Builder builder = ExtensionMetaData.builder(config, extensionType);
                delegatedFactory.buildExtensionInitData(builder);
                return builder.build();
            });

            final ConfigRegistry instanceConfig = extensionMetaData.createInstanceConfiguration(config);

            Map<Method, ExtensionHandlerInvoker> handlers = new HashMap<>();
            final Object proxy = Proxy.newProxyInstance(
                    extensionType.getClassLoader(),
                    new Class[] {extensionType},
                    (proxyInstance, method, args) -> handlers.get(method).invoke(args));

            // if the object created by the delegated factory has actual methods (it is not delegating), attach the
            // delegate and pass it to the handlers. Otherwise assume that there is no backing object and do not call
            // attach.
            final Object delegatedInstance = delegatedFactory.isProxyFactory() ? null : delegatedFactory.attach(extensionType, handleSupplier);

            // add all methods that are delegated to the underlying object / existing handlers
            extensionMetaData.getExtensionHandlers().keySet().forEach(method ->
                    handlers.put(method, extensionMetaData.createExtensionHandlerInvoker(delegatedInstance, method, handleSupplier, instanceConfig)));

            // add proxy specific methods (toString, equals, hashCode, finalize)
            // those will only be added if they don't already exist in the method handler map.

            // If these methods are added, they are special because they operate on the proxy object itself, not the underlying object

            checkMethodHandler(extensionType, Object.class, "toString").ifPresent(method -> {
                ExtensionHandler toStringHandler = (h, target, args) ->
                        "Jdbi extension proxy for " + extensionType.getName() + "@" + Integer.toHexString(proxy.hashCode());
                handlers.put(method, extensionMetaData.new ExtensionHandlerInvoker(proxy, method, toStringHandler, handleSupplier, instanceConfig));
            });

            checkMethodHandler(extensionType, Object.class, "equals", Object.class).ifPresent(method -> handlers.put(method,
                    extensionMetaData.new ExtensionHandlerInvoker(proxy, method, EQUALS_HANDLER, handleSupplier, instanceConfig)));
            checkMethodHandler(extensionType, Object.class, "hashCode").ifPresent(method -> handlers.put(method,
                    extensionMetaData.new ExtensionHandlerInvoker(proxy, method, HASHCODE_HANDLER, handleSupplier, instanceConfig)));
            checkMethodHandler(extensionType, extensionType, "finalize").ifPresent(method -> handlers.put(method,
                    extensionMetaData.new ExtensionHandlerInvoker(proxy, method, NULL_HANDLER, handleSupplier, instanceConfig)));

            return extensionType.cast(proxy);
        }

        private Optional<Method> checkMethodHandler(Class<?> extensionType, Class<?> klass, String methodName, Class<?>... parameterTypes) {
            Optional<Method> method = JdbiClassUtils.safeMethodLookup(extensionType, methodName, parameterTypes);
            if (method.isPresent()) {
                // does the method actually exist in the type itself (e.g. overridden by the implementation class?)
                // if yes, return absent, so the default hander is not added.
                return Optional.empty();
            } else {
                return JdbiClassUtils.safeMethodLookup(klass, methodName, parameterTypes);
            }
        }


        @Override
        public String toString() {
            return "ExtensionFactoryDelegate for " + delegatedFactory.toString();
        }
    }
}
