/*
 * Copyright 2017 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactivestreams.extensions;

import org.reactivestreams.Publisher;

/**
 * Represents a {@link Publisher} source that can return zero or one
 * value synchronously via {@link #value()} or throw an exception.
 * <p>
 * This type of {@code Publisher} should be evaluated at subscription time.
 *
 * @param <T> the value type
 */
public interface DynamicValuePublisher<T> extends Publisher<T> {

    /**
     * Returns the single value, null for indicating emptiness or throws an exception
     * synchronously.
     * @return non-null for a value, null for indicating emptiness
     * @throws Throwable the exception
     */
    T value() throws Throwable;

}
