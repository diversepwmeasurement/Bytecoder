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

public class Try extends Container implements WasmExpression {

    private final PrimitiveType blockType;
    public final Catch catchBlock;

    Try(final Container parent, final PrimitiveType blockType, final Tag catchTag) {
        super(parent);
        this.blockType = blockType;
        catchBlock = new Catch(this, catchTag);
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("try");
        textWriter.space();
        textWriter.opening();
        textWriter.write("do");

        for (final WasmExpression e : getChildren()) {
            e.writeTo(textWriter, context.subWith(this));
        }

        textWriter.closing();
        textWriter.newLine();

        catchBlock.writeTo(textWriter, context);
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        codeWriter.writeByte((byte) 0x06);
        if (blockType != null) {
            blockType.writeTo(codeWriter);
        } else {
            PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        }
        for (final WasmExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        catchBlock.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x0b);
    }
}
