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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;
import java.util.List;

public class Call implements WasmExpression {

    private final Callable function;
    private final List<WasmValue> arguments;

    Call(final Callable function, final List<WasmValue> arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("call");
        textWriter.space();
        textWriter.writeLabel(function.getLabel());
        for (final WasmValue argument : arguments) {
            textWriter.space();
            argument.writeTo(textWriter, context);
        }
        textWriter.closing();
        if (function.resolveResultType(context) == null) {
            textWriter.newLine();
        }
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        for (final WasmValue argument : arguments) {
            argument.writeTo(codeWriter, context);
        }
        codeWriter.writeByte((byte) 0x10);
        codeWriter.writeUnsignedLeb128(function.resolveIndex(context));
    }
}
