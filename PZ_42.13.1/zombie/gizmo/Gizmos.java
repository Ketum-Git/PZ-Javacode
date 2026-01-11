// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.popman.ObjectPool;

@UsedFromLua
public final class Gizmos {
    private static Gizmos instance;
    private final Gizmos.PlayerData[] playerData = new Gizmos.PlayerData[4];
    private final ObjectPool<Gizmos.SceneDrawer> drawerPool = new ObjectPool<>(Gizmos.SceneDrawer::new);

    public static Gizmos getInstance() {
        if (instance == null) {
            instance = new Gizmos();
        }

        return instance;
    }

    private Gizmos() {
        for (int i = 0; i < this.playerData.length; i++) {
            this.playerData[i] = new Gizmos.PlayerData();
        }
    }

    private Gizmos.PlayerData getPlayerData(int playerIndex) {
        return this.playerData[playerIndex];
    }

    public Gizmo getGizmo(int playerIndex) {
        return this.getPlayerData(playerIndex).gizmo;
    }

    public void setGizmo(int playerIndex, Gizmo gizmo) {
        this.getPlayerData(playerIndex).gizmo = gizmo;
    }

    public Gizmo getRotateGizmo(int playerIndex) {
        Gizmos.PlayerData playerData1 = this.getPlayerData(playerIndex);
        return playerData1.getRotateGizmo();
    }

    public Gizmo getTranslateGizmo(int playerIndex) {
        Gizmos.PlayerData playerData1 = this.getPlayerData(playerIndex);
        return playerData1.getTranslateGizmo();
    }

    public boolean isTrackingMouse() {
        int playerIndex = 0;
        Gizmos.PlayerData playerData1 = this.getPlayerData(0);
        Scene scene = playerData1.getScene();
        return scene.mouseDown;
    }

    public boolean hitTest(int mouseX, int mouseY) {
        int playerIndex = 0;
        Gizmos.PlayerData playerData1 = this.getPlayerData(0);
        Scene scene = playerData1.getScene();
        return scene.hitTest(mouseX, mouseY);
    }

    public void render(int playerIndex) {
        Gizmos.PlayerData playerData1 = this.getPlayerData(playerIndex);
        Gizmo gizmo = playerData1.gizmo;
        if (gizmo != null && gizmo.isVisible()) {
            Scene scene = playerData1.getScene();
            scene.setGizmo(gizmo);
            scene.setBounds(
                IsoCamera.getScreenLeft(playerIndex),
                IsoCamera.getScreenTop(playerIndex),
                IsoCamera.getScreenWidth(playerIndex),
                IsoCamera.getScreenHeight(playerIndex)
            );
            scene.renderMain();
            Gizmos.SceneDrawer drawer = getInstance().drawerPool.alloc().init(scene);
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private static final class PlayerData {
        Gizmo gizmo;
        Scene scene;
        RotateGizmo rotateGizmo;
        TranslateGizmo translateGizmo;

        Scene getScene() {
            if (this.scene == null) {
                this.scene = new Scene();
            }

            return this.scene;
        }

        RotateGizmo getRotateGizmo() {
            if (this.rotateGizmo == null) {
                this.rotateGizmo = new RotateGizmo(this.getScene());
            }

            return this.rotateGizmo;
        }

        TranslateGizmo getTranslateGizmo() {
            if (this.translateGizmo == null) {
                this.translateGizmo = new TranslateGizmo(this.getScene());
            }

            return this.translateGizmo;
        }
    }

    private static final class SceneDrawer extends TextureDraw.GenericDrawer {
        Scene scene;

        Gizmos.SceneDrawer init(Scene scene) {
            this.scene = scene;
            return this;
        }

        @Override
        public void render() {
            this.scene.render();
        }

        @Override
        public void postRender() {
            Gizmos.getInstance().drawerPool.release(this);
        }
    }
}
