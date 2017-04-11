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

import org.testng.annotations.Test;

import hu.akarnokd.reactivestreams.extensions.ConstantValuePublisher;

public abstract class ConstantValuePublisherVerification<T> {

    public abstract ConstantValuePublisher<T> createPublisher();

    public int expectedNumberOfValues() {
        return -1;
    }

    @Test
    public void validateExpectedNumberOfValues() {
        VerificationHelper.validateExpectedNumberOfValues(expectedNumberOfValues());
    }

    @Test
    public void requiredPublisherValueWorks() {
        VerificationHelper.requiredPublisherValueWorks(createPublisher(), expectedNumberOfValues());
    }
}