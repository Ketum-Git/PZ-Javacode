// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import java.util.HashSet;
import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.StreamSet;
import pl.mjaron.tinyloki.TinyLoki;
import zombie.core.Core;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.statistics.StatisticManager;
import zombie.util.StringUtils;

public class DebugLogStream extends PrintStream {
    private LogSeverity logSeverity;
    private final PrintStream wrappedStream;
    private final PrintStream wrappedWarnStream;
    private final PrintStream wrappedErrStream;
    private final IDebugLogFormatter formatter;
    private static boolean lokiInit;
    private static TinyLoki loki;
    private static StreamSet logSet;
    private ILogStream errorStream;
    private static final int LEFT_JUSTIFY = 36;
    private final HashSet<String> debugOnceHashSet = new HashSet<>();

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter) {
        this(out, warn, err, formatter, LogSeverity.Off);
    }

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter, LogSeverity logSeverity) {
        super(out);
        this.wrappedStream = out;
        this.wrappedWarnStream = warn;
        this.wrappedErrStream = err;
        this.formatter = formatter;
        this.logSeverity = logSeverity;
        if (!lokiInit) {
            lokiInit = true;
            String lokiUrl = System.getProperty("lokiUrl");
            if (lokiUrl != null) {
                System.out.println("Loki logging enabled.");
                String lokiUser = System.getProperty("lokiUser");
                String lokiPass = System.getProperty("lokiPass");
                loki = TinyLoki.withUrl(lokiUrl)
                    .withThreadExecutor(2000)
                    .withBasicAuth(lokiUser, lokiPass)
                    .withLabels(
                        Labels.of("instance", GameServer.server ? StatisticManager.getInstanceName() : GameClient.username)
                            .l("service_name", GameServer.server ? "pz.server" : "pz.client")
                    )
                    .open();
                logSet = loki.streamSet().open();
            } else {
                loki = null;
                logSet = null;
            }
        }
    }

    private void printToLoki(LogSeverity in_logSeverity, String formattedString) {
        if (logSet != null) {
            switch (in_logSeverity) {
                case Trace:
                    logSet.unknown(formattedString);
                    break;
                case Noise:
                    logSet.verbose(formattedString);
                    break;
                case Debug:
                    logSet.debug(formattedString);
                    break;
                case General:
                    logSet.info(formattedString);
                    break;
                case Warning:
                    logSet.warning(formattedString);
                    break;
                case Error:
                    if (this.errorStream == null) {
                        this.errorStream = loki.stream().l("level", "error").open();
                    }

                    this.errorStream.log(formattedString);
                    break;
                default:
                    logSet.unknown(formattedString);
            }
        }
    }

    public void setLogSeverity(LogSeverity in_newSeverity) {
        this.logSeverity = in_newSeverity;
    }

    public LogSeverity getLogSeverity() {
        return this.logSeverity;
    }

    public PrintStream getWrappedOutStream() {
        return this.wrappedStream;
    }

    public PrintStream getWrappedWarnStream() {
        return this.wrappedWarnStream;
    }

    public PrintStream getWrappedErrStream() {
        return this.wrappedErrStream;
    }

    public IDebugLogFormatter getFormatter() {
        return this.formatter;
    }

    protected void write(PrintStream out, LogSeverity in_logSeverity, String text) {
        if (this.isLogEnabled(in_logSeverity)) {
            String formattedString = this.formatter.format(in_logSeverity, "", true, text);
            if (formattedString != null) {
                out.print(formattedString);
                this.printToLoki(in_logSeverity, formattedString);
            }
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String formatNoParams) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedString = this.formatter.format(logSeverity, "", true, formatNoParams);
            if (formattedString != null) {
                out.println(formattedString);
                this.printToLoki(logSeverity, formattedString);
            }
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String in_format, Object... in_params) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedString = this.formatter.format(logSeverity, "", true, in_format, in_params);
            if (formattedString != null) {
                out.println(formattedString);
                this.printToLoki(logSeverity, formattedString);
            }
        }
    }

    protected void writeWithCallerPrefixln(
        PrintStream out, LogSeverity in_logSeverity, int in_backTraceOffset, boolean in_allowRepeat, Object in_formatNoParams
    ) {
        if (this.isLogEnabled(in_logSeverity)) {
            String callerAffix = generateCallerPrefix_Internal(in_backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
            String formattedString = this.formatter.format(in_logSeverity, callerAffix, in_allowRepeat, "%s", in_formatNoParams);
            if (!in_allowRepeat) {
                if (this.debugOnceHashSet.contains(callerAffix)) {
                    return;
                }

                this.debugOnceHashSet.add(callerAffix);
            }

            if (formattedString != null) {
                out.println(formattedString);
                this.printToLoki(in_logSeverity, formattedString);
            }
        }
    }

    protected void writeWithCallerPrefixln(
        PrintStream out, LogSeverity in_logSeverity, int in_backTraceOffset, boolean in_allowRepeat, String in_format, Object... in_params
    ) {
        if (this.isLogEnabled(in_logSeverity)) {
            String callerAffix = generateCallerPrefix_Internal(in_backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
            String formattedOutputStr = String.format(in_format, in_params);
            String formattedString = this.formatter.format(in_logSeverity, callerAffix, in_allowRepeat, formattedOutputStr);
            if (formattedString != null) {
                out.println(formattedString);
                this.printToLoki(in_logSeverity, formattedString);
            }
        }
    }

    private void writeln(PrintStream out, String in_formatNoParams) {
        this.writeln(out, LogSeverity.General, in_formatNoParams);
    }

    private void writeln(PrintStream out, String in_format, Object... params) {
        this.writeln(out, LogSeverity.General, in_format, params);
    }

    /**
     * Returns the class name and method name prefix of the calling code.
     */
    public static String generateCallerPrefix() {
        return generateCallerPrefix_Internal(1, 0, DebugLog.isLogTraceFileLocationEnabled(), "");
    }

    private static String generateCallerPrefix_Internal(int in_backTraceOffset, int in_leftJustify, boolean in_includeLogTraceFileLocation, String in_suffix) {
        StackTraceElement stackTraceElement = tryGetCallerTraceElement(4 + in_backTraceOffset);
        if (stackTraceElement == null) {
            return StringUtils.leftJustify("(UnknownStack)", in_leftJustify) + in_suffix;
        } else {
            String stackTraceElementString = getStackTraceElementString(stackTraceElement, in_includeLogTraceFileLocation);
            if (in_leftJustify <= 0) {
                String out_result = stackTraceElementString + in_suffix;
                return out_result;
            } else {
                return StringUtils.leftJustify(stackTraceElementString, in_leftJustify) + in_suffix;
            }
        }
    }

    public static StackTraceElement tryGetCallerTraceElement(int depthIdx) {
        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            return stackTraceElements.length <= depthIdx ? null : stackTraceElements[depthIdx];
        } catch (SecurityException var3) {
            return null;
        }
    }

    public static String getStackTraceElementString(StackTraceElement stackTraceElement, boolean in_includeLogTraceFileLocation) {
        if (stackTraceElement == null) {
            return "(UnknownStack)";
        } else {
            String classNameOnly = getUnqualifiedClassName(stackTraceElement.getClassName());
            String methodName = stackTraceElement.getMethodName();
            String comment;
            if (stackTraceElement.isNativeMethod()) {
                comment = " (Native Method)";
            } else if (in_includeLogTraceFileLocation) {
                int lineNo = stackTraceElement.getLineNumber();
                String fileName = stackTraceElement.getFileName();
                comment = String.format("(%s:%d)", fileName, lineNo);
            } else {
                comment = "";
            }

            return classNameOnly + "." + methodName + comment;
        }
    }

    public static String getTopStackTraceString(Throwable ex) {
        if (ex == null) {
            return "Null Exception";
        } else {
            StackTraceElement[] stackTrace = ex.getStackTrace();
            if (stackTrace != null && stackTrace.length != 0) {
                StackTraceElement topElement = stackTrace[0];
                return getStackTraceElementString(topElement, true);
            } else {
                return "No Stack Trace Available";
            }
        }
    }

    public void printStackTrace() {
        this.printStackTrace(0, null);
    }

    public void printStackTrace(String message) {
        this.printStackTrace(0, message);
    }

    public void printStackTrace(int depth) {
        this.printStackTrace(depth, null);
    }

    public void printStackTrace(String message, int depth) {
        this.printStackTrace(depth, message);
    }

    private void printStackTrace(int depth, String message) {
        if (message != null) {
            this.wrappedErrStream.println(message);
            if (logSet != null) {
                logSet.fatal().log(message, Labels.of("type", "StackTraceMessage"));
            }
        }

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        this.wrappedErrStream.println(StackTraceContainer.getStackTraceString(stackTraceElements, "\t", depth + 3, -1));
        if (logSet != null) {
            logSet.fatal().log(StackTraceContainer.getStackTraceString(stackTraceElements, "\t", depth + 3, -1), Labels.of("type", "StackTrace"));
        }
    }

    private static String getUnqualifiedClassName(String className) {
        String classNameOnly = className;
        int lastIndexOf = className.lastIndexOf(46);
        if (lastIndexOf > -1 && lastIndexOf < className.length() - 1) {
            classNameOnly = className.substring(lastIndexOf + 1);
        }

        return classNameOnly;
    }

    public boolean isEnabled() {
        return this.getLogSeverity() != LogSeverity.Off;
    }

    public boolean isLogEnabled(LogSeverity in_logSeverity) {
        return !this.isEnabled() ? false : in_logSeverity.ordinal() >= this.getLogSeverity().ordinal();
    }

    public void trace(Object in_formatNoParams) {
        this.trace(1, in_formatNoParams);
    }

    public void trace(String in_format, Object... in_params) {
        this.trace(1, in_format, in_params);
    }

    public void debugln(Object in_formatNoParams) {
        this.debugln(1, in_formatNoParams);
    }

    public void debugln(String in_format, Object... in_params) {
        this.debugln(1, in_format, in_params);
    }

    public void debugOnceln(Object in_formatNoParams) {
        this.debugOnceln(1, in_formatNoParams);
    }

    public void debugOnceln(String in_format, Object... in_params) {
        this.debugOnceln(1, in_format, in_params);
    }

    public void noise(Object in_formatNoParams) {
        this.noise(1, in_formatNoParams);
    }

    public void noise(String in_format, Object... in_params) {
        this.noise(1, in_format, in_params);
    }

    /**
     * Prints an object to the Warning stream.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param in_formatNoParams The Object to be printed
     */
    public void warn(Object in_formatNoParams) {
        this.warn(1, in_formatNoParams);
    }

    public void warn(String in_format, Object... in_params) {
        this.warn(1, in_format, in_params);
    }

    public void warnOnce(Object in_formatNoParams) {
        this.warnOnce(1, in_formatNoParams);
    }

    public void warnOnce(String in_format, Object... in_params) {
        this.warnOnce(1, in_format, in_params);
    }

    /**
     * Prints an object to the Error stream.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param in_formatNoParams The Object to be printed
     */
    public void error(Object in_formatNoParams) {
        this.error(1, in_formatNoParams);
    }

    public void error(String in_format, Object... in_params) {
        this.error(1, in_format, in_params);
    }

    public void debugln(int in_backTraceOffset, Object in_formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, in_backTraceOffset + 1, true, in_formatNoParams);
        }
    }

    public void debugln(int in_backTraceOffset, String in_format, Object... in_params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, in_backTraceOffset + 1, true, in_format, in_params);
        }
    }

    public void debugOnceln(int in_backTraceOffset, Object in_formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, in_backTraceOffset + 1, false, in_formatNoParams);
        }
    }

    public void debugOnceln(int in_backTraceOffset, String in_format, Object... in_params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, in_backTraceOffset + 1, false, in_format, in_params);
        }
    }

    public void noise(int in_backTraceOffset, Object in_formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, in_backTraceOffset + 1, true, in_formatNoParams);
        }
    }

    public void noise(int in_backTraceOffset, String in_format, Object... in_params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, in_backTraceOffset + 1, true, in_format, in_params);
        }
    }

    public void warn(int in_backTraceOffset, Object in_formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, in_backTraceOffset + 1, true, in_formatNoParams);
    }

    public void warn(int in_backTraceOffset, String in_format, Object... in_params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, in_backTraceOffset + 1, true, in_format, in_params);
    }

    public void error(int in_backTraceOffset, Object in_formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, in_backTraceOffset + 1, true, in_formatNoParams);
    }

    public void error(int in_backTraceOffset, String in_format, Object... in_params) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, in_backTraceOffset + 1, true, in_format, in_params);
    }

    public void trace(int in_backTraceOffset, Object in_formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, in_backTraceOffset + 1, true, in_formatNoParams);
    }

    public void trace(int in_backTraceOffset, String in_format, Object... in_params) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, in_backTraceOffset + 1, true, in_format, in_params);
    }

    public void warnOnce(int in_backTraceOffset, Object in_formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, in_backTraceOffset + 1, false, in_formatNoParams);
    }

    public void warnOnce(int in_backTraceOffset, String in_format, Object... in_params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, in_backTraceOffset + 1, false, in_format, in_params);
    }

    /**
     * Prints a boolean value.  The string produced by String.valueOf(boolean) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param b The boolean to be printed
     */
    @Override
    public void print(boolean b) {
        this.write(this.wrappedStream, LogSeverity.General, b ? "true" : "false");
    }

    /**
     * Prints a character.  The character is translated into one or more bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param c The char to be printed
     */
    @Override
    public void print(char c) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(c));
    }

    /**
     * Prints an integer.  The string produced by String.valueOf(int) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param i The int to be printed
     */
    @Override
    public void print(int i) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(i));
    }

    /**
     * Prints a long integer.  The string produced by String.valueOf(long) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param l The long to be printed
     */
    @Override
    public void print(long l) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(l));
    }

    /**
     * Prints a floating-point number.  The string produced by String.valueOf(float) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param f The float to be printed
     */
    @Override
    public void print(float f) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(f));
    }

    /**
     * Prints a double-precision floating-point number.  The string produced by
     *  String.valueOf(double) is translated into
     *  bytes according to the platform's default character encoding, and these
     *  bytes are written in exactly the manner of the PrintStream.write(int) method.
     * 
     * @param d The double to be printed
     */
    @Override
    public void print(double d) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(d));
    }

    /**
     * Prints a string.  If the argument is null then the string
     *  "null" is printed.  Otherwise, the string's characters are
     *  converted into bytes according to the platform's default character
     *  encoding, and these bytes are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param s The String to be printed
     */
    @Override
    public void print(String s) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(s));
    }

    /**
     * Prints an object.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param obj The Object to be printed
     */
    @Override
    public void print(Object obj) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(obj));
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        this.write(this.wrappedStream, LogSeverity.General, String.format(format, args));
        return this;
    }

    /**
     * Terminates the current line by writing the line separator string.  The
     *  line separator string is defined by the system property
     *  line.separator, and is not necessarily a single newline
     *  character ('\n').
     */
    @Override
    public void println() {
        this.writeln(this.wrappedStream, "");
    }

    /**
     * Prints a boolean and then terminate the line.  This method behaves as
     *  though it invokes print(boolean) and then
     *  println().
     * 
     * @param x The boolean to be printed
     */
    @Override
    public void println(boolean x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a character and then terminate the line.  This method behaves as
     *  though it invokes print(char) and then
     *  println().
     * 
     * @param x The char to be printed.
     */
    @Override
    public void println(char x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints an integer and then terminate the line.  This method behaves as
     *  though it invokes print(int) and then
     *  println().
     * 
     * @param x The int to be printed.
     */
    @Override
    public void println(int x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a long and then terminate the line.  This method behaves as
     *  though it invokes print(long) and then
     *  println().
     * 
     * @param x a The long to be printed.
     */
    @Override
    public void println(long x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a float and then terminate the line.  This method behaves as
     *  though it invokes print(float) and then
     *  println().
     * 
     * @param x The float to be printed.
     */
    @Override
    public void println(float x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a double and then terminate the line.  This method behaves as
     *  though it invokes print(double) and then
     *  println().
     * 
     * @param x The double to be printed.
     */
    @Override
    public void println(double x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a character and then terminate the line.  This method behaves as
     *  though it invokes print(char) and then
     *  println().
     * 
     * @param x The char to be printed.
     */
    @Override
    public void println(char[] x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a String and then terminate the line.  This method behaves as
     *  though it invokes print(String) and then
     *  println().
     * 
     * @param x The String to be printed.
     */
    @Override
    public void println(String x) {
        this.writeln(this.wrappedStream, x);
    }

    /**
     * Prints an Object and then terminate the line.  This method calls
     *  at first String.valueOf(x) to get the printed object's string value,
     *  then behaves as
     *  though it invokes print(String) and then
     *  println().
     * 
     * @param x The Object to be printed.
     */
    @Override
    public void println(Object x) {
        this.writeln(this.wrappedStream, "%s", x);
    }

    public void println(String in_format, Object... in_params) {
        this.writeln(this.wrappedStream, LogSeverity.General, in_format, in_params);
    }

    public void printException(Throwable ex, String errorMessage, LogSeverity severity) {
        this.printException(ex, errorMessage, generateCallerPrefix(), severity);
    }

    public void printException(Throwable ex, String errorMessage, String callerPrefix, LogSeverity severity) {
        if (ex == null) {
            this.warn("Null exception passed.");
        } else if (this.isLogEnabled(severity)) {
            PrintStream outStream;
            boolean includeStack;
            switch (severity) {
                case Trace:
                case General:
                    outStream = this.wrappedStream;
                    includeStack = false;
                    break;
                case Noise:
                case Debug:
                default:
                    this.error("Unhandled LogSeverity: %s. Defaulted to Error.", String.valueOf(severity));
                case Error:
                    outStream = this.wrappedErrStream;
                    includeStack = true;
                    break;
                case Warning:
                    outStream = this.wrappedWarnStream;
                    includeStack = false;
            }

            if (includeStack) {
                StringBuilder sb = new StringBuilder();
                if (errorMessage != null) {
                    sb.append(
                        String.format(
                            "%s> Exception thrown%s\t%s at %s. Message: %s",
                            callerPrefix,
                            System.lineSeparator(),
                            ex.toString(),
                            getTopStackTraceString(ex),
                            errorMessage
                        )
                    );
                } else {
                    sb.append(
                        String.format("%s> Exception thrown%s\t%s at %s.", callerPrefix, System.lineSeparator(), ex.toString(), getTopStackTraceString(ex))
                    );
                }

                sb.append(System.lineSeparator());
                StackTraceContainer.getStackTraceString(sb, ex, "Stack trace:", "\t", 0, -1);
                this.write(outStream, severity, sb.toString());
            } else if (errorMessage != null) {
                String message = String.format(
                    "%s> Exception thrown %s at %s. Message: %s", callerPrefix, ex.toString(), getTopStackTraceString(ex), errorMessage
                );
                this.writeln(outStream, severity, message);
            } else {
                String message = String.format("%s> Exception thrown %s at %s.", callerPrefix, ex.toString(), getTopStackTraceString(ex));
                this.writeln(outStream, severity, message);
            }
        }
    }
}
