import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

class PerformanceTest {
    //Declaration of initial timestamps based on time unit
    public long nanoElapse;
    public long milliElapse;

    //Nanoseconds at the initial flow start
    public void startNanoTimestamp() {
        nanoElapse = System.nanoTime();
    }
    //Milliseconds at the initial flow start
    public void startMilliTimestamp() {
        milliElapse = System.currentTimeMillis();
    }
    //Calculating nanoseconds (post-execution)
    public long stopNanoTimestamp() {
        return (System.nanoTime() - nanoElapse);
    }
    //Calculating milliseconds (post-execution)
    public long stopMilliTimestamp() {
        return (System.currentTimeMillis() - milliElapse);
    }
}

public class graalisolatehello {
    public static void main(String[] args) {
        final var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        IsolateThread mainCtx = CurrentIsolate.getCurrentThread();

        PerformanceTest timer = new PerformanceTest();
        timer.startNanoTimestamp();
        for (int i = 0; i < isolateCount; i++) {
            var isolateCtx = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            ObjectHandle greetHandle = copyString(isolateCtx, "hello");

            Isolates.tearDownIsolate(isolateCtx);
        }
        System.out.println(timer.stopNanoTimestamp());
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
    private static ObjectHandle plotAsSVG(@CEntryPoint.IsolateThreadContext IsolateThread isolateCtx, IsolateThread mainCtx, ObjectHandle greetHandle) {
        String greetStr = ObjectHandles.getGlobal().get(greetHandle);
        ObjectHandles.getGlobal().destroy(greetHandle);

        String greetingMsg = String.join(" ", greetStr, "isolate");

        ObjectHandle byteBufferHandle;
        try (PinnedObject pin = PinnedObject.create(greetingMsg)) {
            byteBufferHandle = createByteBuffer(isolateCtx, pin.addressOfArrayElement(0), greetingMsg.length());
        }

        return byteBufferHandle;
    }
}
