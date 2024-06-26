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
package de.mirkosertic.bytecoder.core.ir;

public abstract class StandardProjections {

    public static final Projection DEFAULT = new Projection.DefaultProjection(EdgeType.FORWARD);

    public static final Projection TRUE = new Projection.TrueProjection(EdgeType.FORWARD);

    public static final Projection FALSE = new Projection.FalseProjection(EdgeType.FORWARD);

    public static final Projection TRYCATCHGUARD = new Projection.TryCatchGuardedProjection(EdgeType.FORWARD);

    public static final Projection TRYCATCHEXIT = new Projection.TryCatchGuardedExit(EdgeType.FORWARD);
}
