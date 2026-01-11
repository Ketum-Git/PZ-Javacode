// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.PrintStream;

public final class ProxyPrintStream extends PrintStream {
    private final PrintStream fileStream;
    private final PrintStream systemStream;

    public ProxyPrintStream(PrintStream system, PrintStream file) {
        super(system);
        this.systemStream = system;
        this.fileStream = file;
    }

    @Override
    public void print(String str) {
        this.systemStream.print(str);
        this.fileStream.print(str);
        this.fileStream.flush();
    }

    @Override
    public void println(String str) {
        this.systemStream.println(str);
        this.fileStream.println(str);
        this.fileStream.flush();
    }

    @Override
    public void println(Object o) {
        this.systemStream.println(o);
        this.fileStream.println(o);
        this.fileStream.flush();
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        this.systemStream.write(buf, off, len);
        this.fileStream.write(buf, off, len);
        this.fileStream.flush();
    }

    @Override
    public void flush() {
        this.systemStream.flush();
        this.fileStream.flush();
    }
}
