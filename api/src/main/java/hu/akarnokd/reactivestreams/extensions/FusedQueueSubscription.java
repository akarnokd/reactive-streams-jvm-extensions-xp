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

import org.reactivestreams.*;

/**
 * Combination of a {@link Subscription} and a {@link FusedQueue} that let's establish
 * a fusion mode between subsequent operators.
 * <p>
 * By implementing {@link FusedQueue}, an upstream {@code FusedQueueSubscription} can take
 * the place of a regular, in-memory queue and establish a fused chain of queues.
 * <p>
 * When an upstream {@code Publisher} is fusion-capable, it should signal a
 * {@code FusedQueueSubscription} via {@code Subscriber#onSubscribe}. If the downstream
 * {@code Subscriber} is not fusion-ready, it can simply ignore the methods other
 * than of {@code Subscription} and work as a regular, non-fused chain. 
 * <p>
 * If the downstream is fusion-enabled, it may detect this feature via an {@code instanceof} check, then
 * call the {@link #requestFusion(int)} with the desired fusion mode ({@link #NONE}, {@link #SYNC},
 * {@link #ASYNC} and the latter two optionally binary-or'd with {@link #BOUNDARY}).
 * <p>
 * The standard {@code Publisher} protocol is extended as follows:
 * <p>
 * <code><pre>
 *             /-&gt; requestFusion == NONE  -&gt; onNext(T)*    (onError | onComplete)?
 * onSubscribe |-&gt; requestFusion == SYNC
 *             \-&gt; requestFusion == ASYNC -&gt; onNext(null)* (onError | onComplete)?
 * </pre></code>
 * The standard {@code Subscriber} changes to the following in an established fusion mode:
 * <code><pre>
 * requestFusion == SYNC  -&gt; (poll | isEmpty)* clear?
 * requestFusion == ASYNC -&gt; (request | poll | isEmpty)* clear?
 * </pre></code>
 * The {@code requestFusion()} method should be called from within the {@code onSubscribe()} method
 * and before any issue of {@code Subscription.request()}. Calling {@code requestFusion()}
 * multiple times, after a {@code request()} call or from within any other {&code onXXX} methods
 * is undefined behavior.
 * <p>
 * The methods {@link #poll()}, {@link #isEmpty()} and {@link #clear()} should be called in a sequential
 * manner.
 * <p>
 * The method {@link #offer(Object)} should not be called when a fusion mode is established
 * (otherwise it is undefined behavior).
 * <p>
 * <h3>Synchronous fusion (<code>requestFusion(SYNC)</code>)</h3>
 * The upstream is able to produce items in a synchronous fashion and
 * return them via {@link FusedQueue#poll()}, then indicating a terminal state via returning
 * null or <code>{@link FusedQueue#isEmpty()} == true</code>.
 * <p>
 * In sync-fused mode, the upstream should not call {@code onNext}, {@code onError} 
 * or {@code onComplete} on the {@code Subscriber} and the {@code Subscriber} should not
 * call {@code Subscription.request()} at all (doing so is undefined behavior).
 * Items should be consumed via {@link #poll()}, errors should be reported by throwing from
 * {@code poll} and the source is considered done when {@code poll()} returns null or
 * {@code isEmpty()} is true.
 * <h3>Asynchronous fusion (<code>requestFusion(ASYNC)</code>)</h3>
 * The upstream is able to produce items in an asynchronous fashion, that is,
 * {@link #poll()} may return null if there is no item available currently from upstream.
 * <p>
 * In sync-fused mode, the upstream should call {@code onNext} with an arbitrary value
 * (including {@code null}) to indicate there are items available via {@link #poll()}
 * and implementors of {@code Subscriber.onNext()} should ignore this value.
 * In addition, the upstream should still call {@code onError} if an error occurs and
 * indicate completion via {@code onComplete}. The downstream should still call 
 * {@code Subscription.request()} to indicate demand.
 * <h3>Boundary fusion (<code>requestFusion(ANY | BOUNDARY)</code>)</h3>
 * The downstream is an explicit asynchronous boundary and certain
 * upstream sources may chose not to fuse in this case.
 * <p>
 * The fusion is usually established by appending processing logic to the {@link #poll()}
 * side of the queue, for example, mapping an upstream value polled from an upstream queue:
 * <code><pre>
 * &#64;Override
 * public R poll() throws Throwable {
 *     T value = upstream.poll();
 *     if (value != null) {
 *         return Objects.requireNonNull(mapper.apply(value), "The mapper returned a null value");
 *     }
 *     return null;
 * }
 * </pre></code>
 * With an explicit boundary, the computation in {@code mapper.apply()} may happen on the other
 * side of the boundary on a thread not necessary desired by the function (for example,
 * a blocking network call that would end up on the UI thread via a downstream {@code poll()}).
 * <p>
 * @param <T> the value type of the queue.
 */
