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

    public ErosionObjSprites(int _stages, String _name, boolean _hasSnow, boolean _hasFlower, boolean _hasChildsprite) {
        this.name = _name;
        this.stages = _stages;
        this.hasSnow = _hasSnow;
        this.hasFlower = _hasFlower;
        this.hasChildSprite = _hasChildsprite;
        this.sprites = new ErosionObjSprites.Stage[_stages];

        for (int i = 0; i < _stages; i++) {
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

    private String getSprite(int _stage, int _section, int _season) {
        return this.sprites[_stage] != null
                && this.sprites[_stage].sections[_section] != null
                && this.sprites[_stage].sections[_section].seasons[_season] != null
            ? this.sprites[_stage].sections[_section].seasons[_season].getNext()
            : null;
    }

    public String getBase(int _stage, int _season) {
        return this.getSprite(_stage, 0, _season);
    }

    public String getFlower(int _stage) {
        return this.hasFlower ? this.getSprite(_stage, 2, 0) : null;
    }

    public String getChildSprite(int _stage, int _season) {
        return this.hasChildSprite ? this.getSprite(_stage, 3, _season) : null;
    }

    public ErosionObjSprites.Entry getEntry(String _sprite) {
        return this.revLookup.getOrDefault(_sprite, null);
    }

    private void setSprite(int _stage, int _section, String _sprite, int _season) {
        if (this.sprites[_stage] != null && this.sprites[_stage].sections[_section] != null) {
            this.sprites[_stage].sections[_section].seasons[_season] = new ErosionObjSprites.Sprites(_sprite);
            this.revLookup.put(_sprite, new ErosionObjSprites.Entry(_stage, _section, _season));
        }
    }

    private void setSprite(int _stage, int _section, ArrayList<String> _sprites, int _season) {
        assert !_sprites.isEmpty();

        if (this.sprites[_stage] != null && this.sprites[_stage].sections[_section] != null) {
            this.sprites[_stage].sections[_section].seasons[_season] = new ErosionObjSprites.Sprites(_sprites);
            _sprites.forEach(s -> this.revLookup.put(s, new ErosionObjSprites.Entry(_stage, _section, _season)));
        }
    }

    public void setBase(int _stage, String _sprite, int _season) {
        this.setSprite(_stage, 0, _sprite, _season);
    }

    public void setBase(int _stage, ArrayList<String> _sprites, int _season) {
        this.setSprite(_stage, 0, _sprites, _season);
    }

    public void setFlower(int _stage, String _sprite) {
        this.setSprite(_stage, 2, _sprite, 0);
    }

    public void setFlower(int _stage, ArrayList<String> _sprites) {
        this.setSprite(_stage, 2, _sprites, 0);
    }

    public void setChildSprite(int _stage, String _sprite, int _season) {
        this.setSprite(_stage, 3, _sprite, _season);
    }

    public void setChildSprite(int _stage, ArrayList<String> _sprites, int _season) {
        this.setSprite(_stage, 3, _sprites, _season);
    }

    public record Entry(int stage, int section, int season) {
    }

    private static class Section {
        public ErosionObjSprites.Sprites[] seasons = new ErosionObjSprites.Sprites[6];
    }

    private static final class Sprites {
        public final ArrayList<String> sprites = new ArrayList<>();
        private int index = -1;

        public Sprites(String _sprite) {
            this.sprites.add(_sprite);
        }

        public Sprites(ArrayList<String> _sprites) {
            this.sprites.addAll(_sprites);
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
