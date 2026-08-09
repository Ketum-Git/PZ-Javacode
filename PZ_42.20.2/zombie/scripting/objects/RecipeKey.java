// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public interface RecipeKey {
    String getTranslationName();

    ResourceLocation getRegistryId();

    static RecipeKey fromId(ResourceLocation id) {
        for (Registry<?> registry : Registries.getAllRecipeRegistries()) {
            if (registry.contains(id)) {
                return (RecipeKey)registry.get(id);
            }
        }

        throw new IllegalArgumentException("Unknown recipe ID: " + id);
    }
}
