// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

public class GameEntityException extends Exception {
    public GameEntityException(String errorMessage) {
        super(errorMessage);
    }

    public GameEntityException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public GameEntityException(String errorMessage, GameEntity entity) {
        super((entity == null ? "[null]" : entity.getExceptionCompatibleString()) + " " + errorMessage);
    }

    public GameEntityException(String errorMessage, Throwable err, GameEntity entity) {
        super((entity == null ? "[null]" : entity.getExceptionCompatibleString()) + " " + errorMessage, err);
    }
}
