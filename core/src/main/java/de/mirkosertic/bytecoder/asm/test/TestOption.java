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
package de.mirkosertic.bytecoder.asm.test;

public class TestOption {

    private final String backendType;
    private final boolean minify;

    public TestOption(final String backendType, final boolean minify) {
        this.backendType = backendType;
        this.minify = minify;
    }

    public String toDescription() {
        return "backend=" +
                backendType.toString() +
                " minify=" +
                minify;
    }

    public String toFilePrefix() {
        final StringBuilder builder = new StringBuilder();
        builder.append(backendType.toString());
        if (minify) {
            builder.append("_minify");
        }
        return builder.toString();
    }

    public String getBackendType() {
        return backendType;
    }

    public boolean isMinify() {
        return minify;
    }
}