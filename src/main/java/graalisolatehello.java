import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.Pointer;
import java.nio.ByteBuffer;

class TimeDiff {
    public enum DiffType { US, MS };
    public DiffType diffType;
    public long elapse;
    public String label;

    public TimeDiff(String label, DiffType diffType) {
        this.label = label;
        this.diffType = diffType;
        this.elapse = current();
    }

    public long current() { return (diffType == DiffType.US) ? System.nanoTime() : System.currentTimeMillis(); }
    public void stop() { System.out.println(String.format("%s : %s %s", label, current() - elapse, diffType)); }
}

public class graalisolatehello {
    public static void main(String[] args) {
        final var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        IsolateThread mainCtx = CurrentIsolate.getCurrentThread();

        for (int i = 0; i < isolateCount; i++) {
            TimeDiff spawn_timer = new TimeDiff("isolate spawn time", TimeDiff.DiffType.US);
            var isolateCtx = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            spawn_timer.stop();

            ObjectHandle greetHandle = copyString(isolateCtx, "hello");

            ObjectHandle resultHandle = greet(isolateCtx, mainCtx, greetHandle);

            ByteBuffer result = ObjectHandles.getGlobal().get(resultHandle);
            ObjectHandles.getGlobal().destroy(resultHandle);

            // System.out.println(new String(result.array()));

            TimeDiff teardown_timer = new TimeDiff("isolate teardown time", TimeDiff.DiffType.US);
            Isolates.tearDownIsolate(isolateCtx);
            teardown_timer.stop();
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
