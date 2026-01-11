// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.spriteconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.iso.IsoObject;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteOverlayConfigScript;
import zombie.world.ScriptsDictionary;

@DebugClassFields
@UsedFromLua
public class SpriteOverlayConfig extends Component {
    private SpriteOverlayConfigScript configScript;
    private String appliedStyle;

    private SpriteOverlayConfig() {
        super(ComponentType.SpriteOverlayConfig);
    }

    public ArrayList<String> getAvailableStyles() {
        return this.configScript.getAllStyleNames();
    }

    public boolean hasStyle(String style) {
        return this.configScript.getStyle(style) != null;
    }

    public void clearStyle() {
        this.setStyle(null);
    }

    public void setStyle(String style) {
        if ((style != null || this.appliedStyle != null) && (style == null || !style.equals(this.appliedStyle))) {
            if (style != null && !this.getAvailableStyles().contains(style)) {
                DebugLog.General.error("SpriteOverlay style: %s not found.", style);
                this.appliedStyle = null;
            } else {
                this.appliedStyle = style;
            }

            this.updateStyle();
        }
    }

    private void updateStyle() {
        if (this.getGameEntity() != null && !this.getGameEntity().isMeta()) {
            SpriteConfig spriteConfig = this.getComponent(ComponentType.SpriteConfig);
            SpriteConfigManager.FaceInfo face = spriteConfig.getFaceInfo();
            SpriteConfigManager.TileInfo tile = spriteConfig.getTileInfo();
            if (face != null && tile != null) {
                SpriteOverlayConfigScript.OverlayStyle style = this.configScript.getStyle(this.appliedStyle);
                SpriteOverlayConfigScript.FaceScript overlayFace = null;
                if (style != null) {
                    overlayFace = style.getFace(SpriteConfigManager.GetFaceIdForString(face.getFaceName()));
                }

                ArrayList<IsoObject> isoObjects = new ArrayList<>();
                spriteConfig.getAllMultiSquareObjects(isoObjects);

                for (IsoObject isoObject : isoObjects) {
                    SpriteConfig isoObjectSpriteConfig = isoObject.getComponent(ComponentType.SpriteConfig);
                    int x = -isoObjectSpriteConfig.getMasterOffsetX();
                    int y = -isoObjectSpriteConfig.getMasterOffsetY();
                    int z = -isoObjectSpriteConfig.getMasterOffsetZ();
                    String tileName = null;
                    if (overlayFace != null) {
                        SpriteOverlayConfigScript.TileScript overlayTile = overlayFace.getLayer(z).getRow(y).getTile(x);
                        tileName = overlayTile.getTileName();
                    }

                    isoObject.setOverlaySprite(tileName);
                }
            } else {
                DebugLog.General.error("Unable to apply SpriteOverlay. SpriteConfig FaceInfo or TileInfo not found.");
            }
        }
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
        if (event.getEntity() == this.getGameEntity()) {
            switch (event.getEventType()) {
                case AddedToWorld:
                    this.updateStyle();
            }
        }
    }

    @Override
    protected void readFromScript(ComponentScript script) {
        super.readFromScript(script);
        this.configScript = (SpriteOverlayConfigScript)script;
    }

    @Override
    protected void reset() {
        super.reset();
        this.appliedStyle = null;
    }

    @Override
    public boolean isValid() {
        return !super.isValid()
            ? false
            : this.getOwner() != null && this.getOwner() instanceof IsoObject && this.getOwner().hasComponent(ComponentType.SpriteConfig);
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
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 240);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.configScript != null ? 1 : 0));
        if (this.configScript != null) {
            ScriptsDictionary.spriteOverlayConfigs.saveScript(output, this.configScript);
            output.putLong(this.configScript.getScriptVersion());
        }

        output.put((byte)(this.appliedStyle != null ? 1 : 0));
        if (this.appliedStyle != null) {
            GameWindow.WriteStringUTF(output, this.appliedStyle);
        }
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        if (input.get() == 0) {
            DebugLog.General.error("Sprite config has no script saved.");
        } else {
            SpriteOverlayConfigScript script = ScriptsDictionary.spriteOverlayConfigs.loadScript(input, WorldVersion);
            long scriptVersion = input.getLong();
            if (script != null) {
                this.readFromScript(script);
                if (script.getScriptVersion() != scriptVersion) {
                }
            } else {
                DebugLog.General.error("Could not load script for sprite config.");
            }

            this.appliedStyle = null;
            if (input.get() == 1) {
                this.setStyle(GameWindow.ReadString(input));
            }
        }
    }
}
