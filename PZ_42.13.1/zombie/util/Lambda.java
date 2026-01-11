// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import zombie.util.lambda.Comparators;
import zombie.util.lambda.Consumers;
import zombie.util.lambda.IntSupplierFunction;
import zombie.util.lambda.Invokers;
import zombie.util.lambda.Predicates;
import zombie.util.lambda.ReturnValueContainer;
import zombie.util.lambda.ReturnValueContainerPrimitives;
import zombie.util.lambda.Stacks;

public final class Lambda {
    public static <E, T1> Predicate<E> predicate(T1 val1, Predicates.Params1.ICallback<E, T1> predicate) {
        return Predicates.Params1.CallbackStackItem.alloc(val1, predicate);
    }

    public static <E, T1, T2> Predicate<E> predicate(T1 val1, T2 val2, Predicates.Params2.ICallback<E, T1, T2> predicate) {
        return Predicates.Params2.CallbackStackItem.alloc(val1, val2, predicate);
    }

    public static <E, T1, T2, T3> Predicate<E> predicate(T1 val1, T2 val2, T3 val3, Predicates.Params3.ICallback<E, T1, T2, T3> predicate) {
        return Predicates.Params3.CallbackStackItem.alloc(val1, val2, val3, predicate);
    }

    public static <E, T1> Comparator<E> comparator(T1 val1, Comparators.Params1.ICallback<E, T1> comparator) {
        return Comparators.Params1.CallbackStackItem.alloc(val1, comparator);
    }

    public static <E, T1, T2> Comparator<E> comparator(T1 val1, T2 val2, Comparators.Params2.ICallback<E, T1, T2> comparator) {
        return Comparators.Params2.CallbackStackItem.alloc(val1, val2, comparator);
    }

    public static <E, T1> Consumer<E> consumer(T1 val1, Consumers.Params1.ICallback<E, T1> consumer) {
        return Consumers.Params1.CallbackStackItem.alloc(val1, consumer);
    }

    public static <E, T1, T2> Consumer<E> consumer(T1 val1, T2 val2, Consumers.Params2.ICallback<E, T1, T2> consumer) {
        return Consumers.Params2.CallbackStackItem.alloc(val1, val2, consumer);
    }

    public static <E, T1, T2, T3> Consumer<E> consumer(T1 val1, T2 val2, T3 val3, Consumers.Params3.ICallback<E, T1, T2, T3> consumer) {
        return Consumers.Params3.CallbackStackItem.alloc(val1, val2, val3, consumer);
    }

    public static <E, T1, T2, T3, T4> Consumer<E> consumer(T1 val1, T2 val2, T3 val3, T4 val4, Consumers.Params4.ICallback<E, T1, T2, T3, T4> consumer) {
        return Consumers.Params4.CallbackStackItem.alloc(val1, val2, val3, val4, consumer);
    }

    public static <E, T1, T2, T3, T4, T5> Consumer<E> consumer(
        T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Consumers.Params5.ICallback<E, T1, T2, T3, T4, T5> consumer
    ) {
        return Consumers.Params5.CallbackStackItem.alloc(val1, val2, val3, val4, val5, consumer);
    }

    public static Invokers.Params0.Boolean.CallbackStackItem invokerBoolean(Invokers.Params0.Boolean.ICallback invoker) {
        return Invokers.Params0.Boolean.CallbackStackItem.alloc(invoker);
    }

    public static <T1> Runnable invoker(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        return Invokers.Params1.CallbackStackItem.alloc(val1, invoker);
    }

    public static <T1, T2> Runnable invoker(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        return Invokers.Params2.CallbackStackItem.alloc(val1, val2, invoker);
    }

    public static <T1, T2, T3> Runnable invoker(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        return Invokers.Params3.CallbackStackItem.alloc(val1, val2, val3, invoker);
    }

    public static <T1, T2, T3, T4> Runnable invoker(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        return Invokers.Params4.CallbackStackItem.alloc(val1, val2, val3, val4, invoker);
    }

    public static <T1, T2, T3, T4, T5> Runnable invoker(T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Invokers.Params5.ICallback<T1, T2, T3, T4, T5> invoker) {
        return Invokers.Params5.CallbackStackItem.alloc(val1, val2, val3, val4, val5, invoker);
    }

    public static <T1> void capture(T1 val1, Stacks.Params1.ICallback<T1> captureConsumer) {
        Stacks.Params1.CallbackStackItem<T1> item = Stacks.Params1.CallbackStackItem.alloc(val1, captureConsumer);
        item.invokeAndRelease();
    }

    public static <T1, T2> void capture(T1 val1, T2 val2, Stacks.Params2.ICallback<T1, T2> captureConsumer) {
        Stacks.Params2.CallbackStackItem<T1, T2> item = Stacks.Params2.CallbackStackItem.alloc(val1, val2, captureConsumer);
        item.invokeAndRelease();
    }

    public static <T1, T2, T3> void capture(T1 val1, T2 val2, T3 val3, Stacks.Params3.ICallback<T1, T2, T3> captureConsumer) {
        Stacks.Params3.CallbackStackItem<T1, T2, T3> item = Stacks.Params3.CallbackStackItem.alloc(val1, val2, val3, captureConsumer);
        item.invokeAndRelease();
    }

