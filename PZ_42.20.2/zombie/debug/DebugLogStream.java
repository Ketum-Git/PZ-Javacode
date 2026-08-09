// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import java.util.HashSet;
import zombie.core.Core;
import zombie.util.StringUtils;
import zombie.util.lambda.PZOptional;
import zombie.util.list.PZArrayUtil;

public class DebugLogStream extends PrintStream {
    private LogSeverity logSeverity;
    private final DebugType debugType;
    private final PrintStream wrappedStream;
    private final PrintStream wrappedWarnStream;
    private final PrintStream wrappedErrStream;
    private final IDebugLogFormatter formatter;
    private static final int LEFT_JUSTIFY = 36;
    private final HashSet<String> debugOnceHashSet = new HashSet<>();

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, LogSeverity logSeverity) {
        super(out);
        this.wrappedStream = out;
        this.wrappedWarnStream = warn;
        this.wrappedErrStream = err;
        this.formatter = this::formatLogStringForConsole;
        this.logSeverity = logSeverity;
        this.debugType = null;
    }

    private String formatLogStringForConsole(DebugType debugType, LogSeverity logSeverity, String affix, Object outputString) {
        return DebugLog.getInstance().formatLogStringForConsole(debugType, logSeverity, affix, outputString);
    }

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, DebugType debugType) {
        super(out);
        this.wrappedStream = out;
        this.wrappedWarnStream = warn;
        this.wrappedErrStream = err;
        this.formatter = null;
        this.logSeverity = null;
        this.debugType = debugType;
    }

    public void setLogSeverity(LogSeverity newSeverity) {
        if (this.debugType != null) {
            this.debugType.setLogSeverity(newSeverity);
        } else {
            this.logSeverity = newSeverity;
        }
    }

    public DebugType getDebugType() {
        return this.debugType;
    }

    public LogSeverity getLogSeverity() {
        return this.debugType != null ? this.debugType.getLogSeverity() : this.logSeverity;
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
        return PZOptional.ifPresent(this.debugType, this.formatter, DebugType::getFormatter);
    }

    protected void write(PrintStream out, LogSeverity logSeverity, Object text) {
        if (this.isLogEnabled(logSeverity)) {
            out.print(this.getFormatter().format(this.debugType, logSeverity, "", text));
            DebugLog.getInstance().echoToLogFiles(this.debugType, logSeverity, "", String.valueOf(text));
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, Object format, Object... params) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedOutputStr = getFormattedOutputStr(format, params);
            out.println(this.getFormatter().format(this.debugType, logSeverity, "", formattedOutputStr));
            DebugLog.getInstance().echoToLogFiles(this.debugType, logSeverity, "", formattedOutputStr);
        }
    }

    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, Object format, Object... params) {
        if (this.isLogEnabled(logSeverity)) {
            String callerAffix = generateCallerPrefix_Internal(backTraceOffset, 36, DebugLog.getInstance().isLogTraceFileLocationEnabled());
            if (!allowRepeat) {
                synchronized (this.debugOnceHashSet) {
                    if (this.debugOnceHashSet.contains(callerAffix)) {
                        return;
                    }

                    this.debugOnceHashSet.add(callerAffix);
                }
            }

            String formattedOutputStr = getFormattedOutputStr(format, params);
            out.println(this.getFormatter().format(this.debugType, logSeverity, callerAffix, formattedOutputStr));
            DebugLog.getInstance().echoToLogFiles(this.debugType, logSeverity, callerAffix, formattedOutputStr);
        }
    }

    private void writeln(PrintStream out, String format, Object... params) {
        this.writeln(out, LogSeverity.General, format, params);
    }

    private static String getFormattedOutputStr(Object format, Object[] params) {
        return params.length > 0 ? String.format(String.valueOf(format), params) : String.valueOf(format);
    }

    /**
     * Returns the class name and method name prefix of the calling code.
     */
    public static String generateCallerPrefix() {
        return generateCallerPrefix_Internal(1, 0, DebugLog.getInstance().isLogTraceFileLocationEnabled());
    }

    private static String generateCallerPrefix_Internal(int backTraceOffset, int leftJustify, boolean includeLogTraceFileLocation) {
        int atIdx = 2 + backTraceOffset;
        StackTraceElement[] stackTraceElements = StackTraceContainer.getStackTrace(atIdx + 1, StackTraceContainer.Filters::excludeNativesAndLuaCalls);
        return StringUtils.leftJustify(getStackTraceElementString(stackTraceElements, atIdx, includeLogTraceFileLocation), leftJustify);
    }

    public static String getStackTraceElementString(StackTraceElement[] stackTraceElements, int atIDx, boolean includeLogTraceFileLocation) {
        StackTraceElement stackTraceElement = PZArrayUtil.getOrDefault(stackTraceElements, atIDx, null);
        if (stackTraceElement == null) {
            return "(UnknownStack)";
        } else {
            String classNameOnly = getUnqualifiedClassName(stackTraceElement.getClassName());
            String methodName = stackTraceElement.getMethodName();
            String comment;
            if (stackTraceElement.isNativeMethod()) {
                comment = " (Native Method)";
            } else if (includeLogTraceFileLocation) {
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
            StackTraceElement[] stackTrace = StackTraceContainer.getStackTrace(ex);
            return stackTrace != null && stackTrace.length != 0 ? getStackTraceElementString(stackTrace, 0, true) : "No Stack Trace Available";
        }
    }

    public void printStackTrace(LogSeverity severity, int depthStart, int depthCount, String messageFormat, Object... params) {
        if (this.isLogEnabled(severity)) {
            PrintStream outStream = this.getPrintStream(severity);
            if (messageFormat != null) {
                String message = !PZArrayUtil.isNullOrEmpty(params) ? String.format(messageFormat, params) : messageFormat;
                outStream.println(message);
                DebugLog.getInstance().echoExceptionLineToLogFiles(this.debugType, severity, "StackTraceMessage", message);
            }

            StackTraceElement[] stackTraceElements = StackTraceContainer.getStackTrace(-1, StackTraceContainer.Filters.forSeverity(severity));
            String stackTraceString = StackTraceContainer.getStackTraceString(stackTraceElements, "\t", depthStart + 1, depthCount);
            outStream.println(stackTraceString);
            DebugLog.getInstance().echoExceptionLineToLogFiles(this.debugType, severity, "StackTrace", stackTraceString);
        }
    }

    private PrintStream getPrintStream(LogSeverity severity) {
        return switch (severity) {
            case Trace, Noise, Debug, General -> this.wrappedStream;
            case Warning -> this.wrappedWarnStream;
            case Error -> this.wrappedErrStream;
            default -> {
                this.error("Unhandled LogSeverity: %s. Defaulted to Error.", String.valueOf(severity));
                yield this.wrappedErrStream;
            }
        };
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

    public boolean isLogEnabled(LogSeverity logSeverity) {
        return this.getLogSeverity().isLogEnabled(logSeverity);
    }

    public void trace(Object format, Object... params) {
        this.traceWithTraceOffset(1, format, params);
    }

    public void debugln(Object format, Object... params) {
        this.debuglnWithTraceOffset(1, format, params);
    }

    public void debugOnceln(Object format, Object... params) {
        this.debugOncelnWithTraceOffset(1, format, params);
    }

    public void noise(Object format, Object... params) {
        this.noiseWithTraceOffset(1, format, params);
    }

    public void warn(Object format, Object... params) {
        this.warnWithTraceOffset(1, format, params);
    }

    public void warnOnce(Object format, Object... params) {
        this.warnOnceWithTraceOffset(1, format, params);
    }

    public void error(Object format, Object... params) {
        this.errorWithTraceOffset(1, format, params);
    }

    public void debuglnWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, true, format, params);
        }
    }

    public void debugOncelnWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, false, format, params);
        }
    }

    public void noiseWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, backTraceOffset + 1, true, format, params);
        }
    }

    public void warnWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, true, format, params);
    }

    public void errorWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, backTraceOffset + 1, true, format, params);
    }

    public void traceWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, backTraceOffset + 1, true, format, params);
    }

    public void warnOnceWithTraceOffset(int backTraceOffset, Object format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, false, format, params);
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

    public void println(Object format, Object... params) {
        this.writeln(this.wrappedStream, LogSeverity.General, format, params);
    }

    public void printException(Throwable ex, String errorMessage, LogSeverity severity) {
        this.printException(ex, errorMessage, generateCallerPrefix(), severity);
    }

    public void printException(Throwable ex, String errorMessage, String callerPrefix, LogSeverity severity) {
        if (ex == null) {
            this.warn("Null exception passed.");
        } else if (this.isLogEnabled(severity)) {
            PrintStream outStream = this.getPrintStream(severity);
            boolean includeStack = this.shouldIncludeStackTrace(severity);
            if (includeStack) {
                StringBuilder sb = new StringBuilder();
                if (errorMessage != null) {
                    sb.append(
                        String.format(
                            "%s> Exception thrown%s\t%s at %s. Message: %s", callerPrefix, System.lineSeparator(), ex, getTopStackTraceString(ex), errorMessage
                        )
                    );
                } else {
                    sb.append(String.format("%s> Exception thrown%s\t%s at %s.", callerPrefix, System.lineSeparator(), ex, getTopStackTraceString(ex)));
                }

                sb.append(System.lineSeparator());
                StackTraceContainer.getStackTraceString(sb, ex, "Stack trace:", "\t", 0, -1);
                this.write(outStream, severity, sb.toString());
            } else if (errorMessage != null) {
                String message = String.format("%s> Exception thrown %s at %s. Message: %s", callerPrefix, ex, getTopStackTraceString(ex), errorMessage);
                this.writeln(outStream, severity, message);
            } else {
                String message = String.format("%s> Exception thrown %s at %s.", callerPrefix, ex, getTopStackTraceString(ex));
                this.writeln(outStream, severity, message);
            }
        }
    }

    private boolean shouldIncludeStackTrace(LogSeverity severity) {
        return switch (severity) {
            case Trace, Noise, General, Warning -> false;
            default -> true;
        };
    }

    public void printException(Throwable ex, LogSeverity severity, String callerPrefix, String errorMessageFormat, Object... params) {
        if (this.isLogEnabled(severity)) {
            String errorMessage = errorMessageFormat != null
                ? (PZArrayUtil.lengthOf(params) > 0 ? String.format(errorMessageFormat, params) : String.format("%s", errorMessageFormat))
                : null;
            this.printException(ex, errorMessage, callerPrefix, severity);
        }
    }
}
