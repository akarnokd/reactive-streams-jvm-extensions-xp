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

import static hu.akarnokd.reactivestreams.extensions.tools.SubscriptionTools.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;
import hu.akarnokd.reactivestreams.extensions.tools.SubscriptionTools.SetOnceResult;

public class SubscriptionToolsTest {

    static void utilityClass(Class<?> clazz) throws Exception {
        Constructor<?> c = clazz.getDeclaredConstructor();

        c.setAccessible(true);

        try {
            c.newInstance();
            fail("Should have thrown!");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IllegalStateException);
            assertEquals(ex.toString(), "No instances!", ex.getCause().getMessage());
        }
    }

    @Test
    public void noInstances() throws Exception {
        utilityClass(SubscriptionTools.class);
    }

    @Test
    public void addAndCapNormal() {
        assertEquals(0L, addAndCap(0L, 0L));
        assertEquals(1L, addAndCap(0L, 1L));
        assertEquals(Long.MAX_VALUE, addAndCap(Long.MAX_VALUE - 1, 1L));
        assertEquals(Long.MAX_VALUE, addAndCap(Long.MAX_VALUE - 1, 2L));
        assertEquals(Long.MAX_VALUE, addAndCap(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, addAndCap(Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 + 1));
        assertEquals(Long.MAX_VALUE, addAndCap(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void multiplyAndCapNormal() {
        assertEquals(0L, multiplyCap(0L, 0L));
        assertEquals(0L, multiplyCap(0L, 1L));
        assertEquals(0L, multiplyCap(1L, 0L));
        assertEquals(1L, multiplyCap(1L, 1L));
        assertEquals((long)Integer.MAX_VALUE * Integer.MAX_VALUE, multiplyCap(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, multiplyCap(Long.MAX_VALUE, Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE / 2, multiplyCap(Long.MAX_VALUE / 2, 1));
        assertEquals(Long.MAX_VALUE - 1, multiplyCap(Long.MAX_VALUE / 2, 2));
        assertEquals(Long.MAX_VALUE, multiplyCap(Long.MAX_VALUE / 2, 3));

        assertEquals(Long.MAX_VALUE / 2, multiplyCap(1, Long.MAX_VALUE / 2));
        assertEquals(Long.MAX_VALUE - 1, multiplyCap(2, Long.MAX_VALUE / 2));
        assertEquals(Long.MAX_VALUE, multiplyCap(3, Long.MAX_VALUE / 2));
    }

    @Test
    public void isTerminatedThrowableNormal() {
        assertFalse(isTerminalThrowable((Throwable)null));
        assertFalse(isTerminalThrowable(new Exception()));
        assertTrue(isTerminalThrowable(SubscriptionTools.TERMINATED));
    }

    @Test
    public void isCancelledNormal() {
        assertFalse(isCancelled((Subscription)null));
        assertFalse(isCancelled(EmptySubscription.INSTANCE));
        assertTrue(isCancelled(CancelledSubscription.INSTANCE));
    }

    @Test
    public void setOnceResult() {
        assertEquals(3, SetOnceResult.values().length);
        assertEquals(SetOnceResult.SUCCESS, SetOnceResult.valueOf("SUCCESS"));
        assertEquals(SetOnceResult.ALREADY_SET, SetOnceResult.valueOf("ALREADY_SET"));
        assertEquals(SetOnceResult.CANCELLED, SetOnceResult.valueOf("CANCELLED"));
    }


    // ------------------------------------------------------------
    // Atomic instances
    // ------------------------------------------------------------

    @Test
    public void cancelAtomic() {
        AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        assertTrue(cancel(upstream));

        assertFalse(cancel(upstream));

        BooleanSubscription bs = new BooleanSubscription();
        assertFalse(bs.isCancelled());
        assertEquals("BooleanSubscription(cancelled=false)", bs.toString());

        upstream.lazySet(bs);

        assertTrue(cancel(upstream));

        assertTrue(bs.isCancelled());
        assertEquals("BooleanSubscription(cancelled=true)", bs.toString());
    }

    @Test
    public void cancelAtomicRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cancel(upstream);
                }
            };

            TestSupport.race(r, r);
        }
    }

    @Test
    public void isCancelledAtomic() {
        AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        assertFalse(isCancelled(upstream));

        BooleanSubscription bs = new BooleanSubscription();
        bs.request(-1);
        bs.request(0);
        bs.request(1);
        assertFalse(bs.isCancelled());

        upstream.lazySet(bs);

        assertFalse(isCancelled(upstream));

        assertTrue(cancel(upstream));

        assertTrue(isCancelled(upstream));

        upstream.lazySet(null);

        assertFalse(isCancelled(upstream));

        clear(upstream);

        assertTrue(isCancelled(upstream));
    }

    @Test
    public void replaceAtomic() {
        AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isCancelled());

        assertTrue(replace(upstream, bs));

        assertFalse(bs.isCancelled());

        assertTrue(replace(upstream, null));

        assertFalse(bs.isCancelled());

        clear(upstream);

        assertFalse(replace(upstream, bs));

        assertTrue(bs.isCancelled());

        assertFalse(replace(upstream, null));

        assertTrue(isCancelled(upstream));
    }

    @Test
    public void replaceRaceAtomic() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    replace(upstream, null);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    replace(upstream, null);
                }
            };

            TestSupport.race(r1, r2);
        }
    }

    @Test
    public void replaceFieldCancelRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    replace(upstream, bs);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cancel(upstream);
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(bs.isCancelled());
        }
    }

    @Test
    public void updateAtomic() {
        AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isCancelled());

        update(upstream, bs);

        assertFalse(bs.isCancelled());

        update(upstream, null);

        assertTrue(bs.isCancelled());

        bs = new BooleanSubscription();

        clear(upstream);

        update(upstream, bs);

        assertTrue(bs.isCancelled());

        assertFalse(update(upstream, null));

        assertTrue(isCancelled(upstream));
    }

    @Test
    public void updateFieldRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    update(upstream, null);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    update(upstream, null);
                }
            };

            TestSupport.race(r1, r2);
        }
    }

    @Test
    public void updateCancelRaceAtomic() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    update(upstream, bs);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cancel(upstream);
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(bs.isCancelled());
        }
    }

    @Test(expected = NullPointerException.class)
    public void setOnceNullAtomic() {
        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        setOnce(upstream, null);
    }

    @Test
    public void setOnceAtomic() {
        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        BooleanSubscription bs = new BooleanSubscription();

        assertEquals(SetOnceResult.SUCCESS, setOnce(upstream, bs));

        BooleanSubscription bs1 = new BooleanSubscription();

        assertEquals(SetOnceResult.ALREADY_SET, setOnce(upstream, bs1));

        assertFalse(bs.isCancelled());
        assertTrue(bs1.isCancelled());

        clear(upstream);

        assertEquals(SetOnceResult.CANCELLED, setOnce(upstream, bs));

        assertTrue(bs.isCancelled());
    }

    @Test(expected = NullPointerException.class)
    public void deferredSetOnceNullAtomic() {
        AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        AtomicLong requested = new AtomicLong();

        deferredSetOnce(upstream, requested, null);
    }

    @Test
    public void deferredSetOnceAtomic() {
        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        AtomicLong requested = new AtomicLong();

        BooleanSubscription bs = new BooleanSubscription();

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(upstream, requested, bs));

        BooleanSubscription bs1 = new BooleanSubscription();

        assertEquals(SetOnceResult.ALREADY_SET, deferredSetOnce(upstream, requested, bs1));

        assertFalse(bs.isCancelled());
        assertTrue(bs1.isCancelled());

        clear(upstream);

        assertEquals(SetOnceResult.CANCELLED, deferredSetOnce(upstream, requested, bs));

        assertTrue(bs.isCancelled());
    }

    @Test
    public void deferredSetOnceRequestAtomic() {
        LongSubscription ls = new LongSubscription();

        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        AtomicLong requested = new AtomicLong();

        requested.lazySet(10);

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(upstream, requested, ls));

        assertEquals(10, ls.requested());

        assertEquals(0, requested.get());
    }

    @Test
    public void deferredRequestAtomic() {
        final LongSubscription ls = new LongSubscription();

        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        final AtomicLong requested = new AtomicLong();

        deferredRequest(upstream, requested, 10);

        assertEquals(0L, ls.requested());

        assertEquals(10L, requested.get());

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(upstream, requested, ls));

        assertEquals(10, ls.requested());

        assertEquals(0, requested.get());

        deferredRequest(upstream, requested, 20);

        assertEquals(0, requested.get());

        assertEquals(30, ls.requested());
    }

    @Test
    public void deferredSetOnceRequestRaceAtomic() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final LongSubscription ls = new LongSubscription();

            final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

            final AtomicLong requested = new AtomicLong();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    deferredSetOnce(upstream, requested, ls);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    deferredRequest(upstream, requested, 10);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(10, ls.requested());
        }
    }

    @Test
    public void isTerminatedThrowableAtomic() {
        AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        assertFalse(isTerminalThrowable(error));
        error.lazySet(new Exception());
        assertFalse(isTerminalThrowable(error));
        error.lazySet(SubscriptionTools.TERMINATED);
        assertTrue(isTerminalThrowable(error));
    }

    @Test
    public void getAndAddRequestedAtomic() {
        final AtomicLong requested = new AtomicLong();

        assertEquals(0, getAndAddRequested(requested, 10));

        assertEquals(10, getAndAddRequested(requested, 20));

        assertEquals(30, getAndAddRequested(requested, Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE, getAndAddRequested(requested, 30));

        assertEquals(Long.MAX_VALUE, getAndAddRequested(requested, Long.MAX_VALUE));
    }

    @Test
    public void getAndAddRequestAtomicRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final AtomicLong requested = new AtomicLong();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    getAndAddRequested(requested, 10);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    getAndAddRequested(requested, 20);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(30, requested.get());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void subtractAndGetRequestedInvalid() {

        final AtomicLong requested = new AtomicLong();

        subtractAndGetRequested(requested, 10);
    }

    @Test
    public void subtractAndGetRequestedAtomic() {
        final AtomicLong requested = new AtomicLong(20);

        assertEquals(10, subtractAndGetRequested(requested, 10));

        assertEquals(10, requested.get());

        assertEquals(0, subtractAndGetRequested(requested, 10));

        assertEquals(0, requested.get());
    }

    @Test
    public void subtractAndGetRequestedFieldUnbounded() {
        final AtomicLong requested = new AtomicLong(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, subtractAndGetRequested(requested, 10));

        assertEquals(Long.MAX_VALUE, requested.get());

        assertEquals(Long.MAX_VALUE, subtractAndGetRequested(requested, Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE, requested.get());
    }

    @Test
    public void subtractAndGetFieldRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final AtomicLong requested = new AtomicLong(30);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    subtractAndGetRequested(requested, 10);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    subtractAndGetRequested(requested, 20);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(0, requested.get());
        }
    }

    static final class BasicSubscriber implements ConditionalSubscriber<Object> {

        final List<Object> events = new ArrayList<Object>();

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object t) {
            events.add(t);
        }

        @Override
        public boolean tryOnNext(Object t) {
            events.add(t);
            return true;
        }

        @Override
        public void onError(Throwable t) {
            events.add(t.getMessage());
        }

        @Override
        public void onComplete() {
            events.add("OnComplete");
        }

    }

    @Test
    public void serializedFieldComplete() {
        BasicSubscriber sub = new BasicSubscriber();

        AtomicLong wip = new AtomicLong();

        AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        assertTrue(serializedOnNext(sub, wip, error, 1));

        assertTrue(serializedTryOnNext(sub, wip, error, 2));

        assertTrue(serializedOnComplete(sub, wip, error));

        assertFalse(serializedOnError(sub, wip, error, new IOException()));

        assertFalse(serializedOnNext(sub, wip, error, 3));

        assertFalse(serializedTryOnNext(sub, wip, error, 4));

        assertFalse(serializedOnComplete(sub, wip, error));

        assertEquals(Arrays.<Object>asList(1, 2, "OnComplete"), sub.events);
    }

    @Test
    public void serializedFieldError() {
        BasicSubscriber sub = new BasicSubscriber();

        AtomicLong wip = new AtomicLong();

        AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        assertTrue(serializedOnNext(sub, wip, error, 1));

        assertTrue(serializedTryOnNext(sub, wip, error, 2));

        assertTrue(serializedOnError(sub, wip, error, new IOException("OnError")));

        assertFalse(serializedOnComplete(sub, wip, error));

        assertFalse(serializedOnNext(sub, wip, error, 3));

        assertFalse(serializedTryOnNext(sub, wip, error, 4));

        assertFalse(serializedOnError(sub, wip, error, new SocketException()));

        assertEquals(Arrays.<Object>asList(1, 2, "OnError"), sub.events);
    }

    @Test
    public void serializedOnNextOnCompleteRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final AtomicLong wip = new AtomicLong();

            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnNext(sub, wip, error, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, wip, error);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnComplete"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnComplete"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }


    @Test
    public void serializedTryOnNextOnCompleteRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final AtomicLong wip = new AtomicLong();

            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedTryOnNext(sub, wip, error, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, wip, error);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnComplete"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnComplete"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedOnNextOnErrorRace() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final AtomicLong wip = new AtomicLong();

            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnNext(sub, wip, error, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, wip, error, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnError"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnError"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedTryOnNextOnErrorRace() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final AtomicLong wip = new AtomicLong();

            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedTryOnNext(sub, wip, error, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, wip, error, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnError"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnError"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedOnErrorOnCompleteRace() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final AtomicLong wip = new AtomicLong();

            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, wip, error, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 1) {
                assertTrue(sub.events.toString(), "OnError".equals(sub.events.get(0)) || "OnComplete".equals(sub.events.get(0)));
            } else {
                fail(sub.events.toString());
            }
        }
    }

    // ------------------------------------------------------------
    // field updaters
    // ------------------------------------------------------------

    static final class Fields {
        volatile long wip;

        static final AtomicLongFieldUpdater<Fields> WIP =
                AtomicLongFieldUpdater.newUpdater(Fields.class, "wip");

        volatile long requested;

        static final AtomicLongFieldUpdater<Fields> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(Fields.class, "requested");

        volatile Subscription upstream;

        static final AtomicReferenceFieldUpdater<Fields, Subscription> UPSTREAM =
                AtomicReferenceFieldUpdater.newUpdater(Fields.class, Subscription.class, "upstream");

        volatile Throwable error;

        static final AtomicReferenceFieldUpdater<Fields, Throwable> ERROR =
                AtomicReferenceFieldUpdater.newUpdater(Fields.class, Throwable.class, "error");
    }

    // ------------------------------------------------------------
    // field updaters
    // ------------------------------------------------------------

    @Test
    public void cancelField() {
        final Fields fields = new Fields();

        assertTrue(cancel(fields, Fields.UPSTREAM));

        assertFalse(cancel(fields, Fields.UPSTREAM));

        BooleanSubscription bs = new BooleanSubscription();
        assertFalse(bs.isCancelled());
        assertEquals("BooleanSubscription(cancelled=false)", bs.toString());

        fields.upstream = bs;

        assertTrue(cancel(fields, Fields.UPSTREAM));

        assertTrue(bs.isCancelled());
        assertEquals("BooleanSubscription(cancelled=true)", bs.toString());
    }

    @Test
    public void cancelRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cancel(fields, Fields.UPSTREAM);
                }
            };

            TestSupport.race(r, r);
        }
    }

    @Test
    public void isCancelledField() {
        final Fields fields = new Fields();

        assertFalse(isCancelled(fields, Fields.UPSTREAM));

        BooleanSubscription bs = new BooleanSubscription();
        bs.request(-1);
        bs.request(0);
        bs.request(1);
        assertFalse(bs.isCancelled());

        fields.upstream = bs;

        assertFalse(isCancelled(fields, Fields.UPSTREAM));

        assertTrue(cancel(fields, Fields.UPSTREAM));

        assertTrue(isCancelled(fields, Fields.UPSTREAM));

        fields.upstream = null;

        assertFalse(isCancelled(fields, Fields.UPSTREAM));

        clear(fields, Fields.UPSTREAM);

        assertTrue(isCancelled(fields, Fields.UPSTREAM));
    }

    @Test
    public void replaceField() {
        final Fields fields = new Fields();

        BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isCancelled());

        assertTrue(replace(fields, Fields.UPSTREAM, bs));

        assertFalse(bs.isCancelled());

        assertTrue(replace(fields, Fields.UPSTREAM, null));

        assertFalse(bs.isCancelled());

        clear(fields, Fields.UPSTREAM);

        assertFalse(replace(fields, Fields.UPSTREAM, bs));

        assertTrue(bs.isCancelled());

        assertFalse(replace(fields, Fields.UPSTREAM, null));

        assertTrue(isCancelled(fields, Fields.UPSTREAM));
    }

    @Test
    public void replaceRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    replace(fields, Fields.UPSTREAM, null);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    replace(fields, Fields.UPSTREAM, null);
                }
            };

            TestSupport.race(r1, r2);
        }
    }

    @Test
    public void replaceCancelRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    replace(fields, Fields.UPSTREAM, bs);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cancel(fields, Fields.UPSTREAM);
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(bs.isCancelled());
        }
    }

    @Test
    public void updateField() {
        final Fields fields = new Fields();

        BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isCancelled());

        update(fields, Fields.UPSTREAM, bs);

        assertFalse(bs.isCancelled());

        update(fields, Fields.UPSTREAM, null);

        assertTrue(bs.isCancelled());

        bs = new BooleanSubscription();

        clear(fields, Fields.UPSTREAM);

        update(fields, Fields.UPSTREAM, bs);

        assertTrue(bs.isCancelled());

        assertFalse(update(fields, Fields.UPSTREAM, null));

        assertTrue(isCancelled(fields, Fields.UPSTREAM));
    }

    @Test
    public void updateRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    update(fields, Fields.UPSTREAM, null);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    update(fields, Fields.UPSTREAM, null);
                }
            };

            TestSupport.race(r1, r2);
        }
    }

    @Test
    public void updateFieldCancelRace() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    update(fields, Fields.UPSTREAM, bs);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cancel(fields, Fields.UPSTREAM);
                }
            };

            TestSupport.race(r1, r2);

            assertTrue(bs.isCancelled());
        }
    }

    @Test(expected = NullPointerException.class)
    public void setOnceFieldNull() {
        final Fields fields = new Fields();

        setOnce(fields, Fields.UPSTREAM, null);
    }

    @Test
    public void setOnceField() {
        final Fields fields = new Fields();

        BooleanSubscription bs = new BooleanSubscription();

        assertEquals(SetOnceResult.SUCCESS, setOnce(fields, Fields.UPSTREAM, bs));

        BooleanSubscription bs1 = new BooleanSubscription();

        assertEquals(SetOnceResult.ALREADY_SET, setOnce(fields, Fields.UPSTREAM, bs1));

        assertFalse(bs.isCancelled());
        assertTrue(bs1.isCancelled());

        clear(fields, Fields.UPSTREAM);

        assertEquals(SetOnceResult.CANCELLED, setOnce(fields, Fields.UPSTREAM, bs));

        assertTrue(bs.isCancelled());
    }

    @Test(expected = NullPointerException.class)
    public void deferredSetOnceFieldNull() {
        final Fields fields = new Fields();

        deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, null);
    }

    @Test
    public void deferredSetOnceField() {
        final Fields fields = new Fields();

        BooleanSubscription bs = new BooleanSubscription();

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, bs));

        BooleanSubscription bs1 = new BooleanSubscription();

        assertEquals(SetOnceResult.ALREADY_SET, deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, bs1));

        assertFalse(bs.isCancelled());
        assertTrue(bs1.isCancelled());

        clear(fields, Fields.UPSTREAM);

        assertEquals(SetOnceResult.CANCELLED, deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, bs));

        assertTrue(bs.isCancelled());
    }

    @Test
    public void deferredSetOnceFieldRequest() {
        LongSubscription ls = new LongSubscription();

        final Fields fields = new Fields();

        fields.requested = 10;

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, ls));

        assertEquals(10, ls.requested());

        assertEquals(0, fields.requested);
    }

    @Test
    public void deferredRequestField() {
        final LongSubscription ls = new LongSubscription();

        final Fields fields = new Fields();

        deferredRequest(fields, Fields.UPSTREAM, Fields.REQUESTED, 10);

        assertEquals(0L, ls.requested());

        assertEquals(10L, fields.requested);

        assertEquals(SetOnceResult.SUCCESS, deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, ls));

        assertEquals(10, ls.requested());

        assertEquals(0, fields.requested);

        deferredRequest(fields, Fields.UPSTREAM, Fields.REQUESTED, 20);

        assertEquals(0, fields.requested);

        assertEquals(30, ls.requested());
    }

    @Test
    public void deferredSetOnceRequestRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final LongSubscription ls = new LongSubscription();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    deferredSetOnce(fields, Fields.UPSTREAM, Fields.REQUESTED, ls);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    deferredRequest(fields, Fields.UPSTREAM, Fields.REQUESTED, 10);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(10, ls.requested());
        }
    }

    @Test
    public void isTerminatedThrowableField() {
        final Fields fields = new Fields();

        assertFalse(isTerminalThrowable(fields, Fields.ERROR));
        fields.error = new Exception();
        assertFalse(isTerminalThrowable(fields, Fields.ERROR));
        fields.error = SubscriptionTools.TERMINATED;
        assertTrue(isTerminalThrowable(fields, Fields.ERROR));
    }

    @Test
    public void getAndAddRequestedField() {
        final Fields fields = new Fields();

        assertEquals(0, getAndAddRequested(fields, Fields.REQUESTED, 10));

        assertEquals(10, getAndAddRequested(fields, Fields.REQUESTED, 20));

        assertEquals(30, getAndAddRequested(fields, Fields.REQUESTED, Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE, getAndAddRequested(fields, Fields.REQUESTED, 30));

        assertEquals(Long.MAX_VALUE, getAndAddRequested(fields, Fields.REQUESTED, Long.MAX_VALUE));
    }

    @Test
    public void getAndAddRequestRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    getAndAddRequested(fields, Fields.REQUESTED, 10);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    getAndAddRequested(fields, Fields.REQUESTED, 20);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(30, fields.requested);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void subtractAndGetRequestedFieldInvalid() {
        final Fields fields = new Fields();

        subtractAndGetRequested(fields, Fields.REQUESTED, 10);
    }

    @Test
    public void subtractAndGetRequestedField() {
        final Fields fields = new Fields();
        fields.requested = 20;

        assertEquals(10, subtractAndGetRequested(fields, Fields.REQUESTED, 10));

        assertEquals(10, fields.requested);

        assertEquals(0, subtractAndGetRequested(fields, Fields.REQUESTED, 10));

        assertEquals(0, fields.requested);
    }

    @Test
    public void subtractAndGetRequestedFieldUnboundedField() {
        final Fields fields = new Fields();
        fields.requested = Long.MAX_VALUE;

        assertEquals(Long.MAX_VALUE, subtractAndGetRequested(fields, Fields.REQUESTED, 10));

        assertEquals(Long.MAX_VALUE, fields.requested);

        assertEquals(Long.MAX_VALUE, subtractAndGetRequested(fields, Fields.REQUESTED, Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE, fields.requested);
    }

    @Test
    public void subtractAndGetFieldRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {

            final Fields fields = new Fields();
            fields.requested = 30;

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    subtractAndGetRequested(fields, Fields.REQUESTED, 10);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    subtractAndGetRequested(fields, Fields.REQUESTED, 20);
                }
            };

            TestSupport.race(r1, r2);

            assertEquals(0, fields.requested);
        }
    }

    @Test
    public void serializedFieldCompleteField() {
        BasicSubscriber sub = new BasicSubscriber();

        final Fields fields = new Fields();

        assertTrue(serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1));

        assertTrue(serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 2));

        assertTrue(serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR));

        assertFalse(serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, new IOException()));

        assertFalse(serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 3));

        assertFalse(serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 4));

        assertFalse(serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR));

        assertEquals(Arrays.<Object>asList(1, 2, "OnComplete"), sub.events);
    }

    @Test
    public void serializedFieldErrorField() {
        BasicSubscriber sub = new BasicSubscriber();

        final Fields fields = new Fields();

        assertTrue(serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1));

        assertTrue(serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 2));

        assertTrue(serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, new IOException("OnError")));

        assertFalse(serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR));

        assertFalse(serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 3));

        assertFalse(serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 4));

        assertFalse(serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, new SocketException()));

        assertEquals(Arrays.<Object>asList(1, 2, "OnError"), sub.events);
    }

    @Test
    public void serializedOnNextOnCompleteRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnComplete"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnComplete"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }


    @Test
    public void serializedTryOnNextOnCompleteRaceField() {
        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnComplete"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnComplete"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedOnNextOnErrorRaceField() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnError"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnError"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedTryOnNextOnErrorRaceField() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedTryOnNext(sub, fields, Fields.WIP, Fields.ERROR, 1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 2) {
                assertEquals(Arrays.<Object>asList(1, "OnError"), sub.events);
            } else
            if (sub.events.size() == 1) {
                assertEquals(Arrays.<Object>asList("OnError"), sub.events);
            } else {
                fail(sub.events.toString());
            }
        }
    }

    @Test
    public void serializedOnErrorOnCompleteRaceField() {
        final Throwable ex = new IOException("OnError");

        for (int i = 0; i < TestSupport.LOOP; i++) {
            final BasicSubscriber sub = new BasicSubscriber();

            final Fields fields = new Fields();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    serializedOnComplete(sub, fields, Fields.WIP, Fields.ERROR);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    serializedOnError(sub, fields, Fields.WIP, Fields.ERROR, ex);
                }
            };

            TestSupport.race(r1, r2);

            if (sub.events.size() == 1) {
                assertTrue(sub.events.toString(), "OnError".equals(sub.events.get(0)) || "OnComplete".equals(sub.events.get(0)));
            } else {
                fail(sub.events.toString());
            }
        }
    }

}