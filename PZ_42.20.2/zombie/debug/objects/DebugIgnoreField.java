// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.objects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DebugIgnoreField {
}
