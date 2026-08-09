// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class TextureBinder {
    public static TextureBinder instance = new TextureBinder();
    public int maxTextureUnits;
    public int[] textureUnitIds;
    public int textureUnitIdStart = 33984;
    public int textureIndex;
    public int activeTextureIndex;

    public TextureBinder() {
        this.maxTextureUnits = 1;
        this.textureUnitIds = new int[this.maxTextureUnits];

        for (int n = 0; n < this.maxTextureUnits; n++) {
            this.textureUnitIds[n] = -1;
        }
    }

    public void bind(int textureID) {
        for (int n = 0; n < this.maxTextureUnits; n++) {
            if (this.textureUnitIds[n] == textureID) {
                int textureUnit = n + this.textureUnitIdStart;
                GL13.glActiveTexture(textureUnit);
                this.activeTextureIndex = textureUnit;
                return;
            }
        }

        this.textureUnitIds[this.textureIndex] = textureID;
        GL13.glActiveTexture(this.textureUnitIdStart + this.textureIndex);
        GL11.glBindTexture(3553, textureID);
        this.activeTextureIndex = this.textureUnitIdStart + this.textureIndex;
        this.textureIndex++;
        if (this.textureIndex >= this.maxTextureUnits) {
            this.textureIndex = 0;
        }
    }
}
