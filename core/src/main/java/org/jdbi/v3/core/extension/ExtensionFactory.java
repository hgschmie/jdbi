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
import java.util.Collection;
import java.util.Collections;

import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;

/**
 * Factory interface used to produce Jdbi extension objects. A factory can provide metadata
 * to create extension objects.
 */
public interface ExtensionFactory {

    /**
     * Returns true if the factory can process the given extension type.
     *
     * @param extensionType the extension type.
     * @return whether the factory can produce an extension of the given type.
     */
    boolean accepts(Class<?> extensionType);

    /**
     * Attaches an extension type. This method is only called if {@link #isProxyFactory()} returns false.
     *
     * @param extensionType  the extension type.
     * @param handleSupplier Supplies the database handle. This supplier may lazily open a Handle on the first
     *                       invocation. Extension implementors should take care not to fetch the handle before it is
     *                       needed, to avoid opening handles unnecessarily.
     * @param <E>            the extension type
     * @return an extension of the given type, attached to the given handle.
     * @throws IllegalArgumentException if the extension type is not supported by this factory.
     * @see org.jdbi.v3.core.Jdbi#onDemand(Class)
     */
    <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier);

    default Collection<? extends ExtensionHandlerFactory> getExtensionHandlerFactories() {
        return Collections.emptySet();
    }

    /**
     * Returns a collection of {@link ExtensionHandlerCustomizer} objects. A customizer is
     * execute after a the {@link ExtensionHandler} for a specific method has been created
     * and before it is registered with the extension framework.
     *
     * @return A collection of {@link ExtensionHandlerCustomizer} objects. Can be empty, must not be null.
     */
    default Collection<? extends ExtensionHandlerCustomizer> getExtensionHandlerCustomizers() {
        return Collections.emptySet();
    }


    /**
     * Receives the {@link ExtensionMetaData.Builder} when the {@link ExtensionMetaData} object for this extension
     * is created.
     * <br/>
     * Code here can call the {@link ExtensionMetaData.Builder#addInstanceConfigCustomizer(ConfigCustomizer)},
     * {@link ExtensionMetaData.Builder#addMethodConfigCustomizer(Method, ConfigCustomizer)} and
     * {@link ExtensionMetaData.Builder#addMethodHandler(Method, ExtensionHandler)} method to configure the data object.
     */
    default void buildExtensionInitData(ExtensionMetaData.Builder builder) {}

    /**
     * Returns true if this factory creates proxy objects.
     * <br>
     * If the factory creates proxy objects, then for every method
     * on an extension type, a method handler must exist that can create a result without an underlying object. E.g. the
     * SQLObject handler can process every method in an interface class without requiring an implementation of the extension
     * type. The extension framework will execute the method handlers and pass in a proxy object instead of an underlying instance.
     * <br>
     * If this method returns false, the extension framework will call attach to get an implementation instance for the extension type
     * and all method handlers will be executed on the implementation instance.
     *
     * @return True if this factory creates proxy objects. The default is false.
     */
    default boolean isProxyFactory() {
        return false;
    }
}
