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
 * Represents a {@link Publisher} source that can return a constant zero or one
 * value synchronously via {@link #value()}.
 * <p>
 * This {@code Publisher} adds restriction to the {@link DynamicValuePublisher}
 * that the {@link #value()} can't throw and should return a constant or
 * is allowed perform computation during assembly time.
 *
 * @param <T> the value type
 */
public interface ConstantValuePublisher<T> extends DynamicValuePublisher<T> {

    /**
     * Returns a single value or null indicating emptiness.
     */
    @Override
    T value();
}
