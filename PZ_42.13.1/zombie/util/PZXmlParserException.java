// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

public final class PZXmlParserException extends Exception {
    public PZXmlParserException() {
    }

    public PZXmlParserException(String message) {
        super(message);
    }

    public PZXmlParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public PZXmlParserException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        String base = super.toString();
        String toString = base;
        Throwable cause = this.getCause();
        if (cause != null) {
            toString = base + System.lineSeparator() + "  Caused by:" + System.lineSeparator() + "    " + cause.toString();
        }

        return toString;
    }
}
