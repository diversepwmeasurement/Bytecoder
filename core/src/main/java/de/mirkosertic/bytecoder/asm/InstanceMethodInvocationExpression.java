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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class InstanceMethodInvocationExpression extends Node implements PotentialSideeffect {

    public final MethodInsnNode insnNode;

    public final ResolvedMethod resolvedMethod;

    public InstanceMethodInvocationExpression(final MethodInsnNode insnNode, final ResolvedMethod rm) {
        super(Type.getReturnType(insnNode.desc));
        this.insnNode = insnNode;
        this.resolvedMethod = rm;
    }

    @Override
    public String additionalDebugInfo() {
        return insnNode.owner + "." + insnNode.name + insnNode.desc;
    }
}
