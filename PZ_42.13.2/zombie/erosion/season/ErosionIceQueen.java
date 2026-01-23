// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.season;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class ErosionIceQueen {
    public static ErosionIceQueen instance;
    private final ArrayList<ErosionIceQueen.Sprite> sprites = new ArrayList<>();
    private final IsoSpriteManager sprMngr;
    private boolean snowState;

    public void addSprite(String _sprite, String _winterSprite) {
        IsoSprite spr = this.sprMngr.getSprite(_sprite);
        IsoSprite winter = this.sprMngr.getSprite(_winterSprite);
        if (spr != null && winter != null) {
            spr.setName(_sprite);
            this.sprites.add(new ErosionIceQueen.Sprite(spr, _sprite, _winterSprite));
        }
    }

    public void setSnow(boolean _isSnow) {
        if (this.snowState != _isSnow) {
            this.snowState = _isSnow;

            for (int i = 0; i < this.sprites.size(); i++) {
                ErosionIceQueen.Sprite sprite = this.sprites.get(i);
                sprite.sprite.ReplaceCurrentAnimFrames(this.snowState ? sprite.winter : sprite.normal);
            }
        }
    }

    public ErosionIceQueen(IsoSpriteManager _sprMngr) {
        instance = this;
        this.sprMngr = _sprMngr;
        this.readTileProperties();
    }

    private void readTileProperties() {
        for (IsoSprite sprite : new ArrayList<>(IsoSpriteManager.instance.namedMap.values())) {
            if (sprite.properties.has("SnowTile")) {
                String snowTile = sprite.properties.get("SnowTile");
                if (!StringUtils.isNullOrWhitespace(snowTile)) {
                    snowTile = this.fixTileName(snowTile);
                    this.addSprite(sprite.name, snowTile);
                }
            }
        }
    }

    private String fixTileName(String tileName) {
        int p = tileName.lastIndexOf(95);
        if (p == -1) {
            return tileName;
        } else {
            String tilesetName = tileName.substring(0, p);
            int index = PZMath.tryParseInt(tileName.substring(p + 1).trim(), -1);
            return index == -1 ? tileName : tilesetName + "_" + index;
        }
    }

    private void setRoofSnowA() {
        for (int i = 0; i < 128; i++) {
            String winter = "e_roof_snow_1_" + i;

            for (int id = 1; id <= 5; id++) {
                String sprite = "roofs_0" + id + "_" + i;
                this.addSprite(sprite, winter);
            }
        }
    }

    private void setRoofSnow() {
        for (int id = 1; id <= 5; id++) {
            for (int i = 0; i < 128; i++) {
                int sid = i;
                switch (id) {
                    case 1:
                        if (i >= 72 && i <= 79) {
                            sid = i - 8;
                        }

                        if (i == 112 || i == 114) {
                            sid = 0;
                        }

                        if (i == 113 || i == 115) {
                            sid = 1;
                        }

                        if (i == 116 || i == 118) {
                            sid = 4;
                        }

                        if (i == 117 || i == 119) {
                            sid = 5;
                        }
                        break;
                    case 2:
                        if (i == 50) {
                            sid = 106;
                        }

                        if (i == 51) {
                            sid = 107;
                        }

                        if (i >= 72 && i <= 79) {
                            sid = i - 8;
                        }

                        if (i == 104 || i == 106) {
                            sid = 0;
                        }

                        if (i == 105 || i == 107) {
                            sid = 1;
                        }

                        if (i == 108 || i == 110) {
                            sid = 4;
                        }

                        if (i == 109 || i == 111) {
                            sid = 5;
                        }
                        break;
                    case 3:
                        if (i == 72 || i == 74) {
                            sid = 0;
                        }

                        if (i == 73 || i == 75) {
                            sid = 1;
                        }

                        if (i == 76 || i == 78) {
                            sid = 4;
                        }

                        if (i == 77 || i == 79) {
                            sid = 5;
                        }

                        if (i == 102) {
                            sid = 70;
                        }

                        if (i == 103) {
                            sid = 71;
                        }

                        if (i == 104 || i == 106) {
                            sid = 0;
                        }

                        if (i == 105 || i == 107) {
                            sid = 1;
                        }

                        if (i == 108 || i == 110) {
                            sid = 4;
                        }

                        if (i == 109 || i == 111) {
                            sid = 5;
                        }

                        if (i >= 120 && i <= 127) {
                            sid = i - 16;
                        }
                        break;
                    case 4:
                        if (i == 48) {
                            sid = 106;
                        }

                        if (i == 49) {
                            sid = 107;
                        }

                        if (i == 50) {
                            sid = 108;
                        }

                        if (i == 51) {
                            sid = 109;
                        }

                        if (i == 72 || i == 74) {
                            sid = 0;
                        }

                        if (i == 73 || i == 75) {
                            sid = 1;
                        }

                        if (i == 76 || i == 78) {
                            sid = 4;
                        }

                        if (i == 77 || i == 79) {
                            sid = 5;
                        }

                        if (i == 102) {
                            sid = 70;
                        }

                        if (i == 103) {
                            sid = 71;
                        }

                        if (i == 104 || i == 106) {
                            sid = 0;
                        }

                        if (i == 105 || i == 107) {
                            sid = 1;
                        }

                        if (i == 108 || i == 110) {
                            sid = 4;
                        }

                        if (i == 109 || i == 111) {
                            sid = 5;
                        }
                        break;
                    case 5:
                        if (i >= 72 && i <= 79) {
                            sid = i - 8;
                        }

                        if (i == 104 || i == 106) {
                            sid = 0;
                        }

                        if (i == 105 || i == 107) {
                            sid = 1;
                        }

                        if (i == 108 || i == 110) {
                            sid = 4;
                        }

                        if (i == 109 || i == 111) {
                            sid = 5;
                        }

                        if (i >= 112 && i <= 119) {
                            sid = i - 32;
                        }
                }

                String sprite = "roofs_0" + id + "_" + i;
                String winter = "e_roof_snow_1_" + sid;
                this.addSprite(sprite, winter);
            }
        }

        int id = 5;

        for (int i = 128; i < 176; i++) {
            int var8;
            if (i == 136 || i == 138) {
                var8 = 0;
            } else if (i == 137 || i == 139) {
                var8 = 1;
            } else if (i == 140 || i == 142) {
                var8 = 4;
            } else if (i != 141 && i != 143) {
                if (i < 128 || i > 135) {
                    continue;
                }

                var8 = i - 128 + 96;
            } else {
                var8 = 5;
            }

            String sprite = "roofs_05_" + i;
            String winter = "e_roof_snow_1_" + var8;
            this.addSprite(sprite, winter);
        }
    }

    private void setRoofSnowOneX() {
        for (int id = 1; id <= 5; id++) {
            for (int i = 0; i < 128; i++) {
                int sid = i;
                switch (id) {
                    case 1:
                        if (i >= 96 && i <= 98) {
                            sid = i - 16;
                        }

                        if (i == 99) {
                            sid = i - 19;
                        }

                        if (i == 100) {
                            sid = i - 13;
                        }

                        if (i >= 101 && i <= 103) {
                            sid = i - 16;
                        }

                        if (i >= 112 && i <= 113) {
                            sid = i - 112;
                        }

                        if (i >= 114 && i <= 115) {
                            sid = i - 114;
                        }

                        if (i == 116 || i == 118) {
                            sid = 5;
                        }

                        if (i == 117 || i == 119) {
                            sid = 4;
                        }
                        break;
                    case 2:
                        if (i >= 96 && i <= 98) {
                            sid = i - 16;
                        }

                        if (i == 99) {
                            sid = i - 19;
                        }

                        if (i == 100) {
                            sid = i - 13;
                        }

                        if (i >= 101 && i <= 103) {
                            sid = i - 16;
                        }

                        if (i >= 104 && i <= 105) {
                            sid = i - 104;
                        }

                        if (i >= 106 && i <= 107) {
                            sid = i - 106;
                        }

                        if (i >= 108 && i <= 109) {
                            sid = i - 104;
                        }

                        if (i >= 110 && i <= 111) {
                            sid = i - 106;
                        }
                        break;
                    case 3:
                        if (i >= 18 && i <= 19) {
                            sid = i - 12;
                        }

                        if (i >= 50 && i <= 51) {
                            sid = i - 44;
                        }

                        if (i >= 72 && i <= 73) {
                            sid = i - 72;
                        }

                        if (i >= 74 && i <= 75) {
                            sid = i - 74;
                        }

                        if (i >= 76 && i <= 77) {
                            sid = i - 72;
                        }

                        if (i >= 78 && i <= 79) {
                            sid = i - 74;
                        }

                        if (i >= 102 && i <= 103) {
                            sid = i - 88;
                        }

                        if (i >= 122 && i <= 125) {
                            sid = i - 16;
                        }
                        break;
                    case 4:
                        if (i >= 18 && i <= 19) {
                            sid = i - 12;
                        }
                        break;
                    case 5:
                        if (i >= 72 && i <= 74) {
                            sid = i + 8;
                        }

                        if (i == 75) {
                            sid = i + 7;
                        }

                        if (i == 76) {
                            sid = i + 11;
                        }

                        if (i >= 77 && i <= 79) {
                            sid = i + 8;
                        }

                        if (i >= 112 && i <= 113) {
                            sid = i - 112;
                        }

                        if (i >= 114 && i <= 115) {
                            sid = i - 114;
                        }

                        if (i == 116 || i == 118) {
                            sid = 5;
                        }

                        if (i == 117 || i == 119) {
                            sid = 4;
                        }
                }

                String sprite = "roofs_0" + id + "_" + i;
                String winter = "e_roof_snow_1_" + sid;
                this.addSprite(sprite, winter);
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.sprites.clear();
            instance = null;
        }
    }

    private static class Sprite {
        public IsoSprite sprite;
        public String normal;
        public String winter;

        public Sprite(IsoSprite _sprite, String _normal, String _winter) {
            this.sprite = _sprite;
            this.normal = _normal;
            this.winter = _winter;
        }
    }
}
