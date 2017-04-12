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

package hu.akarnokd.reactivestreams.extensions.tools;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;

/**
 * Utility class supporting atomic operations with {@link Subscription}s,
 * {@link Throwable}s and {@link Subscriber}s.
 */
public final class SubscriptionTools {

    /** Utility class. */
    private SubscriptionTools() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * The shared terminal indicator for Throwable atomics; should not be emitted
     * via a {@code Subscriber.onError()} as it is meant for an indicator.
     */
    static final TerminalThrowable TERMINATED = new TerminalThrowable();

    /**
     * Adds two non-negative long values and caps the result at {@code Long.MAX_VALUE}.
     * @param a the first value, non-negative (not verified)
     * @param b the second value, non-negative (not verified)
     * @return the sum of the two values capped at {@code Long.MAX_VALUE}
     * @see #multiplyCap(long, long)
     */
    public static long addAndCap(long a, long b) {
        long c = a + b;
        return c < 0 ? Long.MAX_VALUE : c;
    }

    /**
     * Multiplies two non-negative long values and caps the result at {@code Long.MAX_VALUE}.
     * @param a the first value, non-negative (not verified)
     * @param b the second value, non-negative (not verified)
     * @return the product of the two values capped at {@code Long.MAX_VALUE}
     * @see #addAndCap(long, long)
     */
    public static long multiplyCap(long a, long b) {
        long u = a * b;
        if (((a | b) >>> 31) != 0) {
            if (u / a != b) {
                return Long.MAX_VALUE;
            }
        }
        return u;
    }

    /**
     * Checks if the given error instance is the shared terminal-indicator Throwable instance.
     * @param error the error to check
     * @return true if the Throwable instance is the terminated instance
     * @see #isTerminalThrowable(AtomicReference)
     * @see #isTerminalThrowable(Object, AtomicReferenceFieldUpdater)
     */
    public static boolean isTerminalThrowable(Throwable error) {
        return error == TERMINATED;
    }

    /**
     * Checks if the given Subscription is the shared cancelled-indicator Subscription instance.
     * @param subscription the subscription to check
     * @return true if the subscription instanceis the cancelled instance
     * @see #isCancelled(AtomicReference)
     * @see #isCancelled(Object, AtomicReferenceFieldUpdater)
     */
    public static boolean isCancelled(Subscription subscription) {
        return subscription == CancelledSubscription.INSTANCE;
    }

