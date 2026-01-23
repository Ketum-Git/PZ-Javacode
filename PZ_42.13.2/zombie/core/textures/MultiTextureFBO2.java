// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.util.ArrayList;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.utils.ImageUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoCursor;
import zombie.iso.sprite.IsoReticle;
import zombie.network.GameServer;
import zombie.network.ServerGUI;

public final class MultiTextureFBO2 {
    private final float[] zoomLevelsDefault = new float[]{2.5F, 2.25F, 2.0F, 1.75F, 1.5F, 1.25F, 1.0F, 0.75F, 0.5F, 0.25F};
    private float[] zoomLevels;
    public TextureFBO current;
    public volatile TextureFBO fboRendered;
    private final float[] zoom = new float[4];
    private final float[] targetZoom = new float[4];
    private final float[] startZoom = new float[4];
    private float zoomedInLevel;
    private float zoomedOutLevel;
    public final boolean[] autoZoom = new boolean[4];
    public boolean zoomEnabled = true;

    public MultiTextureFBO2() {
        for (int n = 0; n < 4; n++) {
            this.zoom[n] = this.targetZoom[n] = this.startZoom[n] = 1.0F;
        }
    }

    public int getWidth(int playerIndex) {
        return (int)(IsoCamera.getScreenWidth(playerIndex) * this.getDisplayZoom(playerIndex) * (Core.tileScale / 2.0F));
    }

    public int getHeight(int playerIndex) {
        return (int)(IsoCamera.getScreenHeight(playerIndex) * this.getDisplayZoom(playerIndex) * (Core.tileScale / 2.0F));
    }

    public void setZoom(int playerIndex, float value) {
        this.zoom[playerIndex] = value;
    }

    public void setZoomAndTargetZoom(int playerIndex, float value) {
        this.zoom[playerIndex] = value;
        this.targetZoom[playerIndex] = value;
    }

    public float getZoom(int playerIndex) {
        return this.zoom[playerIndex];
    }

    public float getTargetZoom(int playerIndex) {
        return this.targetZoom[playerIndex];
    }

    public float getDisplayZoom(int playerIndex) {
        return Core.width > Core.initialWidth ? this.zoom[playerIndex] * (Core.initialWidth / Core.width) : this.zoom[playerIndex];
    }

    public void setTargetZoom(int playerIndex, float target) {
        if (this.targetZoom[playerIndex] != target) {
            this.targetZoom[playerIndex] = target;
            this.startZoom[playerIndex] = this.zoom[playerIndex];
        }
    }

    public ArrayList<Integer> getDefaultZoomLevels() {
        ArrayList<Integer> percents = new ArrayList<>();
        float[] levels = this.zoomLevelsDefault;

        for (int i = 0; i < levels.length; i++) {
            percents.add(Math.round(levels[i] * 100.0F));
        }

        return percents;
    }

    public void setZoomLevels(Double... zooms) {
        this.zoomLevels = new float[zooms.length];

        for (int i = 0; i < zooms.length; i++) {
            this.zoomLevels[i] = zooms[i].floatValue();
        }
    }

    public void setZoomLevelsFromOption(String levels) {
        this.zoomLevels = this.zoomLevelsDefault;
        if (levels != null && !levels.isEmpty()) {
            String[] ss = levels.split(";");
            if (ss.length != 0) {
                ArrayList<Integer> percents = new ArrayList<>();

                for (String s : ss) {
                    if (!s.isEmpty()) {
                        try {
                            int percent = Integer.parseInt(s);

                            for (float knownLevel : this.zoomLevels) {
                                if (Math.round(knownLevel * 100.0F) == percent) {
                                    if (!percents.contains(percent)) {
                                        percents.add(percent);
                                    }
                                    break;
                                }
                            }
                        } catch (NumberFormatException var13) {
                        }
                    }
                }

                if (!percents.contains(100)) {
                    percents.add(100);
                }

                percents.sort((o1, o2) -> o2 - o1);
                this.zoomLevels = new float[percents.size()];

                for (int i = 0; i < percents.size(); i++) {
                    int playerIndex = IsoPlayer.getPlayerIndex();
                    this.zoomLevels[i] = percents.get(i).intValue() / 100.0F;
                }
            }
        }
    }

