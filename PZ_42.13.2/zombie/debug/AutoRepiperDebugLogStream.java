// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import java.util.Locale;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class AutoRepiperDebugLogStream extends DebugLogStream {
    private final DebugType defaultDebugType;

    public AutoRepiperDebugLogStream(PrintStream defaultOut, DebugType defaultDebugType, LogSeverity logSeverity) {
        super(defaultOut, null, null, null, logSeverity);
        this.defaultDebugType = defaultDebugType;
    }

    public DebugType getDefaultDebugType() {
        return this.defaultDebugType;
    }

    public AutoRepiperDebugLogStream.RepiperPacket parseRepiper(Object in_object, LogSeverity in_defaultLogSeverity) {
        AutoRepiperDebugLogStream.RepiperPacket repiper = AutoRepiperDebugLogStream.RepiperPacket.alloc(
            in_object, in_defaultLogSeverity, this.getDefaultDebugType()
        );
        this.parseRepipeDirection(repiper);
        this.parseRepipedLogSeverity(repiper);
        return repiper;
    }

    protected void parseRepipeDirection(AutoRepiperDebugLogStream.RepiperPacket inout_repiper) {
        if (inout_repiper.getParsedObject() instanceof String text) {
            int indexOfColon = text.indexOf(58);
            if (indexOfColon > 0) {
                String debugTypeStr = text.substring(0, indexOfColon);
                if (debugTypeStr.indexOf(10) <= -1 && debugTypeStr.indexOf(32) <= -1 && debugTypeStr.indexOf(9) <= -1) {
                    for (DebugType debugType : DebugType.values()) {
                        if (debugType.name().equalsIgnoreCase(debugTypeStr)) {
                            inout_repiper.repipeDirection = debugType;
                            inout_repiper.parsedText = text.substring(indexOfColon + 1);
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void parseRepipedLogSeverity(AutoRepiperDebugLogStream.RepiperPacket inout_repiper) {
        if (inout_repiper.getParsedObject() instanceof String text) {
            int startAt = 0;

            for (int i = 0; i < 2; i++) {
                int indexOfColon = text.indexOf(58, startAt);
                if (indexOfColon <= 0) {
                    break;
                }

                String logSeverityStr = text.substring(startAt, indexOfColon);
                LogSeverity parsedLogSeverity = this.parseRepipedLogSeverityExact(logSeverityStr);
                if (parsedLogSeverity != null) {
                    inout_repiper.logSeverity = parsedLogSeverity;
                    inout_repiper.parsedText = text.substring(indexOfColon + 1);
                    return;
                }

                startAt = indexOfColon + 1;
            }
        }
    }

    private LogSeverity parseRepipedLogSeverityExact(String in_logSeverityStr) {
        String var2 = in_logSeverityStr.toUpperCase(Locale.ROOT);

        return switch (var2) {
            case "TRACE" -> LogSeverity.Trace;
            case "NOISE" -> LogSeverity.Noise;
            case "DEBUG" -> LogSeverity.Debug;
            case "WARN" -> LogSeverity.Warning;
            case "ERROR" -> LogSeverity.Error;
            default -> null;
        };
    }

    protected PrintStream getRepipedStream(PrintStream in_stream, DebugType in_repipedTo) {
        return this.getRepipedStream(in_stream, in_repipedTo.getLogStream());
    }

    protected PrintStream getRepipedStream(PrintStream in_stream, DebugLogStream in_repipedTo) {
        if (in_stream == this.getWrappedOutStream()) {
            return in_repipedTo.getWrappedOutStream();
        } else if (in_stream == this.getWrappedWarnStream()) {
            return in_repipedTo.getWrappedWarnStream();
        } else {
            return in_stream == this.getWrappedErrStream() ? in_repipedTo.getWrappedErrStream() : in_repipedTo.getWrappedOutStream();
        }
    }

    @Override
    protected void write(PrintStream out, LogSeverity in_logSeverity, String text) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(text, in_logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().write(repipedOutStream, repipedLogSeverity, text);
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity in_logSeverity, String formatNoParams) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(formatNoParams, in_logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity in_logSeverity, String in_format, Object... params) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(in_format, in_logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString(), params);
        }
    }

    @Override
    protected void writeWithCallerPrefixln(
        PrintStream out, LogSeverity in_logSeverity, int in_backTraceOffset, boolean in_allowRepeat, Object in_formatNoParams
    ) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(in_formatNoParams, in_logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream()
                .writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, in_backTraceOffset + 1, in_allowRepeat, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeWithCallerPrefixln(
        PrintStream out, LogSeverity in_logSeverity, int in_backTraceOffset, boolean in_allowRepeat, String in_format, Object... in_params
    ) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(in_format, in_logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream()
                .writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, in_backTraceOffset, in_allowRepeat, repiperPacket.getParsedString(), in_params);
        }
    }

    public static class RepiperPacket extends PooledObject implements AutoCloseable {
        private String parsedText;
        private Object inObject;
        private LogSeverity logSeverity;
        private DebugType repipeDirection;
        private static final Pool<AutoRepiperDebugLogStream.RepiperPacket> s_pool = new Pool<>(AutoRepiperDebugLogStream.RepiperPacket::new);

        private RepiperPacket() {
        }

        @Override
        public void onReleased() {
            this.parsedText = null;
            this.inObject = null;
            this.logSeverity = null;
            this.repipeDirection = null;
        }

        public Object getParsedObject() {
            return this.parsedText != null ? this.parsedText : this.inObject;
        }

        public String getParsedString() {
            return this.parsedText != null ? this.parsedText : String.valueOf(this.inObject);
        }

        public static AutoRepiperDebugLogStream.RepiperPacket alloc(Object in_object, LogSeverity in_defaultLogSeverity, DebugType in_defaultDebugType) {
            AutoRepiperDebugLogStream.RepiperPacket newInstance = s_pool.alloc();
            newInstance.parsedText = null;
            newInstance.inObject = in_object;
            newInstance.logSeverity = in_defaultLogSeverity;
            newInstance.repipeDirection = in_defaultDebugType;
            return newInstance;
        }

        @Override
        public void close() {
            Pool.tryRelease(this);
        }
    }
}
