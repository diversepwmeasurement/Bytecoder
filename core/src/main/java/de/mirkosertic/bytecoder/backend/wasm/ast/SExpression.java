/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class SExpression extends SValue {

    private final String name;
    private final List<SValue> values;
    private final boolean flat;

    public SExpression(String aName) {
        this(aName, false);
    }

    public SExpression(String aName, boolean aFlat) {
        name = aName;
        values = new ArrayList<>();
        flat = aFlat;
    }

    public boolean isFlat() {
        return flat;
    }

    public void addValue(SValue aValue) {
        values.add(aValue);
    }

    public String getName() {
        return name;
    }

    public List<SValue> getValues() {
        return values;
    }
}