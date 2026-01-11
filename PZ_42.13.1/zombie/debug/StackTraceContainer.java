// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class StackTraceContainer {
    private final int depthStart;
    private final int depthCount;
    private final StackTraceElement[] stackTraceElements;
    private final String indent;

    public StackTraceContainer(StackTraceElement[] in_stackTraceElements, String in_indent, int in_depthStart, int in_depthCount) {
        this.depthStart = Math.max(in_depthStart, 0);
        this.depthCount = in_depthCount;
        this.stackTraceElements = in_stackTraceElements;
        this.indent = in_indent;
    }

    @Override
    public String toString() {
        return getStackTraceString(this.stackTraceElements, this.indent, this.depthStart, this.depthCount);
    }

    public static String getStackTraceString(StackTraceElement[] in_stackTraceElements, String in_indent, int in_depthStart, int in_depthCount) {
        StringBuilder result = new StringBuilder();
        int depthCount = in_depthCount <= 0 ? in_stackTraceElements.length : in_depthCount;
        int d = in_depthStart;

        for (int numElements = 0; numElements < depthCount && d < in_stackTraceElements.length; d++) {
            StackTraceElement stackTraceElement = in_stackTraceElements[d];
            String stackTraceElementString = stackTraceElement.toString();
            if (numElements <= 0 || !stackTraceElementString.startsWith("zombie.core.profiling.PerformanceProbes$")) {
                if (numElements > 0) {
                    result.append("\r\n");
                }

                result.append(in_indent).append(stackTraceElementString);
                numElements++;
            }
        }

        return result.toString();
    }

    public static StringBuilder getStackTraceString(StringBuilder result, Throwable throwable, String in_indent, int in_depthStart, int in_depthCount) {
        StackTraceElement[] trace = throwable.getStackTrace();
        int depthCount = in_depthCount <= 0 ? trace.length : in_depthCount;
        int d = in_depthStart;

        for (int numElements = 0; numElements < depthCount && d < trace.length; d++) {
            StackTraceElement stackTraceElement = trace[d];
            String stackTraceElementString = stackTraceElement.toString();
            if (numElements <= 0 || !stackTraceElementString.startsWith("zombie.core.profiling.PerformanceProbes$")) {
                result.append(in_indent).append(stackTraceElementString).append(System.lineSeparator());
                numElements++;
            }
        }

        return result;
    }

    public static StringBuilder getStackTraceString(
        StringBuilder result, Throwable throwable, String caption, String prefix, int in_depthStart, int in_depthCount
    ) {
        Set<Throwable> done = Collections.newSetFromMap(new IdentityHashMap<>());
        done.add(throwable);
        if (caption != null) {
            result.append(prefix).append(caption).append(System.lineSeparator());
        }

        getStackTraceString(result, throwable, prefix + "\t", in_depthStart, in_depthCount);
        StackTraceElement[] trace = throwable.getStackTrace();

        for (Throwable se : throwable.getSuppressed()) {
            getEnclosedStackTraceString(result, trace, "Suppressed: ", prefix, se, 0, -1, done);
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            getEnclosedStackTraceString(result, trace, "Caused by: ", prefix, cause, 0, -1, done);
        }

        return result;
    }

    public static StringBuilder getEnclosedStackTraceString(
        StringBuilder result,
        StackTraceElement[] enclosingTrace,
        String caption,
        String prefix,
        Throwable throwable,
        int in_depthStart,
        int in_depthCount,
        Set<Throwable> done
    ) {
        if (done.contains(throwable)) {
            result.append(prefix).append(caption).append("[CIRCULAR REFERENCE: ").append(throwable).append("]");
            return result;
        } else {
            done.add(throwable);
            StackTraceElement[] trace = throwable.getStackTrace();
            int m = trace.length - 1;

            for (int n = enclosingTrace.length - 1; m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n]); n--) {
                m--;
            }

            int framesInCommon = trace.length - 1 - m;
            result.append(prefix).append(caption).append(throwable).append(System.lineSeparator());
            getStackTraceString(result, throwable, prefix + "\t", 0, m + 1);
            if (framesInCommon != 0) {
                result.append(prefix).append("\t").append("... ").append(framesInCommon).append(" more").append(System.lineSeparator());
            }

            for (Throwable se : throwable.getSuppressed()) {
                getEnclosedStackTraceString(result, trace, "Suppressed: ", prefix, se, 0, -1, done);
            }

            Throwable cause = throwable.getCause();
            if (cause != null) {
                getEnclosedStackTraceString(result, trace, "Caused by: ", prefix, cause, 0, -1, done);
            }

            return result;
        }
    }
}
