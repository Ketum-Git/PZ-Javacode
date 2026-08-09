// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.viewCone;

import org.lwjgl.opengl.GL20;
import zombie.characters.IsoPlayer;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;

public class BlurShader extends Shader {
    private int screenInfo;
    private static final float[][] floatArrs = new float[25][];

    public BlurShader(String name) {
        super(name);
    }

    private static float[] getFreeFloatArray() {
        for (int i = 0; i < floatArrs.length; i++) {
            if (floatArrs[i] != null) {
                float[] arr = floatArrs[i];
                floatArrs[i] = null;
                return arr;
            }
        }

        return new float[25];
    }

    @Override
    public void startMainThread(TextureDraw texd, int playerIndex) {
        if (playerIndex >= 0 && playerIndex < 4) {
            RenderSettings.PlayerRenderSettings plrSettings = RenderSettings.getInstance().getPlayerSettings(playerIndex);
            IsoPlayer player = IsoPlayer.players[playerIndex];
            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            if (texd.vars == null) {
                texd.vars = getFreeFloatArray();
                if (texd.vars == null) {
                    texd.vars = new float[25];
                }
            }

            texd.vars[0] = IsoCamera.getOffscreenWidth(playerIndex);
            texd.vars[1] = IsoCamera.getOffscreenHeight(playerIndex);
            texd.vars[2] = camera.rightClickX;
            texd.vars[3] = camera.rightClickY;
        }
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        GL20.glUniform4f(this.screenInfo, texd.vars[0], texd.vars[1], texd.vars[2], texd.vars[3]);
    }

    @Override
    public void onCompileSuccess(ShaderProgram sender) {
        int shaderID = this.getID();
        this.screenInfo = GL20.glGetUniformLocation(shaderID, "ScreenInfo");
    }
}
