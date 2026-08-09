// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.reflect;

import java.lang.reflect.InvocationTargetException;

public final class Constructor {
    private final java.lang.reflect.Constructor<?> constructor;

    Constructor(java.lang.reflect.Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Class<?>[] getParameterTypes() {
        return this.constructor.getParameterTypes();
    }

    public Class<?> getDeclaringClass() {
        return this.constructor.getDeclaringClass();
    }

    public boolean isAccessible() {
        return this.constructor.isAccessible();
    }

    public void setAccessible(boolean accessible) {
        this.constructor.setAccessible(accessible);
    }

    public Object newInstance(Object... args) throws ReflectionException {
        try {
            return this.constructor.newInstance(args);
        } catch (IllegalArgumentException var3) {
            throw new ReflectionException("Illegal argument(s) supplied to constructor for class: " + this.getDeclaringClass().getName(), var3);
        } catch (IllegalAccessException | InstantiationException var4) {
            throw new ReflectionException("Could not instantiate instance of class: " + this.getDeclaringClass().getName(), var4);
        } catch (InvocationTargetException var5) {
            throw new ReflectionException("Exception occurred in constructor for class: " + this.getDeclaringClass().getName(), var5);
        }
    }
}
