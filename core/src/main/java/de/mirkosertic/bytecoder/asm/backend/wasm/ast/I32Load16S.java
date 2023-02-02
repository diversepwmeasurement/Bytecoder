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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class I32Load16S implements WasmExpression {

    private final Alignment alignment;
    private final int offset;
    private final WasmValue ptr;

    I32Load16S(final int offset, final WasmValue ptr) {
        this(Alignment.TWO, offset, ptr);
    }

    I32Load16S(final Alignment alignment, final int offset, final WasmValue ptr) {
        this.alignment = alignment;
        this.offset = offset;
        this.ptr = ptr;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("i32.load16_s");
        textWriter.space();
        textWriter.writeAttribute("offset", offset);
        textWriter.space();
        textWriter.writeAttribute("align", alignment.value());
        textWriter.space();
        ptr.writeTo(textWriter, context);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        ptr.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x2e);
        codeWriter.writeUnsignedLeb128(alignment.log2Value());
        codeWriter.writeUnsignedLeb128(offset);
    }
}
