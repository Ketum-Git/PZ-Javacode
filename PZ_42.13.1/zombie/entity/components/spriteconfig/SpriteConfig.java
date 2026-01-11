// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.spriteconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.world.ScriptsDictionary;

@DebugClassFields
@UsedFromLua
public class SpriteConfig extends Component {
    private SpriteConfigScript configScript;
    private boolean wasLoadedAsMaster;
    private SpriteConfigManager.TileInfo tileInfo;
    private SpriteConfigManager.FaceInfo faceInfo;
    private SpriteConfigManager.ObjectInfo objectInfo;

    private SpriteConfig() {
        super(ComponentType.SpriteConfig);
    }

    @Override
    protected void readFromScript(ComponentScript script) {
        super.readFromScript(script);
        this.configScript = (SpriteConfigScript)script;
    }

    @Override
    protected void onAddedToOwner() {
        this.initObjectInfo();
    }

    @Override
    protected void onRemovedFromOwner() {
        this.resetObjectInfo();
    }

    private void initObjectInfo() {
        this.resetObjectInfo();
        if (this.getOwner() != null && this.getOwner() instanceof IsoObject) {
            if (this.configScript != null) {
                this.objectInfo = SpriteConfigManager.GetObjectInfo(this.configScript.getName());
                if (this.objectInfo != null) {
                    IsoSprite sprite = ((IsoObject)this.getOwner()).sprite;
                    if (sprite != null) {
                        this.faceInfo = this.objectInfo.getFaceForSprite(sprite.name);
                        if (this.faceInfo != null) {
                            this.tileInfo = this.faceInfo.getTileInfoForSprite(sprite.name);
                        }
                    }
                }
            }

            if (!this.isValid()) {
                DebugLog.General.warn("Invalid SpriteConfig object! scripted object = " + (this.objectInfo != null ? this.objectInfo.getName() : "null"));
                this.resetObjectInfo();
            }
        }
    }

    private void resetObjectInfo() {
        this.objectInfo = null;
        this.faceInfo = null;
        this.tileInfo = null;
    }

    @Override
    protected void reset() {
        super.reset();
        this.resetObjectInfo();
        this.wasLoadedAsMaster = false;
    }

    public SpriteConfigManager.TileInfo getTileInfo() {
        return this.tileInfo;
    }

    public SpriteConfigManager.FaceInfo getFaceInfo() {
        return this.faceInfo;
    }

    public SpriteConfigManager.ObjectInfo getObjectInfo() {
        return this.objectInfo;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.objectInfo != null && this.faceInfo != null && this.tileInfo != null
            ? this.getOwner() != null && this.getOwner() instanceof IsoObject
            : false;
    }

    public boolean isCanRotate() {
        return this.isValid() ? this.objectInfo.canRotate() : false;
    }

    public boolean isValidMultiSquare() {
        return this.isValid() && this.faceInfo.isMultiSquare();
    }

    public boolean isMultiSquareMaster() {
        return this.isValid() && this.tileInfo.isMaster();
    }

    public boolean isMultiSquareSlave() {
        return this.isValid() && !this.tileInfo.isMaster();
    }

