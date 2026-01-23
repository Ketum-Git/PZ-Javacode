// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.function.Predicate;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Predicates {
    public static final class Params1 {
        public static final class CallbackStackItem<E, T1> extends Predicates.Params1.StackItem<T1> implements Predicate<E> {
            private Predicates.Params1.ICallback<E, T1> predicate;
            private static final Pool<Predicates.Params1.CallbackStackItem<Object, Object>> s_pool = new Pool<>(Predicates.Params1.CallbackStackItem::new);

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1);
            }

            public static <E, T1> Predicates.Params1.CallbackStackItem<E, T1> alloc(T1 val1, Predicates.Params1.ICallback<E, T1> predicate) {
                Predicates.Params1.CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.predicate = predicate;
                return (Predicates.Params1.CallbackStackItem<E, T1>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.predicate = null;
            }
        }

        public interface ICallback<E, T1> {
            boolean test(E var1, T1 var2);
        }

        private static class StackItem<T1> extends PooledObject {
            T1 val1;
        }
    }

    public static final class Params2 {
        public static final class CallbackStackItem<E, T1, T2> extends Predicates.Params2.StackItem<T1, T2> implements Predicate<E> {
            private Predicates.Params2.ICallback<E, T1, T2> predicate;
            private static final Pool<Predicates.Params2.CallbackStackItem<Object, Object, Object>> s_pool = new Pool<>(
                Predicates.Params2.CallbackStackItem::new
            );

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1, this.val2);
            }

            public static <E, T1, T2> Predicates.Params2.CallbackStackItem<E, T1, T2> alloc(T1 val1, T2 val2, Predicates.Params2.ICallback<E, T1, T2> predicate) {
                Predicates.Params2.CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.predicate = predicate;
                return (Predicates.Params2.CallbackStackItem<E, T1, T2>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.predicate = null;
            }
        }

        public interface ICallback<E, T1, T2> {
            boolean test(E var1, T1 var2, T2 var3);
        }

        private static class StackItem<T1, T2> extends PooledObject {
            T1 val1;
            T2 val2;
        }
    }

    public static final class Params3 {
        public static final class CallbackStackItem<E, T1, T2, T3> extends Predicates.Params3.StackItem<T1, T2, T3> implements Predicate<E> {
            private Predicates.Params3.ICallback<E, T1, T2, T3> predicate;
            private static final Pool<Predicates.Params3.CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<>(
                Predicates.Params3.CallbackStackItem::new
            );

            @Override
            public boolean test(E e) {
                return this.predicate.test(e, this.val1, this.val2, this.val3);
            }

            public static <E, T1, T2, T3> Predicates.Params3.CallbackStackItem<E, T1, T2, T3> alloc(
                T1 val1, T2 val2, T3 val3, Predicates.Params3.ICallback<E, T1, T2, T3> predicate
            ) {
                Predicates.Params3.CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.predicate = predicate;
                return (Predicates.Params3.CallbackStackItem<E, T1, T2, T3>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.predicate = null;
            }
        }

        public interface ICallback<E, T1, T2, T3> {
            boolean test(E var1, T1 var2, T2 var3, T3 var4);
        }

        private static class StackItem<T1, T2, T3> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
        }
    }
}
