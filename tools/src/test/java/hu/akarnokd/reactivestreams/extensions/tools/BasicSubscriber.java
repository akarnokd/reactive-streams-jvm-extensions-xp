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

import java.util.*;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;

final class BasicSubscriber implements ConditionalSubscriber<Object> {

    final List<Object> events = new ArrayList<Object>();

    final long initialRequest;

    Subscription upstream;

    BasicSubscriber() {
        initialRequest = Long.MAX_VALUE;
    }

    BasicSubscriber(long initialRequest) {
        this.initialRequest = initialRequest;
    }

    @Override
    public void onSubscribe(Subscription s) {
        upstream = s;
        if (initialRequest != 0L) {
            s.request(initialRequest);
        }
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

    public Subscriber<Object> standard() {
        return new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription s) {
                BasicSubscriber.this.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                BasicSubscriber.this.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                BasicSubscriber.this.onError(t);
            }

            @Override
            public void onComplete() {
                BasicSubscriber.this.onComplete();
            }
        };
    }
}