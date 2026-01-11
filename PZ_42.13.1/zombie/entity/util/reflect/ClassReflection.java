// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.reflect;

public final class ClassReflection {
    public static Constructor getConstructor(Class<?> c, Class<?>... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getConstructor(parameterTypes));
        } catch (SecurityException var3) {
            throw new ReflectionException("Security violation occurred while getting constructor for class: '" + c.getName() + "'.", var3);
        } catch (NoSuchMethodException var4) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), var4);
        }
    }

    public static Constructor getDeclaredConstructor(Class<?> c, Class<?>... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getDeclaredConstructor(parameterTypes));
        } catch (SecurityException var3) {
            throw new ReflectionException("Security violation while getting constructor for class: " + c.getName(), var3);
        } catch (NoSuchMethodException var4) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), var4);
        }
    }
}
