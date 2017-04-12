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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.reactivestreams.*;

import hu.akarnokd.reactivestreams.extensions.tools.SubscriptionTools;
import hu.akarnokd.reactivestreams.extensions.tools.SubscriptionTools.SetOnceResult;

class TckStandardSubscriber<T> implements Subscriber<T> {

    final AtomicReference<Subscription> upstream;

    final AtomicLong requested;

    final int itemTimeoutMillis;

    final ConcurrentLinkedQueue<Object> queue;

    final Lock lock;

    final Condition nonEmpty;

    final CountDownLatch subscribed;

    final CountDownLatch terminated;

    final List<Throwable> errors;

    static final Object COMPLETE = new Object();

    volatile long subscribeCount;

    volatile long elementCount;

    volatile long errorCount;

    volatile long completeCount;

    static final class ErrorSignal {
        final Throwable error;
        ErrorSignal(Throwable error) {
            this.error = error;
        }
        @Override
        public String toString() {
            return error.toString();
        }
    }

    TckStandardSubscriber(int itemTimeoutMillis) {
        this.itemTimeoutMillis = itemTimeoutMillis;
        this.upstream = new AtomicReference<Subscription>();
        this.requested = new AtomicLong();
        this.lock = new ReentrantLock();
        this.nonEmpty = lock.newCondition();
        this.queue = new ConcurrentLinkedQueue<Object>();
        this.subscribed = new CountDownLatch(1);
        this.terminated = new CountDownLatch(1);
        this.errors = Collections.synchronizedList(new ArrayList<Throwable>());
    }

    public void request(long n) {
        SubscriptionTools.deferredRequest(upstream, requested, n);
    }

    public void requestDirect(long n) {
        upstream.get().request(n);
    }

    public void cancel() {
        SubscriptionTools.cancel(upstream);
    }

    void offer(Object t) {
        queue.offer(t);
        lock.lock();
        try {
            nonEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    Object peek(int timeoutMillis) throws InterruptedException {
        Object o = queue.peek();
        if (o == null) {
            lock.lock();
            try {
                o = queue.peek();
                if (o == null) {
                    nonEmpty.await(timeoutMillis, TimeUnit.MILLISECONDS);
                    o = queue.peek();
                }
            } finally {
                lock.unlock();
            }
        }
        return o;
    }

    @Override
    public void onNext(T t) {
        if (t == null) {
            if (errorCount > 0 || completeCount > 0) {
                onError(new NullPointerException("Null element received"));
            } else {
                onError(new IllegalStateException("Null element #" + (elementCount + 1) + " received after terminated"));
            }
        } else {
            if (errorCount > 0 || completeCount > 0) {
                onError(new IllegalStateException("Element #" + (elementCount + 1) + " received after terminated: " + valueAndClass(t)));
            } else {
                offer(t);
                elementCount++;
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (t == null) {
            t = new NullPointerException("Null Throwable received");
        }
        errors.add(t);
        offer(new ErrorSignal(t));
        errorCount++;
        terminated.countDown();
    }

    @Override
    public void onComplete() {
        offer(COMPLETE);
        completeCount++;
        terminated.countDown();
    }

    @Override
    public void onSubscribe(Subscription s) {
        SetOnceResult result = SubscriptionTools.deferredSetOnce(upstream, requested, s);
        if (result == SetOnceResult.ALREADY_SET) {
            onError(new IllegalStateException("Subscription already set!"));
        }
        subscribeCount++;
        subscribed.countDown();
    }

    public boolean expectAnyElementOrComplete() throws Throwable {
        return false;
    }

    public boolean expectAnyElementOrError() throws Throwable {
        return false;
    }

    /**
     * Checks if the given number of elements are received.
     * @param elementCount the number of elements expected, non-negative
     * @throws Throwable allow throwing any exceptions
     */
    public void expectElements(int elementCount) throws Throwable {
        for (int i = 0; i < elementCount; i++) {
            Object o = peek(itemTimeoutMillis);
            if (o == null) {
                throw fail("Element #" + (i + 1) + " not received within " + itemTimeoutMillis + " ms");
            }
            if (o == COMPLETE) {
                throw fail("Unexpected completion after " + (i + 1) + " / " + elementCount + " elements ");
            }
            if (o instanceof ErrorSignal) {
                throw fail("Unexpected error after " + (i + 1) + " / " + elementCount + " elements: " + o);
            }
            queue.poll();
        }
        Object o = peek(itemTimeoutMillis);
        if (o == null || o == COMPLETE || o instanceof ErrorSignal) {
            return;
        }
        if (elementCount == 0) {
            throw fail("No elements expected yet one received: " + valueAndClass(o));
        }
        throw fail("Exactly " + elementCount + " elements expected yet one extra received: " + valueAndClass(o));
    }

    /**
     * Checks if at most the given number of elements are received.
     * @param elmentCount
     * @throws Throwable
     */
    public void expectAnyElements(int elmentCount) throws Throwable {
        for (int i = 0; i < elementCount; i++) {
            Object o = peek(itemTimeoutMillis);
            if (o == null) {
                throw fail("Element #" + (i + 1) + " not received within " + itemTimeoutMillis + " ms");
            }
            if (o == COMPLETE || o instanceof ErrorSignal) {
                return;
            }
            queue.poll();
        }
        Object o = peek(itemTimeoutMillis);
        if (o == null || o == COMPLETE || o instanceof ErrorSignal) {
            return;
        }
        if (elementCount == 0) {
            throw fail("No elements expected yet one received: " + valueAndClass(o));
        }
        throw fail("At most " + elementCount + " elements expected yet one extra received: " + valueAndClass(o));
    }

    public void expectElement(T element) throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("No element received within " + itemTimeoutMillis + " ms");
        }
        if (o instanceof ErrorSignal) {
            throw fail("Element expected but error found: " + o);
        }
        if (o == COMPLETE) {
            throw fail("Element expected but completion found");
        }
        queue.poll();
        if (!element.equals(o)) {
            throw fail("Expected: " + valueAndClass(element) + ", Actual: " + valueAndClass(o));
        }
    }

    public boolean expectAnyElement(Collection<T> elements) throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("No signal received within " + itemTimeoutMillis + " ms");
        }
        if (o instanceof ErrorSignal) {
            throw fail("Element expected but error found: " + o);
        }
        if (o == COMPLETE) {
            throw fail("Element expected but completion found");
        }
        queue.poll();
        if (!elements.contains(o)) {
            throw fail("Element " + valueAndClass(o) + " not in the expected collection " + elements);
        }
        return true;
    }

