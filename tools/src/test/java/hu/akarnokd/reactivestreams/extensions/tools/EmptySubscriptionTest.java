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

import java.io.IOException;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;

public class EmptySubscriptionTest {

    @Test
    public void valueOf() {
        Assert.assertEquals(EmptySubscription.INSTANCE, EmptySubscription.valueOf("INSTANCE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfError() {
        Assert.assertEquals(EmptySubscription.INSTANCE, EmptySubscription.valueOf("A"));
    }

    @Test
    public void values() {
        Assert.assertArrayEquals(new EmptySubscription[] { EmptySubscription.INSTANCE }, EmptySubscription.values());
    }

    @Test(expected = InternalError.class)
    public void offer() {
        EmptySubscription.INSTANCE.offer(new Object());
    }

    @Test
    public void empty() throws Throwable {
        Assert.assertNull(EmptySubscription.INSTANCE.poll());
        Assert.assertTrue(EmptySubscription.INSTANCE.isEmpty());

        Assert.assertEquals(FusedQueueSubscription.ASYNC,
                EmptySubscription.INSTANCE.requestFusion(FusedQueueSubscription.ANY));

        Assert.assertEquals(FusedQueueSubscription.NONE,
                EmptySubscription.INSTANCE.requestFusion(FusedQueueSubscription.SYNC));

        EmptySubscription.INSTANCE.clear();

        EmptySubscription.INSTANCE.request(-1);

        EmptySubscription.INSTANCE.request(0);

        EmptySubscription.INSTANCE.request(1);

        EmptySubscription.INSTANCE.cancel();

        Assert.assertEquals("EmptySubscription", EmptySubscription.INSTANCE.toString());
    }

    @Test
    public void error() {
        final Subscription[] sub = { null };
        final Throwable[] error = { null };
        final int[] onNext = { 0 };
        final int[] onComplete = { 0 };

        EmptySubscription.error(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription s) {
                sub[0] = s;
            }

            @Override
            public void onNext(Object t) {
                onNext[0]++;
            }

            @Override
            public void onError(Throwable t) {
                error[0] = t;
            }

            @Override
            public void onComplete() {
                onComplete[0]++;
            }
        }, new IOException());

        Assert.assertEquals(EmptySubscription.INSTANCE, sub[0]);
        Assert.assertEquals(0, onNext[0]);
        Assert.assertEquals(0, onComplete[0]);
        Assert.assertTrue("" + error[0], error[0] instanceof IOException);
    }

    @Test
    public void complete() {
        final Subscription[] sub = { null };
        final Throwable[] error = { null };
        final int[] onNext = { 0 };
        final int[] onComplete = { 0 };

        EmptySubscription.complete(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription s) {
                sub[0] = s;
            }

            @Override
            public void onNext(Object t) {
                onNext[0]++;
            }

            @Override
            public void onError(Throwable t) {
                error[0] = t;
            }

            @Override
            public void onComplete() {
                onComplete[0]++;
            }
        });

        Assert.assertEquals(EmptySubscription.INSTANCE, sub[0]);
        Assert.assertEquals(0, onNext[0]);
        Assert.assertEquals(1, onComplete[0]);
        Assert.assertNull(error[0]);
    }
}
