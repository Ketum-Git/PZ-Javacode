// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public class RegistryReset {
    public static void resetAll() {
        for (Registry<?> registry : Registries.REGISTRY) {
            registry.reset();
        }

        Registries.REGISTRY.reset();
    }

    public static ResourceLocation createLocation(String id, boolean allowsBaseNamespace) {
        ResourceLocation rl = ResourceLocation.of(id);
        if (!allowsBaseNamespace && "base".equals(rl.getNamespace())) {
            throw new IllegalArgumentException(String.format("Default namespace '%s' is not allowed!", rl));
        } else {
            return rl;
        }
    }
}
