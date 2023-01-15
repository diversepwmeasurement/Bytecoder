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
package de.mirkosertic.bytecoder.asm.loader;

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.asm.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.parser.Loader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class BytecoderLoader implements Loader {

    private final ClassLoader classLoader;

    public BytecoderLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public ClassNode loadClassFor(final Type type) throws IOException, ClassNotFoundException {
        final ClassNode originalClassNode = internalLoad(type.getClassName());
        try {
            String shadowClassName = "de.mirkosertic.bytecoder.classlib." + type.getClassName();
            final int p = shadowClassName.lastIndexOf(".");
            if (p >= 0) {
                shadowClassName = new StringBuilder(shadowClassName).insert(p + 1, "T").toString();
            } else {
                shadowClassName = "T" + shadowClassName;
            }
            final ClassNode shadowType = internalLoad(shadowClassName);
            patchWith(originalClassNode, shadowType);
            return originalClassNode;
        } catch (final ClassNotFoundException e) {
            // Do nothing
        }
        return originalClassNode;
    }

    private ClassNode internalLoad(final String className) throws IOException, ClassNotFoundException {
        final String resourceName = className.replace(".", "/") + ".class";
        for (final ClassLibProvider clProvider : ClassLibProvider.availableProviders()) {
            final InputStream is = clProvider.getClass().getClassLoader().getResourceAsStream(clProvider.getResourceBase() + "/" + resourceName);
            if (is != null) {
                final ClassReader reader = new ClassReader(is);
                final ClassNode classNode = new ClassNode();
                reader.accept(classNode, ClassReader.EXPAND_FRAMES);
                return classNode;
            }
        }
        final InputStream fromRoot = classLoader.getResourceAsStream(resourceName);
        if (fromRoot != null) {
            final ClassReader reader = new ClassReader(fromRoot);
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        }
        throw new ClassNotFoundException(resourceName);
    }

    private void patchWith(final ClassNode original, final ClassNode patch) {
        final Map<String, Object> isObjectInfo = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/IsObject;", patch.visibleAnnotations);
        final Map<String, Object> substitutionInfo = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/SubstitutesInClass;", patch.visibleAnnotations);
        if (Boolean.TRUE.equals(substitutionInfo.get("completeReplace"))) {
            original.methods.clear();
            for (final MethodNode m : patch.methods) {
                if (isObjectInfo != null && m.name.equals("<init>")) {
                    continue;
                }
                final Map<String, Object> replaceInfo = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/Substitutes;", m.visibleAnnotations);
                if (replaceInfo != null) {
                    final String finalName = (String) replaceInfo.get("value");
                    for (final MethodNode o : original.methods) {
                        if (o.name.equals(finalName)) {
                            m.desc = o.desc;
                        }
                    }

                    m.name = finalName;
                }
                original.methods.add(m);
            }

            original.fields.clear();
            original.fields.addAll(patch.fields);
        } else {
            original.fields.addAll(patch.fields);

            for (final MethodNode patchMethod : patch.methods) {
                search: for (final MethodNode originalMethod : original.methods) {
                    if (originalMethod.name.equals(patchMethod.name) && originalMethod.desc.equals(patchMethod.desc)) {
                        // We have something to patch
                        original.methods.remove(originalMethod);
                        break search;
                    }
                }
                original.methods.add(patchMethod);
            }
        }

        final Type originalType = Type.getObjectType(original.name);
        final Type patchType = Type.getObjectType(patch.name);
        for (final FieldNode fn : original.fields) {
            final Type t = Type.getType(fn.desc);
            if (t.getClassName().equals(patchType.getClassName())) {
                fn.desc = originalType.getDescriptor();
            }
        }
        for (final MethodNode m : original.methods) {
            final Type methodType = Type.getMethodType(m.desc);
            Type returnType = methodType.getReturnType();
            if (returnType.equals(patchType)) {
                returnType = originalType;
            }
            final Type patchedType = Type.getMethodType(returnType, methodType.getArgumentTypes());
            m.desc = patchedType.getDescriptor();

            final InsnList insnNodes = m.instructions;
            if (insnNodes != null) {
                AbstractInsnNode n = insnNodes.getFirst();
                while (n != null) {
                    if (n instanceof FieldInsnNode) {
                        final FieldInsnNode f = (FieldInsnNode) n;
                        if (f.owner.equals(patch.name)) {
                            f.owner = original.name;
                        }
                    } else if (n instanceof MethodInsnNode) {
                        final MethodInsnNode mi = (MethodInsnNode) n;
                        if (mi.owner.equals(patch.name)) {
                            mi.owner = original.name;
                        }
                    }
                    n = n.getNext();
                }
            }
        }
    }
}