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

package hu.akarnokd.reactivestreams.extensions.examples;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.*;
import hu.akarnokd.reactivestreams.extensions.tools.*;

public final class FusedRangePublisher implements Publisher<Integer> {

    final int start;

    final int count;

    public FusedRangePublisher(int start, int count) {
        this.start = start;
        this.count = count;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> s) {
        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        }
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new FusedRangeConditionalSubscription((ConditionalSubscriber<? super Integer>)s, start, count));
        } else {
            s = StrictAtomicSubscriber.wrap(s);
            s.onSubscribe(new FusedRangeSubscription(s, start, count));
        }
    }

    static final class FusedRangeSubscription extends AtomicLong implements FusedQueueSubscription<Integer> {

        private static final long serialVersionUID = -216712975160550513L;

        final Subscriber<? super Integer> actual;

        final int end;

        int index;

        volatile boolean cancelled;

        FusedRangeSubscription(Subscriber<? super Integer> actual, int start, int count) {
            this.actual = actual;
            this.index = start;
            this.end = start + count;
        }

        @Override
        public boolean offer(Integer element) {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public Integer poll() throws Throwable {
            int idx = index;
            if (idx != end) {
                index = idx + 1;
                return idx;
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return index == end;
        }

        @Override
        public void clear() {
            index = end;
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                UndeliverableErrors.onError(new IllegalArgumentException("n > 0L required but it was " + n));
                return;
            }
            if (SubscriptionTools.getAndAddRequested(this, n) == 0L) {
                Subscriber<? super Integer> a = actual;
                int idx = index;
                long e = 0L;
                int f = end;

                for (;;) {

                    while (e != n && idx != f) {
                        if (cancelled) {
                            return;
                        }

                        a.onNext(idx);

                        e++;
                        idx++;
                    }

                    if (idx == f) {
                        if (!cancelled) {
                            a.onComplete();
                        }
                        return;
                    }

                    n = get();
                    if (n == e) {
                        index = idx;
                        n = addAndGet(-e);
                        if (n == 0L) {
                            break;
                        }
                        e = 0L;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            return mode & SYNC;
        }
    }


    static final class FusedRangeConditionalSubscription extends AtomicLong implements FusedQueueSubscription<Integer> {

        private static final long serialVersionUID = -216712975160550513L;

        final ConditionalSubscriber<? super Integer> actual;

        final int end;

        int index;

        volatile boolean cancelled;

        FusedRangeConditionalSubscription(ConditionalSubscriber<? super Integer> actual, int start, int count) {
            this.actual = actual;
            this.index = start;
            this.end = start + count;
        }

        @Override
        public boolean offer(Integer element) {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public Integer poll() throws Throwable {
            int idx = index;
            if (idx != end) {
                index = idx + 1;
                return idx;
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return index == end;
        }

        @Override
        public void clear() {
            index = end;
        }

        @Override
        public void request(long n) {
            if (SubscriptionTools.getAndAddRequested(this, n) == 0L) {
                ConditionalSubscriber<? super Integer> a = actual;
                int idx = index;
                long e = 0L;
                int f = end;

                for (;;) {

                    while (e != n && idx != f) {
                        if (cancelled) {
                            return;
                        }

                        if (a.tryOnNext(idx)) {
                            e++;
                        }
                        idx++;
                    }

                    if (idx == f) {
                        if (!cancelled) {
                            a.onComplete();
                        }
                        return;
                    }

                    n = get();
                    if (n == e) {
                        index = idx;
                        n = addAndGet(-e);
                        if (n == 0L) {
                            break;
                        }
                        e = 0L;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            return mode & SYNC;
        }
    }

}