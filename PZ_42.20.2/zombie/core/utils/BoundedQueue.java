// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.util.NoSuchElementException;

public class BoundedQueue<E> {
    private final int numElements;
    private int front;
    private int rear;
    private final E[] elements;

    public BoundedQueue(int numElements) {
        this.numElements = numElements;
        int size = Math.max(numElements, 16);
        size = Integer.highestOneBit(size - 1) << 1;
        this.elements = (E[])(new Object[size]);
    }

    public void add(E e) {
        if (e == null) {
            throw new NullPointerException();
        } else {
            if (this.size() == this.numElements) {
                this.removeFirst();
            }

            this.elements[this.rear] = e;
            this.rear = this.rear + 1 & this.elements.length - 1;
        }
    }

    public E removeFirst() {
        E element = this.elements[this.front];
        if (element == null) {
            throw new NoSuchElementException();
        } else {
            this.elements[this.front] = null;
            this.front = this.front + 1 & this.elements.length - 1;
            return element;
        }
    }

    public E remove(int index) {
        int removedPos = this.front + index & this.elements.length - 1;
        E element = this.elements[removedPos];
        if (element == null) {
            throw new NoSuchElementException();
        } else {
            int cursor = removedPos;

            while (cursor != this.front) {
                int next = cursor - 1 & this.elements.length - 1;
                this.elements[cursor] = this.elements[next];
                cursor = next;
            }

            this.front = this.front + 1 & this.elements.length - 1;
            this.elements[cursor] = null;
            return element;
        }
    }

    public E get(int index) {
        int i = this.front + index & this.elements.length - 1;
        E result = this.elements[i];
        if (result == null) {
            throw new NoSuchElementException();
        } else {
            return result;
        }
    }

    public void clear() {
        while (this.front != this.rear) {
            this.elements[this.front] = null;
            this.front = this.front + 1 & this.elements.length - 1;
        }

        this.front = this.rear = 0;
    }

    public int capacity() {
        return this.numElements;
    }

    public int size() {
        return this.front <= this.rear ? this.rear - this.front : this.rear + this.elements.length - this.front;
    }

    public boolean isEmpty() {
        return this.front == this.rear;
    }

    public boolean isFull() {
        return this.size() == this.capacity();
    }
}
