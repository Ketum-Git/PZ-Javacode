// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

public class AttributeException extends Exception {
    public AttributeException(String errorMessage) {
        super(errorMessage);
    }

    public AttributeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
