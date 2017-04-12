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

package hu.akarnokd.reactivestreams.extensions;

import org.reactivestreams.*;

/**
 * An extension to the {@link Subscriber} interface by relaxing some of the
 * (textual) rules of {@code Subscriber}, otherwise it has the same four methods.
 * <p>
 * The rule relaxations are as follows:
 * <ul>
 * <li><b>§1.3 relaxation:</b> {@code onSubscribe} may run concurrently with {@code onNext} in case 
 * the {@code RelaxedSubscriber} calls {@code request()} from inside {@code onSubscribe} and it is the 
 * resposibility of {@code RelaxedSubscriber} to ensure thread-safety between the remaining 
 * instructions in {@code onSubscribe} and {@code onNext}.</li>
 * <li><b>§2.3 relaxation:</b> calling {@code Subscription.cancel()} and {@code Subscription.request()} from 
 * {@code RelaxedSubscriber.onComplete()} or {@code RelaxedSubscriber.onError()} is considered a no-operation.</li>
 * <li><b>§2.12 relaxation:</b> if the same {@code RelaxedSubscriber} instance is subscribed to 
 * multiple sources, it must ensure its {@code onXXX} methods remain thread safe.</li>
 * <li><b>§3.9 relaxation:</b> issuing a non-positive {@code request()} will not stop the current stream 
 * and otherwise not required to signal an {@code IllegalArgumentException} via {@code onError}.</li>
 * </ul>
 * @param <T> the value type
 */
public interface RelaxedSubscriber<T> extends Subscriber<T> {

    /**
     * {@inheritDoc}
     * <p>
     * Additional requirements/relaxations:
     * <ul>
     * <li><b>§1.3 relaxation:</b> {@code onSubscribe} may run concurrently with {@code onNext} in case 
     * the {@code RelaxedSubscriber} calls {@code request()} from inside {@code onSubscribe} and it is the 
     * resposibility of {@code RelaxedSubscriber} to ensure thread-safety between the remaining 
     * instructions in {@code onSubscribe} and {@code onNext}.</li>
     * </ul>
     */
    @Override
    void onSubscribe(Subscription s);

}