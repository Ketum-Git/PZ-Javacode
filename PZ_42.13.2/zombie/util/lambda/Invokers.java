// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.function.BooleanSupplier;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class Invokers {
    public static final class Params0 {
        public static final class Boolean {
            public static final class CallbackStackItem extends PooledObject implements Runnable, BooleanSupplier {
                private Invokers.Params0.Boolean.ICallback predicate;
                private boolean result;
                private static final Pool<Invokers.Params0.Boolean.CallbackStackItem> s_pool = new Pool<>(Invokers.Params0.Boolean.CallbackStackItem::new);

                @Override
                public void run() {
                    this.result = this.predicate.accept();
                }

                @Override
                public boolean getAsBoolean() {
                    return this.result;
                }

                public static Invokers.Params0.Boolean.CallbackStackItem alloc(Invokers.Params0.Boolean.ICallback predicate) {
                    Invokers.Params0.Boolean.CallbackStackItem item = s_pool.alloc();
                    item.predicate = predicate;
                    return item;
                }

                @Override
                public void onReleased() {
                    this.predicate = null;
                }
            }

            public interface ICallback {
                boolean accept();
            }
        }

        public interface ICallback {
            void accept();
        }
    }

    public static final class Params1 {
        public static final class Boolean {
            public interface ICallback<T1> {
                boolean accept(T1 var1);
            }
        }

        public static final class CallbackStackItem<T1> extends Invokers.Params1.StackItem<T1> implements Runnable {
            private Invokers.Params1.ICallback<T1> invoker;
            private static final Pool<Invokers.Params1.CallbackStackItem<Object>> s_pool = new Pool<>(Invokers.Params1.CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1);
            }

            public static <T1> Invokers.Params1.CallbackStackItem<T1> alloc(T1 val1, Invokers.Params1.ICallback<T1> consumer) {
                Invokers.Params1.CallbackStackItem<Object> item = s_pool.alloc();
                item.val1 = val1;
                item.invoker = consumer;
                return (Invokers.Params1.CallbackStackItem<T1>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.invoker = null;
            }
        }

        public interface ICallback<T1> {
            void accept(T1 var1);
        }

        private static class StackItem<T1> extends PooledObject {
            T1 val1;
        }
    }

    public static final class Params2 {
        public static final class Boolean {
            public interface ICallback<T1, T2> {
                boolean accept(T1 var1, T2 var2);
            }

            public interface IParam1<T> extends Invokers.Params2.Boolean.ICallback<T, T>, Invokers.Params1.Boolean.ICallback<T> {
                @Override
                default boolean accept(T val1, T val2) {
                    return this.accept(val1);
                }
            }

            public interface IParam2<T> extends Invokers.Params2.Boolean.ICallback<T, T>, Invokers.Params1.Boolean.ICallback<T> {
                @Override
                default boolean accept(T val1, T val2) {
                    return this.accept(val2);
                }
            }
        }

        public static final class CallbackStackItem<T1, T2> extends Invokers.Params2.StackItem<T1, T2> implements Runnable {
            private Invokers.Params2.ICallback<T1, T2> invoker;
            private static final Pool<Invokers.Params2.CallbackStackItem<Object, Object>> s_pool = new Pool<>(Invokers.Params2.CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2);
            }

            public static <T1, T2> Invokers.Params2.CallbackStackItem<T1, T2> alloc(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> consumer) {
                Invokers.Params2.CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.invoker = consumer;
                return (Invokers.Params2.CallbackStackItem<T1, T2>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.invoker = null;
            }
        }

        public interface ICallback<T1, T2> {
            void accept(T1 var1, T2 var2);
        }

        private static class StackItem<T1, T2> extends PooledObject {
            T1 val1;
            T2 val2;
        }
    }

    public static final class Params3 {
        public static final class CallbackStackItem<T1, T2, T3> extends Invokers.Params3.StackItem<T1, T2, T3> implements Runnable {
            private Invokers.Params3.ICallback<T1, T2, T3> invoker;
            private static final Pool<Invokers.Params3.CallbackStackItem<Object, Object, Object>> s_pool = new Pool<>(Invokers.Params3.CallbackStackItem::new);

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3);
            }

            public static <T1, T2, T3> Invokers.Params3.CallbackStackItem<T1, T2, T3> alloc(
                T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> consumer
            ) {
                Invokers.Params3.CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.invoker = consumer;
                return (Invokers.Params3.CallbackStackItem<T1, T2, T3>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.invoker = null;
            }
        }

        public interface ICallback<T1, T2, T3> {
            void accept(T1 var1, T2 var2, T3 var3);
        }

        private static class StackItem<T1, T2, T3> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
        }
    }

    public static final class Params4 {
        public static final class CallbackStackItem<T1, T2, T3, T4> extends Invokers.Params4.StackItem<T1, T2, T3, T4> implements Runnable {
            private Invokers.Params4.ICallback<T1, T2, T3, T4> invoker;
            private static final Pool<Invokers.Params4.CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<>(
                Invokers.Params4.CallbackStackItem::new
            );

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3, this.val4);
            }

            public static <T1, T2, T3, T4> Invokers.Params4.CallbackStackItem<T1, T2, T3, T4> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> consumer
            ) {
                Invokers.Params4.CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.invoker = consumer;
                return (Invokers.Params4.CallbackStackItem<T1, T2, T3, T4>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.invoker = null;
            }
        }

        public interface ICallback<T1, T2, T3, T4> {
            void accept(T1 var1, T2 var2, T3 var3, T4 var4);
        }

        private static class StackItem<T1, T2, T3, T4> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
        }
    }

    public static final class Params5 {
        public static final class CallbackStackItem<T1, T2, T3, T4, T5> extends Invokers.Params5.StackItem<T1, T2, T3, T4, T5> implements Runnable {
            private Invokers.Params5.ICallback<T1, T2, T3, T4, T5> invoker;
            private static final Pool<Invokers.Params5.CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Invokers.Params5.CallbackStackItem::new
            );

            @Override
            public void run() {
                this.invoker.accept(this.val1, this.val2, this.val3, this.val4, this.val5);
            }

            public static <T1, T2, T3, T4, T5> Invokers.Params5.CallbackStackItem<T1, T2, T3, T4, T5> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> consumer
            ) {
                Invokers.Params5.CallbackStackItem<Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.invoker = consumer;
                return (Invokers.Params5.CallbackStackItem<T1, T2, T3, T4, T5>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.invoker = null;
            }
        }

        public interface ICallback<T1, T2, T3, T4, T5> {
            void accept(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5);
        }

        private static class StackItem<T1, T2, T3, T4, T5> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
            T5 val5;
        }
    }
}
