// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.util.LocationRNG;
import zombie.util.StringUtils;

@UsedFromLua
public class TileOverlays {
    public static final TileOverlays instance = new TileOverlays();
    private final THashMap<String, TileOverlays.TileOverlay> overlayMap = new THashMap<>();
    private final HashMap<String, ArrayList<String>> overlayNameToUnderlyingName = new HashMap<>();
    private final ArrayList<TileOverlays.TileOverlayEntry> tempEntries = new ArrayList<>();

    public void addOverlays(KahluaTableImpl overlayMap) {
        KahluaTableIterator overlayIter = overlayMap.iterator();

        while (overlayIter.advance()) {
            String key = overlayIter.getKey().toString();
            if (!"VERSION".equalsIgnoreCase(key)) {
                TileOverlays.TileOverlay o = new TileOverlays.TileOverlay();
                o.tile = key;
                KahluaTableImpl overlayTable = (KahluaTableImpl)overlayIter.getValue();
                KahluaTableIterator roomIter = overlayTable.iterator();

                while (roomIter.advance()) {
                    KahluaTableImpl roomTable = (KahluaTableImpl)roomIter.getValue();
                    TileOverlays.TileOverlayEntry e = new TileOverlays.TileOverlayEntry();
                    e.room = roomTable.rawgetStr("name");
                    e.chance = roomTable.rawgetInt("chance");
                    e.usage.parse(roomTable.rawgetStr("usage"));
                    KahluaTableImpl tilesTable = (KahluaTableImpl)roomTable.rawget("tiles");
                    KahluaTableIterator tileIter = tilesTable.iterator();

                    while (tileIter.advance()) {
                        String textureName = tileIter.getValue().toString();
                        if (StringUtils.isNullOrWhitespace(textureName) || "none".equalsIgnoreCase(textureName)) {
                            textureName = "";
                        } else if (Core.debug && !GameServer.server && Texture.getSharedTexture(textureName) == null) {
                            System.out.println("BLANK OVERLAY TEXTURE. Set it to \"none\".: " + textureName);
                        }

                        e.tiles.add(textureName);
                    }

                    o.entries.add(e);
                }

                this.overlayMap.put(o.tile, o);

                for (TileOverlays.TileOverlayEntry e : o.entries) {
                    for (String overlayName : e.tiles) {
                        ArrayList<String> underlyingNames = this.overlayNameToUnderlyingName.get(overlayName);
                        if (underlyingNames == null) {
                            underlyingNames = new ArrayList<>();
                            this.overlayNameToUnderlyingName.put(overlayName, underlyingNames);
                        }

                        if (!underlyingNames.contains(o.tile)) {
                            underlyingNames.add(o.tile);
                        }
                    }
                }
            }
        }
    }

    public boolean hasOverlays(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null && this.overlayMap.containsKey(obj.sprite.name);
    }

    public ArrayList<String> getUnderlyingSpriteNames(String overlayName) {
        return this.overlayNameToUnderlyingName.get(overlayName);
    }

    public void updateTileOverlaySprite(IsoObject obj) {
        if (obj != null) {
            IsoGridSquare sq = obj.getSquare();
            if (sq != null) {
                String overlaySpriteName = null;
                float r = -1.0F;
                float g = -1.0F;
                float b = -1.0F;
                float a = -1.0F;
                if (obj.sprite != null && obj.sprite.name != null) {
                    TileOverlays.TileOverlay overlay = this.overlayMap.get(obj.sprite.name);
                    if (overlay != null) {
                        String room = "other";
                        if (sq.getRoom() != null) {
                            room = sq.getRoom().getName();
                        }

                        TileOverlays.TileOverlayEntry entry = overlay.pickRandom(room, sq);
                        if (entry == null) {
                            entry = overlay.pickRandom("other", sq);
                        }

                        if (entry != null) {
                            if (entry.usage.tableTop && this.hasObjectOnTop(obj)) {
                                return;
                            }

                            overlaySpriteName = entry.pickRandom(Math.abs(sq.x), Math.abs(sq.y), Math.abs(sq.z));
                            if (entry.usage.alpha >= 0.0F) {
                                b = 1.0F;
                                g = 1.0F;
                                r = 1.0F;
                                a = entry.usage.alpha;
                            }
                        }
                    }
                }

                if (!StringUtils.isNullOrWhitespace(overlaySpriteName) && !GameServer.server && Texture.getSharedTexture(overlaySpriteName) == null) {
                    overlaySpriteName = null;
                }

                if (!StringUtils.isNullOrWhitespace(overlaySpriteName)) {
                    if (obj.attachedAnimSprite == null) {
                        obj.attachedAnimSprite = new ArrayList<>(4);
                    }

                    IsoSprite overlaySprite = IsoSpriteManager.instance.getSprite(overlaySpriteName);
                    overlaySprite.name = overlaySpriteName;
                    IsoSpriteInstance inst = IsoSpriteInstance.get(overlaySprite);
                    if (a > 0.0F) {
                        inst.tintr = r;
                        inst.tintg = g;
                        inst.tintb = b;
                        inst.alpha = a;
                    }

                    inst.copyTargetAlpha = false;
                    inst.multiplyObjectAlpha = true;
                    obj.attachedAnimSprite.add(inst);
                }
            }
        }
    }

