// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Stacks {
    public abstract static class GenericStack extends PooledObject {
        private final List<IPooledObject> stackItems = new ArrayList<>();

        public abstract void invoke();

        public void invokeAndRelease() {
            try {
                this.invoke();
            } finally {
                this.release();
            }
        }

        private <E> E push(E pooledObject) {
            this.stackItems.add((IPooledObject)pooledObject);
            return pooledObject;
        }

        @Override
        public void onReleased() {
            this.stackItems.forEach(Pool::tryRelease);
            this.stackItems.clear();
        }

        public <E, T1> Predicate<E> predicate(T1 val1, Predicates.Params1.ICallback<E, T1> predicate) {
            return this.push(Lambda.predicate(val1, predicate));
        }

        public <E, T1, T2> Predicate<E> predicate(T1 val1, T2 val2, Predicates.Params2.ICallback<E, T1, T2> predicate) {
            return this.push(Lambda.predicate(val1, val2, predicate));
        }

        public <E, T1, T2, T3> Predicate<E> predicate(T1 val1, T2 val2, T3 val3, Predicates.Params3.ICallback<E, T1, T2, T3> predicate) {
            return this.push(Lambda.predicate(val1, val2, val3, predicate));
        }

        public <E, T1> Comparator<E> comparator(T1 val1, Comparators.Params1.ICallback<E, T1> comparator) {
            return this.push(Lambda.comparator(val1, comparator));
        }

        public <E, T1, T2> Comparator<E> comparator(T1 val1, T2 val2, Comparators.Params2.ICallback<E, T1, T2> comparator) {
            return this.push(Lambda.comparator(val1, val2, comparator));
        }

        public <E, T1> Consumer<E> consumer(T1 val1, Consumers.Params1.ICallback<E, T1> consumer) {
            return this.push(Lambda.consumer(val1, consumer));
        }

        public <E, T1, T2> Consumer<E> consumer(T1 val1, T2 val2, Consumers.Params2.ICallback<E, T1, T2> consumer) {
            return this.push(Lambda.consumer(val1, val2, consumer));
        }

        public <E, T1, T2, T3> Consumer<E> consumer(T1 val1, T2 val2, T3 val3, Consumers.Params3.ICallback<E, T1, T2, T3> consumer) {
            return this.push(Lambda.consumer(val1, val2, val3, consumer));
        }

        public <E, T1, T2, T3, T4> Consumer<E> consumer(T1 val1, T2 val2, T3 val3, T4 val4, Consumers.Params4.ICallback<E, T1, T2, T3, T4> consumer) {
            return this.push(Lambda.consumer(val1, val2, val3, val4, consumer));
        }

        public <T1> Runnable invoker(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
            return this.push(Lambda.invoker(val1, invoker));
        }

        public <T1, T2> Runnable invoker(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
            return this.push(Lambda.invoker(val1, val2, invoker));
        }

        public <T1, T2, T3> Runnable invoker(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
            return this.push(Lambda.invoker(val1, val2, val3, invoker));
        }

        public <T1, T2, T3, T4> Runnable invoker(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
            return this.push(Lambda.invoker(val1, val2, val3, val4, invoker));
        }

        public <T1, T2, T3, T4, T5> Runnable invoker(T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> invoker) {
            return this.push(Lambda.invoker(val1, val2, val3, val4, val5, invoker));
        }
    }

    public static final class Params1 {
        public static final class CallbackStackItem<T1> extends Stacks.Params1.StackItem<T1> {
            private Stacks.Params1.ICallback<T1> callback;
            private static final Pool<Stacks.Params1.CallbackStackItem<Object>> s_pool = new Pool<>(Stacks.Params1.CallbackStackItem::new);

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1);
            }

            public static <T1> Stacks.Params1.CallbackStackItem<T1> alloc(T1 val1, Stacks.Params1.ICallback<T1> callback) {
                Stacks.Params1.CallbackStackItem<Object> item = s_pool.alloc();
                item.val1 = val1;
                item.callback = callback;
                return (Stacks.Params1.CallbackStackItem<T1>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1> {
            void accept(Stacks.GenericStack var1, T1 var2);
        }

        private abstract static class StackItem<T1> extends Stacks.GenericStack {
            T1 val1;
        }
    }

    public static final class Params2 {
        public static final class CallbackStackItem<T1, T2> extends Stacks.Params2.StackItem<T1, T2> {
            private Stacks.Params2.ICallback<T1, T2> callback;
            private static final Pool<Stacks.Params2.CallbackStackItem<Object, Object>> s_pool = new Pool<>(Stacks.Params2.CallbackStackItem::new);

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2);
            }

            public static <T1, T2> Stacks.Params2.CallbackStackItem<T1, T2> alloc(T1 val1, T2 val2, Stacks.Params2.ICallback<T1, T2> callback) {
                Stacks.Params2.CallbackStackItem<Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.callback = callback;
                return (Stacks.Params2.CallbackStackItem<T1, T2>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3);
        }

        private abstract static class StackItem<T1, T2> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
        }
    }

    public static final class Params3 {
        public static final class CallbackStackItem<T1, T2, T3> extends Stacks.Params3.StackItem<T1, T2, T3> {
            private Stacks.Params3.ICallback<T1, T2, T3> callback;
            private static final Pool<Stacks.Params3.CallbackStackItem<Object, Object, Object>> s_pool = new Pool<>(Stacks.Params3.CallbackStackItem::new);

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2, this.val3);
            }

            public static <T1, T2, T3> Stacks.Params3.CallbackStackItem<T1, T2, T3> alloc(
                T1 val1, T2 val2, T3 val3, Stacks.Params3.ICallback<T1, T2, T3> callback
            ) {
                Stacks.Params3.CallbackStackItem<Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.callback = callback;
                return (Stacks.Params3.CallbackStackItem<T1, T2, T3>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2, T3> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3, T3 var4);
        }

        private abstract static class StackItem<T1, T2, T3> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
            T3 val3;
        }
    }

    public static final class Params4 {
        public static final class CallbackStackItem<T1, T2, T3, T4> extends Stacks.Params4.StackItem<T1, T2, T3, T4> {
            private Stacks.Params4.ICallback<T1, T2, T3, T4> callback;
            private static final Pool<Stacks.Params4.CallbackStackItem<Object, Object, Object, Object>> s_pool = new Pool<>(
                Stacks.Params4.CallbackStackItem::new
            );

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2, this.val3, this.val4);
            }

            public static <T1, T2, T3, T4> Stacks.Params4.CallbackStackItem<T1, T2, T3, T4> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, Stacks.Params4.ICallback<T1, T2, T3, T4> callback
            ) {
                Stacks.Params4.CallbackStackItem<Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.callback = callback;
                return (Stacks.Params4.CallbackStackItem<T1, T2, T3, T4>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2, T3, T4> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5);
        }

        private abstract static class StackItem<T1, T2, T3, T4> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
        }
    }

    public static final class Params5 {
        public static final class CallbackStackItem<T1, T2, T3, T4, T5> extends Stacks.Params5.StackItem<T1, T2, T3, T4, T5> {
            private Stacks.Params5.ICallback<T1, T2, T3, T4, T5> callback;
            private static final Pool<Stacks.Params5.CallbackStackItem<Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Stacks.Params5.CallbackStackItem::new
            );

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2, this.val3, this.val4, this.val5);
            }

            public static <T1, T2, T3, T4, T5> Stacks.Params5.CallbackStackItem<T1, T2, T3, T4, T5> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Stacks.Params5.ICallback<T1, T2, T3, T4, T5> callback
            ) {
                Stacks.Params5.CallbackStackItem<Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.callback = callback;
                return (Stacks.Params5.CallbackStackItem<T1, T2, T3, T4, T5>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2, T3, T4, T5> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6);
        }

        private abstract static class StackItem<T1, T2, T3, T4, T5> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
            T5 val5;
        }
    }

    public static final class Params6 {
        public static final class CallbackStackItem<T1, T2, T3, T4, T5, T6> extends Stacks.Params6.StackItem<T1, T2, T3, T4, T5, T6> {
            private Stacks.Params6.ICallback<T1, T2, T3, T4, T5, T6> callback;
            private static final Pool<Stacks.Params6.CallbackStackItem<Object, Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Stacks.Params6.CallbackStackItem::new
            );

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2, this.val3, this.val4, this.val5, this.val6);
            }

            public static <T1, T2, T3, T4, T5, T6> Stacks.Params6.CallbackStackItem<T1, T2, T3, T4, T5, T6> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, Stacks.Params6.ICallback<T1, T2, T3, T4, T5, T6> callback
            ) {
                Stacks.Params6.CallbackStackItem<Object, Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.val6 = val6;
                item.callback = callback;
                return (Stacks.Params6.CallbackStackItem<T1, T2, T3, T4, T5, T6>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.val6 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2, T3, T4, T5, T6> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6, T6 var7);
        }

        private abstract static class StackItem<T1, T2, T3, T4, T5, T6> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
            T5 val5;
            T6 val6;
        }
    }

    public static final class Params7 {
        public static final class CallbackStackItem<T1, T2, T3, T4, T5, T6, T7> extends Stacks.Params7.StackItem<T1, T2, T3, T4, T5, T6, T7> {
            private Stacks.Params7.ICallback<T1, T2, T3, T4, T5, T6, T7> callback;
            private static final Pool<Stacks.Params7.CallbackStackItem<Object, Object, Object, Object, Object, Object, Object>> s_pool = new Pool<>(
                Stacks.Params7.CallbackStackItem::new
            );

            @Override
            public void invoke() {
                this.callback.accept(this, this.val1, this.val2, this.val3, this.val4, this.val5, this.val6, this.val7);
            }

            public static <T1, T2, T3, T4, T5, T6, T7> Stacks.Params7.CallbackStackItem<T1, T2, T3, T4, T5, T6, T7> alloc(
                T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, Stacks.Params7.ICallback<T1, T2, T3, T4, T5, T6, T7> callback
            ) {
                Stacks.Params7.CallbackStackItem<Object, Object, Object, Object, Object, Object, Object> item = s_pool.alloc();
                item.val1 = val1;
                item.val2 = val2;
                item.val3 = val3;
                item.val4 = val4;
                item.val5 = val5;
                item.val6 = val6;
                item.val7 = val7;
                item.callback = callback;
                return (Stacks.Params7.CallbackStackItem<T1, T2, T3, T4, T5, T6, T7>)item;
            }

            @Override
            public void onReleased() {
                this.val1 = null;
                this.val2 = null;
                this.val3 = null;
                this.val4 = null;
                this.val5 = null;
                this.val6 = null;
                this.val7 = null;
                this.callback = null;
                super.onReleased();
            }
        }

        public interface ICallback<T1, T2, T3, T4, T5, T6, T7> {
            void accept(Stacks.GenericStack var1, T1 var2, T2 var3, T3 var4, T4 var5, T5 var6, T6 var7, T7 var8);
        }

        private abstract static class StackItem<T1, T2, T3, T4, T5, T6, T7> extends Stacks.GenericStack {
            T1 val1;
            T2 val2;
            T3 val3;
            T4 val4;
            T5 val5;
            T6 val6;
            T7 val7;
        }
    }
}
