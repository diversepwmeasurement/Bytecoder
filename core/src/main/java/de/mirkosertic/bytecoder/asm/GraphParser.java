/*
 * Copyright 2022 Mirko Sertic
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

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;

public class GraphParser {

    private final Graph graph;

    private final MethodNode methodNode;

    private final CompileUnit compileUnit;

    private final AnalysisStack analysisStack;

    private final Map<Integer, String> opcodeToName;

    public GraphParser(final CompileUnit compileUnit, final Type ownerType, final MethodNode methodNode, final AnalysisStack analysisStack) {
        this.methodNode = methodNode;
        this.graph = new Graph();
        this.compileUnit = compileUnit;
        this.analysisStack = analysisStack;
        this.opcodeToName = new HashMap<>();

        for (final Field f : Opcodes.class.getDeclaredFields()) {
            if (f.getType() == int.class &&
                !f.getName().startsWith("V") &&
                !f.getName().startsWith("ACC_") &&
                !f.getName().startsWith("T_")  &&
                !f.getName().startsWith("H_") &&
                !f.getName().startsWith("F_")) {
                try {
                    final int code = (int) f.get(Opcodes.class);
                    opcodeToName.put(code, f.getName());
                } catch (final Exception e) {
                    throw new IllegalStateException("Error extracting opcode translations", e);
                }
            }
        }

        parse(ownerType);
    }

    private Region getOrCreateRegionNodeFor(final LabelNode label) {
        final String nodeLabel = label.getLabel().toString();
        final Region r = graph.regionByLabel(nodeLabel);
        if (r != null) {
            return r;
        }
        for (final TryCatchBlockNode n : methodNode.tryCatchBlocks) {
            if (n.start == label) {
                return graph.newTryCatch(nodeLabel);
            }
        }
        return graph.newRegion(nodeLabel);
    }

    private void parse(final Type ownerType) {
        // Construct the initial parsing state
        final Value[] initialStack = new Value[0];
        final Value[] initialLocals = new Value[methodNode.maxLocals];

        final Type methodType = Type.getMethodType(methodNode.desc);

        int localIndex = 0;
        if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
            initialLocals[localIndex++] = graph.newThis(ownerType);
        }
        for (int i = 0; i < methodType.getArgumentTypes().length; i ++)  {
            final Type t = methodType.getArgumentTypes()[i];
            initialLocals[localIndex] = graph.newMethodArgument(t, i);
            localIndex+= t.getSize();
        }

        final Frame startFrame = new Frame(initialLocals, initialStack);
        final Region startRegion = (Region) graph.register(new Region(Graph.START_REGION_NAME));
        startRegion.frame = startFrame;

        final GraphParserState initialState = new GraphParserState(startFrame, startRegion, -1, new TryCatchGuardStackEntry[0]);

        // Step 1: We collect all jump targets
        final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction = new HashMap<>();

        // Step 2: Check for back edges
        final Stack<ControlFlow> controlFlowsToCheck = new Stack<>();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        final Set<AbstractInsnNode> alreadyVisited = new HashSet<>();
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            if (alreadyVisited.add(flow.currentNode)) {
                final AbstractInsnNode next = flow.currentNode.getNext();

                if (flow.currentNode.getOpcode() == Opcodes.RETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.IRETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.LRETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.FRETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.DRETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.ARETURN) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode.getOpcode() == Opcodes.ATHROW) {
                    // Stop parsing here
                    continue;
                }

                if (flow.currentNode instanceof LabelNode) {
                    final LabelNode labelNode = (LabelNode) flow.currentNode;
                    if (next != null) {
                        controlFlowsToCheck.push(flow.addInstructionAndContinueWith(labelNode, next));
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                        jumps.put(flow.currentNode, EdgeType.FORWARD);
                    }
                    for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                        if (tryCatchBlockNode.start == labelNode) {
                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(labelNode, tryCatchBlockNode.handler));
                        }
                    }
                } else if (flow.currentNode instanceof TableSwitchInsnNode) {
                    final TableSwitchInsnNode jump = (TableSwitchInsnNode) flow.currentNode;
                    if (flow.visited(jump.dflt)) {
                        // Back-Edge
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.dflt, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.BACK);
                    } else {
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.dflt, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.FORWARD);
                        controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, jump.dflt));
                    }
                    for (int index = 0; index < jump.labels.size(); index++) {
                        final AbstractInsnNode target = jump.labels.get(index);
                        if (flow.visited(target)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(target, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(target, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);
                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, target));
                        }
                    }
                } else if (flow.currentNode instanceof LookupSwitchInsnNode) {
                    final LookupSwitchInsnNode jump = (LookupSwitchInsnNode) flow.currentNode;
                    for (int index = 0; index < jump.labels.size(); index++) {
                        final AbstractInsnNode target = jump.labels.get(index);

                        if (flow.visited(target)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(target, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(target, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);
                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, target));
                        }
                    }
                } else if (flow.currentNode instanceof JumpInsnNode) {
                    final JumpInsnNode jump = (JumpInsnNode) flow.currentNode;
                    switch (jump.getOpcode()) {
                        case Opcodes.GOTO: {
                            if (flow.visited(jump.label)) {
                                // Back-Edge
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.BACK);
                            } else {
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.FORWARD);
                                controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, jump.label));
                            }
                            break;
                        }
                        case Opcodes.IFNULL:
                        case Opcodes.IFNONNULL:
                        case Opcodes.IFEQ:
                        case Opcodes.IFNE:
                        case Opcodes.IFLT:
                        case Opcodes.IFGE:
                        case Opcodes.IFGT:
                        case Opcodes.IFLE:
                        case Opcodes.IF_ACMPEQ:
                        case Opcodes.IF_ACMPNE:
                        case Opcodes.IF_ICMPGE:
                        case Opcodes.IF_ICMPEQ:
                        case Opcodes.IF_ICMPNE:
                        case Opcodes.IF_ICMPLT:
                        case Opcodes.IF_ICMPGT:
                        case Opcodes.IF_ICMPLE: {
                            if (flow.visited(jump.label)) {
                                // Back-Edge
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.BACK);
                            } else {
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.FORWARD);
                                controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, jump.label));
                            }

                            if (flow.visited(next)) {
                                // Back-Edge
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.BACK);
                            } else {
                                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                                jumps.put(jump, EdgeType.FORWARD);

                                controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump, next));
                            }
                            break;
                        }
                        default:
                            throw new IllegalStateException("Not implemented jump : " + jump.getOpcode());
                    }
                } else {
                    if (next != null) {
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                        jumps.put(flow.currentNode, EdgeType.FORWARD);
                        controlFlowsToCheck.push(flow.addInstructionAndContinueWith(flow.currentNode, next));
                    } else {
                        System.out.println("no more next!");
                    }
                }
            }
        }

        // Step 3: We know the back edges,
        // Now we do a forward control flow analysis
        alreadyVisited.clear();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            if (alreadyVisited.add(flow.currentNode)) {
                final String opcode = opcodeToName.get(flow.currentNode.getOpcode());
                if (opcode != null) {
                    analysisStack.addDebugMessage("Visiting #" + methodNode.instructions.indexOf(flow.currentNode) + " " + opcode + " Stack size is " + flow.graphParserState.frame.incomingStack.length + " Source line " + flow.graphParserState.lineNumber);
                }
                for (final ControlFlow nextOutCome : parse(flow, incomingEdgesPerInstruction)) {
                    if (!alreadyVisited.contains(nextOutCome.currentNode)) {
                        controlFlowsToCheck.push(nextOutCome);
                    }
                }
            }
        }

        // Step 4: Fix the initial control flow
        startRegion.addControlFlowTo(StandardProjections.DEFAULT,
                graph.translationFor(methodNode.instructions.getFirst()).main);


/*        try (final PrintWriter pw = new PrintWriter(Files.newOutputStream(new File("flow.dot").toPath()))) {
            pw.println("digraph debugoutput {");
            final List<AbstractInsnNode> known = new ArrayList<>();
            for (final Map.Entry<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> entry : incomingEdgesPerInstruction.entrySet()) {
                if (!known.contains(entry.getKey())) {
                    known.add(entry.getKey());
                }
                for (final Map.Entry<AbstractInsnNode, EdgeType> x : entry.getValue().entrySet()) {
                    if (!known.contains(x.getKey())) {
                        known.add(x.getKey());
                    }
                    switch (x.getValue()) {
                        case FORWARD:
                            pw.print(" node");
                            pw.print(known.indexOf(x.getKey()));
                            pw.print(" -> node");
                            pw.print(known.indexOf(entry.getKey()));
                            pw.println("[dir=\"forward\" color=\"red\" penwidth=\"2\"];");
                            break;
                        case BACK:
                            pw.print(" node");
                            pw.print(known.indexOf(x.getKey()));
                            pw.print(" -> node");
                            pw.print(known.indexOf(entry.getKey()));
                            pw.println("[dir=\"forward\" color=\"red\" style=\"dashed\" penwidth=\"2\"];");
                            break;
                    }
                }
            }
            for (final AbstractInsnNode node : known) {
                pw.print(" node");
                pw.print(known.indexOf(node));
                pw.print("[shape=\"box\" label=\"");
                pw.print(node);
                final String op = opcodeToString(node.getOpcode());
                if (op != null) {
                    pw.print(" ");
                    pw.print(op);
                }
                pw.println("\"];");
            }
            pw.println("}");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }*/

        // Step 5: Fixup stuff not possible during analysis
        graph.applyFixups(incomingEdgesPerInstruction);
    }

    private List<ControlFlow> parseLabelNode(final ControlFlow currentFlow) {
        final LabelNode node = (LabelNode) currentFlow.currentNode;

        final Region region = getOrCreateRegionNodeFor(node);
        region.frame = currentFlow.graphParserState.frame;
        graph.registerTranslation(node, new InstructionTranslation(region, region.frame));

        final GraphParserState state = currentFlow.graphParserState;
        final List<ControlFlow> flowsToCheck = new ArrayList<>();
        if (node.getNext() != null) {
            if (region instanceof TryCatch) {
                LabelNode endNode = null;
                TryCatch currentTryCatch = (TryCatch) region;
                GraphParserState nextState = state;
                for (int i = methodNode.tryCatchBlocks.size() - 1; i >= 0; i--) {
                    final TryCatchBlockNode tryCatchBlockNode = methodNode.tryCatchBlocks.get(i);
                    if (tryCatchBlockNode.start == node) {
                        if (endNode == null) {
                            endNode = tryCatchBlockNode.end;

                            final TryCatchGuardStackEntry tryCatchGuardStackEntry = new TryCatchGuardStackEntry((TryCatch) region, node, endNode);
                            nextState = nextState.withNewTryCatchOnStack(tryCatchGuardStackEntry);

                        } else {
                            if (endNode != tryCatchBlockNode.end) {
                                final TryCatch nestedTryCatch = graph.newTryCatch(region.label + "_" + endNode.getLabel().toString());
                                final TryCatchGuardStackEntry tryCatchGuardStackEntry = new TryCatchGuardStackEntry((TryCatch) region, node, endNode);
                                nextState = nextState.withNewTryCatchOnStack(tryCatchGuardStackEntry);

                                graph.registerTranslation(node, new InstructionTranslation(nestedTryCatch, state.frame));

                                currentTryCatch.addControlFlowTo(StandardProjections.TRYCATCHGUARD, nestedTryCatch);
                                currentTryCatch = nestedTryCatch;
                            }
                        }
                    }
                }

                graph.addFixup(new ControlFlowFixup(node, nextState.frame, StandardProjections.TRYCATCHGUARD, node.getNext()));
                flowsToCheck.add(currentFlow.continueWith(node.getNext(), nextState));

            } else {

                GraphParserState nextState = state;
                if (nextState.isEndOfTryCatchGuardBlock(node)) {
                    final TryCatchGuardStackEntry topEntry = nextState.tryCatchGuardStack[nextState.tryCatchGuardStack.length - 1];
                    topEntry.tryCatch.addControlFlowTo(StandardProjections.TRYCATCHEXIT, region);
                    nextState = nextState.popLatestTryBatchGuardBlock();
                }

                graph.addFixup(new ControlFlowFixup(node, nextState.frame, StandardProjections.DEFAULT, node.getNext()));
                flowsToCheck.add(currentFlow.continueWith(node.getNext(), nextState));
            }
        }

        if (region instanceof TryCatch) {
            // Check for exceptional flows
            for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                if (tryCatchBlockNode.start == node) {
                    if (tryCatchBlockNode.start != tryCatchBlockNode.handler) {

                        analysisStack.addDebugMessage("Start of try catch block at " + tryCatchBlockNode.start + " with handler " + tryCatchBlockNode.handler.getLabel() +" and type " + tryCatchBlockNode.type);

                        final Type exceptionType =
                                tryCatchBlockNode.type != null ? Type.getObjectType(tryCatchBlockNode.type) : Type.getType(Throwable.class);

                        final Region startRegion = getOrCreateRegionNodeFor(tryCatchBlockNode.handler);
                        final Frame frameWithPushedException = state.frame.pushToStack(graph.newCaughtException(exceptionType));
                        region.addControlFlowTo(new Projection.ExceptionHandler(exceptionType), startRegion);
                        flowsToCheck.add(currentFlow.continueWith(tryCatchBlockNode.handler, state.withFrame(frameWithPushedException)));
                    }
                }
            }
        }

        return flowsToCheck;
    }

    private List<ControlFlow> parseLineNumberNode(final ControlFlow currentFlow) {
        final LineNumberNode node = (LineNumberNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final LineNumberDebugInfo n = graph.newLineNumberDebugInfo();

        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.withLineNumber(node.line).controlFlowsTo(n);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_XLOAD(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Value value = currentState.frame.incomingLocals[local];

        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentFlow.graphParserState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_XSTORE(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Frame.PopResult popResult = currentState.frame.popFromStack();

        final Value value = popResult.value;
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = popResult.newFrame.setLocal(local, variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseVarInsnNode(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.ALOAD:
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
                return parse_XLOAD(currentFlow);
            case Opcodes.ISTORE:
            case Opcodes.ASTORE:
            case Opcodes.FSTORE:
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                return parse_XSTORE(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_INVOKESPECIAL(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        final ResolvedMethod rm = rc.resolveMethod(node.name, methodType, analysisStack);

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[incomingData.length - 1] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = latest.newFrame.popFromStack();
            incomingData[incomingData.length - 2 - i] = latest.value;
        }

        final GraphParserState newState;

        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final InstanceMethodInvocation n = graph.newInstanceMethodInvocation(node, rm);
            n.addIncomingData(incomingData);
            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        } else {
            final InstanceMethodInvocationExpression n = graph.newInstanceMethodInvocationExpression(node, rm);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy();
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.newFrame.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        }
        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_INVOKEVIRTUAL(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        if (targetClass.getSort() == Type.ARRAY) {
            // Arrays are objects, hence only the java.lang.Object type can be called
            targetClass = Type.getType(Object.class);
        }

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        rc.resolveMethod(node.name, methodType, analysisStack);

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[incomingData.length - 1] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = latest.newFrame.popFromStack();
            incomingData[incomingData.length - 2 - i] = latest.value;
        }

        final GraphParserState newState;
        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final VirtualMethodInvocation n = graph.newVirtualMethodInvocation(node);
            n.addIncomingData(incomingData);

            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        } else {
            final VirtualMethodInvocationExpression n = graph.newVirtualMethodInvocationExpression(node);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy();
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.newFrame.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        }

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_INVOKEINTERFACE(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        compileUnit.resolveClass(targetClass, analysisStack);

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[incomingData.length - 1] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = latest.newFrame.popFromStack();
            incomingData[incomingData.length - 2 - i] = latest.value;
        }

        final GraphParserState newState;
        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final InterfaceMethodInvocation n = graph.newInterfaceMethodInvocation(node);
            n.addIncomingData(incomingData);

            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        } else {
            final InterfaceMethodInvocationExpression n = graph.newInterfaceMethodInvocationExpression(node);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy();
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.newFrame.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        }

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }


    private List<ControlFlow> parse_INVOKESTATIC(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        final ResolvedMethod rm = rc.resolveMethod(node.name, methodType, analysisStack);

        Frame latest = currentState.frame;
        incomingData[0] = graph.newTypeReference(targetClass);
        for (int i = 0; i < argumentTypes.length; i++) {
            final Frame.PopResult popresult = latest.popFromStack();
            incomingData[incomingData.length - 1 - i] = popresult.value;
            latest = popresult.newFrame;
        }

        final GraphParserState newState;

        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final StaticMethodInvocation n = graph.newStaticMethodInvocation(node, rm);
            n.addIncomingData(incomingData);
            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        } else {

            final StaticMethodInvocationExpression n = graph.newStaticMethodInvocationExpression(node, rm);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy();
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        }

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseMethodInsNode(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.INVOKESPECIAL:
                return parse_INVOKESPECIAL(currentFlow);
            case Opcodes.INVOKEVIRTUAL:
                return parse_INVOKEVIRTUAL(currentFlow);
            case Opcodes.INVOKEINTERFACE:
                return parse_INVOKEINTERFACE(currentFlow);
            case Opcodes.INVOKESTATIC:
                return parse_INVOKESTATIC(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_BIPUSH(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Value value = graph.newIntNode(node.operand);
        final Variable variable = graph.newVariable(value.type);
        final Copy copyNode = graph.newCopy();
        copyNode.addIncomingData(value);
        variable.addIncomingData(copyNode);
        graph.registerTranslation(node, new InstructionTranslation(copyNode, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_SIPUSH(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Value value = graph.newShortNode((short) node.operand);
        final Variable variable = graph.newVariable(value.type);
        final Copy copyNode = graph.newCopy();
        copyNode.addIncomingData(value);
        variable.addIncomingData(copyNode);
        graph.registerTranslation(node, new InstructionTranslation(copyNode, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_NEWARRAY(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult popresult = currentState.frame.popFromStack();

        final Type elementType;
        switch (node.operand) {
            case 4:
                elementType = Type.BOOLEAN_TYPE;
                break;
            case 5:
                elementType = Type.CHAR_TYPE;
                break;
            case 6:
                elementType = Type.FLOAT_TYPE;
                break;
            case 7:
                elementType = Type.DOUBLE_TYPE;
                break;
            case 8:
                elementType = Type.BYTE_TYPE;
                break;
            case 9:
                elementType = Type.SHORT_TYPE;
                break;
            case 10:
                elementType = Type.INT_TYPE;
                break;
            case 11:
                elementType = Type.LONG_TYPE;
                break;
            default:
                throw new IllegalStateException("Not implemented array type : " + node.operand);
        }
        final NewArray value = graph.newNewArray(elementType);
        value.addIncomingData(popresult.value);

        final Variable variable = graph.newVariable(value.type);
        final Copy copyNode = graph.newCopy();
        copyNode.addIncomingData(value);
        variable.addIncomingData(copyNode);
        graph.registerTranslation(node, new InstructionTranslation(copyNode, currentState.frame));

        final Frame newFrame = popresult.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseIntInsnNode(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.BIPUSH:
                return parse_BIPUSH(currentFlow);
            case Opcodes.SIPUSH:
                return parse_SIPUSH(currentFlow);
            case Opcodes.NEWARRAY:
                return parse_NEWARRAY(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_RETURN(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Return value = graph.newReturnNothing();
        graph.registerTranslation(node, new InstructionTranslation(value, currentState.frame));
        currentState.controlFlowsTo(value);
        return Collections.emptyList();
    }

    private List<ControlFlow> parse_RETURNVALUE(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final ReturnValue value = graph.newReturnValue();
        final Frame.PopResult popResult = currentState.frame.popFromStack();
        value.addIncomingData(popResult.value);
        graph.registerTranslation(node, new InstructionTranslation(value, currentState.frame));
        currentState.controlFlowsTo(value);
        return Collections.emptyList();
    }

    private List<ControlFlow> parse_ICONSTX(final ControlFlow currentFlow, final int constant) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final PrimitiveInt value = graph.newIntNode(constant);
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_FCONSTX(final ControlFlow currentFlow, final float constant) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final PrimitiveFloat value = graph.newFloat(constant);
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_LCONSTX(final ControlFlow currentFlow, final long constant) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final PrimitiveLong value = graph.newLong(constant);
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_DCONSTX(final ControlFlow currentFlow, final double constant) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final PrimitiveDouble value = graph.newDouble(constant);
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ACONST_NULL(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final NullReference value = graph.newNullReference();
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_NARYINS(final ControlFlow currentFlow, final Supplier<Value> valueFactory, final int numberArgs) {

        final AbstractInsnNode node = currentFlow.currentNode;

        final List<Value> popped = new ArrayList<>();
        Frame latestFrame = currentFlow.graphParserState.frame;
        for (int i = 0; i < numberArgs; i++) {
            final Frame.PopResult result = latestFrame.popFromStack();
            popped.add(result.value);
            latestFrame = result.newFrame;
        }
        Collections.reverse(popped);

        final Value v = valueFactory.get();
        for (final Value value : popped) {
            v.addIncomingData(value);
        }

        final Variable variable = graph.newVariable(v.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(v);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentFlow.graphParserState.frame));

        final Frame newFrame = latestFrame.pushToStack(variable);

        final GraphParserState newState = currentFlow.graphParserState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_CMP(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop2 = currentState.frame.popFromStack();
        final Frame.PopResult pop1 = pop2.newFrame.popFromStack();

        final CMP cmpNode = graph.newCMP();
        cmpNode.addIncomingData(pop1.value, pop2.value);

        final Variable variable = graph.newVariable(cmpNode.type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(cmpNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_DUP_X1(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult value1 = currentState.frame.popFromStack();
        final Frame.PopResult value2 = value1.newFrame.popFromStack();

        final Variable variable = graph.newVariable(value1.value.type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(value1.value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = value2.newFrame.pushToStack(value1.value).pushToStack(value2.value).pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_DUP_X2(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult value1 = currentState.frame.popFromStack();

        if (value1.value.type.getSize() == 1) {
            final Frame.PopResult value2 = value1.newFrame.popFromStack();
            final Frame.PopResult value3 = value2.newFrame.popFromStack();

            final Variable variable = graph.newVariable(value1.value.type);

            final Copy copy = graph.newCopy();
            copy.addIncomingData(value1.value);
            variable.addIncomingData(copy);
            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            final Frame newFrame = value3.newFrame.pushToStack(value1.value).pushToStack(value3.value).pushToStack(value2.value).pushToStack(variable);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        final Frame.PopResult value2 = value1.newFrame.popFromStack();

        final Variable variable = graph.newVariable(value1.value.type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(value1.value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = value2.newFrame.pushToStack(value1.value).pushToStack(value2.value).pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ATHROW(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();
        final ControlTokenConsumer throwNode = graph.newUnwind();
        throwNode.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(throwNode, currentState.frame));
        return Collections.emptyList();
    }

    private List<ControlFlow> parse_DUP(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Variable dest = graph.newVariable(pop1.value.type);
        final Copy c = graph.newCopy();
        c.addIncomingData(pop1.value);
        dest.addIncomingData(c);

        graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(pop1.value).pushToStack(dest);

        final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_DUP2(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();
        if (pop1.value.type.getSize() == 2) {

            final Variable dest = graph.newVariable(pop1.value.type);
            final Copy c = graph.newCopy();
            c.addIncomingData(pop1.value);
            dest.addIncomingData(c);

            graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

            final Frame newFrame = pop1.newFrame.pushToStack(pop1.value).pushToStack(dest);

            final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        } else {

            final Frame.PopResult pop2 = pop1.newFrame.popFromStack();

            final Variable dest1 = graph.newVariable(pop1.value.type);
            final Variable dest2 = graph.newVariable(pop2.value.type);

            final Copy c = graph.newCopy();
            c.addIncomingData(pop1.value);
            dest1.addIncomingData(c);

            final Copy c2 = graph.newCopy();
            c2.addIncomingData(pop2.value);
            dest2.addIncomingData(c2);

            c.addControlFlowTo(StandardProjections.DEFAULT,c2);

            graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

            final Frame newFrame = pop2.newFrame.pushToStack(pop2.value).pushToStack(pop1.value).pushToStack(dest2).pushToStack(dest1);

            final GraphParserState newState = currentState.controlFlowsTo(c2).withFrame(newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }

    }

    private List<ControlFlow> parse_POP(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Variable dest = graph.newVariable(pop1.value.type);
        final Copy c = graph.newCopy();
        c.addIncomingData(pop1.value);
        dest.addIncomingData(c);

        graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(pop1.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_TYPECONVERSION(final ControlFlow currentFlow, final Type type) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final TypeConversion mulNode = graph.newTypeConversion(type);
        mulNode.addIncomingData(pop1.value);

        final Variable variable = graph.newVariable(type);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(mulNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_XASTORE(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult valuePop = currentState.frame.popFromStack();
        final Frame.PopResult indexPop = valuePop.newFrame.popFromStack();
        final Frame.PopResult arrayPop = indexPop.newFrame.popFromStack();

        final ArrayStore store = graph.newArrayStore();
        store.addIncomingData(arrayPop.value, indexPop.value, valuePop.value);

        graph.registerTranslation(node, new InstructionTranslation(store, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(store).withFrame(arrayPop.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_XALOAD(final ControlFlow currentFlow, final Type type) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult indexPop = currentState.frame.popFromStack();
        final Frame.PopResult arrayPop = indexPop.newFrame.popFromStack();

        final ArrayLoad load = graph.newArrayLoad(arrayPop.value.type);
        load.addIncomingData(arrayPop.value, indexPop.value);

        final Variable var = graph.newVariable(type);
        final Copy c = graph.newCopy();
        c.addIncomingData(load);
        var.addIncomingData(c);

        final Frame newFrame = arrayPop.newFrame.pushToStack(var);

        graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseInsnNode(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.RETURN:
                return parse_RETURN(currentFlow);
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
                return parse_RETURNVALUE(currentFlow);
            case Opcodes.ACONST_NULL:
                return parse_ACONST_NULL(currentFlow);
            case Opcodes.ICONST_M1:
                return parse_ICONSTX(currentFlow, -1);
            case Opcodes.ICONST_0:
                return parse_ICONSTX(currentFlow, 0);
            case Opcodes.ICONST_1:
                return parse_ICONSTX(currentFlow, 1);
            case Opcodes.ICONST_2:
                return parse_ICONSTX(currentFlow, 2);
            case Opcodes.ICONST_3:
                return parse_ICONSTX(currentFlow, 3);
            case Opcodes.ICONST_4:
                return parse_ICONSTX(currentFlow, 4);
            case Opcodes.ICONST_5:
                return parse_ICONSTX(currentFlow, 5);
            case Opcodes.IADD:
                return parse_NARYINS(currentFlow, () -> graph.newAdd(Type.INT_TYPE), 2);
            case Opcodes.LADD:
                return parse_NARYINS(currentFlow, () -> graph.newAdd(Type.LONG_TYPE), 2);
            case Opcodes.DADD:
                return parse_NARYINS(currentFlow, () -> graph.newAdd(Type.DOUBLE_TYPE), 2);
            case Opcodes.FADD:
                return parse_NARYINS(currentFlow, () -> graph.newAdd(Type.FLOAT_TYPE), 2);
            case Opcodes.IDIV:
                return parse_NARYINS(currentFlow, () -> graph.newDiv(Type.INT_TYPE), 2);
            case Opcodes.LDIV:
                return parse_NARYINS(currentFlow, () -> graph.newDiv(Type.LONG_TYPE), 2);
            case Opcodes.FDIV:
                return parse_NARYINS(currentFlow, () -> graph.newDiv(Type.FLOAT_TYPE), 2);
            case Opcodes.DDIV:
                return parse_NARYINS(currentFlow, () -> graph.newDiv(Type.DOUBLE_TYPE), 2);
            case Opcodes.ISUB:
                return parse_NARYINS(currentFlow, () -> graph.newSub(Type.INT_TYPE), 2);
            case Opcodes.LSUB:
                return parse_NARYINS(currentFlow, () -> graph.newSub(Type.LONG_TYPE), 2);
            case Opcodes.FSUB:
                return parse_NARYINS(currentFlow, () -> graph.newSub(Type.FLOAT_TYPE), 2);
            case Opcodes.DSUB:
                return parse_NARYINS(currentFlow, () -> graph.newSub(Type.DOUBLE_TYPE), 2);
            case Opcodes.ATHROW:
                return parse_ATHROW(currentFlow);
            case Opcodes.DUP:
                return parse_DUP(currentFlow);
            case Opcodes.POP:
                return parse_POP(currentFlow);
            case Opcodes.IASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.AASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.BASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.CASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.SASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.LASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.FASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.DASTORE:
                return parse_XASTORE(currentFlow);
            case Opcodes.IALOAD:
                return parse_XALOAD(currentFlow, Type.INT_TYPE);
            case Opcodes.BALOAD:
                return parse_XALOAD(currentFlow, Type.BYTE_TYPE);
            case Opcodes.CALOAD:
                return parse_XALOAD(currentFlow, Type.CHAR_TYPE);
            case Opcodes.AALOAD:
                return parse_XALOAD(currentFlow, Type.getType(Object.class));
            case Opcodes.LALOAD:
                return parse_XALOAD(currentFlow, Type.LONG_TYPE);
            case Opcodes.DALOAD:
                return parse_XALOAD(currentFlow, Type.DOUBLE_TYPE);
            case Opcodes.SALOAD:
                return parse_XALOAD(currentFlow, Type.SHORT_TYPE);
            case Opcodes.FALOAD:
                return parse_XALOAD(currentFlow, Type.FLOAT_TYPE);
            case Opcodes.LUSHR:
                return parse_NARYINS(currentFlow, () -> graph.newUSHR(Type.LONG_TYPE), 2);
            case Opcodes.IUSHR:
                return parse_NARYINS(currentFlow, () -> graph.newUSHR(Type.INT_TYPE), 2);
            case Opcodes.IAND:
                return parse_NARYINS(currentFlow, () -> graph.newAND(Type.INT_TYPE), 2);
            case Opcodes.LAND:
                return parse_NARYINS(currentFlow, () -> graph.newAND(Type.LONG_TYPE), 2);
            case Opcodes.ARRAYLENGTH:
                return parse_NARYINS(currentFlow, graph::newArrayLength, 1);
            case Opcodes.ISHR:
                return parse_NARYINS(currentFlow, () -> graph.newSHR(Type.INT_TYPE), 2);
            case Opcodes.LSHR:
                return parse_NARYINS(currentFlow, () -> graph.newSHR(Type.LONG_TYPE), 2);
            case Opcodes.ISHL:
                return parse_NARYINS(currentFlow, () -> graph.newSHL(Type.INT_TYPE), 2);
            case Opcodes.LSHL:
                return parse_NARYINS(currentFlow, () -> graph.newSHL(Type.LONG_TYPE), 2);
            case Opcodes.INEG:
                return parse_NARYINS(currentFlow, () -> graph.newNEG(Type.INT_TYPE), 1);
            case Opcodes.LNEG:
                return parse_NARYINS(currentFlow, () -> graph.newNEG(Type.LONG_TYPE), 1);
            case Opcodes.FNEG:
                return parse_NARYINS(currentFlow, () -> graph.newNEG(Type.FLOAT_TYPE), 1);
            case Opcodes.DNEG:
                return parse_NARYINS(currentFlow, () -> graph.newNEG(Type.DOUBLE_TYPE), 1);
            case Opcodes.IMUL:
                return parse_NARYINS(currentFlow, () -> graph.newMul(Type.INT_TYPE), 2);
            case Opcodes.LMUL:
                return parse_NARYINS(currentFlow, () -> graph.newMul(Type.LONG_TYPE), 2);
            case Opcodes.DMUL:
                return parse_NARYINS(currentFlow, () -> graph.newMul(Type.DOUBLE_TYPE), 2);
            case Opcodes.FMUL:
                return parse_NARYINS(currentFlow, () -> graph.newMul(Type.FLOAT_TYPE), 2);
            case Opcodes.IREM:
                return parse_NARYINS(currentFlow, () -> graph.newRem(Type.INT_TYPE), 2);
            case Opcodes.LREM:
                return parse_NARYINS(currentFlow, () -> graph.newRem(Type.LONG_TYPE), 2);
            case Opcodes.FREM:
                return parse_NARYINS(currentFlow, () -> graph.newRem(Type.FLOAT_TYPE), 2);
            case Opcodes.DREM:
                return parse_NARYINS(currentFlow, () -> graph.newRem(Type.DOUBLE_TYPE), 2);
            case Opcodes.I2B:
                return parse_TYPECONVERSION(currentFlow, Type.BYTE_TYPE);
            case Opcodes.I2C:
                return parse_TYPECONVERSION(currentFlow, Type.CHAR_TYPE);
            case Opcodes.I2L:
                return parse_TYPECONVERSION(currentFlow, Type.LONG_TYPE);
            case Opcodes.I2F:
                return parse_TYPECONVERSION(currentFlow, Type.FLOAT_TYPE);
            case Opcodes.D2F:
                return parse_TYPECONVERSION(currentFlow, Type.FLOAT_TYPE);
            case Opcodes.D2I:
                return parse_TYPECONVERSION(currentFlow, Type.INT_TYPE);
            case Opcodes.D2L:
                return parse_TYPECONVERSION(currentFlow, Type.LONG_TYPE);
            case Opcodes.I2D:
                return parse_TYPECONVERSION(currentFlow, Type.DOUBLE_TYPE);
            case Opcodes.I2S:
                return parse_TYPECONVERSION(currentFlow, Type.SHORT_TYPE);
            case Opcodes.L2I:
                return parse_TYPECONVERSION(currentFlow, Type.INT_TYPE);
            case Opcodes.L2D:
                return parse_TYPECONVERSION(currentFlow, Type.DOUBLE_TYPE);
            case Opcodes.F2D:
                return parse_TYPECONVERSION(currentFlow, Type.DOUBLE_TYPE);
            case Opcodes.L2F:
                return parse_TYPECONVERSION(currentFlow, Type.FLOAT_TYPE);
            case Opcodes.F2I:
                return parse_TYPECONVERSION(currentFlow, Type.INT_TYPE);
            case Opcodes.IOR:
                return parse_NARYINS(currentFlow, () -> graph.newOR(Type.INT_TYPE), 2);
            case Opcodes.LOR:
                return parse_NARYINS(currentFlow, () -> graph.newOR(Type.LONG_TYPE), 2);
            case Opcodes.LCMP:
                return parse_CMP(currentFlow);
            case Opcodes.IXOR:
                return parse_NARYINS(currentFlow, () -> graph.newXOR(Type.INT_TYPE), 2);
            case Opcodes.LXOR:
                return parse_NARYINS(currentFlow, () -> graph.newXOR(Type.LONG_TYPE), 2);
            case Opcodes.MONITORENTER:
                return parse_MONITORENTER(currentFlow);
            case Opcodes.MONITOREXIT:
                return parse_MONITOREXIT(currentFlow);
            case Opcodes.DUP_X1:
                return parse_DUP_X1(currentFlow);
            case Opcodes.DUP_X2:
                return parse_DUP_X2(currentFlow);
            case Opcodes.FCONST_0:
                return parse_FCONSTX(currentFlow, 0.0f);
            case Opcodes.FCONST_1:
                return parse_FCONSTX(currentFlow, 1.0f);
            case Opcodes.FCONST_2:
                return parse_FCONSTX(currentFlow, 2.0f);
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
                return parse_CMP(currentFlow);
            case Opcodes.LCONST_0:
                return parse_LCONSTX(currentFlow, 0L);
            case Opcodes.LCONST_1:
                return parse_LCONSTX(currentFlow, 1L);
            case Opcodes.DUP2:
                return parse_DUP2(currentFlow);
            case Opcodes.DCONST_0:
                return parse_DCONSTX(currentFlow, 0d);
            case Opcodes.DCONST_1:
                return parse_DCONSTX(currentFlow, 1d);
            case Opcodes.DCMPG:
            case Opcodes.DCMPL:
                return parse_CMP(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parseFrame(final ControlFlow currentFlow) {
        final FrameNode node = (FrameNode) currentFlow.currentNode;
        if (node.stack.size() != currentFlow.graphParserState.frame.incomingStack.length) {
            throw new AnalysisException(new RuntimeException("Parser stack does not match with compiled bytecode stack! Expected " + node.stack.size() + " != actual " + currentFlow.graphParserState.frame.incomingStack.length), analysisStack);
        }

        analysisStack.addDebugMessage("Check of stack size is ok");

        final GraphParserState currentState = currentFlow.graphParserState;
        final FrameDebugInfo n = graph.newFrameDebugInfo();

        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(n);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_GOTO(final ControlFlow currentFlow) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Goto gotoNode = graph.newGoto();

        graph.registerTranslation(node, new InstructionTranslation(gotoNode, currentState.frame));

        final Region target = graph.regionByLabel(node.label.getLabel().toString());
        graph.addFixup(new ControlFlowFixup(node, currentState.frame, StandardProjections.DEFAULT, node.label));
        return Collections.singletonList(currentFlow.continueWith(node.label, currentState.controlFlowsTo(target)));
    }

    private List<ControlFlow> parse_IF_TWOARGS(final ControlFlow currentFlow, final Supplier<Test> testSupplier) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop2 = currentState.frame.popFromStack();
        final Frame.PopResult pop1 = pop2.newFrame.popFromStack();

        final If ifNode = graph.newIf();
        graph.registerTranslation(node, new InstructionTranslation(ifNode, currentState.frame));

        final Test numericalTest = testSupplier.get();
        numericalTest.addIncomingData(pop1.value, pop2.value);

        ifNode.addIncomingData(numericalTest);

        final List<ControlFlow> results = new ArrayList<>();

        final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop1.newFrame);

        // True-Case
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.TRUE, node.label));
        results.add(currentFlow.continueWith(node.label, origin));

        // False-Case
        final AbstractInsnNode nextNode = node.getNext();
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.FALSE, nextNode));
        results.add(currentFlow.continueWith(nextNode, origin));

        return results;
    }

    private List<ControlFlow> parse_IF_ONEARG(final ControlFlow currentFlow, final Supplier<Test> testSupplier) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final If ifNode = graph.newIf();

        final Test t = testSupplier.get();
        t.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(ifNode, currentState.frame));
        ifNode.addIncomingData(t);

        final List<ControlFlow> results = new ArrayList<>();

        final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop1.newFrame);

        // True-Case
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.TRUE, node.label));
        results.add(currentFlow.continueWith(node.label, origin));

        // False-Case
        final AbstractInsnNode nextNode = node.getNext();
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.FALSE, nextNode));
        results.add(currentFlow.continueWith(nextNode, origin));

        return results;
    }

    private List<ControlFlow> parse_ZEROIF(final ControlFlow currentFlow) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final NumericalTest.Operation compareOperation;
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.IFEQ:
                compareOperation = NumericalTest.Operation.EQ;
                break;
            case Opcodes.IFNE:
                compareOperation = NumericalTest.Operation.NE;
                break;
            case Opcodes.IFLT:
                compareOperation = NumericalTest.Operation.LT;
                break;
            case Opcodes.IFGE:
                compareOperation = NumericalTest.Operation.GE;
                break;
            case Opcodes.IFGT:
                compareOperation = NumericalTest.Operation.GT;
                break;
            case Opcodes.IFLE:
                compareOperation = NumericalTest.Operation.LE;
                break;
            default:
                throw new IllegalStateException("Not implemented : " + currentFlow.currentNode.getOpcode());
        }

        final If ifNode = graph.newIf();
        graph.registerTranslation(node, new InstructionTranslation(ifNode, currentState.frame));

        final NumericalTest numericalTest = graph.newNumericalTest(compareOperation);
        numericalTest.addIncomingData(pop1.value, graph.newIntNode(0));

        ifNode.addIncomingData(numericalTest);

        final List<ControlFlow> results = new ArrayList<>();

        final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop1.newFrame);

        // True-Case
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.TRUE, node.label));
        results.add(currentFlow.continueWith(node.label, origin));

        // False-Case
        final AbstractInsnNode nextNode = node.getNext();
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.FALSE, nextNode));
        results.add(currentFlow.continueWith(nextNode, origin));

        return results;
    }

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.GOTO:
                return parse_GOTO(currentFlow);
            case Opcodes.IF_ICMPEQ:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.EQ));
            case Opcodes.IF_ICMPNE:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.NE));
            case Opcodes.IF_ICMPLT:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.LT));
            case Opcodes.IF_ICMPGE:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.GE));
            case Opcodes.IF_ICMPGT:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.GT));
            case Opcodes.IF_ICMPLE:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newNumericalTest(NumericalTest.Operation.LE));
            case Opcodes.IFNONNULL:
                return parse_IF_ONEARG(currentFlow, () -> graph.newNullTest(NullTest.Operation.NOTNULL));
            case Opcodes.IFNULL:
                return parse_IF_ONEARG(currentFlow, () -> graph.newNullTest(NullTest.Operation.NULL));
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
                return parse_ZEROIF(currentFlow);
            case Opcodes.IF_ACMPEQ:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newReferenceTest(ReferenceTest.Operation.EQ));
            case Opcodes.IF_ACMPNE:
                return parse_IF_TWOARGS(currentFlow, () -> graph.newReferenceTest(ReferenceTest.Operation.EQ));
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parseIincInsnNode(final ControlFlow currentFlow) {
        final IincInsnNode node = (IincInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final int local = node.var;
        final Node currentValue = currentState.frame.incomingLocals[local];
        final Node amountNode = graph.newIntNode(node.incr);

        final Node addNode = graph.newAdd(Type.INT_TYPE);
        addNode.addIncomingData(currentValue, amountNode);

        final Variable variable = graph.newVariable(Type.INT_TYPE);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(addNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.setLocal(local, variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_NEW(final ControlFlow currentFlow) {
        final TypeInsnNode node = (TypeInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type type = Type.getObjectType(node.desc);

        final TypeReference typeReference = graph.newTypeReference(type);
        final New n = graph.newNew(type);
        n.addIncomingData(typeReference);

        final Variable variable = graph.newVariable(type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(n);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_INSTANCEOF(final ControlFlow currentFlow) {
        final TypeInsnNode node = (TypeInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Type type = Type.getObjectType(node.desc);

        final TypeReference typeReference = graph.newTypeReference(type);
        final InstanceOf n = graph.newInstanceOf();
        n.addIncomingData(typeReference);

        final Variable variable = graph.newVariable(n.type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(n);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_CHECKCAST(final ControlFlow currentFlow) {
        final TypeInsnNode node = (TypeInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Type type = Type.getObjectType(node.desc);

        final TypeReference typeReference = graph.newTypeReference(type);
        final CheckCast n = graph.newCheckCast();
        n.addIncomingData(typeReference);
        n.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(n).withFrame(currentState.frame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_MONITORENTER(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final MonitorEnter n = graph.newMonitorEnter();
        n.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(n).withFrame(pop1.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_MONITOREXIT(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final MonitorExit n = graph.newMonitorExit();
        n.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(n).withFrame(pop1.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseTypeInsnNode(final ControlFlow currentFlow) {
        final TypeInsnNode node = (TypeInsnNode) currentFlow.currentNode;
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.NEW:
                return parse_NEW(currentFlow);
            case Opcodes.ANEWARRAY:
                return parse_NARYINS(currentFlow, () -> graph.newNewArray(Type.getObjectType(node.desc)), 1);
            case Opcodes.INSTANCEOF:
                return parse_INSTANCEOF(currentFlow);
            case Opcodes.CHECKCAST:
                return parse_CHECKCAST(currentFlow);
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parse_GETFIELD(final ControlFlow currentFlow) {
        final FieldInsnNode node = (FieldInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type t = Type.getType(node.desc);
        final ResolvedClass targetClass = compileUnit.resolveClass(Type.getObjectType(node.owner), analysisStack);
        final ResolvedField resolvedField = targetClass.resolveField(node.name, t);

        final Frame.PopResult reference = currentState.frame.popFromStack();

        final ReadInstanceField field = graph.newInstanceFieldExpression(t, resolvedField);
        final Variable target = graph.newVariable(t);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(field);
        target.addIncomingData(copy);

        field.addIncomingData(reference.value);

        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(reference.newFrame.pushToStack(target));
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_GETSTATICFIELD(final ControlFlow currentFlow) {
        final FieldInsnNode node = (FieldInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type t = Type.getType(node.desc);
        final ResolvedClass targetClass = compileUnit.resolveClass(Type.getObjectType(node.owner), analysisStack);
        final ResolvedField resolvedField = targetClass.resolveField(node.name, t);

        final ReadClassField field = graph.newClassFieldExpression(t, resolvedField);
        field.addIncomingData(graph.newTypeReference(targetClass.type));
        final Variable target = graph.newVariable(t);
        final Copy copy = graph.newCopy();
        copy.addIncomingData(field);
        target.addIncomingData(copy);

        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(currentState.frame.pushToStack(target));
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }


    private List<ControlFlow> parse_PUTFIELD(final ControlFlow currentFlow) {
        final FieldInsnNode node = (FieldInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type t = Type.getType(node.desc);
        final ResolvedClass targetClass = compileUnit.resolveClass(Type.getObjectType(node.owner), analysisStack);
        final ResolvedField resolvedField = targetClass.resolveField(node.name, t);

        final Frame.PopResult valuePop = currentState.frame.popFromStack();
        final Frame.PopResult referencePop = valuePop.newFrame.popFromStack();

        final SetInstanceField setfield = graph.newSetInstanceField(resolvedField);
        setfield.addIncomingData(valuePop.value);
        referencePop.value.addIncomingData(setfield);

        graph.registerTranslation(node, new InstructionTranslation(setfield, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(setfield).withFrame(referencePop.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_PUTSTATICFIELD(final ControlFlow currentFlow) {
        final FieldInsnNode node = (FieldInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type t = Type.getType(node.desc);
        final ResolvedClass targetClass = compileUnit.resolveClass(Type.getObjectType(node.owner), analysisStack);
        final ResolvedField resolvedField = targetClass.resolveField(node.name, t);

        final Frame.PopResult valuePop = currentState.frame.popFromStack();

        final SetClassField setfield = graph.newSetClassField(resolvedField);

        setfield.addIncomingData(valuePop.value);
        graph.newTypeReference(targetClass.type).addIncomingData(setfield);

        graph.registerTranslation(node, new InstructionTranslation(setfield, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(setfield).withFrame(valuePop.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseFieldInsnNode(final ControlFlow currentFlow) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.GETFIELD:
                return parse_GETFIELD(currentFlow);
            case Opcodes.GETSTATIC:
                return parse_GETSTATICFIELD(currentFlow);
            case Opcodes.PUTFIELD:
                return parse_PUTFIELD(currentFlow);
            case Opcodes.PUTSTATIC:
                return parse_PUTSTATICFIELD(currentFlow);
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parse_LDC(final ControlFlow currentFlow) {
        final LdcInsnNode node = (LdcInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Value source;
        if (node.cst instanceof Integer) {
            source = graph.newObjectInteger((Integer) node.cst);
        } else if (node.cst instanceof Float) {
            source = graph.newObjectFloat((Float) node.cst);
        } else if (node.cst instanceof Long) {
            source = graph.newObjectLong((Long) node.cst);
        } else if (node.cst instanceof Double) {
            source = graph.newObjectDouble((Double) node.cst);
        } else if (node.cst instanceof String) {
            source = graph.newObjectString((String) node.cst);
        } else if (node.cst instanceof Type) {
            source = graph.newTypeReference((Type) node.cst);
        } else {
            throw new IllegalStateException("Unsupported constant : " + node.cst);
        }

        final Variable variable = graph.newVariable(source.type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(source);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseTableSwitchInsnNode(final ControlFlow currentFlow) {
        final TableSwitchInsnNode node = (TableSwitchInsnNode) currentFlow.currentNode;

        final Frame.PopResult valuePop = currentFlow.graphParserState.frame.popFromStack();

        final TableSwitch sw = graph.newTableSwitch(node.min, node.max);
        sw.addIncomingData(valuePop.value);

        graph.registerTranslation(node, new InstructionTranslation(sw, currentFlow.graphParserState.frame));

        final List<ControlFlow> continueWith = new ArrayList<>();

        final GraphParserState newState = currentFlow.graphParserState.controlFlowsTo(sw).withFrame(valuePop.newFrame);

        continueWith.add(currentFlow.continueWith(node.dflt, newState));
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.dflt));

        for (int i = 0; i < node.labels.size(); i++) {
            final AbstractInsnNode target = node.labels.get(i);
            continueWith.add(currentFlow.continueWith(target, newState));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, new Projection.IndexedProjection(EdgeType.FORWARD, i), target));
        }

        return continueWith;
    }

    private List<ControlFlow> parseLookupSwitchInsnNode(final ControlFlow currentFlow) {
        final LookupSwitchInsnNode node = (LookupSwitchInsnNode) currentFlow.currentNode;

        final Frame.PopResult valuePop = currentFlow.graphParserState.frame.popFromStack();

        final LookupSwitch sw = graph.newLookupSwitch();
        sw.addIncomingData(valuePop.value);

        graph.registerTranslation(node, new InstructionTranslation(sw, currentFlow.graphParserState.frame));

        final GraphParserState newState = currentFlow.graphParserState.controlFlowsTo(sw).withFrame(valuePop.newFrame);

        final List<ControlFlow> continueWith = new ArrayList<>();

        for (int i = 0; i < node.labels.size(); i++) {
            final AbstractInsnNode target = node.labels.get(i);
            final Integer key = node.keys.get(i);

            continueWith.add(currentFlow.continueWith(target, newState));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, new Projection.KeyedProjection(EdgeType.FORWARD, key), target));
        }

        return continueWith;
    }

    private List<ControlFlow> parseLdcInsnNode(final ControlFlow currentFlow) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.LDC:
                return parse_LDC(currentFlow);
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parseInvokeDynamicInsnNode(final ControlFlow currentFlow) {
        final InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Handle h = node.bsm;

        switch (h.getTag()) {
            case Opcodes.H_INVOKESTATIC:
                break;
            default:
                throw new IllegalStateException("Not supported method handle tag : " + h.getTag());
        }

        final ResolvedClass bsmType = compileUnit.resolveClass(Type.getObjectType(h.getOwner()), analysisStack);
        final ResolvedMethod bsmMethod = bsmType.resolveMethod(h.getName(), Type.getMethodType(h.getDesc()), analysisStack);

        final ResolveCallsite resolveCallsite = graph.newResolveCallsite();

        final Type invokeDynamicDesc = Type.getMethodType(node.desc);
        resolveCallsite.addIncomingData(graph.newMethodReference(bsmMethod), graph.newObjectString(node.name), graph.newMethodType(invokeDynamicDesc));
        for (final Object bsmArg : node.bsmArgs) {
            if (bsmArg instanceof Handle) {
                final Handle x = (Handle) bsmArg;

                final ResolvedClass argType = compileUnit.resolveClass(Type.getObjectType(x.getOwner()), analysisStack);
                final ResolvedMethod argMethod = argType.resolveMethod(x.getName(), Type.getMethodType(x.getDesc()), analysisStack);

                resolveCallsite.addIncomingData(graph.newMethodReference(argMethod));
            } else if (bsmArg instanceof Type) {
                resolveCallsite.addIncomingData(graph.newMethodType((Type) bsmArg));
            } else {
                throw new IllegalArgumentException("Not supported bootstrap method argument type : " + bsmArg);
            }
        }

        final InvokeDynamicExpression invokeDynamic = graph.newInvokeDynamicExpression(invokeDynamicDesc.getReturnType());
        invokeDynamic.addIncomingData(resolveCallsite);

        final Node[] arguments = new Node[invokeDynamicDesc.getArgumentTypes().length];
        Frame latest = currentState.frame;
        for (int i = 0; i < arguments.length; i++) {
            final Frame.PopResult pr = latest.popFromStack();
            latest = pr.newFrame;
            arguments[arguments.length - 1 - i] = pr.value;
        }

        invokeDynamic.addIncomingData(arguments);

        resolveCallsite.addIncomingData();

        final Variable variable = graph.newVariable(invokeDynamic.type);

        final Copy copy = graph.newCopy();
        copy.addIncomingData(invokeDynamic);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = latest.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseMultiANewArrayInsnNode(final ControlFlow currentFlow) {
        final MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) currentFlow.currentNode;
        return parse_NARYINS(currentFlow, () -> graph.neNewMultiArray(Type.getObjectType(node.desc)), node.dims);
    }

    private List<ControlFlow> parse(final ControlFlow currentFlow, final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction) {
        if (currentFlow.currentNode instanceof LabelNode) {
            final LabelNode labelNode = (LabelNode) currentFlow.currentNode;

            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerInstruction.get(labelNode);
            if (incomingEdges != null) {
                if (incomingEdges.size() > 1) {
                    // We do have multiple incoming edges
                    // Now we have to make sure that every entry on the stack or on local variables
                    // is a PHI. Data copy is handled during the graph fixup phase
                    // We just have to make sure that there are phi variables there to handle everything

                    final GraphParserState graphParserState = currentFlow.graphParserState;
                    final Value[] newLocals = new Value[graphParserState.frame.incomingLocals.length];
                    final Value[] newStack = new Value[graphParserState.frame.incomingStack.length];

                    for (int i = 0; i < graphParserState.frame.incomingLocals.length; i++) {
                        final Value source = graphParserState.frame.incomingLocals[i];
                        if (source != null && !(source instanceof PHI)) {
                            newLocals[i] = graph.newPHI(source.type);
                        } else {
                            newLocals[i] = source;
                        }
                    }
                    for (int i = 0; i < graphParserState.frame.incomingStack.length; i++) {
                        final Value source = graphParserState.frame.incomingStack[i];
                        if (source != null && !(source instanceof PHI)) {
                            newStack[i] = graph.newPHI(source.type);
                        } else {
                            newStack[i] = source;
                        }
                    }

                    final Frame newFrame = graphParserState.frame.withLocalsAndStack(newLocals, newStack);

                    final GraphParserState newState = graphParserState.withFrame(newFrame);
                    final ControlFlow mergedFlow = currentFlow.continueWith(newState);
                    return parseLabelNode(mergedFlow);
                }
            }

            return parseLabelNode(currentFlow);
        } else if (currentFlow.currentNode instanceof LineNumberNode) {
            return parseLineNumberNode(currentFlow);
        } else if (currentFlow.currentNode instanceof VarInsnNode) {
            return parseVarInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof MethodInsnNode) {
            return parseMethodInsNode(currentFlow);
        } else if (currentFlow.currentNode instanceof IntInsnNode) {
            return parseIntInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof InsnNode) {
            return parseInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof FrameNode) {
            return parseFrame(currentFlow);
        } else if (currentFlow.currentNode instanceof JumpInsnNode) {
            return parseJumpInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof IincInsnNode) {
            return parseIincInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof TypeInsnNode) {
            return parseTypeInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof FieldInsnNode) {
            return parseFieldInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof LdcInsnNode) {
            return parseLdcInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof TableSwitchInsnNode) {
            return parseTableSwitchInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof LookupSwitchInsnNode) {
            return parseLookupSwitchInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof InvokeDynamicInsnNode) {
            return parseInvokeDynamicInsnNode(currentFlow);
        } else if (currentFlow.currentNode instanceof MultiANewArrayInsnNode) {
            return parseMultiANewArrayInsnNode(currentFlow);
        }
        throw new IllegalStateException("Not implemented : " + currentFlow.currentNode + " -> " + currentFlow.currentNode.getOpcode());
    }

    public Graph graph() {
        return graph;
    }
}
