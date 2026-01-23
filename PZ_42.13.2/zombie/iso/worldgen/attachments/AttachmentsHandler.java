// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.attachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WorldGenChunk;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.WorldGenUtils;
import zombie.iso.worldgen.utils.Direction;
import zombie.iso.worldgen.utils.SquareCoord;

public class AttachmentsHandler {
    private final Map<String, Map<Direction, List<String>>> attachments = new HashMap<>();

    public void loadAttachments() {
        IsoSpriteManager.instance.namedMap.forEach((k, v) -> {
            PropertyContainer prop = v.getProperties();
            boolean isAttached = prop.has(IsoFlagType.IsFloorAttached);
            String material = prop.get("FloorMaterial");
            if (isAttached) {
                if (material != null && !Objects.equals(material, "null") && !Objects.equals(material, "None")) {
                    if (!this.attachments.containsKey(material)) {
                        this.attachments.put(material, new HashMap<>());

                        for (Direction d : Direction.values()) {
                            this.attachments.get(material).put(d, new ArrayList<>());
                        }
                    }

                    boolean isNorth = prop.has(IsoFlagType.FloorAttachmentN);
                    boolean isSouth = prop.has(IsoFlagType.FloorAttachmentS);
                    boolean isWest = prop.has(IsoFlagType.FloorAttachmentW);
                    boolean isEast = prop.has(IsoFlagType.FloorAttachmentE);
                    int location = (isNorth ? 1 : 0) << 0 | (isSouth ? 1 : 0) << 1 | (isEast ? 1 : 0) << 2 | (isWest ? 1 : 0) << 3;

                    Direction dir = switch (location) {
                        case 1 -> Direction.NORTH;
                        case 2 -> Direction.SOUTH;
                        default -> throw new IllegalArgumentException();
                        case 4 -> Direction.EAST;
                        case 5 -> Direction.NORTH_EAST;
                        case 6 -> Direction.SOUTH_EAST;
                        case 8 -> Direction.WEST;
                        case 9 -> Direction.NORTH_WEST;
                        case 10 -> Direction.SOUTH_WEST;
                    };
                    this.attachments.get(material).get(dir).add(k);
                }
            }
        });
        DebugLog.WorldGen.debugln(this.attachments);
    }

    public void resetAttachments(IsoChunk chunk) {
        chunk.setAttachmentsDoneFull(false);
        chunk.setAttachmentsState(0, false);
        chunk.setAttachmentsState(1, false);
        chunk.setAttachmentsState(2, false);
        chunk.setAttachmentsState(3, false);
        chunk.setAttachmentsState(4, false);
    }

    public void applyAttachments(IsoChunk chunk) {
        for (int wx = -1; wx <= 1; wx++) {
            for (int wy = -1; wy <= 1; wy++) {
                if (IsoWorld.instance.getCell().getChunk(chunk.wx + wx, chunk.wy + wy) == null) {
                    return;
                }
            }
        }

        int minTileX = chunk.wx * 8;
        int minTileY = chunk.wy * 8;
        int maxTileX = (chunk.wx + 1) * 8;
        int maxTileY = (chunk.wy + 1) * 8;
        WorldGenChunk wgChunk = IsoWorld.instance.getWgChunk();
        IsoCell cell = IsoWorld.instance.currentCell;
        if (chunk.attachmentsPartialSize() == null) {
            for (int x = minTileX; x < maxTileX; x++) {
                for (int y = minTileY; y < maxTileY; y++) {
                    this.attach(cell, x, y, wgChunk, true);
                }
            }
        } else {
            for (int i = 0; i < chunk.attachmentsPartialSize(); i++) {
                SquareCoord coord = chunk.getAttachmentsPartial(i);
                this.attach(cell, coord.x(), coord.y(), wgChunk, false);
            }
        }

        for (int i = 0; i <= 4; i++) {
            chunk.setAttachmentsState(i, true);
        }

        chunk.setAttachmentsDoneFull(true);

        for (int i = 0; i <= 4; i++) {
            if (!chunk.isAttachmentsDone(i)) {
                chunk.setAttachmentsDoneFull(false);
            }
        }
    }

