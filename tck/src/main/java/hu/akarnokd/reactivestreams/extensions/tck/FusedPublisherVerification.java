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

package hu.akarnokd.reactivestreams.extensions.tck;

import org.reactivestreams.Publisher;
import org.testng.SkipException;
import org.testng.annotations.Test;

import hu.akarnokd.reactivestreams.extensions.*;

public abstract class FusedPublisherVerification<T> extends RelaxedPublisherVerification<T> {

    /**
     * Implement this to return a typical, non-null element of the
     * generic type T.
     * @return the typical item, not null
     */
    public abstract T typicalItem();

    @Test
    public void requiredFusedPublisherWorks() {
        runPublisher(true, new TestBody<T>() {
            @Override
            public void run(Publisher<T> pub, int elements, boolean exact, boolean errorResult) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();
                try {
                    sub.request(Long.MAX_VALUE);
                    sub.setInitialFusionMode(FusedQueueSubscription.ANY);

                    pub.subscribe(sub);

                    sub.expectFusedSubscribe();

                    sub.expectFusionMode(FusedQueueSubscription.ANY);

                    if (exact) {
                        sub.expectElements(elements);
                    } else {
                        sub.expectAnyElements(elements);
                    }

                    if (errorResult) {
                        sub.expectError();
                        sub.expectNoComplete();
                    } else {
                        sub.expectComplete();
                        sub.expectNoErrors();
                    }
                } catch (Throwable ex) {
                    sub.cancel();
                    throw ex;
                }
            }
        }, 0, 1, 2, 3, 5, 10, 20);
    }

    @Test
    public void requiredOfferShouldThrowOrReturnFalse() {
        final T typicalItem = typicalItem();

        if (typicalItem == null) {
            throw new NullPointerException("typicalItem() returned null");
        }

        runPublisher(true, new TestBody<T>() {
            @Override
            public void run(Publisher<T> publisher, int elements, boolean exact, boolean errorResult) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();
                sub.setInitialFusionMode(FusedQueueSubscription.ANY);

                publisher.subscribe(sub);

                try {
                    sub.expectFusedSubscribe();

                    FusedQueue<T> queue = sub.fusedQueue();

                    boolean b;

                    try {
                        b = queue.offer(typicalItem);
                    } catch (IllegalArgumentException ex) {
                        b = false;
                    } catch (IllegalStateException ex) {
                        b = false;
                    } catch (UnsupportedOperationException ex) {
                        b = false;
                    } catch (Throwable ex) {
                        throw new SkipException("Maybe okay: failed with an unexpected exception", ex);
                    }
                    if (b) {
                        throw new AssertionError("offer() expected to throw or return false");
                    }
                } finally {
                    sub.cancel();
                }
            }
        }, 0, 1, 2, 3, 5, 10, 20);
    }
}