    public void destroy() {
        if (this.current != null) {
            this.current.destroy();
            this.current = null;
            this.fboRendered = null;

            for (int n = 0; n < 4; n++) {
                this.zoom[n] = this.targetZoom[n] = 1.0F;
            }
        }
    }

    public void create(int xres, int yres) throws Exception {
        if (this.zoomEnabled) {
            if (this.zoomLevels == null) {
                this.zoomLevels = this.zoomLevelsDefault;
            }

            this.zoomedInLevel = this.zoomLevels[this.zoomLevels.length - 1];
            this.zoomedOutLevel = this.zoomLevels[0];
            int x = ImageUtils.getNextPowerOfTwoHW(xres);
            int y = ImageUtils.getNextPowerOfTwoHW(yres);
            this.current = this.createTexture(x, y, false);
        }
    }

    public void update() {
        int playerIndex = IsoPlayer.getPlayerIndex();
        if (!this.zoomEnabled) {
            this.zoom[playerIndex] = this.targetZoom[playerIndex] = 1.0F;
        }

        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (this.autoZoom[playerIndex] && isoGameCharacter != null && this.zoomEnabled) {
            float dist = IsoUtils.DistanceTo(IsoCamera.getRightClickOffX(), IsoCamera.getRightClickOffY(), 0.0F, 0.0F);
            float delta = dist / 300.0F;
            if (delta > 1.0F) {
                delta = 1.0F;
            }

            float zoom = this.shouldAutoZoomIn() ? this.zoomedInLevel : this.zoomedOutLevel;
            zoom += delta;
            if (zoom > this.zoomLevels[0]) {
                zoom = this.zoomLevels[0];
            }

            if (isoGameCharacter.getVehicle() != null) {
                zoom = this.getMaxZoom();
            }

            this.setTargetZoom(playerIndex, zoom);
        }

        float step = 0.004F * GameTime.instance.getMultiplier() / GameTime.instance.getTrueMultiplier() * (Core.tileScale == 2 ? 1.5F : 1.5F);
        if (!this.autoZoom[playerIndex]) {
            step *= 5.0F;
        } else if (this.targetZoom[playerIndex] > this.zoom[playerIndex]) {
            step *= 1.0F;
        }

        if (this.targetZoom[playerIndex] > this.zoom[playerIndex]) {
            this.zoom[playerIndex] = this.zoom[playerIndex] + step;
            IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 2.0F;
            if (this.zoom[playerIndex] > this.targetZoom[playerIndex] || Math.abs(this.zoom[playerIndex] - this.targetZoom[playerIndex]) < 0.001F) {
                this.zoom[playerIndex] = this.targetZoom[playerIndex];
            }
        }

        if (this.targetZoom[playerIndex] < this.zoom[playerIndex]) {
            this.zoom[playerIndex] = this.zoom[playerIndex] - step;
            IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 2.0F;
            if (this.zoom[playerIndex] < this.targetZoom[playerIndex] || Math.abs(this.zoom[playerIndex] - this.targetZoom[playerIndex]) < 0.001F) {
                this.zoom[playerIndex] = this.targetZoom[playerIndex];
            }
        }

        this.setCameraToCentre();
    }

