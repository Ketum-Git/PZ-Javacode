// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

public class HashMap {
    private int capacity = 2;
    private int elements;
    private HashMap.Bucket[] buckets = new HashMap.Bucket[this.capacity];

    public HashMap() {
        for (int i = 0; i < this.capacity; i++) {
            this.buckets[i] = new HashMap.Bucket();
        }
    }

    public void clear() {
        this.elements = 0;

        for (int i = 0; i < this.capacity; i++) {
            this.buckets[i].clear();
        }
    }

    private void grow() {
        HashMap.Bucket[] oldBuckets = this.buckets;
        this.capacity *= 2;
        this.elements = 0;
        this.buckets = new HashMap.Bucket[this.capacity];

        for (int i = 0; i < this.capacity; i++) {
            this.buckets[i] = new HashMap.Bucket();
        }

        for (int i = 0; i < oldBuckets.length; i++) {
            HashMap.Bucket bucket = oldBuckets[i];

            for (int j = 0; j < bucket.size(); j++) {
                if (bucket.keys[j] != null) {
                    this.put(bucket.keys[j], bucket.values[j]);
                }
            }
        }
    }

    public Object get(Object key) {
        HashMap.Bucket bucket = this.buckets[Math.abs(key.hashCode()) % this.capacity];

        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.keys[i] != null && bucket.keys[i].equals(key)) {
                return bucket.values[i];
            }
        }

        return null;
    }

    public Object remove(Object key) {
        HashMap.Bucket bucket = this.buckets[Math.abs(key.hashCode()) % this.capacity];
        Object value = bucket.remove(key);
        if (value != null) {
            this.elements--;
            return value;
        } else {
            return null;
        }
    }

    public Object put(Object key, Object value) {
        if (this.elements + 1 >= this.buckets.length) {
            this.grow();
        }

        Object oldValue = this.remove(key);
        HashMap.Bucket bucket = this.buckets[Math.abs(key.hashCode()) % this.capacity];
        bucket.put(key, value);
        this.elements++;
        return oldValue;
    }

    public int size() {
        return this.elements;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public HashMap.Iterator iterator() {
        return new HashMap.Iterator(this);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        for (int i = 0; i < this.buckets.length; i++) {
            HashMap.Bucket bucket = this.buckets[i];

            for (int j = 0; j < bucket.size(); j++) {
                if (bucket.keys[j] != null) {
                    if (!string.isEmpty()) {
                        string.append(", ");
                    }

                    string.append(bucket.keys[j]).append("=").append(bucket.values[j]);
                }
            }
        }

        string = new StringBuilder("HashMap[" + string + "]");
        return string.toString();
    }

    private static class Bucket {
        public Object[] keys;
        public Object[] values;
        public int count;
        public int nextIndex;

        public void put(Object key, Object value) throws IllegalStateException {
            if (this.keys == null) {
                this.grow();
                this.keys[0] = key;
                this.values[0] = value;
                this.nextIndex = 1;
                this.count = 1;
            } else {
                if (this.count == this.keys.length) {
                    this.grow();
                }

                for (int i = 0; i < this.keys.length; i++) {
                    if (this.keys[i] == null) {
                        this.keys[i] = key;
                        this.values[i] = value;
                        this.count++;
                        this.nextIndex = Math.max(this.nextIndex, i + 1);
                        return;
                    }
                }

                throw new IllegalStateException("bucket is full");
            }
        }

        public Object remove(Object key) {
            for (int i = 0; i < this.nextIndex; i++) {
                if (this.keys[i] != null && this.keys[i].equals(key)) {
                    Object value = this.values[i];
                    this.keys[i] = null;
                    this.values[i] = null;
                    this.count--;
                    return value;
                }
            }

            return null;
        }

        private void grow() {
            if (this.keys == null) {
                this.keys = new Object[2];
                this.values = new Object[2];
            } else {
                Object[] oldKeys = this.keys;
                Object[] oldValues = this.values;
                this.keys = new Object[oldKeys.length * 2];
                this.values = new Object[oldValues.length * 2];
                System.arraycopy(oldKeys, 0, this.keys, 0, oldKeys.length);
                System.arraycopy(oldValues, 0, this.values, 0, oldValues.length);
            }
        }

        public int size() {
            return this.nextIndex;
        }

        public void clear() {
            for (int i = 0; i < this.nextIndex; i++) {
                this.keys[i] = null;
                this.values[i] = null;
            }

            this.count = 0;
            this.nextIndex = 0;
        }
    }

    public static class Iterator {
        private final HashMap hashMap;
        private int bucketIdx;
        private int keyValuePairIdx;
        private int elementIdx;
        private Object currentKey;
        private Object currentValue;

        public Iterator(HashMap hashmap) {
            this.hashMap = hashmap;
            this.reset();
        }

        public HashMap.Iterator reset() {
            this.bucketIdx = 0;
            this.keyValuePairIdx = 0;
            this.elementIdx = 0;
            this.currentKey = null;
            this.currentValue = null;
            return this;
        }

        public boolean hasNext() {
            return this.elementIdx < this.hashMap.elements;
        }

        public boolean advance() {
            while (this.bucketIdx < this.hashMap.buckets.length) {
                HashMap.Bucket bucket = this.hashMap.buckets[this.bucketIdx];
                if (this.keyValuePairIdx == bucket.size()) {
                    this.keyValuePairIdx = 0;
                    this.bucketIdx++;
                } else {
                    while (this.keyValuePairIdx < bucket.size()) {
                        if (bucket.keys[this.keyValuePairIdx] != null) {
                            this.currentKey = bucket.keys[this.keyValuePairIdx];
                            this.currentValue = bucket.values[this.keyValuePairIdx];
                            this.keyValuePairIdx++;
                            this.elementIdx++;
                            return true;
                        }

                        this.keyValuePairIdx++;
                    }

                    this.keyValuePairIdx = 0;
                    this.bucketIdx++;
                }
            }

            return false;
        }

        public Object getKey() {
            return this.currentKey;
        }

        public Object getValue() {
            return this.currentValue;
        }
    }
}