    private boolean hasObjectOnTop(IsoObject obj) {
        if (!obj.isTableSurface()) {
            return false;
        } else {
            IsoGridSquare sq = obj.getSquare();

            for (int i = obj.getObjectIndex() + 1; i < sq.getObjects().size(); i++) {
                IsoObject obj2 = sq.getObjects().get(i);
                if (obj2.isTableTopObject() || obj2.isTableSurface()) {
                    return true;
                }
            }

            return false;
        }
    }

    public void fixTableTopOverlays(IsoGridSquare square) {
        if (square != null && !square.getObjects().isEmpty()) {
            boolean hasTableObject = false;

            for (int i = square.getObjects().size() - 1; i >= 0; i--) {
                IsoObject obj = square.getObjects().get(i);
                if (hasTableObject && obj.isTableSurface()) {
                    this.removeTableTopOverlays(obj);
                }

                if (obj.isTableSurface() || obj.isTableTopObject()) {
                    hasTableObject = true;
                }
            }
        }
    }

    private void removeTableTopOverlays(IsoObject obj) {
        if (obj != null && obj.isTableSurface()) {
            if (obj.sprite != null && obj.sprite.name != null) {
                if (obj.attachedAnimSprite != null && !obj.attachedAnimSprite.isEmpty()) {
                    TileOverlays.TileOverlay overlay = this.overlayMap.get(obj.sprite.name);
                    if (overlay != null) {
                        int numOverlays = obj.attachedAnimSprite.size();

                        for (int i = 0; i < overlay.entries.size(); i++) {
                            TileOverlays.TileOverlayEntry entry = overlay.entries.get(i);
                            if (entry.usage.tableTop) {
                                for (int j = 0; j < entry.tiles.size(); j++) {
                                    this.tryRemoveAttachedSprite(obj.attachedAnimSprite, entry.tiles.get(j));
                                }
                            }
                        }

                        if (numOverlays != obj.attachedAnimSprite.size()) {
                        }
                    }
                }
            }
        }
    }

    private void tryRemoveAttachedSprite(ArrayList<IsoSpriteInstance> sprites, String spriteName) {
        for (int i = 0; i < sprites.size(); i++) {
            IsoSpriteInstance sprite = sprites.get(i);
            if (spriteName.equals(sprite.getName())) {
                sprites.remove(i--);
                IsoSpriteInstance.add(sprite);
            }
        }
    }

    public void Reset() {
        this.overlayMap.clear();
        this.overlayNameToUnderlyingName.clear();
    }

    private static final class TileOverlay {
        public String tile;
        public final ArrayList<TileOverlays.TileOverlayEntry> entries = new ArrayList<>();

        public void getEntries(String room, IsoGridSquare square, ArrayList<TileOverlays.TileOverlayEntry> out) {
            out.clear();

            for (int i = 0; i < this.entries.size(); i++) {
                TileOverlays.TileOverlayEntry entry = this.entries.get(i);
                if (entry.room.equalsIgnoreCase(room) && entry.matchUsage(square)) {
                    out.add(entry);
                }
            }
        }

        public TileOverlays.TileOverlayEntry pickRandom(String room, IsoGridSquare square) {
            ArrayList<TileOverlays.TileOverlayEntry> tempEntries = TileOverlays.instance.tempEntries;
            this.getEntries(room, square, tempEntries);
            if (tempEntries.isEmpty()) {
                return null;
            } else {
                int n = LocationRNG.instance.nextInt(tempEntries.size(), square.x, square.y, square.z);
                return tempEntries.get(n);
            }
        }
    }

    private static final class TileOverlayEntry {
        public String room;
        public int chance;
        public final ArrayList<String> tiles = new ArrayList<>();
        public final TileOverlays.TileOverlayUsage usage = new TileOverlays.TileOverlayUsage();

        public boolean matchUsage(IsoGridSquare square) {
            return this.usage.match(square);
        }

        public String pickRandom(int x, int y, int z) {
            int n = LocationRNG.instance.nextInt(this.chance, x, y, z);
            if (n == 0 && !this.tiles.isEmpty()) {
                n = LocationRNG.instance.nextInt(this.tiles.size());
                return this.tiles.get(n);
            } else {
                return null;
            }
        }
    }

    private static final class TileOverlayUsage {
        String usage;
        int zOnly = Integer.MIN_VALUE;
        int zGreaterThan = Integer.MIN_VALUE;
        float alpha = -1.0F;
        boolean tableTop;

        boolean parse(String usage) {
            this.usage = usage.trim();
            if (StringUtils.isNullOrWhitespace(this.usage)) {
                return true;
            } else {
                String[] ss = usage.split(";");

                for (int i = 0; i < ss.length; i++) {
                    String s = ss[i];
                    if (s.startsWith("z=")) {
                        this.zOnly = Integer.parseInt(s.substring(2));
                    } else if (s.startsWith("z>")) {
                        this.zGreaterThan = Integer.parseInt(s.substring(2));
                    } else if (s.startsWith("alpha=")) {
                        this.alpha = Float.parseFloat(s.substring(6));
                        this.alpha = PZMath.clamp(this.alpha, 0.0F, 1.0F);
                    } else {
                        if (!s.startsWith("tabletop")) {
                            return false;
                        }

                        this.tableTop = true;
                    }
                }

                return true;
            }
        }

        boolean match(IsoGridSquare square) {
            return this.zOnly != Integer.MIN_VALUE && square.z != this.zOnly ? false : this.zGreaterThan == Integer.MIN_VALUE || square.z > this.zGreaterThan;
        }
    }
}
