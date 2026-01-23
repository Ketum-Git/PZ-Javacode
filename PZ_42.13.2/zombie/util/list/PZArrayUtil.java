// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import zombie.core.math.PZMath;
import zombie.core.random.RandInterface;
import zombie.core.random.RandStandard;
import zombie.util.ICloner;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;
import zombie.util.lambda.Predicates;

public class PZArrayUtil {
    public static final int[] emptyIntArray = new int[0];
    public static final float[] emptyFloatArray = new float[0];

    public static <E> E pickRandom(E[] collection, RandInterface rnd) {
        if (collection.length == 0) {
            return null;
        } else {
            int randomIndex = rnd.Next(collection.length);
            return collection[randomIndex];
        }
    }

    public static <E> E pickRandom(List<E> collection, RandInterface rnd) {
        if (collection.isEmpty()) {
            return null;
        } else {
            int randomIndex = rnd.Next(collection.size());
            return collection.get(randomIndex);
        }
    }

    public static <E> E pickRandom(Collection<E> collection, RandInterface rnd) {
        if (collection.isEmpty()) {
            return null;
        } else {
            int randomIndex = rnd.Next(collection.size());
            return getElementAt(collection, randomIndex);
        }
    }

    public static <E> E pickRandom(Iterable<E> collection, RandInterface rnd) {
        int size = getSize(collection);
        if (size == 0) {
            return null;
        } else {
            int randomIndex = rnd.Next(size);
            return getElementAt(collection, randomIndex);
        }
    }

