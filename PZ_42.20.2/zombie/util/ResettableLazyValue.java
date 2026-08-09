// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.function.Supplier;

public class ResettableLazyValue<T> extends LazyValue<T> {
    public ResettableLazyValue(Supplier<T> supplier) {
        super(supplier);
    }

    public void reset() {
        HANDLE.setVolatile((ResettableLazyValue)this, (Object)UNSET);
    }
}
