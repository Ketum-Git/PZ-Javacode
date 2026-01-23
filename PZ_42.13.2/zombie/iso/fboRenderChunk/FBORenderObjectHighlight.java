// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.savefile.SavefileThumbnail;

public final class FBORenderObjectHighlight {
    private static FBORenderObjectHighlight instance;
    private boolean rendering;
    private boolean renderingGhostTile;
    private final HashSet<IsoObject> objectSet = new HashSet<>();
    private final HashSet<IsoObject> objectSet2 = new HashSet<>();
    private final HashMap<IsoObject, Byte> objectHighlightRendered = new HashMap<>();
    private final ArrayList<IsoObject> objectList = new ArrayList<>();

    public static FBORenderObjectHighlight getInstance() {
        if (instance == null) {
            instance = new FBORenderObjectHighlight();
        }

        return instance;
    }

    public boolean isRendering() {
        return this.rendering;
    }

    public void setRenderingGhostTile(boolean bRendering) {
        this.renderingGhostTile = bRendering;
    }

    public boolean isRenderingGhostTile() {
        return this.renderingGhostTile;
    }

    public void registerObject(IsoObject object) {
        if (!(object instanceof IsoDeadBody)) {
            this.objectHighlightRendered.put(object, (byte)15);
            if (!this.objectSet.contains(object)) {
                this.objectSet.add(object);
                if (this.isRenderedToChunkTexture(object)) {
                    object.invalidateRenderChunkLevel(256L);
                }
            }
        }
    }

    public void unregisterObject(IsoObject object) {
        boolean bRemoved = this.objectSet.remove(object);
        if (bRemoved) {
            this.objectHighlightRendered.remove(object);
            if (this.isRenderedToChunkTexture(object)) {
                object.invalidateRenderChunkLevel(256L);
            }
        }
    }

    private boolean isRenderedToChunkTexture(IsoObject object) {
        if (object instanceof IsoDeadBody) {
            return DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue();
        } else {
            return object instanceof IsoMannequin mannequin ? !mannequin.shouldRenderEachFrame() : false;
        }
    }

    private boolean isRenderedEveryFrame(IsoObject object) {
        if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            if (object instanceof IsoTree) {
                return true;
            }

            if (object.getWindRenderEffects() != null) {
                return true;
            }
        } else {
            if (FBORenderCell.instance.isTranslucentTree(object)) {
                return true;
            }

            if (object instanceof IsoTree && object.getObjectRenderEffects() != null) {
                return true;
            }
        }

        return false;
    }

    public void render(int playerIndex) {
        if (!SavefileThumbnail.isCreatingThumbnail()) {
            this.objectList.clear();
            if (!this.objectSet.isEmpty()) {
                this.objectList.addAll(this.objectSet);
                this.objectSet2.clear();
            }

            FBORenderCell.instance.renderTranslucentOnly = true;
            this.rendering = true;

            for (int i = 0; i < this.objectList.size(); i++) {
                IsoObject object = this.objectList.get(i);
                if (!this.isRenderedToChunkTexture(object)) {
                    if (object.getObjectIndex() == -1) {
                        this.objectSet.remove(object);
                        this.objectHighlightRendered.remove(object);
                    } else if (!object.isHighlighted()) {
                        this.objectSet.remove(object);
                        this.objectHighlightRendered.remove(object);
                    } else if (object.isHighlighted(playerIndex)) {
                        byte playerFlags = this.objectHighlightRendered.getOrDefault(object, (byte)0);
                        if (!object.isHighlightRenderOnce(playerIndex) || (playerFlags & 1 << playerIndex) != 0) {
                            this.renderObject(playerIndex, object);
                            int allPlayers = 0;

                            for (int j = 0; j < IsoPlayer.numPlayers; j++) {
                                allPlayers |= 1 << j;
                            }

                            playerFlags = (byte)(playerFlags & allPlayers);
                            playerFlags = (byte)(playerFlags & ~((byte)(1 << playerIndex)));
                            this.objectHighlightRendered.put(object, playerFlags);
                        }
                    }
                }
            }

            this.renderSafehouses(playerIndex);
            FBORenderCell.instance.renderTranslucentOnly = false;
            this.rendering = false;
        }
    }

    public void clearHighlightOnceFlag() {
        this.objectList.clear();
        if (!this.objectSet.isEmpty()) {
            this.objectList.addAll(this.objectSet);
        }

        for (int i = 0; i < this.objectList.size(); i++) {
            IsoObject object = this.objectList.get(i);
            if (object.isHighlightRenderOnce()) {
                byte playerFlags = this.objectHighlightRendered.getOrDefault(object, (byte)0);
                if (playerFlags == 0) {
                    object.setHighlighted(false, true);
                    this.objectSet.remove(object);
                    this.objectHighlightRendered.remove(object);
                }
            }
        }
    }

    private void renderObject(int playerIndex, IsoObject object) {
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        if (renderInfo.layer != ObjectRenderLayer.None && !(renderInfo.targetAlpha <= 0.0F)) {
            IsoObject[] objects = object.square.getObjects().getElements();
            int objectCount = object.square.getObjects().size();

            for (int i = object.getObjectIndex(); i < objectCount; i++) {
                IsoObject object1 = objects[i];
                if (!this.isRenderedToChunkTexture(object1)
                    && !this.isRenderedEveryFrame(object1)
                    && (object == object1 || !this.objectSet.contains(object1))
                    && !this.objectSet2.contains(object1)
                    && (!(object1 instanceof IsoWorldInventoryObject) || !DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue())) {
                    renderInfo = object1.getRenderInfo(playerIndex);
                    if (renderInfo.layer != ObjectRenderLayer.None && !(renderInfo.targetAlpha <= 0.0F)) {
                        this.objectSet2.add(object1);
                        if (renderInfo.layer == ObjectRenderLayer.Floor) {
                            FBORenderCell.instance.renderFloor(object1);
                        } else {
                            FBORenderCell.instance.renderTranslucent(object1);
                        }
                    }
                }
            }
        }
    }

    private void renderSafehouses(int playerIndex) {
    }

    public boolean shouldRenderObjectHighlight(IsoObject object) {
        if (object == null) {
            return false;
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            if (object.isHighlighted(playerIndex)) {
                return !PerformanceSettings.fboRenderChunk ? true : this.isRendering() || this.isRenderedEveryFrame(object);
            } else {
                return false;
            }
        }
    }
}
