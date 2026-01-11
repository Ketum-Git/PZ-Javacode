// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;

public final class PathfindNativeRenderer {
    public static final PathfindNativeRenderer instance = new PathfindNativeRenderer();
    final PathfindNativeRenderer.Drawer[][] drawers = new PathfindNativeRenderer.Drawer[4][3];
    PlayerCamera camera;

    public void render() {
        if (Core.debug) {
            if (DebugOptions.instance.pathfindPathToMouseEnable.getValue()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                int stateIndex = SpriteRenderer.instance.getMainStateIndex();
                PathfindNativeRenderer.Drawer drawer = this.drawers[playerIndex][stateIndex];
                if (drawer == null) {
                    drawer = this.drawers[playerIndex][stateIndex] = new PathfindNativeRenderer.Drawer(playerIndex);
                }

                SpriteRenderer.instance.drawGeneric(drawer);
            }
        }
    }

    public void drawLine(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, float thickness, float r, float g, float b, float a) {
        VBORenderer vbor = VBORenderer.getInstance();
        float x1 = this.camera.XToScreenExact(fromX, fromY, fromZ, 0);
        float y1 = this.camera.YToScreenExact(fromX, fromY, fromZ, 0);
        float x2 = this.camera.XToScreenExact(toX, toY, toZ, 0);
        float y2 = this.camera.YToScreenExact(toX, toY, toZ, 0);
        if (PerformanceSettings.fboRenderChunk) {
            x1 += this.camera.fixJigglyModelsX * this.camera.zoom;
            y1 += this.camera.fixJigglyModelsY * this.camera.zoom;
            x2 += this.camera.fixJigglyModelsX * this.camera.zoom;
            y2 += this.camera.fixJigglyModelsY * this.camera.zoom;
        }

        if (thickness == 1.0F) {
            vbor.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, a);
        } else {
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(7);
            vbor.addLineWithThickness(x1, y1, 0.0F, x2, y2, 0.0F, thickness, r, g, b, a);
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);
        }
    }

    public void drawRect(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        float thickness = 1.0F;
        this.drawLine(x, y, z, x + w, y, z, 1.0F, r, g, b, a);
        this.drawLine(x + w, y, z, x + w, y + h, z, 1.0F, r, g, b, a);
        this.drawLine(x + w, y + h, z, x, y + h, z, 1.0F, r, g, b, a);
        this.drawLine(x, y + h, z, x, y, z, 1.0F, r, g, b, a);
    }

    native void renderNative();

    native void setDebugOption(String var1, String var2);

    static final class Drawer extends TextureDraw.GenericDrawer {
        int playerIndex;

        Drawer(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        @Override
        public void render() {
            if (PathfindNativeThread.instance != null) {
                synchronized (PathfindNativeThread.instance.renderLock) {
                    VBORenderer vbor = VBORenderer.getInstance();
                    vbor.startRun(vbor.formatPositionColor);
                    vbor.setMode(1);
                    int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
                    PathfindNativeRenderer.instance.camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
                    PathfindNativeRenderer.instance
                        .setDebugOption(
                            DebugOptions.instance.pathfindPathToMouseRenderSuccessors.getName(),
                            DebugOptions.instance.pathfindPathToMouseRenderSuccessors.getValueAsString()
                        );
                    PathfindNativeRenderer.instance
                        .setDebugOption(
                            DebugOptions.instance.pathfindSmoothPlayerPath.getName(), DebugOptions.instance.pathfindSmoothPlayerPath.getValueAsString()
                        );
                    PathfindNativeRenderer.instance
                        .setDebugOption(
                            DebugOptions.instance.pathfindRenderChunkRegions.getName(), DebugOptions.instance.pathfindRenderChunkRegions.getValueAsString()
                        );
                    PathfindNativeRenderer.instance
                        .setDebugOption(DebugOptions.instance.pathfindRenderPath.getName(), DebugOptions.instance.pathfindRenderPath.getValueAsString());
                    PathfindNativeRenderer.instance
                        .setDebugOption(DebugOptions.instance.polymapRenderClusters.getName(), DebugOptions.instance.polymapRenderClusters.getValueAsString());
                    PathfindNativeRenderer.instance
                        .setDebugOption(
                            DebugOptions.instance.polymapRenderConnections.getName(), DebugOptions.instance.polymapRenderConnections.getValueAsString()
                        );
                    PathfindNativeRenderer.instance
                        .setDebugOption(DebugOptions.instance.polymapRenderCrawling.getName(), DebugOptions.instance.polymapRenderCrawling.getValueAsString());
                    PathfindNativeRenderer.instance
                        .setDebugOption(DebugOptions.instance.polymapRenderNodes.getName(), DebugOptions.instance.polymapRenderNodes.getValueAsString());
                    PathfindNativeRenderer.instance.renderNative();
                    vbor.endRun();
                    vbor.flush();
                }
            }
        }
    }
}
