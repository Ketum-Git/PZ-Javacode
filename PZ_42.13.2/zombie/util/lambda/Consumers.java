// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.function.Consumer;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Consumers {
    public static final class Params1 {
        public static final class CallbackStackItem<E, T1> extends Consumers.Params1.StackItem<T1> implements Consumer<E> {
            private Consumers.Params1.ICallback<E, T1> consumer;
            private static final Pool<Consumers.Params1.CallbackStackItem<Object, Object>> s_pool = new Pool<>(Consumers.Params1.CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1);
            }

            public static <E, T1> Consumers.Params1.CallbackStackItem<E, T1> alloc(T1 val1, Consumers.Params1.ICallback<E, T1> consumer) {
                Consumers.Params1.CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.consumer = consumer;
                return (Consumers.Params1.CallbackStackItem<E, T1>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.consumer = null;
            }
        }

        public interface ICallback<E, T1> {
            void accept(E var1, T1 var2);
        }

        private static class StackItem<T1> extends PooledObject {
            T1 val1;
        }
    }

    public static class Params2 {
        public static final class CallbackStackItem<E, T1, T2> extends Consumers.Params2.StackItem<T1, T2> implements Consumer<E> {
            private Consumers.Params2.ICallback<E, T1, T2> consumer;
            private static final Pool<Consumers.Params2.CallbackStackItem<Object, Object, Object>> s_pool = new Pool<>(Consumers.Params2.CallbackStackItem::new);

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2);
            }

            public static <E, T1, T2> Consumers.Params2.CallbackStackItem<E, T1, T2> alloc(T1 val1, T2 val2, Consumers.Params2.ICallback<E, T1, T2> consumer) {
                Consumers.Params2.CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.consumer = consumer;
                return (Consumers.Params2.CallbackStackItem<E, T1, T2>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.consumer = null;
            }
        }

        public interface ICallback<E, T1, T2> {
            void accept(E var1, T1 var2, T2 var3);
        }

        private static class StackItem<T1, T2> extends PooledObject {
            T1 val1;
            T2 val2;
        }
    }

    public static final class Params3 {
        public static final class CallbackStackItem<E, T1, T2, T3> extends Consumers.Params3.StackItem<T1, T2, T3> implements Consumer<E> {
            private Consumers.Params3.ICallback<E, T1, T2, T3> consumer;
            private static final Pool<Consumers.Params3.CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<>(
                Consumers.Params3.CallbackStackItem::new
            );

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3);
            }

            public static <E, T1, T2, T3> Consumers.Params3.CallbackStackItem<E, T1, T2, T3> alloc(
                T1 val1, T2 val2, T3 val3, Consumers.Params3.ICallback<E, T1, T2, T3> consumer
            ) {
                Consumers.Params3.CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.consumer = consumer;
                return (Consumers.Params3.CallbackStackItem<E, T1, T2, T3>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.consumer = null;
            }
        }

        public interface ICallback<E, T1, T2, T3> {
            void accept(E var1, T1 var2, T2 var3, T3 var4);
        }

        private static class StackItem<T1, T2, T3> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
        }
    }

    public static final class Params4 {
        public static final class CallbackStackItem<E, T1, T2, T3, T4> extends Consumers.Params4.StackItem<T1, T2, T3, T4> implements Consumer<E> {
            private Consumers.Params4.ICallback<E, T1, T2, T3, T4> consumer;
            private static final Pool<Consumers.Params4.CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Consumers.Params4.CallbackStackItem::new
            );

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3, this.val4);
            }

            public static <E, T1, T2, T3, T4> Consumers.Params4.CallbackStackItem<E, T1, T2, T3, T4> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, Consumers.Params4.ICallback<E, T1, T2, T3, T4> consumer
            ) {
                Consumers.Params4.CallbackStackItem<Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.consumer = consumer;
                return (Consumers.Params4.CallbackStackItem<E, T1, T2, T3, T4>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.consumer = null;
            }
        }

        public interface ICallback<E, T1, T2, T3, T4> {
            void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5);
        }

        private static class StackItem<T1, T2, T3, T4> extends PooledObject {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
        }
    }

    public static final class Params5 {
        public static final class CallbackStackItem<E, T1, T2, T3, T4, T5> extends Consumers.Params5.StackItem<T1, T2, T3, T4, T5> implements Consumer<E> {
            private Consumers.Params5.ICallback<E, T1, T2, T3, T4, T5> consumer;
            private static final Pool<Consumers.Params5.CallbackStackItem<Object, Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Consumers.Params5.CallbackStackItem::new
            );

            @Override
            public void accept(E e) {
                this.consumer.accept(e, this.val1, this.val2, this.val3, this.val4, this.val5);
            }

            public static <E, T1, T2, T3, T4, T5> Consumers.Params5.CallbackStackItem<E, T1, T2, T3, T4, T5> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Consumers.Params5.ICallback<E, T1, T2, T3, T4, T5> consumer
            ) {
                Consumers.Params5.CallbackStackItem<Object, Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.consumer = consumer;
                return (Consumers.Params5.CallbackStackItem<E, T1, T2, T3, T4, T5>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.consumer = null;
            }
        }

        public interface ICallback<E, T1, T2, T3, T4, T5> {
            void accept(E var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6);
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