    public void expectComplete() throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("Not completed within " + itemTimeoutMillis + " ms");
        }
        if (o == COMPLETE) {
            queue.poll();
            return;
        }
        if (o instanceof ErrorSignal) {
            throw fail("Completion expected but error found: " + o);
        }
        throw fail("Completion expected but element found: " + valueAndClass(o));
    }

    public void expectError() throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("Error not received within " + itemTimeoutMillis + " ms");
        }
        if (o instanceof ErrorSignal) {
            queue.poll();
            return;
        }
        if (o == COMPLETE) {
            throw fail("Error expected but completion found");
        }
        throw fail("Error expected but element found: " + valueAndClass(o));
    }

    public void expectTerminate() throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("No terminal signal received within " + itemTimeoutMillis + " ms");
        }
        if (o == COMPLETE || o instanceof ErrorSignal) {
            queue.poll();
            return;
        }
        throw fail("Terminal signal expected but element found: " + valueAndClass(o));
    }

    /**
     * Waits for the next signal and returns true if it is an item equal to the expected value,
     * or returns false if this signal is a terminal event, fails otherwise.
     * @param element the expected element
     * @return true if the expected element was received, false if a terminal event was received instead.
     * @throws Throwable allows throwing any exception
     */
    public boolean tryExpectElement(T element) throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("No signal received within " + itemTimeoutMillis + " ms");
        }
        if (o == COMPLETE || o instanceof ErrorSignal) {
            return false;
        }
        queue.poll();
        if (!element.equals(o)) {
            throw fail("Expected: " + valueAndClass(element) + ", Actual: " + valueAndClass(o));
        }
        return true;
    }

    public boolean tryExpectAnyElement(Collection<T> elements) throws Throwable {
        Object o = peek(itemTimeoutMillis);
        if (o == null) {
            throw fail("No signal received within " + itemTimeoutMillis + " ms");
        }
        if (o == COMPLETE || o instanceof ErrorSignal) {
            return false;
        }
        queue.poll();
        if (!elements.contains(o)) {
            throw fail("Element " + valueAndClass(o) + " not in the expected collection " + elements);
        }
        return true;
    }

    public void expectSubscribe() throws Throwable {
        if (!subscribed.await(itemTimeoutMillis, TimeUnit.MILLISECONDS)) {
            throw fail("onSubscribe not called within " + itemTimeoutMillis + " milliseconds");
        }
    }

    public void expectNoErrors() throws Throwable {
        long c = errorCount;
        if (c == 1) {
            throw fail("Unexpected error: " + errors.get(0));
        }
        if (c > 1) {
            throw fail("Unexpected multiple errors: " + c);
        }
    }

    public void expectNoComplete() throws Throwable {
        long c = completeCount;
        if (c == 1) {
            throw fail("Unexpected completion");
        }
        if (c > 1) {
            throw fail("Unexpected multiple completions: " + c);
        }
    }

    public void expectValidState() {
        if (errorCount > 0 && completeCount > 0) {
            throw fail("Invalid state");
        }
    }

    protected final AssertionError fail(String message) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);

        pw.print("onSubscribe: ");
        pw.print(subscribeCount);
        pw.print(", onNext: ");
        pw.print(elementCount);
        pw.print(", onError: ");
        pw.print(errorCount);
        pw.print(", onComplete: ");
        pw.print(completeCount);
        if (SubscriptionTools.isCancelled(upstream)) {
            pw.println(", cancelled");
        } else {
            pw.println();
        }

        for (Throwable e : errors) {
            e.printStackTrace(pw);
        }

        pw.close();
        return new AssertionError(sw.toString());
    }

    protected final String valueAndClass(Object o) {
        if (o == null) {
            return "null";
        }
        return o + " (" + o.getClass().getSimpleName() + ")";
    }
}
