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

package hu.akarnokd.reactivestreams.extensions.examples;

public final class UndeliverableErrors {

    static volatile Handler handler;

    private UndeliverableErrors() {
        throw new IllegalStateException("No instances!");
    }

    public interface Handler {

        void handle(Throwable error);

    }

    public static void onError(Throwable error) {
        if (error == null) {
            error = new NullPointerException("error is null");
        }
        Handler h = handler;
        if (h != null) {
            h.handle(error);
        }
    }
}
