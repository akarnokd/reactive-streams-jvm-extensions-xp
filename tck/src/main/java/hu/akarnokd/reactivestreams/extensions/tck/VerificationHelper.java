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

import org.testng.Assert;

import hu.akarnokd.reactivestreams.extensions.DynamicValuePublisher;

final class VerificationHelper {

    private VerificationHelper() {
        throw new IllegalStateException("No instances!");
    }

    static String valueAndClass(Object value) {
        return value + (value != null ? " (" + value.getClass() + ")" : "");
    }

    static void validateExpectedNumberOfValues(int expected) {
        if (expected < -1 || expected > 1) {
            Assert.fail("The expectedNumberOfValues() should return a number between -1 and 1");
        }
    }

    static <T> void requiredPublisherValueWorks(DynamicValuePublisher<T> publisher, int expectedNumberOfValues) {
        if (publisher == null) {
            Assert.fail("Required test: createPublisher returned null");
        }

        T value = null;
        try {
            value = publisher.value();
        } catch (Throwable ex) {
            Assert.fail("value() not expected to throw", ex);
        }

        if (expectedNumberOfValues == 0 && value != null) {
            Assert.fail("value() should have returned null, indicating emptiness but instead returned a non-null object: " + valueAndClass(value));
        }
        if (expectedNumberOfValues == 1 && value == null) {
            Assert.fail("value() should have returned non-null but instead returned null indicating an empty Publisher");
        }
    }
}
