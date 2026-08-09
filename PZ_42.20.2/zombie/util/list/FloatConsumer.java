// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.Objects;

public interface FloatConsumer {
    void accept(float var1);

    default FloatConsumer andThen(FloatConsumer after) {
        Objects.requireNonNull(after);
        return t -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
