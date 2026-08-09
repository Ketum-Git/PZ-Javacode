// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.logger;

import java.util.Map;
import java.util.Objects;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public class Slf4jBridge implements SLF4JServiceProvider {
    private static final Map<Level, LogSeverity> LOG_SEVERITY_BY_LEVEL = Map.of(
        Level.TRACE,
        LogSeverity.Trace,
        Level.DEBUG,
        LogSeverity.Debug,
        Level.INFO,
        LogSeverity.General,
        Level.WARN,
        LogSeverity.Warning,
        Level.ERROR,
        LogSeverity.Error
    );

    @Override
    public ILoggerFactory getLoggerFactory() {
        return name -> new LegacyAbstractLogger() {
            {
                Objects.requireNonNull(Slf4jBridge.this);
            }

            @Override
            public boolean isTraceEnabled() {
                return DebugType.General.isEnabled(LogSeverity.Trace);
            }

            @Override
            public boolean isDebugEnabled() {
                return DebugType.General.isEnabled(LogSeverity.Debug);
            }

            @Override
            public boolean isInfoEnabled() {
                return DebugType.General.isEnabled(LogSeverity.General);
            }

            @Override
            public boolean isWarnEnabled() {
                return DebugType.General.isEnabled(LogSeverity.Warning);
            }

            @Override
            public boolean isErrorEnabled() {
                return DebugType.General.isEnabled(LogSeverity.Error);
            }

            @Override
            protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] args, Throwable t) {
                msg = msg.replace("%", "%%").replace("{}", "%s");
                if (t == null) {
                    DebugType.General.routedWrite(3, Slf4jBridge.LOG_SEVERITY_BY_LEVEL.get(level), msg.formatted(args));
                } else {
                    DebugType.General.printException(t, Slf4jBridge.LOG_SEVERITY_BY_LEVEL.get(level), msg, args);
                }
            }

            @Override
            protected String getFullyQualifiedCallerName() {
                return null;
            }
        };
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.0";
    }

    @Override
    public void initialize() {
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return null;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return null;
    }
}
