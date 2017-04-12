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

import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Subscription;

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
    public void isCancelledField() {
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
    public void replaceField() {
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
    public void replaceFieldRace() {
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
    public void updateField() {
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
    public void updateFieldCancelRace() {
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
    public void setOnceFieldNull() {
        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        setOnce(upstream, null);
    }

    @Test
    public void setOnceField() {
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
}