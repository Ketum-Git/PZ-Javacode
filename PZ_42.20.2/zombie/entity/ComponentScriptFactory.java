// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import org.jspecify.annotations.Nullable;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;
import zombie.scripting.entity.ComponentScript;

public class ComponentScriptFactory {
    private final ObjectMap<Class<?>, ComponentScriptFactory.ScriptConstructor> scriptConstructors = new ObjectMap<>();

    public <T extends ComponentScript> T create(Class<T> componentScriptClass) {
        ComponentScriptFactory.ScriptConstructor pool = this.scriptConstructors.get(componentScriptClass);
        if (pool == null) {
            pool = new ComponentScriptFactory.ScriptConstructor<>(componentScriptClass);
            this.scriptConstructors.put(componentScriptClass, pool);
        }

        return (T)pool.obtain();
    }

    private static class ScriptConstructor<T extends ComponentScript> {
        private final Constructor constructor;

        public ScriptConstructor(Class<T> type) {
            this.constructor = this.findConstructor(type);
            if (this.constructor == null) {
                throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + type.getName());
            }
        }

        private @Nullable Constructor findConstructor(Class<T> type) {
            try {
                return ClassReflection.getConstructor(type, (Class<?>[])null);
            } catch (Exception var5) {
                try {
                    Constructor constructor = ClassReflection.getDeclaredConstructor(type, (Class<?>[])null);
                    constructor.setAccessible(true);
                    return constructor;
                } catch (ReflectionException var4) {
                    return null;
                }
            }
        }

        protected T obtain() {
            try {
                return (T)this.constructor.newInstance((Object[])null);
            } catch (Exception var2) {
                throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), var2);
            }
        }
    }
}
