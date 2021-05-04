import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.function.CEntryPoint;

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
        var isolateCount = (args.length != 0) ? Integer.parseInt(args[0]) : 1;

        PerformanceTest timer = new PerformanceTest();
        timer.startNanoTimestamp();
        for (int i = 0; i < isolateCount; i++) {
            Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
        }
        System.out.println(timer.stopNanoTimestamp());
    }
}
