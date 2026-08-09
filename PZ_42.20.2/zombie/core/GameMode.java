// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import generation.builders.validation.TranslationKeyValidator;
import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class GameMode {
    public static final GameMode APOCALYPSE = registerBase("Apocalypse");
    public static final GameMode OUTBREAK = registerBase("Outbreak");
    public static final GameMode EXTINCTION = registerBase("Extinction");
    public static final GameMode RISING = registerBase("Rising");
    public static final GameMode SANDBOX = registerBase("Sandbox");
    public static final GameMode CHALLENGES = registerBase("Challenges");
    private final String id;
    private final String title;
    private final String description;
    private final String thumbnail;
    private final String video;

    private GameMode(String id) {
        this.id = id;
        this.title = "UI_NewGame_%s".formatted(id);
        this.description = "%s_desc".formatted(this.title);
        this.thumbnail = "media/ui/playstyleIcons/%s.png".formatted(id);
        this.video = "%s.bik".formatted(id);
    }

    public static GameMode get(ResourceLocation id) {
        return Registries.GAME_MODE.get(id);
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    public String getVideo() {
        return this.video;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public static GameMode register(String id) {
        return register(false, id);
    }

    private static GameMode registerBase(String id) {
        return register(true, id);
    }

    private static GameMode register(boolean allowDefaultNamespace, String id) {
        return Registries.GAME_MODE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new GameMode(id));
    }

    static {
        if (Core.IS_DEV) {
            for (GameMode gameMode : Registries.GAME_MODE) {
                TranslationKeyValidator.of(gameMode.title);
                TranslationKeyValidator.of(gameMode.description);
            }
        }
    }
}
