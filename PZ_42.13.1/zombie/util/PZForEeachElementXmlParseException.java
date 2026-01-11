// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import org.w3c.dom.Element;

public class PZForEeachElementXmlParseException extends RuntimeException {
    private Element xmlElement;

    public PZForEeachElementXmlParseException() {
    }

    public PZForEeachElementXmlParseException(String message) {
        super(message);
    }

    public PZForEeachElementXmlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PZForEeachElementXmlParseException(Throwable cause) {
        super(cause);
    }

    public PZForEeachElementXmlParseException(String message, Element element, Throwable cause) {
        super(message, cause);
        this.xmlElement = element;
    }

    @Override
    public String toString() {
        String base = super.toString();
        String toString = base;
        if (this.xmlElement != null) {
            toString = base + System.lineSeparator();
            toString = toString + " xmlElement:" + PZXmlUtil.elementToPrettyStringSafe(this.xmlElement);
        }

        Throwable cause = this.getCause();
        if (cause != null) {
            toString = toString + System.lineSeparator() + "  Caused by:" + System.lineSeparator() + "    " + cause;
        }

        return toString;
    }
}
