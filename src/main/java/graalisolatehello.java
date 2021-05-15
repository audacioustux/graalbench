import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.ByteSequence;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

class TimeDiff {
    public enum DiffType {
        NS, US
    };

    public DiffType diffType;
    public long elapse;
    public String label;

    public TimeDiff(String label, DiffType diffType) {
        this.label = label;
        this.diffType = diffType;
        this.elapse = current();
    }

    public long current() {
        long nanotime = System.nanoTime();
        return (diffType == DiffType.NS) ? nanotime : nanotime / 1000;
    }

    public void stop() {
        System.out.println(String.format("%s : %s %s", label, current() - elapse, diffType));
    }
}

public class graalisolatehello {
    public static void main(String[] args) {
        final var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        IsolateThread mainCtx = CurrentIsolate.getCurrentThread();

        final long initialMemory = printMemoryUsage("initial: ", 0);
        for (int i = 1; i <= isolateCount; i++) {
            TimeDiff before_iso_timer = new TimeDiff("isolate launch", TimeDiff.DiffType.US);
            var isolateCtx = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            before_iso_timer.stop();

            ObjectHandle greetHandle = copyString(isolateCtx, "hello");
            TimeDiff iso_call_timer = new TimeDiff("isolate call", TimeDiff.DiffType.US);
            ObjectHandle resultHandle = greet(isolateCtx, mainCtx, greetHandle);
            iso_call_timer.stop();

            String result = ObjectHandles.getGlobal().get(resultHandle);
            ObjectHandles.getGlobal().destroy(resultHandle);

            long currentMemory = Long.parseLong(result);
            System.out.println("isolate " + i + ": " + currentMemory / 1024 + " KByte" + (initialMemory == 0 ? ""
                    : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
            // System.out.println(result);

            // var before_tear = printMemoryUsage("before teardown: ", 0);
            Isolates.tearDownIsolate(isolateCtx);
            // printMemoryUsage("after teardown: ", before_tear);

            // printMemoryUsage("after isolates " + i + ": ", initialMemUsage);
        }
    }

    private static ObjectHandle copyString(IsolateThread targetContext, String sourceString) {
        try (CTypeConversion.CCharPointerHolder cStringHolder = CTypeConversion.toCString(sourceString)) {
            return copyString(targetContext, cStringHolder.get());
        }
    }

    @CEntryPoint
    private static ObjectHandle copyString(@CEntryPoint.IsolateThreadContext IsolateThread renderingContext,
            CCharPointer cString) {
        String targetString = CTypeConversion.toJavaString(cString);
        return ObjectHandles.getGlobal().create(targetString);
    }

    @CEntryPoint
    private static ObjectHandle greet(@CEntryPoint.IsolateThreadContext IsolateThread isolateCtx, IsolateThread mainCtx,
            ObjectHandle greetHandle) {
        // long initialMemory = printMemoryUsage("Rendering isolate initial memory
        // usage: ", 0);

        String greetStr = ObjectHandles.getGlobal().get(greetHandle);
        ObjectHandles.getGlobal().destroy(greetHandle);

        // String greetMsg = String.join(" ", greetStr, "isolate");

        //////////////////
        // load js module
        TimeDiff jsCtx_timer = new TimeDiff("js context launch", TimeDiff.DiffType.US);
        Context jsCtx = Context.newBuilder("js").allowIO(true).build();
        jsCtx_timer.stop();

        String jsSrc = "import { Test } from '/Users/tanjimhossain/IdeaProjects/graalbench/src/main/js/test.mjs';"
                + "const test = new Test();" + "test.hello();";
        try {
            TimeDiff poly_exec_timer1 = new TimeDiff("poly exec launch", TimeDiff.DiffType.US);
            jsCtx.eval(Source.newBuilder("js", jsSrc, "test1.mjs").build());
            poly_exec_timer1.stop();

            TimeDiff poly_exec_timer2 = new TimeDiff("poly exec launch", TimeDiff.DiffType.US);
            jsCtx.eval(Source.newBuilder("js", jsSrc, "test2.mjs").build());
            poly_exec_timer2.stop();

            TimeDiff poly_exec_timer3 = new TimeDiff("poly exec launch", TimeDiff.DiffType.US);
            jsCtx.eval(Source.newBuilder("js", jsSrc, "test3.mjs").build());
            poly_exec_timer3.stop();
        } catch (IOException e) {
        }

        //////////////////
        // load wasm
        // TimeDiff wasmCtx_timer = new TimeDiff("js context launch",
        ////////////////// TimeDiff.DiffType.US);
        // Context wasmCtx = Context.newBuilder("wasm").build();
        // wasmCtx_timer.stop();
        // try (DataInputStream wasmData = new DataInputStream(
        // new
        ////////////////// FileInputStream("/Users/tanjimhossain/IdeaProjects/graalbench/src/main/c/floyd.wasm")))
        ////////////////// {
        // int size = wasmData.readInt();
        // byte[] wasmBytes = new byte[size];
        // wasmData.read(wasmBytes);

        // Source.Builder sourceBuilder = Source.newBuilder("wasm",
        // ByteSequence.create(wasmBytes), "floyd");
        // Source source = sourceBuilder.build();

        // wasmCtx.eval(source);

        // Value mainFunction =
        // wasmCtx.getBindings("wasm").getMember("floyd").getMember("_main");
        // mainFunction.execute();
        // } catch (IOException e) {
        // }
        ///////////////////

        // TimeDiff poly_timer = new TimeDiff("poly context launch",
        // TimeDiff.DiffType.US);
        // Context polyglot = Context.create();
        // poly_timer.stop();

        // //////////////////
        // // inline eval
        // TimeDiff poly_exec_timer = new TimeDiff("poly exec launch",
        // TimeDiff.DiffType.NS);
        // Value array = polyglot.eval("js", "[1,2,42,4]");
        // poly_exec_timer.stop();

        // int result = array.getArrayElement(2).asInt();
        // System.out.println(result);

        // TimeDiff poly_exec_timer2 = new TimeDiff("poly exec launch 2",
        // TimeDiff.DiffType.NS);
        // Value array2 = polyglot.eval("js", "[1,2,42,4]");
        // poly_exec_timer2.stop();

        // int result2 = array2.getArrayElement(2).asInt();
        // System.out.println(result2);
        // ////////////////////

        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        // printMemoryUsage("Rendering isolate final memory usage: ", 0);
        return copyString(mainCtx, Long.toString(currentMemory));
    }

    // @CEntryPoint
    // private static ObjectHandle createByteBuffer(IsolateThread renderingContext,
    // Pointer address, int length) {
    // ByteBuffer direct = CTypeConversion.asByteBuffer(address, length);
    // ByteBuffer copy = ByteBuffer.allocate(length);
    // copy.put(direct).rewind();
    // return ObjectHandles.getGlobal().create(copy);
    // }

    private static long printMemoryUsage(String message, long initialMemory) {
        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        System.out.println(message + currentMemory / 1024 + " KByte"
                + (initialMemory == 0 ? "" : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
        return currentMemory;
    }
}
