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

import java.util.*;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

public abstract class RelaxedPublisherVerification<T> {

    public interface ExternalErrorConsumer {

        void accept(Throwable error);

    }

    final TckRelaxedTestSettings settings;

    public RelaxedPublisherVerification() {
        this(new TckRelaxedTestSettings());
    }

    public RelaxedPublisherVerification(TckRelaxedTestSettings settings) {
        this.settings = settings;
    }

    final List<Throwable> externalErrors = Collections.synchronizedList(new ArrayList<Throwable>());

    final ExternalErrorConsumer errorConsumer = new ExternalErrorConsumer() {
        @Override
        public void accept(Throwable error) {
            externalErrors.add(error);
        }
    };

    public abstract Publisher<T> createPublisher(int elements);

    public Publisher<T> createErrorPublisher(int elements) {
        return null;
    }

    public int maximumNumberOfElements() {
        return -1;
    }

    public boolean supportsFusionMode(int mode) {
        return true;
    }

    public void setExternalErrorHandler(ExternalErrorConsumer errorConsumer) {
        // default does not provide any external error handling capability
    }

    interface TestBody<T> {

        void run(Publisher<T> publisher, int elements) throws Throwable;

    }

    @Test
    public void optionalZeroRequestSignalsError() {

    }

    @Test
    public void optionalNegativeRequestSignalsError() {

    }
}
