import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.word.Pointer;
import java.nio.ByteBuffer;
import java.lang.management.ManagementFactory;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Scanner;

class TimeDiff {
    public enum DiffType { NS, US };
    public DiffType diffType;
    public long elapse;
    public String label;

    public TimeDiff(String label, DiffType diffType) {
        this.label = label;
        this.diffType = diffType;
        this.elapse = current();
    }

    public long current() { return (diffType == DiffType.NS) ? System.nanoTime() : System.currentTimeMillis() * 1000; }
    public void stop() { System.out.println(String.format("%s : %s %s", label, current() - elapse, diffType)); }
}

public class graalisolatehello {
    public static void main(String[] args) {
        final var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        IsolateThread mainCtx = CurrentIsolate.getCurrentThread();

        final long initialMemory = printMemoryUsage("initial: ", 0);
        for (int i = 1; i <= isolateCount; i++) {
            TimeDiff before_iso_timer = new TimeDiff("before isolate launch", TimeDiff.DiffType.NS);
            var isolateCtx = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            before_iso_timer.stop();

            ObjectHandle greetHandle = copyString(isolateCtx, "hello");
            TimeDiff iso_call_timer = new TimeDiff("isolate call", TimeDiff.DiffType.NS);
            ObjectHandle resultHandle = greet(isolateCtx, mainCtx, greetHandle);
            iso_call_timer.stop();


            String result = ObjectHandles.getGlobal().get(resultHandle);
            ObjectHandles.getGlobal().destroy(resultHandle);

            long currentMemory = Long.parseLong(result);
            System.out.println("isolate " + i + ": "+ currentMemory / 1024 + " KByte" + (initialMemory == 0 ? "" : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
//            System.out.println(result);

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
    private static ObjectHandle copyString(@CEntryPoint.IsolateThreadContext IsolateThread renderingContext, CCharPointer cString) {
        String targetString = CTypeConversion.toJavaString(cString);
        return ObjectHandles.getGlobal().create(targetString);
    }

    @CEntryPoint
    private static ObjectHandle greet(@CEntryPoint.IsolateThreadContext IsolateThread isolateCtx, IsolateThread mainCtx, ObjectHandle greetHandle) {
        // long initialMemory = printMemoryUsage("Rendering isolate initial memory usage: ", 0);

        String greetStr = ObjectHandles.getGlobal().get(greetHandle);
        ObjectHandles.getGlobal().destroy(greetHandle);

        // String greetMsg = String.join(" ", greetStr, "isolate");

        TimeDiff poly_timer = new TimeDiff("poly context launch", TimeDiff.DiffType.NS);
        Context polyglot = Context.create();
        poly_timer.stop();

        TimeDiff poly_exec_timer = new TimeDiff("poly exec launch", TimeDiff.DiffType.NS);
        Value array = polyglot.eval("js", "[1,2,42,4]");
        poly_exec_timer.stop();

        int result = array.getArrayElement(2).asInt();
        System.out.println(result);

        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//        printMemoryUsage("Rendering isolate final memory usage: ", 0);
        return copyString(mainCtx, Long.toString(currentMemory));
    }

//    @CEntryPoint
//    private static ObjectHandle createByteBuffer(IsolateThread renderingContext, Pointer address, int length) {
//        ByteBuffer direct = CTypeConversion.asByteBuffer(address, length);
//        ByteBuffer copy = ByteBuffer.allocate(length);
//        copy.put(direct).rewind();
//        return ObjectHandles.getGlobal().create(copy);
//    }

    private static long printMemoryUsage(String message, long initialMemory) {
        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        System.out.println(message + currentMemory / 1024 + " KByte" + (initialMemory == 0 ? "" : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
        return currentMemory;
    }
}
