// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import zombie.GameTime;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;

public final class CorpseFlies {
    private static Texture texture;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int COLUMNS = 8;
    private static final int ROWS = 7;
    private static final int NUM_FRAMES = 56;
    private static float counter;
    private static int frame;

    public static void render(int squareX, int squareY, int squareZ) {
        if (texture == null) {
            texture = Texture.getSharedTexture("media/textures/CorpseFlies.png");
        }

        if (texture != null && texture.isReady()) {
            int frame = (CorpseFlies.frame + squareX + squareY) % 56;
            int column = frame % 8;
            int row = frame / 8;
            float u1 = (float)(column * 128) / texture.getWidth();
            float v1 = (float)(row * 128) / texture.getHeight();
            float u2 = (float)((column + 1) * 128) / texture.getWidth();
            float v2 = (float)((row + 1) * 128) / texture.getHeight();
            if (IsoSprite.globalOffsetX == -1.0F) {
                IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
            }

            float screenX = IsoUtils.XToScreen(squareX + 0.5F, squareY + 0.5F, squareZ, 0) + IsoSprite.globalOffsetX;
            float screenY = IsoUtils.YToScreen(squareX + 0.5F, squareY + 0.5F, squareZ, 0) + IsoSprite.globalOffsetY;
            int SQUARE_WIDTH = 64;
            int width = 64 * Core.tileScale;
            screenX -= width / 2;
            screenY -= width + 16 * Core.tileScale;
            if (Core.debug) {
            }

            SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                            squareX + 0.75F,
                            squareY + 0.75F,
                            squareZ + 0.25F
                        )
                        .depthStart
                    * 2.0F
                - 1.0F;
            SpriteRenderer.instance.render(texture, screenX, screenY, width, width, 1.0F, 1.0F, 1.0F, 1.0F, u1, v1, u2, v1, u2, v2, u1, v2);
        }
    }

    public static void update() {
        if (!GameServer.server) {
            counter = counter + GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 1000.0F;
            float FPS = 20.0F;
            if (counter > 50.0F) {
                counter %= 50.0F;
                frame++;
                frame %= 56;
            }
        }
    }

    public static void Reset() {
        texture = null;
    }
}
