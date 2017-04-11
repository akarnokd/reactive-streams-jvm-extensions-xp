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

public final class SubscriptionHelper {

    private SubscriptionHelper() {
        throw new IllegalStateException("No instances!");
    }

    static final TerminalThrowable TERMINATED = new TerminalThrowable();

    public static long addAndCap(long a, long b) {
        long c = a + b;
        return c < 0 ? Long.MAX_VALUE : c;
    }

    public static long multiplyCap(long a, long b) {
        long u = a * b;
        if (((a | b) >>> 31) != 0) {
            if (u / a != b) {
                return Long.MAX_VALUE;
            }
        }
        return u;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Atomic classes versions
    ///////////////////////////////////////////////////////////////////////////////////////////

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

    public static void clear(AtomicReference<Subscription> field) {
        field.lazySet(CancelledSubscription.INSTANCE);
    }

    public static boolean isCancelled(AtomicReference<Subscription> field) {
        return field.get() == CancelledSubscription.INSTANCE;
    }

    public static boolean isCancelled(Subscription subscription) {
        return subscription == CancelledSubscription.INSTANCE;
    }

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

    public enum SetOnceResult {
        SUCCESS,
        ALREADY_SET,
        CANCELLED
    }

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

    public static long subtractAndGetRequested(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = r - n;
            if (u < 0L) {
                throw new IllegalStateException("Can't have negative requested amount: " + u);
            }
            if (requested.compareAndSet(r, u)) {
                return u;
            }
        }
    }

    public static boolean isTerminalThrowable(AtomicReference<Throwable> error) {
        return error.get() == TERMINATED;
    }

    public static boolean isTerminalThrowable(Throwable error) {
        return error == TERMINATED;
    }

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

    public static <T> boolean serializedOnError(Subscriber<? super T> subscriber, AtomicLong wip, AtomicReference<Throwable> error, Throwable t) {
        if (error.compareAndSet(null, t)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onError(t);
            }
            return true;
        }
        return false;
    }

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

    public static <T> boolean cancel(T instance, AtomicReferenceFieldUpdater<T, Subscription> updater) {
        Subscription current = updater.get(instance);
        if (current != CancelledSubscription.INSTANCE) {
            current = updater.getAndSet(instance, CancelledSubscription.INSTANCE);
            if (current != CancelledSubscription.INSTANCE) {
                if (current != null) {
                    current.cancel();
                }
                return true;
            }
        }
        return false;
    }

}