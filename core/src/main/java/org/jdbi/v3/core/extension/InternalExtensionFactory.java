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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

import static java.lang.invoke.MethodHandles.Lookup.PACKAGE;
import static java.lang.invoke.MethodHandles.Lookup.PRIVATE;
import static java.lang.invoke.MethodHandles.Lookup.PROTECTED;
import static java.lang.invoke.MethodHandles.Lookup.PUBLIC;
import static java.util.Collections.synchronizedMap;

/**
 * Provides {@link ExtensionHandlerFactory} instances for all extension types.
 */
class InternalExtensionFactory implements ExtensionFactory {

    private static final int ANY_ACCESS = PUBLIC | PRIVATE | PROTECTED | PACKAGE;
    // MethodHandles.privateLookupIn(Class, Lookup) was added in JDK 9.
    // JDK 9 allows us to unreflectSpecial() on an interface default method, where JDK 8 did not.
    private static final Method PRIVATE_LOOKUP_IN = privateLookupIn();
    private static final Map<Class<?>, Lookup> PRIVATE_LOOKUPS = synchronizedMap(new WeakHashMap<>());

    @Override
    public boolean accepts(Class<?> extensionType) {
        return false; // this is not actually a factory, only provides ExtensionHandlerFactories
    }

    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<? extends ExtensionHandlerFactory> getExtensionHandlerFactories() {
        return Arrays.asList(
                new BridgeMethodExtensionHandlerFactory(),
                new InstanceExtensionHandlerFactory());
    }

    @Override
    public Collection<? extends ExtensionHandlerCustomizer> getExtensionHandlerCustomizers() {
        return Collections.emptyList();
    }

    @Override
    public boolean isProxyFactory() {
        return true;
    }

    private static ExtensionHandler extensionHandlerForMethodHandle(MethodHandle methodHandle) {
        return (handleSupplier, target, args) -> {
            if (target == null) {
                throw new IllegalArgumentException("no object present, called from a proxy factory?");
            }
            return Unchecked.<Object[], Object>function(methodHandle.bindTo(target)::invokeWithArguments).apply(args);
        };
    }

    private static Method privateLookupIn() {
        try {
            return MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException ignored) {
            // Method was added in JDK 9. - @TODO remove this hack when we move to JDK11+
            return null;
        }
    }

    private static MethodHandles.Lookup lookupFor(Class<?> clazz) {
        if (PRIVATE_LOOKUP_IN != null) {
            try {
                return (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, clazz, MethodHandles.lookup());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnableToCreateExtensionException(e,
                        "Error invoking MethodHandles.privateLookupIn(%s.class, MethodHandles.lookup()) in JDK 9+ runtime", clazz);
            }
        }

        // TERRIBLE, HORRIBLE, NO GOOD, VERY BAD HACK
        // Courtesy of:
        // https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/

        // We can use MethodHandles to look up and invoke the super method, but since this class is not an
        // implementation of method.getDeclaringClass(), MethodHandles.Lookup will throw an exception since
        // this class doesn't have access to the super method, according to Java's access rules. This horrible,
        // awful workaround allows us to directly invoke MethodHandles.Lookup's private constructor, bypassing
        // the usual access checks.

        // This workaround is only used in JDK 8.x runtimes. JDK 9+ runtimes use MethodHandles.privateLookupIn()
        // above.
        return PRIVATE_LOOKUPS.computeIfAbsent(clazz, Unchecked.function(InternalExtensionFactory::getConstructorLookup));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static MethodHandles.Lookup getConstructorLookup(Class<?> type) throws ReflectiveOperationException {
        Constructor<Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);

        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }

        return constructor.newInstance(type, ANY_ACCESS);
    }

    /**
     * Provides {@link ExtensionHandler} instances for all methods that have not been covered in
     * any other way. It forwards a call to the handler to a method invocation on the target
     * object. For any extension factory that simply provides an implementation of the extension
     * interface, this forwards the call to the method on the implementation. The extension framework
     * wraps these calls into invocations that manage the extension context for the handle correctly
     * so that logging will work for all extension.
     */
    private static class InstanceExtensionHandlerFactory implements ExtensionHandlerFactory {

        @Override
        public boolean accepts(Class<?> extensionType, Method method) {
            return true;
        }

        @Override
        public Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method) {
            try {
                Class<?> declaringClass = method.getDeclaringClass();
                final MethodHandle methodHandle = lookupFor(declaringClass).unreflect(method);
                return Optional.of(extensionHandlerForMethodHandle(methodHandle));
            } catch (IllegalAccessException e) {
                throw new UnableToCreateExtensionException(e, "Default handler for %s couldn't unreflect %s", extensionType, method);
            }
        }
    }

    private static class BridgeMethodExtensionHandlerFactory implements ExtensionHandlerFactory {

        @Override
        public boolean accepts(Class<?> extensionType, Method method) {
            return method.isBridge();
        }

        @Override
        public Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method) {

            List<Method> candidates = Stream.of(extensionType.getMethods())
                    .filter(candidate -> !candidate.isBridge())
                    .filter(candidate -> Objects.equals(candidate.getName(), method.getName()))
                    .filter(candidate -> candidate.getParameterCount() == method.getParameterCount())
                    .filter(candidate -> {
                        Class<?>[] candidateParamTypes = candidate.getParameterTypes();
                        Class<?>[] methodParamTypes = method.getParameterTypes();
                        return IntStream.range(0, method.getParameterCount())
                                .allMatch(i -> methodParamTypes[i].isAssignableFrom(candidateParamTypes[i]));
                    })
                    .collect(Collectors.toList());

            Optional<ExtensionHandler> result = Optional.empty();
            for (Method candidate : candidates) {
                try {
                    final MethodHandle methodHandle = lookupFor(extensionType).unreflect(candidate);
                    result = Optional.of(extensionHandlerForMethodHandle(methodHandle));
                    break;
                } catch (IllegalAccessException e) {
                    throw new UnableToCreateExtensionException(e,
                            "Could not create an extension handler for bridge method %s#%s, could not unreflect %s", extensionType, method, candidate);
                }
            }

            return result;
        }
    }
}
