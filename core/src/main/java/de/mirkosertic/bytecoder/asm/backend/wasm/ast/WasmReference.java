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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class WasmReference implements WasmValue {

    private final ReferencableType type;
    private final boolean nullable;

    WasmReference(final ReferencableType type, final boolean nullable) {
        this.type = type;
        this.nullable = nullable;
    }

    @Override
    public void writeTo(TextWriter writer, ExportContext context) throws IOException {
        writer.opening();
        writer.write("ref ");
        if (nullable) {
            writer.write("null ");
        }
        type.writeRefTo(writer);
        writer.closing();
    }

    @Override
    public void writeTo(BinaryWriter.Writer binaryWriter, ExportContext context) throws IOException {
        //TODO
    }
}
