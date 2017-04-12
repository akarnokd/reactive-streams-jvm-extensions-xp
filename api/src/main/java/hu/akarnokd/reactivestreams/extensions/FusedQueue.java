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
 * A common, minimal Queue-like interface to allow enqueueing and dequeueing
 * over an asynchronous boundary between subsequent {@code Publisher}s.
 *
 * @param <T> the value type
 */
public interface FusedQueue<T> {

    /**
     * Try to offer an element into the queue and return true for success.
     * @param element the element to offer, not null
     * @return true if successful, false if the queue is full
     */
    boolean offer(T element);

    /**
     * Poll the next element from the queue, return null for an empty queue or
     * throw an exception if the poll failed.
     * <p>
     * Similar to concurrent Java queues, {@code poll()} should return an item if the
     * queue is supposed to be non-empty. In some queue implementations, temporary
     * holes may appear between elements due to concurrent offer but the next element
     * may not be visible immediately. Implementations usually spin-loop over this case.
     * <p>
     * Dequeueing from an in-memory source usually doesn't throw but due to fusion, functions may
     * get applied to the value in a queue-chain which functions may throw:
     * <pre><code>
     * &#64;Override
     * public R poll() throws Throwable {
     *     T value = upstream.poll();
     *     if (value != null) {
     *         return Objects.requireNonNull(mapper.apply(value), "The mapper returned a null value");
     *     }
     *     return null;
     * }
     * </code></pre>
     * In this example, there is an upstream queue which if returns a value, a mapper function is
     * applied where the {@code apply()} may throw or the function may return an illegal null value.
     * By defining the method as {@code throws Throwable}, consumers are required to handle
     * any exceptions.
     * <p>
     * Note that when implementing the {@code FusedQueue} interface, <code>{@link #isEmpty()} == false</code>
     * may still lead to <code>{@link #poll()} == null</code> because the dequeue logic may chose to drop
     * an item. However, <code>{@link #isEmpty()} == true</code> should result in <code>{@link #poll()} == null</code> if
     * there was no concurrent {@link #offer(Object)} between the two. 
     * @return the value polled, null if the queue is empty.
     * @throws Throwable the exception thrown by the dequeueing logic or the fused chain
     */
    T poll() throws Throwable;

    /**
     * Returns true if this queue is empty.
     * <p>
     * Implementations should not throw. Instead, implementations may defer any exceptions 
     * till the {@link #poll()} is called.
     * <p>
     * Note that when implementing the {@code FusedQueue} interface, <code>{@link #isEmpty()} == false</code>
     * may still lead to <code>{@link #poll()} == null</code> because the dequeue logic may chose to drop
     * an item. However, <code>{@link #isEmpty()} == true</code> should result in <code>{@link #poll()} == null</code> if
     * there was no concurrent {@link #offer(Object)} between the two. 
     * <p>
     * It is not required this method call is thread safe as it is expected to be called from
     * the {@code poll()} side only.
     * @return true if this queue is empty.
     */
    boolean isEmpty();

    /**
     * Clears the contents of the queue.
     * <p>
     * In case {@link #poll()} throws, calling {@code clear()} should be safe.
     * <p>
     * It is not required this method call is thread safe as it is expected to be called from
     * the {@code poll()} side only.
     */
    void clear();
}
