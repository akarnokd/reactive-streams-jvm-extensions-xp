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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.*;
import hu.akarnokd.reactivestreams.extensions.tools.*;

public final class FusedRangePublisher implements Publisher<Integer> {

    final int start;

    final int count;

    final Throwable error;

    final CancellationTracker tracker;

    public FusedRangePublisher(int start, int count, Throwable error, CancellationTracker tracker) {
        this.start = start;
        this.count = count;
        this.tracker = tracker;
        this.error = error;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> s) {
        long trackerId = tracker.add();
        IsFused sub;
        if (s instanceof ConditionalSubscriber) {
            sub = new FusedRangeConditionalSubscription((ConditionalSubscriber<? super Integer>)s, start, count, error, trackerId, tracker);
        } else {
            s = StrictAtomicSubscriber.wrap(s);
            sub = new FusedRangeSubscription(s, start, count, error, trackerId, tracker);
        }
        s.onSubscribe(sub);
        if (count == 0 && !sub.isFused()) {
            sub.request(1L);
        }
    }

    interface IsFused extends FusedQueueSubscription<Integer> {
        boolean isFused();
    }

    static final class FusedRangeSubscription extends AtomicLong implements IsFused {

        private static final long serialVersionUID = -216712975160550513L;

        final Subscriber<? super Integer> actual;

        final int end;

        final long trackerId;

        final CancellationTracker tracker;

        final Throwable error;

        int index;

        volatile boolean cancelled;

        boolean fused;

        FusedRangeSubscription(Subscriber<? super Integer> actual, int start, int count, Throwable error, long trackerId, CancellationTracker tracker) {
            this.actual = actual;
            this.index = start;
            this.end = start + count;
            this.trackerId = trackerId;
            this.tracker = tracker;
            this.error = error;
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
            if (error != null) {
                throw error;
            }
            tracker.remove(trackerId);
            return null;
        }

        @Override
        public boolean isEmpty() {
            return index == end;
        }

        @Override
        public void clear() {
            index = end;
            tracker.remove(trackerId);
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                TckUndeliverableErrors.onError(new IllegalArgumentException("n > 0L required but it was " + n));
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
                            if (error == null) {
                                a.onComplete();
                            } else {
                                a.onError(error);
                            }
                            tracker.remove(trackerId);
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
            tracker.remove(trackerId);
        }

        @Override
        public int requestFusion(int mode) {
            int m = mode & SYNC;
            fused = m != 0;
            return m;
        }

        @Override
        public boolean isFused() {
            return fused;
        }
    }


    static final class FusedRangeConditionalSubscription extends AtomicLong implements IsFused {

        private static final long serialVersionUID = -216712975160550513L;

        final ConditionalSubscriber<? super Integer> actual;

        final int end;

        int index;

        volatile boolean cancelled;

        final long trackerId;

        final CancellationTracker tracker;

        final Throwable error;

        boolean fused;

        FusedRangeConditionalSubscription(ConditionalSubscriber<? super Integer> actual, int start, int count, Throwable error, long trackerId, CancellationTracker tracker) {
            this.actual = actual;
            this.index = start;
            this.end = start + count;
            this.trackerId = trackerId;
            this.tracker = tracker;
            this.error = error;
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
            if (error != null) {
                throw error;
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
            tracker.remove(trackerId);
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
                            if (error == null) {
                                a.onComplete();
                            } else {
                                a.onError(error);
                            }
                            tracker.remove(trackerId);
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
            tracker.remove(trackerId);
        }

        @Override
        public int requestFusion(int mode) {
            int m = mode & SYNC;
            fused = m != 0;
            return m;
        }

        @Override
        public boolean isFused() {
            return fused;
        }
    }

}