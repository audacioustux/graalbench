import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.Pointer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

            ObjectHandle resultHandle = greet(isolateCtx, mainCtx, greetHandle);

            ByteBuffer result = ObjectHandles.getGlobal().get(resultHandle);
            ObjectHandles.getGlobal().destroy(resultHandle);

            System.out.println(new String(result.array()));

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
    private static ObjectHandle greet(@CEntryPoint.IsolateThreadContext IsolateThread isolateCtx, IsolateThread mainCtx, ObjectHandle greetHandle) {
        String greetStr = ObjectHandles.getGlobal().get(greetHandle);
        ObjectHandles.getGlobal().destroy(greetHandle);

        byte[] greetMsgBytes = String.join(" ", greetStr, "isolate").getBytes();

        ObjectHandle byteBufferHandle;
        try (PinnedObject pin = PinnedObject.create(greetMsgBytes)) {
            byteBufferHandle = createByteBuffer(mainCtx, pin.addressOfArrayElement(0), greetMsgBytes.length);
        }

        return byteBufferHandle;
    }

    @CEntryPoint
    private static ObjectHandle createByteBuffer(IsolateThread renderingContext, Pointer address, int length) {
        ByteBuffer direct = CTypeConversion.asByteBuffer(address, length);
        ByteBuffer copy = ByteBuffer.allocate(length);
        copy.put(direct).rewind();
        return ObjectHandles.getGlobal().create(copy);
    }
}
