// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PZOptional {
    public static <T> void ifPresent(T x, Consumer<T> ifPresentCall) {
        if (x != null) {
            ifPresentCall.accept(x);
        }
    }

    public static <T, P1> void ifPresent(T x, BiConsumer<T, P1> ifPresentCall, P1 param1) {
        if (x != null) {
            ifPresentCall.accept(x, param1);
        }
    }

    public static <T> boolean ifPresent(T x, Predicate<T> ifPresentPredicate) {
        return x != null && ifPresentPredicate.test(x);
    }

    public static <T, R> R ifPresent(T x, R defaultIfNull, Function<T, R> ifPresent) {
        return x != null ? ifPresent.apply(x) : defaultIfNull;
    }

    public static <T, R, P1> R ifPresent(T x, R defaultIfNull, BiFunction<T, P1, R> ifPresent, P1 param1) {
        return x != null ? ifPresent.apply(x, param1) : defaultIfNull;
    }
}