    public int getMasterOffsetX() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetX() : 0;
    }

    public int getMasterOffsetY() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetY() : 0;
    }

    public int getMasterOffsetZ() {
        return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetZ() : 0;
    }

    public IsoObject getMultiSquareMaster() {
        if (!this.isValid()) {
            return null;
        } else if (this.faceInfo.isMultiSquare() && !this.tileInfo.isMaster()) {
            IsoObject ownerObj = (IsoObject)this.getOwner();
            IsoGridSquare sq = IsoWorld.instance
                .currentCell
                .getGridSquare(
                    (double)(ownerObj.getX() + this.tileInfo.getMasterOffsetX()),
                    (double)(ownerObj.getY() + this.tileInfo.getMasterOffsetY()),
                    (double)(ownerObj.getZ() + this.tileInfo.getMasterOffsetZ())
                );
            SpriteConfigManager.TileInfo masterTileInfo = this.faceInfo.getMasterTileInfo();
            if (sq != null) {
                for (int i = 0; i < sq.getObjects().size(); i++) {
                    IsoObject obj = sq.getObjects().get(i);
                    SpriteConfig config = obj.getSpriteConfig();
                    if (config != null && config.isMultiSquareMaster() && masterTileInfo.verifyObject(obj)) {
                        return obj;
                    }
                }
            }

            return null;
        } else {
            SpriteConfigManager.TileInfo masterTileInfo = this.faceInfo.getMasterTileInfo();
            IsoObject ownerObj = (IsoObject)this.getOwner();
            return masterTileInfo.verifyObject(ownerObj) ? ownerObj : null;
        }
    }

    public boolean isMultiSquareFullyLoaded() {
        return !this.isValidMultiSquare() ? false : this.findAllMultiSquareObjects(null);
    }

    public boolean getAllMultiSquareObjects(ArrayList<IsoObject> outlist) {
        if (!this.isValid()) {
            return false;
        } else if (!this.faceInfo.isMultiSquare()) {
            if (outlist != null) {
                outlist.add((IsoObject)this.getOwner());
            }

            return true;
        } else {
            return this.findAllMultiSquareObjects(outlist);
        }
    }

    private boolean findAllMultiSquareObjects(ArrayList<IsoObject> outlist) {
        if (!this.isValid()) {
            return false;
        } else if (!this.faceInfo.isMultiSquare()) {
            if (outlist != null) {
                outlist.add((IsoObject)this.getOwner());
            }

            return true;
        } else {
            IsoObject ownerObj = (IsoObject)this.getOwner();
            int cx = ownerObj.getSquare().getX() - this.tileInfo.getX();
            int cy = ownerObj.getSquare().getY() - this.tileInfo.getY();
            int cz = ownerObj.getSquare().getZ() - this.tileInfo.getZ();
            int z = cz;

            for (int tz = 0; z < cz + this.faceInfo.getzLayers(); tz++) {
                int x = cx;

                for (int tx = 0; x < cx + this.faceInfo.getWidth(); tx++) {
                    int y = cy;

                    for (int ty = 0; y < cy + this.faceInfo.getHeight(); ty++) {
                        SpriteConfigManager.TileInfo tileInfo = this.faceInfo.getTileInfo(tx, ty, tz);
                        if (!tileInfo.isEmpty()) {
                            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                            if (sq == null) {
                                return false;
                            }

                            boolean hasMulti = false;

                            for (int i = 0; i < sq.getObjects().size(); i++) {
                                IsoObject object = sq.getObjects().get(i);
                                if (tileInfo.verifyObject(object) && object.getSpriteConfig() != null) {
                                    if (outlist != null) {
                                        outlist.add(object);
                                    }

                                    hasMulti = true;
                                }
                            }

                            if (!hasMulti) {
                                return false;
                            }
                        }

                        y++;
                    }

                    x++;
                }

                z++;
            }

            return true;
        }
    }

    public boolean isWasLoadedAsMaster() {
        return this.wasLoadedAsMaster;
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        switch (type) {
            default:
                return false;
        }
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.configScript != null ? 1 : 0));
        if (this.configScript != null) {
            ScriptsDictionary.spriteConfigs.saveScript(output, this.configScript);
            output.putLong(this.configScript.getScriptVersion());
            output.put((byte)(this.isMultiSquareMaster() ? 1 : 0));
        }
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        if (input.get() == 0) {
            DebugLog.General.error("Sprite config has no script saved.");
        } else {
            SpriteConfigScript script = ScriptsDictionary.spriteConfigs.loadScript(input, WorldVersion);
            long scriptVersion = input.getLong();
            this.wasLoadedAsMaster = input.get() == 1;
            if (script != null) {
                this.readFromScript(script);
                if (script.getScriptVersion() != scriptVersion) {
                }
            } else {
                DebugLog.General.error("Could not load script for sprite config.");
            }
        }
    }
}
