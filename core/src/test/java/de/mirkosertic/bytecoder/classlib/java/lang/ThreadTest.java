/*
 * Copyright 2019 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class ThreadTest {

    @Test
    public void testThreadGroupName() {
        final ThreadGroup g = Thread.currentThread().getThreadGroup();
        final String name = g.getName();
        Assert.assertEquals("main", name);
    }

    @Test
    public void testThreadName() {
        final Thread t = Thread.currentThread();
        final String name = t.getName();
        Assert.assertEquals("main", name);
    }

    static class Holder {
        static int counter;
    }

    @Test
    public void testThreadRun() {
        Holder.counter = 0;
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Holder.counter = Holder.counter + 1;
                }
            }
        });
        t.run();
        Assert.assertEquals(10, Holder.counter);
    }

    @Test
    public void testThreadStart() throws InterruptedException {
        Holder.counter = 0;
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Holder.counter = Holder.counter + 1;
                }
            }
        });
        t.start();
        t.join();
        Assert.assertEquals(10, Holder.counter);
    }

}