    private boolean shouldAutoZoomIn() {
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            return false;
        } else {
            IsoGridSquare square = isoGameCharacter.getCurrentSquare();
            if (square != null && !square.isOutside()) {
                return true;
            } else if (isoGameCharacter instanceof IsoPlayer player) {
                if (player.isRunning() || player.isSprinting()) {
                    return false;
                } else {
                    return player.closestZombie < 6.0F && player.isTargetedByZombie() ? true : player.lastTargeted < PerformanceSettings.getLockFPS() * 4;
                }
            } else {
                return false;
            }
        }
    }

    private void setCameraToCentre() {
        PlayerCamera camera = IsoCamera.cameras[IsoPlayer.getPlayerIndex()];
        camera.center();
    }

    private TextureFBO createTexture(int x, int y, boolean test) {
        if (test) {
            Texture tex = new Texture(x, y, 16);
            TextureFBO newOne = new TextureFBO(tex);
            newOne.destroy();
            return null;
        } else {
            Texture tex = new Texture(x, y, 19);
            return new TextureFBO(tex);
        }
    }

    public void render() {
        if (this.current != null) {
            int max = 0;

            for (int playerIndex = 3; playerIndex >= 0; playerIndex--) {
                if (IsoPlayer.players[playerIndex] != null) {
                    max = playerIndex > 1 ? 3 : playerIndex;
                    break;
                }
            }

            max = Math.max(max, IsoPlayer.numPlayers - 1);

            for (int playerIndexx = 0; playerIndexx <= max; playerIndexx++) {
                if (SceneShaderStore.weatherShader != null && DebugOptions.instance.fboRenderChunk.useWeatherShader.getValue()) {
                    IndieGL.StartShader(SceneShaderStore.weatherShader, playerIndexx);
                }

                int sx = IsoCamera.getScreenLeft(playerIndexx);
                int sy = IsoCamera.getScreenTop(playerIndexx);
                int sw = IsoCamera.getScreenWidth(playerIndexx);
                int sh = IsoCamera.getScreenHeight(playerIndexx);
                if (IsoPlayer.players[playerIndexx] != null || GameServer.server && ServerGUI.isCreated()) {
                    ((Texture)this.current.getTexture()).rendershader2(sx, sy, sw, sh, sx, sy, sw, sh, 1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    SpriteRenderer.instance.renderi(null, sx, sy, sw, sh, 0.0F, 0.0F, 0.0F, 1.0F, null);
                }
            }

            if (SceneShaderStore.weatherShader != null) {
                IndieGL.EndShader();
            }

            switch (CombatManager.targetReticleMode) {
                case 1:
                    IsoReticle.getInstance().render(0);
                    break;
                default:
                    IsoCursor.getInstance().render(0);
            }
        }
    }

    public TextureFBO getCurrent(int nPlayer) {
        return this.current;
    }

    public Texture getTexture(int nPlayer) {
        return (Texture)this.current.getTexture();
    }

    public void doZoomScroll(int playerIndex, int del) {
        this.targetZoom[playerIndex] = this.getNextZoom(playerIndex, del);
    }

    public float getNextZoom(int playerIndex, int del) {
        if (this.zoomEnabled && this.zoomLevels != null) {
            if (del > 0) {
                for (int i = this.zoomLevels.length - 1; i > 0; i--) {
                    if (this.targetZoom[playerIndex] == this.zoomLevels[i]) {
                        return this.zoomLevels[i - 1];
                    }
                }
            } else if (del < 0) {
                for (int ix = 0; ix < this.zoomLevels.length - 1; ix++) {
                    if (this.targetZoom[playerIndex] == this.zoomLevels[ix]) {
                        return this.zoomLevels[ix + 1];
                    }
                }
            }

            return this.targetZoom[playerIndex];
        } else {
            return 1.0F;
        }
    }

    public float getMinZoom() {
        return this.zoomEnabled && this.zoomLevels != null && this.zoomLevels.length != 0 ? this.zoomLevels[this.zoomLevels.length - 1] : 1.0F;
    }

    public float getMaxZoom() {
        return this.zoomEnabled && this.zoomLevels != null && this.zoomLevels.length != 0 ? this.zoomLevels[0] : 1.0F;
    }

    public boolean test() {
        try {
            this.createTexture(16, 16, true);
            return true;
        } catch (Exception var2) {
            DebugLog.General.error("Failed to create Test FBO");
            var2.printStackTrace();
            Core.safeMode = true;
            return false;
        }
    }
}
