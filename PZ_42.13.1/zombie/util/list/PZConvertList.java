// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class PZConvertList<S, T> extends AbstractList<T> implements RandomAccess {
    private final List<S> list;
    private final Function<S, T> converterSt;
    private final Function<T, S> converterTs;

    public PZConvertList(List<S> list, Function<S, T> converterSt) {
        this(list, converterSt, null);
    }

    public PZConvertList(List<S> list, Function<S, T> converterSt, Function<T, S> converterTs) {
        this.list = Objects.requireNonNull(list);
        this.converterSt = converterSt;
        this.converterTs = converterTs;
    }

    public boolean isReadonly() {
        return this.converterTs == null;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Object[] toArray() {
        return this.list.toArray();
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
        return this.convertST(this.list.get(index));
    }

    @Override
    public T set(int index, T element) {
        T oldValue = this.get(index);
        this.setS(index, this.convertTS(element));
        return oldValue;
    }

    public S setS(int index, S element) {
        S oldValue = this.list.get(index);
        this.list.set(index, element);
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
        int i = 0;

        for (int count = this.size(); i < count; i++) {
            T oldValue = this.get(i);
            T newValue = operator.apply(oldValue);
            this.set(i, newValue);
        }
    }

    @Override
    public void sort(Comparator<? super T> c) {
        this.list.sort((o1, o2) -> c.compare(this.convertST((S)o1), this.convertST((S)o2)));
    }

    private T convertST(S s) {
        return this.converterSt.apply(s);
    }

    private S convertTS(T t) {
        return this.converterTs.apply(t);
    }
}
