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

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;

public class StrictVolatileSubscriber<T> implements RelaxedSubscriber<T>, Subscription {

    protected final Subscriber<? super T> actual;

    protected volatile Subscription upstream;
    @SuppressWarnings("rawtypes")
    protected static final AtomicReferenceFieldUpdater<StrictVolatileSubscriber, Subscription> UPSTREAM =
            AtomicReferenceFieldUpdater.newUpdater(StrictVolatileSubscriber.class, Subscription.class, "upstream");

    protected volatile long requested;
    @SuppressWarnings("rawtypes")
    protected static final AtomicLongFieldUpdater<StrictVolatileSubscriber> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(StrictVolatileSubscriber.class, "requested");

    protected volatile long wip;
    @SuppressWarnings("rawtypes")
    protected static final AtomicLongFieldUpdater<StrictVolatileSubscriber> WIP =
            AtomicLongFieldUpdater.newUpdater(StrictVolatileSubscriber.class, "wip");

    protected volatile Throwable error;
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<StrictVolatileSubscriber, Throwable> ERROR =
            AtomicReferenceFieldUpdater.newUpdater(StrictVolatileSubscriber.class, Throwable.class, "error");

    protected volatile int once;
    @SuppressWarnings("rawtypes")
    protected static final AtomicIntegerFieldUpdater<StrictVolatileSubscriber> ONCE =
            AtomicIntegerFieldUpdater.newUpdater(StrictVolatileSubscriber.class, "once");

    public StrictVolatileSubscriber(Subscriber<? super T> actual) {
        this.actual = actual;
    }

    @Override
    public void onNext(T t) {
        SubscriptionTools.serializedOnNext(actual, this, WIP, ERROR, t);
    }

    @Override
    public void onError(Throwable t) {
        SubscriptionTools.clear(this, UPSTREAM);
        if (!SubscriptionTools.serializedOnError(actual, this, WIP, ERROR, t)) {
            undeliverableException(t);
        }
    }

    @Override
    public void onComplete() {
        SubscriptionTools.clear(this, UPSTREAM);
        SubscriptionTools.serializedOnComplete(actual, this, WIP, ERROR);
    }

    @Override
    public void request(long n) {
        if (n <= 0L) {
            onError(new IllegalArgumentException("§3.9 violated: positive request amount required but it was " + n));
        } else {
            SubscriptionTools.deferredRequest(this, UPSTREAM, REQUESTED, n);
        }
    }

    @Override
    public void cancel() {
        SubscriptionTools.cancel(this, UPSTREAM);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (s == null) {
            throw new NullPointerException("s is null");
        }
        if (ONCE.compareAndSet(this, 0, 1)) {

            actual.onSubscribe(this);

            UPSTREAM.lazySet(this, s);
            long r = REQUESTED.getAndSet(this, 0L);
            if (r != 0L) {
                s.request(r);
            }
        } else {
            if (!SubscriptionTools.isCancelled(upstream)) {
                cancel();
                onError(new IllegalStateException("Subscription already set!"));
            }
        }
    }

    protected void undeliverableException(Throwable error) {
        // default is no-op
    }

    @SuppressWarnings("unchecked")
    public static <T> RelaxedSubscriber<T> wrap(Subscriber<? super T> subscriber) {
        if (subscriber instanceof RelaxedSubscriber) {
            return (RelaxedSubscriber<T>)subscriber;
        }
        return new StrictAtomicSubscriber<T>(subscriber);
    }
}
