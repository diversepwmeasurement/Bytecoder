package de.mirkosertic.bytecoder.asm.test;

import de.mirkosertic.bytecoder.asm.AnalysisStack;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.backend.js.JSBackend;
import de.mirkosertic.bytecoder.asm.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.asm.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.Loader;
import org.objectweb.asm.Type;

import java.io.PrintWriter;

public class ASMTest {

    public static void main(final String[] args) {
        // Writing method for Ljava/io/FileSystem; . <clinit>()V
        //GOTO 39 Region L917331754

        final ClassLoader cl = ASMTest.class.getClassLoader();
        final Loader loader = new BytecoderLoader(cl);
        final AnalysisStack analysisStack = new AnalysisStack();

        final CompileUnit compileUnit = new CompileUnit(loader, new JSIntrinsics());
        final Type invokedType = Type.getType("Ljava/io/FileSystem;");
        final ResolvedClass resolvedClass = compileUnit.resolveClass(invokedType, analysisStack);
        final ResolvedMethod method = resolvedClass.resolveMethod("<clinit>", Type.getMethodType(Type.VOID_TYPE), analysisStack);

        compileUnit.printStatisticsTo(System.out);

        final JSBackend backend = new JSBackend();
        backend.generateMethod(new PrintWriter(System.out), compileUnit, resolvedClass, method);
    }
}