// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class PZConvertArray<S, T> extends AbstractList<T> implements RandomAccess {
    private final S[] array;
    private final Function<S, T> converterSt;
    private final Function<T, S> converterTs;

    public PZConvertArray(S[] array, Function<S, T> converterSt) {
        this(array, converterSt, null);
    }

    public PZConvertArray(S[] array, Function<S, T> converterSt, Function<T, S> converterTs) {
        this.array = (S[])((Object[])Objects.requireNonNull((T)array));
        this.converterSt = converterSt;
        this.converterTs = converterTs;
    }

    public boolean isReadonly() {
        return this.converterTs == null;
    }

    @Override
    public int size() {
        return this.array.length;
    }

    @Override
    public Object[] toArray() {
        return Arrays.asList(this.array).toArray();
    }

    @Override
    public <R> R[] toArray(R[] result) {
        int count = this.size();

        for (int i = 0; i < count && i < result.length; i++) {
            R val = (R)this.get(i);
            result[i] = val;
        }

        if (result.length > count) {
            result[count] = null;
        }

        return result;
    }

    @Override
    public T get(int index) {
        return this.convertST(this.array[index]);
    }

    @Override
    public T set(int index, T element) {
        T oldValue = this.get(index);
        this.setS(index, this.convertTS(element));
        return oldValue;
    }

    public S setS(int index, S element) {
        S oldValue = this.array[index];
        this.array[index] = element;
        return oldValue;
    }

    @Override
    public int indexOf(Object val) {
        int indexOf = -1;
        int i = 0;

        for (int count = this.size(); i < count; i++) {
            if (objectsEqual(val, this.get(i))) {
                indexOf = i;
                break;
            }
        }

        return indexOf;
    }

    private static boolean objectsEqual(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) != -1;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        int i = 0;

        for (int count = this.size(); i < count; i++) {
            action.accept(this.get(i));
        }
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        Objects.requireNonNull(operator);
        S[] a = this.array;

        for (int i = 0; i < a.length; i++) {
            T t = this.get(i);
            T c = operator.apply(t);
            a[i] = this.convertTS(c);
        }
    }

    @Override
    public void sort(Comparator<? super T> c) {
        Arrays.sort(this.array, (o1, o2) -> c.compare(this.convertST((S)o1), this.convertST((S)o2)));
    }

    private T convertST(S s) {
        return this.converterSt.apply(s);
    }

    private S convertTS(T t) {
        return this.converterTs.apply(t);
    }
}
