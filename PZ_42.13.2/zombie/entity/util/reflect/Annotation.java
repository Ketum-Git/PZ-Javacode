// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.reflect;

public final class Annotation {
    private final java.lang.annotation.Annotation annotation;

    Annotation(java.lang.annotation.Annotation annotation) {
        this.annotation = annotation;
    }

    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationType) {
        return (T)(this.annotation.annotationType().equals(annotationType) ? this.annotation : null);
    }

    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return this.annotation.annotationType();
    }
}
