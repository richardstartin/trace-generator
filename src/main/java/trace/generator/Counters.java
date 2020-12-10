package trace.generator;

import org.openjdk.jmh.annotations.*;

@AuxCounters(AuxCounters.Type.EVENTS)
@State(Scope.Thread)
public class Counters {

    long success;
    long failed;

    public long success() {
        return success;
    }

    public long failed() {
        return failed;
    }

    @Setup(Level.Iteration)
    public void reset() {
        success = 0;
        failed = 0;
    }
}
