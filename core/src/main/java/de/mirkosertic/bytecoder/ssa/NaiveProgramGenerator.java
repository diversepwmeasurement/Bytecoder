/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.java.lang.TObject;
import de.mirkosertic.bytecoder.classlib.java.lang.invoke.TMethodHandle;
import de.mirkosertic.bytecoder.classlib.java.lang.invoke.TRuntimeGeneratedType;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethod;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethodsAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionANEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARRAYLENGTH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionD2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2X1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionF2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericAND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericNEG;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericREM;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericUSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericXOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFACMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIINC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEDYNAMIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionInvoke;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITORENTER;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITOREXIT;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRET;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeInvokeDynamicConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodHandleConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeMethodTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeReferenceIndex;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.optimizer.AllOptimizer;

public class NaiveProgramGenerator implements ProgramGenerator {

    public final static ProgramGeneratorFactory FACTORY = new ProgramGeneratorFactory() {
        @Override
        public ProgramGenerator createFor(BytecodeLinkerContext aLinkerContext) {
            return new NaiveProgramGenerator(aLinkerContext);
        }
    };

    private static class ParsingHelper {

        private final GraphNode block;
        private final Stack<Value> stack;
        private final Map<Integer, Value> localVariables;

        public ParsingHelper(GraphNode aBlock) {
            stack = new Stack();
            block = aBlock;
            localVariables = new HashMap<>();
        }

        public ParsingHelper cloneToNewFor(GraphNode aBlock) {
            ParsingHelper theNew = new ParsingHelper(aBlock);
            for (Map.Entry<Integer, Value> theEntry : localVariables.entrySet()) {
                theNew.localVariables.put(theEntry.getKey(), theEntry.getValue());
                aBlock.addToImportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
                aBlock.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
            }
            for (int i=0;i<stack.size();i++) {
                theNew.stack.push(stack.get(i));
            }
            return theNew;
        }

        public ParsingHelper cloneToNewWithPHIFunctions(GraphNode aBlock, Program aProgram) {
            ParsingHelper theNew = new ParsingHelper(aBlock);
            for (Map.Entry<Integer, Value> theEntry : localVariables.entrySet()) {
                Variable theVar = aProgram.createVariable(theEntry.getValue().resolveType());
                aBlock.addToImportedList(theVar, new LocalVariableDescription(theEntry.getKey()));
                aBlock.addToExportedList(theVar, new LocalVariableDescription(theEntry.getKey()));
                theNew.localVariables.put(theEntry.getKey(), theVar);
            }
            for (int i=stack.size() - 1 ; i>= 0; i--) {
                Variable theVar = aProgram.createVariable(stack.get(stack.size() - 1 - i).resolveType());
                aBlock.addToImportedList(theVar, new StackVariableDescription(i));
                theNew.stack.push(theVar);
            }
            return theNew;
        }

        public Value pop() {
            return stack.pop();
        }

        public Value peek() {
            return stack.peek();
        }

        public void push(Value aValue) {
            if (aValue == null) {
                throw new IllegalStateException("Trying to push null in " + this);
            }
            stack.push(aValue);
        }

        public Value getLocalVariable(int aIndex) {
            Value theValue = localVariables.get(aIndex);
            if (theValue == null) {
                VariableDescription theDesc = new LocalVariableDescription(aIndex);
                theValue = block.newImportedVariable(TypeRef.Native.UNKNOWN, new UnknownValue(),
                        theDesc);
                localVariables.put(aIndex, theValue);
                block.addToExportedList(theValue, theDesc);
                return theValue;
            }
            if (theValue == null) {
                throw new IllegalStateException("local variable " + aIndex + " is null in " + this);
            }
            return theValue;
        }

        public void setLocalVariable(int aIndex, Value aValue) {
            if (aValue == null) {
                throw new IllegalStateException("local variable " + aIndex + " must not be null in " + this);
            }
            localVariables.put(aIndex, aValue);
            block.addToExportedList(aValue, new LocalVariableDescription(aIndex));
        }

        public void finalizeExportState() {
            for (Map.Entry<Integer, Value> theEntry : localVariables.entrySet()) {
                block.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
            }
            for (int i=stack.size() - 1 ; i>= 0; i--) {
                // Numbering must be consistent here!!
                block.addToExportedList(stack.get(i), new StackVariableDescription(stack.size() - 1 - i));
            }
        }

        public void removeVariable(Variable aVariable) {
            block.removeVariable(aVariable);
        }
    }

    private final BytecodeLinkerContext linkerContext;

