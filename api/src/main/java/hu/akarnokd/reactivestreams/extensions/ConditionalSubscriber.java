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

/**
 * Represents a {@link RelaxedSubscriber} that offers a {@link #tryOnNext(Object)} method that
 * can indicate if the particular item has been consumed or not; if not, the
 * upstream is allowed to send the next value without a {@code request(1)}.
 * <p>
 * When implementing {@code ConditionalSubscriber}s, one can define the regular
 * {@link #onNext(Object)} by delegating to {@link #tryOnNext(Object)} and
 * requesting if the method returned false:
 * <code><pre>
 * &#64;Override
 * public void onNext(T t) {
 *     if (!tryOnNext(t) && !done) {
 *         upstream.request(1);
 *     }
 * }
 * </pre></code>
 * where {@code done} is a boolean field indicating the upstream was cancelled by {@code tryOnNext()}
 * and {@code upstream} holds the {@code Subscription} received via {@link #onSubscribe(org.reactivestreams.Subscription)}.
 *
 * @param <T> the value type
 */
public interface ConditionalSubscriber<T> extends RelaxedSubscriber<T> {

    /**
     * Try to process/consume a value.
     * @param t the value to be consumed, may be null if this
     *          ConditionalSubscriber participates in a fused chain.
     * @return true if the value was processed/consumed, false to
     *         indicate the upstream can send the next value immediately
     */
    boolean tryOnNext(T t);

}
