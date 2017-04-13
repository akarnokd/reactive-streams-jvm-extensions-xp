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
import java.util.Map;

import org.reactivestreams.Publisher;
import org.testng.annotations.*;

import hu.akarnokd.reactivestreams.extensions.tck.support.*;
import hu.akarnokd.reactivestreams.extensions.tck.support.TckUndeliverableErrors.Handler;

public class FusedRangeErrorPublisherTckTest extends FusedConditionalPublisherVerification<Integer> {

    static final CancellationTracker TRACKER = new CancellationTracker();

    @Override
    public Publisher<Integer> createPublisher(int elements) {
        return new FusedRangePublisher(1, elements, new Exception(), TRACKER);
    }

    @Override
    public Integer typicalItem() {
        return 1;
    }

    @Override
    public boolean isErrorPublisher() {
        return true;
    }

    @Override
    @Test(enabled = false)
    public void setExternalErrorHandler(final ExternalErrorConsumer errorConsumer) {
        if (errorConsumer == null) {
            TckUndeliverableErrors.handler = null;
        } else {
            TckUndeliverableErrors.handler = new Handler() {
                @Override
                public void handle(Throwable e) {
                    errorConsumer.accept(e);
                }
            };
        }
    }

    @AfterClass
    public static void afterClass() {
        if (!TRACKER.stacks.isEmpty()) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Some tests didn't consume or cancel the source properly.");
            for (Map.Entry<Long, StackTraceElement[]> e : TRACKER.stacks.entrySet()) {
                pw.print("Case #");
                pw.println(e.getKey());
                for (StackTraceElement ste : e.getValue()) {
                    pw.print("\tat ");
                    pw.println(ste);
                }
            }
            pw.close();
            throw new AssertionError(sw.toString());
        }
    }
}