    public NaiveProgramGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    @Override
    public Program generateFrom(BytecodeClass aOwningClass, BytecodeMethod aMethod) {

        BytecodeCodeAttributeInfo theCode = aMethod.getCode(aOwningClass);
        BytecodeProgram theBytecode = theCode.getProgramm();

        Program theProgram = new Program();

        List<BytecodeBasicBlock> theBlocks = new ArrayList<>();
        Function<BytecodeOpcodeAddress, BytecodeBasicBlock> theBasicBlockByAddress = aValue -> {
            for (BytecodeBasicBlock theBlock : theBlocks) {
                if (aValue.equals(theBlock.getStartAddress())) {
                    return theBlock;
                }
            }
            throw new IllegalStateException("No Block for " + aValue);
        };

        Set<BytecodeOpcodeAddress> theJumpTarget = theBytecode.getJumpTargets();
        BytecodeBasicBlock currentBlock = null;
        for (BytecodeInstruction theInstruction : theBytecode.getInstructions()) {
            if (theJumpTarget.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                currentBlock = null;
            }
            if (theBytecode.isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                currentBlock = null;
            }
            if (currentBlock == null) {
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (BytecodeExceptionTableEntry theHandler : theBytecode.getExceptionHandlers()) {
                    if (theHandler.getHandlerPc().equals(theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                        }
                    }
                }
                BytecodeBasicBlock theCurrentTemp = currentBlock;
                currentBlock = new BytecodeBasicBlock(theType);
                if (theCurrentTemp != null && !theCurrentTemp.endsWithReturn() && !theCurrentTemp.endsWithThrow() && theCurrentTemp.endsWithGoto() && !theCurrentTemp.endsWithConditionalJump()) {
                    theCurrentTemp.addSuccessor(currentBlock);
                }
                theBlocks.add(currentBlock);
            }
            currentBlock.addInstruction(theInstruction);
            if (theInstruction.isJumpSource()) {
                // conditional or unconditional jump, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRET) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                // thowing an exception, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionInvoke) {
                // invocation, start new basic block
                // currentBlock = null;
            }
        }

        // Now, we have to build the successors of each block
        for (int i=0;i<theBlocks.size();i++) {
            BytecodeBasicBlock theBlock = theBlocks.get(i);
            if (!theBlock.endsWithReturn() && !theBlock.endsWithThrow()) {
                if (theBlock.endsWithJump()) {
                    for (BytecodeInstruction theInstruction : theBlock.getInstructions()) {
                        if (theInstruction.isJumpSource()) {
                            for (BytecodeOpcodeAddress theBlockJumpTarget : theInstruction.getPotentialJumpTargets()) {
                                theBlock.addSuccessor(theBasicBlockByAddress.apply(theBlockJumpTarget));
                            }
                        }
                    }
                    if (theBlock.endsWithConditionalJump()) {
                        if (i<theBlocks.size()-1) {
                            theBlock.addSuccessor(theBlocks.get(i + 1));
                        } else {
                            throw new IllegalStateException("Block at end with no jump target!");
                        }
                    }
                } else {
                    if (i<theBlocks.size()-1) {
                        theBlock.addSuccessor(theBlocks.get(i + 1));
                    } else {
                        throw new IllegalStateException("Block at end with no jump target!");
                    }
                }
            }
        }

        // Ok, now we transform it to GraphNodes with yet empty content
        Map<BytecodeBasicBlock, GraphNode> theCreatedBlocks = new HashMap<>();

        ControlFlowGraph theGraph = theProgram.getControlFlowGraph();
        for (BytecodeBasicBlock theBlock : theBlocks) {
            GraphNode theSingleAssignmentBlock;
            switch (theBlock.getType()) {
            case NORMAL:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.NORMAL);
                break;
            case EXCEPTION_HANDLER:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.EXCEPTION_HANDLER);
                break;
            case FINALLY:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.FINALLY);
                break;
            default:
                throw new IllegalStateException("Unsupported block type : " + theBlock.getType());
            }
            theCreatedBlocks.put(theBlock, theSingleAssignmentBlock);
        }

        // Initialize Block dependency graph
        for (Map.Entry<BytecodeBasicBlock, GraphNode> theEntry : theCreatedBlocks.entrySet()) {
            for (BytecodeBasicBlock theSuccessor : theEntry.getKey().getSuccessors()) {
                GraphNode theSuccessorBlock = theCreatedBlocks.get(theSuccessor);
                if (theSuccessorBlock == null) {
                    throw new IllegalStateException("Cannot find successor block");
                }
                theEntry.getValue().addSuccessor(theSuccessorBlock);
            }
        }

        // Now we can add the SSA instructions to the graph nodes
        Set<GraphNode> theVisited = new HashSet<>();
        GraphNode theStart = theProgram.getControlFlowGraph().nodeStartingAt(new BytecodeOpcodeAddress(0));

        ParsingHelper theHelper = new ParsingHelper(theStart);

        // At this point, local variables are initialized based on the method signature
        // The stack is empty
        int theCurrentIndex = 0;
        if (!aMethod.getAccessFlags().isStatic()) {
            Variable theRef = Variable.createThisRef();
            theHelper.setLocalVariable(theCurrentIndex++, theRef);
        }
        BytecodeTypeRef[] theTypes = aMethod.getSignature().getArguments();
        for (int i=0;i<theTypes.length;i++) {
            BytecodeTypeRef theRef = theTypes[i];

            Variable theArgument = Variable.createMethodParameter((i+1), TypeRef.toType(theTypes[i]));
            theHelper.setLocalVariable(theCurrentIndex++, theArgument);

            if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                theCurrentIndex++;
            }
        }

        // This will traverse the CFG from top to bottom
        initializeBlock(aOwningClass, aMethod, theProgram, theStart, theVisited, theHelper, theBasicBlockByAddress);

        // Finally, we have to check for blocks what were not directly accessible, for instance exception handlers or
        // finally blocks
        for (Map.Entry<BytecodeBasicBlock, GraphNode> theEntry : theCreatedBlocks.entrySet()) {
            GraphNode theNewBlock = theEntry.getValue();
            if (theVisited.add(theNewBlock)) {
                ParsingHelper theNewHelper = new ParsingHelper(theNewBlock);
                if (theNewBlock.getType() != GraphNode.BlockType.NORMAL) {
                    theHelper.push(theNewBlock.newVariable(TypeRef.Native.REFERENCE, new CurrentExceptionValue()));
                }
                // TODO: Check what exception handler might reference from here!!!
                initializeBlock(aOwningClass, aMethod, theProgram, theNewBlock, theVisited, theNewHelper, theBasicBlockByAddress);
            }
        }

        // Check for blocks with an conditional jump at the end and the successor has PHI functions
        // For these cases we have resolve the PHIs
        for (Map.Entry<BytecodeBasicBlock, GraphNode> theEntry : theCreatedBlocks.entrySet()) {
            GraphNode theBlock = theEntry.getValue();
            if (!theBlock.endWithNeverReturningExpression() && !theBlock.getSuccessors().isEmpty()) {
                // This is a block
                BlockState theFinalState = theBlock.toFinalState();

                BytecodeBasicBlock theNext = theBlocks.get(theBlocks.indexOf(theEntry.getKey()) + 1);
                GraphNode theDirectSuccessor = theCreatedBlocks.get(theNext);

                BlockState theImportingState = theDirectSuccessor.toStartState();

                theBlock.getExpressions().add(new CommentExpression("Code to jump to " + theDirectSuccessor.getStartAddress().getAddress()));

                for (Map.Entry<VariableDescription, Value> theImport : theImportingState.getPorts().entrySet()) {
                    Value theValue = theImport.getValue();
                    if (!(theValue instanceof Variable)) {
                        continue;
                    }
                    Variable theVariable = (Variable) theValue;

                    // We found one, we need to resolve a variable from the final state that satisfied the required constraints
                    Value theFinalValue = theFinalState.findBySlot(theImport.getKey());
                    Value theNewValue = null;
                    if (theFinalValue == null) {
                        // Variable is not present, hence we assume it is null
                        theNewValue = new NullValue();
                    } else {
                        theNewValue = theFinalValue;
                    }

                    theVariable.initializeWith(theNewValue);

                    InitVariableExpression theExpression = new InitVariableExpression(theVariable, theNewValue);
                    theBlock.getExpressions().add(new CommentExpression("Resolving " + theImport.getKey()));
                    theBlock.getExpressions().add(theExpression);

                }

                theBlock.getExpressions().add(new GotoExpression(theDirectSuccessor.getStartAddress(), theBlock));
            }
        }

        // Insert jumpcode and perform node inlining
        for (GraphNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
            processGotosIn(theProgram.getControlFlowGraph(), theNode, theNode.getExpressions());
        }

        // Check if there are infinite looping blocks
        for (GraphNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
            ExpressionList theCurrentList = theNode.getExpressions();
            Expression theLast = theCurrentList.lastExpression();
            while (theLast instanceof InlinedNodeExpression) {
                InlinedNodeExpression theInlined = (InlinedNodeExpression) theLast;
                theCurrentList = theInlined.getNode().getExpressions();
                theLast = theCurrentList.lastExpression();
            }
            if (theLast instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theLast;
                if (theGoto.getJumpTarget().equals(theNode.getStartAddress())) {
                    theCurrentList.remove(theGoto);
                    theNode.markAsInfiniteLoop();
                }
            }
        }

        AllOptimizer theOptimizer = new AllOptimizer();
        theOptimizer.optimize(theProgram.getControlFlowGraph(), linkerContext);

        return theProgram;
    }

    private void insertCodeToJumpFrom(GraphNode aSource, GraphNode aTarget, Expression aExpressionToInsertBefore, ExpressionList aExpressionList) {
        BlockState theImportingState = aTarget.toStartState();

        Expression theComment = new CommentExpression("Code to jump to " + aTarget.getStartAddress().getAddress());
        aExpressionList.addBefore(theComment, aExpressionToInsertBefore);

        BlockState theFinalState = aSource.toFinalState();

        for (Map.Entry<VariableDescription, Value> theImport : theImportingState.getPorts().entrySet()) {
            Value theValue = theImport.getValue();
            if (!(theValue instanceof Variable)) {
                continue;
            }
            Variable theVariable = (Variable) theValue;;

            // We found one, we need to resolve a variable from the final state that satisfied the required constraints
            Value theFinalValue = theFinalState.findBySlot(theImport.getKey());
            Value theNewValue;
            if (theFinalValue == null) {
                // Variable is not present, hence we assume it is null
                theNewValue = new NullValue();
            } else {
                theNewValue = theFinalValue;
            }

            InitVariableExpression theExpression = new InitVariableExpression(theVariable, theNewValue);
            aExpressionList.addBefore(new CommentExpression("Resolving " + theImport.getKey()), aExpressionToInsertBefore);
            aExpressionList.addBefore(theExpression, aExpressionToInsertBefore);
        }
    }

    private void processGotosIn(ControlFlowGraph aGraph, GraphNode aNode, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    processGotosIn(aGraph, aNode, theList);
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGOTO = (GotoExpression) theExpression;
                GraphNode theTargetNode = aGraph.nodeStartingAt(theGOTO.getJumpTarget());

                if (theTargetNode.isStrictlyDominatedBy(aNode)) {
                    // Node can be inlined
                    InlinedNodeExpression theInlined = new InlinedNodeExpression(theTargetNode);
                    aList.replace(theGOTO, theInlined);
                    aGraph.removeDominatedNode(theTargetNode);
                } else {
                    insertCodeToJumpFrom(aNode, theTargetNode, theGOTO, aList);
                }
            }
        }
    }

    private void initializeBlock(BytecodeClass aOwningClass, BytecodeMethod aMethod, Program aProgram, GraphNode aCurrentBlock, Set<GraphNode> aAlreadyVisited, ParsingHelper aHelper, Function<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocksByAddress) {
        if (aAlreadyVisited.add(aCurrentBlock)) {

            initializeBlockWith(aOwningClass, aMethod, aCurrentBlock, aBlocksByAddress, aHelper);

            for (GraphNode theSuccessor : aCurrentBlock.getSuccessors()) {
                if (!aAlreadyVisited.contains(theSuccessor)) {
                    ParsingHelper theNewHelper;
                    if (theSuccessor.getPredecessors().size() == 1) {
                        // Everything is ok
                        theNewHelper = aHelper.cloneToNewFor(theSuccessor);
                    } else {
                        // We have a join point, so we have to introduce PHI function for everything that was imported
                        // This is the naive implementation that can be vastly improved
                        theNewHelper = aHelper.cloneToNewWithPHIFunctions(theSuccessor, aProgram);
                    }

                    initializeBlock(aOwningClass, aMethod, aProgram, theSuccessor, aAlreadyVisited, theNewHelper, aBlocksByAddress);
                }
            }
        }
    }

    private void initializeBlockWith(BytecodeClass aOwningClass, BytecodeMethod aMethod, GraphNode aTargetBlock, Function<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocksByAddress,  ParsingHelper aHelper) {
        // Finally we can start to parse the program
        BytecodeBasicBlock theBytecodeBlock = aBlocksByAddress.apply(aTargetBlock.getStartAddress());

        for (BytecodeInstruction theInstruction : theBytecodeBlock.getInstructions()) {

            aTargetBlock.addExpression(new CommentExpression(" OP " + theInstruction.getOpcodeAddress().getAddress()));

            if (theInstruction instanceof BytecodeInstructionNOP) {
                BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITORENTER) {
                BytecodeInstructionMONITORENTER theINS = (BytecodeInstructionMONITORENTER) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITOREXIT) {
                BytecodeInstructionMONITOREXIT theINS = (BytecodeInstructionMONITOREXIT) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                Value theValue = aHelper.peek();
                aTargetBlock.addExpression(new CheckCastExpression(theValue, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                aHelper.pop();
            } else if (theInstruction instanceof BytecodeInstructionPOP2) {
                BytecodeInstructionPOP2 theINS = (BytecodeInstructionPOP2) theInstruction;
                Value theValue = aHelper.pop();
                switch (theValue.resolveType().resolve()) {
                    case LONG:
                        break;
                    case DOUBLE:
                        break;
                    default:
                        aHelper.pop();
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                Value theValue = aHelper.peek();
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionDUP2X1) {
                BytecodeInstructionDUP2X1 theINS = (BytecodeInstructionDUP2X1) theInstruction;
                Value theValue1 = aHelper.pop();
                if (theValue1.resolveType().resolve() == TypeRef.Native.LONG || theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    Value theValue2 = aHelper.pop();

                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                } else {
                    Value theValue2 = aHelper.pop();
                    Value theValue3 = aHelper.pop();

                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                    aHelper.push(theValue3);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                Value theValue1 = aHelper.pop();
                Value theValue2 = aHelper.pop();

                aHelper.push(theValue1);
                aHelper.push(theValue2);
                aHelper.push(theValue1);

            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                GetStaticValue theValue = new GetStaticValue(theINS.getConstant());
                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), theValue);
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theINS = (BytecodeInstructionASTORE) theInstruction;
                Value theValue = aHelper.pop();
                aHelper.setLocalVariable(theINS.getVariableIndex(), theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theINS = (BytecodeInstructionGenericSTORE) theInstruction;
                Value theValue = aHelper.pop();
                aHelper.setLocalVariable(theINS.getVariableIndex(), theValue);
            } else if (theInstruction instanceof BytecodeInstructionObjectArrayLOAD) {
                BytecodeInstructionObjectArrayLOAD theINS = (BytecodeInstructionObjectArrayLOAD) theInstruction;
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new ArrayEntryValue(TypeRef.Native.REFERENCE, theTarget, theIndex));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();

                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new ArrayEntryValue(TypeRef.toType(theINS.getType()), theTarget, theIndex));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArraySTORE) {
                BytecodeInstructionGenericArraySTORE theINS = (BytecodeInstructionGenericArraySTORE) theInstruction;
                Value theValue = aHelper.pop();
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.addExpression(new ArrayStoreExpression(TypeRef.toType(theINS.getType()), theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionObjectArraySTORE) {
                BytecodeInstructionObjectArraySTORE theINS = (BytecodeInstructionObjectArraySTORE) theInstruction;
                Value theValue = aHelper.pop();
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.addExpression(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                BytecodeInstructionACONSTNULL theINS = (BytecodeInstructionACONSTNULL) theInstruction;
                aHelper.push(new NullValue());
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                Value theValue = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.addExpression(new PutFieldExpression(theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                Value theTarget = aHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), new GetFieldValue(theINS.getFieldRefConstant(), theTarget));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC theINS = (BytecodeInstructionPUTSTATIC) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.addExpression(new PutStaticExpression(theINS.getConstant(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theINS = (BytecodeInstructionGenericLDC) theInstruction;
                BytecodeConstant theConstant = theINS.constant();
                if (theConstant instanceof BytecodeDoubleConstant) {
                    BytecodeDoubleConstant theC = (BytecodeDoubleConstant) theConstant;
                    aHelper.push(new DoubleValue(theC.getDoubleValue()));
                } else if (theConstant instanceof BytecodeLongConstant) {
                    BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    aHelper.push(new LongValue(theC.getLongValue()));
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    aHelper.push(new FloatValue(theC.getFloatValue()));
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    aHelper.push(new IntegerValue(theC.getIntegerValue()));
                } else if (theConstant instanceof BytecodeStringConstant) {
                    BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, new StringValue(theC.getValue().stringValue()));
                    aHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;
                    aHelper.push(new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getShortValue()));
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                aHelper.push(new IntegerValue(theINS.getIntConst()));
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                aHelper.push(new FloatValue(theINS.getFloatValue()));
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                aHelper.push(new DoubleValue(theINS.getDoubleConst()));
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                aHelper.push(new LongValue(theINS.getLongConst()));
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(theValue.resolveType(), new NegatedValue(theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(TypeRef.Native.INT, new ArrayLengthValue(theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theINS = (BytecodeInstructionGenericLOAD) theInstruction;
                Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex());
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theINS = (BytecodeInstructionALOAD) theInstruction;
                Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex());
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericCMP) {
                BytecodeInstructionGenericCMP theINS = (BytecodeInstructionGenericCMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getPrimitiveTypeRef()), new CompareValue(theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                BytecodeInstructionLCMP theINS = (BytecodeInstructionLCMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new CompareValue(theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                Value theValueToIncrement = aHelper.getLocalVariable(theINS.getIndex());
                Value theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.INT, new BinaryValue(TypeRef.Native.INT, theValueToIncrement, BinaryValue.Operator.ADD, new IntegerValue(theINS.getConstant())));
                aHelper.setLocalVariable(theINS.getIndex(), theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.REMAINDER, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.ADD, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();

                Variable theNewVariable;
                Value theDivValue = new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.DIV, theValue2);
                switch (theINS.getType()) {
                case FLOAT:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                case DOUBLE:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                default:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new FloorValue(theDivValue, TypeRef.toType(theINS.getType())));
                    break;
                }

                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.MUL, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.SUB, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYXOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYAND, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYSHIFTLEFT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryValue(TypeRef.toType(theINS.getType()), theValue1, BinaryValue.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                Value theValue = aHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNULL);
                Variable theResult = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theINS.getOpcodeAddress(), theResult, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                Value theValue = aHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNONNULL);
                Variable theResult = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theINS.getOpcodeAddress(), theResult, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                case lt:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.LESSTHAN, theValue2);
                    break;
                case eq:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.EQUALS, theValue2);
                    break;
                case gt:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.GREATERTHAN, theValue2);
                    break;
                case ge:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.GREATEROREQUALS, theValue2);
                    break;
                case le:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.LESSTHANOREQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theINS.getOpcodeAddress(), theNewVariable, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theINS = (BytecodeInstructionIFACMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                case eq:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.EQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue1, BinaryValue.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theINS.getOpcodeAddress(), theNewVariable, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                Value theValue = aHelper.pop();
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                    case lt:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.LESSTHAN, new IntegerValue(0));
                        break;
                    case eq:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.EQUALS, new IntegerValue(0));
                        break;
                    case gt:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.GREATERTHAN, new IntegerValue(0));
                        break;
                    case ge:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.GREATEROREQUALS, new IntegerValue(0));
                        break;
                    case le:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.LESSTHANOREQUALS, new IntegerValue(0));
                        break;
                    case ne:
                        theBinaryValue = new BinaryValue(TypeRef.Native.BOOLEAN, theValue, BinaryValue.Operator.NOTEQUALS, new IntegerValue(0));
                        break;
                    default:
                        throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theINS.getOpcodeAddress(), theNewVariable, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                BytecodeInstructionObjectRETURN theINS = (BytecodeInstructionObjectRETURN) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.addExpression(new ReturnValueExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                BytecodeInstructionGenericRETURN theINS = (BytecodeInstructionGenericRETURN) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.addExpression(new ReturnValueExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theINS = (BytecodeInstructionATHROW) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.addExpression(new ThrowExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                BytecodeInstructionRETURN theINS = (BytecodeInstructionRETURN) theInstruction;
                aTargetBlock.addExpression(new ReturnExpression());
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theINS = (BytecodeInstructionNEW) theInstruction;

                BytecodeClassinfoConstant theClassInfo = theINS.getClassInfoForObjectToCreate();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant());
                if (theObjectType.name().equals(Address.class.getName())) {
                    // At this time the exact location is unknown, the value
                    // will be set at constructor invocation time
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT);
                    aHelper.push(theNewVariable);
                } else {
                    if (theObjectType.equals(BytecodeObjectTypeRef.fromRuntimeClass(TRuntimeGeneratedType.class))) {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, new RuntimeGeneratedTypeValue());
                        aHelper.push(theNewVariable);
                    } else {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, new NewObjectValue(theClassInfo));
                        aHelper.push(theNewVariable);
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                Value theLength = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewArrayValue(theINS.getPrimitiveType(), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                List<Value> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(aHelper.pop());
                }
                Collections.reverse(theDimensions);
                BytecodeTypeRef theTypeRef = linkerContext.getSignatureParser().toFieldType(new BytecodeUtf8Constant(theINS.getTypeConstant().getConstant().stringValue()));
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewMultiArrayValue(theTypeRef, theDimensions));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                Value theLength = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewArrayValue(BytecodeObjectTypeRef.fromUtf8Constant(theINS.getTypeConstant().getConstant()), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                aTargetBlock.addExpression(new GotoExpression(theINS.getJumpAddress(), aTargetBlock));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionValue(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionValue(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionValue(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionValue(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Variable theTarget = (Variable) aHelper.pop();
                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                if (theType.equals(BytecodeObjectTypeRef.fromRuntimeClass(TRuntimeGeneratedType.class))) {
                    RuntimeGeneratedTypeValue theValue = (RuntimeGeneratedTypeValue) theTarget.singleInitValue();
                    theValue.setType(theArguments.get(0));
                    theValue.setMethodRef(theArguments.get(1));
                } else if (theType.equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    theTarget.initializeWith(theArguments.get(0));
                } else {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
                    if ("getClass".equals(theMethodName) && BytecodeLinkedClass.GET_CLASS_SIGNATURE.metchesExactlyTo(theSignature)) {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), new TypeOfValue(theTarget));
                        aHelper.push(theNewVariable);
                    } else {
                        DirectInvokeMethodValue theValue = new DirectInvokeMethodValue(theType, theMethodName, theSignature, theTarget, theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.addExpression(new DirectInvokeMethodExpression(theValue));
                        } else {
                            Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                            aHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                BytecodeInstructionINVOKEVIRTUAL theINS = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                if (theSignature.metchesExactlyTo(BytecodeLinkedClass.GET_CLASS_SIGNATURE) && "getClass".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    Value theValue = new TypeOfValue(aHelper.pop());
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                    aHelper.push(theNewVariable);
                    return;
                }

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Value theTarget = aHelper.pop();
                InvokeVirtualMethodValue theValue = new InvokeVirtualMethodValue(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new InvokeVirtualMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                    aHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Value theTarget = aHelper.pop();
                InvokeVirtualMethodValue theValue = new InvokeVirtualMethodValue(theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new InvokeVirtualMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                    aHelper.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                BytecodeClassinfoConstant theTargetClass = theINS.getMethodReference().getClassIndex().getClassConstant();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theTargetClass.getConstant());
                if (theObjectType.name().equals(MemoryManager.class.getName()) && "initTestMemory".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    // This invocation can be skipped!!!
                } else if (theObjectType.name().equals(Address.class.getName())) {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex()
                            .getName().stringValue();
                    switch (theMethodName) {
                    case "setIntValue": {

                        Value theTarget = theArguments.get(0);
                        Value theOffset = theArguments.get(1);
                        Value theNewValue = theArguments.get(2);

                        ComputedMemoryLocationWriteValue theLocation = new ComputedMemoryLocationWriteValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theLocation);
                        aTargetBlock.addExpression(new SetMemoryLocationExpression(theNewVariable, theNewValue));
                        break;
                    }
                    case "getStart": {

                        Value theTarget = theArguments.get(0);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theTarget);

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getStackTop": {

                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new StackTopValue());

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getMemorySize": {

                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new MemorySizeValue());

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getIntValue": {

                        Value theTarget = theArguments.get(0);
                        Value theOffset = theArguments.get(1);

                        ComputedMemoryLocationReadValue theLocation = new ComputedMemoryLocationReadValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theLocation);
                        aHelper.push(theNewVariable);

                        break;
                    }
                    case "unreachable": {
                        aTargetBlock.addExpression(new UnreachableExpression());
                        break;
                    }
                    default:
                        throw new IllegalStateException("Not implemented : " + theMethodName);
                    }
                } else {
                    BytecodeObjectTypeRef theClassToInvoke = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                    BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(theClassToInvoke)
                            .linkStaticMethod(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());

                    InvokeStaticMethodValue theValue = new InvokeStaticMethodValue(
                            theLinkedClass.getClassName(),
                            theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                            theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(),
                            theArguments);
                    if (theSignature.getReturnType().isVoid()) {
                        aTargetBlock.addExpression(new InvokeStaticMethodExpression(theValue));
                    } else {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                Value theValueToCheck = aHelper.pop();
                InstanceOfValue theValue = new InstanceOfValue(theValueToCheck, theINS.getTypeRef());

                Variable theCheckResult = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theValue);
                aHelper.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                Value theValue = aHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget(), aTargetBlock));

                Map<Long, ExpressionList> theOffsets = new HashMap<>();
                long[] theJumpTargets = theINS.getOffsets();
                for (int i=0;i<theJumpTargets.length;i++) {
                    ExpressionList theJump = new ExpressionList();
                    theJump.add(new GotoExpression(theINS.getOpcodeAddress().add((int) theJumpTargets[i]), aTargetBlock));
                    theOffsets.put((long) i, theJump);
                }

                aTargetBlock.addExpression(new TableSwitchExpression(theValue, theINS.getLowValue(), theINS.getHighValue(),
                        theDefault, theOffsets));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                Value theValue = aHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget(), aTargetBlock));

                Map<Long, ExpressionList> thePairs = new HashMap<>();
                for (BytecodeInstructionLOOKUPSWITCH.Pair thePair : theINS.getPairs()) {
                    ExpressionList thePairExpressions = new ExpressionList();
                    thePairExpressions.add(new GotoExpression(theINS.getOpcodeAddress().add((int) thePair.getOffset()), aTargetBlock));
                    thePairs.put(thePair.getMatch(), thePairExpressions);
                }

                aTargetBlock.addExpression(new LookupSwitchExpression(theValue, theDefault, thePairs));
            } else if (theInstruction instanceof BytecodeInstructionINVOKEDYNAMIC) {
                BytecodeInstructionINVOKEDYNAMIC theINS = (BytecodeInstructionINVOKEDYNAMIC) theInstruction;

                BytecodeInvokeDynamicConstant theConstant = theINS.getCallSite();
                BytecodeMethodSignature theInitSignature = theConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();


                BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
                BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());

                BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();
                BytecodeMethodRefConstant theBootstrapMethodToInvoke = (BytecodeMethodRefConstant) theMethodRef.getReferenceIndex().getConstant();

                Program theProgram = new Program();
                GraphNode theInitNode = new GraphNode(GraphNode.BlockType.NORMAL, theProgram, new BytecodeOpcodeAddress(0));

                switch (theMethodRef.getReferenceKind()) {
                case REF_invokeStatic: {

                    BytecodeObjectTypeRef theClassWithBootstrapMethod = BytecodeObjectTypeRef
                            .fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant());

                    BytecodeMethodSignature theSignature = theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType()
                            .getDescriptorIndex().methodSignature();

                    List<Value> theArguments = new ArrayList<>();
                    // Add the three default constants
                    // TMethodHandles.Lookup aCaller,
                    theArguments.add(theInitNode
                            .newVariable(TypeRef.Native.REFERENCE, new MethodHandlesGeneratedLookupValue(theClassWithBootstrapMethod)));
                    theArguments.add(theInitNode.newVariable(
                            TypeRef.Native.REFERENCE, new StringValue(theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())));
                    // TMethodType aInvokedType,
                    theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE, new MethodTypeValue(
                            theInitSignature)));

                    // Revolve the static arguments
                    for (BytecodeConstant theArgumentConstant : theBootstrapMethod.getArguments()) {

                        if (theArgumentConstant instanceof BytecodeMethodTypeConstant) {
                            BytecodeMethodTypeConstant theMethodType = (BytecodeMethodTypeConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE,
                                    new MethodTypeValue(theMethodType.getDescriptorIndex().methodSignature())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeStringConstant) {
                            BytecodeStringConstant thePrimitive = (BytecodeStringConstant) theArgumentConstant;
                            theArguments.add(theInitNode
                                    .newVariable(TypeRef.Native.REFERENCE, new StringValue(thePrimitive.getValue().stringValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeLongConstant) {
                            BytecodeLongConstant thePrimitive = (BytecodeLongConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.LONG, new LongValue(thePrimitive.getLongValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeIntegerConstant) {
                            BytecodeIntegerConstant thePrimitive = (BytecodeIntegerConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.INT, new LongValue(thePrimitive.getIntegerValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeFloatConstant) {
                            BytecodeFloatConstant thePrimitive = (BytecodeFloatConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.FLOAT, new FloatValue(thePrimitive.getFloatValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeDoubleConstant) {
                            BytecodeDoubleConstant thePrimitive = (BytecodeDoubleConstant) theArgumentConstant;
                            theArguments
                                    .add(theInitNode.newVariable(TypeRef.Native.DOUBLE, new DoubleValue(thePrimitive.getDoubleValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeMethodHandleConstant) {
                            BytecodeMethodHandleConstant theMethodHandle = (BytecodeMethodHandleConstant) theArgumentConstant;
                            BytecodeReferenceIndex theReference = theMethodHandle.getReferenceIndex();
                            BytecodeMethodRefConstant theReferenceConstant = (BytecodeMethodRefConstant) theReference
                                    .getConstant();
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE, new MethodRefValue(theReferenceConstant)));
                            continue;
                        }
                        throw new IllegalStateException("Unsupported argument type : " + theArgumentConstant);
                    }

                    // Ok, is the last argument of the bootstrap method a vararg argument
                    BytecodeTypeRef theLastArgument = theSignature.getArguments()[theSignature.getArguments().length - 1];
                    if (theLastArgument.isArray()) {
                        // Yes, so we have to wrap everything from this position on in an array
                        int theSignatureLength = theSignature.getArguments().length;
                        int theArgumentsLength = theArguments.size();

                        int theVarArgsLength = theArgumentsLength - theSignatureLength + 1;
                        Variable theNewVarargsArray = theInitNode.newVariable(TypeRef.Native.REFERENCE, new NewArrayValue(
                                BytecodeObjectTypeRef.fromRuntimeClass(TObject.class), new IntegerValue(theVarArgsLength)));
                        for (int i = theSignatureLength - 1; i < theArgumentsLength; i++) {
                            Value theVariable = theArguments.get(i);
                            theArguments.remove(theVariable);
                            theInitNode.addExpression(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theNewVarargsArray, new IntegerValue(i - theSignatureLength + 1), theVariable));
                        }
                        theArguments.add(theNewVarargsArray);
                    }

                    InvokeStaticMethodValue theInvokeStaticValue = new InvokeStaticMethodValue(
                            BytecodeObjectTypeRef.fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant()),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(),
                            theArguments);
                    Variable theNewVariable = theInitNode.newVariable(TypeRef.Native.REFERENCE, theInvokeStaticValue);
                    theInitNode.addExpression(new ReturnValueExpression(theNewVariable));

                    // First step, we construct a callsitre
                    ResolveCallsiteObjectValue theValue = new ResolveCallsiteObjectValue(aOwningClass.getThisInfo().getConstant().stringValue() + "_" + aMethod.getName().stringValue() + "_" + theINS.getOpcodeAddress().getAddress(), aOwningClass, theProgram, theInitNode);
                    Variable theCallsiteVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theValue);

                    // Second step, we invoke the callsite to get whatever we are searching
                    InvokeVirtualMethodValue theGetTargetValue = new InvokeVirtualMethodValue("getTarget",
                            new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TMethodHandle.class),
                                    new BytecodeTypeRef[0]),
                            theCallsiteVariable, new ArrayList<>());
                    Variable theMethodHandleVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theGetTargetValue);

                    List<Value> theInvokeArguments = new ArrayList<>();

                    Variable theArray = aTargetBlock.newVariable(
                            TypeRef.Native.REFERENCE, new NewArrayValue(BytecodeObjectTypeRef.fromRuntimeClass(TObject.class), new IntegerValue(theInitSignature.getArguments().length)));

                    for (int i=0;i<theInitSignature.getArguments().length;i++) {
                        Variable theIndex = aTargetBlock.newVariable(TypeRef.Native.INT, new IntegerValue(i));
                        aTargetBlock.addExpression(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theArray, theIndex, aHelper.pop()));
                    }

                    theInvokeArguments.add(theArray);

                    InvokeVirtualMethodValue theInvokeValue = new InvokeVirtualMethodValue("invokeExact",
                            new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TObject.class),
                                    new BytecodeTypeRef[] {
                                            new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(TObject.class), 1) }),
                            theMethodHandleVariable, theInvokeArguments);

                    Variable theInvokeExactResult = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theInvokeValue);
                    aHelper.push(theInvokeExactResult);

                    break;
                }
                default:
                    throw new IllegalStateException(
                            "Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
                }
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        aTargetBlock.addExpression(new CommentExpression("Final stack size is " + aHelper.stack.size()));

        aHelper.finalizeExportState();
    }
}