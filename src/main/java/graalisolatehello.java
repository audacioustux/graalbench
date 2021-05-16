import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import java.lang.management.ManagementFactory;

public class graalisolatehello {
    public static void main(String[] args) {
        final var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        IsolateThread mainCtx = CurrentIsolate.getCurrentThread();

        final long initialMemory = printMemoryUsage("initial: ", 0);
        for (int i = 1; i <= isolateCount; i++) {
            TimeDiff iso_launch_timer = new TimeDiff("isolate launch", TimeDiff.DiffType.US);
            var isolateCtx = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            iso_launch_timer.stop();

            ObjectHandle greetHandle = copyString(isolateCtx, "hello");
            TimeDiff iso_call_timer = new TimeDiff("isolate call", TimeDiff.DiffType.US);
            ObjectHandle resultHandle = greet(isolateCtx, mainCtx, greetHandle);
            iso_call_timer.stop();

            String result = ObjectHandles.getGlobal().get(resultHandle);
            ObjectHandles.getGlobal().destroy(resultHandle);

            long currentMemory = Long.parseLong(result);
            System.out.println("isolate " + i + ": " + currentMemory / 1024 + " KByte" + (initialMemory == 0 ? ""
                    : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));

            TimeDiff iso_tear_timer = new TimeDiff("isolate tear", TimeDiff.DiffType.US);
            Isolates.tearDownIsolate(isolateCtx);
            iso_tear_timer.stop();
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
        ObjectHandles.getGlobal().destroy(greetHandle);

        new PolyglotEval();

        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return copyString(mainCtx, Long.toString(currentMemory));
    }

    private static long printMemoryUsage(String message, long initialMemory) {
        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        System.out.println(message + currentMemory / 1024 + " KByte"
                + (initialMemory == 0 ? "" : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
        return currentMemory;
    }
}
