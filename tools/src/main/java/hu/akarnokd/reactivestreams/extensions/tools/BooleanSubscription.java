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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscription;

/**
 * Helper class implementing Subscription that ignores the request
 * amounts and allows querying for the cancellation state.
 */
final class BooleanSubscription extends AtomicBoolean implements Subscription {

    private static final long serialVersionUID = 2277764714274956560L;

    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
        lazySet(true);
    }

    /**
     * Returns true if this BooleanSubscription has been cancelled.
     * @return true if this BooleanSubscription has been cancelled
     */
    public boolean isCancelled() {
        return get();
    }

    @Override
    public String toString() {
        return "BooleanSubscription(cancelled=" + get() + ")";
    }

}
