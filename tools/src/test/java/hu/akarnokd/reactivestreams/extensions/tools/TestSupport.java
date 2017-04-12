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

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test support utility methods.
 */
public final class TestSupport {

    public static final int LOOP = 1000;

    static final ExecutorService executor;

    static {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "TestSupportThread");
                        t.setDaemon(true);
                        return t;
                    }
                }
        );
        tpe.setKeepAliveTime(1, TimeUnit.SECONDS);
        tpe.allowCoreThreadTimeOut(true);

        executor = tpe;
    }

    /** Utility class. */
    private TestSupport() {
        throw new IllegalStateException("No instances!");
    }

    public static void race(Runnable r1, final Runnable r2) {

        final AtomicInteger counter = new AtomicInteger(2);
        final CountDownLatch cdl = new CountDownLatch(1);

        final Throwable[] errors = { null, null };

        Future<?> f = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (counter.decrementAndGet() != 0) {
                        while (counter.get() != 0) { }
                    }
                    r2.run();
                } catch (Throwable ex) {
                    errors[1] = ex;
                }
                cdl.countDown();
            }
        });

        try {
            if (counter.decrementAndGet() != 0) {
                while (counter.get() != 0) { }
            }
            r1.run();
        } catch (Throwable ex) {
            errors[0] = ex;
        }

        try {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                f.cancel(true);
                if (errors[1] == null) {
                    errors[1] = new TimeoutException("r2 did not finish on time");
                }
            }
        } catch (InterruptedException ex) {
            throw new AssertionError("race interrupted");
        }

        if (errors[0] != null && errors[1] == null) {
            AssertionError ae = new AssertionError("r1 failed");
            ae.initCause(errors[0]);
            throw ae;
        }

        if (errors[0] == null && errors[1] != null) {
            AssertionError ae = new AssertionError("r2 failed");
            ae.initCause(errors[1]);
            throw ae;
        }

        if (errors[0] != null && errors[1] != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Both r1 & r2 failed");
            errors[0].printStackTrace(pw);
            errors[1].printStackTrace(pw);
            throw new AssertionError(sw.toString());
        }
    }
}
