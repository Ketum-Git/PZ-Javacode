// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ErosionObjSprites {
    public static final int SECTION_BASE = 0;
    public static final int SECTION_SNOW = 1;
    public static final int SECTION_FLOWER = 2;
    public static final int SECTION_CHILD = 3;
    public static final int NUM_SECTIONS = 4;
    public String name;
    public int stages;
    public boolean hasSnow;
    public boolean hasFlower;
    public boolean hasChildSprite;
    public boolean noSeasonBase;
    public int cycleTime = 1;
    private final Map<String, ErosionObjSprites.Entry> revLookup = new HashMap<>();
    private final ErosionObjSprites.Stage[] sprites;

    public ErosionObjSprites(int stages, String name, boolean hasSnow, boolean hasFlower, boolean hasChildsprite) {
        this.name = name;
        this.stages = stages;
        this.hasSnow = hasSnow;
        this.hasFlower = hasFlower;
        this.hasChildSprite = hasChildsprite;
        this.sprites = new ErosionObjSprites.Stage[stages];

        for (int i = 0; i < stages; i++) {
            this.sprites[i] = new ErosionObjSprites.Stage();
            this.sprites[i].sections[0] = new ErosionObjSprites.Section();
            if (this.hasSnow) {
                this.sprites[i].sections[1] = new ErosionObjSprites.Section();
            }

            if (this.hasFlower) {
                this.sprites[i].sections[2] = new ErosionObjSprites.Section();
            }

            if (this.hasChildSprite) {
                this.sprites[i].sections[3] = new ErosionObjSprites.Section();
            }
        }
    }

    private String getSprite(int stage, int section, int season) {
        return this.sprites[stage] != null && this.sprites[stage].sections[section] != null && this.sprites[stage].sections[section].seasons[season] != null
            ? this.sprites[stage].sections[section].seasons[season].getNext()
            : null;
    }

    public String getBase(int stage, int season) {
        return this.getSprite(stage, 0, season);
    }

    public String getFlower(int stage) {
        return this.hasFlower ? this.getSprite(stage, 2, 0) : null;
    }

    public String getChildSprite(int stage, int season) {
        return this.hasChildSprite ? this.getSprite(stage, 3, season) : null;
    }

    public ErosionObjSprites.Entry getEntry(String sprite) {
        return this.revLookup.getOrDefault(sprite, null);
    }

    private void setSprite(int stage, int section, String sprite, int season) {
        if (this.sprites[stage] != null && this.sprites[stage].sections[section] != null) {
            this.sprites[stage].sections[section].seasons[season] = new ErosionObjSprites.Sprites(sprite);
            this.revLookup.put(sprite, new ErosionObjSprites.Entry(stage, section, season));
        }
    }

    private void setSprite(int stage, int section, ArrayList<String> sprites, int season) {
        assert !sprites.isEmpty();

        if (this.sprites[stage] != null && this.sprites[stage].sections[section] != null) {
            this.sprites[stage].sections[section].seasons[season] = new ErosionObjSprites.Sprites(sprites);
            sprites.forEach(s -> this.revLookup.put(s, new ErosionObjSprites.Entry(stage, section, season)));
        }
    }

    public void setBase(int stage, String sprite, int season) {
        this.setSprite(stage, 0, sprite, season);
    }

    public void setBase(int stage, ArrayList<String> sprites, int season) {
        this.setSprite(stage, 0, sprites, season);
    }

    public void setFlower(int stage, String sprite) {
        this.setSprite(stage, 2, sprite, 0);
    }

    public void setFlower(int stage, ArrayList<String> sprites) {
        this.setSprite(stage, 2, sprites, 0);
    }

    public void setChildSprite(int stage, String sprite, int season) {
        this.setSprite(stage, 3, sprite, season);
    }

    public void setChildSprite(int stage, ArrayList<String> sprites, int season) {
        this.setSprite(stage, 3, sprites, season);
    }

    public record Entry(int stage, int section, int season) {
    }

    private static class Section {
        public ErosionObjSprites.Sprites[] seasons = new ErosionObjSprites.Sprites[6];
    }

    private static final class Sprites {
        public final ArrayList<String> sprites = new ArrayList<>();
        private int index = -1;

        public Sprites(String sprite) {
            this.sprites.add(sprite);
        }

        public Sprites(ArrayList<String> sprites) {
            this.sprites.addAll(sprites);
        }

        public String getNext() {
            if (++this.index >= this.sprites.size()) {
                this.index = 0;
            }

            return this.sprites.get(this.index);
        }
    }

    private static class Stage {
        public ErosionObjSprites.Section[] sections = new ErosionObjSprites.Section[4];
    }
}