    public static <E> E pickRandom(E[] collection) {
        return pickRandom(collection, RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(List<E> collection) {
        return pickRandom(collection, RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(Collection<E> collection) {
        return pickRandom(collection, RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(Iterable<E> collection) {
        return pickRandom(collection, RandStandard.INSTANCE);
    }

    public static <E> int getSize(Iterable<E> collection) {
        int count = 0;
        Iterator<E> it = collection.iterator();

        while (it.hasNext()) {
            count++;
            it.next();
        }

        return count;
    }

    public static <E> E getElementAt(Iterable<E> collection, int index) throws ArrayIndexOutOfBoundsException {
        E item = null;
        Iterator<E> it = collection.iterator();

        for (int i = 0; i <= index; i++) {
            if (!it.hasNext()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            if (i == index) {
                item = it.next();
            }
        }

        return item;
    }

    public static <E> void copy(ArrayList<E> target, ArrayList<E> source) {
        copy(target, source, elem -> elem);
    }

    public static <E> void copy(ArrayList<E> target, ArrayList<E> source, ICloner<E> elementCloner) {
        if (target != source) {
            target.clear();
            target.ensureCapacity(source.size());

            for (int i = 0; i < source.size(); i++) {
                E srcE = source.get(i);
                target.add(elementCloner.clone(srcE));
            }
        }
    }

    public static <E, E1> int indexOf(E[] collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        try {
            int i = 0;

            for (int collectionLength = lengthOf(collection); i < collectionLength; i++) {
                E element = collection[i];
                if (predicate.test(containsItem, element)) {
                    return i;
                }
            }

            return -1;
        } finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E, E1> int indexOf(List<E> collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        int var9;
        try {
            int foundIdx = -1;

            for (int i = 0; i < collection.size(); i++) {
                E element = collection.get(i);
                if (predicate.test(containsItem, element)) {
                    foundIdx = i;
                    break;
                }
            }

            var9 = foundIdx;
        } finally {
            Pool.tryRelease(predicate);
        }

        return var9;
    }

    public static <E> int indexOf(E[] collection, Predicate<E> predicate) {
        try {
            int i = 0;

            for (int collectionLength = lengthOf(collection); i < collectionLength; i++) {
                E element = collection[i];
                if (predicate.test(element)) {
                    return i;
                }
            }

            return -1;
        } finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E> int indexOf(List<E> collection, Predicate<E> predicate) {
        int var8;
        try {
            int foundIdx = -1;

            for (int i = 0; i < collection.size(); i++) {
                E element = collection.get(i);
                if (predicate.test(element)) {
                    foundIdx = i;
                    break;
                }
            }

            var8 = foundIdx;
        } finally {
            Pool.tryRelease(predicate);
        }

        return var8;
    }

    public static <E> boolean contains(E[] collection, int count, E e) {
        return indexOf(collection, count, e) != -1;
    }

    public static <E, E1> boolean contains(E[] collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        return indexOf(collection, containsItem, predicate) > -1;
    }

    public static <E, E1> boolean contains(List<E> collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        return indexOf(collection, containsItem, predicate) > -1;
    }

    public static <E, E1> boolean contains(Collection<E> it, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        if (it instanceof List<E> es) {
            return contains(es, containsItem, predicate);
        } else {
            boolean var10;
            try {
                boolean contains = false;

                for (E val : it) {
                    if (predicate.test(containsItem, val)) {
                        contains = true;
                        break;
                    }
                }

                var10 = contains;
            } finally {
                Pool.tryRelease(predicate);
            }

            return var10;
        }
    }

    public static <E, E1> boolean contains(Iterable<E> it, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        if (it instanceof List<E> es) {
            return indexOf(es, containsItem, predicate) > -1;
        } else {
            boolean var10;
            try {
                boolean contains = false;

                for (E val : it) {
                    if (predicate.test(containsItem, val)) {
                        contains = true;
                        break;
                    }
                }

                var10 = contains;
            } finally {
                Pool.tryRelease(predicate);
            }

            return var10;
        }
    }

    public static <E> boolean contains(E[] collection, Predicate<E> predicate) {
        return indexOf(collection, predicate) > -1;
    }

    public static <E> boolean contains(List<E> collection, Predicate<E> predicate) {
        return indexOf(collection, predicate) > -1;
    }

    public static <E> boolean contains(Collection<E> it, Predicate<E> predicate) {
        if (it instanceof List<E> es) {
            return contains(es, predicate);
        } else {
            boolean var9;
            try {
                boolean contains = false;

                for (E val : it) {
                    if (predicate.test(val)) {
                        contains = true;
                        break;
                    }
                }

                var9 = contains;
            } finally {
                Pool.tryRelease(predicate);
            }

            return var9;
        }
    }

    public static <E> boolean contains(Iterable<E> it, Predicate<E> predicate) {
        if (it instanceof List<E> es) {
            return indexOf(es, predicate) > -1;
        } else {
            boolean var9;
            try {
                boolean contains = false;

                for (E val : it) {
                    if (predicate.test(val)) {
                        contains = true;
                        break;
                    }
                }

                var9 = contains;
            } finally {
                Pool.tryRelease(predicate);
            }

            return var9;
        }
    }

    public static <E> E find(E[] collection, Predicate<E> predicate) {
        int indexOf = indexOf(collection, predicate);
        return indexOf > -1 ? collection[indexOf] : null;
    }

    public static <E> E find(List<E> collection, Predicate<E> predicate) {
        int indexOf = indexOf(collection, predicate);
        return indexOf > -1 ? collection.get(indexOf) : null;
    }

    public static <E> E find(Iterable<E> collection, Predicate<E> predicate) {
        if (collection instanceof List<E> es) {
            return find(es, predicate);
        } else {
            Object var4;
            try {
                Iterator es = collection.iterator();

                E element;
                do {
                    if (!es.hasNext()) {
                        return null;
                    }

                    element = (E)es.next();
                } while (!predicate.test(element));

                var4 = element;
            } finally {
                Pool.tryRelease(predicate);
            }

            return (E)var4;
        }
    }

    public static <E, S> List<E> listConvert(List<S> source, Function<S, E> converter) {
        return (List<E>)(source.isEmpty() ? PZArrayList.emptyList() : new PZConvertList<>(source, converter));
    }

    public static <E, S> Iterable<E> itConvert(Iterable<S> source, Function<S, E> converter) {
        return new PZConvertIterable<>(source, converter);
    }

    public static <E, S> List<E> listConvert(List<S> source, List<E> dest, Function<S, E> converter) {
        dest.clear();

        for (int i = 0; i < source.size(); i++) {
            dest.add(converter.apply(source.get(i)));
        }

        return dest;
    }

    public static <E> int lengthOf(E[] in_array) {
        return in_array != null ? in_array.length : 0;
    }

    public static int lengthOf(int[] in_array) {
        return in_array != null ? in_array.length : 0;
    }

    public static int lengthOf(float[] in_array) {
        return in_array != null ? in_array.length : 0;
    }

    public static <E, S, T1> List<E> listConvert(List<S> source, List<E> dest, T1 v1, PZArrayUtil.IListConverter1Param<S, E, T1> converter) {
        dest.clear();

        for (int i = 0; i < source.size(); i++) {
            dest.add(converter.convert(source.get(i), v1));
        }

        return dest;
    }

    private static <E> List<E> asList(E[] list) {
        return Arrays.asList(list);
    }

    private static List<Float> asList(float[] list) {
        return new PrimitiveFloatList(list);
    }

    private static <E> Iterable<E> asSafeIterable(E[] array) {
        return (Iterable<E>)(array != null ? asList(array) : PZEmptyIterable.getInstance());
    }

    private static Iterable<Float> asSafeIterable(float[] array) {
        return (Iterable<Float>)(array != null ? asList(array) : PZEmptyIterable.getInstance());
    }

    public static String arrayToString(float[] list) {
        return arrayToString(asSafeIterable(list));
    }

    public static String arrayToString(float[] list, String prefix, String suffix, String delimiter) {
        return arrayToString(asSafeIterable(list), prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(E[] list) {
        return arrayToString(asSafeIterable(list));
    }

    public static <E> String arrayToString(E[] list, String prefix, String suffix, String delimiter) {
        return arrayToString(asSafeIterable(list), prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(E[] list, Function<E, String> toString, String prefix, String suffix, String delimiter) {
        return arrayToString(asSafeIterable(list), toString, prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(Iterable<E> list, Function<E, String> toString) {
        return arrayToString(list, toString, "{", "}", System.lineSeparator());
    }

    public static <E> String arrayToString(Iterable<E> list) {
        return arrayToString(list, String::valueOf, "{", "}", System.lineSeparator());
    }

    public static <E> String arrayToString(Iterable<E> list, String prefix, String suffix, String delimiter) {
        return arrayToString(list, String::valueOf, prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(Iterable<E> list, Function<E, String> toString, String prefix, String suffix, String delimiter) {
        StringBuilder result = new StringBuilder(prefix);
        if (list != null) {
            boolean isFirst = true;

            for (E item : list) {
                if (!isFirst) {
                    result.append(delimiter);
                }

                String stringVal = toString.apply(item);
                result.append(stringVal);
                isFirst = false;
            }
        }

        result.append(suffix);
        Pool.tryRelease(toString);
        return result.toString();
    }

    public static <E> E[] newInstance(Class<?> componentType, int length) {
        return (E[])((Object[])Array.newInstance(componentType, length));
    }

    public static <E> E[] newInstance(Class<?> componentType, int length, Supplier<E> allocator) {
        E[] newArray = (E[])newInstance(componentType, length);
        int i = 0;

        for (int count = newArray.length; i < count; i++) {
            newArray[i] = allocator.get();
        }

        return newArray;
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength) {
        return (E[])newInstance(componentType, reusableArray, newLength, false, () -> null);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, boolean growOnly) {
        return (E[])newInstance(componentType, reusableArray, newLength, growOnly, () -> null);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, Supplier<E> newAllocator) {
        return newInstance(componentType, reusableArray, newLength, false, newAllocator);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, boolean growOnly, Supplier<E> newAllocator) {
        if (reusableArray == null) {
            return newInstance(componentType, newLength, newAllocator);
        } else {
            int oldLength = reusableArray.length;
            if (oldLength == newLength) {
                return reusableArray;
            } else if (growOnly && oldLength > newLength) {
                return reusableArray;
            } else {
                E[] newArray = (E[])newInstance(componentType, newLength);
                arrayCopy(newArray, reusableArray, 0, PZMath.min(newLength, oldLength));
                if (newLength > oldLength) {
                    for (int i = oldLength; i < newLength; i++) {
                        newArray[i] = newAllocator.get();
                    }
                }

                if (newLength < oldLength) {
                    for (int i = newLength; i < oldLength; i++) {
                        reusableArray[i] = Pool.tryRelease(reusableArray[i]);
                    }
                }

                return newArray;
            }
        }
    }

    public static float[] add(float[] array, float val) {
        int lengthOf = lengthOf(array);
        float[] newArray = new float[lengthOf + 1];
        arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static int[] add(int[] array, int val) {
        int lengthOf = lengthOf(array);
        int[] newArray = new int[lengthOf + 1];
        arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static <E> E[] add(E[] array, E val) {
        int lengthOf = lengthOf(array);
        E[] newArray = (E[])newInstance(array.getClass().getComponentType(), lengthOf + 1);
        arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static <E> E[] concat(E[] arrayA, E[] arrayB) {
        boolean arrayAEmpty = arrayA == null || arrayA.length == 0;
        boolean arrayBEmpty = arrayB == null || arrayB.length == 0;
        if (arrayAEmpty && arrayBEmpty) {
            return null;
        } else if (arrayAEmpty) {
            return (E[])shallowClone(arrayB);
        } else if (arrayBEmpty) {
            return arrayA;
        } else {
            E[] newArray = (E[])newInstance(arrayA.getClass().getComponentType(), arrayA.length + arrayB.length);
            arrayCopy(newArray, arrayA, 0, arrayA.length);
            arrayCopy(newArray, arrayB, arrayA.length, newArray.length);
            return newArray;
        }
    }

    public static <E, S extends E> E[] arrayCopy(E[] to, S[] from, int startIdx, int endIdx) {
        return (E[])arrayCopy(to, from, startIdx, endIdx, null, null);
    }

    public static <E, S extends E> E[] arrayCopy(
        E[] to, S[] from, int startIdx, int endIdx, Supplier<E> in_allocator, Invokers.Params2.ICallback<E, S> in_copier
    ) {
        if (in_copier != null) {
            for (int i = startIdx; i < endIdx; i++) {
                if (to[i] == null && in_allocator != null) {
                    to[i] = in_allocator.get();
                }

                in_copier.accept(to[i], from[i]);
            }
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                to[i] = (E)from[i];
            }
        }

        return to;
    }

    public static float[] arrayCopy(float[] to, float[] from, int startIdx, int endIdx) {
        for (int i = startIdx; i < endIdx; i++) {
            to[i] = from[i];
        }

        return to;
    }

    public static int[] arrayCopy(int[] to, int[] from, int startIdx, int endIdx) {
        for (int i = startIdx; i < endIdx; i++) {
            to[i] = from[i];
        }

        return to;
    }

    public static <L extends List<E>, E> L arrayCopy(L to, List<? extends E> from) {
        to.clear();
        to.addAll(from);
        return to;
    }

    public static <E> E[] arrayCopy(E[] to, List<? extends E> from) {
        for (int i = 0; i < from.size(); i++) {
            to[i] = (E)from.get(i);
        }

        return to;
    }

    public static <E, S extends E> E[] arrayCopy(E[] to, S[] from) {
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    public static <L extends List<E>, E, S> L arrayConvert(L to, List<S> from, Function<S, E> converter) {
        to.clear();
        int i = 0;

        for (int size = from.size(); i < size; i++) {
            S fromVal = from.get(i);
            to.add(converter.apply(fromVal));
        }

        return to;
    }

    public static float[] clone(float[] src) {
        if (isNullOrEmpty(src)) {
            return src;
        } else {
            float[] copy = new float[src.length];
            arrayCopy(copy, src, 0, src.length);
            return copy;
        }
    }

    public static <E> E[] clone(E[] src, Supplier<E> in_allocator, Invokers.Params2.ICallback<E, E> in_copier) {
        if (isNullOrEmpty(src)) {
            return src;
        } else {
            E[] copy = (E[])newInstance(src.getClass().getComponentType(), src.length);
            arrayCopy(copy, src, 0, src.length, in_allocator, in_copier);
            return copy;
        }
    }

    public static <E> E[] shallowClone(E[] src) {
        return (E[])clone(src, null, null);
    }

    public static <E> boolean isNullOrEmpty(E[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNullOrEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNullOrEmpty(float[] array) {
        return array == null || array.length == 0;
    }

    public static <E> boolean isNullOrEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    public static <E> boolean isNullOrEmpty(Iterable<E> it) {
        if (it instanceof List<E> es) {
            return isNullOrEmpty(es);
        } else {
            boolean isEmpty = true;
            Iterator var2 = it.iterator();
            if (var2.hasNext()) {
                E e = (E)var2.next();
                isEmpty = false;
            }

            return isEmpty;
        }
    }

    public static <E> E getOrDefault(List<E> list, int i) {
        return getOrDefault(list, i, null);
    }

    public static <E> E getOrDefault(List<E> list, int i, E defaultVal) {
        return i >= 0 && i < list.size() ? list.get(i) : defaultVal;
    }

    public static <E> E getOrDefault(E[] list, int i, E defaultVal) {
        return list != null && i >= 0 && i < list.length ? list[i] : defaultVal;
    }

    public static float getOrDefault(float[] list, int i, float defaultVal) {
        return list != null && i >= 0 && i < list.length ? list[i] : defaultVal;
    }

    public static int[] arraySet(int[] arr, int val) {
        if (isNullOrEmpty(arr)) {
            return arr;
        } else {
            int i = 0;

            for (int count = arr.length; i < count; i++) {
                arr[i] = val;
            }

            return arr;
        }
    }

    public static float[] arraySet(float[] arr, float val) {
        if (isNullOrEmpty(arr)) {
            return arr;
        } else {
            int i = 0;

            for (int count = arr.length; i < count; i++) {
                arr[i] = val;
            }

            return arr;
        }
    }

    public static <E> E[] arraySet(E[] arr, E val) {
        if (isNullOrEmpty(arr)) {
            return arr;
        } else {
            int i = 0;

            for (int count = arr.length; i < count; i++) {
                arr[i] = val;
            }

            return arr;
        }
    }

    public static <E> E[] arrayPopulate(E[] arr, Supplier<E> supplier) {
        return arrayPopulate(arr, supplier, 0, lengthOf(arr));
    }

    public static <E> E[] arrayPopulate(E[] arr, Supplier<E> supplier, int in_startIdx, int in_endIdx) {
        if (isNullOrEmpty(arr)) {
            return arr;
        } else {
            for (int i = in_startIdx; i < in_endIdx; i++) {
                arr[i] = supplier.get();
            }

            return arr;
        }
    }

    public static void insertAt(int[] arr, int insertAt, int val) {
        for (int i = arr.length - 1; i > insertAt; i--) {
            arr[i] = arr[i - 1];
        }

        arr[insertAt] = val;
    }

    public static void insertAt(float[] arr, int insertAt, float val) {
        for (int i = arr.length - 1; i > insertAt; i--) {
            arr[i] = arr[i - 1];
        }

        arr[insertAt] = val;
    }

    public static <E> E[] toArray(List<E> list) {
        if (list != null && !list.isEmpty()) {
            E[] newArray = (E[])newInstance(list.get(0).getClass(), list.size());
            arrayCopy(newArray, list);
            return newArray;
        } else {
            return null;
        }
    }

    public static <E> int indexOf(E[] arr, int count, E val) {
        for (int i = 0; i < count; i++) {
            if (arr[i] == val) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOf(float[] arr, int count, float val) {
        for (int i = 0; i < count; i++) {
            if (arr[i] == val) {
                return i;
            }
        }

        return -1;
    }

    public static boolean contains(float[] arr, int count, float val) {
        return indexOf(arr, count, val) != -1;
    }

    public static int indexOf(int[] arr, int count, int val) {
        for (int i = 0; i < count; i++) {
            if (arr[i] == val) {
                return i;
            }
        }

        return -1;
    }

    public static boolean contains(int[] arr, int count, int val) {
        return indexOf(arr, count, val) != -1;
    }

    public static <E> void forEach(List<E> list, Consumer<? super E> consumer) {
        try {
            if (list == null) {
                return;
            }

            int i = 0;

            for (int count = list.size(); i < count; i++) {
                E element = list.get(i);
                consumer.accept(element);
            }
        } finally {
            Pool.tryRelease(consumer);
        }
    }

    public static <E> void forEach(Iterable<E> it, Consumer<? super E> consumer) {
        if (it == null) {
            Pool.tryRelease(consumer);
        } else if (it instanceof List<E> es) {
            forEach(es, consumer);
        } else {
            try {
                for (E element : it) {
                    consumer.accept(element);
                }
            } finally {
                Pool.tryRelease(consumer);
            }
        }
    }

    public static <E> void forEach(E[] elements, Consumer<? super E> consumer) {
        if (!isNullOrEmpty(elements)) {
            int i = 0;

            for (int elementsLength = elements.length; i < elementsLength; i++) {
                consumer.accept(elements[i]);
            }
        }
    }

    public static <E> void forEachReplace(List<E> list, Function<? super E, ? super E> replacer) {
        try {
            if (list == null) {
                return;
            }

            int i = 0;

            for (int count = list.size(); i < count; i++) {
                E element = list.get(i);
                E replacement = (E)replacer.apply(element);
                list.set(i, replacement);
            }
        } finally {
            Pool.tryRelease(replacer);
        }
    }

    public static <K, V> V getOrCreate(HashMap<K, V> map, K key, Supplier<V> allocator) {
        V val = map.get(key);
        if (val == null) {
            val = allocator.get();
            map.put(key, val);
        }

        return val;
    }

    public static <E> void sort(Stack<E> stack, Comparator<E> comparator) {
        try {
            stack.sort(comparator);
        } finally {
            Pool.tryRelease(comparator);
        }
    }

    public static <E> boolean sequenceEqual(E[] a, List<? extends E> b) {
        return sequenceEqual(a, b, PZArrayUtil.Comparators::objectsEqual);
    }

    public static <E> boolean sequenceEqual(E[] a, List<? extends E> b, Comparator<E> comparator) {
        return a.length == b.size() && sequenceEqual(asList(a), b, comparator);
    }

    public static <E> boolean sequenceEqual(List<? extends E> a, List<? extends E> b) {
        return sequenceEqual(a, b, PZArrayUtil.Comparators::objectsEqual);
    }

    public static <E> boolean sequenceEqual(List<? extends E> a, List<? extends E> b, Comparator<E> comparator) {
        if (a.size() != b.size()) {
            return false;
        } else {
            boolean equals = true;
            int i = 0;

            for (int count = a.size(); i < count; i++) {
                E valA = (E)a.get(i);
                E valB = (E)b.get(i);
                if (comparator.compare(valA, valB) != 0) {
                    equals = false;
                    break;
                }
            }

            return equals;
        }
    }

    public static int[] arrayAdd(int[] in_out_a, int[] b) {
        for (int i = 0; i < in_out_a.length; i++) {
            in_out_a[i] += b[i];
        }

        return in_out_a;
    }

    public static <E> void addAll(ArrayList<E> dest, List<E> src) {
        dest.ensureCapacity(dest.size() + src.size());

        for (int i = 0; i < src.size(); i++) {
            dest.add(src.get(i));
        }
    }

    public static <E> void addAll(PZArrayList<E> dest, List<E> src) {
        dest.ensureCapacity(dest.size() + src.size());

        for (int i = 0; i < src.size(); i++) {
            dest.add(src.get(i));
        }
    }

    public static class Comparators {
        public static <E> int referencesEqual(E a, E b) {
            return a == b ? 0 : 1;
        }

        public static <E> int objectsEqual(E a, E b) {
            return a != null && a.equals(b) ? 0 : 1;
        }

        public static int equalsIgnoreCase(String a, String b) {
            return StringUtils.equals(a, b) ? 0 : 1;
        }
    }

    public interface IListConverter1Param<S, E, T1> {
        E convert(S var1, T1 var2);
    }
}
