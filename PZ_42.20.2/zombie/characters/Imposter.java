// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.Objects;
import zombie.core.opengl.RenderThread;
import zombie.core.rendering.RenderTarget;
import zombie.core.rendering.RenderTexture;

public class Imposter {
    public static RenderTexture blendTexture;
    public RenderTexture card;
    public boolean cardRendered;
    public int sinceLastUpdate;
    public static int imposterCount;
    public static final int UpdateDelay = 10;
    public static final int Width = 256;
    public static final int Height = 256;

    public static void CreateBlend() {
        if (blendTexture == null) {
            blendTexture = new RenderTexture(new RenderTexture.Descriptor("Imposter Blend") {
                {
                    this.width = 256;
                    this.height = 256;
                    this.colourFormat = 32856;
                    this.depthFormat = 35056;
                    this.depthAsTexture = true;
                }
            });
            blendTexture.Create();
        }
    }

    public void create() {
        if (this.card == null) {
            this.card = new RenderTexture(new RenderTexture.Descriptor("Imposter Card") {
                {
                    Objects.requireNonNull(Imposter.this);
                    this.width = 256;
                    this.height = 256;
                    this.colourFormat = 32856;
                    this.depthFormat = 33189;
                    this.depthAsTexture = true;
                }
            });
            this.card.Create();
            this.sinceLastUpdate = imposterCount % 10;
            imposterCount++;
        }
    }

    public void destroy() {
        if (this.card != null) {
            RenderThread.invokeOnRenderContext(this.card, RenderTarget::Destroy);
            this.card = null;
            this.cardRendered = false;
            this.sinceLastUpdate = 0;
        }
    }
}
