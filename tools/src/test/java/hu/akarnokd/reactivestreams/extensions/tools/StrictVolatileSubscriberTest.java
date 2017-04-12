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

import java.util.List;

import org.reactivestreams.Subscriber;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;

public class StrictVolatileSubscriberTest extends AbstractStrictSubscriberTest {

    @Override
    public RelaxedSubscriber<Object> create(Subscriber<Object> actual, final List<Throwable> undeliverables) {
        return new StrictVolatileSubscriber<Object>(actual) {
            @Override
            protected void undeliverableException(Throwable error) {
                super.undeliverableException(error);
                undeliverables.add(error);
            }
        };
    }

    @Override
    public RelaxedSubscriber<Object> wrap(Subscriber<Object> actual) {
        return StrictVolatileSubscriber.wrap(actual);
    }

}
