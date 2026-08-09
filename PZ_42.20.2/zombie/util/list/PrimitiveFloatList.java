// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class PrimitiveFloatList extends AbstractList<Float> implements RandomAccess {
    private final float[] array;

    public PrimitiveFloatList(float[] array) {
        this.array = Objects.requireNonNull(array);
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
    public <T> T[] toArray(T[] result) {
        int count = this.size();

        for (int i = 0; i < count && i < result.length; i++) {
            Float val = this.array[i];
            result[i] = (T)val;
        }

        if (result.length > count) {
            result[count] = null;
        }

        return result;
    }

    public Float get(int index) {
        return this.array[index];
    }

    public Float set(int index, Float element) {
        return this.set(index, element.floatValue());
    }

    public float set(int index, float element) {
        float oldValue = this.array[index];
        this.array[index] = element;
        return oldValue;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            return -1;
        } else {
            return o instanceof Number number ? this.indexOf(number.floatValue()) : -1;
        }
    }

    public int indexOf(float val) {
        int indexOf = -1;
        int i = 0;

        for (int count = this.size(); i < count; i++) {
            if (this.array[i] == val) {
                indexOf = i;
                break;
            }
        }

        return indexOf;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) != -1;
    }

    public boolean contains(float val) {
        return this.indexOf(val) != -1;
    }

    @Override
    public void forEach(Consumer<? super Float> action) {
        this.forEach(action::accept);
    }

    public void forEach(FloatConsumer action) {
        int i = 0;

        for (int count = this.size(); i < count; i++) {
            action.accept(this.array[i]);
        }
    }

    @Override
    public void replaceAll(UnaryOperator<Float> operator) {
        Objects.requireNonNull(operator);
        float[] a = this.array;

        for (int i = 0; i < a.length; i++) {
            a[i] = operator.apply(a[i]);
        }
    }

    @Override
    public void sort(Comparator<? super Float> unused) {
        this.sort();
    }

    public void sort() {
        Arrays.sort(this.array);
    }
}
