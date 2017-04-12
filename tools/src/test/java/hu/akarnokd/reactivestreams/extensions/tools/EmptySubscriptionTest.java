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

    @Test(expected = UnsupportedOperationException.class)
    public void offer() {
        EmptySubscription.INSTANCE.offer(new Object());
    }

    @Test
    public void empty() {
        
    }
}
