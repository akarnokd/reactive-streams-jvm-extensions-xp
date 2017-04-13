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

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import hu.akarnokd.reactivestreams.extensions.examples.UndeliverableErrors.Handler;
import hu.akarnokd.reactivestreams.extensions.tck.FusedConditionalPublisherVerification;

public class FusedRangeTckTest extends FusedConditionalPublisherVerification<Integer> {

    @Override
    public Publisher<Integer> createPublisher(int elements) {
        return new FusedRange(1, elements);
    }

    @Override
    public Integer typicalItem() {
        return 1;
    }

    @Override
    @Test(enabled = false)
    public void setExternalErrorHandler(final ExternalErrorConsumer errorConsumer) {
        if (errorConsumer == null) {
            UndeliverableErrors.handler = null;
        } else {
            UndeliverableErrors.handler = new Handler() {
                @Override
                public void handle(Throwable e) {
                    errorConsumer.accept(e);
                }
            };
        }
    }
}
