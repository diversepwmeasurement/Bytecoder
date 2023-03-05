/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.backend.wasm;

import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpaqueTypesAdapterMethods {

    private final Map<ResolvedClass, List<ResolvedMethod>> knownMethods;

    public OpaqueTypesAdapterMethods() {
        knownMethods = new HashMap<>();
    }

    public void register(final ResolvedClass resolvedClass, final ResolvedMethod method) {
        final List<ResolvedMethod> knownForClass = knownMethods.computeIfAbsent(resolvedClass, key -> new ArrayList<>());
        knownForClass.add(method);
    }

    public Map<ResolvedClass, List<ResolvedMethod>> getKnownMethods() {
        return knownMethods;
    }
}
