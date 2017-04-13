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

import org.reactivestreams.Publisher;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Performs the standard Publisher verification tests from the Reactive-Streams TCK
 * with extended element count ranges.
 * <p>
 * In the original TCK tests almost all tests use a specific element count and
 * many don't test shorter or longer Publishers.
 * <p>
 * In addition, sometimes libraries return special Publishers when one requests
 * 0 or 1 element Publisher and either these are not tested by some methods or
 * not really tested with longer settings.
 * @param <T> the element type
 */
@Test
public abstract class StandardPublisherVerification<T> {

    /**
     * The test settings.
     */
    protected final TckTestSettings settings;

    /**
     * Constructs a StandardPublisherVerification with the default
     * test settings.
     */
    public StandardPublisherVerification() {
        this(new TckTestSettings());
    }

    /**
     * Constructs a StandardPublisherVerification with the specified
     * custom test settings.
     * @param settings the test settings to use, not null
     */
    public StandardPublisherVerification(TckTestSettings settings) {
        this.settings = settings;
    }

    /**
     * Called for each test method and number of elements to be tested and
     * the implementor of the verification class should return a non-null
     * Publisher that is able to emit the number of elements specified
     * and then complete.
     * <p>
     * Override the {@link #maximumNumberOfElements()} to specify the maximum
     * number of elements this Publisher can be created for. Default value
     * is any length.
     * <p>
     * Override the {@link #mayReturnLessElements()} to indicate the Publisher
     * may return less elements than the number of elements specified at creation.
     * <p>
     * Override the {@link #isErrorPublisher()} to indicate the Publisher returned
     * terminates with an onError instead of an onComplete.
     * @param elements the number of elements expected from the returned publisher
     * @return the Publisher instance prepared to be tested
     */
    public abstract Publisher<T> createPublisher(int elements);

    /**
     * Override this method to specify the publisher created via {@link #createPublisher(int)}
     * terminates with an onError instead of an onComplete.
     * @return true if the Publisher terminates with an error
     */
    public boolean isErrorPublisher() {
        return false;
    }

    /**
     * Override this method to specify the minimum number of elements
     * to create a Publisher for via {@link #createPublisher(int)}
     * @return the minimum number of elements the Publisher supports
     */
    public int minimumNumberOfElements() {
        return 0;
    }

    /**
     * Override this method to specify the maximum number of elements for both
     * the {@link #createPublisher(int)}
     * can return. The default implementation indicates an any-number Publisher.
     * @return the maximum number of elements
     */
    public int maximumNumberOfElements() {
        return -1;
    }

    /**
     * Override this method to specify the {@link #createPublisher(int)}
     * may return fewer elements and
     * terminate than the element count specified at creation time.
     * The default is false indicating a Publisher which can produce exactly the
     * required number of elements.
     * @return if true, the Publisher may return fewer elements
     */
    public boolean mayReturnLessElements() {
        return false;
    }

    @Test
    public void requiredPublisherWorks() {
        runPublisher(true, new TestBody<T>() {
            @Override
            public void run(Publisher<T> pub, int elements, boolean exact, boolean error) throws Throwable {
                TckStandardSubscriber<T> sub = settings.newStandardSubscriber();
                try {
                    if (elements != 0) {
                        sub.request(Long.MAX_VALUE);
                    }

                    pub.subscribe(sub);

                    sub.expectSubscribe();

                    if (exact) {
                        sub.expectElements(elements);
                    } else {
                        sub.expectAnyElements(elements);
                    }

                    if (error) {
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

    // -------------------------------------------------------------------------
    // Standard test infrastructure
    // -------------------------------------------------------------------------


    protected interface TestBody<T> {

        void run(Publisher<T> publisher, int elements, boolean exact, boolean errorResult) throws Throwable;

    }

    protected final AssertionError fail(String message, List<? extends Throwable> errors, List<Integer> elementCounts) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        for (int i = 0; i < errors.size(); i++) {
            pw.print("Elements: ");
            pw.print(elementCounts.get(i));
            Throwable e = errors.get(i);

            pw.print(" - ");
            pw.print(e.getClass().getSimpleName());
            pw.print(": ");
            String[] msg = e.getMessage().split("\n");
            if (msg.length != 0) {
                pw.println(msg[0].trim());
            }

            e.printStackTrace(pw);
        }
        pw.close();
        return new AssertionError(sw.toString());
    }

    protected final AssertionError fail(String message, List<? extends Throwable> errors) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        for (int i = 0; i < errors.size(); i++) {
            Throwable e = errors.get(i);
            pw.print(" - ");
            pw.print(e.getClass().getSimpleName());
            pw.print(": ");
            String[] msg = e.getMessage().split("\n");
            if (msg.length != 0) {
                pw.println(msg[0].trim());
            }

            e.printStackTrace(pw);
        }
        pw.close();
        return new AssertionError(sw.toString());
    }

    protected final SkipException skip(String message, List<? extends Throwable> errors, List<Integer> elementCounts) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        for (int i = 0; i < errors.size(); i++) {
            pw.print("Elements: ");
            pw.println(elementCounts.get(i));
            Throwable e = errors.get(i);
            e.printStackTrace(pw);
        }
        pw.close();
        return new SkipException(sw.toString());
    }

    protected final SkipException skip(String message, List<? extends Throwable> errors) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        for (int i = 0; i < errors.size(); i++) {
            Throwable e = errors.get(i);
            e.printStackTrace(pw);
        }
        pw.close();
        return new SkipException(sw.toString());
    }

    /**
     * Runs the TestBody with either the normal or error Publisher created and with
     * the varargs array of element counts to try.
     * @param required should at least one of the element tests pass and not skip?
     * @param body the callback that receives the current Publisher, the element count
     * and if the Publisher can return the exact number of items.
     * @param elements the number of elements to try
     */
    protected final void runPublisher(boolean required, TestBody<T> body, int... elements) {
        int n = elements.length;
        if (n == 0) {
            throw new IllegalArgumentException("At least one element count must be specified");
        }

        List<Throwable> errors = new ArrayList<Throwable>();
        List<Integer> elementCounts = new ArrayList<Integer>();

        boolean hasFailure = false;

        for (int element : elements) {
            try {
                int minElementSupport = minimumNumberOfElements();
                int maxElementSupport = maximumNumberOfElements();
                if (element >= minElementSupport
                        && (maxElementSupport < 0 || maxElementSupport >= element)) {
                    Publisher<T> pub = createPublisher(element);
                    boolean exact = !mayReturnLessElements();
                    boolean error = isErrorPublisher();

                    body.run(pub, element, exact, error);
                } else {
                    errors.add(new SkipException("Publisher doesn't support this many elements. Required: " + element + ", Actual: " + minElementSupport + " .. " + maxElementSupport));
                    elementCounts.add(element);
                }
            } catch (SkipException ex) {
                errors.add(ex);
                elementCounts.add(element);
                if (!ex.isSkip()) {
                    hasFailure = true;
                }
            } catch (Throwable ex) {
                errors.add(ex);
                elementCounts.add(element);
                hasFailure = true;
            }
        }

        if (hasFailure) {
            throw fail("Some or all sub-tests failed", errors, elementCounts);
        }

        if (errors.size() == n) {
            if (required) {
                throw fail("All sub-tests skipped in a required tests", errors, elementCounts);
            } else {
                throw skip("All sub-tests skipped", errors, elementCounts);
            }
        }
    }
}