    private void attach(IsoCell cell, int x, int y, WorldGenChunk wgChunk, boolean doPriority) {
        int z = 0;
        Random rnd = WorldGenParams.INSTANCE.getRandom(x, y);
        IsoObject floor = WorldGenUtils.INSTANCE.doesFloorExit(cell, x, y, 0);
        if (floor != null) {
            String floorMaterial = floor.getSprite().getProperties().get("FloorMaterial");
            Map<Direction, String> materials = new HashMap<>();

            for (Direction dir : Direction.rose()) {
                materials.put(dir, null);
                IsoObject floorRemote = WorldGenUtils.INSTANCE.doesFloorExit(cell, x + dir.x, y + dir.y, 0);
                if (floorRemote != null) {
                    if (Direction.cardinals().contains(dir)) {
                        this.deleteCoreAttachments(dir, doPriority, floor, floorRemote);
                    }

                    materials.put(dir, floorRemote.getSprite().getProperties().get("FloorMaterial"));
                }
            }

            List<String> materialsFound = materials.values().stream().distinct().toList();
            List<String> materialsFoundNotNull = materials.values().stream().distinct().filter(Objects::nonNull).toList();
            if (!materialsFoundNotNull.isEmpty() && (materialsFoundNotNull.size() != 1 || !materialsFoundNotNull.get(0).equals(floorMaterial))) {
                List<Direction> validDirs = new ArrayList<>(Direction.rose());
                if (materialsFound.size() > 1) {
                    for (Direction dirx : Direction.diagonals()) {
                        List<String> mat = Stream.of(materials.get(dirx), materials.get(dirx.prev()), materials.get(dirx.next())).distinct().toList();
                        if ((mat.size() != 1 || mat.get(0) != null) && mat.size() == 1 && !mat.get(0).equals(floorMaterial)) {
                            validDirs.remove(dirx);
                            validDirs.remove(dirx.prev());
                            validDirs.remove(dirx.next());
                            this.applyAttachment(dirx, wgChunk, doPriority, rnd, floor, floorMaterial, materials.get(dirx));
                        }
                    }
                }

                for (Direction dirxx : Direction.cardinals()) {
                    if (materials.containsKey(dirxx) && materials.get(dirxx) != null && validDirs.contains(dirxx)) {
                        IsoObject floorRemote = WorldGenUtils.INSTANCE.doesFloorExit(cell, x + dirxx.x, y + dirxx.y, 0);
                        if (floorRemote != null) {
                            validDirs.remove(dirxx);
                            this.applyAttachment(dirxx.opposite(), wgChunk, doPriority, rnd, floorRemote, materials.get(dirxx), floorMaterial);
                        }
                    }
                }
            }
        }
    }

    private void deleteCoreAttachments(Direction dir, boolean doPriority, IsoObject floor, IsoObject floorRemote) {
        if (!doPriority) {
            int posFloor = this.containsAttachedSpriteInDir(floor, dir);
            int posFloorRemote = this.containsAttachedSpriteInDir(floorRemote, dir.opposite());
            if (posFloor >= 0) {
                floor.attachedAnimSprite.remove(posFloor);
            }

            if (posFloorRemote >= 0) {
                floorRemote.attachedAnimSprite.remove(posFloorRemote);
            }
        }
    }

    private void applyAttachment(Direction dir, WorldGenChunk wgChunk, boolean doPriority, Random rnd, IsoObject floor, String material, String materialRemote) {
        if (!Objects.equals(material, materialRemote) && (!doPriority || wgChunk.priority(materialRemote, material))) {
            Map<Direction, List<String>> materials = this.attachments.get(materialRemote);
            if (materials == null) {
                return;
            }

            List<String> tiles = materials.get(dir);
            if (tiles == null || tiles.isEmpty()) {
                return;
            }

            String tile = tiles.get(rnd.nextInt(tiles.size()));
            floor.addAttachedAnimSprite(IsoSpriteManager.instance.namedMap.get(tile));
        }
    }

    private int containsAttachedSpriteInDir(IsoObject floor, Direction dir) {
        List<IsoSpriteInstance> attachedAnimSprites = floor.getAttachedAnimSprite();
        if (attachedAnimSprites != null) {
            for (IsoSpriteInstance animSprite : attachedAnimSprites) {
                PropertyContainer prop = animSprite.getParentSprite().getProperties();
                if (dir == Direction.NORTH && prop.has(IsoFlagType.FloorAttachmentN)
                    || dir == Direction.SOUTH && prop.has(IsoFlagType.FloorAttachmentS)
                    || dir == Direction.WEST && prop.has(IsoFlagType.FloorAttachmentW)
                    || dir == Direction.EAST && prop.has(IsoFlagType.FloorAttachmentE)) {
                    return attachedAnimSprites.indexOf(animSprite);
                }
            }
        }

        return -1;
    }
}
