// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T var1, U var2, V var3);

    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (l, r, m) -> {
            this.accept(l, r, m);
            after.accept(l, r, m);
        };
    }
}
