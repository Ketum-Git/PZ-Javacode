// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import zombie.UsedFromLua;
import zombie.entity.util.BitSet;
import zombie.entity.util.ObjectMap;

@UsedFromLua
public class Family {
    private static final ObjectMap<String, Family> families = new ObjectMap<>();
    private static int familyIndex;
    private static final Family.Builder builder = new Family.Builder();
    private static final BitSet zeroBits = new BitSet();
    private final BitSet all;
    private final BitSet one;
    private final BitSet exclude;
    private final int index;

    private Family(BitSet all, BitSet any, BitSet exclude) {
        this.all = all;
        this.one = any;
        this.exclude = exclude;
        this.index = familyIndex++;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean matches(GameEntity entity) {
        BitSet entityComponentBits = entity.getComponentBits();
        if (entityComponentBits == null) {
            return false;
        } else if (!entityComponentBits.containsAll(this.all)) {
            return false;
        } else {
            return !this.one.isEmpty() && !this.one.intersects(entityComponentBits)
                ? false
                : this.exclude.isEmpty() || !this.exclude.intersects(entityComponentBits);
        }
    }

    public static final Family.Builder all(ComponentType... componentTypes) {
        return builder.reset().all(componentTypes);
    }

    public static final Family.Builder one(ComponentType... componentTypes) {
        return builder.reset().one(componentTypes);
    }

    public static final Family.Builder exclude(ComponentType... componentTypes) {
        return builder.reset().exclude(componentTypes);
    }

    @Override
    public int hashCode() {
        return this.index;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    private static String getFamilyHash(BitSet all, BitSet one, BitSet exclude) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!all.isEmpty()) {
            stringBuilder.append("{all:").append(getBitsString(all)).append("}");
        }

        if (!one.isEmpty()) {
            stringBuilder.append("{one:").append(getBitsString(one)).append("}");
        }

        if (!exclude.isEmpty()) {
            stringBuilder.append("{exclude:").append(getBitsString(exclude)).append("}");
        }

        return stringBuilder.toString();
    }

    private static String getBitsString(BitSet bits) {
        StringBuilder stringBuilder = new StringBuilder();
        int numBits = bits.length();

        for (int i = 0; i < numBits; i++) {
            stringBuilder.append(bits.get(i) ? "1" : "0");
        }

        return stringBuilder.toString();
    }

    public static class Builder {
        private BitSet all = Family.zeroBits;
        private BitSet one = Family.zeroBits;
        private BitSet exclude = Family.zeroBits;

        Builder() {
        }

        public Family.Builder reset() {
            this.all = Family.zeroBits;
            this.one = Family.zeroBits;
            this.exclude = Family.zeroBits;
            return this;
        }

        public final Family.Builder all(ComponentType... componentTypes) {
            this.all = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public final Family.Builder one(ComponentType... componentTypes) {
            this.one = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public final Family.Builder exclude(ComponentType... componentTypes) {
            this.exclude = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public Family get() {
            String hash = Family.getFamilyHash(this.all, this.one, this.exclude);
            Family family = Family.families.get(hash, null);
            if (family == null) {
                family = new Family(this.all, this.one, this.exclude);
                Family.families.put(hash, family);
            }

            return family;
        }
    }
}