    public static <T1, T2, T3, T4> void capture(T1 val1, T2 val2, T3 val3, T4 val4, Stacks.Params4.ICallback<T1, T2, T3, T4> captureConsumer) {
        Stacks.Params4.CallbackStackItem<T1, T2, T3, T4> item = Stacks.Params4.CallbackStackItem.alloc(val1, val2, val3, val4, captureConsumer);
        item.invokeAndRelease();
    }

    public static <T1, T2, T3, T4, T5> void capture(T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, Stacks.Params5.ICallback<T1, T2, T3, T4, T5> captureConsumer) {
        Stacks.Params5.CallbackStackItem<T1, T2, T3, T4, T5> item = Stacks.Params5.CallbackStackItem.alloc(val1, val2, val3, val4, val5, captureConsumer);
        item.invokeAndRelease();
    }

    public static <T1, T2, T3, T4, T5, T6> void capture(
        T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, Stacks.Params6.ICallback<T1, T2, T3, T4, T5, T6> captureConsumer
    ) {
        Stacks.Params6.CallbackStackItem<T1, T2, T3, T4, T5, T6> item = Stacks.Params6.CallbackStackItem.alloc(
            val1, val2, val3, val4, val5, val6, captureConsumer
        );
        item.invokeAndRelease();
    }

    public static <T1, T2, T3, T4, T5, T6, T7> void capture(
        T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, Stacks.Params7.ICallback<T1, T2, T3, T4, T5, T6, T7> captureConsumer
    ) {
        Stacks.Params7.CallbackStackItem<T1, T2, T3, T4, T5, T6, T7> item = Stacks.Params7.CallbackStackItem.alloc(
            val1, val2, val3, val4, val5, val6, val7, captureConsumer
        );
        item.invokeAndRelease();
    }

    public static <E, T1> void forEach(Consumer<Consumer<E>> forEachFunction, T1 captureVal1, Consumers.Params1.ICallback<E, T1> lambdaFunc) {
        capture(
            forEachFunction,
            captureVal1,
            lambdaFunc,
            (stack, l_forEachFunction, l_captureVal1, l_lambdaFunc) -> l_forEachFunction.accept(stack.consumer(l_captureVal1, l_lambdaFunc))
        );
    }

    public static <E, T1, T2> void forEach(
        Consumer<Consumer<E>> forEachFunction, T1 captureVal1, T2 captureVal2, Consumers.Params2.ICallback<E, T1, T2> lambdaFunc
    ) {
        capture(
            forEachFunction,
            captureVal1,
            captureVal2,
            lambdaFunc,
            (stack, l_forEachFunction, l_captureVal1, l_captureVal2, l_lambdaFunc) -> l_forEachFunction.accept(
                stack.consumer(l_captureVal1, l_captureVal2, l_lambdaFunc)
            )
        );
    }

