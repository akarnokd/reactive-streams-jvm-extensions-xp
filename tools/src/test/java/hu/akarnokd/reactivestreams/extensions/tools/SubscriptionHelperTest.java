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

import java.lang.reflect.*;

import org.junit.*;

public class SubscriptionHelperTest {
    
    @Test
    public void noInstances() throws Exception {
        Constructor<SubscriptionHelper> c = SubscriptionHelper.class.getDeclaredConstructor();
        
        c.setAccessible(true);
        
        try {
            c.newInstance();
            Assert.fail("Should have thrown!");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.toString(), ex.getCause() instanceof IllegalStateException);
            Assert.assertEquals(ex.toString(), "No instances!", ex.getCause().getMessage());
        }
    }
}