    /**
     * The result from calling {@code setOnce}.
     */
    public enum SetOnceResult {
        /** The new Subscription was successfully set. */
        SUCCESS,
        /** Some other Subscription was already set. */
        ALREADY_SET,
        /** The target was alreday cancelled. */
        CANCELLED
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Atomic classes versions
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Atomically swaps in the shared cancelled Subscription instance
     * and cancels the previous Subscription in the field if there
     * was any.
     * <p>
     * This operation makes sure that any subsequent {@code setOnce},
     * {@code replace}, {@code update} gets its Subscription cancelled.
     * <p>
     * Sometimes, this is called deferred cancellation because if
     * the actual Subscription is in time or late, in both cases it
     * will be cancelled eventually.
     * @param field the target field to cancel the contents
     * @return true if the current thread successfully cancelled the
     * contents.
     * @see #update(AtomicReference, Subscription)
     * @see #replace(AtomicReference, Subscription)
     * @see #setOnce(AtomicReference, Subscription)
     */
    public static boolean cancel(AtomicReference<Subscription> field) {
        Subscription current = field.get();
        if (current != CancelledSubscription.INSTANCE) {
            current = field.getAndSet(CancelledSubscription.INSTANCE);
            if (current != CancelledSubscription.INSTANCE) {
                if (current != null) {
                    current.cancel();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the shared cancelled-indicator on the target AtomicReference field
     * in an via lazySet; any previous value is simply overwritten.
     * @param field the target AtomicReference field, not null
     * @see #cancel(AtomicReference)
     */
    public static void clear(AtomicReference<Subscription> field) {
        field.lazySet(CancelledSubscription.INSTANCE);
    }

    /**
     * Returns true if the target AtomicReference contains the shared
     * cancelled-indicator Subscription instance.
     * @param field the target AtomicReference, not null
     * @return true if the target contains the cancelled instance
     */
    public static boolean isCancelled(AtomicReference<Subscription> field) {
        return field.get() == CancelledSubscription.INSTANCE;
    }

    /**
     * Atomically replaces the Subscription in the AtomicReference with the provided
     * value or cancels this value if the field contains the shared cancelled-indicator
     * instance.
     * @param field the target AtomicReference field, not null
     * @param next the Subscription to replace the current contents, may be null
     * @return true if the replacement succeeded, false if the field contains the
     * cancelled-indicator
     */
    public static boolean replace(AtomicReference<Subscription> field, Subscription next) {
        for (;;) {
            Subscription current = field.get();
            if (current == CancelledSubscription.INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    /**
     * Atomically updates the Subscription in the AtomicReference with the provided
     * value and cancels the previous Subscription (if not null) or cancels this
     * value if the field contains the shared cancelled-indicator instance.
     * @param field the target AtomicReference field, not null
     * @param next the Subscription to replace the current contents, may be null
     * @return true if the replacement succeeded, false if the field contains the
     * cancelled-indicator
     */
    public static boolean update(AtomicReference<Subscription> field, Subscription next) {
        for (;;) {
            Subscription current = field.get();
            if (current == CancelledSubscription.INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(current, next)) {
                if (current != null) {
                    current.cancel();
                }
                return true;
            }
        }
    }

    /**
     * Atomically sets the only non-null Subscription on the target AtomicReference if
     * it is null, otherwise cancels the Subscription and returns the
     * result of the operation (success, already set, cancelled).
     * @param field the target AtomicReferfence field
     * @param subscription the Subscription to set, not null
     * @return the result of the operation: success, already set, cancelled
     * @throws NullPointerException if {@code subscription} is null
     */
    public static SetOnceResult setOnce(AtomicReference<Subscription> field, Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription is null");
        }
        if (!field.compareAndSet(null, subscription)) {
            subscription.cancel();
            if (field.get() == CancelledSubscription.INSTANCE) {
                return SetOnceResult.CANCELLED;
            }
            return SetOnceResult.ALREADY_SET;
        }
        return SetOnceResult.SUCCESS;
    }

    /**
     * Atomically sets the only non-null Subscription on the target AtomicReference if
     * it is null then atomically replaces the value in the requested AtomicLong and
     * if it it was non-zero, requests that amount from the subscription;
     * otherwise cancels the Subscription and returns the
     * result of the operation (success, already set, cancelled).
     * <p>
     * This method is useful when the upstream Subscription may appear
     * later than any potential request from the downstream and
     * the two have to catch up with each other eventually.
     * @param field the target AtomicReferfence field
     * @param requested the AtomicLong field containing the accumulated requested amount
     * @param subscription the Subscription to set, not null
     * @return the result of the operation: success, already set, cancelled
     * @see #deferredRequest(AtomicReference, AtomicLong, long)
     */
    public static SetOnceResult deferredSetOnce(AtomicReference<Subscription> field, AtomicLong requested, Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription is null");
        }
        if (!field.compareAndSet(null, subscription)) {
            subscription.cancel();
            if (field.get() == CancelledSubscription.INSTANCE) {
                return SetOnceResult.CANCELLED;
            }
            return SetOnceResult.ALREADY_SET;
        }

        long r = requested.getAndSet(0L);
        if (r != 0L) {
            subscription.request(r);
        }
        return SetOnceResult.SUCCESS;
    }

    /**
     * Requests the specified amount from the Subscription inside the AtomicReference field or
     * stores the amount in the requested AtomicLong till the Subscription arrives.
     * <p>
     * This is the pair of the {@link #deferredSetOnce(AtomicReference, AtomicLong, Subscription)} method,
     * having both ensures that if the request amount comes before the Subscription arrives, the
     * request amount is accumulated and then requested from upstream once possible.
     * @param field the target AtomicReferfence field
     * @param requested the AtomicLong field containing the accumulated requested amount
     * @param n the request amount, positive (not validated)
     * @return true if direct requesting was possible, false if the request was accumulated in the requested AtomicLong
     * @see #deferredSetOnce(AtomicReference, AtomicLong, Subscription)
     */
    public static boolean deferredRequest(AtomicReference<Subscription> field, AtomicLong requested, long n) {
        Subscription current = field.get();
        if (current != null) {
            current.request(n);
            return true;
        }
        getAndAddRequested(requested, n);
        current = field.get();
        if (current != null) {
            long r = requested.getAndSet(0L);
            if (r != 0L) {
                current.request(r);
                return true;
            }
        }
        return false;
    }

    /**
     * Atomically adds the given amount to the requested AtomicLong, capped at
     * Long.MAX_VALUE, if the current value is not Long.MAX_VALUE already,
     * and returns the original value before the addition.
     * @param requested the target requested AtomicLong
     * @param n the number to add, positive (not validated)
     * @return the original value before the addition
     */
    public static long getAndAddRequested(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = addAndCap(r, n);
            if (requested.compareAndSet(r, u)) {
                return r;
            }
        }
    }

    /**
     * Atomically subtracts the given amount from the requested AtomicLong, if the current
     * value is not Long.MAX_VALUE, and returns the new value after.
     * @param requested the target requested AtomicLong
     * @param n the number to subtract, positive (not validated)
     * @return the new value after the subtraction
     * @throws IllegalArgumentException if n is larger than the amount in the AtomicLong
     * @see #getAndAddRequested(AtomicLong, long)
     */
    public static long subtractAndGetRequested(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = r - n;
            if (u < 0L) {
                throw new IllegalArgumentException("Can't have negative requested amount: " + u);
            }
            if (requested.compareAndSet(r, u)) {
                return u;
            }
        }
    }

    /**
     * Checks if the given AtomicReference contains the shared terminal-indicator Throwable instance.
     * @param error the target AtomicReference to check
     * @return true if the target contains the terminal-indicator instance
     */
    public static boolean isTerminalThrowable(AtomicReference<Throwable> error) {
        return error.get() == TERMINATED;
    }

    /**
     * Atomically calls onNext on the given subscriber if there was no concurrent serialized call from the
     * other similar methods.
     * <p>
     * The aim is to serialize a single thread emitting onNext items with any other thread(s) calling
     * {@link #serializedOnError(Subscriber, AtomicLong, AtomicReference, Throwable)} or
     * {@link #serializedOnComplete(Subscriber, AtomicLong, AtomicReference)} methods by making sure once
     * the onNext item was emitted, any terminal signal is also emitted immediately.
     * @param <T> the value type
     * @param subscriber the target subscriber receiving signals in a serialized manner.
     * @param wip the work-in-progress indicator when an onNext call is being emitted.
     * @param error the error AtomicReference temporarily holding the error or terminal signal
     * @param item the item to emit via onNext
     * @return true if the emission succeeded, false if an async terminal event was first emitted to
     * the subscriber
     * @see #serializedOnError(Subscriber, AtomicLong, AtomicReference, Throwable)
     * @see #serializedOnComplete(Subscriber, AtomicLong, AtomicReference)
     * @see #serializedTryOnNext(ConditionalSubscriber, AtomicLong, AtomicReference, Object)
     */
    public static <T> boolean serializedOnNext(Subscriber<? super T> subscriber, AtomicLong wip, AtomicReference<Throwable> error, T item) {
        if (wip.get() == 0L && wip.compareAndSet(0, 1)) {
            subscriber.onNext(item);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = error.get();
                if (ex == TERMINATED) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(ex);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Atomically calls tryOnNext on the given conditional subscriber if there was no concurrent serialized call from the
     * other similar methods.
     * <p>
     * The aim is to serialize a single thread emitting tryOnNext items with any other thread(s) calling
     * {@link #serializedOnError(Subscriber, AtomicLong, AtomicReference, Throwable)} or
     * {@link #serializedOnComplete(Subscriber, AtomicLong, AtomicReference)} methods by making sure once
     * the tryOnNext item was emitted, any terminal signal is also emitted immediately.
     * @param <T> the value type
     * @param subscriber the target subscriber receiving signals in a serialized manner.
     * @param wip the work-in-progress indicator when an onNext call is being emitted.
     * @param error the error AtomicReference temporarily holding the error or terminal signal
     * @param item the item to emit via onNext
     * @return true if both the emission and the returned value of tryOnNext is true
     * (successful consumption), false otherwise (failed consumption or terminal event already emitted)
     * @see #serializedOnError(Subscriber, AtomicLong, AtomicReference, Throwable)
     * @see #serializedOnComplete(Subscriber, AtomicLong, AtomicReference)
     */
    public static <T> boolean serializedTryOnNext(ConditionalSubscriber<? super T> subscriber, AtomicLong wip, AtomicReference<Throwable> error, T item) {
        if (wip.get() == 0L && wip.compareAndSet(0, 1)) {
            boolean b = subscriber.tryOnNext(item);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = error.get();
                if (ex == TERMINATED) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(ex);
                }
            }
            return b;
        }
        return false;
    }

    /**
     * Atomically calls onError on the target subscriber if there is no ongoing onNext or tryOnNext emissions,
     * otherwise stores the error Throwable, if the target error AtomicReference is empty, so the onNext can
     * pick it up once its emission completes.
     * @param <T> the value type
     * @param subscriber the target subscriber receiving signals in a serialized manner.
     * @param wip the work-in-progress indicator when an onNext call is being emitted.
     * @param error the error AtomicReference temporarily holding the error or terminal signal
     * @param t the Throwable instance to emit or store for later emission
     * @return true if the Throwable error was emitted immediately or was successfully stored in the error AtomicReference,
     * false if there was a terminal indicator in the error AtomicReference already
     * @see #serializedOnComplete(Subscriber, AtomicLong, AtomicReference)
     * @see #serializedOnNext(Subscriber, AtomicLong, AtomicReference, Object)
     * @see #serializedTryOnNext(ConditionalSubscriber, AtomicLong, AtomicReference, Object)
     */
    public static <T> boolean serializedOnError(Subscriber<? super T> subscriber, AtomicLong wip, AtomicReference<Throwable> error, Throwable t) {
        if (error.compareAndSet(null, t)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onError(t);
            }
            return true;
        }
        return false;
    }

    /**
     * Atomically calls onComplete on the target subscriber if there is no ongoing onNext or tryOnNext emissions,
     * otherwise stores a completion indicator in the error AtomicReference, if the target error AtomicReference is
     * empty, so the onNext can pick it up once its emission completes.
     * @param <T> the value type
     * @param subscriber the target subscriber receiving signals in a serialized manner.
     * @param wip the work-in-progress indicator when an onNext call is being emitted.
     * @param error the error AtomicReference temporarily holding the error or terminal signal
     * @return true if the onComplete was successfully emitted or its indicator successfully stored in the error AtomicReference,
     * false if there was a terminal indicator in the error AtomicReference already
     * @see #serializedOnNext(Subscriber, AtomicLong, AtomicReference, Object)
     * @see #serializedTryOnNext(ConditionalSubscriber, AtomicLong, AtomicReference, Object)
     * @see #serializedOnError(Subscriber, AtomicLong, AtomicReference, Throwable)
     */
    public static <T> boolean serializedOnComplete(Subscriber<? super T> subscriber, AtomicLong wip, AtomicReference<Throwable> error) {
        if (error.compareAndSet(null, TERMINATED)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onComplete();
            }
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Field updater versions
    ///////////////////////////////////////////////////////////////////////////////////////////

    public static <T> boolean cancel(T instance, AtomicReferenceFieldUpdater<T, Subscription> field) {
        Subscription current = field.get(instance);
        if (current != CancelledSubscription.INSTANCE) {
            current = field.getAndSet(instance, CancelledSubscription.INSTANCE);
            if (current != CancelledSubscription.INSTANCE) {
                if (current != null) {
                    current.cancel();
                }
                return true;
            }
        }
        return false;
    }

    public static <T> void clear(T instance, AtomicReferenceFieldUpdater<T, Subscription> field) {
        field.lazySet(instance, CancelledSubscription.INSTANCE);
    }

    public static <T> boolean isCancelled(T instance, AtomicReferenceFieldUpdater<T, Subscription> field) {
        return field.get(instance) == CancelledSubscription.INSTANCE;
    }

    public static <T> boolean replace(T instance, AtomicReferenceFieldUpdater<T, Subscription> field, Subscription next) {
        for (;;) {
            Subscription current = field.get(instance);
            if (current == CancelledSubscription.INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(instance, current, next)) {
                return true;
            }
        }
    }

    public static <T> boolean update(T instance, AtomicReferenceFieldUpdater<T, Subscription> field, Subscription next) {
        for (;;) {
            Subscription current = field.get(instance);
            if (current == CancelledSubscription.INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(instance, current, next)) {
                if (current != null) {
                    current.cancel();
                }
                return true;
            }
        }
    }

    public static <T> SetOnceResult setOnce(T instance, AtomicReferenceFieldUpdater<T, Subscription> field, Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription is null");
        }
        if (!field.compareAndSet(instance, null, subscription)) {
            subscription.cancel();
            if (field.get(instance) == CancelledSubscription.INSTANCE) {
                return SetOnceResult.CANCELLED;
            }
            return SetOnceResult.ALREADY_SET;
        }
        return SetOnceResult.SUCCESS;
    }

    public static <T> SetOnceResult deferredSetOnce(T instance, AtomicReferenceFieldUpdater<T, Subscription> field, AtomicLongFieldUpdater<T> requested, Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription is null");
        }
        if (!field.compareAndSet(instance, null, subscription)) {
            subscription.cancel();
            if (field.get(instance) == CancelledSubscription.INSTANCE) {
                return SetOnceResult.CANCELLED;
            }
            return SetOnceResult.ALREADY_SET;
        }

        long r = requested.getAndSet(instance, 0L);
        if (r != 0L) {
            subscription.request(r);
        }
        return SetOnceResult.SUCCESS;
    }

    public static <T> boolean deferredRequest(T instance, AtomicReferenceFieldUpdater<T, Subscription> field, AtomicLongFieldUpdater<T> requested, long n) {
        Subscription current = field.get(instance);
        if (current != null) {
            current.request(n);
            return true;
        }
        getAndAddRequested(instance, requested, n);
        current = field.get(instance);
        if (current != null) {
            long r = requested.getAndSet(instance, 0L);
            if (r != 0L) {
                current.request(r);
                return true;
            }
        }
        return false;
    }

    public static <T> long getAndAddRequested(T instance, AtomicLongFieldUpdater<T> requested, long n) {
        for (;;) {
            long r = requested.get(instance);
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = addAndCap(r, n);
            if (requested.compareAndSet(instance, r, u)) {
                return r;
            }
        }
    }

    public static <T> long subtractAndGetRequested(T instance, AtomicLongFieldUpdater<T> requested, long n) {
        for (;;) {
            long r = requested.get(instance);
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = r - n;
            if (u < 0L) {
                throw new IllegalArgumentException("Can't have negative requested amount: " + u);
            }
            if (requested.compareAndSet(instance, r, u)) {
                return u;
            }
        }
    }

    public static <T> boolean isTerminalThrowable(T instance, AtomicReferenceFieldUpdater<T, Throwable> error) {
        return error.get(instance) == TERMINATED;
    }

    public static <T, U> boolean serializedOnNext(Subscriber<? super T> subscriber, U instance, AtomicLongFieldUpdater<U> wip, AtomicReferenceFieldUpdater<U, Throwable> error, T item) {
        if (wip.get(instance) == 0L && wip.compareAndSet(instance, 0, 1)) {
            subscriber.onNext(item);
            if (wip.decrementAndGet(instance) != 0) {
                Throwable ex = error.get(instance);
                if (ex == TERMINATED) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(ex);
                }
            }
            return true;
        }
        return false;
    }

    public static <T, U> boolean serializedTryOnNext(ConditionalSubscriber<? super T> subscriber, U instance, AtomicLongFieldUpdater<U> wip, AtomicReferenceFieldUpdater<U, Throwable> error, T item) {
        if (wip.get(instance) == 0L && wip.compareAndSet(instance, 0, 1)) {
            boolean b = subscriber.tryOnNext(item);
            if (wip.decrementAndGet(instance) != 0) {
                Throwable ex = error.get(instance);
                if (ex == TERMINATED) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(ex);
                }
            }
            return b;
        }
        return false;
    }

    public static <T, U> boolean serializedOnError(Subscriber<? super T> subscriber, U instance, AtomicLongFieldUpdater<U> wip, AtomicReferenceFieldUpdater<U, Throwable> error, Throwable t) {
        if (error.compareAndSet(instance, null, t)) {
            if (wip.getAndIncrement(instance) == 0) {
                subscriber.onError(t);
            }
            return true;
        }
        return false;
    }

    public static <T, U> boolean serializedOnComplete(Subscriber<? super T> subscriber, U instance, AtomicLongFieldUpdater<U> wip, AtomicReferenceFieldUpdater<U, Throwable> error) {
        if (error.compareAndSet(instance, null, TERMINATED)) {
            if (wip.getAndIncrement(instance) == 0) {
                subscriber.onComplete();
            }
            return true;
        }
        return false;
    }
}