    public static <E, T1> void forEachFrom(
        BiConsumer<List<E>, Consumer<E>> forEachFunction, List<E> from, T1 captureVal1, Consumers.Params1.ICallback<E, T1> lambdaFunc
    ) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_lambdaFunc) -> l_forEachFunction.accept(l_from, stack.consumer(l_captureVal1, l_lambdaFunc))
        );
    }

    public static <E, T1, T2> void forEachFrom(
        BiConsumer<List<E>, Consumer<E>> forEachFunction, List<E> from, T1 captureVal1, T2 captureVal2, Consumers.Params2.ICallback<E, T1, T2> lambdaFunc
    ) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            captureVal2,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_captureVal2, l_lambdaFunc) -> l_forEachFunction.accept(
                l_from, stack.consumer(l_captureVal1, l_captureVal2, l_lambdaFunc)
            )
        );
    }

    public static <E, F, T1> void forEachFrom(BiConsumer<F, Consumer<E>> forEachFunction, F from, T1 captureVal1, Consumers.Params1.ICallback<E, T1> lambdaFunc) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_lambdaFunc) -> l_forEachFunction.accept(l_from, stack.consumer(l_captureVal1, l_lambdaFunc))
        );
    }

    public static <E, F, T1, T2> void forEachFrom(
        BiConsumer<F, Consumer<E>> forEachFunction, F from, T1 captureVal1, T2 captureVal2, Consumers.Params2.ICallback<E, T1, T2> lambdaFunc
    ) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            captureVal2,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_captureVal2, l_lambdaFunc) -> l_forEachFunction.accept(
                l_from, stack.consumer(l_captureVal1, l_captureVal2, l_lambdaFunc)
            )
        );
    }

    public static <E, F, T1, T2, T3> void forEachFrom(
        BiConsumer<F, Consumer<E>> forEachFunction,
        F from,
        T1 captureVal1,
        T2 captureVal2,
        T3 captureVal3,
        Consumers.Params3.ICallback<E, T1, T2, T3> lambdaFunc
    ) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            captureVal2,
            captureVal3,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_captureVal2, l_captureVal3, l_lambdaFunc) -> l_forEachFunction.accept(
                l_from, stack.consumer(l_captureVal1, l_captureVal2, l_captureVal3, l_lambdaFunc)
            )
        );
    }

    public static <E, F, T1, T2, T3, T4> void forEachFrom(
        BiConsumer<F, Consumer<E>> forEachFunction,
        F from,
        T1 captureVal1,
        T2 captureVal2,
        T3 captureVal3,
        T4 captureVal4,
        Consumers.Params4.ICallback<E, T1, T2, T3, T4> lambdaFunc
    ) {
        capture(
            forEachFunction,
            from,
            captureVal1,
            captureVal2,
            captureVal3,
            captureVal4,
            lambdaFunc,
            (stack, l_forEachFunction, l_from, l_captureVal1, l_captureVal2, l_captureVal3, l_captureVal4, l_lambdaFunc) -> l_forEachFunction.accept(
                l_from, stack.consumer(l_captureVal1, l_captureVal2, l_captureVal3, l_captureVal4, l_lambdaFunc)
            )
        );
    }

    public static <E, T1, R> R find(Function<Predicate<E>, R> findFunction, T1 captureVal1, Predicates.Params1.ICallback<E, T1> lambdaFunc) {
        ReturnValueContainer<R> returnContainer = ReturnValueContainer.alloc();
        capture(
            findFunction,
            captureVal1,
            lambdaFunc,
            returnContainer,
            (stack, l_findFunction, l_captureVal1, l_lambdaFunc, l_returnContainer) -> l_returnContainer.returnVal = l_findFunction.apply(
                stack.predicate(l_captureVal1, l_lambdaFunc)
            )
        );
        R returnval = returnContainer.returnVal;
        returnContainer.release();
        return returnval;
    }

    public static <E, T1> int indexOf(IntSupplierFunction<Predicate<E>> findFunction, T1 captureVal1, Predicates.Params1.ICallback<E, T1> lambdaFunc) {
        ReturnValueContainerPrimitives.RVInt returnContainer = ReturnValueContainerPrimitives.RVInt.alloc();
        capture(
            findFunction,
            captureVal1,
            lambdaFunc,
            returnContainer,
            (stack, l_findFunction, l_captureVal1, l_lambdaFunc, l_returnContainer) -> l_returnContainer.returnVal = l_findFunction.getInt(
                stack.predicate(l_captureVal1, l_lambdaFunc)
            )
        );
        int returnval = returnContainer.returnVal;
        returnContainer.release();
        return returnval;
    }

    public static <E, T1> boolean contains(Predicate<Predicate<E>> findFunction, T1 captureVal1, Predicates.Params1.ICallback<E, T1> lambdaFunc) {
        ReturnValueContainerPrimitives.RVBoolean returnContainer = ReturnValueContainerPrimitives.RVBoolean.alloc();
        capture(
            findFunction,
            captureVal1,
            lambdaFunc,
            returnContainer,
            (stack, l_findFunction, l_captureVal1, l_lambdaFunc, l_returnContainer) -> l_returnContainer.returnVal = l_findFunction.test(
                stack.predicate(l_captureVal1, l_lambdaFunc)
            )
        );
        Boolean returnval = returnContainer.returnVal;
        returnContainer.release();
        return returnval;
    }

    public static <E, F extends Iterable<E>, T1> boolean containsFrom(
        BiPredicate<F, Predicate<E>> findFunction, F from, T1 captureVal1, Predicates.Params1.ICallback<E, T1> lambdaFunc
    ) {
        ReturnValueContainerPrimitives.RVBoolean returnContainer = ReturnValueContainerPrimitives.RVBoolean.alloc();
        capture(
            findFunction,
            from,
            captureVal1,
            lambdaFunc,
            returnContainer,
            (stack, l_findFunction, l_from, l_captureVal1, l_lambdaFunc, l_returnContainer) -> l_returnContainer.returnVal = l_findFunction.test(
                l_from, stack.predicate(l_captureVal1, l_lambdaFunc)
            )
        );
        Boolean returnval = returnContainer.returnVal;
        returnContainer.release();
        return returnval;
    }

    public static <T1> void invoke(Consumer<Runnable> runFunction, T1 captureVal1, Invokers.Params1.ICallback<T1> lambdaFunc) {
        capture(
            runFunction,
            captureVal1,
            lambdaFunc,
            (stack, l_runFunction, l_captureVal1, l_lambdaFunc) -> l_runFunction.accept(stack.invoker(l_captureVal1, l_lambdaFunc))
        );
    }

    public static <T1, T2> void invoke(Consumer<Runnable> runFunction, T1 captureVal1, T2 captureVal2, Invokers.Params2.ICallback<T1, T2> lambdaFunc) {
        capture(
            runFunction,
            captureVal1,
            captureVal2,
            lambdaFunc,
            (stack, l_runFunction, l_captureVal1, l_captureVal2, l_lambdaFunc) -> l_runFunction.accept(
                stack.invoker(l_captureVal1, l_captureVal2, l_lambdaFunc)
            )
        );
    }
}
