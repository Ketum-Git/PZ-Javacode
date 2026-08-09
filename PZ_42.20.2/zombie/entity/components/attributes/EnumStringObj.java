// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public class EnumStringObj<E extends Enum<E> & IOEnum> {
    private final ArrayList<String> stringValues = new ArrayList<>();
    private EnumSet<E> enumValues;
    private boolean dirty;

    protected EnumStringObj() {
    }

    protected void initialize(Class<E> elementType) {
        this.enumValues = EnumSet.noneOf(elementType);
    }

    protected void reset() {
        this.clear();
        this.enumValues = null;
    }

    protected EnumSet<E> getEnumValues() {
        return this.enumValues;
    }

    protected ArrayList<String> getStringValues() {
        return this.stringValues;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EnumStringObj<E> other)) {
            return false;
        } else if (!this.enumValues.equals(other.enumValues)) {
            return false;
        } else if (this.stringValues.isEmpty() && other.stringValues.isEmpty()) {
            return true;
        } else if (this.stringValues.size() != other.stringValues.size()) {
            return false;
        } else {
            this.sort();
            other.sort();
            return this.stringValues.equals(other.stringValues);
        }
    }

    @Override
    public String toString() {
        if (this.enumValues.isEmpty() && this.stringValues.isEmpty()) {
            return "E[] S[]";
        } else {
            String s = "E" + this.enumValues;
            this.sort();
            s = s + " S[";

            for (int i = 0; i < this.stringValues.size(); i++) {
                s = s + this.stringValues.get(i);
                if (i < this.stringValues.size() - 1) {
                    s = s + ",";
                }
            }

            return s + "]";
        }
    }

    public EnumStringObj<E> copy() {
        EnumStringObj<E> copy = new EnumStringObj<>();
        copy.enumValues = EnumSet.copyOf(this.enumValues);
        copy.stringValues.addAll(this.stringValues);
        return copy;
    }

    private void sort() {
        if (this.dirty) {
            Collections.sort(this.stringValues);
            this.dirty = false;
        }
    }

    public void getSortedNames(ArrayList<String> list) {
        if (!list.isEmpty()) {
            list.clear();
        }

        list.addAll(this.stringValues);

        for (E e : this.enumValues) {
            list.add(e.toString());
        }

        Collections.sort(list);
    }

    public int size() {
        return this.enumValues.size() + this.stringValues.size();
    }

    public int sizeEnums() {
        return this.enumValues.size();
    }

    public int sizeStrings() {
        return this.stringValues.size();
    }

    public void clear() {
        this.enumValues.clear();
        this.stringValues.clear();
        this.dirty = true;
    }

    public boolean isEmpty() {
        return this.enumValues.isEmpty() && this.stringValues.isEmpty();
    }

    public void add(E e) {
        this.enumValues.add(e);
    }

    public void add(String s) {
        if (this.stringValues.size() >= 127) {
            throw new UnsupportedOperationException("String values size may not exceed: 127");
        } else {
            if (s != null && !this.stringValues.contains(s)) {
                this.stringValues.add(s);
                this.dirty = true;
            }
        }
    }

    public boolean remove(E e) {
        return this.enumValues.remove(e);
    }

    public boolean remove(String s) {
        this.dirty = true;
        return this.stringValues.remove(s);
    }

    public boolean contains(E o) {
        return this.enumValues.contains(o);
    }

    public boolean contains(String o) {
        return this.stringValues.contains(o);
    }

    public void removeAllStrings() {
        this.stringValues.clear();
    }

    public void removeAllEnums() {
        this.enumValues.clear();
    }

    public void addAll(boolean clearAll, EnumStringObj<E> c) {
        if (clearAll) {
            if (!this.stringValues.isEmpty()) {
                this.stringValues.clear();
            }

            if (!this.enumValues.isEmpty()) {
                this.enumValues.clear();
            }
        }

        this.dirty = true;
        this.stringValues.addAll(c.stringValues);
        this.enumValues.addAll(c.enumValues);
    }

    public void addAll(EnumStringObj<E> c) {
        this.addAll(false, c);
    }
}
