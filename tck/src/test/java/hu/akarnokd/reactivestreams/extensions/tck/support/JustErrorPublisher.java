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

package hu.akarnokd.reactivestreams.extensions.tck.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.tools.StrictAtomicSubscriber;

public final class JustErrorPublisher<T> implements Publisher<T> {

    final T item;

    final Throwable error;

    public JustErrorPublisher(T item, Throwable error) {
        this.item = item;
        this.error = error;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        s = StrictAtomicSubscriber.wrap(s);
        s.onSubscribe(new JustSubscription<T>(s, item, error));
    }

    static final class JustSubscription<T> extends AtomicBoolean implements Subscription {

        private static final long serialVersionUID = 4005856977777738160L;

        final Subscriber<? super T> actual;

        final T item;

        final Throwable error;

        volatile boolean cancelled;

        JustSubscription(Subscriber<? super T> actual, T item, Throwable error) {
            this.actual = actual;
            this.item = item;
            this.error = error;
        }

        @Override
        public void request(long n) {
            if (!cancelled && compareAndSet(false, true)) {
                actual.onNext(item);
                if (!cancelled) {
                    actual.onError(error);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
