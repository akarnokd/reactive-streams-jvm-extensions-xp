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
import org.testng.annotations.*;

public abstract class RelaxedPublisherVerification<T> extends StandardPublisherVerification<T> {

    /**
     * The synchronized list of externally received errors.
     */
    protected final List<Throwable> externalErrors = Collections.synchronizedList(new ArrayList<Throwable>());

    /**
     * The functional interface to consume external errors.
     */
    public interface ExternalErrorConsumer {

        void accept(Throwable error);

    }

    final ExternalErrorConsumer errorConsumer = new ExternalErrorConsumer() {
        @Override
        public void accept(Throwable error) {
            externalErrors.add(error);
        }
    };

    /**
     * Called before and after each test to install an optional
     * external error consumer to capture errors not sent
     * through onError for some reason. Not all tests
     * will care about such external errors.
     * <p>
     * Note that due to how TestNG works, you have to put
     * a <code>&#64;Test(enabled = false)</code> annotation
     * when overriding this method.
     * @param errorConsumer the consumer callback, null to unset a consumer
     */
    @Test(enabled = false)
    public void setExternalErrorHandler(ExternalErrorConsumer errorConsumer) {
        // default does not provide any external error handling capability
    }

    @BeforeMethod
    public void setupExternalErrorHandler() {
        setExternalErrorHandler(errorConsumer);
    }

    @AfterMethod
    public void cleanupExternalErrorHandler() {
        setExternalErrorHandler(null);
    }

    protected void clearExternalErrors() {
        externalErrors.clear();
    }

    protected void expectNoExternalErrors() {
        if (!externalErrors.isEmpty()) {
            throw fail("External errors present: ", externalErrors);
        }
    }

    protected boolean tryExpectExternalErrorMessageContains(String text) {
        for (Throwable ex : externalErrors) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            if (sw.toString().contains(text)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void optionalZeroRequestSignalsError() {
        runPublisher(false, false, new TestBody<T>() {
            @Override
            public void run(Publisher<T> publisher, int elements, boolean exact) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();

                try {
                    publisher.subscribe(sub);

                    sub.expectSubscribe();

                    sub.requestDirect(0);

                    Throwable error = sub.tryExpectError();
                    if (error == null) {
                        if (externalErrors.isEmpty()) {
                            throw new SkipException("No error received within " + settings.itemTimeoutMillis + " ms");
                        } else {
                            for (Throwable ex : externalErrors) {
                                if (ex instanceof IllegalArgumentException) {
                                    return;
                                }
                                if (ex instanceof IllegalStateException) {
                                    return;
                                }
                            }
                            throw skip("Errors received but none of them was recognized", externalErrors);
                        }
                    } else {
                        if (error instanceof IllegalArgumentException) {
                            return;
                        }
                        if (error instanceof IllegalStateException) {
                            return;
                        }
                        throw new SkipException("An unrecognized error was received", error);
                    }
                } catch (Throwable ex) {
                    sub.cancel();
                    throw ex;
                }
            }
        }, 0, 1, 2, 3, 5, 10, 20);
    }


    @Test
    public void optionalZeroRequestSignalsErrorAfterOneElement() {
        runPublisher(false, false, new TestBody<T>() {
            @Override
            public void run(Publisher<T> publisher, int elements, boolean exact) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();

                try {
                    publisher.subscribe(sub);

                    sub.expectSubscribe();

                    sub.request(1);

                    if (exact) {
                        sub.expectElements(1);
                    } else {
                        sub.expectAnyElements(1);
                    }
                    sub.requestDirect(0);

                    Throwable error = sub.tryExpectError();
                    if (error == null) {
                        if (externalErrors.isEmpty()) {
                            throw new SkipException("No error received within " + settings.itemTimeoutMillis + " ms");
                        } else {
                            for (Throwable ex : externalErrors) {
                                if (ex instanceof IllegalArgumentException) {
                                    return;
                                }
                                if (ex instanceof IllegalStateException) {
                                    return;
                                }
                            }
                            throw skip("Errors received but none of them was recognized", externalErrors);
                        }
                    } else {
                        if (error instanceof IllegalArgumentException) {
                            return;
                        }
                        if (error instanceof IllegalStateException) {
                            return;
                        }
                        throw new SkipException("An unrecognized error was received", error);
                    }
                } catch (Throwable ex) {
                    sub.cancel();
                    throw ex;
                }
            }
        }, 2, 3, 5, 10, 20);
    }


    @Test
    public void optionalNegativeRequestSignalsError() {
        runPublisher(false, false, new TestBody<T>() {
            @Override
            public void run(Publisher<T> publisher, int elements, boolean exact) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();

                try {
                    publisher.subscribe(sub);

                    sub.expectSubscribe();

                    sub.requestDirect(-1);

                    Throwable error = sub.tryExpectError();
                    if (error == null) {
                        if (externalErrors.isEmpty()) {
                            throw new SkipException("No error received within " + settings.itemTimeoutMillis + " ms");
                        } else {
                            for (Throwable ex : externalErrors) {
                                if (ex instanceof IllegalArgumentException) {
                                    return;
                                }
                                if (ex instanceof IllegalStateException) {
                                    return;
                                }
                            }
                            throw skip("Errors received but none of them was recognized", externalErrors);
                        }
                    } else {
                        if (error instanceof IllegalArgumentException) {
                            return;
                        }
                        if (error instanceof IllegalStateException) {
                            return;
                        }
                        throw new SkipException("An unrecognized error was received", error);
                    }
                } catch (Throwable ex) {
                    sub.cancel();
                    throw ex;
                }
            }
        }, 0, 1, 2, 3, 5, 10, 20);
    }


    @Test
    public void optionalNegativeRequestSignalsErrorAfterOneElement() {
        runPublisher(false, false, new TestBody<T>() {
            @Override
            public void run(Publisher<T> publisher, int elements, boolean exact) throws Throwable {
                TckFusedSubscriber<T> sub = settings.newFusedSubscriber();

                try {
                    publisher.subscribe(sub);

                    sub.expectSubscribe();

                    sub.request(1);

                    if (exact) {
                        sub.expectElements(1);
                    } else {
                        sub.expectAnyElements(1);
                    }
                    sub.requestDirect(-1);

                    Throwable error = sub.tryExpectError();
                    if (error == null) {
                        if (externalErrors.isEmpty()) {
                            throw new SkipException("No error received within " + settings.itemTimeoutMillis + " ms");
                        } else {
                            for (Throwable ex : externalErrors) {
                                if (ex instanceof IllegalArgumentException) {
                                    return;
                                }
                                if (ex instanceof IllegalStateException) {
                                    return;
                                }
                            }
                            throw skip("Errors received but none of them was recognized", externalErrors);
                        }
                    } else {
                        if (error instanceof IllegalArgumentException) {
                            return;
                        }
                        if (error instanceof IllegalStateException) {
                            return;
                        }
                        throw new SkipException("An unrecognized error was received", error);
                    }
                } catch (Throwable ex) {
                    sub.cancel();
                    throw ex;
                }
            }
        }, 2, 3, 5, 10, 20);
    }
}
