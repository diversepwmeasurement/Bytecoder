/*
 * Copyright 2021 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang.math;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;

@RunWith(UnitTestRunner.class)
public class BigIntegerTest {

    @Test
    public void testCurrentValue() {
        final BigInteger value = BigInteger.ONE;
        Assert.assertEquals(1, value.intValue());
    }

    @Test
    public void testAdd() {
        final BigInteger value = BigInteger.ONE;
        final BigInteger value2 = value.add(BigInteger.ONE);
        Assert.assertEquals(2, value2.intValue());
    }

    @Test
    public void testAdd2() {
        final BigInteger value = BigInteger.ONE;
        final BigInteger value2 = value.add(BigInteger.valueOf(2));
        Assert.assertEquals(3, value2.intValue());
    }
}
