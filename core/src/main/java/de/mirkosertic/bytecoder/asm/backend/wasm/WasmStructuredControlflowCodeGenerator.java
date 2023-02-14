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

import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Container;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.HostType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.RefType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmValue;
import de.mirkosertic.bytecoder.asm.ir.AbstractVar;
import de.mirkosertic.bytecoder.asm.ir.Add;
import de.mirkosertic.bytecoder.asm.ir.And;
import de.mirkosertic.bytecoder.asm.ir.ArrayLength;
import de.mirkosertic.bytecoder.asm.ir.ArrayLoad;
import de.mirkosertic.bytecoder.asm.ir.ArrayStore;
import de.mirkosertic.bytecoder.asm.ir.CMP;
import de.mirkosertic.bytecoder.asm.ir.CaughtException;
import de.mirkosertic.bytecoder.asm.ir.CheckCast;
import de.mirkosertic.bytecoder.asm.ir.Copy;
import de.mirkosertic.bytecoder.asm.ir.Div;
import de.mirkosertic.bytecoder.asm.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.asm.ir.Goto;
import de.mirkosertic.bytecoder.asm.ir.If;
import de.mirkosertic.bytecoder.asm.ir.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.InstanceMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.InstanceOf;
import de.mirkosertic.bytecoder.asm.ir.InterfaceMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.InterfaceMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.InvokeDynamicExpression;
import de.mirkosertic.bytecoder.asm.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.asm.ir.LookupSwitch;
import de.mirkosertic.bytecoder.asm.ir.MethodArgument;
import de.mirkosertic.bytecoder.asm.ir.MonitorEnter;
import de.mirkosertic.bytecoder.asm.ir.MonitorExit;
import de.mirkosertic.bytecoder.asm.ir.Mul;
import de.mirkosertic.bytecoder.asm.ir.Neg;
import de.mirkosertic.bytecoder.asm.ir.New;
import de.mirkosertic.bytecoder.asm.ir.NewArray;
import de.mirkosertic.bytecoder.asm.ir.Node;
import de.mirkosertic.bytecoder.asm.ir.NullReference;
import de.mirkosertic.bytecoder.asm.ir.ObjectString;
import de.mirkosertic.bytecoder.asm.ir.Or;
import de.mirkosertic.bytecoder.asm.ir.PHI;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveDouble;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveFloat;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveShort;
import de.mirkosertic.bytecoder.asm.ir.ReadClassField;
import de.mirkosertic.bytecoder.asm.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.asm.ir.ResolveCallsite;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedField;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.ir.Return;
import de.mirkosertic.bytecoder.asm.ir.ReturnValue;
import de.mirkosertic.bytecoder.asm.ir.RuntimeClass;
import de.mirkosertic.bytecoder.asm.ir.SHL;
import de.mirkosertic.bytecoder.asm.ir.SHR;
import de.mirkosertic.bytecoder.asm.ir.SetClassField;
import de.mirkosertic.bytecoder.asm.ir.SetInstanceField;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.Sub;
import de.mirkosertic.bytecoder.asm.ir.TableSwitch;
import de.mirkosertic.bytecoder.asm.ir.This;
import de.mirkosertic.bytecoder.asm.ir.TypeConversion;
import de.mirkosertic.bytecoder.asm.ir.TypeReference;
import de.mirkosertic.bytecoder.asm.ir.USHR;
import de.mirkosertic.bytecoder.asm.ir.Unwind;
import de.mirkosertic.bytecoder.asm.ir.Value;
import de.mirkosertic.bytecoder.asm.ir.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.VirtualMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.XOr;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.asm.backend.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.classlib.Array;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WasmStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    private final CompileUnit compileUnit;

    private final Module module;

    private final Map<ResolvedClass, StructType> objectTypeMappings;

    private final Map<ResolvedClass, StructType> rtMappings;

    private final ExportableFunction exportableFunction;

    private final Function<Type, WasmType> typeConverter;

    private final Map<AbstractVar, Local> varLocalMap;

    private final Container targetContainer;

    public WasmStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final Module module, final Map<ResolvedClass, StructType> rtMappings, final Map<ResolvedClass, StructType> objectTypeMappings, final ExportableFunction exportableFunction, final Function<Type, WasmType> typeConverter) {
        this.compileUnit = compileUnit;
        this.module = module;
        this.exportableFunction = exportableFunction;
        this.rtMappings = rtMappings;
        this.objectTypeMappings = objectTypeMappings;
        this.typeConverter = typeConverter;
        this.varLocalMap = new HashMap<>();
        this.targetContainer = exportableFunction;
    }

    @Override
    public void registerVariables(final List<AbstractVar> variables) {
        for (int i = 0; i < variables.size(); i++) {
            final AbstractVar v = variables.get(i);
            final String varName;
            if (v instanceof PHI) {
                varName = "phi" + i;
            } else {
                varName = "var" + i;
            }
            final WasmType type = typeConverter.apply(v.type);
            final Local local = exportableFunction.newLocal(varName, type);

            varLocalMap.put(v, local);
        }
    }

    @Override
    public void write(final InstanceMethodInvocation value) {
        targetContainer.flow.comment("InstanceMethodInvocation");
    }

    @Override
    public void write(final VirtualMethodInvocation node) {
        targetContainer.flow.comment("VirtualMethodInvocation");
    }

    @Override
    public void write(final StaticMethodInvocation node) {
        targetContainer.flow.comment("StaticMethodInvocation");
    }

    @Override
    public void write(final InterfaceMethodInvocation node) {
        targetContainer.flow.comment("InterfaceMethodInvocation");
    }

    private WasmValue toWasmValue(final This value) {
        final Local local = exportableFunction.localByLabel("thisref");
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final ObjectString value) {
        final int index = value.value.index;
        final Global global = module.getGlobals().globalsIndex().globalByLabel("stringpool_" + index);
        return ConstExpressions.getGlobal(global);
    }

    private WasmValue toWasmValue(final PrimitiveShort value) {
        return ConstExpressions.i32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveInt value) {
        return ConstExpressions.i32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveLong value) {
        return ConstExpressions.i64.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveFloat value) {
        return ConstExpressions.f32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveDouble value) {
        return ConstExpressions.f64.c(value.value);
    }

    private WasmValue toWasmValue(final MethodArgument value) {
        final String localName = "arg" + value.index;
        final Local local = exportableFunction.localByLabel(localName);
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final AbstractVar value) {
        final Local local = varLocalMap.get(value);
        if (local == null) {
            throw new IllegalArgumentException("Cannot find Wasm local for variable " + value);
        }
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final NullReference value) {
        return ConstExpressions.ref.nullRef();
    }

    private WasmValue toWasmValue(final New value) {
        final ResolvedClass cl = compileUnit.findClass(value.type);
        final StructType type = objectTypeMappings.get(cl);
        final List<WasmValue> initArgs = new ArrayList<>();

        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            initArgs.add(ConstExpressions.call(initFunction, Collections.emptyList()));
        } else {
            final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
            initArgs.add(ConstExpressions.getGlobal(global));
        }
        for (int i = 1; i < type.getFields().size(); i++) {
            final StructType.Field f = type.getFields().get(i);
            if (f.getType() instanceof PrimitiveType) {
                switch ((PrimitiveType) f.getType()) {
                    case i32:
                        initArgs.add(ConstExpressions.i32.c(0));
                        break;
                    case f32:
                        initArgs.add(ConstExpressions.f32.c(0.0f));
                        break;
                    case i64:
                        initArgs.add(ConstExpressions.i64.c(0L));
                        break;
                    case f64:
                        initArgs.add(ConstExpressions.f64.c(0.0d));
                        break;
                    default:
                        throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
                }
            } else if (f.getType() instanceof HostType) {
                initArgs.add(ConstExpressions.ref.nullRef());
            } else if (f.getType() instanceof RefType) {
                initArgs.add(ConstExpressions.ref.nullRef());
            } else {
                throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
            }
        }

        return ConstExpressions.struct.newInstance(type, initArgs);
    }

    private WasmValue toWasmValue(final ReadInstanceField value) {
        final ResolvedClass cl = compileUnit.findClass(value.resolvedField.owner.type);
        final StructType type = objectTypeMappings.get(cl);
        return ConstExpressions.struct.get(type,
                ConstExpressions.ref.cast(type, toWasmValue((Value) value.incomingDataFlows[0])),
                value.resolvedField.name);
    }

    private WasmValue toWasmValue(final StaticMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            callArgs.add(ConstExpressions.call(initFunction, Collections.emptyList()));
        } else {
            final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
            callArgs.add(ConstExpressions.getGlobal(global));
        }

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (final Node arg : value.incomingDataFlows) {
            callArgs.add(toWasmValue((Value) arg));
        }
        return ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    private WasmValue toWasmValue(final InstanceMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (final Node arg : value.incomingDataFlows) {
            callArgs.add(toWasmValue((Value) arg));
        }
        return ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    private WasmValue toWasmValue(final VirtualMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);

        for (final Node arg : value.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(-1)); // TODO: Resolve function index

        final StructType classType = rtMappings.get(cl);

        resolverArgs.add(ConstExpressions.struct.get(
                classType,
                ConstExpressions.ref.cast(classType, toWasmValue((Value) value.incomingDataFlows[0])),
                "vt_resolver"
        ));

        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = (FunctionType) typeConverter.apply(value.resolvedMethod.methodType);

        return ConstExpressions.call(ft, indirectCallArgs, resolver);
    }

    private WasmValue toWasmValue(final InterfaceMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);

        for (final Node arg : value.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(-1)); // TODO: Resolve function index

        final StructType classType = rtMappings.get(cl);

        resolverArgs.add(ConstExpressions.struct.get(
                classType,
                ConstExpressions.ref.cast(classType, toWasmValue((Value) value.incomingDataFlows[0])),
                "vt_resolver"
        ));

        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = (FunctionType) typeConverter.apply(value.resolvedMethod.methodType);

        return ConstExpressions.call(ft, indirectCallArgs, resolver);
    }

    private WasmValue toWasmValue(final InvokeDynamicExpression value) {
        // TODO: Implement this
        return ConstExpressions.ref.nullRef();
        //return ConstExpressions.i32.c(-1000);
    }

    private WasmValue toWasmValue(final ResolveCallsite value) {
        // TODO: implement this
        return ConstExpressions.i32.c(-2000);
    }

    private WasmValue toType(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE: {
                // Primitive type

                // TODO: resolve correct class here
                final ResolvedClass cl = compileUnit.findClass(Type.getType(Object.class));
                final String className = WasmHelpers.generateClassName(cl.type);

                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            case Type.OBJECT: {
                // Object type
                final ResolvedClass cl = compileUnit.findClass(type);
                final String className = WasmHelpers.generateClassName(cl.type);
                if (cl.requiresClassInitializer()) {
                    final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
                    return ConstExpressions.call(initFunction, Collections.emptyList());
                }
                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            case Type.ARRAY: {
                // Array type

                // TODO: resolve correct class here
                final ResolvedClass cl = compileUnit.findClass(Type.getType(Object.class));
                final String className = WasmHelpers.generateClassName(cl.type);

                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            default: {
                throw new IllegalStateException("Unsupported type " + type);
            }
        }
    }

    private WasmValue toWasmValue(final TypeReference value) {
        return toType(value.type);
    }

    private WasmValue toWasmValue(final ReadClassField value) {
        final ResolvedField field = value.resolvedField;
        final ResolvedClass cl = field.owner;
        final StructType type = rtMappings.get(cl);
        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            return ConstExpressions.struct.get(type,
                    ConstExpressions.ref.cast(type, ConstExpressions.call(initFunction, Collections.emptyList())),
                    field.name);
        }
        final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
        return ConstExpressions.struct.get(type, ConstExpressions.getGlobal(global),
                    field.name);
    }

    private WasmValue toWasmValue(final RuntimeClass value) {
        return toType(value.type);
    }

    private WasmValue toWasmValue(final CaughtException value) {
        final Global g = module.getGlobals().globalsIndex().globalByLabel("lastthrownexception");
        return ConstExpressions.getGlobal(g);
    }

    private WasmValue toWasmValue(final CMP value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        final List<WasmValue> arguments = new ArrayList<>();
        arguments.add(toWasmValue(left));
        arguments.add(toWasmValue(right));
        switch (left.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_i32"), arguments);
            case Type.FLOAT:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_f32"), arguments);
            case Type.LONG:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_i64"), arguments);
            case Type.DOUBLE:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_f64"), arguments);
            default:
                throw new IllegalStateException("Not implemented compare for " + left.type);
        }
    }

    private WasmValue toWasmValue(final Mul value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.mul(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.mul(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.mul(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.mul(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented mul for " + left.type);
        }
    }

    private WasmValue toWasmValue(final SHR value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shr_s(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shr_s(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented SHR for " + left.type);
        }
    }

    private WasmValue toWasmValue(final TypeConversion value) {
        final Value incoming = (Value) value.incomingDataFlows[0];
        switch (incoming.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                switch (value.type.getSort()) {
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        // Nothing to do
                        return toWasmValue(incoming);
                    case Type.FLOAT:
                        return ConstExpressions.f32.convert_si32(toWasmValue(incoming));
                    case Type.LONG:
                        return ConstExpressions.i64.extend_i32s(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.convert_si32(toWasmValue(incoming));
                    default:
                        throw new IllegalArgumentException("Not implemented!");
                }
            case Type.LONG:
                switch (value.type.getSort()) {
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.wrap_i64(toWasmValue(incoming));
                    case Type.LONG:
                        return toWasmValue(incoming);
                    case Type.FLOAT:
                        return ConstExpressions.f32.convert_si64(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.convert_si64(toWasmValue(incoming));
                    default:
                        throw new IllegalArgumentException("Not implemented!");
                }
            case Type.FLOAT:
                switch (value.type.getSort()) {
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.trunc_sf32(toWasmValue(incoming));
                    case Type.FLOAT:
                        return toWasmValue(incoming);
                    case Type.LONG:
                        return ConstExpressions.i64.trunc_sf32(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.promote_f32(toWasmValue(incoming));
                    default:
                        throw new IllegalArgumentException("Not implemented!");
                }
            case Type.DOUBLE:
                switch (value.type.getSort()) {
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.trunc_f64s(toWasmValue(incoming));
                    case Type.FLOAT:
                        return ConstExpressions.f32.trunc_f64s(toWasmValue(incoming));
                    case Type.LONG:
                        return ConstExpressions.i64.trunc_sf64(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return toWasmValue(incoming);
                    default:
                        throw new IllegalArgumentException("Not implemented!");
                }
            default:
                throw new IllegalStateException("Not implemented type conversion for " + incoming.type);
        }
    }

    private WasmValue toWasmValue(final NewArray value) {
        final Type elementType = value.type;
        final Value length = (Value) value.incomingDataFlows[0];

        final String typeToInstantiate;
        final WasmValue emptyArray;
        switch (elementType.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.BOOLEAN:
            case Type.INT:
                typeToInstantiate = "i32_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.i32), toWasmValue(length));
                break;
            case Type.LONG:
                typeToInstantiate = "i64_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.i64), toWasmValue(length));
                break;
            case Type.FLOAT:
                typeToInstantiate = "f32_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.f32), toWasmValue(length));
                break;
            case Type.DOUBLE:
                typeToInstantiate = "f64_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.f64), toWasmValue(length));
                break;
            case Type.OBJECT:
                typeToInstantiate = "obj_array";
                final ResolvedClass rc = compileUnit.findClass(Type.getType(Object.class));
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(ConstExpressions.ref.type(objectTypeMappings.get(rc), true)), toWasmValue(length));
                break;
            default:
                throw new IllegalArgumentException("Not supported array type " + elementType);
        }

        final StructType structType = module.getTypes().structTypeByName(typeToInstantiate);
        final List<WasmValue> initArguments = new ArrayList<>();
        final Type arrayClass = Type.getType(Array.class);
        final Global arrayGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(arrayClass) + "_cls");
        initArguments.add(ConstExpressions.getGlobal(arrayGlobal));
        initArguments.add(emptyArray);
        return ConstExpressions.struct.newInstance(structType, initArguments);
    }

    private WasmValue toWasmValue(final And value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.and(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.and(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented and for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Or value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.or(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.or(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented and for " + value.type);
        }
    }

    private WasmValue toWasmValue(final InstanceOf value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        final List<WasmValue> params = new ArrayList<>();
        params.add(toWasmValue(left));
        params.add(toWasmValue(right));
        return ConstExpressions.call(ConstExpressions.weakFunctionReference("instanceOf"), params);
    }

    private WasmValue toWasmValue(final Sub value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.sub(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.sub(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.sub(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.sub(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented sub for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Div value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.div_s(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.div(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.div_s(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.div(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented div for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Add value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.add(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.add(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.add(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.add(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented add for " + value.type);
        }
    }

    private WasmValue toWasmValue(final ArrayLength value) {
        final Value array = (Value) value.incomingDataFlows[0];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: {
                final StructType type = module.getTypes().structTypeByName("i32_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.i32),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.FLOAT: {
                final StructType type = module.getTypes().structTypeByName("f32_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.f32),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.LONG: {
                final StructType type = module.getTypes().structTypeByName("i64_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.i64),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.DOUBLE: {
                final StructType type = module.getTypes().structTypeByName("f64_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.f64),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.ARRAY: {
                final StructType type = module.getTypes().structTypeByName("obj_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            default:
                throw new IllegalStateException("Not implemented arraylength for " + value.type + " sort " + value.type.getSort());
        }
    }

    private WasmValue toWasmValue(final SHL value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shl(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shl(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented shl for " + value.type);
        }
    }

    private WasmValue toWasmValue(final USHR value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shr_u(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shr_u(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented ushr for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Neg value) {
        final Value left = (Value) value.incomingDataFlows[0];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.mul(toWasmValue(left), ConstExpressions.i32.c(-1));
            case Type.LONG:
                return ConstExpressions.i64.mul(toWasmValue(left), ConstExpressions.i64.c(-1L));
            case Type.FLOAT:
                return ConstExpressions.f32.mul(toWasmValue(left), ConstExpressions.f32.c(-1f));
            case Type.DOUBLE:
                return ConstExpressions.f64.mul(toWasmValue(left), ConstExpressions.f64.c(-1d));
            default:
                throw new IllegalStateException("Not implemented neg for " + value.type);
        }
    }

    private WasmValue toWasmValue(final XOr value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.xor(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.xor(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented shl for " + value.type);
        }
    }

    private WasmValue toWasmValue(final ArrayLoad value) {
        final Value array = (Value) value.incomingDataFlows[0];
        final Value index = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: {
                final StructType arrayType = module.getTypes().structTypeByName("i32_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.i32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.FLOAT: {
                final StructType arrayType = module.getTypes().structTypeByName("f32_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.f32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.LONG: {
                final StructType arrayType = module.getTypes().structTypeByName("i64_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.i64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.DOUBLE: {
                final StructType arrayType = module.getTypes().structTypeByName("f64_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.f64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.OBJECT:
            case Type.ARRAY: {
                final StructType arrayType = module.getTypes().structTypeByName("obj_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            default:
                throw new IllegalStateException("Not implemented arrayload for " + value.type + " sort " + value.type.getSort());
        }
    }

    private WasmValue toWasmValue(final Value value) {
        if (value instanceof This) {
            return toWasmValue((This) value);
        } else if (value instanceof ObjectString) {
            return toWasmValue((ObjectString) value);
        } else if (value instanceof PrimitiveShort) {
            return toWasmValue((PrimitiveShort) value);
        } else if (value instanceof PrimitiveInt) {
            return toWasmValue((PrimitiveInt) value);
        } else if (value instanceof PrimitiveLong) {
            return toWasmValue((PrimitiveLong) value);
        } else if (value instanceof PrimitiveFloat) {
            return toWasmValue((PrimitiveFloat) value);
        } else if (value instanceof PrimitiveDouble) {
            return toWasmValue((PrimitiveDouble) value);
        } else if (value instanceof MethodArgument) {
            return toWasmValue((MethodArgument) value);
        } else if (value instanceof AbstractVar) {
            return toWasmValue((AbstractVar) value);
        } else if (value instanceof NullReference) {
            return toWasmValue((NullReference) value);
        } else if (value instanceof New) {
            return toWasmValue((New) value);
        } else if (value instanceof ReadInstanceField) {
            return toWasmValue((ReadInstanceField) value);
        } else if (value instanceof StaticMethodInvocationExpression) {
            return toWasmValue((StaticMethodInvocationExpression) value);
        } else if (value instanceof VirtualMethodInvocationExpression) {
            return toWasmValue((VirtualMethodInvocationExpression) value);
        } else if (value instanceof InterfaceMethodInvocationExpression) {
            return toWasmValue((InterfaceMethodInvocationExpression) value);
        } else if (value instanceof InstanceMethodInvocationExpression) {
            return toWasmValue((InstanceMethodInvocationExpression) value);
        } else if (value instanceof InvokeDynamicExpression) {
            return toWasmValue((InvokeDynamicExpression) value);
        } else if (value instanceof TypeReference) {
            return toWasmValue((TypeReference) value);
        } else if (value instanceof ReadClassField) {
            return toWasmValue((ReadClassField) value);
        } else if (value instanceof RuntimeClass) {
            return toWasmValue((RuntimeClass) value);
        } else if (value instanceof CaughtException) {
            return toWasmValue((CaughtException) value);
        } else if (value instanceof NewArray) {
            return toWasmValue((NewArray) value);
        } else if (value instanceof CMP) {
            return toWasmValue((CMP) value);
        } else if (value instanceof Mul) {
            return toWasmValue((Mul) value);
        } else if (value instanceof SHR) {
            return toWasmValue((SHR) value);
        } else if (value instanceof TypeConversion) {
            return toWasmValue((TypeConversion) value);
        } else if (value instanceof And) {
            return toWasmValue((And) value);
        } else if (value instanceof ResolveCallsite) {
            return toWasmValue((ResolveCallsite) value);
        } else if (value instanceof Sub) {
            return toWasmValue((Sub) value);
        } else if (value instanceof ArrayLoad) {
            return toWasmValue((ArrayLoad) value);
        } else if (value instanceof Div) {
            return toWasmValue((Div) value);
        } else if (value instanceof SHL) {
            return toWasmValue((SHL) value);
        } else if (value instanceof XOr) {
            return toWasmValue((XOr) value);
        } else if (value instanceof Add) {
            return toWasmValue((Add) value);
        } else if (value instanceof ArrayLength) {
            return toWasmValue((ArrayLength) value);
        } else if (value instanceof Or) {
            return toWasmValue((Or) value);
        } else if (value instanceof InstanceOf) {
            return toWasmValue((InstanceOf) value);
        } else if (value instanceof USHR) {
            return toWasmValue((USHR) value);
        } else if (value instanceof Neg) {
            return toWasmValue((Neg) value);
        }
        throw new IllegalArgumentException("Not implemented " + value.getClass());
    }

    @Override
    public void write(final Copy node) {
        final Value value = (Value) node.incomingDataFlows[0];
        final Node target = node.outgoingFlows[0];
        if (target instanceof AbstractVar) {
            final Local local = varLocalMap.get((AbstractVar) target);
            if (local == null) {
                throw new IllegalArgumentException("Cannot find Wasm local for variable " + target);
            }
            targetContainer.flow.setLocal(local, toWasmValue(value));

        } else {
            targetContainer.flow.comment("Copy from " + value.getClass() + " to " + target.getClass());
        }
    }

    @Override
    public void writeIfAndStartTrueBlock(final If node) {
        targetContainer.flow.comment("if");
    }

    @Override
    public void startIfElseBlock(final If node) {
        targetContainer.flow.comment("Else");
    }

    @Override
    public void finishBlock() {
        targetContainer.flow.comment("finishBlock");
    }

    @Override
    public void startBlock(final Sequencer.Block node) {
        targetContainer.flow.comment("Start " + node.label + ", " + node.type);
    }

    @Override
    public void write(final LineNumberDebugInfo node) {
        targetContainer.flow.comment("Line number #" + node.lineNumber);
    }

    @Override
    public void write(final FrameDebugInfo node) {
        targetContainer.flow.comment("FrameDebugInfo");
    }

    @Override
    public void write(final Goto node) {
        targetContainer.flow.comment("Goto");
    }

    @Override
    public void write(final MonitorEnter node) {
        targetContainer.flow.comment("MonitorEnter");
    }

    @Override
    public void write(final MonitorExit node) {
        targetContainer.flow.comment("MonitorExit");
    }

    @Override
    public void write(final Unwind node) {
        targetContainer.flow.comment("Unwind");
    }

    @Override
    public void write(final Return node) {
        targetContainer.flow.comment("Return");
    }

    @Override
    public void write(final ReturnValue node) {
        targetContainer.flow.comment("ReturnValue");
    }

    @Override
    public void write(final SetInstanceField node) {
        targetContainer.flow.comment("SetInstanceField");
    }

    @Override
    public void write(final SetClassField node) {
        targetContainer.flow.comment("SetClassField");
    }

    @Override
    public void write(final ArrayStore node) {
        targetContainer.flow.comment("ArrayStore");
    }

    @Override
    public void write(final CheckCast node) {
        targetContainer.flow.comment("CheckCast");
    }

    @Override
    public void writeBreakTo(final String label) {
        targetContainer.flow.comment("writeBreakTo " + label);
    }

    @Override
    public void writeContinueTo(final String label) {
        targetContainer.flow.comment("writeContinueTo " + label);
    }

    @Override
    public void startTryCatch(final String label) {
        targetContainer.flow.comment("startTryCatch " + label);
    }

    @Override
    public void startCatchBlock() {
        targetContainer.flow.comment("startCatchBlock");
    }

    @Override
    public void startCatchHandler(final Type type) {
        targetContainer.flow.comment("startCatchHandler");
    }

    @Override
    public void endCatchHandler() {
        targetContainer.flow.comment("endCatchHandler");
    }

    @Override
    public void writeRethrowException() {
        targetContainer.flow.comment("writeRethrowException");
    }

    @Override
    public void startFinallyBlock() {
        targetContainer.flow.comment("startFinallyBlock");
    }

    @Override
    public void writeSwitch(final TableSwitch node) {
        targetContainer.flow.comment("TableSwitch");
    }

    @Override
    public void startTableSwitchDefaultBlock() {
        targetContainer.flow.comment("startTableSwitchDefaultBlock");
    }

    @Override
    public void writeSwitch(final LookupSwitch node) {
        targetContainer.flow.comment("writeSwitch");
    }

    @Override
    public void writeSwitchCase(final int index) {
        targetContainer.flow.comment("writeSwitchCase");
    }

    @Override
    public void writeSwitchDefaultCase() {
        targetContainer.flow.comment("writeSwitchDefaultCase");
    }

    @Override
    public void finishSwitchCase() {
        targetContainer.flow.comment("finishSwitchCase");
    }

    @Override
    public void writeDebugNote(final String message) {
        targetContainer.flow.comment(message);
    }
}
