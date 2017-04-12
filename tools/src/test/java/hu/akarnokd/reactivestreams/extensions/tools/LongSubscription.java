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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscription;

/**
 * Subscription implementation that accumulates requests
 * and can be checked for a cancelled state.
 */
final class LongSubscription extends AtomicLong implements Subscription {

    private static final long serialVersionUID = 4615584786703011394L;

    volatile boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public long requested() {
        return get();
    }

    @Override
    public void request(long n) {
        if (n <= 0L) {
            throw new InternalError("Non-positive request amount: " + n);
        }
        SubscriptionTools.getAndAddRequested(this, n);
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
