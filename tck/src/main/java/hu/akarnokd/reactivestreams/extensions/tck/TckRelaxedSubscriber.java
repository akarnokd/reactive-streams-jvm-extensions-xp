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

import java.util.Collection;

import org.reactivestreams.Subscription;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;

final class TckRelaxedSubscriber<T> implements RelaxedSubscriber<T> {

    final int itemTimeoutMillis;

    TckRelaxedSubscriber(int itemTimeoutMillis) {
        this.itemTimeoutMillis = itemTimeoutMillis;
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
    public void onSubscribe(Subscription s) {
        // TODO Auto-generated method stub
    }

    public void expectElement(T element) {
        // TODO Auto-generated method stub
    }

    public void expectComplete() {
        // TODO Auto-generated method stub
    }

    public void expectError() {
        // TODO Auto-generated method stub
    }

    public void expectTerminate() {
        // TODO Auto-generated method stub
    }

    public <E extends Throwable> void expectError(Class<E> cause) {
        // TODO Auto-generated method stub
    }

    public boolean tryExpectElement(T element) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean tryExpectAnyElement(Collection<T> elements) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean tryExpectComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean tryExpectError() {
        // TODO Auto-generated method stub
        return false;
    }

    public <E extends Throwable> boolean tryExpectError(Class<E> cause) {
        // TODO Auto-generated method stub
        return false;
    }

    public <E extends Throwable> boolean tryExpectAnyError(Collection<Class<E>> cause) {
        // TODO Auto-generated method stub
        return false;
    }

    public void request(long n) {
        // TODO Auto-generated method stub
    }

    public void cancel() {
        // TODO Auto-generated method stub
    }

    public void expectSubscribe() {
        // TODO Auto-generated method stub
    }

    public void expectFusionMode(int mode) {
        // TODO Auto-generated method stub
    }

    public void requestFusionMode(int mode) {
     // TODO Auto-generated method stub
    }
}
