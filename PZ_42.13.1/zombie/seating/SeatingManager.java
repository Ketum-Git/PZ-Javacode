// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seating;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class SeatingManager {
    private static SeatingManager instance;
    private static final String DEFAULT_FACING = "S";
    private final ArrayList<SeatingManager.ModData> modData = new ArrayList<>();
    private final SeatingData mergedTilesets = new SeatingData(null);

    public static SeatingManager getInstance() {
        if (instance == null) {
            instance = new SeatingManager();
        }

        return instance;
    }

    private SeatingManager() {
    }

    public void init() {
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.mediaFile.common.absoluteFile, "seating.txt");
                if (file.exists()) {
                    this.initModData(mod);
                }
            }
        }

        this.initGameData();
        this.initMergedTilesets();
    }

    public void initGameData() {
        SeatingManager.ModData data = new SeatingManager.ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.data.init();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        SeatingManager.ModData data = new SeatingManager.ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
        data.data.init();
        this.modData.add(data);
    }

    private void initMergedTilesets() {
        this.mergedTilesets.Reset();
        this.mergedTilesets.initMerged();

        for (int i = 0; i < this.modData.size(); i++) {
            SeatingManager.ModData data = this.modData.get(i);
            this.mergedTilesets.mergeTilesets(data.data);
        }
    }

    public void mergeAfterEditing() {
        this.initMergedTilesets();
    }

    public ArrayList<String> getModIDs() {
        ArrayList<String> modIDs = new ArrayList<>();

        for (int i = 0; i < this.modData.size(); i++) {
            SeatingManager.ModData data = this.modData.get(i);
            modIDs.add(data.modId);
        }

        return modIDs;
    }

    SeatingManager.ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); i++) {
            SeatingManager.ModData data = this.modData.get(i);
            if (StringUtils.equals(modID, data.modId)) {
                return data;
            }
        }

        return null;
    }

    SeatingManager.ModData getModDataRequired(String modID) {
        SeatingManager.ModData modData = this.getModData(modID);
        if (modData == null) {
            throw new RuntimeException("unknown modID \"%s\"".formatted(modID));
        } else {
            return modData;
        }
    }

    SeatingData getSeatingDataRequired(String modID) {
        return this.getModDataRequired(modID).data;
    }

    public int addTilePosition(String modID, String tilesetName, int col, int row, String id) {
        return this.getSeatingDataRequired(modID).addPosition(tilesetName, col, row, id);
    }

    public void removeTilePosition(String modID, String tilesetName, int col, int row, int index) {
        this.getSeatingDataRequired(modID).removePosition(tilesetName, col, row, index);
    }

    public int getTilePositionCount(String modID, String tilesetName, int col, int row) {
        return this.getSeatingDataRequired(modID).getPositionCount(tilesetName, col, row);
    }

    public int getTilePositionCount(String tilesetName, int col, int row) {
        return this.mergedTilesets.getPositionCount(tilesetName, col, row);
    }

    public int getTilePositionCount(IsoObject isoObject) {
        if (isoObject != null && isoObject.getSprite() != null && isoObject.getSprite().tilesetName != null) {
            IsoSprite sprite = isoObject.getSprite();
            int col = sprite.tileSheetIndex % 8;
            int row = sprite.tileSheetIndex / 8;
            return this.mergedTilesets.getPositionCount(sprite.tilesetName, col, row);
        } else {
            return 0;
        }
    }

    public String getTilePositionID(String modID, String tilesetName, int col, int row, int index) {
        return this.getSeatingDataRequired(modID).getPositionID(tilesetName, col, row, index);
    }

    public boolean hasTilePositionWithID(String modID, String tilesetName, int col, int row, String id) {
        return this.getSeatingDataRequired(modID).hasPositionWithID(tilesetName, col, row, id);
    }

    public Vector3f getTilePositionTranslate(String modID, String tilesetName, int col, int row, int index) {
        return this.getSeatingDataRequired(modID).getPositionTranslate(tilesetName, col, row, index);
    }

    public String getTilePositionProperty(String modID, String tilesetName, int col, int row, int index, String key) {
        return StringUtils.isNullOrWhitespace(key) ? null : this.getSeatingDataRequired(modID).getPositionProperty(tilesetName, col, row, index, key);
    }

    public void setTilePositionProperty(String modID, String tilesetName, int col, int row, int index, String key, String value) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            SeatingFile.Tile tile = this.getSeatingDataRequired(modID).getOrCreateTile(tilesetName, col, row);
            SeatingFile.Position position = tile.getPositionByIndex(index);
            if (position != null) {
                position.setProperty(key, value);
            }
        }
    }

    public String getTileProperty(String tilesetName, int col, int row, String key) {
        return this.mergedTilesets.getProperty(tilesetName, col, row, key);
    }

    public String getTileProperty(String modID, String tilesetName, int col, int row, String key) {
        return this.getSeatingDataRequired(modID).getProperty(tilesetName, col, row, key);
    }

    public void setTileProperty(String modID, String tilesetName, int col, int row, String key, String value) {
        this.getSeatingDataRequired(modID).setProperty(tilesetName, col, row, key, value);
    }

    public SeatingFile.Tile getTile(String modID, String tilesetName, int col, int row) {
        return this.getSeatingDataRequired(modID).getTile(tilesetName, col, row);
    }

    public SeatingFile.Tile getOrCreateTile(String modID, String tilesetName, int col, int row) {
        return this.getSeatingDataRequired(modID).getOrCreateTile(tilesetName, col, row);
    }

    public Vector3f getTranslation(String modID, IsoSprite sprite, String sitDirection, Vector3f xln) {
        xln.set(0.0F);
        return sprite != null && sprite.tilesetName != null ? this.getTranslation(modID, sprite.tilesetName, sprite.tileSheetIndex, sitDirection, xln) : xln;
    }

    private Vector3f getTranslation(SeatingData data, String tilesetName, int tileSheetIndex, String sitDirection, Vector3f xln) {
        xln.set(0.0F);
        int col = tileSheetIndex % 8;
        int row = tileSheetIndex / 8;
        SeatingFile.Position position = data.getPositionWithID(tilesetName, col, row, sitDirection);
        return position == null ? xln : xln.set(position.translate.x, position.translate.z, position.translate.y);
    }

    public Vector3f getTranslation(String modID, String tilesetName, int tileSheetIndex, String sitDirection, Vector3f xln) {
        return this.getTranslation(this.getSeatingDataRequired(modID), tilesetName, tileSheetIndex, sitDirection, xln);
    }

    public Vector3f getTranslation(String tilesetName, int tileSheetIndex, String sitDirection, Vector3f xln) {
        return this.getTranslation(this.mergedTilesets, tilesetName, tileSheetIndex, sitDirection, xln);
    }

    private Vector3f getTranslation(SeatingData data, IsoSprite sprite, String sitDirection, Vector3f xln) {
        xln.set(0.0F);
        return sprite != null && sprite.tilesetName != null ? this.getTranslation(data, sprite.tilesetName, sprite.tileSheetIndex, sitDirection, xln) : xln;
    }

    public Vector3f getTranslation(IsoSprite sprite, String sitDirection, Vector3f xln) {
        return this.getTranslation(this.mergedTilesets, sprite, sitDirection, xln);
    }

    public boolean getAdjacentPosition(
        IsoGameCharacter character, IsoObject isoObject, String sitDirection, String side, String animStateName, String animNodeName, Vector3f worldPos
    ) {
        worldPos.set(0.0F);
        if (isoObject != null && isoObject.getSprite() != null && isoObject.getSprite().tilesetName != null) {
            Model model = character.getAnimationPlayer().getModel();
            AnimationSet animationSet = character.getAdvancedAnimator().animSet;
            Vector2f localPos = new Vector2f();
            boolean valid = this.getAdjacentPosition(
                this.mergedTilesets, isoObject.getSprite(), sitDirection, side, model, animationSet.name, animStateName, animNodeName, localPos
            );
            if (!valid) {
                return false;
            } else {
                worldPos.set(isoObject.square.x + 0.5F + localPos.x, isoObject.square.y + 0.5F + localPos.y, (float)isoObject.square.z);
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean getAdjacentPosition(
        SeatingData data,
        IsoSprite sprite,
        String sitDirectionStr,
        String sideStr,
        Model model,
        String animSetName,
        String animStateName,
        String animNodeName,
        Vector2f worldPos
    ) {
        worldPos.set(0.0F);
        if (sprite != null && sprite.tilesetName != null) {
            int col = sprite.tileSheetIndex % 8;
            int row = sprite.tileSheetIndex / 8;
            SeatingFile.Position position = data.getPositionWithID(sprite.tilesetName, col, row, sitDirectionStr);
            if (position == null) {
                return false;
            } else if ("Left".equalsIgnoreCase(sideStr) && position.getProperty("BlockLeft") != null) {
                return false;
            } else if ("Right".equalsIgnoreCase(sideStr) && position.getProperty("BlockRight") != null) {
                return false;
            } else {
                Vector3f xln = this.getTranslation(data, sprite, sitDirectionStr, new Vector3f());
                float sxf = xln.x;
                float syf = xln.y;
                float szf = xln.z;
                AnimationSet animationSet = AnimationSet.GetAnimationSet(animSetName, false);
                AnimState animState = animationSet.GetState(animStateName);
                AnimNode animNode = PZArrayUtil.find(animState.nodes, animNode1 -> animNodeName.equalsIgnoreCase(animNode1.name));
                Vector2 dm = new Vector2();
                SkinningData skinningData = (SkinningData)model.tag;
                this.getTotalDeferredMovement(skinningData, animNode, dm);
                dm.scale(1.5F);
                float angleRadians = 0.0F;

                float SitOnFurnitureDirection = switch (sideStr) {
                    case "Front" -> 0.0F;
                    case "Left" -> 90.0F;
                    case "Right" -> -90.0F;
                    default -> 0.0F;
                };

                angleRadians = switch (sitDirectionStr) {
                    case "N" -> (180.0F + SitOnFurnitureDirection) * (float) (Math.PI / 180.0);
                    case "S" -> (0.0F + SitOnFurnitureDirection) * (float) (Math.PI / 180.0);
                    case "W" -> (90.0F + SitOnFurnitureDirection) * (float) (Math.PI / 180.0);
                    case "E" -> (270.0F + SitOnFurnitureDirection) * (float) (Math.PI / 180.0);
                    default -> angleRadians;
                };
                dm.rotate(angleRadians);
                worldPos.set(sxf - dm.x, syf - dm.y);
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean getAdjacentPosition(
        String modID,
        IsoSprite sprite,
        String sitDirectionStr,
        String sideStr,
        Model model,
        String animSetName,
        String animStateName,
        String animNodeName,
        Vector2f worldPos
    ) {
        return this.getAdjacentPosition(
            this.getSeatingDataRequired(modID), sprite, sitDirectionStr, sideStr, model, animSetName, animStateName, animNodeName, worldPos
        );
    }

    private String getFacingDirection(SeatingData data, String tilesetName, int col, int row) {
        int count = data.getPositionCount(tilesetName, col, row);
        String Facing = count == 0 ? null : data.getPositionID(tilesetName, col, row, 0);
        if (Facing == null) {
            String spriteName = String.format("%s_%d", tilesetName, col + row * 8);
            IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
            if (sprite == null) {
                return "S";
            }

            Facing = sprite.getProperties().get("Facing");
            if (Facing == null) {
                return "S";
            }
        }

        return Facing;
    }

    public String getFacingDirection(String modID, String tilesetName, int col, int row) {
        return this.getFacingDirection(this.getSeatingDataRequired(modID), tilesetName, col, row);
    }

    public String getFacingDirection(String tilesetName, int col, int row) {
        return this.getFacingDirection(this.mergedTilesets, tilesetName, col, row);
    }

    public String getFacingDirection(IsoSprite sprite) {
        return sprite != null && sprite.tilesetName != null
            ? this.getFacingDirection(sprite.tilesetName, sprite.tileSheetIndex % 8, sprite.tileSheetIndex / 8)
            : "S";
    }

    public String getFacingDirection(IsoObject object) {
        return object == null ? "S" : this.getFacingDirection(object.getSprite());
    }

    void getTotalDeferredMovement(SkinningData skinningData, AnimNode animNode, Vector2 result) {
        result.set(0.0F, 0.0F);
        String animName = animNode.animName;
        String boneName = animNode.getDeferredBoneName();
        BoneAxis boneAxis = animNode.getDeferredBoneAxis();
        int boneIndex = skinningData.boneIndices.getOrDefault(boneName, -1);
        AnimationClip animationClip = skinningData.animationClips.get(animName);
        Keyframe[] keyframes = animationClip.getBoneFramesAt(boneIndex);
        Keyframe keyframe1 = keyframes[0];
        Keyframe keyframe2 = keyframes[keyframes.length - 1];
        Vector2 v1 = this.getDeferredMovement(boneAxis, keyframe1.position, new Vector2());
        Vector2 v2 = this.getDeferredMovement(boneAxis, keyframe2.position, new Vector2());
        result.set(v2.x - v1.x, v2.y - v1.y);
    }

    public Vector2 getDeferredMovement(BoneAxis boneAxis, org.lwjgl.util.vector.Vector3f bonePos, Vector2 out_deferredPos) {
        if (boneAxis == BoneAxis.Y) {
            out_deferredPos.set(bonePos.x, -bonePos.z);
        } else {
            out_deferredPos.set(bonePos.x, bonePos.y);
        }

        return out_deferredPos;
    }

    public float getAnimationTrackFraction(IsoGameCharacter character, String animNodeName) {
        AdvancedAnimator aa = character.getAdvancedAnimator();
        AnimLayer rootLayer = aa.getRootLayer();
        List<LiveAnimNode> liveAnimNodes = rootLayer.getLiveAnimNodes();

        for (int i = 0; i < liveAnimNodes.size(); i++) {
            LiveAnimNode animNode = liveAnimNodes.get(i);
            if (animNode.getName().startsWith(animNodeName)) {
                for (int j = 0; j < animNode.getMainAnimationTracksCount(); j++) {
                    AnimationTrack track = animNode.getMainAnimationTrackAt(j);
                    if (track.isPlaying) {
                        return track.getCurrentTimeFraction();
                    }
                }
            }
        }

        return -1.0F;
    }

    public void write(String modID) {
        this.getSeatingDataRequired(modID).write();
    }

    public void fixDefaultPositions() {
        for (SeatingManager.ModData data : this.modData) {
            data.data.fixDefaultPositions();
        }
    }

    public void Reset() {
        for (SeatingManager.ModData data : this.modData) {
            data.data.Reset();
        }

        this.modData.clear();
        this.mergedTilesets.Reset();
    }

    private static final class ModData {
        private final String modId;
        private final String mediaAbsPath;
        private final SeatingData data;

        private ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.data = new SeatingData(mediaAbsPath);
        }
    }
}
