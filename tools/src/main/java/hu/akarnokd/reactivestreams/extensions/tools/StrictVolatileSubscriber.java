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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;

public class StrictVolatileSubscriber<T> implements RelaxedSubscriber<T>, Subscription {

    protected final Subscriber<? super T> actual;
    
    protected volatile Subscription upstream;
    @SuppressWarnings("rawtypes")
    protected static final AtomicReferenceFieldUpdater<StrictVolatileSubscriber, Subscription> UPSTREAM =
            AtomicReferenceFieldUpdater.newUpdater(StrictVolatileSubscriber.class, Subscription.class, "upstream");
    
    protected volatile long requested;
    @SuppressWarnings("rawtypes")
    protected static final AtomicLongFieldUpdater<StrictVolatileSubscriber> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(StrictVolatileSubscriber.class, "requested");
    
    protected volatile long wip;
    @SuppressWarnings("rawtypes")
    protected static final AtomicLongFieldUpdater<StrictVolatileSubscriber> WIP =
            AtomicLongFieldUpdater.newUpdater(StrictVolatileSubscriber.class, "wip");
    
    protected volatile Throwable error;
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<StrictVolatileSubscriber, Throwable> ERROR =
            AtomicReferenceFieldUpdater.newUpdater(StrictVolatileSubscriber.class, Throwable.class, "error");

    public StrictVolatileSubscriber(Subscriber<? super T> actual) {
        this.actual = actual;
    }

    @Override
    public void onNext(T t) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onError(Throwable t) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void request(long n) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSubscribe(Subscription s) {
        // TODO Auto-generated method stub
        
    }
}
