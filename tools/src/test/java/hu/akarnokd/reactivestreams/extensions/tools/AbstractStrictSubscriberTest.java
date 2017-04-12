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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;

public abstract class AbstractStrictSubscriberTest {

    public abstract RelaxedSubscriber<Object> create(Subscriber<Object> actual, List<Throwable> undeliverables);

    public abstract RelaxedSubscriber<Object> wrap(Subscriber<Object> actual);

    final BasicSubscriber subscriber = new BasicSubscriber();

    final List<Throwable> errors = new ArrayList<Throwable>();

    @Test
    public void simpleFinite() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        sub.onNext(1);
        sub.onNext(2);
        sub.onComplete();

        assertEquals(Long.MAX_VALUE, ls.requested());
        assertEquals(Arrays.<Object>asList(1, 2, "OnComplete"), subscriber.events);

        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void simpleError() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        sub.onNext(1);
        sub.onNext(2);
        sub.onError(new Exception("OnError"));

        assertEquals(Long.MAX_VALUE, ls.requested());
        assertEquals(Arrays.<Object>asList(1, 2, "OnError"), subscriber.events);

        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void zeroRequest() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        subscriber.upstream.request(0L);

        assertEquals(Long.MAX_VALUE, ls.requested());
        assertTrue(subscriber.events.toString(), ((String)subscriber.events.get(0)).contains("3.9"));

        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void negativeRequest() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        subscriber.upstream.request(-1L);

        assertEquals(Long.MAX_VALUE, ls.requested());
        assertTrue(subscriber.events.toString(), ((String)subscriber.events.get(0)).contains("3.9"));

        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void dontWrapRelaxed() {
        assertSame(subscriber, wrap(subscriber));
    }

    @Test
    public void wrapStandard() {
        RelaxedSubscriber<Object> sub = wrap(subscriber.standard());
        assertNotSame(subscriber, sub);
    }

    @Test
    public void noErrorAfterComplete() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        sub.onComplete();

        subscriber.upstream.request(-1L);

        assertEquals(Long.MAX_VALUE, ls.requested());

        assertEquals(Arrays.<Object>asList("OnComplete"), subscriber.events);

        assertTrue(errors.toString(), errors.get(0).getMessage().contains("3.9"));
    }

    @Test
    public void cancel() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        subscriber.upstream.cancel();

        assertTrue(ls.isCancelled());
    }

    @Test(expected = NullPointerException.class)
    public void onSubscribeNull() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        sub.onSubscribe(null);
    }

    @Test
    public void doubleOnSubscribe() {
        RelaxedSubscriber<Object> sub = create(subscriber, errors);

        LongSubscription ls = new LongSubscription();

        sub.onSubscribe(ls);

        LongSubscription ls2 = new LongSubscription();

        sub.onSubscribe(ls2);

        assertTrue(ls.isCancelled());

        assertTrue(ls2.isCancelled());

        assertTrue(subscriber.events.toString(), ((String)subscriber.events.get(0)).contains("Subscription already set!"));
    }

    @Test
    public void onSubscribeRequestRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber consumer = new BasicSubscriber(0L);

            final RelaxedSubscriber<Object> sub = create(consumer, errors);
            final Subscription upstream = (Subscription)sub;

            final LongSubscription ls = new LongSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sub.onSubscribe(ls);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    upstream.request(10);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(10, ls.requested());

            assertTrue(errors.toString(), errors.isEmpty());
        }
    }

    @Test
    public void onSubscribeCancelRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber consumer = new BasicSubscriber();

            final RelaxedSubscriber<Object> sub = create(consumer, errors);
            final Subscription upstream = (Subscription)sub;

            final LongSubscription ls = new LongSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sub.onSubscribe(ls);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    upstream.cancel();
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(ls.isCancelled());

            assertTrue(errors.toString(), errors.isEmpty());
        }
    }

    @Test
    public void onSubscribeTwoCancelRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber consumer = new BasicSubscriber();

            final RelaxedSubscriber<Object> sub = create(consumer, errors);
            final Subscription upstream = (Subscription)sub;

            final LongSubscription ls0 = new LongSubscription();

            sub.onSubscribe(ls0);

            final LongSubscription ls = new LongSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sub.onSubscribe(ls);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    upstream.cancel();
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(ls.isCancelled());
            assertTrue(ls0.isCancelled());

            assertTrue(errors.toString(), errors.isEmpty());
        }
    }
}
