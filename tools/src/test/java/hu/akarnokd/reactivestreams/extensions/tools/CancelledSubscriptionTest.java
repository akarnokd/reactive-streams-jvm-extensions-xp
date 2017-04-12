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

import org.junit.*;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;

public class CancelledSubscriptionTest {

    @Test
    public void valueOf() {
        Assert.assertEquals(CancelledSubscription.INSTANCE, CancelledSubscription.valueOf("INSTANCE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfError() {
        Assert.assertEquals(CancelledSubscription.INSTANCE, CancelledSubscription.valueOf("A"));
    }

    @Test
    public void values() {
        Assert.assertArrayEquals(new CancelledSubscription[] { CancelledSubscription.INSTANCE }, CancelledSubscription.values());
    }

    @Test(expected = InternalError.class)
    public void offer() {
        CancelledSubscription.INSTANCE.offer(new Object());
    }

    @Test
    public void empty() throws Throwable {
        Assert.assertNull(CancelledSubscription.INSTANCE.poll());
        Assert.assertTrue(CancelledSubscription.INSTANCE.isEmpty());

        Assert.assertEquals(FusedQueueSubscription.NONE,
                CancelledSubscription.INSTANCE.requestFusion(FusedQueueSubscription.ANY));

        Assert.assertEquals(FusedQueueSubscription.NONE,
                CancelledSubscription.INSTANCE.requestFusion(FusedQueueSubscription.SYNC));

        Assert.assertEquals(FusedQueueSubscription.NONE,
                CancelledSubscription.INSTANCE.requestFusion(FusedQueueSubscription.ASYNC));

        CancelledSubscription.INSTANCE.clear();

        CancelledSubscription.INSTANCE.request(-1);

        CancelledSubscription.INSTANCE.request(0);

        CancelledSubscription.INSTANCE.request(1);

        CancelledSubscription.INSTANCE.cancel();

        Assert.assertEquals("CancelledSubscription", CancelledSubscription.INSTANCE.toString());
    }
}