public interface FusedQueueSubscription<T> extends FusedQueue<T>, Subscription {

    /**
     * Returned by {@link #requestFusion(int)} indicating that the desired fusion mode
     * is not available.
     */
    int NONE = 0;

    /**
     * Offered as a parameter to {@link #requestFusion(int)} and may be returned by it,
     * indicating the upstream is able to produce items in a synchronous fashion and
     * return them via {@link FusedQueue#poll()}, then indicating a terminal state via returning
     * null or <code>{@link FusedQueue#isEmpty()} == true</code>.
     * <p>
     * In sync-fused mode, the upstream should not call {@code onNext}, {@code onError} 
     * or {@code onComplete} on the {@code Subscriber} and the {@code Subscriber} should not
     * call {@code Subscription.request()} at all (doing so is undefined behavior).
     * Items should be consumed via {@link #poll()}, errors should be reported by throwing from
     * {@code poll} and the source is considered done when {@code poll()} returns null or
     * {@code isEmpty()} is true.
     * see {@link #ASYNC}
     */
    int SYNC = 1;

    /**
     * Offered as a parameter to {@link #requestFusion(int)} and may be returned by it,
     * indicating the upstream is able to produce items in an asynchronous fashion, that is,
     * {@link #poll()} may return null if there is no item available currently from upstream.
     * <p>
     * In sync-fused mode, the upstream should call {@code onNext} with an arbitrary value
     * (including {@code null}) to indicate there are items available via {@link #poll()}
     * and implementors of {@code Subscriber.onNext()} should ignore this value.
     * In addition, the upstream should still call {@code onError} if an error occurs and
     * indicate completion via {@code onComplete}. The downstream should still call 
     * {@code Subscription.request()} to indicate demand.
     */
    int ASYNC = 2;

    /**
     * Offered as a parameter to {@link #requestFusion(int)} to indicate the downstream
     * is able to work with both {@link #SYNC} and {@link #ASYNC} fused upstream.
     * @see #SYNC
     * @see #ASYNC
     */
    int ANY = SYNC | ASYNC;

    /**
     * Binary or-ed with the other constants and offered to {@link #requestFusion(int)}
     * to indicate the downstream is an explicit asynchronous boundary and certain
     * upstream sources may chose not to fuse in this case.
     * <p>
     * The fusion is usually established by appending processing logic to the {@link #poll()}
     * side of the queue, for example, mapping an upstream value polled from an upstream queue:
     * <code><pre>
     * &#64;Override
     * public R poll() throws Throwable {
     *     T value = upstream.poll();
     *     if (value != null) {
     *         return Objects.requireNonNull(mapper.apply(value), "The mapper returned a null value");
     *     }
     *     return null;
     * }
     * </pre></code>
     * With an explicit boundary, the computation in {@code mapper.apply()} may happen on the other
     * side of the boundary on a thread not necessary desired by the function (for example,
     * a blocking network call that would end up on the UI thread via a downstream {@code poll()}).
     */
    int BOUNDARY = 4;

    /**
     * Requests the upstream to enter the specified fusion mode.
     * <p>
     * The {@code requestFusion()} method should be called from within the {@code onSubscribe()} method
     * and before any issue of {@code Subscription.request()}. Calling {@code requestFusion()}
     * multiple times, after a {@code request()} call or from within any other {&code onXXX} methods
     * is undefined behavior.
     * <p>
     * Most consumers will likely call with {@link #ANY} or {@link #ANY} | {@link #BOUNDARY} because
     * consuming in either sync or async mode can be implemented largely the same way.
     * <p> 
     * @param mode the requested fusion mode, allowed values are
     * <ul>
     * <li>{@link #SYNC} - request a pure synchronous fusion mode</li>
     * <li>{@link #SYNC} | {@link #BOUNDARY} - request a pure synchronous fusion mode over an asynchronous boundary</li>
     * <li>{@link #ASYNC} - request a pure asynchronous fusion mode</li>
     * <li>{@link #ASYNC} | {@link #BOUNDARY}</li>
     * <li>{@link #SYNC} | {@link #ASYNC}</li>
     * <li>{@link #SYNC} | {@link #ASYNC} | {@link #BOUNDARY}</li>
     * <li>{@link #ANY}</li>
     * <li>{@link #ANY} | {@link #BOUNDARY}</li>
     * </ul>
     * @return the established fusion mode, possible values are: {@link #NONE}, {@link #SYNC} or {@link #ASYNC} 
     */
    int requestFusion(int mode);
}
