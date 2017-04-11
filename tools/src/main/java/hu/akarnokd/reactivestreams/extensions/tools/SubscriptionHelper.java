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

import org.reactivestreams.Subscription;

public final class SubscriptionHelper {
    
    private SubscriptionHelper() {
        throw new IllegalStateException("No instances!");
    }

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

    public static boolean isCancelled(AtomicReference<Subscription> field) {
        return field.get() == CancelledSubscription.INSTANCE;
    }
    
    public static boolean isCancelled(Subscription subscription) {
        return subscription == CancelledSubscription.INSTANCE;
    }
    
    public static boolean setOnce(AtomicReference<Subscription> field, Subscription subscription) {
        // TODO implement
        return false;
    }

    public static boolean deferredSetOnce(AtomicReference<Subscription> upstream, AtomicLong requested, Subscription subscription) {
        // TODO implement
        return false;
    }

    public static boolean deferredRequest(AtomicReference<Subscription> upstream, AtomicLong requested, long n) {
        // TODO implement
        return false;
    }

    public static long getAndAddRequested(AtomicLong requested, long n) {
        // TODO implement
        return 0L;
    }

    public static long subtractAndGetRequested(AtomicLong requested, long n) {
        // TODO implement
        return 0L;
    }
    
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