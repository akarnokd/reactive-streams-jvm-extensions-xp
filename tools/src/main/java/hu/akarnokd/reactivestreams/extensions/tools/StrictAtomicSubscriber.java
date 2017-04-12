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

public class StrictAtomicSubscriber<T> implements RelaxedSubscriber<T>, Subscription {

    protected final Subscriber<? super T> actual;

    protected final AtomicReference<Subscription> upstream;

    protected final AtomicLong requested;

    protected final AtomicLong wip;

    protected final AtomicReference<Throwable> error;

    protected final AtomicBoolean once;

    public StrictAtomicSubscriber(Subscriber<? super T> actual) {
        this.actual = actual;
        this.upstream = new AtomicReference<Subscription>();
        this.requested = new AtomicLong();
        this.wip = new AtomicLong();
        this.error = new AtomicReference<Throwable>();
        this.once = new AtomicBoolean();
    }

    @Override
    public void onNext(T t) {
        SubscriptionTools.serializedOnNext(actual, wip, error, t);
    }

    @Override
    public void onError(Throwable t) {
        SubscriptionTools.clear(upstream);
        if (!SubscriptionTools.serializedOnError(actual, wip, error, t)) {
            undeliverableException(t);
        }
    }

    @Override
    public void onComplete() {
        SubscriptionTools.clear(upstream);
        SubscriptionTools.serializedOnComplete(actual, wip, error);
    }

    @Override
    public void request(long n) {
        if (n <= 0L) {
            onError(new IllegalArgumentException("§3.9 violated: positive request amount required but it was " + n));
        } else {
            SubscriptionTools.deferredRequest(upstream, requested, n);
        }
    }

    @Override
    public void cancel() {
        SubscriptionTools.cancel(upstream);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (s == null) {
            throw new NullPointerException("s is null");
        }
        if (once.compareAndSet(false, true)) {

            actual.onSubscribe(this);

            SubscriptionTools.deferredSetOnce(upstream, requested, s);
        } else {
            s.cancel();
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
