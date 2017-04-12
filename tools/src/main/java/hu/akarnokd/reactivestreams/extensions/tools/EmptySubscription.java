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

import org.reactivestreams.Subscriber;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;

public enum EmptySubscription implements FusedQueueSubscription<Object> {

    INSTANCE;

    @Override
    public boolean offer(Object element) {
        throw new InternalError("Should not be called!");
    }

    @Override
    public Object poll() throws Throwable {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void clear() {
        // deliberately no-op
    }

    @Override
    public void request(long n) {
        // deliberately ignored
    }

    @Override
    public void cancel() {
        // deliberately no-op
    }

    @Override
    public int requestFusion(int mode) {
        return mode & ASYNC;
    }

    public static <T> void error(Subscriber<? super T> subscriber, Throwable error) {
        subscriber.onSubscribe(INSTANCE);
        subscriber.onError(error);
    }

    public static <T> void complete(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(INSTANCE);
        subscriber.onComplete();
    }

    @Override
    public String toString() {
        return "EmptySubscription";
    }
}
